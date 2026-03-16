package com.reeled.quizoverlay.data.repository

import android.content.Context
import com.reeled.quizoverlay.data.local.AppDatabase
import com.reeled.quizoverlay.data.local.entity.EventLogEntity
import com.reeled.quizoverlay.data.local.entity.QuizAttemptEntity
import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity
import com.reeled.quizoverlay.data.local.entity.toDomain
import com.reeled.quizoverlay.data.remote.SupabaseClient
import com.reeled.quizoverlay.data.remote.dto.toDto
import com.reeled.quizoverlay.data.remote.dto.toEntity
import com.reeled.quizoverlay.prefs.AppPrefs
import java.util.UUID

class QuizRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val questionDao = database.quizQuestionDao()
    private val attemptDao = database.quizAttemptDao()
    private val eventLogDao = database.eventLogDao()
    private val appPrefs = AppPrefs(context)
    private val api = SupabaseClient.api

    // Questions
    suspend fun getActiveQuestionCount(): Int = questionDao.getActiveCount()

    suspend fun fetchActiveQuestionsFromRemote(limit: Int): List<QuizQuestionEntity> {
        val remoteApi = api ?: return emptyList()
        return remoteApi.getActiveQuestions(limit = limit).map { it.toEntity() }
    }

    suspend fun upsertQuestions(questions: List<QuizQuestionEntity>) {
        questionDao.upsertAll(questions)
    }

    suspend fun getAllActiveQuestions(): List<com.reeled.quizoverlay.model.QuizQuestion> = 
        questionDao.getAllActive().map { it.toDomain() }

    // Attempts
    suspend fun saveAttempt(
        questionId: String,
        selectedOptionId: String?,
        isCorrect: Boolean,
        responseTimeMs: Long,
        sourceApp: String,
        dismissed: Boolean
    ) {
        val attempt = QuizAttemptEntity(
            id = UUID.randomUUID().toString(),
            testerId = appPrefs.getTesterId(),
            questionId = questionId,
            shownAt = System.currentTimeMillis() - responseTimeMs,
            answeredAt = System.currentTimeMillis(),
            selectedOptionId = selectedOptionId,
            isCorrect = isCorrect,
            responseTimeMs = responseTimeMs,
            sourceApp = sourceApp,
            dismissed = dismissed,
            synced = false
        )
        attemptDao.insert(attempt)
    }

    suspend fun getUnsyncedAttempts(): List<QuizAttemptEntity> = attemptDao.getUnsynced()

    suspend fun batchUploadAttempts(attempts: List<QuizAttemptEntity>) {
        val remoteApi = api ?: return
        remoteApi.postAttempts(attempts.map { it.toDto() })
    }

    suspend fun markAttemptsSynced(ids: List<String>) {
        attemptDao.markSynced(ids)
    }

    // Events
    suspend fun logEvent(eventType: String, payloadJson: String) {
        val event = EventLogEntity(
            id = UUID.randomUUID().toString(),
            testerId = appPrefs.getTesterId(),
            eventType = eventType,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis(),
            synced = false
        )
        eventLogDao.insert(event)
    }

    suspend fun getUnsyncedEvents(): List<EventLogEntity> = eventLogDao.getUnsynced()

    suspend fun batchUploadEvents(events: List<EventLogEntity>) {
        val remoteApi = api ?: return
        remoteApi.postEvents(events.map { it.toDto() })
    }

    suspend fun markEventsSynced(ids: List<String>) {
        eventLogDao.markSynced(ids)
    }

    // Dashboard Stats
    suspend fun getShownCountToday(startOfDay: Long): Int = attemptDao.getShownCountToday(startOfDay)
    suspend fun getAnsweredCountToday(startOfDay: Long): Int = attemptDao.getAnsweredCountToday(startOfDay)
    suspend fun getCorrectCountToday(startOfDay: Long): Int = attemptDao.getCorrectCountToday(startOfDay)
    suspend fun getAvgResponseTimeToday(startOfDay: Long): Float? = attemptDao.getAvgResponseTimeToday(startOfDay)
    
    suspend fun getAttemptedQuestionIdsToday(): List<String> {
        val startOfDay = com.reeled.quizoverlay.util.TimeUtils.getStartOfDay()
        return attemptDao.getAttemptedIdsToday(startOfDay)
    }

    suspend fun getRecentAttemptDetails(limit: Int) = attemptDao.getRecentAttemptDetails(limit)
}
