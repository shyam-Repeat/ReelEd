package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity

data class QuizQuestionDto(
    @SerializedName("id") val id: String,
    @SerializedName("card_type") val cardType: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("difficulty") val difficulty: Int,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("instruction_label") val instructionLabel: String,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("payload_json") val payloadJson: String,
    @SerializedName("timer_seconds") val timerSeconds: Int,
    @SerializedName("strict_mode") val strictMode: Boolean,
    @SerializedName("show_correct_on_wrong") val showCorrectOnWrong: Boolean,
    @SerializedName("active") val active: Boolean
)

fun QuizQuestionDto.toEntity(): QuizQuestionEntity {
    return QuizQuestionEntity(
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
        showCorrectOnWrong = showCorrectOnWrong,
        active = active,
        fetchedAt = System.currentTimeMillis()
    )
}
