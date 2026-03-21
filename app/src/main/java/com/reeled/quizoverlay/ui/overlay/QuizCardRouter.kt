package com.reeled.quizoverlay.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import com.reeled.quizoverlay.ui.overlay.components.ConfettiEffect
import com.reeled.quizoverlay.ui.overlay.components.RightMascot
import com.reeled.quizoverlay.ui.overlay.components.TrainAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showConfetti by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    // Intercept results
    val onResultIntercept: (QuizAttemptResult) -> Unit = { result ->
        if (result.isCorrect) {
            showConfetti = true
        } else {
            scope.launch {
                repeat(4) {
                    shakeOffset.animateTo(10f, tween(50))
                    shakeOffset.animateTo(-10f, tween(50))
                }
                shakeOffset.animateTo(0f, tween(50))
            }
        }
        scope.launch {
            delay(1500)
            onResult(result)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.value.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Train Section (Top 25%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
            ) {
                TrainAnimation(modifier = Modifier.fillMaxSize())
            }

            // 2. Quiz Content Area (Bottom 75%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = 32.dp) // centered on the divider
                    .zIndex(1f)
                    .size(64.dp)
            ) {
                RightMascot(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 4. Confetti overlay
        if (showConfetti) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize(),
                trigger = showConfetti,
                onFinished = { showConfetti = false }
            )
        }
    }
}
