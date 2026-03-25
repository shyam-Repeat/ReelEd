package com.reeled.quizoverlay.ui.overlay.cards

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.model.payload.DragChip
import com.reeled.quizoverlay.ui.overlay.components.ChipItem
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private data class ChipPlacement(
    val chipId: String,
    val offset: Offset
)

@Composable
fun DragDropMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as? QuizPayload.DragDropPayload ?: return
    val scope = rememberCoroutineScope()
    val startTime = remember { System.currentTimeMillis() }
    val density = LocalDensity.current
    val context = LocalContext.current

    // Supabase payload uses `draggables` + `targets` (+ `correct_pairs` parsed in QuizCardConfig).
    val targetSlot = payload.targets.firstOrNull() ?: return
    val draggables = payload.draggables

    var matchedChipId by remember { mutableStateOf<String?>(null) }
    var evaluated by remember { mutableStateOf(false) }
    var slotCenter by remember { mutableStateOf(Offset.Zero) }
    val slotSize = 116.dp
    var ttsReady by remember { mutableStateOf(false) }
    val textToSpeech = remember {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    LaunchedEffect(textToSpeech, ttsReady) {
        if (ttsReady) {
            textToSpeech.language = Locale.US
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val speakChipLabel: (String) -> Unit = remember(textToSpeech, ttsReady) {
        { label ->
            if (ttsReady) {
                textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, "drag-chip-$label")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                val areaWidth = with(density) { maxWidth.toPx() }
                val areaHeight = with(density) { maxHeight.toPx() }
                val slotRadiusPx = with(density) { (slotSize / 2).toPx() }
                val chipSpacingPx = with(density) { 84.dp.toPx() }
                val chipMarginPx = with(density) { 40.dp.toPx() }
                val placements = remember(draggables, areaWidth, areaHeight) {
                    buildChipPlacements(
                        draggables = draggables,
                        areaWidth = areaWidth,
                        areaHeight = areaHeight,
                        centerClearancePx = slotRadiusPx + chipSpacingPx,
                        chipSpacingPx = chipSpacingPx,
                        edgeMarginPx = chipMarginPx
                    )
                }

                Box(
                    modifier = Modifier
                        .size(slotSize)
                        .align(Alignment.Center)
                        .onGloballyPositioned {
                            val pos = it.positionInParent()
                            slotCenter = Offset(pos.x + it.size.width / 2f, pos.y + it.size.height / 2f)
                        }
                        .clip(CircleShape)
                        .background(if (matchedChipId != null) Color(0xFFC8E6C9) else Color(0x1A0D47A1))
                        .border(
                            width = 4.dp,
                            color = if (matchedChipId != null) Color(0xFF4CAF50) else Color(0xFFBBDEFB).copy(alpha = 0.65f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val centerLabel = if (matchedChipId != null) {
                        draggables.find { it.chipId == matchedChipId }?.label.orEmpty()
                    } else {
                        draggables.find { it.chipId in targetSlot.correctChipIds }?.label ?: "?"
                    }

                    Text(
                        text = centerLabel,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = if (matchedChipId != null) Color(0xFF2E7D32) else Color(0x22000000),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 88.dp)
                    )
                }

                if (matchedChipId != null && evaluated) {
                    Text(
                        text = "✓ Correct Match!",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (slotSize / 2) + 28.dp)
                    )
                }

                draggables.forEach { draggable ->
                    val placement = placements.firstOrNull { it.chipId == draggable.chipId } ?: return@forEach
                    if (draggable.chipId != matchedChipId) {
                        DraggableChip(
                            label = draggable.label,
                            initialOffset = placement.offset,
                            onChipPressed = speakChipLabel,
                            onDropped = { center ->
                                val distance = (center - slotCenter).getDistance()
                                val dropTolerancePx = with(density) { 18.dp.toPx() }
                                
                                if (distance <= slotRadiusPx + dropTolerancePx) {
                                    if (draggable.chipId in targetSlot.correctChipIds) {
                                        matchedChipId = draggable.chipId
                                        if (!evaluated) {
                                            evaluated = true
                                            scope.launch {
                                                delay(350)
                                                onResult(
                                                    QuizAttemptResult(
                                                        questionId = config.id,
                                                        selectedOptionId = draggable.chipId,
                                                        isCorrect = true,
                                                        wasDismissed = false,
                                                        wasTimerExpired = false,
                                                        responseTimeMs = System.currentTimeMillis() - startTime,
                                                        sourceApp = sourceApp
                                                    )
                                                )
                                            }
                                        }
                                        true
                                    } else {
                                        // WRONG DROP detected
                                        onResult(
                                            QuizAttemptResult(
                                                questionId = config.id,
                                                selectedOptionId = draggable.chipId,
                                                isCorrect = false,
                                                wasDismissed = false,
                                                wasTimerExpired = false,
                                                responseTimeMs = System.currentTimeMillis() - startTime,
                                                sourceApp = sourceApp
                                            )
                                        )
                                        false // Return to original position
                                    }
                                } else {
                                    false
                                }
                            }
                        )
                    }
                }
            }
        }

        if (!config.rules.strictMode) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                ParentCornerButton()
            }
        }
    }
}

private fun buildChipPlacements(
    draggables: List<DragChip>,
    areaWidth: Float,
    areaHeight: Float,
    centerClearancePx: Float,
    chipSpacingPx: Float,
    edgeMarginPx: Float
): List<ChipPlacement> {
    if (draggables.isEmpty() || areaWidth <= 0f || areaHeight <= 0f) return emptyList()

    val center = Offset(areaWidth / 2f, areaHeight / 2f)
    val minX = -center.x + edgeMarginPx
    val maxX = center.x - edgeMarginPx
    val minY = -center.y + edgeMarginPx
    val maxY = center.y - edgeMarginPx
    val random = Random(draggables.joinToString("|") { it.chipId }.hashCode())
    val placements = mutableListOf<ChipPlacement>()

    draggables.forEachIndexed { index, draggable ->
        var chosen: Offset? = null
        repeat(100) {
            val candidate = Offset(
                x = random.nextFloat() * (maxX - minX) + minX,
                y = random.nextFloat() * (maxY - minY) + minY
            )
            val distanceFromCenter = sqrt(candidate.x * candidate.x + candidate.y * candidate.y)
            val farEnoughFromCenter = distanceFromCenter >= centerClearancePx
            val farEnoughFromOthers = placements.none { placed ->
                (placed.offset - candidate).getDistance() < chipSpacingPx
            }
            if (farEnoughFromCenter && farEnoughFromOthers) {
                chosen = candidate
                return@repeat
            }
        }

        if (chosen == null) {
            val orbit = centerClearancePx + (index % 3) * (chipSpacingPx * 0.35f)
            val angle = (index.toFloat() / max(draggables.size, 1)) * (Math.PI.toFloat() * 2f)
            chosen = Offset(
                x = (kotlin.math.cos(angle) * orbit).coerceIn(minX, maxX),
                y = (kotlin.math.sin(angle) * orbit).coerceIn(minY, maxY)
            )
        }

        placements += ChipPlacement(chipId = draggable.chipId, offset = chosen!!)
    }

    return placements
}

@Composable
fun BoxScope.DraggableChip(
    label: String,
    initialOffset: Offset,
    onChipPressed: (String) -> Unit,
    onDropped: (Offset) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var baseCenter by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(
                    (initialOffset.x + dragOffset.value.x).roundToInt(),
                    (initialOffset.y + dragOffset.value.y).roundToInt()
                )
            }
            .pointerInput(label, initialOffset) {
                detectDragGestures(
                    onDragStart = { onChipPressed(label) },
                    onDragEnd = {
                        val center = (baseCenter ?: Offset.Zero) + dragOffset.value
                        if (!onDropped(center)) {
                            scope.launch {
                                dragOffset.animateTo(Offset.Zero, spring())
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo(dragOffset.value + dragAmount)
                        }
                    }
                )
            }
            .onGloballyPositioned {
                val pos = it.positionInParent()
                baseCenter = Offset(
                    x = pos.x + (it.size.width / 2f) - dragOffset.value.x,
                    y = pos.y + (it.size.height / 2f) - dragOffset.value.y
                )
            }
    ) {
        ChipItem(
            label = label,
            onClick = { onChipPressed(label) }
        )
    }
}
