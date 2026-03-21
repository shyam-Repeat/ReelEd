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

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

@Composable
fun ChipItem(
    label: String, 
    enabled: Boolean = true, 
    backgroundColor: Color = Color(0xFFE3F2FD),
    contentColor: Color = Color(0xFF0D47A1),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .height(56.dp)
            .width(IntrinsicSize.Min)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) backgroundColor else Color(0xFFF5F5F5))
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
                fontWeight = FontWeight.Black,
                color = if (enabled) contentColor else Color(0xFF9E9E9E)
            )
        )
    }
}
