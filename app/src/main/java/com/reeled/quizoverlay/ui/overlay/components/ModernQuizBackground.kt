package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun ModernQuizBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundAnim")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShapeFloat"
    )
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShapeDrift"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                color = Color(0xFFE3F2FD),
                radius = 120f + (floatPhase * 10f),
                center = Offset(
                    width * 0.08f + (driftPhase * 18f),
                    height * 0.24f + (floatPhase * 34f)
                )
            )
            drawCircle(
                color = Color(0xFFFCE7F3),
                radius = 150f + (floatPhase * 14f),
                center = Offset(
                    width * 0.92f - (driftPhase * 16f),
                    height * 0.18f - (floatPhase * 26f)
                )
            )
            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = 188f + (floatPhase * 16f),
                center = Offset(
                    width * 0.5f + (driftPhase * 20f),
                    height * 0.9f + (floatPhase * 28f)
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
