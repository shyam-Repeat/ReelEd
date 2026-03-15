package com.reeled.quizoverlay.model

import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity
import com.reeled.quizoverlay.model.payload.ChoiceOption
import com.reeled.quizoverlay.model.payload.DragChip
import com.reeled.quizoverlay.model.payload.DropSlot
import com.reeled.quizoverlay.model.payload.MatchPair
import com.reeled.quizoverlay.model.payload.WordChip
import org.json.JSONObject

data class QuizCardConfig(
    val id: String,
    val cardType: QuizCardType,
    val subject: String,
    val difficulty: Int,
    val display: QuizDisplay,
    val payload: QuizPayload,
    val rules: QuizRules
) {
    companion object {
        fun from(entity: QuizQuestionEntity): QuizCardConfig {
            val type = QuizCardType.valueOf(entity.cardType)
            return QuizCardConfig(
                id = entity.id,
                cardType = type,
                subject = entity.subject,
                difficulty = entity.difficulty,
                display = QuizDisplay(
                    questionText = entity.questionText,
                    instructionLabel = entity.instructionLabel,
                    mediaUrl = entity.mediaUrl
                ),
                payload = parsePayload(type, entity.payloadJson),
                rules = QuizRules(
                    timerSeconds = entity.timerSeconds,
                    strictMode = entity.strictMode,
                    showCorrectOnWrong = entity.showCorrectOnWrong
                )
            )
        }

        fun from(question: QuizQuestion): QuizCardConfig {
            val type = QuizCardType.valueOf(question.cardType)
            return QuizCardConfig(
                id = question.id,
                cardType = type,
                subject = question.subject,
                difficulty = question.difficulty,
                display = QuizDisplay(
                    questionText = question.questionText,
                    instructionLabel = question.instructionLabel,
                    mediaUrl = question.mediaUrl
                ),
                payload = parsePayload(type, question.payloadJson),
                rules = QuizRules(
                    timerSeconds = question.timerSeconds,
                    strictMode = question.strictMode,
                    showCorrectOnWrong = question.showCorrectOnWrong
                )
            )
        }

        private fun parsePayload(type: QuizCardType, json: String): QuizPayload {
            val root = JSONObject(json)
            return when (type) {
                QuizCardType.TAP_CHOICE -> {
                    val options = root.getJSONArray("options")
                    QuizPayload.TapChoicePayload(
                        options = List(options.length()) { index ->
                            val item = options.getJSONObject(index)
                            ChoiceOption(
                                id = item.getString("id"),
                                label = item.getString("label"),
                                isCorrect = item.getBoolean("is_correct")
                            )
                        }
                    )
                }

                QuizCardType.TAP_TAP_MATCH -> {
                    val pairs = root.getJSONArray("pairs")
                    val right = root.getJSONArray("right_order_shuffled")
                    QuizPayload.TapTapMatchPayload(
                        pairs = List(pairs.length()) { index ->
                            val item = pairs.getJSONObject(index)
                            MatchPair(
                                leftId = item.getString("left_id"),
                                leftLabel = item.getString("left_label"),
                                rightId = item.getString("right_id"),
                                rightLabel = item.getString("right_label")
                            )
                        },
                        rightOrderShuffled = List(right.length()) { index -> right.getString(index) }
                    )
                }

                QuizCardType.DRAG_DROP_MATCH -> {
                    val chips = root.getJSONArray("chips")
                    val slots = root.getJSONArray("slots")
                    QuizPayload.DragDropPayload(
                        chips = List(chips.length()) { index ->
                            val item = chips.getJSONObject(index)
                            DragChip(
                                chipId = item.getString("chip_id"),
                                label = item.getString("label")
                            )
                        },
                        slots = List(slots.length()) { index ->
                            val item = slots.getJSONObject(index)
                            DropSlot(
                                slotId = item.getString("slot_id"),
                                slotLabel = item.getString("slot_label"),
                                correctChipId = item.getString("correct_chip_id")
                            )
                        }
                    )
                }

                QuizCardType.FILL_BLANK -> {
                    val wordBank = root.getJSONArray("word_bank")
                    QuizPayload.FillBlankPayload(
                        sentenceTemplate = root.getString("sentence_template"),
                        wordBank = List(wordBank.length()) { index ->
                            val item = wordBank.getJSONObject(index)
                            WordChip(
                                chipId = item.getString("chip_id"),
                                label = item.getString("label"),
                                isCorrect = item.optBoolean("is_correct", false)
                            )
                        }
                    )
                }
            }
        }
    }
}
