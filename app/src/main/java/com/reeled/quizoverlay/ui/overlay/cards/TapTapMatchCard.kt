package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.reeled.quizoverlay.ui.overlay.components.RiveMedia
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign

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
    val leftPairs = payload.pairs

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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    leftPairs.forEach { pair ->
                        val isMatched = matchedPairs.containsKey(pair.leftId)
                        val isWrong = wrongLeft == pair.leftId
                        val isSelected = selectedLeft == pair.leftId
                        
                        val baseColor = when {
                            isMatched -> Color(0xFFC8E6C9)
                            isWrong -> Color(0xFFFFCDD2)
                            isSelected -> Color(0xFFBBDEFB)
                            else -> Color(0xFFF5F5F5)
                        }
                        
                        val contentColor = Color(0xFF0D47A1)

                        PuzzlePieceButton(
                            label = pair.leftLabel,
                            backgroundColor = baseColor,
                            contentColor = contentColor,
                            enabled = !isMatched,
                            onClick = { selectedLeft = pair.leftId }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    payload.rightOrderShuffled.forEach { rightId ->
                        val isMatched = matchedPairs.containsValue(rightId)
                        val isWrong = wrongRight == rightId
                        
                        val baseColor = when {
                            isMatched -> Color(0xFFC8E6C9)
                            isWrong -> Color(0xFFFFCDD2)
                            else -> Color(0xFFF5F5F5)
                        }
                        
                        val contentColor = Color(0xFF0D47A1)

                        PuzzlePieceButton(
                            label = rightById[rightId].orEmpty(),
                            backgroundColor = baseColor,
                            contentColor = contentColor,
                            enabled = !isMatched,
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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(if (enabled) 2.dp else 0.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
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
                color = contentColor
            ),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
