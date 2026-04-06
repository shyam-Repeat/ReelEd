package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun OverlayAmbientBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val topLeftGlow = Brush.radialGradient(
            colors = listOf(Color(0xFF378ADD).copy(alpha = 0.22f), Color.Transparent),
            center = Offset(width * 0.12f, height * 0.12f),
            radius = width * 0.42f
        )
        drawRect(brush = topLeftGlow)

        val bottomRightGlow = Brush.radialGradient(
            colors = listOf(Color(0xFF378ADD).copy(alpha = 0.18f), Color.Transparent),
            center = Offset(width * 0.88f, height * 0.88f),
            radius = width * 0.46f
        )
        drawRect(brush = bottomRightGlow)

        val centerWash = Brush.radialGradient(
            colors = listOf(Color(0xFF378ADD).copy(alpha = 0.07f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = width * 0.58f
        )
        drawRect(brush = centerWash)
    }
}
