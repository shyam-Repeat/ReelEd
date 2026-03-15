package com.reeled.quizoverlay.trigger

data class TriggerState(
    val parentPauseActive: Boolean = false,
    val parentPauseExpiryMs: Long = 0,
    val overlayActive: Boolean = false,
    val quizzesShownToday: Int = 0,
    val lastQuizShownTime: Long = 0,
    val sessionStartTime: Long = 0,
    val lastWasDismissed: Boolean = false,
    val lastWasCorrect: Boolean = false,
    val lastShownQuestionId: String? = null
)
