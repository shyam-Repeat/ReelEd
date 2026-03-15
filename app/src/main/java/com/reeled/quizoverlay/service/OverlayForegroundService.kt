package com.reeled.quizoverlay.service
package com.yourappname.quizoverlay.service

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
import com.yourappname.quizoverlay.R
import com.yourappname.quizoverlay.data.repository.QuizRepository
import com.yourappname.quizoverlay.model.QuizAttemptResult
import com.yourappname.quizoverlay.model.QuizQuestion
import com.yourappname.quizoverlay.prefs.AppPrefs
import com.yourappname.quizoverlay.prefs.TriggerPrefs
import com.yourappname.quizoverlay.trigger.TriggerConfig
import com.yourappname.quizoverlay.trigger.TriggerDecision
import com.yourappname.quizoverlay.trigger.TriggerEngine
import com.yourappname.quizoverlay.ui.overlay.QuizCardRouter
import com.yourappname.quizoverlay.util.AudioMuter
import com.yourappname.quizoverlay.util.NotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// OverlayForegroundService
//
// Responsibilities (from layout doc / service.md):
//   1. Show persistent notification — required by Android for foreground services
//   2. Run a polling coroutine every TriggerConfig.POLLING_INTERVAL_MS (30 s)
//   3. Call TriggerEngine.checkAndFire() on each tick
//   4. On TriggerDecision.Fire → draw overlay via WindowManager
//   5. Receive QuizAttemptResult → write to Room via repository
//   6. Update DataStore trigger state via TriggerPrefs
//
// What this service does NOT do (by design):
//   • No network calls — that is SyncWorker's job
//   • No heavy Room queries on Main thread — IO dispatcher only
//   • No GPS, audio recording, or camera access
//
// Dependency map (layout doc):
//   service → model, data/repository, prefs, trigger, util
// ─────────────────────────────────────────────────────────────────────────────

class OverlayForegroundService : Service() {

    // ── Notification constants ────────────────────────────────────────────────
    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "quiz_overlay_channel"
        const val CHANNEL_NAME = "Quiz Overlay"

        // Intent actions used by NotificationBuilder PendingIntents
        const val ACTION_PAUSE = "com.yourappname.quizoverlay.ACTION_PAUSE"
        const val ACTION_RESUME = "com.yourappname.quizoverlay.ACTION_RESUME"

        fun startIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, OverlayForegroundService::class.java).apply {
                action = "STOP"
            }
    }

    // ── Dependencies (injected via field — replace with Hilt/DI if added later) ──
    private lateinit var triggerEngine: TriggerEngine
    private lateinit var repository: QuizRepository
    private lateinit var triggerPrefs: TriggerPrefs
    private lateinit var appPrefs: AppPrefs
    private lateinit var audioMuter: AudioMuter
    private lateinit var windowManager: WindowManager

    // ── Overlay lifecycle owner (required to host ComposeView in a Service) ──
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    // ── Active overlay view reference — null when no quiz is showing ─────────
    private var overlayView: ComposeView? = null

    // ── Coroutine scope — SupervisorJob so one failed tick doesn't kill the loop ──
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Initialise the lifecycle owner so ComposeView can observe lifecycle events
        lifecycleOwner = OverlayLifecycleOwner().also { it.onCreate() }

        // Wire up dependencies — swap for DI constructor injection when ready
        audioMuter = AudioMuter(this)
        triggerPrefs = TriggerPrefs(this)
        appPrefs = AppPrefs(this)
        repository = QuizRepository(this)           // replace with DI-provided singleton
        triggerEngine = TriggerEngine(repository, triggerPrefs, this)

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

        // START_STICKY — Android will restart this service if killed.
        // Critical for overlay continuity between app-kill events.
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

    override fun onBind(intent: Intent?): IBinder? = null     // not a bound service

    // ─────────────────────────────────────────────────────────────────────────
    // Polling loop
    // ─────────────────────────────────────────────────────────────────────────

    private fun startPollingLoop() {
        // Cancel any existing loop before starting a new one (handles restart)
        pollingJob?.cancel()

        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val decision = triggerEngine.checkAndFire()

                    if (decision is TriggerDecision.Fire) {
                        withContext(Dispatchers.Main) {
                            // Only show a new overlay if one is not already visible
                            if (overlayView == null) {
                                showOverlay(decision.question)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Swallow individual tick failures — loop must not crash
                    // Crashlytics will pick this up via the uncaught handler in App.kt
                }

                delay(TriggerConfig.POLLING_INTERVAL_MS)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlay management (Main thread only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inflates a ComposeView hosting QuizCardRouter and adds it to WindowManager.
     * Must be called on Main thread.
     */
    private fun showOverlay(question: QuizQuestion) {
        // Guard: do not stack overlays
        if (overlayView != null) return

        val params = buildWindowParams()

        val composeView = ComposeView(this).apply {
            // Wire the lifecycle owner so Compose recomposition works correctly
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                QuizCardRouter(
                    question = question,
                    onResult = { result -> onQuizResult(result) },
                    onDismiss = { onQuizDismissed(question) }
                )
            }
        }

        windowManager.addView(composeView, params)
        overlayView = composeView

        // Mute media audio for the duration of the quiz (restored in onQuizResult)
        audioMuter.mute()

        // Mark overlay as active in DataStore so TriggerEngine skips while showing
        serviceScope.launch {
            triggerPrefs.setOverlayActive(true)
        }
    }

    /**
     * Receives the result from the quiz card Composable.
     * Writes to Room on IO dispatcher — never calls Supabase directly.
     */
    private fun onQuizResult(result: QuizAttemptResult) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 1. Remove overlay on Main thread first for instant UX feedback
                withContext(Dispatchers.Main) {
                    removeOverlayIfShowing()
                    audioMuter.restore()
                }

                // 2. Persist attempt to Room (synced = false — SyncWorker handles upload)
                repository.saveAttempt(result)

                // 3. Write matching event log entry to Room
                repository.logEvent(
                    eventType = if (result.dismissed) "quiz_dismissed" else "quiz_answered",
                    payload = result.toEventPayload()
                )

                // 4. Update trigger state: lastQuizShownTime, quizzesToday counter
                triggerPrefs.recordQuizShown(
                    questionId = result.questionId,
                    wasCorrect = result.isCorrect,
                    wasDismissed = result.dismissed
                )

                // 5. Clear overlay-active flag so next poll tick can fire
                triggerPrefs.setOverlayActive(false)

            } catch (e: Exception) {
                // Ensure overlay flag is cleared even on failure
                triggerPrefs.setOverlayActive(false)
            }
        }
    }

    /**
     * Handles a parent-dismissed quiz (tapped lock icon, not answered).
     * Records a dismissed attempt so the dashboard counts are accurate.
     */
    private fun onQuizDismissed(question: QuizQuestion) {
        val dismissedResult = QuizAttemptResult(
            questionId = question.id,
            selectedOptionId = null,
            isCorrect = false,
            responseTimeMs = 0L,
            sourceApp = triggerPrefs.getLastForegroundApp(),
            dismissed = true
        )
        onQuizResult(dismissedResult)
    }

    /**
     * Removes the overlay view from WindowManager if one is present.
     * Safe to call from any thread — posts to Main if needed.
     */
    private fun removeOverlayIfShowing() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // View may already have been removed — ignore
            }
            overlayView = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WindowManager layout params
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildWindowParams(): WindowManager.LayoutParams {
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

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the persistent foreground notification.
     * PRIORITY_LOW = no sound, minimal UI interruption for the child.
     * Two states: active (quiz running) and paused (parent paused session).
     */
    fun buildNotification(paused: Boolean = false): Notification {
        val title = if (paused) "Learning mode paused" else "Learning mode active"
        val text = if (paused) "Quiz overlay is paused" else "Quiz overlay is running"

        // Pause / Resume action button — opens PinActivity via PendingIntent
        // (PinActivity has noHistory + noAnimation per AndroidManifest)
        val actionIntent = if (paused) {
            PendingIntent.getService(
                this, 0,
                Intent(this, OverlayForegroundService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            NotificationBuilder.buildPinActivityPendingIntent(this)
        }

        val actionLabel = if (paused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_brain)
            .setPriority(NotificationCompat.PRIORITY_LOW)   // no sound, no heads-up
            .setOngoing(true)                                // not dismissible by swipe
            .addAction(0, actionLabel, actionIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW             // matches PRIORITY_LOW
            ).apply {
                description = "Persistent notification for the quiz overlay service"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
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
}