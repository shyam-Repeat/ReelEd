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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.ui.theme.QuizBlue

@Composable
fun ChipItem(
    label: String, 
    enabled: Boolean = true, 
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .height(52.dp)
            .width(IntrinsicSize.Min)
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) backgroundColor else Color.White.copy(alpha = 0.05f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) contentColor else Color.White.copy(alpha = 0.3f)
            )
        )
    }
}
