package com.reeled.quizoverlay.util

import android.content.Context
import android.media.AudioManager
import android.os.Build

class AudioMuter(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isCurrentlyMuted = false

    fun mute() {
        if (isCurrentlyMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
            }
            isCurrentlyMuted = true
        } catch (e: Exception) {
            // Log error
        }
    }

    fun restore() {
        if (!isCurrentlyMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }
            isCurrentlyMuted = false
        } catch (e: Exception) {
            // Log error
        }
    }
}
