package com.reeled.quizoverlay.model

data class QuizQuestion(
    val id: String,
    val cardType: String,
    val subject: String,
    val difficulty: Int,
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?,
    val payloadJson: String,
    val timerSeconds: Int,
    val strictMode: Boolean,
    val showCorrectOnWrong: Boolean
)
