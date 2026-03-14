package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.reeled.quizoverlay.ui.overlay.components.ChipItem
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FillBlankCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val payload = config.payload as QuizPayload.FillBlankPayload
    val parts = payload.sentenceTemplate.split("___")
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var blankFilledChipId by remember { mutableStateOf<String?>(null) }
    var revealCorrectId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = config.display.questionText, fontWeight = FontWeight.Bold)
        Text(text = config.display.instructionLabel, color = Color.Gray)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(parts.firstOrNull().orEmpty())
            val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
            OutlinedButton(onClick = { if (blankFilledChipId != null) blankFilledChipId = null }) {
                Text(selected?.label ?: "____")
            }
            Text(parts.getOrNull(1).orEmpty())
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            payload.wordBank.forEach { chip ->
                val used = blankFilledChipId == chip.chipId
                ChipItem(label = chip.label, enabled = !used) {
                    if (blankFilledChipId == null) blankFilledChipId = chip.chipId
                }
            }
        }

        if (blankFilledChipId != null) {
            Button(onClick = {
                scope.launch {
                    val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                    val isCorrect = selected?.isCorrect == true
                    if (!isCorrect && config.rules.showCorrectOnWrong) {
                        revealCorrectId = payload.wordBank.find { it.isCorrect }?.chipId
                        delay(1500)
                    } else {
                        delay(800)
                    }
                    onResult(
                        QuizAttemptResult(
                            questionId = config.id,
                            selectedOptionId = blankFilledChipId,
                            isCorrect = isCorrect,
                            wasDismissed = false,
                            wasTimerExpired = false,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            sourceApp = sourceApp
                        )
                    )
                }
            }) {
                Text("Submit")
            }
        }

        if (revealCorrectId != null) {
            val answer = payload.wordBank.find { it.chipId == revealCorrectId }?.label.orEmpty()
            Surface(border = BorderStroke(1.dp, Color(0xFF3C9A63)), color = Color(0xFFD7F5E1)) {
                Text("Correct answer: $answer", modifier = Modifier.padding(8.dp))
            }
        }

        if (!config.rules.strictMode) ParentCornerButton(onDismissed)
    }
}
