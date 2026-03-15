package com.reeled.quizoverlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.trigger.ForegroundAppDetector
import com.reeled.quizoverlay.trigger.TriggerDecision
import com.reeled.quizoverlay.trigger.TriggerEngine
import com.reeled.quizoverlay.trigger.VideoPlaybackDetector
import com.reeled.quizoverlay.ui.overlay.QuizCardRouter
import com.reeled.quizoverlay.util.AudioMuter
import com.reeled.quizoverlay.util.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java).apply {
                action = "STOP"
            }
    }

    private lateinit var triggerEngine: TriggerEngine
    private lateinit var repository: QuizRepository
    private lateinit var triggerPrefs: TriggerPrefs
    private lateinit var appPrefs: AppPrefs
    private lateinit var audioMuter: AudioMuter
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner().also { it.onCreate() }
        
        audioMuter = AudioMuter(this)
        triggerPrefs = TriggerPrefs(this)
        appPrefs = AppPrefs(this)
        repository = QuizRepository(this)
        
        val foregroundAppDetector = ForegroundAppDetector(this)
        val videoPlaybackDetector = VideoPlaybackDetector(this, foregroundAppDetector)
        triggerEngine = TriggerEngine(
            repository, 
            triggerPrefs, 
            foregroundAppDetector, 
            videoPlaybackDetector
        )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification(paused = false))
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        startPollingLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
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
                    val decision = triggerEngine.checkAndFire()

                    if (decision is TriggerDecision.Fire) {
                        withContext(Dispatchers.Main) {
                            if (overlayView == null) {
                                showOverlay(decision.question, decision.sourceApp)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error
                }
                delay(30000L) // 30 seconds
            }
        }
    }

    private fun showOverlay(question: com.reeled.quizoverlay.model.QuizQuestion, sourceApp: String) {
        if (overlayView != null) return

        val params = buildWindowParams(question.strictMode)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                // Mapping Entity to Config for UI
                val config = QuizCardConfig.from(question)
                QuizCardRouter(
                    config = config,
                    sourceApp = sourceApp,
                    onResult = { result -> onQuizResult(result) },
                    onDismissed = { onQuizDismissed(question, sourceApp) }
                )
            }
        }

        windowManager.addView(composeView, params)
        overlayView = composeView
        audioMuter.mute()
    }

    private fun onQuizResult(result: QuizAttemptResult) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    removeOverlayIfShowing()
                    audioMuter.restore()
                }

                repository.saveAttempt(
                    questionId = result.questionId,
                    selectedOptionId = result.selectedOptionId,
                    isCorrect = result.isCorrect,
                    responseTimeMs = result.responseTimeMs,
                    sourceApp = result.sourceApp,
                    dismissed = result.wasDismissed
                )

            } catch (e: Exception) {
                // Log error
            }
        }
    }

    private fun onQuizDismissed(question: com.reeled.quizoverlay.model.QuizQuestion, sourceApp: String) {
        onQuizResult(QuizAttemptResult(
            questionId = question.id,
            selectedOptionId = null,
            isCorrect = false,
            wasDismissed = true,
            wasTimerExpired = false,
            responseTimeMs = 0,
            sourceApp = sourceApp
        ))
    }

    private fun removeOverlayIfShowing() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun buildWindowParams(strictMode: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    private fun buildNotification(paused: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (paused) "Learning mode paused" else "Learning mode active")
            .setContentText(if (paused) "Quiz overlay is paused" else "Quiz overlay is running")
            .setSmallIcon(R.drawable.ic_brain)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
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
        removeOverlayIfShowing()
        pollingJob?.cancel()
        stopForeground(true)
        stopSelf()
    }
}
