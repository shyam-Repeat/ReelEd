package com.reeled.quizoverlay.trigger

import com.reeled.quizoverlay.data.model.QuizQuestion
import com.reeled.quizoverlay.data.model.TriggerConfig
import com.reeled.quizoverlay.data.model.TriggerDecision
import com.reeled.quizoverlay.data.prefs.OverlayPrefs
import com.reeled.quizoverlay.data.repository.QuizRepository
import kotlin.random.Random

class TriggerEngine(
    private val prefs: OverlayPrefs,
    private val repository: QuizRepository
) {
    suspend fun checkAndFire(now: Long = System.currentTimeMillis()): TriggerDecision {
        prefs.clearExpiredParentPause(now)
        val state = prefs.getTriggerState()

        if (state.parentPauseActive && now < state.parentPauseEndsAt) {
            return TriggerDecision.Skip("parent_pause_active")
        }
        if (state.overlayActive) return TriggerDecision.Skip("overlay_active")
        if (state.quizzesShownToday >= TriggerConfig.MAX_DAILY) {
            return TriggerDecision.Skip("daily_cap_reached")
        }

        val cooldown = if (state.lastWasDismissed) {
            TriggerConfig.COOLDOWN_MS + TriggerConfig.DISMISS_PENALTY_MS
        } else {
            TriggerConfig.COOLDOWN_MS
        }

        if (now - state.lastQuizShownTime < cooldown) {
            return TriggerDecision.Skip("cooldown_active")
        }
        if (now - state.sessionStartTime < TriggerConfig.WARMUP_MS) {
            return TriggerDecision.Skip("warmup_not_done")
        }
        if (now - state.lastQuizShownTime < TriggerConfig.INTERVAL_MS) {
            return TriggerDecision.Skip("interval_not_elapsed")
        }

        val questions = repository.getActiveQuestions()
        if (questions.isEmpty()) return TriggerDecision.Skip("no_cached_questions")

        val startOfDay = now - (now % 86_400_000)
        val attemptedToday = repository.getAttemptedIdsToday(startOfDay)

        val question = selectNextQuestion(questions, attemptedToday, state.lastShownQuestionId)
        return TriggerDecision.Fire(question)
    }

    fun selectNextQuestion(
        allCachedQuestions: List<QuizQuestion>,
        attemptedTodayIds: Set<String>,
        lastShownId: String?
    ): QuizQuestion {
        val notSeenToday = allCachedQuestions.filter { it.id !in attemptedTodayIds }
        val candidates = notSeenToday.filter { it.id != lastShownId }
        val pool = candidates.ifEmpty { allCachedQuestions.filter { it.id != lastShownId } }

        val todayCount = attemptedTodayIds.size
        val difficultyPool = when {
            todayCount < 2 -> pool.filter { it.difficulty == 1 }
            todayCount < 4 -> pool.filter { it.difficulty <= 2 }
            else -> pool
        }.ifEmpty { pool }

        val leastShownSubject = getLeastShownSubjectToday(attemptedTodayIds, allCachedQuestions)
        val subjectPool = difficultyPool.filter { it.subject == leastShownSubject }.ifEmpty { difficultyPool }

        return subjectPool[Random.nextInt(subjectPool.size)]
    }

    fun getLeastShownSubjectToday(shownIds: Set<String>, allQuestions: List<QuizQuestion>): String {
        val subjects = listOf("math", "english", "science", "general")
        val shownQuestions = allQuestions.filter { it.id in shownIds }
        val countBySubject = subjects.associateWith { subject ->
            shownQuestions.count { it.subject == subject }
        }
        return countBySubject.minByOrNull { it.value }?.key ?: subjects.random()
    }
}
