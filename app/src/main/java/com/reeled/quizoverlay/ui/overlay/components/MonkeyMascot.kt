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
    val stateMachineName = "Monkey state machine"

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(), // Ensure Rive content never bleeds out
        factory = { context ->
            RiveAnimationView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                try {
                    setRiveResource(
                        resId = R.raw.swinging_monkey,
                        stateMachineName = stateMachineName,
                        fit = Fit.COVER, // Focus on the monkey, crop edges
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                } catch (_: Exception) {
                    // Fall back to default artboard animation instead of crashing
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
            // Map MascotEmotion to the new Rive State Machine Inputs (Bool)
            // For now, we use hoverSwing and hoverChill based on the emotion.
            val isSwinging = when (emotion) {
                MascotEmotion.HAPPY, MascotEmotion.CHEER, MascotEmotion.CORRECT -> true
                else -> false
            }
            val isChilling = !isSwinging

            try {
                view.setBooleanState(stateMachineName, "Btn_Swing_Hover", isSwinging)
                view.setBooleanState(stateMachineName, "Btn_Chill_Hover", isChilling)
            } catch (e: Exception) {
                // If the state machine doesn't exist or input is wrong, 
                // it will just play the default animation.
            }
        }
    )
}
