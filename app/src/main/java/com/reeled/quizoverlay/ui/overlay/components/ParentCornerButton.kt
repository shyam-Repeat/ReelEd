package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ParentCornerButton(onDismissed: () -> Unit) {
    TextButton(onClick = onDismissed, modifier = Modifier.fillMaxWidth()) {
        Text("Dismiss")
    }
}
