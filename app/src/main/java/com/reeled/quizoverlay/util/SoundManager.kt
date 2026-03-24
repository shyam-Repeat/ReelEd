package com.reeled.quizoverlay.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.reeled.quizoverlay.R

class SoundManager(private val context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var isLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload sounds
        // Note: These will fail until files are added to res/raw
        loadSound("correct", R.raw.sfx_correct)
        loadSound("wrong", R.raw.sfx_wrong)
        loadSound("match", R.raw.sfx_match)
        loadSound("train", R.raw.sfx_train)

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }
    }

    private fun loadSound(name: String, resId: Int) {
        try {
            soundMap[name] = soundPool.load(context, resId, 1)
        } catch (e: Exception) {
            // Resource might not exist yet
        }
    }

    fun play(name: String) {
        val soundId = soundMap[name] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun playTrain(durationMs: Int) {
        val soundId = soundMap["train"] ?: return
        // We could loop or just play once depending on sound file length
        soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
