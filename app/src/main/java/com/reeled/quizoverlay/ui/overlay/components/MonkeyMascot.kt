package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import com.reeled.quizoverlay.R

import androidx.compose.ui.draw.clipToBounds

@Composable
fun MonkeyMascot(
    emotion: MascotEmotion,
    modifier: Modifier = Modifier
) {
    // Restoring the correct state machine name as confirmed
    val stateMachineName = "Monkey state machine"

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(), // Ensure Rive content never bleeds out
        factory = { context ->
            RiveAnimationView(context).apply {
                try {
                    setRiveResource(
                        resId = R.raw.swinging_monkey,
                        stateMachineName = stateMachineName,
                        fit = Fit.COVER,
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                } catch (_: Exception) {
                    setRiveResource(
                        resId = R.raw.swinging_monkey,
                        fit = Fit.COVER,
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                }
            }
        },
        update = { view ->
            val isSwinging = when (emotion) {
                MascotEmotion.HAPPY, MascotEmotion.CHEER, MascotEmotion.CORRECT -> true
                else -> false
            }
            val isChilling = !isSwinging

            try {
                // Set the boolean states using the correct lowercase 'hover' names
                view.setBooleanState(stateMachineName, "Btn_Swing_hover", isSwinging)
                view.setBooleanState(stateMachineName, "Btn_Chill_hover", isChilling)
                
                // Use triggers for immediate emotional feedback
                if (isSwinging) {
                    view.fireState(stateMachineName, "Button swing click")
                } else if (emotion == MascotEmotion.IDLE) {
                    view.fireState(stateMachineName, "Button chill click")
                }
            } catch (_: Exception) {
                // Fail silently to prevent crashes if state machine name or inputs mismatch
            }
        }
    )
}
