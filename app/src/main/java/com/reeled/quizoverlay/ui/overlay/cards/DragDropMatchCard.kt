package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.ui.overlay.components.ChipItem
import com.reeled.quizoverlay.ui.overlay.components.ParentCornerButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

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

    // Logic: Simple for kids - usually 1 target slot and multiple draggables
    val targetSlot = payload.targets.firstOrNull() ?: return
    val draggables = payload.draggables
    
    var matchedChipId by remember { mutableStateOf<String?>(null) }
    var evaluated by remember { mutableStateOf(false) }

    // Slot position for hit testing
    var slotCenter by remember { mutableStateOf(Offset.Zero) }
    val slotSize = 120.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(16.dp)
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

            // Game Area - Utilizing full remaining space (Fix for Point 2: Dead Space)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val areaWidth = with(density) { maxWidth.toPx() }
                val areaHeight = with(density) { maxHeight.toPx() }
                
                // Calculate random offsets once the constraints are known
                val draggableOffsets = remember(draggables, areaWidth, areaHeight) {
                    draggables.map {
                        var x: Float
                        var y: Float
                        val centerX = areaWidth / 2
                        val centerY = areaHeight / 2
                        do {
                            // Spread across the whole BoxWithConstraints area
                            x = (Random.nextFloat() * areaWidth) - centerX
                            y = (Random.nextFloat() * areaHeight) - centerY
                            
                            // Distance from the center slot
                            val distSq = x * x + y * y
                            val minClearance = with(density) { 100.dp.toPx() }
                        } while (distSq < minClearance * minClearance)
                        Offset(x, y)
                    }
                }

                // Drop Target (Center)
                Box(
                    modifier = Modifier
                        .size(slotSize)
                        .align(Alignment.Center)
                        .onGloballyPositioned {
                            val pos = it.positionInParent()
                            slotCenter = Offset(pos.x + it.size.width / 2, pos.y + it.size.height / 2)
                        }
                        .clip(CircleShape)
                        .background(
                            if (matchedChipId != null) Color(0xFFC8E6C9) 
                            else Color(0x1A0D47A1) // Soft translucent blue
                        )
                        .border(
                            width = 4.dp,
                            color = if (matchedChipId != null) Color(0xFF4CAF50) else Color(0xFFBBDEFB).copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (matchedChipId != null) {
                        val matchedDraggable = draggables.find { it.chipId == matchedChipId }
                        Text(
                            text = matchedDraggable?.label ?: "",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        // FIX for Point 3: Grayed out/faint version of the correct answer
                        val correctDraggable = draggables.find { it.chipId in targetSlot.correctChipIds }
                        Text(
                            text = correctDraggable?.label ?: "?",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0x22000000) // Faint "faded" look
                        )
                    }
                }

                if (matchedChipId != null && evaluated) {
                    Text(
                        text = "✓ Correct Match!",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (slotSize / 2) + 32.dp)
                    )
                }

                // Draggable Items scattered everywhere within the Box area
                draggables.forEachIndexed { index, draggable ->
                    if (draggable.chipId != matchedChipId && index < draggableOffsets.size) {
                        DraggableChip(
                            label = draggable.label,
                            initialOffset = draggableOffsets[index],
                            onDropped = { offset ->
                                val distance = (offset - slotCenter).getDistance()
                                if (distance < 160f && draggable.chipId in targetSlot.correctChipIds) {
                                    matchedChipId = draggable.chipId
                                    if (!evaluated) {
                                        evaluated = true
                                        scope.launch {
                                            delay(1200)
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

@Composable
fun BoxScope.DraggableChip(
    label: String,
    initialOffset: Offset,
    onDropped: (Offset) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var currentPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(
                    (initialOffset.x + dragOffset.value.x).roundToInt(),
                    (initialOffset.y + dragOffset.value.y).roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val absolutePos = currentPos + dragOffset.value
                        if (!onDropped(absolutePos)) {
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
                currentPos = Offset(
                    pos.x + it.size.width / 2,
                    pos.y + it.size.height / 2
                )
            }
    ) {
        ChipItem(
            label = label,
            backgroundColor = Color(0xFFBBDEFB),
            contentColor = Color(0xFF0D47A1),
            onClick = {} // Dragging handles it
        )
    }
}
