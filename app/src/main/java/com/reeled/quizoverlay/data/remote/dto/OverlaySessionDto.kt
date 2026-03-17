package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.reeled.quizoverlay.data.local.entity.OverlaySessionEntity
import com.reeled.quizoverlay.util.TimeUtils

data class OverlaySessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("tester_id") val testerId: String,
    @SerializedName("started_at") val startedAt: String, // ISO 8601
    @SerializedName("ended_at") val endedAt: String?, // ISO 8601
    @SerializedName("total_quizzes_shown") val totalQuizzesShown: Int,
    @SerializedName("total_answered") val totalAnswered: Int,
    @SerializedName("total_dismissed") val totalDismissed: Int,
    @SerializedName("total_timer_expired") val totalTimerExpired: Int
)

fun OverlaySessionEntity.toDto() = OverlaySessionDto(
    id = id,
    testerId = testerId,
    startedAt = TimeUtils.toIsoString(startedAt),
    endedAt = endedAt?.let { TimeUtils.toIsoString(it) },
    totalQuizzesShown = totalQuizzesShown,
    totalAnswered = totalAnswered,
    totalDismissed = totalDismissed,
    totalTimerExpired = totalTimerExpired
)
