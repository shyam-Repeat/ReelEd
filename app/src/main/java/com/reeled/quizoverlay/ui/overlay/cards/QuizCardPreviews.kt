package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizCardType
import com.reeled.quizoverlay.model.QuizDisplay
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.model.QuizRules
import com.reeled.quizoverlay.model.payload.ChoiceOption
import com.reeled.quizoverlay.model.payload.DragChip
import com.reeled.quizoverlay.model.payload.DropSlot
import com.reeled.quizoverlay.model.payload.MatchPair
import com.reeled.quizoverlay.model.payload.WordChip
import com.reeled.quizoverlay.ui.overlay.QuizLayoutMode
import com.reeled.quizoverlay.util.SoundManager

@Preview(name = "Tap Choice Portrait", widthDp = 412, heightDp = 915)
@Composable
private fun TapChoicePortraitPreview() {
    PreviewTheme {
        val context = LocalContext.current
        TapChoiceCard(
            config = previewTapChoiceConfig(),
            sourceApp = "Preview",
            _soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.Vertical,
            onResult = {}
        )
    }
}

@Preview(name = "Tap Choice Landscape", widthDp = 915, heightDp = 412)
@Composable
private fun TapChoiceLandscapePreview() {
    PreviewTheme {
        val context = LocalContext.current
        TapChoiceCard(
            config = previewTapChoiceConfig(),
            sourceApp = "Preview",
            _soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.HorizontalCompact,
            onResult = {}
        )
    }
}

@Preview(name = "Tap Match Landscape", widthDp = 915, heightDp = 412)
@Composable
private fun TapMatchLandscapePreview() {
    PreviewTheme {
        val context = LocalContext.current
        TapTapMatchCard(
            config = previewTapTapMatchConfig(),
            sourceApp = "Preview",
            soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.HorizontalCompact,
            onResult = {}
        )
    }
}

@Preview(name = "Fill Blank Landscape", widthDp = 915, heightDp = 412)
@Composable
private fun FillBlankLandscapePreview() {
    PreviewTheme {
        val context = LocalContext.current
        FillBlankCard(
            config = previewFillBlankConfig(),
            sourceApp = "Preview",
            _soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.HorizontalCompact,
            onResult = {}
        )
    }
}

@Preview(name = "Drag Drop Landscape", widthDp = 915, heightDp = 412)
@Composable
private fun DragDropLandscapePreview() {
    PreviewTheme {
        val context = LocalContext.current
        DragDropMatchCard(
            config = previewDragDropConfig(),
            sourceApp = "Preview",
            soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.HorizontalCompact,
            onResult = {}
        )
    }
}

@Preview(name = "Draw Match Landscape", widthDp = 915, heightDp = 412)
@Composable
private fun DrawMatchLandscapePreview() {
    PreviewTheme {
        val context = LocalContext.current
        DrawMatchCard(
            config = previewDrawMatchConfig(),
            sourceApp = "Preview",
            soundManager = SoundManager(context),
            layoutMode = QuizLayoutMode.HorizontalCompact,
            onResult = {}
        )
    }
}

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

private fun previewTapChoiceConfig() = QuizCardConfig(
    id = "preview-tap-choice",
    cardType = QuizCardType.TAP_CHOICE,
    subject = "Math",
    difficulty = 1,
    display = QuizDisplay(
        questionText = "8 + 5",
        instructionLabel = "Tap the correct answer",
        mediaUrl = null
    ),
    payload = QuizPayload.TapChoicePayload(
        options = listOf(
            ChoiceOption("a", "11", false, "#E24B4A"),
            ChoiceOption("b", "13", true, "#378ADD"),
            ChoiceOption("c", "12", false, "#EF9F27"),
            ChoiceOption("d", "14", false, "#7CB342")
        )
    ),
    rules = QuizRules(timerSeconds = 120, strictMode = false, showCorrectOnWrong = false)
)

private fun previewTapTapMatchConfig() = QuizCardConfig(
    id = "preview-tap-match",
    cardType = QuizCardType.TAP_TAP_MATCH,
    subject = "Reading",
    difficulty = 1,
    display = QuizDisplay(
        questionText = "Match the words",
        instructionLabel = "Tap one item from each side",
        mediaUrl = null
    ),
    payload = QuizPayload.TapTapMatchPayload(
        pairs = listOf(
            MatchPair("l1", "Sun", "r1", "Day"),
            MatchPair("l2", "Moon", "r2", "Night"),
            MatchPair("l3", "Dog", "r3", "Bark")
        ),
        rightOrderShuffled = listOf("r2", "r3", "r1")
    ),
    rules = QuizRules(timerSeconds = 120, strictMode = false, showCorrectOnWrong = false)
)

private fun previewFillBlankConfig() = QuizCardConfig(
    id = "preview-fill-blank",
    cardType = QuizCardType.FILL_BLANK,
    subject = "Grammar",
    difficulty = 1,
    display = QuizDisplay(
        questionText = "Complete the sentence",
        instructionLabel = "Choose the best word",
        mediaUrl = null
    ),
    payload = QuizPayload.FillBlankPayload(
        sentenceTemplate = "The cat ___ on the mat.",
        wordBank = listOf(
            WordChip("w1", "sat", true),
            WordChip("w2", "run", false),
            WordChip("w3", "blue", false)
        )
    ),
    rules = QuizRules(timerSeconds = 120, strictMode = false, showCorrectOnWrong = false)
)

private fun previewDragDropConfig() = QuizCardConfig(
    id = "preview-drag-drop",
    cardType = QuizCardType.DRAG_DROP_MATCH,
    subject = "Science",
    difficulty = 1,
    display = QuizDisplay(
        questionText = "Drag the correct answer",
        instructionLabel = "Drop the correct label in the middle",
        mediaUrl = null
    ),
    payload = QuizPayload.DragDropPayload(
        draggables = listOf(
            DragChip("d1", "Planet"),
            DragChip("d2", "Car"),
            DragChip("d3", "Tree")
        ),
        targets = listOf(
            DropSlot("t1", "Center", correctChipIds = listOf("d1"))
        )
    ),
    rules = QuizRules(timerSeconds = 120, strictMode = false, showCorrectOnWrong = false)
)

private fun previewDrawMatchConfig() = QuizCardConfig(
    id = "preview-draw-match",
    cardType = QuizCardType.DRAW_MATCH,
    subject = "Writing",
    difficulty = 1,
    display = QuizDisplay(
        questionText = "Trace the number",
        instructionLabel = "Draw over the large shape",
        mediaUrl = null
    ),
    payload = QuizPayload.DrawMatchPayload(text = "5"),
    rules = QuizRules(timerSeconds = 120, strictMode = false, showCorrectOnWrong = false)
)
