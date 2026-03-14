# SECTION 3: TRIGGER LOGIC ALGORITHM
## No AI, Rule-Based, Designed for Learning Signal

---

### 3.1 DESIGN PRINCIPLES

- Trigger often enough to gather data across 10 testers
- Not so often that parents disable the app in day 1
- Respect the child's session — mid-scroll interruption
  is the point, but catastrophic interruption kills trust
- All state stored in DataStore (lightweight, no Room needed)

---

### 3.2 TRIGGER ALGORITHM — COMPLETE RULES

```
TRIGGER ENGINE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

INPUT SIGNALS (all tracked by Foreground Service):
  - session_start_time     : when child opened target app
  - last_quiz_shown_time   : epoch ms of last quiz
  - quizzes_shown_today    : count, resets at midnight
  - last_answer_was_correct: boolean
  - overlay_active         : boolean (quiz on screen now)

HARD GATES (check first — if ANY is true, do NOT trigger):
  1. overlay_active == true            (already showing quiz)
  2. quizzes_shown_today >= MAX_DAILY  (daily cap reached)
  3. now - last_quiz_shown_time < COOLDOWN_MS (cooldown active)
  4. now - session_start_time < WARMUP_MS     (too soon in session)

IF all gates pass → check TRIGGER CONDITION:
  5. now - session_start_time >= TRIGGER_AFTER_MS
     AND
     now - last_quiz_shown_time >= INTERVAL_MS

IF condition met → FIRE QUIZ

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 3.3 TIMING CONSTANTS (MVP VALUES)

These are tuned for 10-tester learning signal collection.
Adjust after week 1 based on dismiss rate data.

```kotlin
object TriggerConfig {

    // How long child must be in app before FIRST quiz fires
    val WARMUP_MS = 3 * 60 * 1000L          // 3 minutes

    // Minimum time between any two quizzes
    val COOLDOWN_MS = 12 * 60 * 1000L       // 12 minutes

    // How long into a session before quiz is eligible again
    val INTERVAL_MS = 10 * 60 * 1000L       // 10 minutes

    // Max quizzes per day per child (resets at midnight)
    val MAX_DAILY = 6

    // How often the Foreground Service checks trigger conditions
    val POLLING_INTERVAL_MS = 30 * 1000L    // every 30 seconds

    // If child dismissed last quiz, extend cooldown by this
    val DISMISS_PENALTY_MS = 8 * 60 * 1000L // 8 extra minutes

}
```

Rationale:
- 3-min warmup: child settles into app, less hostile to interruption
- 12-min cooldown: parent-friendly, feels like ad breaks on TV
- Max 6/day: at 12-min intervals, max possible = 6 in ~72 min session
- Dismiss penalty: if child dismissed, wait longer before retrying

---

### 3.4 QUESTION SELECTION ALGORITHM

```kotlin
fun selectNextQuestion(
    allCachedQuestions: List<QuizQuestion>,
    attemptedTodayIds: Set<String>,
    lastShownId: String?
): QuizQuestion {

    // Rule 1: Never repeat a question shown today
    val notSeenToday = allCachedQuestions.filter {
        it.id !in attemptedTodayIds
    }

    // Rule 2: Never show the same question back-to-back
    val candidates = notSeenToday.filter { it.id != lastShownId }

    // Rule 3: If no unseen questions remain, reset pool
    //         (only happens if MAX_DAILY > question pool size, unlikely)
    val pool = candidates.ifEmpty { allCachedQuestions.filter { it.id != lastShownId } }

    // Rule 4: Difficulty progression within a day
    //   - First 2 quizzes of day → difficulty == 1 (easy)
    //   - Quiz 3-4              → difficulty == 1 or 2
    //   - Quiz 5+               → any difficulty
    val todayCount = attemptedTodayIds.size
    val difficultyPool = when {
        todayCount < 2  -> pool.filter { it.difficulty == 1 }
        todayCount < 4  -> pool.filter { it.difficulty <= 2 }
        else            -> pool
    }.ifEmpty { pool }

    // Rule 5: Balance subjects across the day
    //   Pick the subject that has been shown LEAST today
    //   Simple round-robin across [math, english, general, science]
    val leastShownSubject = getLeastShownSubjectToday(attemptedTodayIds, allCachedQuestions)
    val subjectFiltered = difficultyPool.filter { it.subject == leastShownSubject }
        .ifEmpty { difficultyPool }

    // Final: random pick from filtered pool
    return subjectFiltered.random()
}
```

```kotlin
fun getLeastShownSubjectToday(
    shownIds: Set<String>,
    allQuestions: List<QuizQuestion>
): String {
    val subjects = listOf("math", "english", "science", "general")
    val shownQuestions = allQuestions.filter { it.id in shownIds }
    val countBySubject = subjects.associateWith { subject ->
        shownQuestions.count { it.subject == subject }
    }
    return countBySubject.minByOrNull { it.value }?.key ?: subjects.random()
}
```

---

### 3.5 TRIGGER CHECKER — SERVICE LOOP

```kotlin
class TriggerEngine(
    private val prefs: TriggerPrefs,    // DataStore wrapper
    private val roomDao: QuizDao
) {

    // Called by OverlayForegroundService every POLLING_INTERVAL_MS
    suspend fun checkAndFire(): TriggerDecision {

        val now = System.currentTimeMillis()
        val state = prefs.getTriggerState() // reads DataStore

        // Gate 1: Already showing
        if (state.overlayActive) return TriggerDecision.Skip("overlay_active")

        // Gate 2: Daily cap
        if (state.quizzesShownToday >= TriggerConfig.MAX_DAILY)
            return TriggerDecision.Skip("daily_cap_reached")

        // Gate 3: Cooldown (including dismiss penalty if applicable)
        val effectiveCooldown = if (state.lastWasDismissed)
            TriggerConfig.COOLDOWN_MS + TriggerConfig.DISMISS_PENALTY_MS
        else TriggerConfig.COOLDOWN_MS
        if (now - state.lastQuizShownTime < effectiveCooldown)
            return TriggerDecision.Skip("cooldown_active")

        // Gate 4: Session warmup
        if (now - state.sessionStartTime < TriggerConfig.WARMUP_MS)
            return TriggerDecision.Skip("warmup_not_done")

        // Gate 5: Interval since last quiz
        if (now - state.lastQuizShownTime < TriggerConfig.INTERVAL_MS)
            return TriggerDecision.Skip("interval_not_elapsed")

        // All gates passed — select question
        val question = selectNextQuestion(
            allCachedQuestions = roomDao.getAllActive(),
            attemptedTodayIds = roomDao.getAttemptedIdsToday(),
            lastShownId = state.lastShownQuestionId
        )

        return TriggerDecision.Fire(question)
    }
}

sealed class TriggerDecision {
    data class Fire(val question: QuizQuestion) : TriggerDecision()
    data class Skip(val reason: String) : TriggerDecision()
}
```

---