package com.reeled.quizoverlay.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizCardType
import com.reeled.quizoverlay.ui.overlay.cards.DragDropMatchCard
import com.reeled.quizoverlay.ui.overlay.cards.FillBlankCard
import com.reeled.quizoverlay.ui.overlay.cards.TapChoiceCard
import com.reeled.quizoverlay.ui.overlay.cards.TapTapMatchCard
import com.reeled.quizoverlay.ui.overlay.components.ConfettiEffect
import com.reeled.quizoverlay.ui.overlay.components.ModernQuizBackground
import com.reeled.quizoverlay.ui.overlay.components.TrainAnimation
import com.reeled.quizoverlay.ui.overlay.components.RightMascot
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

    ModernQuizBackground(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.value.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Section: Train
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.22f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        TrainAnimation(modifier = Modifier.fillMaxSize())
                    }

                    // Bottom Section: Quiz Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.78f)
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (config.cardType) {
                                QuizCardType.TAP_CHOICE -> TapChoiceCard(config, sourceApp, onResultIntercept)
                                QuizCardType.TAP_TAP_MATCH -> TapTapMatchCard(config, sourceApp, onResultIntercept)
                                QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(config, sourceApp, onResultIntercept)
                                QuizCardType.FILL_BLANK -> FillBlankCard(config, sourceApp, onResultIntercept)
                            }
                        }
                    }
                }

                // Floating Mascot: Using RightMascot (arrow_book.riv)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = 16.dp)
                        .size(120.dp)
                ) {
                    RightMascot(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (showConfetti) {
                    ConfettiEffect(
                        modifier = Modifier.fillMaxSize(),
                        trigger = showConfetti,
                        onFinished = { showConfetti = false }
                    )
                }
            }
        }
    }
}
