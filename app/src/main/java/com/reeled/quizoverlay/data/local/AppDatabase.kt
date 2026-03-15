package com.reeled.quizoverlay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [QuizQuestionEntity::class, QuizAttemptEntity::class, EventLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizDao(): QuizDao
}
