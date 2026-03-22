package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import com.reeled.quizoverlay.R

@Composable
fun RiveMedia(
    modifier: Modifier = Modifier,
    fit: Fit = Fit.CONTAIN,
    alignment: Alignment = Alignment.CENTER
) {
    AndroidView(
        modifier = modifier.background(Color.Transparent),
        factory = { context ->
            RiveAnimationView(context).apply {
                setRiveResource(
                    resId = R.raw.hover,
                    fit = fit,
                    alignment = alignment,
                    autoplay = true
                )
            }
        }
    )
}
