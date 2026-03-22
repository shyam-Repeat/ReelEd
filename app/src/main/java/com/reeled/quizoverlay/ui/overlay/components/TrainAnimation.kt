package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun TrainAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "TrainAnimation")

    val trainBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                -12f at 2000 with FastOutSlowInEasing
                0f at 4000 with FastOutSlowInEasing
            }
        ),
        label = "TrainBounce"
    )

    val entryAnim = remember { Animatable(-420f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(1500, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f))
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val widthScale = (canvasWidth * 0.88f) / 400f
            val heightScale = (canvasHeight * 0.72f) / 120f
            val scale = min(widthScale, heightScale).coerceAtLeast(0.35f)

            val scaledWidth = 400f * scale
            val scaledHeight = 120f * scale
            val xOffsetBase = (canvasWidth - scaledWidth) / 2f
            val yOffsetBase = (canvasHeight - scaledHeight) / 2f
            val bounceY = trainBounce * scale

            withTransform({
                translate(left = entryAnim.value + xOffsetBase, top = yOffsetBase + bounceY)
                scale(scale, scale, Offset.Zero)
            }) {
                drawTrack()
                drawBackCar()
                drawMiddleCar()
                drawFrontCarBody()
                drawEngine()
            }
        }

        RiveMedia(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-86).dp, y = (-2).dp)
                .size(34.dp)
        )
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

private fun DrawScope.drawFrontCarBody() {
    val redCar = Color(0xFFEF4444)
    val shineColor = Color(0xFFFCA5A5).copy(alpha = 0.6f)
    val windowColor = Color(0xFFFEF3F2)
    val couplingColor = Color(0xFF1F2937)

    drawRoundRect(color = redCar, topLeft = Offset(20f, 40f), size = Size(80f, 65f), cornerRadius = CornerRadius(8f))
    drawRoundRect(color = shineColor, topLeft = Offset(25f, 45f), size = Size(70f, 12f), cornerRadius = CornerRadius(4f))
    drawRoundRect(color = windowColor, topLeft = Offset(30f, 55f), size = Size(60f, 35f), cornerRadius = CornerRadius(4f))
    drawLine(color = couplingColor, start = Offset(100f, 80f), end = Offset(125f, 80f), strokeWidth = 2f)
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
    drawRect(color = grayChimney, topLeft = Offset(335f, 35f), size = Size(8f, 20f))
    drawRoundRect(color = darkGray, topLeft = Offset(333f, 32f), size = Size(12f, 5f), cornerRadius = CornerRadius(2f))
    drawCircle(color = wheelDark, radius = 10f, center = Offset(320f, 115f))
    drawCircle(color = wheelDark, radius = 10f, center = Offset(370f, 115f))
    drawCircle(color = wheelBlue, radius = 6f, center = Offset(320f, 115f))
    drawCircle(color = wheelBlue, radius = 6f, center = Offset(370f, 115f))
}
