package com.reeled.quizoverlay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity

@Dao
interface QuizQuestionDao {
    @Query("SELECT * FROM quiz_questions WHERE active = 1")
    suspend fun getAllActive(): List<QuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM quiz_questions WHERE active = 1")
    suspend fun getActiveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(questions: List<QuizQuestionEntity>)

    @Query("SELECT * FROM quiz_questions WHERE id = :id")
    suspend fun getById(id: String): QuizQuestionEntity?
}
