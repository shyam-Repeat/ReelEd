package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Compact Question
        Text(
            text = config.display.questionText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        // Large Options (Horizontal for compactness in 80dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            payload.options.take(2).forEach { option ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = option.label,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}
