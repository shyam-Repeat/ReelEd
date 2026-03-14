package com.reeled.quizoverlay.model

data class QuizRules(
    val timerSeconds: Int,
    val strictMode: Boolean,
    val showCorrectOnWrong: Boolean
)
