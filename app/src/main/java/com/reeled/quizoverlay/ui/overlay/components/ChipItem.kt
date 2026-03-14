package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChipItem(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        color = if (enabled) Color(0xFFF7F8FB) else Color(0xFFE0E3EA),
        border = BorderStroke(1.dp, Color(0xFFCDD3DF)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(text = label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}
