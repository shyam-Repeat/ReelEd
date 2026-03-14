# SECTION 5: FOREGROUND & BACKGROUND DATA SERVICES
## Minimal Usage, Local-First, Smart Sync

---

### 5.1 DESIGN PRINCIPLES

- Foreground Service is ALWAYS minimal — does only what's needed
- Network calls = rare, batched, WiFi-preferred
- Room = source of truth for all quiz and attempt data
- Supabase = backup + analysis layer, not real-time
- DataStore = fast lightweight flags, NOT quiz data

---

### 5.2 FOREGROUND SERVICE — OverlayForegroundService

```kotlin
class OverlayForegroundService : Service() {

    // WHAT THIS SERVICE DOES:
    // 1. Shows persistent notification (required by Android for foreground)
    // 2. Runs a polling loop (coroutine) every 30 seconds
    // 3. Calls TriggerEngine.checkAndFire() on each tick
    // 4. If TriggerDecision.Fire: draws overlay via WindowManager
    // 5. Listens for QuizAttemptResult and writes to Room
    // 6. Updates DataStore trigger state (lastQuizShownTime, etc.)

    // WHAT THIS SERVICE DOES NOT DO:
    // - No network calls (that is the SyncWorker's job)
    // - No heavy Room queries on main thread (use IO dispatcher)
    // - No GPS or other sensors
    // - No audio or camera

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        startPollingLoop()
        return START_STICKY  // restart if killed, important for overlay continuity
    }

    private fun startPollingLoop() {
        pollingJob = serviceScope.launch {
            while (isActive) {
                val decision = triggerEngine.checkAndFire()
                if (decision is TriggerDecision.Fire) {
                    withContext(Dispatchers.Main) {
                        showOverlay(decision.question)
                    }
                }
                delay(TriggerConfig.POLLING_INTERVAL_MS)
            }
        }
    }

    private fun showOverlay(question: QuizQuestion) {
        // 1. Inflate ComposeView with QuizCardRouter
        // 2. Add to WindowManager with params from Section 1.4
        // 3. Set onResult callback → onQuizResult()
        // 4. Update DataStore: overlayActive = true
    }

    private fun onQuizResult(result: QuizAttemptResult) {
        serviceScope.launch(Dispatchers.IO) {
            // 1. Remove overlay view from WindowManager (main thread)
            // 2. Write result to Room quiz_attempts table
            // 3. Write event log to Room event_logs table
            // 4. Update DataStore trigger state
            // 5. Mark result as unsynced (synced = false in Room row)
            // Note: DO NOT call Supabase here. That is SyncWorker's job.
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        removeOverlayIfShowing()
        super.onDestroy()
    }
}
```

Notification (required for foreground service):
```kotlin
fun buildNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Learning mode active")
        .setContentText("Quiz overlay is running")
        .setSmallIcon(R.drawable.ic_brain)
        .setPriority(NotificationCompat.PRIORITY_LOW)  // LOW = no sound, minimal UI
        .setOngoing(true)
        .build()
}
// Use PRIORITY_LOW — HIGH or DEFAULT would alert the child on every quiz
```

---

### 5.3 BACKGROUND SYNC — SyncWorker

```kotlin
class SyncWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    // WHAT THIS WORKER DOES:
    // 1. Reads all unsynced quiz_attempts from Room (synced = false)
    // 2. Reads all unsynced event_logs from Room (synced = false)
    // 3. Batch POSTs both to Supabase REST API via Retrofit
    // 4. On success: marks rows as synced = true in Room
    // 5. On failure: logs error, leaves rows unsynced (will retry next run)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val unsyncedAttempts = roomDao.getUnsyncedAttempts()
                val unsyncedEvents = roomDao.getUnsyncedEvents()

                if (unsyncedAttempts.isEmpty() && unsyncedEvents.isEmpty())
                    return@withContext Result.success()

                // Batch upsert to Supabase
                supabaseApi.batchInsertAttempts(unsyncedAttempts.map { it.toDto() })
                supabaseApi.batchInsertEvents(unsyncedEvents.map { it.toDto() })

                // Mark synced in Room
                roomDao.markAttemptsSynced(unsyncedAttempts.map { it.id })
                roomDao.markEventsSynced(unsyncedEvents.map { it.id })

                Result.success()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Result.retry()  // WorkManager will retry with backoff
            }
        }
    }
}
```

SyncWorker scheduling:
```kotlin
fun scheduleSyncWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // any network, not WiFi-only
        // WiFi-only would block sync on mobile data → bad for testers
        .build()

    val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
        repeatInterval = 30, TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "quiz_sync",
        ExistingPeriodicWorkPolicy.KEEP,  // don't reset if already scheduled
        syncRequest
    )
}
```

---

### 5.4 QUIZ QUESTION DOWNLOAD — QuizFetchWorker

```kotlin
class QuizFetchWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    // WHAT THIS WORKER DOES:
    // 1. Checks how many active questions are in Room
    // 2. If count >= MIN_CACHE_SIZE: skip (cache is healthy)
    // 3. If count < MIN_CACHE_SIZE: fetch from Supabase
    // 4. Upsert fetched questions into Room (INSERT OR REPLACE)
    // 5. Runs once on app open + once daily

    companion object {
        val MIN_CACHE_SIZE = 30   // always have at least 30 questions cached
        val MAX_FETCH = 100       // never fetch more than 100 at a time
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val cachedCount = roomDao.getActiveQuestionCount()
                if (cachedCount >= MIN_CACHE_SIZE) return@withContext Result.success()

                val questions = supabaseApi.fetchActiveQuestions(limit = MAX_FETCH)
                roomDao.upsertQuestions(questions.map { it.toEntity() })

                Result.success()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Result.retry()
            }
        }
    }
}
```

Trigger QuizFetchWorker:
```kotlin
// On app open (MainActivity.onCreate):
WorkManager.getInstance(this).enqueue(
    OneTimeWorkRequestBuilder<QuizFetchWorker>()
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build()
)

// Also daily:
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "quiz_fetch",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<QuizFetchWorker>(24, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build()
)
```

---

### 5.5 ROOM DATABASE — TABLE DEFINITIONS

```kotlin
@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val cardType: String,           // TAP_CHOICE | TAP_TAP_MATCH | DRAG_DROP_MATCH | FILL_BLANK
    val subject: String,            // math | english | science | general
    val difficulty: Int,            // 1 | 2 | 3
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?,
    val payloadJson: String,        // full payload as JSON string (parsed at render time)
    val timerSeconds: Int,
    val strictMode: Boolean,
    val active: Boolean,
    val fetchedAt: Long             // epoch ms — for cache freshness check
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,     // UUID generated locally
    val testerId: String,
    val questionId: String,
    val shownAt: Long,
    val answeredAt: Long?,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String,
    val dismissed: Boolean,
    val synced: Boolean = false     // false until SyncWorker pushes to Supabase
)

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey val id: String,
    val testerId: String,
    val eventType: String,          // quiz_shown | quiz_answered | quiz_dismissed |
                                    // overlay_started | overlay_stopped | app_opened
    val payloadJson: String,
    val createdAt: Long,
    val synced: Boolean = false
)
```

---

### 5.6 DATA FLOW SUMMARY

```
QUIZ QUESTION FLOW (down):
Supabase quiz_questions
    ↓  QuizFetchWorker (on open + daily, network-gated)
Room quiz_questions (local cache, MIN 30 questions)
    ↓  TriggerEngine.selectNextQuestion()
OverlayForegroundService → WindowManager → Quiz Card UI

ATTEMPT DATA FLOW (up):
Quiz Card UI → onResult callback
    ↓  OverlayForegroundService (immediate, no network)
Room quiz_attempts (synced = false)
Room event_logs (synced = false)
    ↓  SyncWorker (every 30 min, network-gated, batched)
Supabase quiz_attempts
Supabase event_logs

NETWORK BUDGET PER SESSION:
  Quiz fetch   : ~50KB  (100 questions × ~500 bytes JSON)
  Attempt sync : ~2KB   (6 attempts × ~300 bytes each)
  Event sync   : ~3KB   (20 events × ~150 bytes each)
  Total        : ~55KB per session — extremely low
```

---

### 5.7 WHAT NEVER GOES OVER THE NETWORK IN MVP

```
NEVER sent to Supabase:
  - Child's name or any PII
  - Device ID or IMEI
  - Location data
  - Screen recordings or screenshots
  - Content of what the child was viewing in Instagram

ONLY sent to Supabase:
  - tester_id (anonymous UUID generated at install)
  - quiz attempt results (question_id, selected_option, correct, time)
  - event type logs (overlay_started, quiz_shown, etc.)
  - app version + overlay_permission_granted flag
```

---