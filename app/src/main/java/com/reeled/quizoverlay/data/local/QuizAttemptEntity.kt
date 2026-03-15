package com.reeled.quizoverlay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val testerId: String,
    val questionId: String,
    val shownAt: Long,
    val answeredAt: Long?,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val wasDismissed: Boolean,
    val wasTimerExpired: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String,
    val synced: Boolean = false
)
