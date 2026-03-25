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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import com.reeled.quizoverlay.ui.overlay.components.TrainAnimation
import com.reeled.quizoverlay.ui.overlay.components.RightMascot
import com.reeled.quizoverlay.util.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect
import com.reeled.quizoverlay.ui.overlay.components.TimerBar

@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfetti by remember { mutableStateOf(false) }
    var quizFinished by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    val totalTimerSeconds = (if (config.rules.timerSeconds > 0) config.rules.timerSeconds else 120).toFloat()
    var timeLeft by remember { mutableStateOf(totalTimerSeconds) }
    var wrongAttempts by remember { mutableStateOf(0) }

    // Timer Loop
    LaunchedEffect(quizFinished) {
        if (quizFinished) return@LaunchedEffect
        
        val startTime = System.currentTimeMillis()
        while (timeLeft > 0 && !quizFinished) {
            delay(250)
            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
            timeLeft = (totalTimerSeconds - elapsed).coerceAtLeast(0f)
        }
        
        if (timeLeft <= 0 && !quizFinished) {
            quizFinished = true // Mark finished IMMEDIATELY
            soundManager.play("wrong")
            delay(800)
            onResult(
                QuizAttemptResult(
                    questionId = config.id,
                    selectedOptionId = "TIMER_EXPIRED",
                    isCorrect = false,
                    wasDismissed = false,
                    wasTimerExpired = true,
                    responseTimeMs = (totalTimerSeconds * 1000).toLong(),
                    sourceApp = sourceApp
                )
            )
        }
    }

    val onResultIntercept: (QuizAttemptResult) -> Unit = { result ->
        if (!quizFinished) {
            if (result.isCorrect) {
                quizFinished = true // Mark finished IMMEDIATELY
                soundManager.play("correct")
                showConfetti = true
                scope.launch {
                    delay(1500)
                    onResult(result)
                }
            } else {
                // INCORRECT ATTEMPT - Count strikes
                wrongAttempts++
                soundManager.play("wrong")
                scope.launch {
                    repeat(4) {
                        shakeOffset.animateTo(10f, tween(50))
                        shakeOffset.animateTo(-10f, tween(50))
                    }
                    shakeOffset.animateTo(0f, tween(50))
                }
                
                if (wrongAttempts >= 3) {
                    quizFinished = true // Mark finished IMMEDIATELY
                    scope.launch {
                        delay(1200)
                        onResult(result) // End quiz after 3rd strike
                    }
                }
            }
        }
    }

    ModernQuizBackground(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.value.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White // Solid white for the main card area
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Circles are already inside ModernQuizBackground Canvas
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Timer bar
                    if (totalTimerSeconds > 0) {
                        TimerBar(
                            progress = timeLeft / totalTimerSeconds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 12.dp)
                        )
                    }

                    // Top Section: Train
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.25f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        TrainAnimation(
                            modifier = Modifier.fillMaxSize(),
                            onStart = { soundManager.playTrain(1500) }
                        )
                    }

                    // Bottom Section: Quiz Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.75f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (config.cardType) {
                                QuizCardType.TAP_CHOICE -> TapChoiceCard(config, sourceApp, onResultIntercept)
                                QuizCardType.TAP_TAP_MATCH -> TapTapMatchCard(config, sourceApp, soundManager, onResultIntercept)
                                QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(config, sourceApp, onResultIntercept)
                                QuizCardType.FILL_BLANK -> FillBlankCard(config, sourceApp, onResultIntercept)
                            }
                        }
                    }
                }

                // Repositioned Mascot: Top-Right, larger, anchored
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 12.dp)
                        .size(160.dp)
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

                if (!config.rules.strictMode) {
                    ParentCornerButton(
                        sourceApp = sourceApp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, bottom = 14.dp)
                    )
                }
            }
        }
    }
}
