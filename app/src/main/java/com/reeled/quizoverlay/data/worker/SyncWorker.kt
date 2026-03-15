package com.reeled.quizoverlay.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reeled.quizoverlay.di.AppContainer

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            AppContainer(applicationContext).quizRepository.syncUnsynced()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
