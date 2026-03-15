package com.reeled.quizoverlay.trigger

object TriggerConfig {
    const val WARMUP_MS = 3 * 60 * 1000L                 // 3 minutes
    const val COOLDOWN_MS = 12 * 60 * 1000L              // 12 minutes
    const val INTERVAL_MS = 10 * 60 * 1000L              // 10 minutes
    const val MAX_DAILY = 6
    const val POLLING_INTERVAL_MS = 30 * 1000L           // 30 seconds
    const val DISMISS_PENALTY_MS = 8 * 60 * 1000L        // 8 minutes
    const val STREAK_REDUCTION_MS = 2 * 60 * 1000L       // 2 minutes
    const val MIN_COOLDOWN_MS = 5 * 60 * 1000L           // 5 minutes
    
    val SUBJECTS = listOf("math", "english", "science", "general")
    
    const val DIFFICULTY_EASY_UNTIL  = 2
    const val DIFFICULTY_MEDIUM_UNTIL = 4
}
