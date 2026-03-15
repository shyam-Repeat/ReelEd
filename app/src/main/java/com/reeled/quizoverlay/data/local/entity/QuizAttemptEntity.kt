package com.reeled.quizoverlay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,     // UUID generated locally
    val testerId: String,
    val questionId: String,
    val shownAt: Long,
    val answeredAt: Long?,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String,
    val dismissed: Boolean,
    val synced: Boolean = false
)
