package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OptionButton(
    label: String,
    enabled: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val shadowColor = Color(
        (backgroundColor.red * 0.8f).coerceIn(0f, 1f),
        (backgroundColor.green * 0.8f).coerceIn(0f, 1f),
        (backgroundColor.blue * 0.8f).coerceIn(0f, 1f),
        backgroundColor.alpha
    )

    val contentAlpha = if (enabled) 1f else 0.5f
    val effectiveBgColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.6f)
    val effectiveShadowColor = if (enabled) shadowColor else shadowColor.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Shadow layer (the "3D" part)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(effectiveShadowColor, RoundedCornerShape(24.dp))
        )
        
        // Main Button layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 4.dp)
                .offset(y = if (isPressed) 4.dp else 0.dp)
                .background(effectiveBgColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.Black.copy(alpha = contentAlpha)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
