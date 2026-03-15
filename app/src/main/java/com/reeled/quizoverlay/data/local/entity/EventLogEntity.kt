package com.reeled.quizoverlay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey val id: String,
    val testerId: String,
    val eventType: String,
    val payloadJson: String,
    val createdAt: Long,
    val synced: Boolean = false
)
