package com.reeled.quizoverlay.trigger

import android.util.Log
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.model.QuizQuestion
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.util.PermissionChecker

class TriggerEngine(
    private val repository: QuizRepository,
    private val prefs: TriggerPrefs,
    private val foregroundAppDetector: ForegroundAppDetector,
    private val videoPlaybackDetector: VideoPlaybackDetector
) {

    private val TAG = "TriggerEngine"

    suspend fun checkAndFire(monitoredApps: Set<String>): TriggerDecision {
        val now = System.currentTimeMillis()
        val state = prefs.getTriggerState()
        val dailyCap = prefs.getDailyCap()

        if (state.parentPauseActive) {
            if (now < state.parentPauseExpiryMs) {
                return skip("parent_paused")
            } else {
                prefs.clearParentPause()
            }
        }

        if (state.overlayActive) {
            return skip("overlay_active")
        }

        val foregroundPackage = foregroundAppDetector.getCurrentForegroundPackage()
        val allTargets = if (monitoredApps.isNotEmpty()) monitoredApps else ForegroundAppDetector.TARGET_PACKAGES
        
        if (foregroundPackage == null || !allTargets.contains(foregroundPackage)) {
            prefs.clearActiveSession()
            return skip("target_app_not_foreground", foregroundPackage)
        }

        prefs.updateSessionIfNeeded(foregroundPackage)

        val appPauseExpiryMs = prefs.getAppPauseExpiry(foregroundPackage)
        if (appPauseExpiryMs > now) {
            val remaining = (appPauseExpiryMs - now) / 1000
            return skip("app_paused (${remaining}s)", foregroundPackage)
        }

        if (state.quizzesShownToday >= dailyCap) {
            return skip("daily_cap_reached", foregroundPackage)
        }

        val effectiveCooldown = computeEffectiveCooldown(state)
        val timeSinceLast = now - state.lastQuizShownTime
        if (timeSinceLast < effectiveCooldown) {
            val remaining = (effectiveCooldown - timeSinceLast) / 1000
            return skip("cooldown_active (${remaining}s)", foregroundPackage)
        }

        val timeSinceSessionStart = now - state.sessionStartTime
        if (timeSinceSessionStart < TriggerConfig.WARMUP_MS) {
            val remaining = (TriggerConfig.WARMUP_MS - timeSinceSessionStart) / 1000
            return skip("warmup_not_done (${remaining}s)", foregroundPackage)
        }

        if (timeSinceLast < TriggerConfig.INTERVAL_MS) {
            val remaining = (TriggerConfig.INTERVAL_MS - timeSinceLast) / 1000
            return skip("interval_not_elapsed (${remaining}s)", foregroundPackage)
        }

        val interruptScore = videoPlaybackDetector.getInterruptScore(monitoredApps)
        val remainingQuota = dailyCap - state.quizzesShownToday
        if (interruptScore.isPoor && remainingQuota > 1) {
            return skip("poor_interrupt_moment", foregroundPackage)
        }

        val allQuestions = repository.getAllActiveQuestions()
        if (allQuestions.isEmpty()) {
            return skip("no_questions_cached", foregroundPackage)
        }

        val attemptedTodayIds = repository.getAttemptedQuestionIdsToday().toSet()
        val question = selectNextQuestion(
            allCachedQuestions = allQuestions,
            attemptedTodayIds = attemptedTodayIds,
            lastShownId = state.lastShownQuestionId,
            quizzesShownToday = state.quizzesShownToday
        )

        Log.d(TAG, "FIRE: Quiz for $foregroundPackage")
        return TriggerDecision.Fire(question, foregroundPackage)
    }

    private suspend fun skip(reason: String, foregroundPackage: String? = null): TriggerDecision {
        val overlayPerm = PermissionChecker.hasOverlayPermission(repository.context)
        val usagePerm = PermissionChecker.hasUsageStatsPermission(repository.context)
        Log.d(TAG, "SKIP: $reason | app: $foregroundPackage | perm: O=$overlayPerm, U=$usagePerm")
        
        // Enhance local event log for debugging in Dev Mode
        val payload = "{\"reason\":\"${jsonSafe(reason)}\",\"app\":\"${jsonSafe(foregroundPackage.orEmpty())}\",\"perm_overlay\":$overlayPerm,\"perm_usage\":$usagePerm}"
        prefs.setLastSkipReason(reason)
        
        // try {
        //     repository.logEvent("trigger_skip_debug", payload)
        // } catch (_: Exception) {}
        
        return TriggerDecision.Skip(reason)
    }

    private fun jsonSafe(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun computeEffectiveCooldown(state: TriggerState): Long {
        var cooldown = TriggerConfig.COOLDOWN_MS
        if (state.lastWasDismissed) {
            // 1m 30s base + 3m penalty = 4m 30s total wait
            cooldown += TriggerConfig.DISMISS_PENALTY_MS
        } else if (state.lastWasCorrect) {
            // 1m 30s base + 30s extra = 2m total wait
            cooldown += TriggerConfig.CORRECT_ADDITIONAL_MS
        }
        // Base Wrong answer (neither dismissed nor correct) = 1m 30s
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
