package com.reeled.quizoverlay.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "overlay_prefs")

class OverlayPrefs(private val context: Context) {
    private object Keys {
        val testerId = stringPreferencesKey("tester_id")
        val sessionStartTime = longPreferencesKey("session_start_time")
        val activeSessionPackage = stringPreferencesKey("active_session_package")
        val lastQuizShownTime = longPreferencesKey("last_quiz_shown_time")
        val lastShownQuestionId = stringPreferencesKey("last_shown_question_id")
        val quizzesShownToday = longPreferencesKey("quizzes_shown_today")
        val lastWasDismissed = booleanPreferencesKey("last_was_dismissed")
        val overlayActive = booleanPreferencesKey("overlay_active")
        val parentPauseActive = booleanPreferencesKey("parent_pause_active")
        val parentPauseEndsAt = longPreferencesKey("parent_pause_ends_at")
    }

    suspend fun getTriggerState(): TriggerState {
        val prefs = context.dataStore.data.first()
        return TriggerState(
            sessionStartTime = prefs[Keys.sessionStartTime] ?: 0L,
            lastQuizShownTime = prefs[Keys.lastQuizShownTime] ?: 0L,
            quizzesShownToday = (prefs[Keys.quizzesShownToday] ?: 0L).toInt(),
            lastWasDismissed = prefs[Keys.lastWasDismissed] ?: false,
            overlayActive = prefs[Keys.overlayActive] ?: false,
            parentPauseActive = prefs[Keys.parentPauseActive] ?: false,
            parentPauseEndsAt = prefs[Keys.parentPauseEndsAt] ?: 0L,
            lastShownQuestionId = prefs[Keys.lastShownQuestionId],
            activeSessionPackage = prefs[Keys.activeSessionPackage]
        )
    }

    suspend fun setTesterId(testerId: String) {
        context.dataStore.edit { it[Keys.testerId] = testerId }
    }

    suspend fun getTesterId(): String? = context.dataStore.data.first()[Keys.testerId]

    suspend fun setOverlayActive(isActive: Boolean) {
        context.dataStore.edit { it[Keys.overlayActive] = isActive }
    }

    suspend fun updateSessionIfNeeded(packageName: String, now: Long) {
        context.dataStore.edit {
            if (it[Keys.activeSessionPackage] != packageName) {
                it[Keys.sessionStartTime] = now
                it[Keys.activeSessionPackage] = packageName
            }
        }
    }

    suspend fun clearActiveSession() {
        context.dataStore.edit {
            it.remove(Keys.sessionStartTime)
            it.remove(Keys.activeSessionPackage)
        }
    }

    suspend fun setParentPause(durationMs: Long, now: Long) {
        context.dataStore.edit {
            it[Keys.parentPauseActive] = true
            it[Keys.parentPauseEndsAt] = now + durationMs
            it[Keys.sessionStartTime] = now
        }
    }

    suspend fun clearExpiredParentPause(now: Long) {
        context.dataStore.edit {
            val endTime = it[Keys.parentPauseEndsAt] ?: 0L
            if (it[Keys.parentPauseActive] == true && now >= endTime) {
                it[Keys.parentPauseActive] = false
                it.remove(Keys.parentPauseEndsAt)
            }
        }
    }

    suspend fun recordQuizShown(questionId: String, dismissed: Boolean, now: Long) {
        context.dataStore.edit {
            it[Keys.lastQuizShownTime] = now
            it[Keys.lastShownQuestionId] = questionId
            it[Keys.lastWasDismissed] = dismissed
            it[Keys.quizzesShownToday] = (it[Keys.quizzesShownToday] ?: 0L) + 1L
        }
    }
}

data class TriggerState(
    val sessionStartTime: Long,
    val lastQuizShownTime: Long,
    val quizzesShownToday: Int,
    val lastWasDismissed: Boolean,
    val overlayActive: Boolean,
    val parentPauseActive: Boolean,
    val parentPauseEndsAt: Long,
    val lastShownQuestionId: String?,
    val activeSessionPackage: String?
)
