package com.reeled.quizoverlay.trigger
package com.yourappname.quizoverlay.trigger

// ─────────────────────────────────────────────────────────────────────────────
// TriggerConfig
//
// Single source of truth for every timing constant used by TriggerEngine.
// Adjust these after Week 1 based on dismiss-rate data from Supabase event_logs.
//
// Layout doc: "All timing constants in one place."
// Dependency map: trigger → (no internal deps — pure constants)
// ─────────────────────────────────────────────────────────────────────────────

object TriggerConfig {

    // ── Session warmup ────────────────────────────────────────────────────────
    // Child must be in a target app for this long before the FIRST quiz fires.
    // Rationale: child settles into the app, less hostile to early interruption.
    val WARMUP_MS = 3 * 60 * 1000L                 // 3 minutes

    // ── Cooldown ──────────────────────────────────────────────────────────────
    // Minimum gap between any two successive quizzes in the same session.
    // Rationale: feels like ad breaks on TV — predictable, not punishing.
    val COOLDOWN_MS = 12 * 60 * 1000L              // 12 minutes

    // ── Inter-quiz interval ───────────────────────────────────────────────────
    // How far into a session the engine waits before re-checking eligibility
    // after a quiz has been answered or dismissed.
    val INTERVAL_MS = 10 * 60 * 1000L              // 10 minutes

    // ── Daily cap ─────────────────────────────────────────────────────────────
    // Maximum quizzes shown per child per calendar day (midnight → midnight).
    // At 12-min cooldown, 6 quizzes = ~72 min of continuous session time.
    val MAX_DAILY = 6

    // ── Polling interval ──────────────────────────────────────────────────────
    // How often OverlayForegroundService calls TriggerEngine.checkAndFire().
    val POLLING_INTERVAL_MS = 30 * 1000L           // every 30 seconds

    // ── Dismiss penalty ───────────────────────────────────────────────────────
    // Added on top of COOLDOWN_MS when the child dismissed the previous quiz
    // without answering. Discourages rapid dismiss-to-escape behaviour.
    val DISMISS_PENALTY_MS = 8 * 60 * 1000L        // 8 extra minutes

    // ── Streak bonus ──────────────────────────────────────────────────────────
    // IMPROVEMENT: If the child answered the last quiz correctly, shorten the
    // next cooldown slightly. Positive reinforcement — reward engagement.
    // Net cooldown on correct streak = COOLDOWN_MS - STREAK_REDUCTION_MS (min 5 min).
    val STREAK_REDUCTION_MS = 2 * 60 * 1000L       // shave 2 min off cooldown

    // ── Minimum cooldown floor ────────────────────────────────────────────────
    // Cooldown can never drop below this, even with streak bonuses applied.
    val MIN_COOLDOWN_MS = 5 * 60 * 1000L           // 5 minutes hard floor

    // ── Subject rotation ─────────────────────────────────────────────────────
    // Canonical subject list. Used by selectNextQuestion for round-robin balance.
    val SUBJECTS = listOf("math", "english", "science", "general")

    // ── Difficulty thresholds ─────────────────────────────────────────────────
    // Quizzes 1–2  → difficulty 1 only (ease child in)
    // Quizzes 3–4  → difficulty 1 or 2
    // Quiz  5+     → any difficulty
    val DIFFICULTY_EASY_UNTIL  = 2      // exclusive upper bound on easy-only window
    val DIFFICULTY_MEDIUM_UNTIL = 4     // exclusive upper bound on medium window
}