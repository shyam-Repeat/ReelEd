package com.reeled.quizoverlay.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "trigger_prefs")

class TriggerPrefs(private val context: Context) {

    companion object {
        private val PAUSE_ACTIVE = booleanPreferencesKey("pause_active")
        private val PAUSE_EXPIRY = longPreferencesKey("pause_expiry")
    }

    val isPaused: Flow<Boolean> = context.dataStore.data.map { it[PAUSE_ACTIVE] ?: false }
    val pauseExpiry: Flow<Long> = context.dataStore.data.map { it[PAUSE_EXPIRY] ?: 0L }

    suspend fun setPause(active: Boolean, expiry: Long = 0L) {
        context.dataStore.edit {
            it[PAUSE_ACTIVE] = active
            it[PAUSE_EXPIRY] = expiry
        }
    }

    suspend fun clearPause() {
        context.dataStore.edit {
            it[PAUSE_ACTIVE] = false
            it[PAUSE_EXPIRY] = 0L
        }
    }
}
