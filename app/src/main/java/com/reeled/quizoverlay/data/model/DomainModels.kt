package com.reeled.quizoverlay.data.model

enum class QuizCardType {
    TAP_CHOICE,
    TAP_TAP_MATCH,
    DRAG_DROP_MATCH,
    FILL_BLANK
}

data class QuizQuestion(
    val id: String,
    val cardType: QuizCardType,
    val subject: String,
    val difficulty: Int,
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?,
    val payloadJson: String,
    val timerSeconds: Int,
    val strictMode: Boolean,
    val showCorrectOnWrong: Boolean,
    val active: Boolean
)

object TriggerConfig {
    const val WARMUP_MS = 3 * 60 * 1000L
    const val COOLDOWN_MS = 12 * 60 * 1000L
    const val INTERVAL_MS = 10 * 60 * 1000L
    const val MAX_DAILY = 6
    const val POLLING_INTERVAL_MS = 30 * 1000L
    const val DISMISS_PENALTY_MS = 8 * 60 * 1000L
}

sealed class TriggerDecision {
    data class Fire(val question: QuizQuestion) : TriggerDecision()
    data class Skip(val reason: String) : TriggerDecision()
}
