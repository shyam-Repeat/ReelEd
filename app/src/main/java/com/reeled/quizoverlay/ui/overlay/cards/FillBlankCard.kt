package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillBlankCard(
    config: QuizCardConfig,
    sourceApp: String,
    soundManager: com.reeled.quizoverlay.util.SoundManager,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.FillBlankPayload
    val parts = payload.sentenceTemplate.split("___")
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var blankFilledChipId by remember { mutableStateOf<String?>(null) }
    var revealCorrectId by remember { mutableStateOf<String?>(null) }
    var evaluated by remember { mutableStateOf(false) }

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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = parts.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E)
                    )
                )
                
                val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                LargeTactileBlank(
                    label = selected?.label ?: " ? ",
                    isFilled = selected != null,
                    enabled = !evaluated,
                    onClick = { if (blankFilledChipId != null && !evaluated) blankFilledChipId = null }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = parts.getOrNull(1).orEmpty(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E)
                    )
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                val chipColors = listOf(Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFFFF9C4), Color(0xFFF8BBD0))
                
                payload.wordBank.forEachIndexed { index, chip ->
                    val used = blankFilledChipId == chip.chipId
                    val baseColor = chipColors[index % chipColors.size]
                    
                    ChipItem(
                        label = chip.label, 
                        enabled = !used && !evaluated,
                        backgroundColor = baseColor,
                        contentColor = Color(0xFF0D47A1)
                    ) {
                        if (blankFilledChipId == null) blankFilledChipId = chip.chipId
                    }
                }
            }

            if (blankFilledChipId != null && !evaluated) {
                Button(
                    onClick = {
                        scope.launch {
                            val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                            val isCorrect = selected?.isCorrect == true
                            
                            evaluated = true // Lock UI temporarily
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
                            
                            if (!isCorrect) {
                                delay(1000)
                                evaluated = false // Re-enable for another try
                                blankFilledChipId = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(64.dp).align(Alignment.CenterHorizontally).shadow(8.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("Check Answer", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                }
            }

            if (revealCorrectId != null || (evaluated && blankFilledChipId != null)) {
                val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                val isCorrect = selected?.isCorrect == true
                val feedbackColor = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE53935)
                val feedbackText = if (isCorrect) "Awesome! Correct!" else "Nice try! Correct: ${payload.wordBank.find { it.isCorrect }?.label}"
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.CenterHorizontally)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(feedbackColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LargeTactileBlank(
    label: String,
    isFilled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val bgColor = if (isFilled) Color(0xFFE3F2FD) else Color(0xFFBBDEFB).copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .height(64.dp)
            .width(IntrinsicSize.Min)
            .shadow(if (isFilled) 4.dp else 0.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = 2.dp,
                color = Color(0xFF1E88E5),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = if (isFilled) Color(0xFF0D47A1) else Color(0xFF0D47A1).copy(alpha = 0.3f)
            )
        )
    }
}
