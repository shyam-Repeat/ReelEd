package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.components.ChipItem
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillBlankCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.FillBlankPayload
    val parts = payload.sentenceTemplate.split("___")
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var blankFilledChipId by remember { mutableStateOf<String?>(null) }
    var revealCorrectId by remember { mutableStateOf<String?>(null) }
    var evaluated by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = QuizBackground,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = config.display.questionText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = QuizBlue,
                        lineHeight = 32.sp
                    )
                )
                Text(
                    text = config.display.instructionLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, QuizBlue.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = parts.firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    )
                    
                    val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    LargeTactileBlank(
                        label = selected?.label ?: " ? ",
                        isFilled = selected != null,
                        enabled = !evaluated,
                        onClick = { if (blankFilledChipId != null && !evaluated) blankFilledChipId = null }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = parts.getOrNull(1).orEmpty(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val chipColors = listOf(QuizBlue, QuizPurple, KeyOrange, QuizMint)
                
                payload.wordBank.forEachIndexed { index, chip ->
                    val used = blankFilledChipId == chip.chipId
                    val baseColor = chipColors[index % chipColors.size]
                    
                    ChipItem(
                        label = chip.label, 
                        enabled = !used && !evaluated,
                        backgroundColor = baseColor.copy(alpha = 0.2f),
                        contentColor = baseColor
                    ) {
                        if (blankFilledChipId == null) blankFilledChipId = chip.chipId
                    }
                }
            }

            if (blankFilledChipId != null && !evaluated) {
                Button(
                    onClick = {
                        scope.launch {
                            evaluated = true
                            val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                            val isCorrect = selected?.isCorrect == true
                            if (!isCorrect && config.rules.showCorrectOnWrong) {
                                revealCorrectId = payload.wordBank.find { it.isCorrect }?.chipId
                                delay(2000)
                            } else {
                                delay(1000)
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
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuizBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text("Check Answer", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }

            if (revealCorrectId != null || (evaluated && blankFilledChipId != null)) {
                val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
                val isCorrect = selected?.isCorrect == true
                val feedbackColor = if (isCorrect) QuizMint else KeyPink
                val feedbackText = if (isCorrect) "Awesome! Correct!" else "Nice try! Correct: ${payload.wordBank.find { it.isCorrect }?.label}"
                
                Surface(
                    color = feedbackColor.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, feedbackColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackText,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    )
                }
            }

            if (!config.rules.strictMode) ParentCornerButton()
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
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val bgColor = if (isFilled) QuizYellow else Color.White
    val shadowColor = if (isFilled) Color(0xFFCCAC00) else Color(0xFFCDD3DF)

    Box(
        modifier = Modifier
            .height(64.dp)
            .width(IntrinsicSize.Min)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(shadowColor, RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 4.dp)
                .offset(y = if (isPressed) 4.dp else 0.dp)
                .background(bgColor, RoundedCornerShape(16.dp))
                .then(if (!isFilled) Modifier.border(2.dp, QuizBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFilled) Color.Black else Color.LightGray
                )
            )
        }
    }
}
