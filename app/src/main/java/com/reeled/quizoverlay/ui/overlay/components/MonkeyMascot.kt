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
                // Only set state if the state machine is actually playing/active
                if (view.stateMachineNames.contains(stateMachineName)) {
                    view.setBooleanState(stateMachineName, "Btn_Swing_Hover", isSwinging)
                    view.setBooleanState(stateMachineName, "Btn_Chill_Hover", isChilling)
                }
            } catch (_: Exception) {
                // If it fails, we just don't animate.
            }
        }
    )
}
