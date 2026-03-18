package com.reeled.quizoverlay.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.prefs.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// SyncWorker
//
// Responsibilities (service.md §5.3 / layout doc):
//   1. Read all unsynced quiz_attempts from Room  (synced = false)
//   2. Read all unsynced event_logs from Room     (synced = false)
//   3. Batch POST both collections to Supabase via Retrofit
//   4. On success → mark rows synced = true in Room
//   5. On failure → log to Crashlytics, return Result.retry() with backoff
//
// What this worker does NOT do:
//   • Does not touch the WindowManager or overlay
//   • Does not fetch quiz questions — that is QuizFetchWorker's job
//   • Does not run on Main thread — IO dispatcher throughout
//
// Schedule: every 30 minutes, CONNECTED network, exponential backoff on failure
//
// Dependency map (layout doc): worker → data/repository, prefs
// ─────────────────────────────────────────────────────────────────────────────

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "quiz_sync"
        const val REPEAT_INTERVAL_MINUTES = 30L
        const val BACKOFF_DELAY_MINUTES = 5L

        /**
         * Enqueues the periodic SyncWorker.
         * Uses KEEP policy — does not reset an already-scheduled worker.
         * Call once from App.kt after WorkManager initialisation.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                // Any network connection — WiFi-only would block sync on mobile data,
                // which would break the 10-tester MVP rollout. (service.md §5.3)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = REPEAT_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }

    // Injected via repository — replace with DI-provided instance when ready.
    // Worker constructors can only receive Context + WorkerParameters, so we
    // build the repository here for MVP. With Hilt: use HiltWorker annotation.
    private val repository = QuizRepository(applicationContext)
    private val appPrefs = AppPrefs(applicationContext)

    // ─────────────────────────────────────────────────────────────────────────
    // Work
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedAttempts = repository.getUnsyncedAttempts()
            val unsyncedEvents = repository.getUnsyncedEvents()
            val unsyncedSessions = repository.getUnsyncedSessions()

            // Nothing to sync — exit early, counts as success
            if (unsyncedAttempts.isEmpty() && unsyncedEvents.isEmpty() && unsyncedSessions.isEmpty()) {
                return@withContext Result.success()
            }

            // Ensure tester is registered before syncing anything to prevent 409 FK violations
            val nickname = appPrefs.nickname.first()
            repository.registerTester(nickname)

            // ── Batch upsert to Supabase ──────────────────────────────────────
            if (unsyncedAttempts.isNotEmpty()) {
                repository.batchUploadAttempts(unsyncedAttempts)
            }

            if (unsyncedEvents.isNotEmpty()) {
                repository.batchUploadEvents(unsyncedEvents)
            }

            if (unsyncedSessions.isNotEmpty()) {
                repository.batchUploadSessions(unsyncedSessions)
            }

            // ── Mark rows as synced in Room ───────────────────────────────────
            if (unsyncedAttempts.isNotEmpty()) {
                repository.markAttemptsSynced(unsyncedAttempts.map { it.id })
            }

            if (unsyncedEvents.isNotEmpty()) {
                repository.markEventsSynced(unsyncedEvents.map { it.id })
            }

            if (unsyncedSessions.isNotEmpty()) {
                repository.markSessionsSynced(unsyncedSessions.map { it.id })
            }

            Result.success()

        } catch (e: Exception) {
            // Result.retry() — WorkManager will retry with the exponential
            // backoff configured in schedule(). Rows stay unsynced until success.
            Result.retry()
        }
    }
}