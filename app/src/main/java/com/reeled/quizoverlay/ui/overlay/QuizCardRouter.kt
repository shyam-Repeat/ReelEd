package com.reeled.quizoverlay.ui.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizCardType
import com.reeled.quizoverlay.ui.overlay.cards.DragDropMatchCard
import com.reeled.quizoverlay.ui.overlay.cards.FillBlankCard
import com.reeled.quizoverlay.ui.overlay.cards.TapChoiceCard
import com.reeled.quizoverlay.ui.overlay.cards.TapTapMatchCard
import com.reeled.quizoverlay.ui.overlay.components.PandaTrainAnimation

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

    Column {
        PandaTrainAnimation(modifier = Modifier.height(100.dp))
        Spacer(modifier = Modifier.height(20.dp))
        
        when (effectiveConfig.cardType) {
            QuizCardType.TAP_CHOICE -> TapChoiceCard(effectiveConfig, sourceApp, onResult)
            QuizCardType.TAP_TAP_MATCH -> TapTapMatchCard(effectiveConfig, sourceApp, onResult)
            QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(effectiveConfig, sourceApp, onResult)
            QuizCardType.FILL_BLANK -> FillBlankCard(effectiveConfig, sourceApp, onResult)
        }
    }
}
