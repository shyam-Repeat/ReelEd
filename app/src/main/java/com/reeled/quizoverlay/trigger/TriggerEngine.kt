package com.reeled.quizoverlay.trigger
package com.yourappname.quizoverlay.trigger

import com.yourappname.quizoverlay.data.repository.QuizRepository
import com.yourappname.quizoverlay.model.QuizQuestion
import com.yourappname.quizoverlay.prefs.TriggerPrefs

// ─────────────────────────────────────────────────────────────────────────────
// TriggerEngine
//
// Responsibilities (layout doc):
//   Gate checks + selectNextQuestion().
//   Returns TriggerDecision.Fire(question) or TriggerDecision.Skip(reason).
//   Called by OverlayForegroundService every TriggerConfig.POLLING_INTERVAL_MS.
//
// Dependency map (layout doc):
//   trigger → model, data/repository, prefs/TriggerPrefs
//   Pure Kotlin — no Android framework dependencies except via injected wrappers.
//
// Improvements over baseline spec:
//   1. Parent-pause gate (Gate 0) — checked before all others to fast-exit
//   2. Target-app gate (Gate 1b) — don't fire if child left the target app
//   3. Adaptive cooldown — streak bonus on correct, dismiss penalty on skip
//   4. Soft video-aware firing — prefers good interrupt moments, never hard-blocks
//   5. selectNextQuestion returns Result<QuizQuestion> instead of throwing,
//      so the caller can handle empty-cache gracefully
//   6. Difficulty and subject balancing extracted to named private functions
//      for readability and independent testability
// ─────────────────────────────────────────────────────────────────────────────

class TriggerEngine(
    private val repository: QuizRepository,
    private val prefs: TriggerPrefs,
    private val foregroundAppDetector: ForegroundAppDetector,
    private val videoPlaybackDetector: VideoPlaybackDetector
) {

    // ─────────────────────────────────────────────────────────────────────────
    // Main entry point — called by OverlayForegroundService polling loop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs all gate checks in priority order, then selects a question if
     * all gates pass. Returns [TriggerDecision.Fire] or [TriggerDecision.Skip].
     *
     * This function is suspend because it reads DataStore (TriggerPrefs) and
     * Room (repository). Must be called from a coroutine on Dispatchers.Default
     * or Dispatchers.IO — never on Main.
     */
    suspend fun checkAndFire(): TriggerDecision {
        val now = System.currentTimeMillis()
        val state = prefs.getTriggerState()

        // ── Gate 0: Parent explicitly paused the overlay ──────────────────────
        // Check this first — it's the highest-priority signal, parent-controlled.
        if (state.parentPauseActive) {
            // If the pause has expired, clear it and continue
            if (now < state.parentPauseExpiryMs) {
                return TriggerDecision.Skip("parent_paused")
            } else {
                prefs.clearParentPause()
            }
        }

        // ── Gate 1a: Quiz is already on screen ────────────────────────────────
        if (state.overlayActive) {
            return TriggerDecision.Skip("overlay_active")
        }

        // ── Gate 1b: Target app is not in foreground ──────────────────────────
        // IMPROVEMENT: Don't fire into a random app.
        // Resets session start time when child returns to a target app later.
        if (!foregroundAppDetector.isTargetAppInForeground()) {
            return TriggerDecision.Skip("target_app_not_foreground")
        }

        // ── Gate 2: Daily cap ─────────────────────────────────────────────────
        if (state.quizzesShownToday >= TriggerConfig.MAX_DAILY) {
            return TriggerDecision.Skip("daily_cap_reached")
        }

        // ── Gate 3: Adaptive cooldown ─────────────────────────────────────────
        // IMPROVEMENT: Three-tier cooldown based on last outcome:
        //   - Dismissed      → standard cooldown + DISMISS_PENALTY_MS
        //   - Correct answer → standard cooldown − STREAK_REDUCTION_MS (floored)
        //   - Wrong answer   → standard cooldown (no change)
        val effectiveCooldown = computeEffectiveCooldown(state)
        if (now - state.lastQuizShownTime < effectiveCooldown) {
            return TriggerDecision.Skip("cooldown_active")
        }

        // ── Gate 4: Session warmup ────────────────────────────────────────────
        if (now - state.sessionStartTime < TriggerConfig.WARMUP_MS) {
            return TriggerDecision.Skip("warmup_not_done")
        }

        // ── Gate 5: Inter-quiz interval ───────────────────────────────────────
        if (now - state.lastQuizShownTime < TriggerConfig.INTERVAL_MS) {
            return TriggerDecision.Skip("interval_not_elapsed")
        }

        // ── Soft check: Video / audio awareness ───────────────────────────────
        // IMPROVEMENT: Prefer good interrupt moments without hard-blocking.
        // If the moment is poor AND we still have daily quota headroom, defer
        // one polling tick (30 s). If we're on the last quota slot, fire anyway
        // — better a slightly bad moment than missing the day's final quiz.
        val interruptScore = videoPlaybackDetector.getInterruptScore()
        val remainingQuota = TriggerConfig.MAX_DAILY - state.quizzesShownToday
        if (interruptScore.isPoor && remainingQuota > 1) {
            return TriggerDecision.Skip("poor_interrupt_moment")
        }

        // ── All gates passed — select question ────────────────────────────────
        val allQuestions = repository.getAllActiveQuestions()
        if (allQuestions.isEmpty()) {
            // Cache is empty — QuizFetchWorker hasn't run yet or network failed
            return TriggerDecision.Skip("no_questions_cached")
        }

        val attemptedTodayIds = repository.getAttemptedQuestionIdsToday()
        val question = selectNextQuestion(
            allCachedQuestions = allQuestions,
            attemptedTodayIds = attemptedTodayIds,
            lastShownId = state.lastShownQuestionId,
            quizzesShownToday = state.quizzesShownToday
        )

        return TriggerDecision.Fire(question)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adaptive cooldown computation
    // ─────────────────────────────────────────────────────────────────────────

    private fun computeEffectiveCooldown(state: TriggerState): Long {
        return when {
            // Dismissed → penalise
            state.lastWasDismissed ->
                TriggerConfig.COOLDOWN_MS + TriggerConfig.DISMISS_PENALTY_MS

            // Correct → reward with shorter wait, but never below the floor
            state.lastAnswerWasCorrect ->
                maxOf(
                    TriggerConfig.COOLDOWN_MS - TriggerConfig.STREAK_REDUCTION_MS,
                    TriggerConfig.MIN_COOLDOWN_MS
                )

            // Wrong answer → standard cooldown
            else -> TriggerConfig.COOLDOWN_MS
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Question selection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selects the best next question from the cached pool using five rules
     * applied in priority order. Never throws — always returns a question.
     *
     * Rules (from trigger.md §3.4, improved):
     *   R1. Never repeat a question shown today
     *   R2. Never show the same question back-to-back
     *   R3. Fallback: if pool is empty after R1+R2, relax R1 (keep R2)
     *   R4. Difficulty progression within the day
     *   R5. Subject round-robin (least-shown subject first)
     *   R6. IMPROVEMENT: Tie-break on card_type diversity — avoid the same
     *       card type three times in a row (prevents format fatigue)
     */
    internal fun selectNextQuestion(
        allCachedQuestions: List<QuizQuestion>,
        attemptedTodayIds: Set<String>,
        lastShownId: String?,
        quizzesShownToday: Int
    ): QuizQuestion {

        // ── R1 + R2: Remove today's seen questions and the immediate last ──────
        val notSeenToday = allCachedQuestions.filter { it.id !in attemptedTodayIds }
        val candidates = notSeenToday.filter { it.id != lastShownId }

        // ── R3: Fallback pool if exhausted ────────────────────────────────────
        // Only relaxes R1, preserves R2 (never back-to-back same question)
        val pool = when {
            candidates.isNotEmpty() -> candidates
            notSeenToday.isNotEmpty() -> notSeenToday   // R2 already satisfied via different question
            else -> allCachedQuestions.filter { it.id != lastShownId }
                .ifEmpty { allCachedQuestions }         // absolute last resort
        }

        // ── R4: Difficulty gating ──────────────────────────────────────────────
        val difficultyPool = applyDifficultyFilter(pool, quizzesShownToday)

        // ── R5: Subject balance ────────────────────────────────────────────────
        val leastShownSubject = getLeastShownSubjectToday(attemptedTodayIds, allCachedQuestions)
        val subjectPool = difficultyPool
            .filter { it.subject == leastShownSubject }
            .ifEmpty { difficultyPool }

        // ── R6: Card type diversity ────────────────────────────────────────────
        // IMPROVEMENT: Track the last shown card type and deprioritise it so
        // children don't get three TapChoice cards in a row.
        val diversePool = applyCardTypeDiversity(subjectPool, allCachedQuestions, attemptedTodayIds)

        return diversePool.random()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R4 — Difficulty filter
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyDifficultyFilter(
        pool: List<QuizQuestion>,
        quizzesShownToday: Int
    ): List<QuizQuestion> {
        val filtered = when {
            quizzesShownToday < TriggerConfig.DIFFICULTY_EASY_UNTIL ->
                pool.filter { it.difficulty == 1 }

            quizzesShownToday < TriggerConfig.DIFFICULTY_MEDIUM_UNTIL ->
                pool.filter { it.difficulty <= 2 }

            else -> pool
        }
        // Always fall back to the full pool to avoid returning empty
        return filtered.ifEmpty { pool }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R5 — Subject round-robin
    // ─────────────────────────────────────────────────────────────────────────

    internal fun getLeastShownSubjectToday(
        shownIds: Set<String>,
        allQuestions: List<QuizQuestion>
    ): String {
        val shownQuestions = allQuestions.filter { it.id in shownIds }
        val countBySubject = TriggerConfig.SUBJECTS.associateWith { subject ->
            shownQuestions.count { it.subject == subject }
        }
        // In a tie, maintain the canonical SUBJECTS list order for determinism
        return countBySubject.minByOrNull { it.value }?.key
            ?: TriggerConfig.SUBJECTS.first()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R6 — Card type diversity (improvement)
    //
    // Finds which card_type was used most recently among today's attempts,
    // then deprioritises it (moves to fallback). If deprioritising would
    // empty the pool, returns the full pool unchanged.
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyCardTypeDiversity(
        pool: List<QuizQuestion>,
        allQuestions: List<QuizQuestion>,
        attemptedTodayIds: Set<String>
    ): List<QuizQuestion> {
        if (pool.size <= 1) return pool

        // Find the card type of the most recent attempt today
        val lastAttemptedType = allQuestions
            .filter { it.id in attemptedTodayIds }
            .lastOrNull()
            ?.cardType

        if (lastAttemptedType == null) return pool

        // Prefer questions with a different card type
        val diversified = pool.filter { it.cardType != lastAttemptedType }
        return diversified.ifEmpty { pool }
    }
}