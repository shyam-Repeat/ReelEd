package com.reeled.quizoverlay.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioMuter(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isCurrentlyMuted = false
    private var focusRequest: AudioFocusRequest? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        // We don't really care about focus changes as we are an overlay,
        // but we need the listener for the request.
    }

    fun mute() {
        if (isCurrentlyMuted) return
        try {
            // 1. Request transient exclusive audio focus to signal other apps to pause hard
            requestAudioFocus()

            // Removed STREAM_MUSIC mute as it also mutes our own SoundManager/TTS.
            // Exclusive focus ownership should pause foreground video apps like YouTube.

            isCurrentlyMuted = true
        } catch (e: Exception) {
            // Log error
        }
    }

    fun restore() {
        if (!isCurrentlyMuted) return
        try {
            // 1. Abandon audio focus to let other apps resume
            abandonAudioFocus()

            isCurrentlyMuted = false
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }
}
