package com.reeled.quizoverlay.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
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
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.cards.DragDropMatchCard
import com.reeled.quizoverlay.ui.overlay.cards.DrawMatchCard
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
    forceQuizEnabled: Boolean = false,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit,
    onInvalidPayload: (questionId: String, reason: String) -> Unit
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

    // Auto-read question text shortly after train intro, so JSON question text is always spoken.
    LaunchedEffect(config.id) {
        delay(1700)
        if (!quizFinished) {
            soundManager.speak(config.display.questionText)
        }
    }

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
                
                if (!forceQuizEnabled && wrongAttempts >= 3) {
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val layoutMode = resolveQuizLayoutMode(maxWidth, maxHeight)
                    val isHorizontal = layoutMode != QuizLayoutMode.Vertical
                    val mascotSize = if (isHorizontal) 108.dp else 160.dp
                    val timerPadding = if (isHorizontal) 24.dp else 40.dp
                    val bodyPaddingTop = if (isHorizontal) 8.dp else 0.dp

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (totalTimerSeconds > 0) {
                            TimerBar(
                                progress = timeLeft / totalTimerSeconds,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = timerPadding, vertical = 12.dp)
                            )
                        }

                        if (isHorizontal) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = bodyPaddingTop)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(if (layoutMode == QuizLayoutMode.HorizontalWide) 0.30f else 0.36f)
                                        .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(mascotSize)
                                            .padding(top = 4.dp)
                                    ) {
                                        RightMascot(modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.weight(0.08f))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(0.92f)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        TrainAnimation(
                                            modifier = Modifier.fillMaxSize(),
                                            onStart = { soundManager.playTrain(1500) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(if (layoutMode == QuizLayoutMode.HorizontalWide) 0.70f else 0.64f)
                                        .fillMaxSize()
                                ) {
                                    QuizCardContent(
                                        config = config,
                                        sourceApp = sourceApp,
                                        soundManager = soundManager,
                                        layoutMode = layoutMode,
                                        onResultIntercept = onResultIntercept,
                                        onInvalidPayload = onInvalidPayload
                                    )
                                }
                            }
                        } else {
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.75f)
                            ) {
                                QuizCardContent(
                                    config = config,
                                    sourceApp = sourceApp,
                                    soundManager = soundManager,
                                    layoutMode = layoutMode,
                                    onResultIntercept = onResultIntercept,
                                    onInvalidPayload = onInvalidPayload
                                )
                            }
                        }
                    }

                    if (!isHorizontal) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 80.dp, end = 12.dp)
                                .size(mascotSize)
                        ) {
                            RightMascot(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (showConfetti) {
                    ConfettiEffect(
                        modifier = Modifier.fillMaxSize(),
                        trigger = showConfetti,
                        onFinished = { showConfetti = false }
                    )
                }

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

@Composable
private fun QuizCardContent(
    config: QuizCardConfig,
    sourceApp: String,
    soundManager: SoundManager,
    layoutMode: QuizLayoutMode,
    onResultIntercept: (QuizAttemptResult) -> Unit,
    onInvalidPayload: (questionId: String, reason: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (config.cardType) {
            QuizCardType.TAP_CHOICE -> {
                if (config.payload is QuizPayload.TapChoicePayload) {
                    TapChoiceCard(config, sourceApp, soundManager, layoutMode, onResultIntercept)
                } else {
                    InvalidPayloadGate(config.id, "tap_choice_payload_mismatch", onInvalidPayload)
                }
            }
            QuizCardType.TAP_TAP_MATCH -> {
                if (config.payload is QuizPayload.TapTapMatchPayload) {
                    TapTapMatchCard(config, sourceApp, soundManager, layoutMode, onResultIntercept)
                } else {
                    InvalidPayloadGate(config.id, "tap_tap_match_payload_mismatch", onInvalidPayload)
                }
            }
            QuizCardType.DRAG_DROP_MATCH -> {
                if (config.payload is QuizPayload.DragDropPayload) {
                    DragDropMatchCard(config, sourceApp, layoutMode, onResultIntercept)
                } else {
                    InvalidPayloadGate(config.id, "drag_drop_payload_mismatch", onInvalidPayload)
                }
            }
            QuizCardType.FILL_BLANK -> {
                if (config.payload is QuizPayload.FillBlankPayload) {
                    FillBlankCard(config, sourceApp, soundManager, layoutMode, onResultIntercept)
                } else {
                    InvalidPayloadGate(config.id, "fill_blank_payload_mismatch", onInvalidPayload)
                }
            }
            QuizCardType.DRAW_MATCH -> {
                if (config.payload is QuizPayload.DrawMatchPayload) {
                    DrawMatchCard(config, sourceApp, layoutMode, onResultIntercept)
                } else {
                    InvalidPayloadGate(config.id, "draw_match_payload_mismatch", onInvalidPayload)
                }
            }
        }
    }
}

@Composable
private fun InvalidPayloadGate(
    questionId: String,
    reason: String,
    onInvalidPayload: (questionId: String, reason: String) -> Unit
) {
    LaunchedEffect(questionId, reason) {
        onInvalidPayload(questionId, reason)
    }
}
