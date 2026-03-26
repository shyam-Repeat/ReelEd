package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload

@Composable
fun DrawMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.DrawMatchPayload
    val startTime = remember { System.currentTimeMillis() }
    
    // 2. State Management
    // Use a trigger for redrawing instead of just counting points
    var redrawTrigger by remember { mutableStateOf(0) }
    val drawnPath = remember { Path() }
    var isCompleted by remember { mutableStateOf(false) }
    var pointsCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val instruction = config.display.instructionLabel.ifBlank {
            stringResource(R.string.quiz_instruction_default)
        }

        // 3. UI Consistency: Instruction Section (Matches TapChoiceCard pattern)
        Box(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Drawing Area (Matches weight pattern of other cards)
        Box(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Background reference character
            Text(
                text = payload.text,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 280.sp, // Slightly reduced to ensure it fits common screens
                fontWeight = FontWeight.Black,
                color = Color(0xFFF0F0F0),
                textAlign = TextAlign.Center
            )

            // Interactive Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isCompleted) {
                        if (isCompleted) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                drawnPath.moveTo(offset.x, offset.y)
                                redrawTrigger++
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                drawnPath.lineTo(change.position.x, change.position.y)
                                pointsCount++
                                redrawTrigger++
                                
                                // Heuristic: Completion threshold
                                if (pointsCount > 50 && !isCompleted) {
                                    isCompleted = true
                                    onResult(
                                        QuizAttemptResult(
                                            questionId = config.id,
                                            selectedOptionId = "DRAW_SUCCESS",
                                            isCorrect = true,
                                            wasDismissed = false,
                                            wasTimerExpired = false,
                                            responseTimeMs = System.currentTimeMillis() - startTime,
                                            sourceApp = sourceApp
                                        )
                                    )
                                }
                            }
                        )
                    }
            ) {
                // Accessing redrawTrigger forces the canvas to redraw when paths change
                @Suppress("UNUSED_EXPRESSION")
                redrawTrigger
                
                drawPath(
                    path = drawnPath,
                    color = Color(0xFF378ADD), // Standard blue used in other cards
                    style = Stroke(
                        width = 20.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            
            if (isCompleted) {
                Text(
                    text = "✓",
                    fontSize = 120.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
