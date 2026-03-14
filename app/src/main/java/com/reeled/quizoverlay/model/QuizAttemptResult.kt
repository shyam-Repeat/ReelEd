package com.reeled.quizoverlay.model

data class QuizAttemptResult(
    val questionId: String,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val wasDismissed: Boolean,
    val wasTimerExpired: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String
)
