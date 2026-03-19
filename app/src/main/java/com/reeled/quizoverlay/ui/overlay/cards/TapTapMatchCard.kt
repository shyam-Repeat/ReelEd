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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    leftPairs.forEach { pair ->
                        val isMatched = matchedPairs.containsKey(pair.leftId)
                        val isWrong = wrongLeft == pair.leftId
                        val isSelected = selectedLeft == pair.leftId
                        
                        val baseColor = when {
                            isMatched -> QuizMint
                            isWrong -> KeyPink
                            isSelected -> QuizPurple
                            else -> QuizPurple.copy(alpha = 0.2f)
                        }
                        
                        val contentColor = if (isMatched || isWrong || isSelected) Color.White else QuizPurple

                        PuzzlePieceButton(
                            label = pair.leftLabel,
                            backgroundColor = baseColor,
                            contentColor = contentColor,
                            enabled = !isMatched,
                            onClick = { selectedLeft = pair.leftId }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    payload.rightOrderShuffled.forEach { rightId ->
                        val isMatched = matchedPairs.containsValue(rightId)
                        val isWrong = wrongRight == rightId
                        
                        val baseColor = when {
                            isMatched -> QuizMint
                            isWrong -> KeyPink
                            else -> KeyOrange.copy(alpha = 0.2f)
                        }
                        
                        val contentColor = if (isMatched || isWrong) Color.White else KeyOrange

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
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val shadowColor = Color(
        (backgroundColor.red * 0.8f).coerceIn(0f, 1f),
        (backgroundColor.green * 0.8f).coerceIn(0f, 1f),
        (backgroundColor.blue * 0.8f).coerceIn(0f, 1f),
        backgroundColor.alpha
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
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
                .background(if (enabled) shadowColor else Color(0xFFCDD3DF), RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isPressed) 0.dp else 4.dp)
                .offset(y = if (isPressed) 4.dp else 0.dp)
                .background(if (enabled) backgroundColor else Color(0xFFE0E3EA), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                ),
                maxLines = 1
            )
        }
    }
}
