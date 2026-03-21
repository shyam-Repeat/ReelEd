package com.reeled.quizoverlay.ui.overlay.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    // Hardcoded to arrow_book as requested since mediaUrl is not implemented
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { context ->
            RiveAnimationView(context).apply {
                setRiveResource(
                    resId = R.raw.arrow_book,
                    fit = Fit.CONTAIN,
                    alignment = Alignment.CENTER,
                    autoplay = true
                )
            }
        }
    )
}
