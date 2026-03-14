package com.reeled.quizoverlay.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pin_prefs")

class PinPrefs(private val context: Context) {

    companion object {
        private val PARENT_PIN_HASH = stringPreferencesKey("parent_pin_hash")
        private val PIN_SET = booleanPreferencesKey("pin_set")
        private val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        private val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { it[PARENT_PIN_HASH] }
    val isPinSet: Flow<Boolean> = context.dataStore.data.map { it[PIN_SET] ?: false }
    val failedAttempts: Flow<Int> = context.dataStore.data.map { it[FAILED_ATTEMPTS] ?: 0 }
    val lockoutUntil: Flow<Long> = context.dataStore.data.map { it[LOCKOUT_UNTIL] ?: 0L }

    suspend fun savePinHash(hash: String) {
        context.dataStore.edit {
            it[PARENT_PIN_HASH] = hash
            it[PIN_SET] = true
        }
    }

    suspend fun incrementFailedAttempts() {
        context.dataStore.edit {
            val current = it[FAILED_ATTEMPTS] ?: 0
            it[FAILED_ATTEMPTS] = current + 1
        }
    }

    suspend fun resetFailedAttempts() {
        context.dataStore.edit {
            it[FAILED_ATTEMPTS] = 0
            it[LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun setLockout(until: Long) {
        context.dataStore.edit {
            it[LOCKOUT_UNTIL] = until
        }
    }
}
