package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TesterDto(
    @SerializedName("id") val id: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("has_overlay_perm") val hasOverlayPerm: Boolean,
    @SerializedName("has_usage_stats_perm") val hasUsageStatsPerm: Boolean,
    @SerializedName("has_notification_perm") val hasNotificationPerm: Boolean,
    @SerializedName("is_battery_optimized") val isBatteryOptimized: Boolean,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("device_info") val deviceInfo: String,
    @SerializedName("created_at") val createdAt: Long
)
