package com.reeled.quizoverlay.trigger
package com.yourappname.quizoverlay.trigger

import android.content.Context
import android.media.AudioManager

// ─────────────────────────────────────────────────────────────────────────────
// VideoPlaybackDetector
//
// Responsibilities (layout doc):
//   Three-signal heuristic. Combines foreground check + AudioManager state.
//
// Why this matters:
//   Interrupting a child mid-scroll is fine (mild annoyance, recoverable).
//   Interrupting a child mid-video with sound abruptly cut is hostile —
//   parent trust evaporates fast. This detector lets TriggerEngine PREFER
//   quiz moments when video is NOT playing, without hard-blocking when it is
//   (hard-blocking would mean a child playing music all day never gets a quiz).
//
// Three signals used:
//   Signal 1 — Target app is foreground   (ForegroundAppDetector)
//   Signal 2 — Audio stream MUSIC is active (AudioManager.isMusicActive)
//   Signal 3 — Ringer mode is NORMAL or SILENT (not vibrate, which may
//              indicate the device is in a pocket/bag mid-video anyway)
//
// Improvement over baseline:
//   The original spec mentions "three-signal heuristic" without detailing all
//   three. We add a CONFIDENCE score (0–3) so TriggerEngine can soft-prefer
//   good moments without hard-blocking. Score of 0 = strongly prefer to skip,
//   3 = ideal interrupt moment.
//
// Dependency map (layout doc): trigger → (no internal package deps)
// ─────────────────────────────────────────────────────────────────────────────

class VideoPlaybackDetector(
    private val context: Context,
    private val foregroundAppDetector: ForegroundAppDetector
) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the interrupt confidence score for the current moment (0–3).
     *
     *   3 = Great time to interrupt (target app foreground, no audio, normal ringer)
     *   2 = Good  (target app foreground, no audio active)
     *   1 = Acceptable (target app foreground, audio active but ringer suggests hands-on use)
     *   0 = Poor  (audio active + vibrate/silent ringer = likely mid-video, pocket or earphones)
     *
     * TriggerEngine uses this score to PREFER good moments. It never hard-blocks
     * based on score alone — that would cause the daily cap to go unreached on
     * heavy video-watching days.
     */
    fun getInterruptScore(): InterruptScore {
        val isTargetForeground = foregroundAppDetector.isTargetAppInForeground()
        val isAudioActive = audioManager.isMusicActive
        val ringerMode = audioManager.ringerMode

        val isDeviceLikelyInUse = ringerMode == AudioManager.RINGER_MODE_NORMAL ||
                ringerMode == AudioManager.RINGER_MODE_SILENT

        val score = when {
            // Target app not in foreground at all — this check is done before
            // VideoPlaybackDetector is even consulted in TriggerEngine, but
            // return a defined score just in case the call order changes.
            !isTargetForeground -> 0

            // No audio + ringer normal = clean scroll session, ideal moment
            !isAudioActive && ringerMode == AudioManager.RINGER_MODE_NORMAL -> 3

            // No audio playing (silent/paused video) = still a good moment
            !isAudioActive -> 2

            // Audio active but device in normal use (sound on) = acceptable
            isAudioActive && ringerMode == AudioManager.RINGER_MODE_NORMAL -> 1

            // Audio active + vibrate suggests earphones + active video = poorest moment
            else -> 0
        }

        return InterruptScore(
            score = score,
            isTargetForeground = isTargetForeground,
            isAudioActive = isAudioActive,
            ringerMode = ringerMode
        )
    }

    /**
     * Convenience: returns true if this is an acceptable interrupt moment.
     * Score >= 1 means the child is in a target app and at minimum hands-on.
     * TriggerEngine uses this for the soft-prefer check — still fires at 0
     * if the child is nearing the daily cap and time is running out.
     */
    fun isAcceptableInterruptMoment(): Boolean = getInterruptScore().score >= 1

    // ─────────────────────────────────────────────────────────────────────────
    // Result type
    // ─────────────────────────────────────────────────────────────────────────

    data class InterruptScore(
        val score: Int,                 // 0–3 confidence
        val isTargetForeground: Boolean,
        val isAudioActive: Boolean,
        val ringerMode: Int             // AudioManager.RINGER_MODE_*
    ) {
        val isIdeal: Boolean get() = score == 3
        val isAcceptable: Boolean get() = score >= 1
        val isPoor: Boolean get() = score == 0
    }
}