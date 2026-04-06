package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.model.QuizAttemptResult
import com.reeled.quizoverlay.model.QuizCardConfig
import com.reeled.quizoverlay.model.QuizPayload
import com.reeled.quizoverlay.util.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TapTapMatchCard(
    config: QuizCardConfig,
    sourceApp: String,
    soundManager: SoundManager,
    onResult: (QuizAttemptResult) -> Unit
) {
    val payload = config.payload as QuizPayload.TapTapMatchPayload
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()

    // Keep payload order: top row left_items, bottom row right_items.
    val leftTiles = remember(payload.pairs) {
        payload.pairs.mapIndexed { index, pair ->
            MatchTile(
                key = "L:${pair.leftId}:$index",
                id = pair.leftId,
                label = pair.leftLabel,
                side = TileSide.LEFT,
                pairIndex = index
            )
        }
    }
    val rightTiles = remember(payload.pairs) {
        payload.pairs.mapIndexed { index, pair ->
            MatchTile(
                key = "R:${pair.rightId}:$index",
                id = pair.rightId,
                label = pair.rightLabel,
                side = TileSide.RIGHT,
                pairIndex = index
            )
        }
    }

    var selectedTile by remember(config.id) { mutableStateOf<MatchTile?>(null) }
    var matchedKeys by remember(config.id) { mutableStateOf(setOf<String>()) }

    val pairColors = listOf(
        Color(0xFFEAF3DE), // Light Green
        Color(0xFFE6F1FB), // Light Blue
        Color(0xFFFAEEDA), // Light Orange
        Color(0xFFEEEDFE), // Light Purple
        Color(0xFFEDE8DC), // Light Sand
        Color(0xFFE1F5EE)  // Light Mint
    )

    fun onTileClick(tile: MatchTile) {
        if (matchedKeys.contains(tile.key)) return
        soundManager.speak(tile.label)
        if (selectedTile == tile) {
            selectedTile = null
            return
        }

        val currentSelected = selectedTile
        if (currentSelected == null) {
            selectedTile = tile
            return
        }

        val isMatch = currentSelected.side != tile.side && currentSelected.pairIndex == tile.pairIndex

        if (isMatch) {
            soundManager.play("match")
            val updatedMatched = matchedKeys + currentSelected.key + tile.key
            matchedKeys = updatedMatched
            selectedTile = null

            if (updatedMatched.size == leftTiles.size + rightTiles.size) {
                scope.launch {
                    delay(1000)
                    onResult(
                        QuizAttemptResult(
                            questionId = config.id,
                            selectedOptionId = "ALL_MATCHED",
                            isCorrect = true,
                            wasDismissed = false,
                            wasTimerExpired = false,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            sourceApp = sourceApp
                        )
                    )
                }
            }
        } else {
            // Incorrect selection pair, report for strike counting.
            onResult(
                QuizAttemptResult(
                    questionId = config.id,
                    selectedOptionId = "${currentSelected.id}_${tile.id}",
                    isCorrect = false,
                    wasDismissed = false,
                    wasTimerExpired = false,
                    responseTimeMs = System.currentTimeMillis() - startTime,
                    sourceApp = sourceApp
                )
            )
            selectedTile = tile
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp, start = 14.dp, end = 14.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = config.display.questionText,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        val instruction = config.display.instructionLabel.ifBlank {
            stringResource(R.string.quiz_matching_instruction)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MatchTileRow(
                tiles = leftTiles,
                matchedKeys = matchedKeys,
                selectedTile = selectedTile,
                pairColors = pairColors,
                modifier = Modifier.weight(1f),
                onTileClick = ::onTileClick
            )

            MatchTileRow(
                tiles = rightTiles,
                matchedKeys = matchedKeys,
                selectedTile = selectedTile,
                pairColors = pairColors,
                modifier = Modifier.weight(1f),
                onTileClick = ::onTileClick
            )
        }
    }
}

@Composable
private fun MatchTileRow(
    tiles: List<MatchTile>,
    matchedKeys: Set<String>,
    selectedTile: MatchTile?,
    pairColors: List<Color>,
    modifier: Modifier = Modifier,
    onTileClick: (MatchTile) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tiles.forEach { tile ->
            val isMatched = matchedKeys.contains(tile.key)
            val isSelected = selectedTile?.key == tile.key
            val baseColor = pairColors[tile.pairIndex % pairColors.size]

            MatchGridTile(
                label = tile.label,
                baseColor = baseColor,
                isMatched = isMatched,
                isSelected = isSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onTileClick(tile) }
            )
        }
    }
}

@Composable
fun MatchGridTile(
    label: String,
    baseColor: Color,
    isMatched: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isMatched) 0.5f else 1f)
    val checkSymbol = stringResource(R.string.quiz_result_correct_symbol)

    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .background(baseColor)
            .let {
                if (isSelected) {
                    it.border(3.dp, Color(0xFF534AB7), RoundedCornerShape(20.dp))
                } else if (isMatched) {
                    it.border(3.dp, Color(0xFF3B6D11), RoundedCornerShape(20.dp))
                } else {
                    it.border(2.dp, Color(0xFFA7B999), RoundedCornerShape(20.dp))
                }
            }
            .clickable(enabled = !isMatched, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 44.sp,
                textAlign = TextAlign.Center
            )
            if (isMatched) {
                Text(
                    text = checkSymbol,
                    color = Color(0xFF3B6D11),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class MatchTile(
    val key: String,
    val id: String,
    val label: String,
    val side: TileSide,
    val pairIndex: Int
)

enum class TileSide { LEFT, RIGHT }
