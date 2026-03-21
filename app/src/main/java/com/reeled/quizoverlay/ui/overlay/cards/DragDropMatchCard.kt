package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.reeled.quizoverlay.ui.overlay.components.ChipItem
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.overlay.components.RiveMedia
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Rive Media (Hardcoded as requested)
            RiveMedia(
                modifier = Modifier.height(140.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                val chipColors = listOf(Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFFFF9C4), Color(0xFFF8BBD0))
                payload.chips.forEachIndexed { index, chip ->
                    val inSlot = slotContents.values.contains(chip.chipId)
                    val baseColor = chipColors[index % chipColors.size]
                    if (!inSlot) {
                        ChipItem(
                            label = chip.label,
                            enabled = !evaluated,
                            backgroundColor = baseColor,
                            contentColor = Color(0xFF0D47A1)
                        ) {
                            selectedChipId = chip.chipId
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                payload.slots.forEach { slot ->
                    val placedChip = chipById[slotContents[slot.slotId]]
                    val isCorrect = placedChip?.chipId in slot.correctChipIds
                    
                    val slotBg = when {
                        !evaluated -> Color(0xFFF5F5F5)
                        isCorrect -> Color(0xFFC8E6C9)
                        else -> Color(0xFFFFCDD2)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(slotBg)
                            .border(
                                width = if (selectedChipId != null) 2.dp else 0.dp,
                                color = Color(0xFF1E88E5),
                                shape = RoundedCornerShape(16.dp)
                            )
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
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${slot.slotLabel}: ${placedChip?.label ?: "___"}",
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            
                            Text(
                                text = if (placedChip != null) "Clear" else "Place",
                                color = Color(0xFF1E88E5),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    modifier = Modifier.fillMaxWidth(0.8f).height(64.dp).shadow(8.dp, RoundedCornerShape(32.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text("Submit", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }

            if (!config.rules.strictMode) ParentCornerButton()
        }
    }
}
