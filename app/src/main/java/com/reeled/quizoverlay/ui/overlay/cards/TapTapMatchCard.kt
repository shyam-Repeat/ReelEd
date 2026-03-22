package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.overlay.components.RiveMedia
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

    val colors = listOf(
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFF48FB1), // Pink
        Color(0xFF81D4FA), // Blue
        Color(0xFFA5D6A7), // Green
        Color(0xFFCE93D8)  // Purple
    )

    fun onRightTap(rightId: String) {
        val leftId = selectedLeft ?: return
        val isCorrect = payload.pairs.any { it.leftId == leftId && it.rightId == rightId }
        
        if (isCorrect) {
            val updated = matchedPairs + (leftId to rightId)
            matchedPairs = updated
            selectedLeft = null
            if (updated.size == payload.pairs.size) {
                scope.launch {
                    delay(1000)
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
                delay(800)
                wrongLeft = null
                wrongRight = null
                selectedLeft = null
            }
        }
    }

    val rightById = payload.pairs.associateBy({ it.rightId }, { it.rightLabel })
    
    // Stable shuffled lists for the current session
    val shuffledLeft = remember(payload.pairs) { payload.pairs.shuffled() }
    val shuffledRight = remember(payload.rightOrderShuffled) { payload.rightOrderShuffled.shuffled() }

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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    shuffledLeft.forEachIndexed { index, pair ->
                        val isMatched = matchedPairs.containsKey(pair.leftId)
                        val isWrong = wrongLeft == pair.leftId
                        val isSelected = selectedLeft == pair.leftId
                        
                        val baseColor = when {
                            isMatched -> Color(0xFFC8E6C9)
                            isWrong -> Color(0xFFFFCDD2)
                            isSelected -> Color(0xFFE3F2FD)
                            else -> colors[index % colors.size]
                        }
                        
                        PuzzlePieceButton(
                            label = pair.leftLabel,
                            backgroundColor = baseColor,
                            contentColor = Color(0xFF1A237E),
                            enabled = !isMatched,
                            isSelected = isSelected,
                            onClick = { selectedLeft = pair.leftId }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    shuffledRight.forEachIndexed { index, rightId ->
                        val isMatched = matchedPairs.containsValue(rightId)
                        val isWrong = wrongRight == rightId
                        
                        val baseColor = when {
                            isMatched -> Color(0xFFC8E6C9)
                            isWrong -> Color(0xFFFFCDD2)
                            else -> colors[(index + 2) % colors.size]
                        }
                        
                        PuzzlePieceButton(
                            label = rightById[rightId].orEmpty(),
                            backgroundColor = baseColor,
                            contentColor = Color(0xFF1A237E),
                            enabled = !isMatched,
                            isSelected = false,
                            onClick = { onRightTap(rightId) }
                        )
                    }
                }
            }

            if (!config.rules.strictMode) ParentCornerButton()
        }
    }
}

@Composable
fun PuzzlePieceButton(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) backgroundColor else Color(0xFFF5F5F5))
            .let { 
                if (isSelected) it.background(Color(0xFFBBDEFB)) else it
            }
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
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Black,
                color = if (enabled) contentColor else Color(0xFF9E9E9E),
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
