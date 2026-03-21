package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

@Composable
fun PandaTrainAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "PandaTrain")

    // Train Bounce: translateY(0 to -12 to 0)
    val trainBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                -12f at 700 with FastOutSlowInEasing
                0f at 1400 with FastOutSlowInEasing
            }
        ),
        label = "TrainBounce"
    )

    // Panda Wave: rotate(0 to -20 to 20 to 0)
    val pandaWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                -20f at 500
                20f at 1500
                0f at 2000
            }
        ),
        label = "PandaWave"
    )

    // Panda Idle Bob: translateY(0 to -8 to 0)
    val pandaIdle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                -8f at 1500 with LinearEasing
                0f at 3000 with LinearEasing
            }
        ),
        label = "PandaIdle"
    )

    // Entry animation (Slide in from left)
    val entryAnim = remember { Animatable(-400f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(3000, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f))
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Aspect ratio of the original design is 400:120 (3.33)
            // We scale based on width but ensure we don't exceed height
            val scaleX = canvasWidth / 400f
            val scaleY = canvasHeight / 120f
            val scale = minOf(scaleX, scaleY)
            
            val xOffsetBase = (canvasWidth - (400f * scale)) / 2f
            val yOffsetBase = (canvasHeight - (120f * scale)) / 2f
            val bounceY = trainBounce * scale
            
            withTransform({
                translate(left = (entryAnim.value * scale) + xOffsetBase, top = yOffsetBase + bounceY)
                scale(scale, scale, Offset.Zero)
            }) {
                drawTrack()
                drawBackCar()
                drawMiddleCar()
                drawFrontCarWithPanda(pandaWave, pandaIdle)
                drawEngine()
            }
        }
    }
}

private fun DrawScope.drawTrack() {
    val brownTrack = Color(0xFF8B7355)
    val brownTie = Color(0xFF6B5344)
    
    drawLine(
        color = brownTrack,
        start = Offset(0f, 110f),
        end = Offset(400f, 110f),
        strokeWidth = 3f
    )
    
    listOf(40f, 100f, 160f, 220f, 280f, 340f).forEach { x ->
        drawCircle(color = brownTie, radius = 8f, center = Offset(x, 115f))
    }
}

private fun DrawScope.drawFrontCarWithPanda(waveRotation: Float, pandaBob: Float) {
    val redCar = Color(0xFFEF4444)
    val shineColor = Color(0xFFFCA5A5).copy(alpha = 0.6f)
    val windowColor = Color(0xFFFEF3F2)
    
    // Unified Mascot Colors
    val pandaWhite = Color(0xFFF5F5F5)
    val pandaDark = Color(0xFF1A1A1A)
    val blushColor = Color(0xFFFFB3C1).copy(alpha = 0.7f)
    val pinkMouth = Color(0xFFEC4899)

    // Car Body
    drawRoundRect(color = redCar, topLeft = Offset(20f, 40f), size = Size(80f, 65f), cornerRadius = CornerRadius(8f))
    drawRoundRect(color = shineColor, topLeft = Offset(25f, 45f), size = Size(70f, 12f), cornerRadius = CornerRadius(4f))
    drawRoundRect(color = windowColor, topLeft = Offset(30f, 55f), size = Size(60f, 35f), cornerRadius = CornerRadius(4f))

    // Panda
    withTransform({
        translate(top = pandaBob)
    }) {
        // Head
        drawCircle(color = pandaWhite, radius = 18f, center = Offset(60f, 65f))
        
        // Ears
        drawCircle(color = pandaDark, radius = 8f, center = Offset(48f, 52f))
        drawCircle(color = pandaDark, radius = 8f, center = Offset(72f, 52f))
        
        // Eyes
        drawCircle(color = pandaDark, radius = 4.5f, center = Offset(53f, 63f))
        drawCircle(color = pandaDark, radius = 4.5f, center = Offset(67f, 63f))
        drawCircle(color = Color.White, radius = 1.5f, center = Offset(54.5f, 61.5f))
        drawCircle(color = Color.White, radius = 1.5f, center = Offset(68.5f, 61.5f))
        
        // Blush
        drawCircle(color = blushColor, radius = 3.5f, center = Offset(45f, 70f))
        drawCircle(color = blushColor, radius = 3.5f, center = Offset(75f, 70f))
        
        // Mouth
        val mouthPath = Path().apply {
            moveTo(60f, 75f)
            lineTo(58f, 78f)
            lineTo(62f, 78f)
            close()
        }
        drawPath(path = mouthPath, color = pandaDark)
        
        val tonguePath = Path().apply {
            moveTo(60f, 78f)
            quadraticBezierTo(57f, 81f, 60f, 82f)
            quadraticBezierTo(63f, 81f, 60f, 78f)
            close()
        }
        drawPath(path = tonguePath, color = pinkMouth)

        // Waving Hand
        withTransform({
            rotate(degrees = waveRotation, pivot = Offset(60f, 75f))
        }) {
            drawCircle(color = pandaWhite, radius = 4f, center = Offset(72f, 78f))
            drawLine(
                color = pandaWhite,
                start = Offset(72f, 78f),
                end = Offset(78f, 68f),
                strokeWidth = 3f
            )
        }
    }

    // Coupling
    drawLine(color = pandaDark, start = Offset(100f, 80f), end = Offset(125f, 80f), strokeWidth = 2f)
}

private fun DrawScope.drawMiddleCar() {
    val yellowCar = Color(0xFFFBBF24)
    val shineColor = Color(0xFFFCD34D).copy(alpha = 0.6f)
    val windowColor = Color(0xFFFEF3F2)
    val couplingColor = Color(0xFF1F2937)

    drawRoundRect(color = yellowCar, topLeft = Offset(125f, 50f), size = Size(70f, 55f), cornerRadius = CornerRadius(6f))
    drawRoundRect(color = shineColor, topLeft = Offset(130f, 55f), size = Size(60f, 10f), cornerRadius = CornerRadius(3f))
    drawRoundRect(color = windowColor, topLeft = Offset(135f, 70f), size = Size(25f, 25f), cornerRadius = CornerRadius(3f))
    drawRoundRect(color = windowColor, topLeft = Offset(165f, 70f), size = Size(25f, 25f), cornerRadius = CornerRadius(3f))
    
    drawLine(color = couplingColor, start = Offset(195f, 80f), end = Offset(220f, 80f), strokeWidth = 2f)
}

private fun DrawScope.drawBackCar() {
    val blueCar = Color(0xFF3B82F6)
    val shineColor = Color(0xFF93C5FD).copy(alpha = 0.6f)
    val windowColor = Color(0xFFFEF3F2)

    drawRoundRect(color = blueCar, topLeft = Offset(220f, 55f), size = Size(70f, 50f), cornerRadius = CornerRadius(6f))
    drawRoundRect(color = shineColor, topLeft = Offset(225f, 60f), size = Size(60f, 10f), cornerRadius = CornerRadius(3f))
    drawRoundRect(color = windowColor, topLeft = Offset(230f, 75f), size = Size(30f, 22f), cornerRadius = CornerRadius(3f))
    drawRoundRect(color = windowColor, topLeft = Offset(265f, 75f), size = Size(20f, 22f), cornerRadius = CornerRadius(3f))
}

private fun DrawScope.drawEngine() {
    val greenCar = Color(0xFF10B981)
    val boilerColor = Color(0xFF34D399)
    val grayChimney = Color(0xFF6B7280)
    val darkGray = Color(0xFF4B5563)
    val wheelDark = Color(0xFF1F2937)
    val wheelBlue = Color(0xFF3B82F6)

    drawRoundRect(color = greenCar, topLeft = Offset(300f, 50f), size = Size(80f, 60f), cornerRadius = CornerRadius(6f))
    drawRoundRect(color = boilerColor, topLeft = Offset(310f, 55f), size = Size(60f, 25f), cornerRadius = CornerRadius(3f))
    
    // Chimney
    drawRect(color = grayChimney, topLeft = Offset(335f, 35f), size = Size(8f, 20f))
    drawRoundRect(color = darkGray, topLeft = Offset(333f, 32f), size = Size(12f, 5f), cornerRadius = CornerRadius(2f))
    
    // Wheels
    drawCircle(color = wheelDark, radius = 10f, center = Offset(320f, 115f))
    drawCircle(color = wheelDark, radius = 10f, center = Offset(370f, 115f))
    drawCircle(color = wheelBlue, radius = 6f, center = Offset(320f, 115f))
    drawCircle(color = wheelBlue, radius = 6f, center = Offset(370f, 115f))
}
