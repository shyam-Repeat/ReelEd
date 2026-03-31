package com.reeled.quizoverlay.worker

import android.app.ActivityManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.service.OverlayForegroundService
import com.reeled.quizoverlay.service.OverlayServiceCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "service_watchdog"
        const val REPEAT_INTERVAL_MINUTES = 15L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                repeatInterval = REPEAT_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    private val appPrefs = AppPrefs(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val onboardingComplete = appPrefs.onboardingComplete.first()
            val overlayEnabled = appPrefs.overlayEnabled.first()
            if (!onboardingComplete || !overlayEnabled) return@withContext Result.success()

            if (!isServiceRunning(OverlayForegroundService::class.java)) {
                OverlayServiceCoordinator.startAfterOnboarding(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
