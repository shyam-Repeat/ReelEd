package com.reeled.quizoverlay.di

import android.content.Context
import androidx.room.Room
import com.reeled.quizoverlay.data.local.AppDatabase
import com.reeled.quizoverlay.data.prefs.OverlayPrefs
import com.reeled.quizoverlay.data.repository.QuizRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "reeled.db").build()
    }

    private val supabaseApi by lazy { NetworkModule.createSupabaseApi() }

    val overlayPrefs: OverlayPrefs by lazy { OverlayPrefs(appContext) }

    val quizRepository: QuizRepository by lazy {
        QuizRepository(database.quizDao(), supabaseApi)
    }
}
