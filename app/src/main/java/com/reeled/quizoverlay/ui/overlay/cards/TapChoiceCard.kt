package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.QuizLayoutMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapChoiceCard(
    config: QuizCardConfig,
    sourceApp: String,
    _soundManager: com.reeled.quizoverlay.util.SoundManager,
    layoutMode: QuizLayoutMode,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.TapChoicePayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(false) }

    val defaultColors = listOf(
        Color(0xFFE24B4A), // Red
        Color(0xFF378ADD), // Blue
        Color(0xFFEF9F27), // Orange
        Color(0xFF7CB342)  // Green
    )

    val instruction = config.display.instructionLabel.ifBlank {
        stringResource(R.string.quiz_instruction_default)
    }
    val isHorizontal = layoutMode != QuizLayoutMode.Vertical
    val questionFont = if (layoutMode == QuizLayoutMode.Vertical) 72.sp else 56.sp

    if (isHorizontal) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PromptPanel(
                questionText = config.display.questionText,
                instruction = instruction,
                questionFont = questionFont,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
            )

            ChoiceGrid(
                options = payload.options,
                defaultColors = defaultColors,
                selectedOptionId = selectedOptionId,
                locked = locked,
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                onOptionSelected = { optionId, isCorrect ->
                    selectedOptionId = optionId
                    locked = true
                    scope.launch {
                        delay(600)
                        onResult(
                            QuizAttemptResult(
                                questionId = config.id,
                                selectedOptionId = optionId,
                                isCorrect = isCorrect,
                                wasDismissed = false,
                                wasTimerExpired = false,
                                responseTimeMs = System.currentTimeMillis() - startTime,
                                sourceApp = sourceApp
                            )
                        )
                        if (!isCorrect) {
                            delay(400)
                            selectedOptionId = null
                            locked = false
                        }
                    }
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PromptPanel(
                questionText = config.display.questionText,
                instruction = instruction,
                questionFont = questionFont,
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            ChoiceStack(
                options = payload.options,
                defaultColors = defaultColors,
                selectedOptionId = selectedOptionId,
                locked = locked,
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxWidth(),
                onOptionSelected = { optionId, isCorrect ->
                    selectedOptionId = optionId
                    locked = true
                    scope.launch {
                        delay(600)
                        onResult(
                            QuizAttemptResult(
                                questionId = config.id,
                                selectedOptionId = optionId,
                                isCorrect = isCorrect,
                                wasDismissed = false,
                                wasTimerExpired = false,
                                responseTimeMs = System.currentTimeMillis() - startTime,
                                sourceApp = sourceApp
                            )
                        )
                        if (!isCorrect) {
                            delay(400)
                            selectedOptionId = null
                            locked = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PromptPanel(
    questionText: String,
    instruction: String,
    questionFont: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = questionText,
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                fontSize = questionFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChoiceStack(
    options: List<com.reeled.quizoverlay.model.payload.ChoiceOption>,
    defaultColors: List<Color>,
    selectedOptionId: String?,
    locked: Boolean,
    modifier: Modifier = Modifier,
    onOptionSelected: (String, Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        options.forEachIndexed { index, option ->
            val color = option.color?.let { parseColor(it) } ?: defaultColors[index % defaultColors.size]
            val isSelected = selectedOptionId == option.id
            val isAnySelected = selectedOptionId != null

            ChoiceCircleButton(
                label = option.label,
                color = color,
                isSelected = isSelected,
                isDimmed = isAnySelected && !isSelected,
                enabled = !locked,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 84.dp, max = 110.dp),
                onClick = { onOptionSelected(option.id, option.isCorrect) }
            )
        }
    }
}

@Composable
private fun ChoiceGrid(
    options: List<com.reeled.quizoverlay.model.payload.ChoiceOption>,
    defaultColors: List<Color>,
    selectedOptionId: String?,
    locked: Boolean,
    modifier: Modifier = Modifier,
    onOptionSelected: (String, Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { option ->
                    val globalIndex = options.indexOf(option)
                    val color = option.color?.let { parseColor(it) } ?: defaultColors[globalIndex % defaultColors.size]
                    val isSelected = selectedOptionId == option.id
                    val isAnySelected = selectedOptionId != null

                    ChoiceCircleButton(
                        label = option.label,
                        color = color,
                        isSelected = isSelected,
                        isDimmed = isAnySelected && !isSelected,
                        enabled = !locked,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 90.dp),
                        onClick = { onOptionSelected(option.id, option.isCorrect) }
                    )
                }

                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ChoiceCircleButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isDimmed) 0.35f else 1f)
    val shape: Shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(shape)
            .background(color)
            .let {
                if (isSelected) {
                    it.border(3.dp, Color(0xFF3C3489), shape)
                        .padding(2.dp)
                        .border(4.dp, Color(0xFFCECBF6), shape)
                } else it
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF378ADD)
    }
}
