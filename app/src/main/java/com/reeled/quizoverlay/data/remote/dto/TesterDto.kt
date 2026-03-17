package com.reeled.quizoverlay.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TesterDto(
    @SerializedName("tester_id") val testerId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("overlay_permission_granted") val overlayPermissionGranted: Boolean,
    @SerializedName("usage_access_granted") val usageAccessGranted: Boolean,
    @SerializedName("battery_opt_disabled") val batteryOptDisabled: Boolean,
    @SerializedName("created_at") val createdAt: String,
)
