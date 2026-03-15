package com.reeled.quizoverlay.data.repository

import com.reeled.quizoverlay.data.local.EventLogEntity
import com.reeled.quizoverlay.data.local.QuizAttemptEntity
import com.reeled.quizoverlay.data.local.QuizDao
import com.reeled.quizoverlay.data.model.QuizQuestion
import com.reeled.quizoverlay.data.network.SupabaseApi

class QuizRepository(
    private val quizDao: QuizDao,
    private val supabaseApi: SupabaseApi
) {
    suspend fun getActiveQuestions(): List<QuizQuestion> =
        quizDao.getAllActive().map { it.toDomain() }

    suspend fun getActiveQuestionCount(): Int = quizDao.getActiveQuestionCount()

    suspend fun refreshQuestions(limit: Int) {
        val rows = supabaseApi.fetchActiveQuestions(limit = limit)
        quizDao.upsertQuestions(rows.map { it.toEntity() })
    }

    suspend fun insertAttempt(attempt: QuizAttemptEntity) = quizDao.insertAttempt(attempt)

    suspend fun insertEvent(event: EventLogEntity) = quizDao.insertEvent(event)

    suspend fun getAttemptedIdsToday(startOfDayMs: Long): Set<String> =
        quizDao.getAttemptedQuestionIdsSince(startOfDayMs).toSet()

    suspend fun syncUnsynced() {
        val attempts = quizDao.getUnsyncedAttempts()
        val events = quizDao.getUnsyncedEvents()

        if (attempts.isNotEmpty()) {
            supabaseApi.batchInsertAttempts(attempts.map { it.toDto() })
            quizDao.markAttemptsSynced(attempts.map { it.id })
        }

        if (events.isNotEmpty()) {
            supabaseApi.batchInsertEvents(events.map { it.toDto() })
            quizDao.markEventsSynced(events.map { it.id })
        }
    }
}
