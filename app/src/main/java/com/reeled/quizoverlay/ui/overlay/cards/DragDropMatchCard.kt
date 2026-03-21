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
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DragDropMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.DragDropPayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    var selectedChipId by remember { mutableStateOf<String?>(null) }
    var slotContents by remember { mutableStateOf(payload.slots.associate { it.slotId to null as String? }) }
    var evaluated by remember { mutableStateOf(false) }

    val chipById = payload.chips.associateBy { it.chipId }
    val allFilled = slotContents.values.all { it != null }

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(config.display.questionText, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp)
        Text(config.display.instructionLabel, color = Color.White.copy(alpha = 0.7f))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            payload.chips.forEach { chip ->
                val inSlot = slotContents.values.contains(chip.chipId)
                if (!inSlot) {
                    ChipItem(label = chip.label, enabled = !evaluated) { selectedChipId = chip.chipId }
                }
            }
        }

        payload.slots.forEach { slot ->
            val placedChip = chipById[slotContents[slot.slotId]]
            val isCorrect = placedChip?.chipId in slot.correctChipIds
            
            val slotBg = when {
                !evaluated -> Color.White.copy(alpha = 0.1f)
                isCorrect -> Color.Green.copy(alpha = 0.2f)
                else -> Color.Red.copy(alpha = 0.2f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(slotBg)
                    .clickable(enabled = !evaluated) {
                        if (placedChip != null) {
                            slotContents = slotContents + (slot.slotId to null)
                        } else if (selectedChipId != null) {
                            val chipId = selectedChipId
                            slotContents = slotContents.mapValues { if (it.value == chipId) null else it.value } + (slot.slotId to chipId)
                            selectedChipId = null
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${slot.slotLabel}: ${placedChip?.label ?: "___"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = if (placedChip != null) "Clear" else "Place",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (allFilled && !evaluated) {
            Button(
                onClick = {
                    scope.launch {
                        evaluated = true
                        delay(1500)
                        onResult(
                            QuizAttemptResult(
                                questionId = config.id,
                                selectedOptionId = "SUBMIT",
                                isCorrect = payload.slots.all { slotContents[it.slotId] in it.correctChipIds },
                                wasDismissed = false,
                                wasTimerExpired = false,
                                responseTimeMs = System.currentTimeMillis() - startTime,
                                sourceApp = sourceApp
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (!config.rules.strictMode) ParentCornerButton()
    }
}
