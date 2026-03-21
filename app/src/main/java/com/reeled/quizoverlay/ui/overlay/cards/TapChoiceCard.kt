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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.reeled.quizoverlay.ui.overlay.components.RiveMedia

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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Question Text
        Text(
            text = config.display.questionText,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF0D47A1),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Rive Media (Hardcoded as requested)
        RiveMedia(
            modifier = Modifier.height(180.dp)
        )

        // Options (Restore take(2) logic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            payload.options.take(2).forEach { option ->
                val bgColor = if (option.isCorrect) Color(0xFF4CAF50) else Color(0xFF2196F3)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(
                            width = 4.dp,
                            color = bgColor.copy(alpha = 0.3f),
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
                        fontSize = 32.sp,
                        color = Color(0xFF1A237E),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
