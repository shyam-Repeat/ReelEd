package com.reeled.quizoverlay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reeled.quizoverlay.data.local.entity.QuizAttemptEntity

@Dao
interface QuizAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: QuizAttemptEntity)

    @Query("SELECT * FROM quiz_attempts WHERE synced = 0")
    suspend fun getUnsynced(): List<QuizAttemptEntity>

    @Query("UPDATE quiz_attempts SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT questionId FROM quiz_attempts WHERE shownAt >= :startOfDay")
    suspend fun getAttemptedIdsToday(startOfDay: Long): List<String>

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE shownAt >= :startOfDay")
    suspend fun getShownCountToday(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE shownAt >= :startOfDay AND dismissed = 0")
    suspend fun getAnsweredCountToday(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE shownAt >= :startOfDay AND isCorrect = 1")
    suspend fun getCorrectCountToday(startOfDay: Long): Int

    @Query("SELECT AVG(responseTimeMs) FROM quiz_attempts WHERE shownAt >= :startOfDay AND dismissed = 0")
    suspend fun getAvgResponseTimeToday(startOfDay: Long): Float?

    // Recent attempts with question text and subject
    @Query("""
        SELECT a.shownAt, a.responseTimeMs, a.isCorrect, a.dismissed, q.questionText, q.subject
        FROM quiz_attempts a
        JOIN quiz_questions q ON a.questionId = q.id
        ORDER BY a.shownAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentAttemptDetails(limit: Int): List<RecentAttemptDetail>

    data class RecentAttemptDetail(
        val shownAt: Long,
        val questionText: String,
        val subject: String,
        val responseTimeMs: Long,
        val isCorrect: Boolean,
        val dismissed: Boolean
    )
}
