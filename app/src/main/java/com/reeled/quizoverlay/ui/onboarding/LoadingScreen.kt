package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Loading")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Shimmer"
    )

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val totalTime = 3000L
        val steps = 60
        val delayPerStep = totalTime / steps
        for (i in 1..steps) {
            delay(delayPerStep)
            progress = i.toFloat() / steps
        }
        onLoadingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Circuit Pattern Background (simplified as animated lines)
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
            val stroke = Stroke(width = 1.dp.toPx())
            val spacing = 150.dp.toPx()
            for (x in 0..(size.width / spacing).toInt() + 1) {
                for (y in 0..(size.height / spacing).toInt() + 1) {
                    val px = x * spacing
                    val py = y * spacing
                    drawCircle(color = Primary, radius = 2.dp.toPx(), center = Offset(px, py))
                    drawLine(color = Primary, start = Offset(px, py), end = Offset(px + spacing, py), strokeWidth = stroke.width)
                    drawLine(color = Primary, start = Offset(px, py), end = Offset(px, py + spacing), strokeWidth = stroke.width)
                }
            }
        }

        // Blurs
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-50).dp)
                    .size(200.dp)
                    .background(Primary.copy(alpha = 0.1f), CircleShape)
                    .blur(40.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = 50.dp)
                    .size(200.dp)
                    .background(Primary.copy(alpha = 0.15f), CircleShape)
                    .blur(40.dp)
            )
        }

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Logo / Icon
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().alpha(pulse)) {
                    val leftWing = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.28f)
                        cubicTo(size.width * 0.35f, size.height * 0.28f, size.width * 0.22f, size.height * 0.38f, size.width * 0.22f, size.height * 0.5f)
                        cubicTo(size.width * 0.22f, size.height * 0.6f, size.width * 0.3f, size.height * 0.7f, size.width * 0.38f, size.height * 0.76f)
                        cubicTo(size.width * 0.41f, size.height * 0.82f, size.width * 0.45f, size.height * 0.88f, size.width * 0.5f, size.height * 0.93f)
                        close()
                    }
                    val rightWing = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.28f)
                        cubicTo(size.width * 0.65f, size.height * 0.28f, size.width * 0.78f, size.height * 0.38f, size.width * 0.78f, size.height * 0.5f)
                        cubicTo(size.width * 0.78f, size.height * 0.63f, size.width * 0.68f, size.height * 0.73f, size.width * 0.58f, size.height * 0.82f)
                        cubicTo(size.width * 0.55f, size.height * 0.87f, size.width * 0.52f, size.height * 0.9f, size.width * 0.5f, size.height * 0.95f)
                        close()
                    }
                    drawPath(leftWing, color = Primary.copy(alpha = 0.9f))
                    drawPath(rightWing, color = Primary)
                    
                    drawCircle(color = Color.White.copy(alpha = 0.45f * shimmer), radius = 4.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.45f))
                    drawCircle(color = Color.White.copy(alpha = 0.45f * shimmer), radius = 3.dp.toPx(), center = Offset(size.width * 0.37f, size.height * 0.6f))
                    drawCircle(color = Color.White.copy(alpha = 0.45f * shimmer), radius = 3.dp.toPx(), center = Offset(size.width * 0.62f, size.height * 0.74f))
                    rotate(degrees = 20f, pivot = Offset(size.width * 0.88f, size.height * 0.5f)) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.28f),
                            start = Offset(size.width * 0.77f, size.height * 0.41f),
                            end = Offset(size.width * 0.87f, size.height * 0.48f),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Row {
                Text(
                    text = "Reel",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
                Text(
                    text = "Ed",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LEARNING THROUGH MOTION",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bottom Progress Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .fillMaxWidth(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .background(Primary, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Preparing systems", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(Primary.copy(alpha = if (pulse > 0.5f) 1f else 0.3f), CircleShape))
                    Box(modifier = Modifier.size(4.dp).background(Primary.copy(alpha = if (pulse > 0.7f) 1f else 0.3f), CircleShape))
                    Box(modifier = Modifier.size(4.dp).background(Primary.copy(alpha = if (pulse > 0.9f) 1f else 0.3f), CircleShape))
                }
            }
        }

        Text(
            text = "© 2024 REELED INC.",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}
