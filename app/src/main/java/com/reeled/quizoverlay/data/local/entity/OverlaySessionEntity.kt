package com.reeled.quizoverlay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overlay_sessions")
data class OverlaySessionEntity(
    @PrimaryKey val id: String,
    val testerId: String,
    val startedAt: Long,
    var endedAt: Long? = null,
    var totalQuizzesShown: Int = 0,
    var totalAnswered: Int = 0,
    var totalDismissed: Int = 0,
    var totalTimerExpired: Int = 0,
    var synced: Boolean = false
)
