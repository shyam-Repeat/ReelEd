package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.reeled.quizoverlay.data.local.entity.EventLogEntity

data class EventLogDto(
    @SerializedName("id") val id: String,
    @SerializedName("tester_id") val testerId: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("payload_json") val payloadJson: String,
    @SerializedName("created_at") val createdAt: String // ISO 8601
)

fun EventLogEntity.toDto(): EventLogDto {
    return EventLogDto(
        id = id,
        testerId = testerId,
        eventType = eventType,
        payloadJson = payloadJson,
        createdAt = createdAt.toString() // Needs proper formatting
    )
}
