package com.reeled.quizoverlay.data.model

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
