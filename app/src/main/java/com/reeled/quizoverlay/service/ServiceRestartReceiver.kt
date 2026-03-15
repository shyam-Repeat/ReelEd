package com.reeled.quizoverlay.service
package com.yourappname.quizoverlay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.yourappname.quizoverlay.prefs.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ServiceRestartReceiver
//
// Responsibilities (layout doc):
//   BroadcastReceiver that restarts OverlayForegroundService if the OS kills it.
//   Declared in AndroidManifest with exported="false" — internal only.
//
// Triggers handled:
//   1. ACTION_BOOT_COMPLETED — restart service after device reboot
//      (requires RECEIVE_BOOT_COMPLETED permission in AndroidManifest)
//   2. Custom ACTION_RESTART_SERVICE — broadcast sent by ServiceWatchdogWorker
//      when it detects the service has stopped unexpectedly
//
// Guard:
//   Only restarts if the overlay is enabled in AppPrefs.
//   Does nothing if the parent has disabled the overlay — respects parent control.
//
// AndroidManifest declaration required:
//   <receiver
//       android:name=".service.ServiceRestartReceiver"
//       android:exported="false">
//       <intent-filter>
//           <action android:name="android.intent.action.BOOT_COMPLETED" />
//           <action android:name="com.yourappname.quizoverlay.RESTART_SERVICE" />
//       </intent-filter>
//   </receiver>
//
// Dependency map (layout doc): service → prefs, model
// ─────────────────────────────────────────────────────────────────────────────

class ServiceRestartReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESTART_SERVICE =
            "com.yourappname.quizoverlay.RESTART_SERVICE"

        /** Broadcast this from ServiceWatchdogWorker to trigger a restart. */
        fun restartIntent(context: Context): Intent =
            Intent(context, ServiceRestartReceiver::class.java).apply {
                action = ACTION_RESTART_SERVICE
            }
    }

    // BroadcastReceiver callbacks must be fast — we use a short-lived coroutine
    // only for the DataStore read, then immediately kick the service.
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_RESTART_SERVICE -> handleRestart(context)

            else -> { /* ignore unknown actions */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Restart logic
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleRestart(context: Context) {
        // goAsync() gives us 10 seconds to do async work in a BroadcastReceiver.
        // We need this only for the DataStore read — everything else is sync.
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val appPrefs = AppPrefs(context)
                val overlayEnabled = appPrefs.isOverlayEnabled()
                val onboardingComplete = appPrefs.isOnboardingComplete()

                // Only restart if:
                //   - Onboarding is done (permissions granted, PIN set)
                //   - Parent has not disabled the overlay
                if (overlayEnabled && onboardingComplete) {
                    startService(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startService(context: Context) {
        val serviceIntent = OverlayForegroundService.startIntent(context)
        // startForegroundService required for API 26+ foreground services
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}