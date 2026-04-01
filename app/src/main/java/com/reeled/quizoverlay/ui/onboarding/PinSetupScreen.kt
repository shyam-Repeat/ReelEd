package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    onPinSet: (String) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(PinSetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val currentPin = if (step == PinSetupStep.CREATE) firstPin else secondPin
    val title = if (step == PinSetupStep.CREATE) "Create Parental PIN" else "Confirm Parental PIN"
    val subtitle = "Set a 4-digit code to protect settings and restricted content."

    fun handleInput(digit: String) {
        if (currentPin.length < 4) {
            if (step == PinSetupStep.CREATE) firstPin += digit else secondPin += digit
        }
        
        if (currentPin.length + 1 == 4) {
            scope.launch {
                delay(200)
                if (step == PinSetupStep.CREATE) {
                    step = PinSetupStep.CONFIRM
                } else {
                    if (firstPin == secondPin) {
                        onPinSet(firstPin)
                    } else {
                        // Shake and reset confirmation
                        secondPin = ""
                        repeat(4) {
                            shakeOffset.animateTo(10f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                            shakeOffset.animateTo(-10f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                        shakeOffset.animateTo(0f)
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (step == PinSetupStep.CREATE) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (secondPin.isNotEmpty()) secondPin = secondPin.dropLast(1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Security Setup",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Box(modifier = Modifier.size(48.dp))
        }

        // Progress
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Step 3 of 8", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("3 / 8", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.375f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .offset(x = shakeOffset.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(4.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.EnhancedEncryption,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(4) { index ->
                        val isFilled = index < currentPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFilled) Primary else Color.Transparent)
                                .border(
                                    2.dp,
                                    if (isFilled) Primary else Primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keyRows = listOf(
                    listOf("1" to KeyOrange, "2" to KeyGreen, "3" to KeyPurple),
                    listOf("4" to KeyPink, "5" to Primary, "6" to KeyOrange),
                    listOf("7" to KeyGreen, "8" to KeyPurple, "9" to KeyPink)
                )

                keyRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { (digit, color) ->
                            KeypadButton(digit, color) { handleInput(digit) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.size(64.dp))
                    KeypadButton("0", MaterialTheme.colorScheme.surfaceVariant) { handleInput("0") }
                    IconButton(
                        onClick = { handleBackspace() },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (color == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurface else color)
        }
    }
}

enum class PinSetupStep {
    CREATE, CONFIRM
}
