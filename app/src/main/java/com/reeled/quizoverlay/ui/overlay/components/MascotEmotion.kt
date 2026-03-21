package com.reeled.quizoverlay.ui.overlay.components

/**
 * All emotional states for the mascot (Monkey).
 * These are mapped to Rive state machine inputs.
 */
enum class MascotEmotion {
    IDLE,       // calm, slow blink
    HAPPY,      // big smile
    CHEER,      // jumping/cheering
    SAD,        // droopy eyes, frown
    THINKING,   // one eyebrow raised, side glance
    CORRECT,    // heart eyes or positive feedback
    WRONG,      // sweat drop or worried
    SLEEPING    // closed eyes
}
