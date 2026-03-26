package com.reeled.quizoverlay.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reeled.quizoverlay.data.repository.QuizRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// QuizFetchWorker
//
// Responsibilities (service.md §5.4 / layout doc):
//   1. Check how many active quiz questions are in Room
//   2. If count >= MIN_CACHE_SIZE (30) → skip, cache is healthy
//   3. If count <  MIN_CACHE_SIZE    → fetch up to MAX_FETCH (100) from Supabase
//   4. Upsert fetched questions into Room (INSERT OR REPLACE)
//
// When it runs:
//   • One-time on every app open (MainActivity.onCreate)
//   • Daily periodic run for background cache refresh
//
// What this worker does NOT do:
//   • Does not upload any attempt or event data — that is SyncWorker's job
//   • Does not run if network is unavailable (Constraints enforce CONNECTED)
//
// Dependency map (layout doc): worker → data/repository, prefs
// ─────────────────────────────────────────────────────────────────────────────

class QuizFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // Minimum number of active questions that must be in Room at all times.
        // If the device goes offline, the overlay can still fire from the cache.
        const val MIN_CACHE_SIZE = 30

        // Upper bound per fetch request — avoids oversized payloads.
        // ~100 questions × ~500 bytes JSON = ~50 KB per fetch (service.md §5.6)
        const val MAX_FETCH = 100

        const val PERIODIC_WORK_NAME = "quiz_fetch"

        // ── Network constraint shared by both one-time and periodic requests ──
        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Enqueues a one-time fetch. Call from MainActivity.onCreate().
         * Does not replace or cancel any already-running periodic work.
         */
        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<QuizFetchWorker>()
                .setConstraints(networkConstraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Enqueues the daily periodic fetch.
         * Uses KEEP policy — does not reset an already-scheduled run.
         * Call once from App.kt after WorkManager initialisation.
         */
        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<QuizFetchWorker>(
                repeatInterval = 48,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    private val repository = QuizRepository(applicationContext)

    // ─────────────────────────────────────────────────────────────────────────
    // Work
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cachedCount = repository.getActiveQuestionCount()

            // Cache is healthy — no fetch needed
            if (cachedCount >= MIN_CACHE_SIZE) {
                return@withContext Result.success()
            }

            // ── Balanced Fetch from Supabase ─────────────────────────────────
            // Fetch 10 questions per supported type to maintain variety.
            val types = listOf(
                "TAP_CHOICE",
                "TAP_TAP_MATCH",
                "DRAG_DROP_MATCH",
                "FILL_BLANK",
                "DRAW_MATCH"
            )
            val allFetched = mutableListOf<com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity>()
            
            for (type in types) {
                val questions = repository.fetchActiveQuestionsFromRemote(limit = 10, cardType = type)
                allFetched.addAll(questions)
            }

            if (allFetched.isEmpty()) {
                return@withContext Result.success()
            }

            // ── Upsert into Room ──────────────────────────────────────────────
            repository.upsertQuestions(allFetched)

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}
