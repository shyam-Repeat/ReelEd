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
                repeatInterval = 24,
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

            // ── Fetch from Supabase ───────────────────────────────────────────
            // fetchActiveQuestions returns QuizQuestionDto list.
            // toEntity() extension functions live on the DTO classes (layout doc).
            val questions = repository.fetchActiveQuestionsFromRemote(limit = MAX_FETCH)

            if (questions.isEmpty()) {
                // Supabase returned nothing — succeed silently.
                // Could be an empty table in early dev; don't retry aggressively.
                return@withContext Result.success()
            }

            // ── Upsert into Room ──────────────────────────────────────────────
            // INSERT OR REPLACE semantics — safe to re-fetch the same question IDs.
            // Updates existing rows if question content has changed server-side.
            repository.upsertQuestions(questions)

            Result.success()

        } catch (e: Exception) {
            // Retry with WorkManager default backoff.
            // The overlay can still fire from whatever is cached — non-fatal.
            Result.retry()
        }
    }
}