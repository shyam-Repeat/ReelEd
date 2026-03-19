package com.reeled.quizoverlay.model

import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity
import com.reeled.quizoverlay.model.payload.ChoiceOption
import com.reeled.quizoverlay.model.payload.DragChip
import com.reeled.quizoverlay.model.payload.DropSlot
import com.reeled.quizoverlay.model.payload.MatchPair
import com.reeled.quizoverlay.model.payload.WordChip
import org.json.JSONArray
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
                    val leftItems = root.getJSONArray("left_items")
                    val rightItems = root.getJSONArray("right_items")
                    val correctPairs = root.getJSONArray("correct_pairs")

                    // Build a lookup map for right items: id -> label
                    val rightLabelMap = buildMap {
                        repeat(rightItems.length()) { i ->
                            val item = rightItems.getJSONObject(i)
                            put(item.getString("id"), item.getString("label"))
                        }
                    }

                    // Build a lookup map for left items: id -> label
                    val leftLabelMap = buildMap {
                        repeat(leftItems.length()) { i ->
                            val item = leftItems.getJSONObject(i)
                            put(item.getString("id"), item.getString("label"))
                        }
                    }

                    // Build pairs from correct_pairs [["L1","R3"], ...]
                    val pairs = List(correctPairs.length()) { index ->
                        val pair = correctPairs.getJSONArray(index)
                        val leftId = pair.getString(0)
                        val rightId = pair.getString(1)
                        MatchPair(
                            leftId = leftId,
                            leftLabel = leftLabelMap[leftId] ?: "",
                            rightId = rightId,
                            rightLabel = rightLabelMap[rightId] ?: ""
                        )
                    }

                    // Shuffled right order from right_items as-is
                    val rightOrderShuffled = List(rightItems.length()) { i ->
                        rightItems.getJSONObject(i).getString("id")
                    }

                    QuizPayload.TapTapMatchPayload(
                        pairs = pairs,
                        rightOrderShuffled = rightOrderShuffled
                    )
            }

                QuizCardType.DRAG_DROP_MATCH -> {
                    val draggables = root.optJSONArray("draggables") ?: JSONArray()
                    val targets = root.optJSONArray("targets") ?: JSONArray()
                    val correctPairs = root.optJSONArray("correct_pairs") ?: JSONArray()
                    val correctPairMap = mutableMapOf<String, MutableList<String>>()

                    repeat(correctPairs.length()) { index ->
                        when (val item = correctPairs.get(index)) {
                            // Format 1: {"draggable_id": "D1", "target_id": "T1"}
                            is JSONObject -> {
                                val targetId = item.optString("target_id", item.optString("slot_id"))
                                val draggableId = item.optString("draggable_id", item.optString("chip_id"))
                                if (targetId.isNotBlank() && draggableId.isNotBlank()) {
                                    correctPairMap.getOrPut(targetId) { mutableListOf() }.add(draggableId)
                                }
                            }
                            // Format 2: ["D1", "T1"]  →  index 0 = draggable_id, index 1 = target_id
                            is JSONArray -> {
                                val draggableId = item.optString(0)
                                val targetId = item.optString(1)
                                if (targetId.isNotBlank() && draggableId.isNotBlank()) {
                                    correctPairMap.getOrPut(targetId) { mutableListOf() }.add(draggableId)
                                }
                            }
                            else -> { /* skip unknown formats */ }
                        }
                    }

                    QuizPayload.DragDropPayload(
                        chips = List(draggables.length()) { index ->
                            val item = draggables.getJSONObject(index)
                            DragChip(
                                chipId = item.optString("chip_id", item.optString("draggable_id", item.optString("id"))),
                                label = item.getString("label")
                            )
                        },
                        slots = List(targets.length()) { index ->
                            val item = targets.getJSONObject(index)
                            val slotId = item.optString("slot_id", item.optString("target_id", item.optString("id")))
                            DropSlot(
                                slotId = slotId,
                                slotLabel = item.optString("slot_label", item.optString("label")),
                                correctChipIds = correctPairMap[slotId].orEmpty()
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
