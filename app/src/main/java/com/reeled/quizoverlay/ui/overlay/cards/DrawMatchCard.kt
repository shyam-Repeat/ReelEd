package com.reeled.quizoverlay.ui.overlay.cards

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    
    val density = LocalDensity.current
    val drawnPath = remember { Path() }
    var pathVersion by remember { mutableIntStateOf(0) }
    var submittedSuccess by remember { mutableStateOf(false) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }

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
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val canvasWidthPx = with(density) { maxWidth.toPx().toInt().coerceAtLeast(1) }
                val canvasHeightPx = with(density) { maxHeight.toPx().toInt().coerceAtLeast(1) }
                val tolerancePx = with(density) { 22.dp.toPx().toInt() }

                val scoreTracker = remember(payload.text, canvasWidthPx, canvasHeightPx, tolerancePx) {
                    DrawScoreTracker(
                        text = payload.text,
                        width = canvasWidthPx,
                        height = canvasHeightPx,
                        tolerancePx = tolerancePx
                    )
                }

                val backgroundFontSize = minOf(maxWidth, maxHeight) * 0.58f

                Text(
                    text = payload.text,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = with(density) { backgroundFontSize.toSp() },
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE9E9E9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = maxWidth)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(submittedSuccess, scoreTracker) {
                            if (submittedSuccess) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    lastPoint = offset
                                    drawnPath.moveTo(offset.x, offset.y)
                                    scoreTracker.markPoint(offset.x.toInt(), offset.y.toInt())
                                    pathVersion++
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val previous = lastPoint
                                    val current = change.position
                                    drawnPath.lineTo(current.x, current.y)
                                    if (previous != null) {
                                        scoreTracker.markSegment(previous, current)
                                    } else {
                                        scoreTracker.markPoint(current.x.toInt(), current.y.toInt())
                                    }
                                    lastPoint = current
                                    pathVersion++
                                },
                                onDragEnd = {
                                    lastPoint = null
                                    val isAccurate = scoreTracker.isSuccessful()
                                    if (isAccurate && !submittedSuccess) {
                                        submittedSuccess = true
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
                    @Suppress("UNUSED_EXPRESSION")
                    pathVersion

                    drawPath(
                        path = drawnPath,
                        color = Color(0xFF378ADD),
                        style = Stroke(
                            width = 20.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                if (submittedSuccess) {
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
}

private class DrawScoreTracker(
    text: String,
    width: Int,
    height: Int,
    tolerancePx: Int
) {
    private val safeWidth = width.coerceAtLeast(1)
    private val safeHeight = height.coerceAtLeast(1)
    private val targetMask = renderTargetMask(text, safeWidth, safeHeight)
    private val expandedTargetMask = expandMask(targetMask, safeWidth, safeHeight, tolerancePx)
    private val drawnMask = BooleanArray(safeWidth * safeHeight)
    private val expandedDrawnMask = BooleanArray(safeWidth * safeHeight)
    private var targetCoveredCount = 0
    private var drawnCount = 0
    private var onTargetDrawnCount = 0
    private val minStrokeCount = (safeWidth * safeHeight * 0.002f).toInt().coerceAtLeast(60)
    private val targetPixelCount = targetMask.count { it }

    fun markSegment(from: Offset, to: Offset) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val steps = maxOf(1, kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toInt())
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = from.x + (dx * t)
            val y = from.y + (dy * t)
            markPoint(x.toInt(), y.toInt())
        }
    }

    fun markPoint(x: Int, y: Int) {
        if (x !in 0 until safeWidth || y !in 0 until safeHeight) return
        val index = y * safeWidth + x
        if (!drawnMask[index]) {
            drawnMask[index] = true
            drawnCount++
            if (expandedTargetMask[index]) {
                onTargetDrawnCount++
            }
        }
        stampExpandedDrawn(x, y, radius = 22)
    }

    fun isSuccessful(): Boolean {
        if (drawnCount < minStrokeCount || targetPixelCount == 0) return false
        val recall = targetCoveredCount.toFloat() / targetPixelCount.toFloat()
        val precision = onTargetDrawnCount.toFloat() / drawnCount.toFloat()
        return recall >= 0.90f && precision >= 0.35f
    }

    private fun stampExpandedDrawn(cx: Int, cy: Int, radius: Int) {
        val left = (cx - radius).coerceAtLeast(0)
        val right = (cx + radius).coerceAtMost(safeWidth - 1)
        val top = (cy - radius).coerceAtLeast(0)
        val bottom = (cy + radius).coerceAtMost(safeHeight - 1)
        val radiusSq = radius * radius
        for (yy in top..bottom) {
            val yDeltaSq = (yy - cy) * (yy - cy)
            for (xx in left..right) {
                val xDelta = xx - cx
                if ((xDelta * xDelta) + yDeltaSq > radiusSq) continue
                val idx = yy * safeWidth + xx
                if (!expandedDrawnMask[idx]) {
                    expandedDrawnMask[idx] = true
                    if (targetMask[idx]) {
                        targetCoveredCount++
                    }
                }
            }
        }
    }
}

private fun renderTargetMask(text: String, width: Int, height: Int): BooleanArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    val canvas = AndroidCanvas(bitmap)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = AndroidPaint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    var textSize = (minOf(width, height) * 0.58f).coerceAtLeast(1f)
    paint.textSize = textSize
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val maxGlyphWidth = width * 0.78f
    val maxGlyphHeight = height * 0.78f
    val widthScale = if (bounds.width() > 0) maxGlyphWidth / bounds.width().toFloat() else 1f
    val heightScale = if (bounds.height() > 0) maxGlyphHeight / bounds.height().toFloat() else 1f
    val scale = minOf(1f, widthScale, heightScale)
    textSize *= scale
    paint.textSize = textSize
    paint.getTextBounds(text, 0, text.length, bounds)

    val x = width / 2f
    val y = (height / 2f) - (bounds.top + bounds.bottom) / 2f
    canvas.drawText(text, x, y, paint)

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return BooleanArray(width * height) { idx -> pixels[idx] != 0 }
}

private fun expandMask(mask: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
    if (radius <= 0) return mask.copyOf()
    val expanded = BooleanArray(width * height)
    val radiusSq = radius * radius
    for (y in 0 until height) {
        for (x in 0 until width) {
            val idx = y * width + x
            if (!mask[idx]) continue
            val left = (x - radius).coerceAtLeast(0)
            val right = (x + radius).coerceAtMost(width - 1)
            val top = (y - radius).coerceAtLeast(0)
            val bottom = (y + radius).coerceAtMost(height - 1)
            for (yy in top..bottom) {
                val yDeltaSq = (yy - y) * (yy - y)
                for (xx in left..right) {
                    val xDelta = xx - x
                    if ((xDelta * xDelta) + yDeltaSq <= radiusSq) {
                        expanded[yy * width + xx] = true
                    }
                }
            }
        }
    }
    return expanded
}
