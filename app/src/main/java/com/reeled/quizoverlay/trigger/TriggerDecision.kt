package com.reeled.quizoverlay.trigger
package com.yourappname.quizoverlay.trigger

import com.yourappname.quizoverlay.model.QuizQuestion

// ─────────────────────────────────────────────────────────────────────────────
// TriggerDecision
//
// Sealed return type of TriggerEngine.checkAndFire().
// The caller (OverlayForegroundService) pattern-matches on this — no booleans,
// no nullables, no ambiguity about what happened and why.
//
// Layout doc: "Sealed class: Fire(question) | Skip(reason)"
// Dependency map: trigger → model
// ─────────────────────────────────────────────────────────────────────────────

sealed class TriggerDecision {

    /**
     * All gates passed. Show this question as the next quiz overlay.
     */
    data class Fire(val question: QuizQuestion) : TriggerDecision()

    /**
     * One or more gates blocked the trigger. [reason] is a stable snake_case
     * string logged to event_logs for post-session analysis.
     *
     * Defined skip reasons (never change these strings — they are stored in Supabase):
     *   overlay_active        — a quiz is already on screen
     *   daily_cap_reached     — MAX_DAILY quizzes shown today
     *   cooldown_active       — standard or dismiss-penalised cooldown in progress
     *   warmup_not_done       — child hasn't been in the app long enough yet
     *   interval_not_elapsed  — not enough time since the last quiz
     *   no_questions_cached   — Room question cache is empty (QuizFetchWorker pending)
     *   parent_paused         — parent manually paused the overlay
     */
    data class Skip(val reason: String) : TriggerDecision()
}