package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.reeled.quizoverlay.data.local.entity.QuizAttemptEntity

data class QuizAttemptDto(
    @SerializedName("id") val id: String,
    @SerializedName("tester_id") val testerId: String,
    @SerializedName("question_id") val questionId: String,
    @SerializedName("shown_at") val shownAt: String, // ISO 8601
    @SerializedName("answered_at") val answeredAt: String?, // ISO 8601
    @SerializedName("selected_option_id") val selectedOptionId: String?,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("was_dismissed") val wasDismissed: Boolean,
    @SerializedName("was_timer_expired") val wasTimerExpired: Boolean,
    @SerializedName("response_time_ms") val responseTimeMs: Long,
    @SerializedName("source_app") val sourceApp: String
)

fun QuizAttemptEntity.toDto(): QuizAttemptDto {
    // Note: In a real app, use a proper ISO 8601 formatter.
    // For now, using a placeholder string or simplified format.
    return QuizAttemptDto(
        id = id,
        testerId = testerId,
        questionId = questionId,
        shownAt = shownAt.toString(), // Needs proper formatting
        answeredAt = answeredAt?.toString(), // Needs proper formatting
        selectedOptionId = selectedOptionId,
        isCorrect = isCorrect,
        wasDismissed = dismissed,
        wasTimerExpired = false, // Add to entity if needed
        responseTimeMs = responseTimeMs,
        sourceApp = sourceApp
    )
}
