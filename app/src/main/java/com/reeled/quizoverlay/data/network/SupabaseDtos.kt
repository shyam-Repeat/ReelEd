package com.reeled.quizoverlay.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestionDto(
    val id: String,
    @SerialName("card_type") val cardType: String,
    val subject: String,
    val difficulty: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("instruction_label") val instructionLabel: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("payload_json") val payloadJson: String,
    @SerialName("timer_seconds") val timerSeconds: Int,
    @SerialName("strict_mode") val strictMode: Boolean,
    @SerialName("show_correct_on_wrong") val showCorrectOnWrong: Boolean,
    val active: Boolean
)

@Serializable
data class QuizAttemptDto(
    val id: String,
    @SerialName("tester_id") val testerId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("shown_at") val shownAt: String,
    @SerialName("answered_at") val answeredAt: String? = null,
    @SerialName("selected_option_id") val selectedOptionId: String? = null,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("was_dismissed") val wasDismissed: Boolean,
    @SerialName("was_timer_expired") val wasTimerExpired: Boolean,
    @SerialName("response_time_ms") val responseTimeMs: Long,
    @SerialName("source_app") val sourceApp: String
)

@Serializable
data class EventLogDto(
    val id: String,
    @SerialName("tester_id") val testerId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("payload_json") val payloadJson: String? = null,
    @SerialName("created_at") val createdAt: String
)
