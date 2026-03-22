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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.Primary
import kotlinx.coroutines.delay

import com.reeled.quizoverlay.ui.overlay.components.MonkeyMascot
import com.reeled.quizoverlay.ui.overlay.components.MascotEmotion

@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Loading")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
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
        // 1. Monkey Mascot (Fills Screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pulse },
            contentAlignment = Alignment.Center
        ) {
            MonkeyMascot(
                emotion = MascotEmotion.HAPPY,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Blurs for modern UI feel (on top of mascot but subtle)
        Box(modifier = Modifier.fillMaxSize().alpha(0.5f)) {
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-50).dp)
                    .size(300.dp)
                    .background(Primary.copy(alpha = 0.12f), CircleShape)
                    .blur(60.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .size(300.dp)
                    .background(Primary.copy(alpha = 0.15f), CircleShape)
                    .blur(60.dp)
            )
        }

        // 3. Main content on top
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Title
            Row {
                Text(
                    text = "Reel",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ed",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Integrated Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Primary, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Text
            Text(
                text = "Initializing Learning Engine...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
