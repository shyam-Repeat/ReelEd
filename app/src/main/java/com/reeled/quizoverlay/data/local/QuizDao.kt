package com.reeled.quizoverlay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions WHERE active = 1")
    suspend fun getAllActive(): List<QuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM quiz_questions WHERE active = 1")
    suspend fun getActiveQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    @Query("SELECT * FROM quiz_attempts WHERE synced = 0")
    suspend fun getUnsyncedAttempts(): List<QuizAttemptEntity>

    @Query("UPDATE quiz_attempts SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAttemptsSynced(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventLogEntity)

    @Query("SELECT * FROM event_logs WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<EventLogEntity>

    @Query("UPDATE event_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markEventsSynced(ids: List<String>)

    @Query("SELECT questionId FROM quiz_attempts WHERE shownAt >= :startOfDayMs")
    suspend fun getAttemptedQuestionIdsSince(startOfDayMs: Long): List<String>
}
