package com.reeled.quizoverlay.ui.pin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reeled.quizoverlay.util.PinHasher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinGateDialog(
    title: String = "Parent PIN required",
    subtitle: String = "Verify your identity",
    storedPinHash: String,
    isLocked: Boolean = false,
    lockoutTimeRemaining: Long = 0L,
    onPinCorrect: () -> Unit,
    onPinIncorrect: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    fun shake() {
        scope.launch {
            repeat(4) {
                shakeOffset.animateTo(10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
                shakeOffset.animateTo(-10f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
            }
            shakeOffset.animateTo(0f)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .offset(x = shakeOffset.value.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLocked) "Locked. Try again in ${lockoutTimeRemaining / 1000}s" else subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                if (!isLocked) {
                    // Numpad
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "DEL")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    NumpadKey(
                                        text = key,
                                        onClick = {
                                            when (key) {
                                                "C" -> enteredPin = ""
                                                "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        if (enteredPin.length == 4) {
                                                            if (PinHasher.verify(enteredPin, storedPinHash)) {
                                                                onPinCorrect()
                                                            } else {
                                                                onPinIncorrect()
                                                                shake()
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun NumpadKey(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}
