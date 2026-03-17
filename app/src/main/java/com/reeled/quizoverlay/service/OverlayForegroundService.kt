package com.reeled.quizoverlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.trigger.ForegroundAppDetector
import com.reeled.quizoverlay.trigger.TriggerConfig
import com.reeled.quizoverlay.trigger.TriggerDecision
import com.reeled.quizoverlay.trigger.TriggerEngine
import com.reeled.quizoverlay.trigger.VideoPlaybackDetector
import com.reeled.quizoverlay.ui.overlay.QuizCardRouter
import com.reeled.quizoverlay.ui.pin.PinActivity
import com.reeled.quizoverlay.util.AudioMuter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayForegroundService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "quiz_overlay_channel"
        const val CHANNEL_NAME = "Quiz Overlay"

        const val ACTION_PAUSE = "com.reeled.quizoverlay.ACTION_PAUSE"
        const val ACTION_RESUME = "com.reeled.quizoverlay.ACTION_RESUME"
        const val ACTION_EXTEND = "com.reeled.quizoverlay.ACTION_EXTEND"

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java).apply {
                action = "STOP"
            }
    }

    private lateinit var triggerEngine: TriggerEngine
    private lateinit var foregroundAppDetector: ForegroundAppDetector
    private lateinit var repository: QuizRepository
    private lateinit var triggerPrefs: TriggerPrefs
    private lateinit var appPrefs: AppPrefs
    private lateinit var audioMuter: AudioMuter
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    private var currentSessionId: String? = null
    private var totalQuizzesShown = 0
    private var totalAnswered = 0
    private var totalDismissed = 0
    private var totalTimerExpired = 0

    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null
    private var sessionActive: Boolean = false
    private var audioMutedForSession: Boolean = false
    private var lastReportedSkipReason: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner().also { it.onCreate() }

        audioMuter = AudioMuter(this)
        triggerPrefs = TriggerPrefs(this)
        appPrefs = AppPrefs(this)
        repository = QuizRepository(this)

        foregroundAppDetector = ForegroundAppDetector(this)
        val videoPlaybackDetector = VideoPlaybackDetector(this, foregroundAppDetector)
        triggerEngine = TriggerEngine(
            repository,
            triggerPrefs,
            foregroundAppDetector,
            videoPlaybackDetector
        )

        createNotificationChannel()

        // Start new session
        val sessionId = java.util.UUID.randomUUID().toString()
        currentSessionId = sessionId
        serviceScope.launch(Dispatchers.IO) {
            repository.startSession(sessionId)
            try {
                repository.logEvent("overlay_started", "{\"session_id\":\"$sessionId\"}")
            } catch (_: Exception) {
                // Keep service startup resilient.
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                serviceScope.launch { triggerPrefs.clearParentPause() }
            }
            ACTION_EXTEND -> {
                serviceScope.launch {
                    val state = triggerPrefs.getTriggerState()
                    val base = maxOf(System.currentTimeMillis(), state.parentPauseExpiryMs)
                    triggerPrefs.setPause(true, base + 30 * 60 * 1000)
                }
            }
        }

        sessionActive = true
        startForeground(NOTIF_ID, buildNotification(paused = false))
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
        syncAudioState()

        startPollingLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        sessionActive = false
        pollingJob?.cancel()
        currentSessionId?.let { sessionId ->
            serviceScope.launch(Dispatchers.IO) {
                try {
                    repository.logEvent("overlay_stopped", "{\"session_id\":\"$sessionId\"}")
                } catch (_: Exception) {
                    // no-op
                }
                repository.endSession(sessionId)
                serviceScope.cancel()
            }
        } ?: serviceScope.cancel()

        removeOverlayIfShowing()
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    reconcileOverlayState()
                    val monitoredApps = appPrefs.monitoredApps.first()
                    val decision = triggerEngine.checkAndFire(monitoredApps)

                    when (decision) {
                        is TriggerDecision.Fire -> {
                            val latestForeground = triggerEngineForegroundPackage()
                            if (latestForeground != decision.sourceApp) {
                                val reason = "fire_aborted_not_foreground"
                                if (reason != lastReportedSkipReason) {
                                    lastReportedSkipReason = reason
                                    logEventSafely(
                                        eventType = "trigger_skip",
                                        payloadJson = "{\"reason\":\"${jsonSafe(reason)}\",\"expected\":\"${jsonSafe(decision.sourceApp)}\",\"actual\":\"${jsonSafe(latestForeground.orEmpty())}\"}"
                                    )
                                }
                            } else {
                                lastReportedSkipReason = null
                                withContext(Dispatchers.Main) {
                                    if (overlayView == null) {
                                        totalQuizzesShown++
                                        updateSessionStats()
                                        showOverlay(decision.question, decision.sourceApp)
                                    }
                                }
                            }
                        }
                        is TriggerDecision.Skip -> {
                            if (decision.reason != lastReportedSkipReason) {
                                lastReportedSkipReason = decision.reason
                                logEventSafely(
                                    eventType = "trigger_skip",
                                    payloadJson = "{\"reason\":\"${jsonSafe(decision.reason)}\"}"
                                )
                            }
                        }
                    }
                } catch (exception: Exception) {
                    logEventSafely(
                        eventType = "overlay_poll_error",
                        payloadJson = "{\"message\":\"${jsonSafe(exception.message.orEmpty())}\"}"
                    )
                    // Keep service alive and continue polling.
                }
                delay(TriggerConfig.POLLING_INTERVAL_MS)
            }
        }
    }

    private suspend fun reconcileOverlayState() {
        val attached = overlayView?.isAttachedToWindow == true
        if (!attached) {
            if (overlayView != null) {
                overlayView = null
            }
            val state = triggerPrefs.getTriggerState()
            if (state.overlayActive) {
                triggerPrefs.setOverlayActive(false)
                logEventSafely(
                    eventType = "overlay_state_recovered",
                    payloadJson = "{\"reason\":\"stale_overlay_flag\"}"
                )
            }
        }
    }

    private fun triggerEngineForegroundPackage(): String? {
        return try {
            foregroundAppDetector.getCurrentForegroundPackage()
        } catch (_: Exception) {
            null
        }
    }

    private fun updateSessionStats() {
        val sessionId = currentSessionId ?: return
        serviceScope.launch(Dispatchers.IO) {
            repository.updateSessionStats(
                sessionId,
                totalQuizzesShown,
                totalAnswered,
                totalDismissed,
                totalTimerExpired
            )
        }
    }

    private fun showOverlay(question: com.reeled.quizoverlay.model.QuizQuestion, sourceApp: String) {
        if (overlayView != null) return

        val params = buildWindowParams(strictMode = question.strictMode)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val config = QuizCardConfig.from(question)
                QuizCardRouter(
                    config = config,
                    sourceApp = sourceApp,
                    onResult = { result -> onQuizResult(result) },
                    onDismissed = { onQuizDismissed(question, sourceApp) }
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (exception: Exception) {
            logEventSafely(
                eventType = "overlay_add_failed",
                payloadJson = "{\"question_id\":\"${question.id}\",\"source_app\":\"${jsonSafe(sourceApp)}\",\"message\":\"${jsonSafe(exception.message.orEmpty())}\"}"
            )
            return
        }
        overlayView = composeView
        serviceScope.launch(Dispatchers.IO) {
            triggerPrefs.setOverlayActive(true)
            logEventSafely(
                eventType = "quiz_shown",
                payloadJson = "{\"question_id\":\"${question.id}\",\"source_app\":\"${jsonSafe(sourceApp)}\"}",
            )
        }
        syncAudioState()
    }

    private fun onQuizResult(result: QuizAttemptResult) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    if (result.wasTimerExpired) {
                        totalTimerExpired++
                    } else if (result.wasDismissed) {
                        totalDismissed++
                    } else {
                        totalAnswered++
                    }
                    updateSessionStats()
                    removeOverlayIfShowing()
                }

                repository.saveAttempt(
                    questionId = result.questionId,
                    selectedOptionId = result.selectedOptionId,
                    isCorrect = result.isCorrect,
                    responseTimeMs = result.responseTimeMs,
                    sourceApp = result.sourceApp,
                    dismissed = result.wasDismissed
                )
                val eventType = when {
                    result.wasTimerExpired -> "quiz_timer_expired"
                    result.wasDismissed -> "quiz_dismissed"
                    else -> "quiz_answered"
                }
                logEventSafely(
                    eventType = eventType,
                    payloadJson = "{\"question_id\":\"${result.questionId}\",\"correct\":${result.isCorrect},\"response_ms\":${result.responseTimeMs},\"source_app\":\"${jsonSafe(result.sourceApp)}\"}",
                )
                triggerPrefs.recordQuizShown(
                    questionId = result.questionId,
                    wasCorrect = result.isCorrect,
                    wasDismissed = result.wasDismissed
                )

            } catch (exception: Exception) {
                logEventSafely(
                    eventType = "quiz_result_error",
                    payloadJson = "{\"message\":\"${jsonSafe(exception.message.orEmpty())}\",\"question_id\":\"${result.questionId}\"}"
                )
            }
        }
    }

    private fun onQuizDismissed(question: com.reeled.quizoverlay.model.QuizQuestion, sourceApp: String) {
        onQuizResult(
            QuizAttemptResult(
                questionId = question.id,
                selectedOptionId = null,
                isCorrect = false,
                wasDismissed = true,
                wasTimerExpired = false,
                responseTimeMs = 0,
                sourceApp = sourceApp
            )
        )
    }

    private fun removeOverlayIfShowing() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
            overlayView = null
        }
        serviceScope.launch(Dispatchers.IO) { triggerPrefs.setOverlayActive(false) }
        syncAudioState()
    }

    private fun syncAudioState() {
        val shouldMute = sessionActive && overlayView != null
        when {
            shouldMute && !audioMutedForSession -> {
                audioMuter.mute()
                audioMutedForSession = true
            }
            !shouldMute && audioMutedForSession -> {
                audioMuter.restore()
                audioMutedForSession = false
            }
        }
    }

    private fun buildWindowParams(strictMode: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val baseFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val flags = if (strictMode) {
            baseFlags
        } else {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    private fun buildNotification(paused: Boolean): Notification {
        val pauseIntent = Intent(this, PinActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pausePendingIntent = PendingIntent.getActivity(
            this,
            100,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, OverlayForegroundService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this,
            101,
            resumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = Intent(this, OverlayForegroundService::class.java).apply {
            action = ACTION_EXTEND
        }
        val extendPendingIntent = PendingIntent.getService(
            this,
            102,
            extendIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (paused) "Learning mode paused" else "Learning mode active")
            .setContentText(if (paused) "Quiz overlay is paused" else "Quiz overlay is running")
            .setSmallIcon(R.drawable.ic_brain)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Pause Quizzes", pausePendingIntent)
            .addAction(0, "Resume", resumePendingIntent)
            .addAction(0, "Extend", extendPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        sessionActive = false
        removeOverlayIfShowing()
        pollingJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun logEventSafely(eventType: String, payloadJson: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                repository.logEvent(eventType = eventType, payloadJson = payloadJson)
            } catch (_: Exception) {
                // no-op
            }
        }
    }
    private fun jsonSafe(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

}
