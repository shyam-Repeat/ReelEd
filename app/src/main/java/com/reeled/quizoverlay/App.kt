package com.reeled.quizoverlay

import android.app.Application
import com.reeled.quizoverlay.worker.QuizFetchWorker
import com.reeled.quizoverlay.worker.SyncWorker
import com.reeled.quizoverlay.worker.ServiceWatchdogWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic background tasks
        QuizFetchWorker.scheduleDaily(this)
        SyncWorker.schedule(this)
        ServiceWatchdogWorker.schedule(this)
    }
}
