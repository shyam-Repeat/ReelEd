package com.reeled.quizoverlay.data.repository

import com.reeled.quizoverlay.data.local.EventLogEntity
import com.reeled.quizoverlay.data.local.QuizAttemptEntity
import com.reeled.quizoverlay.data.local.QuizQuestionEntity
import com.reeled.quizoverlay.data.model.QuizCardType
import com.reeled.quizoverlay.data.model.QuizQuestion
import com.reeled.quizoverlay.data.network.EventLogDto
import com.reeled.quizoverlay.data.network.QuizAttemptDto
import com.reeled.quizoverlay.data.network.QuizQuestionDto
import java.time.Instant

fun QuizQuestionDto.toEntity(now: Long = System.currentTimeMillis()): QuizQuestionEntity = QuizQuestionEntity(
    id = id,
    cardType = cardType,
    subject = subject,
    difficulty = difficulty,
    questionText = questionText,
    instructionLabel = instructionLabel,
    mediaUrl = mediaUrl,
    payloadJson = payloadJson,
    timerSeconds = timerSeconds,
    strictMode = strictMode,
    showCorrectOnWrong = showCorrectOnWrong,
    active = active,
    fetchedAt = now
)

fun QuizQuestionEntity.toDomain(): QuizQuestion = QuizQuestion(
    id = id,
    cardType = QuizCardType.valueOf(cardType),
    subject = subject,
    difficulty = difficulty,
    questionText = questionText,
    instructionLabel = instructionLabel,
    mediaUrl = mediaUrl,
    payloadJson = payloadJson,
    timerSeconds = timerSeconds,
    strictMode = strictMode,
    showCorrectOnWrong = showCorrectOnWrong,
    active = active
)

fun QuizAttemptEntity.toDto(): QuizAttemptDto = QuizAttemptDto(
    id = id,
    testerId = testerId,
    questionId = questionId,
    shownAt = Instant.ofEpochMilli(shownAt).toString(),
    answeredAt = answeredAt?.let { Instant.ofEpochMilli(it).toString() },
    selectedOptionId = selectedOptionId,
    isCorrect = isCorrect,
    wasDismissed = wasDismissed,
    wasTimerExpired = wasTimerExpired,
    responseTimeMs = responseTimeMs,
    sourceApp = sourceApp
)

fun EventLogEntity.toDto(): EventLogDto = EventLogDto(
    id = id,
    testerId = testerId,
    eventType = eventType,
    payloadJson = payloadJson,
    createdAt = Instant.ofEpochMilli(createdAt).toString()
)
