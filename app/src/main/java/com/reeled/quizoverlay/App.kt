package com.reeled.quizoverlay

import android.app.Application
import com.reeled.quizoverlay.service.OverlayServiceCoordinator
import com.reeled.quizoverlay.util.CrashLogger
import com.reeled.quizoverlay.worker.QuizFetchWorker
import com.reeled.quizoverlay.worker.SyncWorker
import com.reeled.quizoverlay.worker.ServiceWatchdogWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize global crash detection
        CrashLogger.init(this)
        
        // 2. Schedule periodic background tasks
        QuizFetchWorker.scheduleDaily(this)
        SyncWorker.schedule(this)
        ServiceWatchdogWorker.schedule(this)

        // Monitoring should be driven by onboarding completion, not UI routes.
        OverlayServiceCoordinator.ensureStartedIfOnboardingComplete(this)
    }
}
