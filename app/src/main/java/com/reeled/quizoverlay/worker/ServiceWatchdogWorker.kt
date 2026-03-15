package com.reeled.quizoverlay.worker
package com.yourappname.quizoverlay.worker

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yourappname.quizoverlay.prefs.AppPrefs
import com.yourappname.quizoverlay.service.OverlayForegroundService
import com.yourappname.quizoverlay.service.ServiceRestartReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// ServiceWatchdogWorker
//
// Responsibilities (layout doc):
//   Checks if OverlayForegroundService is running.
//   Restarts it if not, AND overlay is enabled in AppPrefs.
//   Runs every 15 minutes via WorkManager periodic scheduling.
//
// Why this exists:
//   Android's OEM-specific battery optimisation (Xiaomi MIUI, Samsung OneUI, etc.)
//   can kill foreground services even with FOREGROUND_SERVICE permission.
//   START_STICKY in the service handles clean kills. This worker handles cases
//   where the BroadcastReceiver restart also fails (e.g. on aggressive OEMs).
//
// Two-layer restart strategy:
//   Layer 1: START_STICKY in OverlayForegroundService.onStartCommand
//   Layer 2: ServiceRestartReceiver.ACTION_RESTART_SERVICE broadcast (this worker)
//
// Guard:
//   Does nothing if overlay is disabled by the parent (AppPrefs check).
//   Does nothing if the service is already running — no duplicate starts.
//
// Dependency map (layout doc): worker → data/repository, prefs, service (start intent only)
// ─────────────────────────────────────────────────────────────────────────────

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "service_watchdog"
        const val REPEAT_INTERVAL_MINUTES = 15L

        /**
         * Enqueues the watchdog. No network constraint — should run offline too.
         * Uses KEEP — do not reset an already-scheduled watchdog.
         * Call from App.kt after WorkManager initialisation.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                repeatInterval = REPEAT_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()
            // No network constraint intentionally — watchdog must run regardless
            // of connectivity. Restarting a service needs no network.

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    private val appPrefs = AppPrefs(applicationContext)

    // ─────────────────────────────────────────────────────────────────────────
    // Work
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // ── Guard: only watchdog if overlay is supposed to be active ──────
            val overlayEnabled = appPrefs.isOverlayEnabled()
            val onboardingComplete = appPrefs.isOnboardingComplete()

            if (!overlayEnabled || !onboardingComplete) {
                return@withContext Result.success()     // parent disabled it — do nothing
            }

            // ── Check if the service is currently running ─────────────────────
            if (!isServiceRunning()) {
                restartService()
            }

            Result.success()

        } catch (e: Exception) {
            // Watchdog failures are non-fatal — don't crash the WorkManager chain
            Result.success()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service detection
    //
    // ActivityManager.getRunningServices() is deprecated for API 26+ but remains
    // the only reliable way to check for your OWN service's running state.
    // The deprecation notice applies to checking OTHER apps' services — checking
    // your own service is still the documented approach for watchdog patterns.
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
                as ActivityManager

        return am.getRunningServices(Int.MAX_VALUE).any { serviceInfo ->
            serviceInfo.service.className == OverlayForegroundService::class.java.name
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Restart
    // Route restart through ServiceRestartReceiver so all restart logic
    // lives in one place (guards, logging, etc.)
    // ─────────────────────────────────────────────────────────────────────────

    private fun restartService() {
        // Option 1: via broadcast → ServiceRestartReceiver (preferred — single path)
        val restartBroadcast = ServiceRestartReceiver.restartIntent(applicationContext)
        applicationContext.sendBroadcast(restartBroadcast)

        // Option 2 (fallback): direct start if the broadcast is delayed by the OS
        // Uncomment only if broadcast-based restart proves unreliable on target OEMs.
        // val serviceIntent = OverlayForegroundService.startIntent(applicationContext)
        // ContextCompat.startForegroundService(applicationContext, serviceIntent)
    }
}