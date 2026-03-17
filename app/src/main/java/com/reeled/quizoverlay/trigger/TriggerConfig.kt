package com.reeled.quizoverlay.trigger

object TriggerConfig {
    const val WARMUP_MS = 30 * 1000L                      // 30 seconds
    const val COOLDOWN_MS = 3 * 60 * 1000L               // 3 minutes
    const val INTERVAL_MS = 2 * 60 * 1000L               // 2 minutes
    const val MAX_DAILY = 6
    const val POLLING_INTERVAL_MS = 10 * 1000L           // 10 seconds
    const val DISMISS_PENALTY_MS = 5 * 60 * 1000L        // 5 minutes
    const val STREAK_REDUCTION_MS = 1 * 60 * 1000L       // 1 minute
    const val MIN_COOLDOWN_MS = 30 * 1000L               // 30 seconds
    
    val SUBJECTS = listOf(
        "math", "english", "science", "general", "animals", 
        "colors", "shapes", "food", "sounds", "counting", "sizes"
    )
    
    const val DIFFICULTY_EASY_UNTIL  = 2
    const val DIFFICULTY_MEDIUM_UNTIL = 4
}
