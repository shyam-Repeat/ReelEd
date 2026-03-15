package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.reeled.quizoverlay.model.payload.ChoiceOption
import com.reeled.quizoverlay.ui.overlay.components.OptionButton
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.overlay.components.TimerBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapChoiceCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val payload = config.payload as QuizPayload.TapChoicePayload
    val rules = config.rules
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    var selectedId by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var timerProgress by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(rules.timerSeconds) {
        if (rules.timerSeconds > 0) {
            val total = rules.timerSeconds * 10
            repeat(total) { tick ->
                delay(100)
                timerProgress = 1f - ((tick + 1) / total.toFloat())
            }
            if (!locked) {
                locked = true
                onResult(
                    QuizAttemptResult(
                        questionId = config.id,
                        selectedOptionId = null,
                        isCorrect = false,
                        wasDismissed = false,
                        wasTimerExpired = true,
                        responseTimeMs = rules.timerSeconds * 1000L,
                        sourceApp = sourceApp
                    )
                )
            }
        }
    }

    suspend fun resolve(option: ChoiceOption) {
        if (locked) return
        locked = true
        selectedId = option.id
        revealed = true
        delay(if (option.isCorrect) 800 else 1500)
        onResult(
            QuizAttemptResult(
                questionId = config.id,
                selectedOptionId = option.id,
                isCorrect = option.isCorrect,
                wasDismissed = false,
                wasTimerExpired = false,
                responseTimeMs = System.currentTimeMillis() - startTime,
                sourceApp = sourceApp
            )
        )
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (rules.timerSeconds > 0) TimerBar(progress = timerProgress, modifier = Modifier.fillMaxWidth())
        Text(text = config.display.questionText, fontWeight = FontWeight.Bold)
        Text(text = config.display.instructionLabel, color = Color.Gray)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(payload.options, key = { it.id }) { option ->
                val bg = when {
                    !revealed && selectedId == option.id -> Color(0xFFDCE8FF)
                    revealed && option.isCorrect -> Color(0xFFD7F5E1)
                    revealed && selectedId == option.id && !option.isCorrect -> Color(0xFFFFD8D8)
                    else -> Color.White
                }
                OptionButton(
                    label = option.label,
                    enabled = !locked,
                    backgroundColor = bg,
                    onClick = { scope.launch { resolve(option) } }
                )
            }
        }

        if (!rules.strictMode) ParentCornerButton()
    }
}
