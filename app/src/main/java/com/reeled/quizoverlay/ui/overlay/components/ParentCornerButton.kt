package com.reeled.quizoverlay.ui.overlay.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.ui.pin.PinActivity

@Composable
fun ParentCornerButton() {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .size(48.dp) // Large tap target
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Subtle, no ripple to avoid child's attention
                ) {
                    val intent = Intent(context, PinActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    context.startActivity(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = "Parent Control",
                modifier = Modifier.size(16.dp), // Visually small
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) // Muted
            )
        }
    }
}
