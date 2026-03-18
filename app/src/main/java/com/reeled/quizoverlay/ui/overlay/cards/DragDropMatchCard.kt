package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

            if (!config.rules.strictMode) ParentCornerButton()
        }
    }
}

@Composable
fun SmallTactileButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val shadowColor = Color(
        (color.red * 0.8f).coerceIn(0f, 1f),
        (color.green * 0.8f).coerceIn(0f, 1f),
        (color.blue * 0.8f).coerceIn(0f, 1f),
        color.alpha
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(IntrinsicSize.Min)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(if (enabled) shadowColor else Color(0xFFCDD3DF), RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 4.dp)
                .offset(y = if (isPressed) 4.dp else 0.dp)
                .background(if (enabled) color else Color(0xFFE0E3EA), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) Color.White else Color.Gray
                )
            )
        }
    }
}
