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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
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
        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Question Emoji
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getSubjectEmoji(config.subject),
                fontSize = 120.sp,
                textAlign = TextAlign.Center
            )
        }

        // Question text and instructions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = config.display.questionText,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val instruction = config.display.instructionLabel.ifBlank {
                stringResource(R.string.quiz_instruction_default)
            }
            
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }

        // Options Row at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
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
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedOptionId = option.id
                        locked = true
                        scope.launch {
                            delay(600)
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
                            if (!option.isCorrect) {
                                delay(400)
                                selectedOptionId = null
                                locked = false
                            }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isDimmed) 0.35f else 1f)
    
    Box(
        modifier = modifier
            .aspectRatio(1f) // Keep it a perfect circle
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
            .let { 
                if (isSelected) {
                    it.border(3.dp, Color(0xFF3C3489), CircleShape)
                      .padding(2.dp)
                      .border(4.dp, Color(0xFFCECBF6), CircleShape)
                } else it
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
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
