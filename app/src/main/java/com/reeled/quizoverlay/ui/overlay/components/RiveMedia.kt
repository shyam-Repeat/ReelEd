package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import com.reeled.quizoverlay.R

@Composable
fun RiveMedia(
    modifier: Modifier = Modifier
) {
    // Using hover.riv instead of arrow_book
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Transparent),
        factory = { context ->
            RiveAnimationView(context).apply {
                setRiveResource(
                    resId = R.raw.hover,
                    fit = Fit.CONTAIN,
                    alignment = Alignment.CENTER,
                    autoplay = true
                )
            }
        }
    )
}
