package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
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
                            riveView?.setBooleanState(stateMachineName, "IsTracking", true)
                        } catch (_: Exception) {}
                        
                        tryAwaitRelease()
                        
                        try {
                            riveView?.setBooleanState(stateMachineName, "IsTracking", false)
                        } catch (_: Exception) {}
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            factory = { context ->
                RiveAnimationView(context).apply {
                    setRiveResource(
                        resId = R.raw.hover,
                        stateMachineName = stateMachineName,
                        fit = Fit.CONTAIN,
                        alignment = Alignment.CENTER,
                        autoplay = true
                    )
                    riveView = this
                }
            },
            update = { _ -> }
        )
    }
}
