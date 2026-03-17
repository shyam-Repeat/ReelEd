package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.reeled.quizoverlay.data.local.entity.OverlaySessionEntity

data class OverlaySessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("tester_id") val testerId: String,
    @SerializedName("started_at") val startedAt: Long,
    @SerializedName("ended_at") val endedAt: Long?,
    @SerializedName("quizzes_shown") val quizzesShown: Int,
    @SerializedName("quizzes_answered") val quizzesAnswered: Int,
    @SerializedName("quizzes_dismissed") val quizzesDismissed: Int,
    @SerializedName("quizzes_expired") val quizzesExpired: Int
)

fun OverlaySessionEntity.toDto() = OverlaySessionDto(
    id = id,
    testerId = testerId,
    startedAt = startedAt,
    endedAt = endedAt,
    quizzesShown = totalQuizzesShown,
    quizzesAnswered = totalAnswered,
    quizzesDismissed = totalDismissed,
    quizzesExpired = totalTimerExpired
)
