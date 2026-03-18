package com.reeled.quizoverlay.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.reeled.quizoverlay.prefs.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object OverlayServiceCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun ensureStartedIfOnboardingComplete(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val appPrefs = AppPrefs(appContext)
            if (appPrefs.onboardingComplete.first()) {
                ContextCompat.startForegroundService(
                    appContext,
                    OverlayForegroundService.startIntent(appContext)
                )
            }
        }
    }

    fun startAfterOnboarding(context: Context) {
        val appContext = context.applicationContext
        ContextCompat.startForegroundService(
            appContext,
            OverlayForegroundService.startIntent(appContext)
        )
    }
}
