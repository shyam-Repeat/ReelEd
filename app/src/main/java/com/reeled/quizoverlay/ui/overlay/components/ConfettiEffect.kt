package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val angle: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    trigger: Boolean,
    onFinished: () -> Unit
) {
    if (!trigger) return

    val particles = remember {
        List(30) {
            Particle(
                x = 0.5f, // Start from center-ish
                y = 0.5f,
                color = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFF4500), // OrangeRed
                    Color(0xFF00FF00), // Green
                    Color(0xFF1E90FF), // DodgerBlue
                    Color(0xFFFF69B4)  // HotPink
                ).random(),
                size = Random.nextFloat() * 10f + 5f,
                speed = Random.nextFloat() * 400f + 200f,
                angle = Random.nextFloat() * 360f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 720f - 360f
            )
        }
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = animatable.value
        val centerX = size.width / 2
        val centerY = size.height / 2

        particles.forEach { particle ->
            val distance = particle.speed * progress
            val radian = Math.toRadians(particle.angle.toDouble())
            val x = centerX + (distance * Math.cos(radian)).toFloat()
            val y = centerY + (distance * Math.sin(radian)).toFloat() + (progress * progress * 500f) // gravity

            val currentRotation = particle.rotation + (particle.rotationSpeed * progress)

            rotate(currentRotation, Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = 1f - progress),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size)
                )
            }
        }
    }
}
