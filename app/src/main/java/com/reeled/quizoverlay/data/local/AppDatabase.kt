package com.reeled.quizoverlay.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reeled.quizoverlay.data.local.dao.EventLogDao
import com.reeled.quizoverlay.data.local.dao.QuizAttemptDao
import com.reeled.quizoverlay.data.local.dao.QuizQuestionDao
import com.reeled.quizoverlay.data.local.entity.EventLogEntity
import com.reeled.quizoverlay.data.local.entity.QuizAttemptEntity
import com.reeled.quizoverlay.data.local.entity.QuizQuestionEntity

@Database(
    entities = [
        QuizQuestionEntity::class,
        QuizAttemptEntity::class,
        EventLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reeled_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
