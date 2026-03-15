package com.reeled.quizoverlay.data.model

sealed class TriggerDecision {
    data class Fire(val question: QuizQuestion) : TriggerDecision()
    data class Skip(val reason: String) : TriggerDecision()
}
