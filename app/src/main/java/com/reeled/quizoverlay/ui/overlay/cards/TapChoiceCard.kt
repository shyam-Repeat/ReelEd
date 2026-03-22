package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.components.RiveMedia
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
    var locked by remember { mutableStateOf(false) }

    val optionColors = listOf(
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFF48FB1), // Pink
        Color(0xFF81D4FA), // Blue
        Color(0xFFA5D6A7)  // Green
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = config.display.questionText,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0D47A1),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = config.display.instructionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Options Grid (2x2)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = payload.options.chunked(2)
            rows.forEachIndexed { rowIndex, rowOptions ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEachIndexed { colIndex, option ->
                        val colorIndex = (rowIndex * 2 + colIndex) % optionColors.size
                        val baseColor = optionColors[colorIndex]
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(baseColor)
                                .border(
                                    width = 4.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = !locked) {
                                    locked = true
                                    scope.launch {
                                        delay(500)
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
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp,
                                color = Color(0xFF1A237E),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
