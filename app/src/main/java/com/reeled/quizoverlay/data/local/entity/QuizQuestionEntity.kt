package com.reeled.quizoverlay.data.local.entity

data class QuizQuestionEntity(
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
