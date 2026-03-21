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
    val stateMachineName = "State Machine 1"

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
                        fit = Fit.COVER, // Focus on the monkey, crop edges
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                } catch (_: Exception) {
                    // Some .riv replacements may not include the expected state machine.
                    // Fall back to the default artboard animation instead of crashing.
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
            // Map MascotEmotion to Rive State Machine Inputs
            val inputName = "emotion"
            val inputValue = when (emotion) {
                MascotEmotion.IDLE -> 0f
                MascotEmotion.HAPPY -> 1f
                MascotEmotion.CHEER -> 2f
                MascotEmotion.SAD -> 3f
                MascotEmotion.THINKING -> 4f
                MascotEmotion.CORRECT -> 5f
                MascotEmotion.WRONG -> 6f
                MascotEmotion.SLEEPING -> 7f
            }
            
            try {
                view.setNumberState(stateMachineName, inputName, inputValue)
            } catch (e: Exception) {
                // If the state machine doesn't exist or input is wrong, 
                // it will just play the default animation.
            }
        }
    )
}
