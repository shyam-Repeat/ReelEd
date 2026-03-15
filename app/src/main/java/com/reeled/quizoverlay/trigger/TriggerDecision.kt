package com.reeled.quizoverlay.trigger

import com.reeled.quizoverlay.model.QuizQuestion

sealed class TriggerDecision {
    data class Fire(val question: QuizQuestion) : TriggerDecision()
    data class Skip(val reason: String) : TriggerDecision()
}
