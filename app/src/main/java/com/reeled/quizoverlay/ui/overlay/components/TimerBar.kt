package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun TimerBar(progress: Float, modifier: Modifier = Modifier) {
    val clamped = max(0f, progress)
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(Color(0xFFE8EAF0))
            .height(6.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                .background(Color(0xFF4F7DF3))
                .height(6.dp)
                 .fillMaxWidth(clamped.coerceIn(0f,1f))
        )
    }
}
