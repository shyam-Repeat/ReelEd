package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.model.payload.ChoiceOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapChoiceCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.TapChoicePayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(false) }

    val defaultColors = listOf(
        Color(0xFFE24B4A), // Red
        Color(0xFF378ADD), // Blue
        Color(0xFFEF9F27), // Orange
        Color(0xFF7CB342)  // Green
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subject Emoji Box
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFFFF8EC), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getSubjectEmoji(config.subject),
                fontSize = 72.sp,
                lineHeight = 1.sp
            )
        }

        // Question & Instruction
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = config.display.questionText,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 0.04.sp
            )
            // Use instructionLabel as sub-question text if it's different
            if (config.display.instructionLabel.isNotBlank()) {
                Text(
                    text = config.display.instructionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            payload.options.forEachIndexed { index, option ->
                val color = option.color?.let { parseColor(it) } ?: defaultColors[index % defaultColors.size]
                val isSelected = selectedOptionId == option.id
                val isAnySelected = selectedOptionId != null
                
                ChoiceCircleButton(
                    label = option.label,
                    color = color,
                    isSelected = isSelected,
                    isDimmed = isAnySelected && !isSelected,
                    enabled = !locked,
                    onClick = {
                        selectedOptionId = option.id
                        locked = true
                        scope.launch {
                            delay(800)
                            onResult(
                                QuizAttemptResult(
                                    questionId = config.id,
                                    selectedOptionId = option.id,
                                    isCorrect = option.isCorrect,
                                    wasDismissed = false,
                                    wasTimerExpired = false,
                                    responseTimeMs = System.currentTimeMillis() - startTime,
                                    sourceApp = sourceApp
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChoiceCircleButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isDimmed) 0.35f else 1f)
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .alpha(alpha)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
            .let { 
                if (isSelected) {
                    it.border(3.dp, Color(0xFF3C3489), androidx.compose.foundation.shape.CircleShape)
                      .padding(2.dp)
                      .border(4.dp, Color(0xFFCECBF6), androidx.compose.foundation.shape.CircleShape)
                } else it
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label.length <= 3) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
        }
    }
}

fun getSubjectEmoji(subject: String): String {
    return when (subject.lowercase()) {
        "math" -> "➗"
        "english" -> "📖"
        "science" -> "🧪"
        "general" -> "🍎"
        else -> "🌟"
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF378ADD)
    }
}
