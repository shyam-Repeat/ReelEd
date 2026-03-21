package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import com.reeled.quizoverlay.R

@Composable
fun RightMascot(
    modifier: Modifier = Modifier
) {
    val stateMachineName = "State Machine 1"
    var riveView by remember { mutableStateOf<RiveAnimationView?>(null) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            riveView?.setBooleanState(stateMachineName, "mouse move", true)
                            riveView?.fireState(stateMachineName, "click")
                        } catch (_: Exception) {}
                        
                        tryAwaitRelease()
                        
                        try {
                            riveView?.setBooleanState(stateMachineName, "mouse move", false)
                        } catch (_: Exception) {}
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                RiveAnimationView(context).apply {
                    try {
                        setRiveResource(
                            resId = R.raw.arrow_book,
                            stateMachineName = stateMachineName,
                            fit = Fit.CONTAIN,
                            alignment = Alignment.CENTER,
                            autoplay = true
                        )
                        riveView = this
                    } catch (_: Exception) {
                        setRiveResource(
                            resId = R.raw.arrow_book,
                            fit = Fit.CONTAIN,
                            alignment = Alignment.CENTER,
                            autoplay = true
                        )
                    }
                }
            },
            update = { _ -> }
        )
    }
}
