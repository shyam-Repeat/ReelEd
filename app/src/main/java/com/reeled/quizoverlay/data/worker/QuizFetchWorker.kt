package com.reeled.quizoverlay.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reeled.quizoverlay.di.AppContainer

class QuizFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = AppContainer(applicationContext).quizRepository
            if (repository.getActiveQuestionCount() < MIN_CACHE_SIZE) {
                repository.refreshQuestions(limit = MAX_FETCH)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val MIN_CACHE_SIZE = 30
        private const val MAX_FETCH = 100
    }
}
