package com.reeled.quizoverlay.trigger

import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.model.QuizQuestion
import com.reeled.quizoverlay.prefs.TriggerPrefs

class TriggerEngine(
    private val repository: QuizRepository,
    private val prefs: TriggerPrefs,
    private val foregroundAppDetector: ForegroundAppDetector,
    private val videoPlaybackDetector: VideoPlaybackDetector
) {

    suspend fun checkAndFire(): TriggerDecision {
        val now = System.currentTimeMillis()
        val state = prefs.getTriggerState()

        if (state.parentPauseActive) {
            if (now < state.parentPauseExpiryMs) {
                return TriggerDecision.Skip("parent_paused")
            } else {
                prefs.clearParentPause()
            }
        }

        if (state.overlayActive) {
            return TriggerDecision.Skip("overlay_active")
        }

        val foregroundPackage = foregroundAppDetector.getCurrentForegroundPackage()
        if (foregroundPackage == null || !ForegroundAppDetector.TARGET_PACKAGES.contains(foregroundPackage)) {
            prefs.clearActiveSession()
            return TriggerDecision.Skip("target_app_not_foreground")
        }

        prefs.updateSessionIfNeeded(foregroundPackage)

        if (state.quizzesShownToday >= TriggerConfig.MAX_DAILY) {
            return TriggerDecision.Skip("daily_cap_reached")
        }

        val effectiveCooldown = computeEffectiveCooldown(state)
        if (now - state.lastQuizShownTime < effectiveCooldown) {
            return TriggerDecision.Skip("cooldown_active")
        }

        if (now - state.sessionStartTime < TriggerConfig.WARMUP_MS) {
            return TriggerDecision.Skip("warmup_not_done")
        }

        if (now - state.lastQuizShownTime < TriggerConfig.INTERVAL_MS) {
            return TriggerDecision.Skip("interval_not_elapsed")
        }

        val interruptScore = videoPlaybackDetector.getInterruptScore()
        val remainingQuota = TriggerConfig.MAX_DAILY - state.quizzesShownToday
        if (interruptScore.isPoor && remainingQuota > 1) {
            return TriggerDecision.Skip("poor_interrupt_moment")
        }

        val allQuestions = repository.getAllActiveQuestions()
        if (allQuestions.isEmpty()) {
            return TriggerDecision.Skip("no_questions_cached")
        }

        val attemptedTodayIds = repository.getAttemptedQuestionIdsToday().toSet()
        val question = selectNextQuestion(
            allCachedQuestions = allQuestions,
            attemptedTodayIds = attemptedTodayIds,
            lastShownId = state.lastShownQuestionId,
            quizzesShownToday = state.quizzesShownToday
        )

        return TriggerDecision.Fire(question, foregroundPackage)
    }

    private fun computeEffectiveCooldown(state: TriggerState): Long {
        var cooldown = TriggerConfig.COOLDOWN_MS
        if (state.lastWasDismissed) {
            cooldown += TriggerConfig.DISMISS_PENALTY_MS
        } else if (state.lastWasCorrect) {
            cooldown -= TriggerConfig.STREAK_REDUCTION_MS
        }
        return cooldown.coerceAtLeast(TriggerConfig.MIN_COOLDOWN_MS)
    }

    internal fun selectNextQuestion(
        allCachedQuestions: List<QuizQuestion>,
        attemptedTodayIds: Set<String>,
        lastShownId: String?,
        quizzesShownToday: Int
    ): QuizQuestion {
        val notSeenToday = allCachedQuestions.filter { it.id !in attemptedTodayIds }
        val candidates = notSeenToday.filter { it.id != lastShownId }

        val pool = when {
            candidates.isNotEmpty() -> candidates
            notSeenToday.isNotEmpty() -> notSeenToday
            else -> allCachedQuestions.filter { it.id != lastShownId }
                .ifEmpty { allCachedQuestions }
        }

        val difficultyPool = applyDifficultyFilter(pool, quizzesShownToday)

        val leastShownSubject = getLeastShownSubjectToday(attemptedTodayIds, allCachedQuestions)
        val subjectPool = difficultyPool
            .filter { it.subject == leastShownSubject }
            .ifEmpty { difficultyPool }

        val diversePool = applyCardTypeDiversity(subjectPool, allCachedQuestions, attemptedTodayIds)

        return diversePool.random()
    }

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
        return filtered.ifEmpty { pool }
    }

    internal fun getLeastShownSubjectToday(
        shownIds: Set<String>,
        allQuestions: List<QuizQuestion>
    ): String {
        val shownQuestions = allQuestions.filter { it.id in shownIds }
        val countBySubject = TriggerConfig.SUBJECTS.associateWith { subject ->
            shownQuestions.count { it.subject == subject }
        }
        return countBySubject.minByOrNull { it.value }?.key
            ?: TriggerConfig.SUBJECTS.first()
    }

    private fun applyCardTypeDiversity(
        pool: List<QuizQuestion>,
        allQuestions: List<QuizQuestion>,
        attemptedTodayIds: Set<String>
    ): List<QuizQuestion> {
        if (pool.size <= 1) return pool

        val lastAttemptedType = allQuestions
            .filter { it.id in attemptedTodayIds }
            .lastOrNull()
            ?.cardType

        if (lastAttemptedType == null) return pool

        val diversified = pool.filter { it.cardType != lastAttemptedType }
        return diversified.ifEmpty { pool }
    }
}
