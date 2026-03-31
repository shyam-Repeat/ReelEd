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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import com.reeled.quizoverlay.ui.theme.ReelEdTheme
import com.reeled.quizoverlay.util.AudioMuter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class OverlayForegroundService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "quiz_overlay_channel"
        const val CHANNEL_NAME = "Quiz Overlay"

        const val ACTION_PAUSE = "com.reeled.quizoverlay.ACTION_PAUSE"
        const val ACTION_RESUME = "com.reeled.quizoverlay.ACTION_RESUME"
        const val ACTION_EXTEND = "com.reeled.quizoverlay.ACTION_EXTEND"
        const val ACTION_SET_APP_PAUSE = "com.reeled.quizoverlay.ACTION_SET_APP_PAUSE"
        const val EXTRA_SOURCE_APP = "extra_source_app"
        const val EXTRA_PAUSE_EXPIRY_MS = "extra_pause_expiry_ms"

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

    private var currentSessionId: String? = null
    private var totalQuizzesShown = 0
    private var totalAnswered = 0
    private var totalDismissed = 0
    private var totalTimerExpired = 0

    private var overlayView: ComposeView? = null
    private var currentOverlaySourceApp: String? = null
    private var currentOverlayQuestionId: String? = null
    private var overlayShownAtMs: Long = 0L
    private var overlayTimeoutMs: Long = 120_000L
    private var overlayForegroundMismatchSinceMs: Long = 0L
    private var currentViewLifecycleOwner: OverlayLifecycleOwner? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null
    private var sessionActive: Boolean = false
    private var audioMutedForSession: Boolean = false
    private var lastReportedSkipReason: String? = null
    private var lastTestModeQuizTime: Long = 0
    private var lastNotificationPaused: Boolean? = null
    private var configuredQuizTimerSeconds: Int = TriggerPrefs.DEFAULT_QUIZ_TIMER_SECONDS
    private var configuredForceQuizEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

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

        serviceScope.launch {
            triggerPrefs.quizTimerSeconds.collect { configuredQuizTimerSeconds = it }
        }
        serviceScope.launch {
            triggerPrefs.forceQuizEnabled.collect { configuredForceQuizEnabled = it }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    triggerPrefs.clearParentPause()
                    triggerPrefs.clearAllAppPauses()
                    refreshNotificationPauseState()
                }
            }
            ACTION_EXTEND -> {
                serviceScope.launch {
                    val state = triggerPrefs.getTriggerState()
                    val base = maxOf(System.currentTimeMillis(), state.parentPauseExpiryMs)
                    triggerPrefs.setPause(true, base + 30 * 60 * 1000)
                    refreshNotificationPauseState()
                }
            }
            ACTION_SET_APP_PAUSE -> {
                val sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP)
                val expiryMs = intent.getLongExtra(EXTRA_PAUSE_EXPIRY_MS, 0L)
                if (!sourceApp.isNullOrBlank() && expiryMs > System.currentTimeMillis()) {
                    serviceScope.launch {
                        triggerPrefs.setAppPause(sourceApp, expiryMs)
                        if (currentOverlaySourceApp == sourceApp) {
                            withContext(Dispatchers.Main) {
                                logEventSafely(
                                    eventType = "overlay_removed_parent_pause_app",
                                    payloadJson = "{\"source_app\":\"${jsonSafe(sourceApp)}\",\"expiry_ms\":$expiryMs}"
                                )
                                removeOverlayIfShowing()
                            }
                        }
                        refreshNotificationPauseState()
                    }
                }
            }
        }

        val overlayEnabled = runBlocking { appPrefs.overlayEnabled.first() }
        if (!overlayEnabled) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        sessionActive = true
        startForeground(NOTIF_ID, buildNotification(paused = false, overlayEnabled = true))
        serviceScope.launch { refreshNotificationPauseState() }
        syncAudioState()

        startPollingLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        sessionActive = false
        pollingJob?.cancel()
        currentSessionId?.let { sessionId ->
            serviceScope.launch(Dispatchers.IO) {
                withContext(NonCancellable) {
                    try {
                        repository.logEvent("overlay_stopped", "{\"session_id\":\"$sessionId\"}")
                    } catch (_: Exception) {
                        // no-op
                    }
                    repository.endSession(sessionId)
                }
            }
        }
        serviceScope.cancel()

        removeOverlayIfShowing()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val enabled = appPrefs.overlayEnabled.first()
                    if (!enabled) {
                        val reason = "overlay_disabled_by_parent"
                        if (reason != lastReportedSkipReason) {
                            lastReportedSkipReason = reason
                            logEventSafely(
                                eventType = "trigger_skip",
                                payloadJson = "{\"reason\":\"${jsonSafe(reason)}\"}"
                            )
                        }
                        removeOverlayIfShowing()
                        stopForegroundService()
                        return@launch
                    } else {
                        reconcileOverlayState()
                        val isTestMode = appPrefs.isTestMode.first()
                        
                        if (isTestMode) {
                            handleTestModePolling()
                        } else {
                            handleNormalPolling()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    logEventSafely(
                        eventType = "overlay_poll_error",
                        payloadJson = "{\"message\":\"${jsonSafe(exception.message.orEmpty())}\"}"
                    )
                }
                refreshNotificationPauseState()
                val nextDelay = if (overlayView != null) 1000L else TriggerConfig.POLLING_INTERVAL_MS
                delay(nextDelay)
            }
        }
    }

    private suspend fun handleTestModePolling() {
        if (overlayView != null) return

        val now = System.currentTimeMillis()
        if (now - lastTestModeQuizTime < 30 * 1000L) {
            val remaining = (30 * 1000L - (now - lastTestModeQuizTime)) / 1000
            val reason = "test_mode_cooldown (${remaining}s)"
            if (reason != lastReportedSkipReason) {
                lastReportedSkipReason = reason
                logEventSafely(
                    eventType = "trigger_skip",
                    payloadJson = "{\"reason\":\"${jsonSafe(reason)}\"}"
                )
            }
            return
        }

        val allQuestions = repository.getAllActiveQuestions()
        if (allQuestions.isEmpty()) {
            val reason = "test_mode_no_questions"
            if (reason != lastReportedSkipReason) {
                lastReportedSkipReason = reason
                logEventSafely(
                    eventType = "trigger_skip",
                    payloadJson = "{\"reason\":\"${jsonSafe(reason)}\"}"
                )
            }
            return
        }

        val question = allQuestions.random()
        val sourceApp = triggerEngineForegroundPackage() ?: "test.mode.app"
        
        lastReportedSkipReason = null
        lastTestModeQuizTime = now
        
        withContext(Dispatchers.Main) {
            if (overlayView == null) {
                totalQuizzesShown++
                updateSessionStats()
                showOverlay(question, sourceApp)
            }
        }
    }

    private suspend fun handleNormalPolling() {
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
    }

    private suspend fun reconcileOverlayState() {
        val view = overlayView
        if (view == null) {
            overlayForegroundMismatchSinceMs = 0L
            // Check if we have a stale flag in triggerPrefs
            val state = triggerPrefs.getTriggerState()
            if (state.overlayActive) {
                triggerPrefs.setOverlayActive(false)
                logEventSafely(
                    eventType = "overlay_state_recovered",
                    payloadJson = "{\"reason\":\"stale_overlay_flag\"}"
                )
            }
            return
        }

        // If the view is attached, check for app switches.
        // We only check for app switches if the view is attached to a window.
        if (view.isAttachedToWindow) {
            val now = System.currentTimeMillis()

            // Service-side timeout safety: always end quiz even if UI timer fails.
            if (overlayShownAtMs > 0L && now - overlayShownAtMs >= overlayTimeoutMs) {
                val qid = currentOverlayQuestionId
                val src = currentOverlaySourceApp
                if (qid != null && src != null) {
                    withContext(Dispatchers.Main) {
                        logEventSafely(
                            eventType = "overlay_timeout_enforced",
                            payloadJson = "{\"question_id\":\"${jsonSafe(qid)}\",\"source_app\":\"${jsonSafe(src)}\"}"
                        )
                        onQuizResult(
                            QuizAttemptResult(
                                questionId = qid,
                                selectedOptionId = "TIMER_EXPIRED_SERVICE",
                                isCorrect = false,
                                wasDismissed = false,
                                wasTimerExpired = true,
                                responseTimeMs = overlayTimeoutMs,
                                sourceApp = src
                            )
                        )
                    }
                }
                return
            }

            val activeSourceApp = currentOverlaySourceApp
            if (activeSourceApp != null) {
                val appPauseExpiry = triggerPrefs.getAppPauseExpiry(activeSourceApp)
                if (appPauseExpiry > now) {
                    withContext(Dispatchers.Main) {
                        logEventSafely(
                            eventType = "overlay_removed_parent_pause_app",
                            payloadJson = "{\"source_app\":\"${jsonSafe(activeSourceApp)}\",\"expiry_ms\":$appPauseExpiry}"
                        )
                        removeOverlayIfShowing()
                    }
                    return
                }

                val currentForeground = triggerEngineForegroundPackage()

                // Dismiss when source app is no longer foreground (including app close/home).
                if (currentForeground == activeSourceApp) {
                    overlayForegroundMismatchSinceMs = 0L
                } else {
                    if (overlayForegroundMismatchSinceMs == 0L) {
                        overlayForegroundMismatchSinceMs = now
                    }
                    if (now - overlayForegroundMismatchSinceMs >= 1500L) {
                        val qid = currentOverlayQuestionId
                        if (qid != null) {
                            withContext(Dispatchers.Main) {
                                logEventSafely(
                                    eventType = "overlay_removed_app_switched",
                                    payloadJson = "{\"question_id\":\"${jsonSafe(qid)}\",\"expected\":\"${jsonSafe(activeSourceApp)}\",\"actual\":\"${jsonSafe(currentForeground.orEmpty())}\"}"
                                )
                                onQuizDismissed(qid, activeSourceApp)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                removeOverlayIfShowing()
                            }
                        }
                    }
                }
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

    private fun showOverlay(
        question: com.reeled.quizoverlay.model.QuizQuestion,
        sourceApp: String,
        skippedInvalidQuestionIds: Set<String> = emptySet()
    ) {
        if (overlayView != null) return

        val latestForeground = triggerEngineForegroundPackage()
        if (sourceApp != latestForeground) {
            logEventSafely(
                eventType = "overlay_show_aborted_not_foreground",
                payloadJson = "{\"expected\":\"${jsonSafe(sourceApp)}\",\"actual\":\"${jsonSafe(latestForeground.orEmpty())}\"}"
            )
            return
        }

        val config = try {
            QuizCardConfig.from(question)
        } catch (exception: Exception) {
            continueAfterInvalidQuestion(
                invalidQuestionId = question.id,
                sourceApp = sourceApp,
                skippedInvalidQuestionIds = skippedInvalidQuestionIds,
                reason = "config_parse_failed:${exception.javaClass.simpleName}",
                incrementShownForNext = false
            )
            return
        }

        val params = buildWindowParams(strictMode = question.strictMode)
        val effectiveTimerSeconds = configuredQuizTimerSeconds
        val effectiveConfig = config.copy(
            rules = config.rules.copy(timerSeconds = effectiveTimerSeconds)
        )

        val viewLifecycleOwner = OverlayLifecycleOwner().also { it.onCreate() }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(viewLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(viewLifecycleOwner)

            setContent {
                ReelEdTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        QuizCardRouter(
                            config = effectiveConfig,
                            sourceApp = sourceApp,
                            forceQuizEnabled = configuredForceQuizEnabled,
                            onResult = { result -> onQuizResult(result) },
                            onDismissed = { onQuizDismissed(question, sourceApp) },
                            onInvalidPayload = { questionId, reason ->
                                continueAfterInvalidQuestion(
                                    invalidQuestionId = questionId,
                                    sourceApp = sourceApp,
                                    skippedInvalidQuestionIds = skippedInvalidQuestionIds,
                                    reason = reason,
                                    incrementShownForNext = true
                                )
                            }
                        )
                    }
                }
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
        
        viewLifecycleOwner.onStart()
        viewLifecycleOwner.onResume()

        currentViewLifecycleOwner = viewLifecycleOwner
        currentOverlaySourceApp = sourceApp
        currentOverlayQuestionId = question.id
        overlayShownAtMs = System.currentTimeMillis()
        overlayTimeoutMs = effectiveTimerSeconds * 1000L
        overlayForegroundMismatchSinceMs = 0L
        overlayView = composeView
        serviceScope.launch(Dispatchers.IO) {
            triggerPrefs.markQuizShown(question.id)
            triggerPrefs.setOverlayActive(true)
            logEventSafely(
                eventType = "quiz_shown",
                payloadJson = "{\"question_id\":\"${question.id}\",\"source_app\":\"${jsonSafe(sourceApp)}\"}",
            )
        }
        syncAudioState()
    }

    private fun continueAfterInvalidQuestion(
        invalidQuestionId: String,
        sourceApp: String,
        skippedInvalidQuestionIds: Set<String>,
        reason: String,
        incrementShownForNext: Boolean
    ) {
        val updatedSkippedIds = skippedInvalidQuestionIds + invalidQuestionId
        serviceScope.launch(Dispatchers.IO) {
            logEventSafely(
                eventType = "quiz_invalid_skipped",
                payloadJson = "{\"question_id\":\"${jsonSafe(invalidQuestionId)}\",\"source_app\":\"${jsonSafe(sourceApp)}\",\"reason\":\"${jsonSafe(reason)}\"}"
            )

            withContext(Dispatchers.Main) {
                removeOverlayIfShowing()
            }

            val candidates = repository.getAllActiveQuestions()
                .filter { it.id !in updatedSkippedIds }
            val nextQuestion = candidates.randomOrNull() ?: return@launch

            val latestForeground = triggerEngineForegroundPackage()
            if (latestForeground != sourceApp) {
                logEventSafely(
                    eventType = "overlay_show_aborted_not_foreground",
                    payloadJson = "{\"expected\":\"${jsonSafe(sourceApp)}\",\"actual\":\"${jsonSafe(latestForeground.orEmpty())}\"}"
                )
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (overlayView == null) {
                    if (incrementShownForNext) {
                        totalQuizzesShown++
                        updateSessionStats()
                    }
                    showOverlay(nextQuestion, sourceApp, updatedSkippedIds)
                }
            }
        }
    }

    private fun onQuizResult(result: QuizAttemptResult) {
        // Use a lock-like check to ensure we only process one "ending" result per overlay instance
        val currentView = overlayView
        if (currentView == null) return

        serviceScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    if (overlayView != currentView) return@withContext // Already handled this view
                    
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
                triggerPrefs.recordQuizOutcome(
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

    private fun onQuizDismissed(questionId: String, sourceApp: String) {
        onQuizResult(
            QuizAttemptResult(
                questionId = questionId,
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
                if (view.isAttachedToWindow || view.windowToken != null) {
                    windowManager.removeView(view)
                }
            } catch (_: Exception) {
            }
        }
        overlayView = null
        currentOverlaySourceApp = null
        currentOverlayQuestionId = null
        overlayShownAtMs = 0L
        overlayTimeoutMs = 120_000L
        overlayForegroundMismatchSinceMs = 0L

        currentViewLifecycleOwner?.let { owner ->
            owner.onPause()
            owner.onStop()
            owner.onDestroy()
        }
        currentViewLifecycleOwner = null

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

        val baseFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

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

    private fun buildNotification(paused: Boolean, overlayEnabled: Boolean): Notification {
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

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                if (!overlayEnabled) "Learning mode off"
                else if (paused) "Learning mode paused"
                else "Learning mode active"
            )
            .setContentText(
                if (!overlayEnabled) "Enable quiz overlay from parent controls"
                else if (paused) "Quiz overlay paused for one or more apps"
                else "Quiz overlay is running"
            )
            .setSmallIcon(R.drawable.ic_brain)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (overlayEnabled) {
            builder
                .addAction(0, "Pause Quizzes", pausePendingIntent)
                .addAction(0, "Resume All", resumePendingIntent)
                .addAction(0, "Extend", extendPendingIntent)
        }

        return builder.build()
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

    private suspend fun refreshNotificationPauseState() {
        if (!appPrefs.overlayEnabled.first()) return
        val paused = isPauseActiveNow()
        if (lastNotificationPaused == paused) return
        val notification = buildNotification(paused = paused, overlayEnabled = true)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
        lastNotificationPaused = paused
    }

    private suspend fun isPauseActiveNow(): Boolean {
        val now = System.currentTimeMillis()
        val state = triggerPrefs.getTriggerState()
        val parentPaused = state.parentPauseActive && now < state.parentPauseExpiryMs
        if (state.parentPauseActive && !parentPaused) {
            triggerPrefs.clearParentPause()
        }
        val anyAppPaused = triggerPrefs.hasActiveAppPause()
        return parentPaused || anyAppPaused
    }

}
