package com.reeled.quizoverlay.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.reeled.quizoverlay.R
import java.util.Locale

class SoundManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val pendingPlayNames = mutableListOf<String>()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private var backgroundPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingBackgroundStart: Runnable? = null
    private val speechParams = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
    }

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
        loadSound("correct", R.raw.sfx_correct)
        loadSound("wrong", R.raw.sfx_wrong)
        loadSound("match", R.raw.sfx_match)
        loadSound("train", R.raw.sfx_train)

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
                flushPendingPlays()
            }
        }

        // Initialize TTS
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                it.language = Locale.US
                it.setSpeechRate(1.1f) // Slightly faster as per user feedback
                it.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = Unit
                    override fun onError(utteranceId: String?) = Unit
                })
                ttsReady = true
                pendingSpeech?.let { text ->
                    speak(text)
                    pendingSpeech = null
                }
            }
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
        playInternal(name, leftVolume = 1f, rightVolume = 1f)
    }

    fun playTrain(durationMs: Int) {
        playInternal("train", leftVolume = 0.7f, rightVolume = 0.7f)
        scheduleBackgroundAfterTrain(durationMs)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (ttsReady) {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                speechParams,
                "quiz_tts_${text.hashCode()}"
            )
        } else {
            pendingSpeech = text
        }
    }

    fun stopAll() {
        tts?.stop()
        stopBackgroundMusic()
        // No direct soundPool.stopAll() but we can stop active streams if needed.
    }

    fun release() {
        stopBackgroundMusic(releasePlayer = true)
        soundPool.release()
        tts?.stop()
        tts?.shutdown()
    }

    private fun playInternal(name: String, leftVolume: Float, rightVolume: Float) {
        val soundId = soundMap[name] ?: return
        if (loadedSoundIds.contains(soundId)) {
            soundPool.play(soundId, leftVolume, rightVolume, 1, 0, 1f)
        } else {
            pendingPlayNames.add(name)
        }
    }

    private fun flushPendingPlays() {
        if (pendingPlayNames.isEmpty()) return
        val iterator = pendingPlayNames.iterator()
        while (iterator.hasNext()) {
            val name = iterator.next()
            val soundId = soundMap[name] ?: continue
            if (loadedSoundIds.contains(soundId)) {
                val volume = if (name == "train") 0.7f else 1f
                soundPool.play(soundId, volume, volume, 1, 0, 1f)
                iterator.remove()
            }
        }
    }

    private fun scheduleBackgroundAfterTrain(delayMs: Int) {
        pendingBackgroundStart?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable { startBackgroundMusic() }
        pendingBackgroundStart = runnable
        mainHandler.postDelayed(runnable, delayMs.toLong().coerceAtLeast(0L))
    }

    private fun startBackgroundMusic() {
        val player = backgroundPlayer ?: createBackgroundPlayer() ?: return
        try {
            if (!player.isPlaying) {
                player.start()
            }
        } catch (_: Exception) {
        }
    }

    private fun stopBackgroundMusic(releasePlayer: Boolean = false) {
        pendingBackgroundStart?.let { mainHandler.removeCallbacks(it) }
        pendingBackgroundStart = null

        val player = backgroundPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            player.seekTo(0)
        } catch (_: Exception) {
        }

        if (releasePlayer) {
            try {
                player.release()
            } catch (_: Exception) {
            }
            backgroundPlayer = null
        }
    }

    private fun createBackgroundPlayer(): MediaPlayer? {
        return try {
            MediaPlayer.create(context.applicationContext, R.raw.background)?.apply {
                isLooping = true
                setVolume(BACKGROUND_VOLUME, BACKGROUND_VOLUME)
            }.also { backgroundPlayer = it }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val BACKGROUND_VOLUME = 0.12f
    }
}
