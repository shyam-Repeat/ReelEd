package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.ui.pin.NumpadKey
import com.reeled.quizoverlay.util.PinHasher
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    pinPrefs: PinPrefs,
    onPinSet: () -> Unit
) {
    var step by remember { mutableStateOf(SetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
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

    val currentPin = if (step == SetupStep.CREATE) firstPin else secondPin
    val title = if (step == SetupStep.CREATE) "Create Parent PIN" else "Confirm Parent PIN"
    val subtitle = if (step == SetupStep.CREATE) 
        "Create a 4-digit PIN to access parent controls." 
        else "Enter the PIN again to confirm."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .offset(x = shakeOffset.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(48.dp))

        // PIN Dots
        Row(
            horizontalArrangement = Modifier.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < currentPin.length
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

        Spacer(modifier = Modifier.height(64.dp))

        // Numpad
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "DEL")
        Column(verticalArrangement = Modifier.spacedBy(12.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Modifier.spacedBy(12.dp)) {
                    row.forEach { key ->
                        NumpadKey(
                            text = key,
                            onClick = {
                                if (key == "C") {
                                    if (step == SetupStep.CREATE) firstPin = "" else secondPin = ""
                                } else if (key == "DEL") {
                                    if (step == SetupStep.CREATE) {
                                        if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                                    } else {
                                        if (secondPin.isNotEmpty()) secondPin = secondPin.dropLast(1)
                                    }
                                } else {
                                    if (step == SetupStep.CREATE) {
                                        if (firstPin.length < 4) {
                                            firstPin += key
                                            if (firstPin.length == 4) step = SetupStep.CONFIRM
                                        }
                                    } else {
                                        if (secondPin.length < 4) {
                                            secondPin += key
                                            if (secondPin.length == 4) {
                                                if (firstPin == secondPin) {
                                                    scope.launch {
                                                        pinPrefs.savePinHash(PinHasher.hash(secondPin))
                                                        onPinSet()
                                                    }
                                                } else {
                                                    shake()
                                                    secondPin = ""
                                                    step = SetupStep.CREATE
                                                    firstPin = ""
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
}

enum class SetupStep {
    CREATE, CONFIRM
}
