package com.reeled.quizoverlay.trigger

import android.content.Context
import android.media.AudioManager
import com.reeled.quizoverlay.prefs.AppDetectionMode

class VideoPlaybackDetector(
    private val context: Context,
    private val foregroundAppDetector: ForegroundAppDetector,
    private val mediaSessionAppDetector: MediaSessionAppDetector
) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun getInterruptScore(
        monitoredApps: Set<String> = emptySet(),
        detectionMode: AppDetectionMode = AppDetectionMode.USAGE_STATS
    ): InterruptScore {
        val detectedPackage = when (detectionMode) {
            AppDetectionMode.USAGE_STATS -> foregroundAppDetector.getCurrentForegroundPackage()
            AppDetectionMode.MEDIA_SESSION -> mediaSessionAppDetector.getCurrentPlayingPackage(monitoredApps)
        }
        val targets = if (monitoredApps.isNotEmpty()) monitoredApps else ForegroundAppDetector.TARGET_PACKAGES
        val isTargetForeground = detectedPackage in targets
        val isAudioActive = audioManager.isMusicActive
        val ringerMode = audioManager.ringerMode

        val score = when {
            !isTargetForeground -> 0
            !isAudioActive -> 0 // Only trigger when audio is active (video playing)
            ringerMode == AudioManager.RINGER_MODE_NORMAL -> 3
            else -> 2
        }

        return InterruptScore(
            score = score,
            isTargetForeground = isTargetForeground,
            isAudioActive = isAudioActive,
            ringerMode = ringerMode,
            detectedPackage = detectedPackage
        )
    }

    fun isAcceptableInterruptMoment(
        monitoredApps: Set<String> = emptySet(),
        detectionMode: AppDetectionMode = AppDetectionMode.USAGE_STATS
    ): Boolean =
        getInterruptScore(monitoredApps, detectionMode).score >= 1

    data class InterruptScore(
        val score: Int,
        val isTargetForeground: Boolean,
        val isAudioActive: Boolean,
        val ringerMode: Int,
        val detectedPackage: String?
    ) {
        val isIdeal: Boolean get() = score == 3
        val isAcceptable: Boolean get() = score >= 1
        val isPoor: Boolean get() = score == 0
    }
}
