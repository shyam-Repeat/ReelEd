package com.reeled.quizoverlay.model

import com.reeled.quizoverlay.model.payload.ChoiceOption
import com.reeled.quizoverlay.model.payload.DragChip
import com.reeled.quizoverlay.model.payload.DropSlot
import com.reeled.quizoverlay.model.payload.MatchPair
import com.reeled.quizoverlay.model.payload.WordChip

sealed class QuizPayload {
    data class TapChoicePayload(val options: List<ChoiceOption>) : QuizPayload()

    data class TapTapMatchPayload(
        val pairs: List<MatchPair>,
        val rightOrderShuffled: List<String>
    ) : QuizPayload()

    data class DragDropPayload(
        val chips: List<DragChip>,
        val slots: List<DropSlot>
    ) : QuizPayload()

    data class FillBlankPayload(
        val sentenceTemplate: String,
        val wordBank: List<WordChip>
    ) : QuizPayload()
}
