package com.reeled.quizoverlay.ui.overlay

import androidx.compose.runtime.Composable
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizCardType
import com.reeled.quizoverlay.ui.overlay.cards.DragDropMatchCard
import com.reeled.quizoverlay.ui.overlay.cards.FillBlankCard
import com.reeled.quizoverlay.ui.overlay.cards.TapChoiceCard
import com.reeled.quizoverlay.ui.overlay.cards.TapTapMatchCard

@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val effectiveConfig = if (config.cardType == QuizCardType.DRAG_DROP_MATCH) {
        config.copy(rules = config.rules.copy(strictMode = true))
    } else {
        config
    }

    when (effectiveConfig.cardType) {
        QuizCardType.TAP_CHOICE -> TapChoiceCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.TAP_TAP_MATCH -> TapTapMatchCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.FILL_BLANK -> FillBlankCard(effectiveConfig, sourceApp, onResult, onDismissed)
    }
}
