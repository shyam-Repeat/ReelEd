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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun ModernQuizBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundAnim")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShapeFloat"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                color = Color(0xFFE3F2FD),
                radius = 120f,
                center = Offset(width * 0.12f, height * 0.18f + (animValue * 36f))
            )
            drawCircle(
                color = Color(0xFFFCE7F3),
                radius = 140f,
                center = Offset(width * 0.88f, height * 0.14f - (animValue * 24f))
            )
            drawCircle(
                color = Color(0xFFDCFCE7),
                radius = 180f,
                center = Offset(width * 0.5f, height * 0.92f + (animValue * 40f))
            )
        }

        BubbleMascot(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-24).dp, y = 92.dp)
                .size(124.dp)
        )
        BubbleMascot(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = 42.dp)
                .size(148.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun BubbleMascot(modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(CircleShape)) {
        RiveMedia(modifier = Modifier.fillMaxSize())
    }
}
