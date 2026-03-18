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
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.ui.theme.QuizBlue

@Composable
fun ChipItem(
    label: String, 
    enabled: Boolean = true, 
    backgroundColor: Color = QuizBlue,
    contentColor: Color = Color.Black,
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

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .height(52.dp)
            .width(IntrinsicSize.Min)
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(if (enabled) shadowColor else Color(0xFFCDD3DF), RoundedCornerShape(24.dp))
        )
        
        // Chip layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 4.dp)
                .offset(y = if (isPressed) 4.dp else 0.dp)
                .background(if (enabled) backgroundColor else Color(0xFFE0E3EA), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) contentColor else Color.Gray
                )
            )
        }
    }
}
