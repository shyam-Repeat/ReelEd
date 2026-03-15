package com.reeled.quizoverlay.trigger

import android.content.Context
import android.media.AudioManager

class VideoPlaybackDetector(
    private val context: Context,
    private val foregroundAppDetector: ForegroundAppDetector
) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun getInterruptScore(): InterruptScore {
        val isTargetForeground = foregroundAppDetector.isTargetAppInForeground()
        val isAudioActive = audioManager.isMusicActive
        val ringerMode = audioManager.ringerMode

        val score = when {
            !isTargetForeground -> 0
            !isAudioActive && ringerMode == AudioManager.RINGER_MODE_NORMAL -> 3
            !isAudioActive -> 2
            isAudioActive && ringerMode == AudioManager.RINGER_MODE_NORMAL -> 1
            else -> 0
        }

        return InterruptScore(
            score = score,
            isTargetForeground = isTargetForeground,
            isAudioActive = isAudioActive,
            ringerMode = ringerMode
        )
    }

    fun isAcceptableInterruptMoment(): Boolean = getInterruptScore().score >= 1

    data class InterruptScore(
        val score: Int,
        val isTargetForeground: Boolean,
        val isAudioActive: Boolean,
        val ringerMode: Int
    ) {
        val isIdeal: Boolean get() = score == 3
        val isAcceptable: Boolean get() = score >= 1
        val isPoor: Boolean get() = score == 0
    }
}
