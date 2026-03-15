package com.reeled.quizoverlay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val cardType: String,
    val subject: String,
    val difficulty: Int,
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?,
    val payloadJson: String,
    val timerSeconds: Int,
    val strictMode: Boolean,
    val showCorrectOnWrong: Boolean,
    val active: Boolean,
    val fetchedAt: Long
)

fun QuizQuestionEntity.toDomain(): com.reeled.quizoverlay.model.QuizQuestion {
    return com.reeled.quizoverlay.model.QuizQuestion(
        id = id,
        cardType = cardType,
        subject = subject,
        difficulty = difficulty,
        questionText = questionText,
        instructionLabel = instructionLabel,
        mediaUrl = mediaUrl,
        payloadJson = payloadJson,
        timerSeconds = timerSeconds,
        strictMode = strictMode,
        showCorrectOnWrong = showCorrectOnWrong
    )
}
