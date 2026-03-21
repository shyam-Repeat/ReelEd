package com.reeled.quizoverlay.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizCardType
import com.reeled.quizoverlay.ui.overlay.cards.DragDropMatchCard
import com.reeled.quizoverlay.ui.overlay.cards.FillBlankCard
import com.reeled.quizoverlay.ui.overlay.cards.TapChoiceCard
import com.reeled.quizoverlay.ui.overlay.cards.TapTapMatchCard
import com.reeled.quizoverlay.ui.overlay.components.MonkeyMascot
import com.reeled.quizoverlay.ui.overlay.components.TrainAnimation
import com.reeled.quizoverlay.ui.overlay.components.MascotEmotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    var mascotEmotion by remember { mutableStateOf(MascotEmotion.IDLE) }
    val scope = rememberCoroutineScope()

    // Intercept results to show mascot emotion briefly
    val onResultIntercept: (QuizAttemptResult) -> Unit = { result ->
        mascotEmotion = if (result.isCorrect) MascotEmotion.CORRECT else MascotEmotion.WRONG
        scope.launch {
            delay(1500) // Give user time to see mascot reaction
            onResult(result)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Train Section (Top 80dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            color = Color(0xFFF0F9FF), // Soft blue for train area
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                ) {
                    TrainAnimation(modifier = Modifier.fillMaxSize())
                }

                // 2. Quiz Content Area (Bottom 80dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(start = 12.dp, end = 40.dp) // Space for mascot
                ) {
                    when (config.cardType) {
                        QuizCardType.TAP_CHOICE -> TapChoiceCard(config, sourceApp, onResultIntercept)
                        QuizCardType.TAP_TAP_MATCH -> TapTapMatchCard(config, sourceApp, onResultIntercept)
                        QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(config, sourceApp, onResultIntercept)
                        QuizCardType.FILL_BLANK -> FillBlankCard(config, sourceApp, onResultIntercept)
                    }
                }
            }

            // 3. Mascot (Floating on divider line)
            // Specs: 64x64dp, centered on 80dp divider, -16dp from right edge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (80 - 32).dp) 
                    .zIndex(1f)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .background(Color.White, CircleShape)
                    .size(64.dp)
                    .clip(CircleShape) // GUARANTEE: No Rive background overlap
            ) {
                MonkeyMascot(
                    emotion = mascotEmotion,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
