package com.reeled.quizoverlay

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reeled.quizoverlay.data.worker.QuizFetchWorker
import com.reeled.quizoverlay.data.worker.SyncWorker
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES).build()
        val fetchRequest = PeriodicWorkRequestBuilder<QuizFetchWorker>(24, TimeUnit.HOURS).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "quiz_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "quiz_fetch",
            ExistingPeriodicWorkPolicy.KEEP,
            fetchRequest
        )
    }
}
