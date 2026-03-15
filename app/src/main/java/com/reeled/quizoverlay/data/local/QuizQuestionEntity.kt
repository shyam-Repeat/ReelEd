package com.reeled.quizoverlay.data.local

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
