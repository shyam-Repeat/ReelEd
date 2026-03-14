package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DragDropMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val payload = config.payload as QuizPayload.DragDropPayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    var selectedChipId by remember { mutableStateOf<String?>(null) }
    var slotContents by remember { mutableStateOf(payload.slots.associate { it.slotId to null as String? }) }
    var evaluated by remember { mutableStateOf(false) }

    val chipById = payload.chips.associateBy { it.chipId }
    val allFilled = slotContents.values.all { it != null }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(config.display.questionText, fontWeight = FontWeight.Bold)
        Text(config.display.instructionLabel, color = Color.Gray)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            payload.chips.forEach { chip ->
                val inSlot = slotContents.values.contains(chip.chipId)
                if (!inSlot) {
                    ChipItem(label = chip.label, enabled = !evaluated) { selectedChipId = chip.chipId }
                }
            }
        }

        payload.slots.forEach { slot ->
            val placedChip = chipById[slotContents[slot.slotId]]
            val isCorrect = placedChip?.chipId == slot.correctChipId
            val fill = if (!evaluated) Color.White else if (isCorrect) Color(0xFFD7F5E1) else Color(0xFFFFD8D8)
            Surface(
                color = fill,
                border = BorderStroke(1.dp, Color(0xFFCDD3DF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${slot.slotLabel}: ${placedChip?.label ?: "___"}")
                    Button(onClick = {
                        if (evaluated) return@Button
                        if (placedChip != null) {
                            slotContents = slotContents + (slot.slotId to null)
                        } else if (selectedChipId != null) {
                            val chipId = selectedChipId
                            slotContents = slotContents.mapValues { if (it.value == chipId) null else it.value } + (slot.slotId to chipId)
                            selectedChipId = null
                        }
                    }) {
                        Text(if (placedChip != null) "Clear" else "Place")
                    }
                }
            }
        }

        if (allFilled && !evaluated) {
            Button(onClick = {
                scope.launch {
                    evaluated = true
                    delay(1500)
                    onResult(
                        QuizAttemptResult(
                            questionId = config.id,
                            selectedOptionId = "SUBMIT",
                            isCorrect = payload.slots.all { slotContents[it.slotId] == it.correctChipId },
                            wasDismissed = false,
                            wasTimerExpired = false,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            sourceApp = sourceApp
                        )
                    )
                }
            }) { Text("Submit") }
        }
    }
}
