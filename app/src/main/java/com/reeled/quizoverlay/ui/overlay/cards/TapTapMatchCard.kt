package com.reeled.quizoverlay.ui.overlay.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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

    // Flatten left and right items into a single list of 6 tiles
    val tiles = remember(payload) {
        val leftTiles = payload.pairs.map { MatchTile(it.leftId, it.leftLabel, TileSide.LEFT) }
        val rightTiles = payload.pairs.map { MatchTile(it.rightId, it.rightLabel, TileSide.RIGHT) }
        (leftTiles + rightTiles).shuffled()
    }

    var selectedTile by remember { mutableStateOf<MatchTile?>(null) }
    var matchedIds by remember { mutableStateOf(setOf<String>()) }

    val gridColors = listOf(
        Color(0xFFEAF3DE), // Light Green
        Color(0xFFE6F1FB), // Light Blue
        Color(0xFFFAEEDA), // Light Orange
        Color(0xFFEEEDFE), // Light Purple
        Color(0xFFFAEEDA),
        Color(0xFFE1F5EE)  // Light Cyan
    )

    fun onTileClick(tile: MatchTile) {
        if (matchedIds.contains(tile.id)) return
        if (selectedTile == tile) {
            selectedTile = null
            return
        }

        val currentSelected = selectedTile
        if (currentSelected == null) {
            selectedTile = tile
        } else {
            val isMatch = if (currentSelected.side == tile.side) {
                false
            } else {
                val (left, right) = if (currentSelected.side == TileSide.LEFT) {
                    currentSelected to tile
                } else {
                    tile to currentSelected
                }
                payload.pairs.any { it.leftId == left.id && it.rightId == right.id }
            }

            if (isMatch) {
                soundManager.play("match")
                matchedIds = matchedIds + currentSelected.id + tile.id
                selectedTile = null
                
                if (matchedIds.size == tiles.size) {
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
                // INCORRECT MATCH - report to router for strike counting
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
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
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
            
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(tiles) { index, tile ->
                val isMatched = matchedIds.contains(tile.id)
                val isSelected = selectedTile == tile
                val baseColor = gridColors[index % gridColors.size]

                MatchGridTile(
                    label = tile.label,
                    baseColor = baseColor,
                    isMatched = isMatched,
                    isSelected = isSelected,
                    onClick = { onTileClick(tile) }
                )
            }
        }
    }
}

@Composable
fun MatchGridTile(
    label: String,
    baseColor: Color,
    isMatched: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isMatched) 0.4f else 1f)
    val checkSymbol = stringResource(R.string.match_correct_symbol)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(baseColor)
            .let { 
                if (isSelected) {
                    it.border(2.5.dp, Color(0xFF534AB7), RoundedCornerShape(16.dp))
                } else if (isMatched) {
                    it.border(2.5.dp, Color(0xFF3B6D11), RoundedCornerShape(16.dp))
                } else it
            }
            .clickable(enabled = !isMatched, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 32.sp,
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
    val id: String,
    val label: String,
    val side: TileSide
)

enum class TileSide { LEFT, RIGHT }
