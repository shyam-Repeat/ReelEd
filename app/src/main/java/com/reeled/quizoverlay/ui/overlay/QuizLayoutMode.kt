package com.reeled.quizoverlay.ui.overlay

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp

enum class QuizLayoutMode {
    Vertical,
    HorizontalCompact,
    HorizontalWide
}

@Stable
fun resolveQuizLayoutMode(maxWidth: Dp, maxHeight: Dp): QuizLayoutMode {
    val aspectRatio = if (maxHeight.value == 0f) 1f else maxWidth.value / maxHeight.value
    return when {
        aspectRatio >= 1.65f || maxWidth.value >= 900f -> QuizLayoutMode.HorizontalWide
        aspectRatio >= 1.1f || maxWidth.value >= 700f -> QuizLayoutMode.HorizontalCompact
        else -> QuizLayoutMode.Vertical
    }
}
