package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapTapMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.TapTapMatchPayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var matchedPairs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var wrongLeft by remember { mutableStateOf<String?>(null) }
    var wrongRight by remember { mutableStateOf<String?>(null) }

    fun onRightTap(rightId: String) {
        val leftId = selectedLeft ?: return
        val pair = payload.pairs.find { it.leftId == leftId } ?: return
        if (pair.rightId == rightId) {
            val updated = matchedPairs + (leftId to rightId)
            matchedPairs = updated
            selectedLeft = null
            if (updated.size == payload.pairs.size) {
                scope.launch {
                    delay(600)
                    onResult(
                        QuizAttemptResult(
                            questionId = config.id,
                            selectedOptionId = "ALL_MATCHED",
                            isCorrect = true,
                            wasDismissed = false,
                            wasTimerExpired = false,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            sourceApp = sourceApp
                        )
                    )
                }
            }
        } else {
            scope.launch {
                wrongLeft = leftId
                wrongRight = rightId
                delay(600)
                wrongLeft = null
                wrongRight = null
                selectedLeft = null
            }
        }
    }

    val rightById = payload.pairs.associateBy({ it.rightId }, { it.rightLabel })
    val leftPairs = payload.pairs

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = config.display.questionText, fontWeight = FontWeight.Bold)
        Text(text = config.display.instructionLabel, color = Color.Gray)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                leftPairs.forEach { pair ->
                    val bg = when {
                        matchedPairs.containsKey(pair.leftId) -> Color(0xFFD7F5E1)
                        wrongLeft == pair.leftId -> Color(0xFFFFD8D8)
                        selectedLeft == pair.leftId -> Color(0xFFDCE8FF)
                        else -> Color.White
                    }
                    Button(
                        onClick = { if (!matchedPairs.containsKey(pair.leftId)) selectedLeft = pair.leftId },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !matchedPairs.containsKey(pair.leftId)
                    ) { Text(pair.leftLabel, color = bg) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                payload.rightOrderShuffled.forEach { rightId ->
                    val matched = matchedPairs.containsValue(rightId)
                    val bg = when {
                        matched -> Color(0xFFD7F5E1)
                        wrongRight == rightId -> Color(0xFFFFD8D8)
                        else -> Color.White
                    }
                    Button(
                        onClick = { if (!matched) onRightTap(rightId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !matched
                    ) { Text(rightById[rightId].orEmpty(), color = bg) }
                }
            }
        }

        if (!config.rules.strictMode) ParentCornerButton()
    }
}
