package com.yourapp.mascot

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────
// 1. EMOTION ENUM — all panda states
// ─────────────────────────────────────────────
enum class PandaEmotion {
    IDLE,       // calm, slow blink
    HAPPY,      // big smile, rosy cheeks
    CHEER,      // jumping, star eyes
    SAD,        // droopy eyes, frown
    THINKING,   // one eyebrow raised, side glance
    CORRECT,    // heart eyes, huge grin
    WRONG,      // sweat drop, worried brows
    SLEEPING    // closed eyes, zzz
}

// ─────────────────────────────────────────────
// 2. COLORS
// ─────────────────────────────────────────────
private val PandaWhite   = Color(0xFFF5F5F5)
private val PandaBlack   = Color(0xFF1A1A1A)
private val PandaPink    = Color(0xFFFFB3C1)
private val PandaRed     = Color(0xFFFF6B6B)
private val PandaGreen   = Color(0xFF58CC02)
private val PandaYellow  = Color(0xFFFFD700)
private val PandaBlue    = Color(0xFF64B5F6)
private val EyeWhite     = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────
// 3. MAIN COMPOSABLE — drop this anywhere
// ─────────────────────────────────────────────
@Composable
fun PandaMascot(
    emotion: PandaEmotion,
    modifier: Modifier = Modifier,
    size: Float = 200f
) {
    // --- Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "panda")

    // Idle float up/down
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (emotion == PandaEmotion.IDLE || emotion == PandaEmotion.HAPPY) 8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Blink timer
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                1f at 0
                1f at 2700
                0f at 2800
                1f at 2900
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    // Cheer jump
    val cheerJump by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (emotion == PandaEmotion.CHEER) -20f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cheerJump"
    )

    // Shake for WRONG
    val shakeX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (emotion == PandaEmotion.WRONG) 6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(80),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeX"
    )

    // Rosy cheek pulse for HAPPY/CORRECT
    val cheekAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cheekAlpha"
    )

    val offsetY = floatY + cheerJump
    val offsetX = shakeX

    Canvas(modifier = modifier.size(size.dp)) {
        translate(left = offsetX, top = offsetY) {
            drawPanda(
                emotion = emotion,
                blinkAlpha = blinkAlpha,
                cheekAlpha = cheekAlpha,
                canvasSize = size
            )
        }
    }
}

// ─────────────────────────────────────────────
// 4. DRAW PANDA — all parts
// ─────────────────────────────────────────────
private fun DrawScope.drawPanda(
    emotion: PandaEmotion,
    blinkAlpha: Float,
    cheekAlpha: Float,
    canvasSize: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r  = size.width * 0.38f  // head radius

    // --- Ear patches (black, behind head) ---
    drawCircle(color = PandaBlack, radius = r * 0.32f, center = Offset(cx - r * 0.75f, cy - r * 0.75f))
    drawCircle(color = PandaBlack, radius = r * 0.32f, center = Offset(cx + r * 0.75f, cy - r * 0.75f))
    // Inner ear
    drawCircle(color = PandaPink, radius = r * 0.16f, center = Offset(cx - r * 0.75f, cy - r * 0.75f))
    drawCircle(color = PandaPink, radius = r * 0.16f, center = Offset(cx + r * 0.75f, cy - r * 0.75f))

    // --- Head (white) ---
    drawCircle(color = PandaWhite, radius = r, center = Offset(cx, cy))
    drawCircle(
        color = PandaBlack.copy(alpha = 0.08f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )

    // --- Eye patches (black oval) ---
    drawOval(
        color = PandaBlack,
        topLeft = Offset(cx - r * 0.58f, cy - r * 0.32f),
        size = Size(r * 0.44f, r * 0.38f)
    )
    drawOval(
        color = PandaBlack,
        topLeft = Offset(cx + r * 0.14f, cy - r * 0.32f),
        size = Size(r * 0.44f, r * 0.38f)
    )

    // --- Eyes (white) ---
    val eyeLY = cy - r * 0.16f
    val eyeLX = cx - r * 0.36f
    val eyeRX = cx + r * 0.36f
    val eyeR  = r * 0.14f

    if (emotion == PandaEmotion.SLEEPING) {
        // Closed arc eyes
        drawArc(
            color = PandaBlack, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(eyeLX - eyeR, eyeLY - eyeR * 0.5f),
            size = Size(eyeR * 2f, eyeR),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
        drawArc(
            color = PandaBlack, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(eyeRX - eyeR, eyeLY - eyeR * 0.5f),
            size = Size(eyeR * 2f, eyeR),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    } else {
        // Normal white eyeballs
        drawCircle(color = EyeWhite, radius = eyeR, center = Offset(eyeLX, eyeLY))
        drawCircle(color = EyeWhite, radius = eyeR, center = Offset(eyeRX, eyeLY))

        when (emotion) {
            PandaEmotion.CORRECT -> {
                // Heart eyes ♥
                drawHeart(cx = eyeLX, cy = eyeLY, size = eyeR * 0.9f, color = PandaRed)
                drawHeart(cx = eyeRX, cy = eyeLY, size = eyeR * 0.9f, color = PandaRed)
            }
            PandaEmotion.CHEER -> {
                // Star eyes ★
                drawStar(cx = eyeLX, cy = eyeLY, radius = eyeR * 0.85f, color = PandaYellow)
                drawStar(cx = eyeRX, cy = eyeLY, radius = eyeR * 0.85f, color = PandaYellow)
            }
            PandaEmotion.THINKING -> {
                // Side glance — pupils shifted right
                drawCircle(color = PandaBlack, radius = eyeR * 0.55f, center = Offset(eyeLX + eyeR * 0.3f, eyeLY))
                drawCircle(color = PandaBlack, radius = eyeR * 0.55f, center = Offset(eyeRX + eyeR * 0.3f, eyeLY))
                // Shine
                drawCircle(color = EyeWhite, radius = eyeR * 0.2f, center = Offset(eyeLX + eyeR * 0.45f, eyeLY - eyeR * 0.2f))
                drawCircle(color = EyeWhite, radius = eyeR * 0.2f, center = Offset(eyeRX + eyeR * 0.45f, eyeLY - eyeR * 0.2f))
            }
            PandaEmotion.SAD, PandaEmotion.WRONG -> {
                // Smaller sad pupils
                drawCircle(color = PandaBlack, radius = eyeR * 0.5f * blinkAlpha, center = Offset(eyeLX, eyeLY + eyeR * 0.1f))
                drawCircle(color = PandaBlack, radius = eyeR * 0.5f * blinkAlpha, center = Offset(eyeRX, eyeLY + eyeR * 0.1f))
            }
            else -> {
                // Normal pupils with blink
                drawCircle(color = PandaBlack, radius = eyeR * 0.55f * blinkAlpha, center = Offset(eyeLX, eyeLY))
                drawCircle(color = PandaBlack, radius = eyeR * 0.55f * blinkAlpha, center = Offset(eyeRX, eyeLY))
                // Shine dots
                drawCircle(color = EyeWhite, radius = eyeR * 0.18f, center = Offset(eyeLX - eyeR * 0.15f, eyeLY - eyeR * 0.2f))
                drawCircle(color = EyeWhite, radius = eyeR * 0.18f, center = Offset(eyeRX - eyeR * 0.15f, eyeLY - eyeR * 0.2f))
            }
        }
    }

    // --- Eyebrows ---
    val browY = cy - r * 0.38f
    val browStroke = Stroke(width = 3.5f, cap = StrokeCap.Round)
    when (emotion) {
        PandaEmotion.SAD, PandaEmotion.WRONG -> {
            // Sad brows — angled inward downward
            drawLine(color = PandaBlack, start = Offset(eyeLX - eyeR, browY + 4f), end = Offset(eyeLX + eyeR * 0.5f, browY - 2f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color = PandaBlack, start = Offset(eyeRX - eyeR * 0.5f, browY - 2f), end = Offset(eyeRX + eyeR, browY + 4f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        PandaEmotion.THINKING -> {
            // One brow raised
            drawLine(color = PandaBlack, start = Offset(eyeLX - eyeR, browY - 6f), end = Offset(eyeLX + eyeR, browY - 10f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color = PandaBlack, start = Offset(eyeRX - eyeR, browY), end = Offset(eyeRX + eyeR, browY), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        PandaEmotion.CHEER, PandaEmotion.CORRECT, PandaEmotion.HAPPY -> {
            // Raised happy brows
            drawLine(color = PandaBlack, start = Offset(eyeLX - eyeR, browY - 4f), end = Offset(eyeLX + eyeR, browY - 8f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color = PandaBlack, start = Offset(eyeRX - eyeR, browY - 8f), end = Offset(eyeRX + eyeR, browY - 4f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        else -> {
            // Neutral flat brows
            drawLine(color = PandaBlack, start = Offset(eyeLX - eyeR, browY), end = Offset(eyeLX + eyeR, browY), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(color = PandaBlack, start = Offset(eyeRX - eyeR, browY), end = Offset(eyeRX + eyeR, browY), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
    }

    // --- Nose (small oval) ---
    drawOval(
        color = PandaBlack.copy(alpha = 0.7f),
        topLeft = Offset(cx - r * 0.09f, cy + r * 0.06f),
        size = Size(r * 0.18f, r * 0.11f)
    )

    // --- Mouth ---
    val mouthY  = cy + r * 0.26f
    val mouthPath = Path()
    when (emotion) {
        PandaEmotion.HAPPY, PandaEmotion.CHEER, PandaEmotion.CORRECT -> {
            // Big open smile
            mouthPath.moveTo(cx - r * 0.28f, mouthY)
            mouthPath.cubicTo(cx - r * 0.1f, mouthY + r * 0.22f, cx + r * 0.1f, mouthY + r * 0.22f, cx + r * 0.28f, mouthY)
        }
        PandaEmotion.SAD, PandaEmotion.WRONG -> {
            // Frown
            mouthPath.moveTo(cx - r * 0.22f, mouthY + r * 0.1f)
            mouthPath.cubicTo(cx - r * 0.08f, mouthY - r * 0.08f, cx + r * 0.08f, mouthY - r * 0.08f, cx + r * 0.22f, mouthY + r * 0.1f)
        }
        PandaEmotion.THINKING -> {
            // Slight smirk
            mouthPath.moveTo(cx - r * 0.1f, mouthY)
            mouthPath.cubicTo(cx, mouthY, cx + r * 0.15f, mouthY + r * 0.08f, cx + r * 0.2f, mouthY - r * 0.02f)
        }
        PandaEmotion.SLEEPING -> {
            // Tiny neutral line
            mouthPath.moveTo(cx - r * 0.1f, mouthY)
            mouthPath.lineTo(cx + r * 0.1f, mouthY)
        }
        else -> {
            // Simple smile
            mouthPath.moveTo(cx - r * 0.2f, mouthY)
            mouthPath.cubicTo(cx - r * 0.05f, mouthY + r * 0.14f, cx + r * 0.05f, mouthY + r * 0.14f, cx + r * 0.2f, mouthY)
        }
    }
    drawPath(
        path = mouthPath,
        color = PandaBlack,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // --- Rosy cheeks (HAPPY / CORRECT / CHEER) ---
    if (emotion in listOf(PandaEmotion.HAPPY, PandaEmotion.CORRECT, PandaEmotion.CHEER)) {
        drawCircle(color = PandaPink.copy(alpha = cheekAlpha * 0.5f), radius = r * 0.2f, center = Offset(cx - r * 0.58f, cy + r * 0.18f))
        drawCircle(color = PandaPink.copy(alpha = cheekAlpha * 0.5f), radius = r * 0.2f, center = Offset(cx + r * 0.58f, cy + r * 0.18f))
    }

    // --- Sweat drop (WRONG) ---
    if (emotion == PandaEmotion.WRONG) {
        drawCircle(color = PandaBlue.copy(alpha = 0.85f), radius = r * 0.08f, center = Offset(cx + r * 0.82f, cy - r * 0.1f))
    }

    // --- ZZZ (SLEEPING) ---
    if (emotion == PandaEmotion.SLEEPING) {
        // Simple Z letters above head — draw as text replacement with lines
        drawCircle(color = PandaBlue.copy(alpha = 0.5f), radius = r * 0.08f, center = Offset(cx + r * 0.7f, cy - r * 0.7f))
        drawCircle(color = PandaBlue.copy(alpha = 0.3f), radius = r * 0.05f, center = Offset(cx + r * 0.88f, cy - r * 0.88f))
    }
}

// ─────────────────────────────────────────────
// 5. HELPER — draw heart shape
// ─────────────────────────────────────────────
private fun DrawScope.drawHeart(cx: Float, cy: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + size * 0.3f)
        cubicTo(cx - size * 1.2f, cy - size * 0.4f, cx - size * 1.2f, cy - size, cx, cy - size * 0.3f)
        cubicTo(cx + size * 1.2f, cy - size, cx + size * 1.2f, cy - size * 0.4f, cx, cy + size * 0.3f)
        close()
    }
    drawPath(path, color)
}

// ─────────────────────────────────────────────
// 6. HELPER — draw star shape
// ─────────────────────────────────────────────
private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    val innerRadius = radius * 0.4f
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = Math.PI * i / points - Math.PI / 2
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

// ─────────────────────────────────────────────
// 7. PREVIEW SCREEN — shows all 8 emotions
// ─────────────────────────────────────────────
@Composable
fun PandaEmotionPreviewScreen() {
    var currentEmotion by remember { mutableStateOf(PandaEmotion.IDLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Panda: ${currentEmotion.name}",
            fontSize = 20.sp,
            color = Color(0xFF3C3C3C),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Main panda display
        PandaMascot(
            emotion = currentEmotion,
            modifier = Modifier.size(200.dp),
            size = 200f
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Emotion buttons grid
        val emotions = PandaEmotion.values()
        val colors = mapOf(
            PandaEmotion.IDLE     to Color(0xFF78909C),
            PandaEmotion.HAPPY    to Color(0xFF58CC02),
            PandaEmotion.CHEER    to Color(0xFFFFD700),
            PandaEmotion.SAD      to Color(0xFF64B5F6),
            PandaEmotion.THINKING to Color(0xFF9C27B0),
            PandaEmotion.CORRECT  to Color(0xFFFF6B6B),
            PandaEmotion.WRONG    to Color(0xFFFF5722),
            PandaEmotion.SLEEPING to Color(0xFF455A64),
        )

        emotions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emotion ->
                    Button(
                        onClick = { currentEmotion = emotion },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentEmotion == emotion)
                                (colors[emotion] ?: Color.Gray)
                            else
                                (colors[emotion] ?: Color.Gray).copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = emotion.name,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Usage hint
        Text(
            text = "Use in your quiz:\nPandaMascot(emotion = PandaEmotion.CHEER)",
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────
// 8. HOW TO USE IN YOUR QUIZ SCREEN
// ─────────────────────────────────────────────
/*
var pandaEmotion by remember { mutableStateOf(PandaEmotion.IDLE) }

// Show panda
PandaMascot(
    emotion = pandaEmotion,
    modifier = Modifier.size(160.dp)
)

// Change emotion based on quiz events:
onCorrectAnswer  -> pandaEmotion = PandaEmotion.CORRECT
onWrongAnswer    -> pandaEmotion = PandaEmotion.WRONG
onQuizComplete   -> pandaEmotion = PandaEmotion.CHEER
onQuizStart      -> pandaEmotion = PandaEmotion.IDLE
onUserThinking   -> pandaEmotion = PandaEmotion.THINKING
*/