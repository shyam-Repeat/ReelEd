package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

@Composable
fun ModernQuizBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundAnim")
    
    // Floating shapes animation
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShapeFloat"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFE3F2FD))) {
        // 1. Playful shapes in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Subtle clouds or circles
            drawCircle(
                color = Color(0xFFBBDEFB).copy(alpha = 0.5f),
                radius = 150f,
                center = Offset(width * 0.1f, height * 0.2f + (animValue * 50f))
            )
            
            drawCircle(
                color = Color(0xFFBBDEFB).copy(alpha = 0.4f),
                radius = 200f,
                center = Offset(width * 0.85f, height * 0.1f - (animValue * 30f))
            )
            
            drawCircle(
                color = Color(0xFFBBDEFB).copy(alpha = 0.3f),
                radius = 300f,
                center = Offset(width * 0.5f, height * 0.9f + (animValue * 60f))
            )
            
            drawCircle(
                color = Color(0xFF90CAF9).copy(alpha = 0.2f),
                radius = 120f,
                center = Offset(width * 0.2f, height * 0.7f - (animValue * 40f))
            )
        }
        
        // 2. The Content with Padding
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred Margin Effect
            // We draw a blurred rectangle around the 20dp padding area
            Canvas(modifier = Modifier.fillMaxSize()) {
                // This is a bit tricky with Canvas. 
                // Alternatively, we can use a Box with Modifier.blur() for the margin area.
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                content()
            }
            
            // Modern "Blur" feel for margins
            // Let's use a subtle gradient or blurred boxes at the edges
            Box(modifier = Modifier.fillMaxSize()) {
                // Top margin blur
                Box(modifier = Modifier.fillMaxWidth().height(20.dp).background(Color(0xFFE3F2FD).copy(alpha = 0.6f)).blur(8.dp))
                // Bottom margin blur
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(Color(0xFFE3F2FD).copy(alpha = 0.6f)).blur(8.dp))
                // Left margin blur
                Box(modifier = Modifier.fillMaxHeight().width(20.dp).background(Color(0xFFE3F2FD).copy(alpha = 0.6f)).blur(8.dp))
                // Right margin blur
                Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(20.dp).background(Color(0xFFE3F2FD).copy(alpha = 0.6f)).blur(8.dp))
            }
        }
    }
}
