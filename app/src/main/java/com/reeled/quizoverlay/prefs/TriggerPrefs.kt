package com.reeled.quizoverlay.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.reeled.quizoverlay.trigger.TriggerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.triggerDataStore by preferencesDataStore(name = "trigger_prefs")

class TriggerPrefs(private val context: Context) {

    companion object {
        private val PAUSE_ACTIVE = booleanPreferencesKey("pause_active")
        private val PAUSE_EXPIRY = longPreferencesKey("pause_expiry")
        private val OVERLAY_ACTIVE = booleanPreferencesKey("overlay_active")
        private val QUIZZES_SHOWN_TODAY = intPreferencesKey("quizzes_shown_today")
        private val LAST_QUIZ_SHOWN_TIME = longPreferencesKey("last_quiz_shown_time")
        private val SESSION_START_TIME = longPreferencesKey("session_start_time")
        private val LAST_WAS_DISMISSED = booleanPreferencesKey("last_was_dismissed")
        private val LAST_WAS_CORRECT = booleanPreferencesKey("last_was_correct")
        private val LAST_SHOWN_QUESTION_ID = stringPreferencesKey("last_shown_question_id")
        private val LAST_FOREGROUND_APP = stringPreferencesKey("last_foreground_app")
    }

    suspend fun getTriggerState(): TriggerState {
        val prefs = context.triggerDataStore.data.first()
        return TriggerState(
            parentPauseActive = prefs[PAUSE_ACTIVE] ?: false,
            parentPauseExpiryMs = prefs[PAUSE_EXPIRY] ?: 0,
            overlayActive = prefs[OVERLAY_ACTIVE] ?: false,
            quizzesShownToday = prefs[QUIZZES_SHOWN_TODAY] ?: 0,
            lastQuizShownTime = prefs[LAST_QUIZ_SHOWN_TIME] ?: 0,
            sessionStartTime = prefs[SESSION_START_TIME] ?: 0,
            lastWasDismissed = prefs[LAST_WAS_DISMISSED] ?: false,
            lastWasCorrect = prefs[LAST_WAS_CORRECT] ?: false,
            lastShownQuestionId = prefs[LAST_SHOWN_QUESTION_ID]
        )
    }

    suspend fun setOverlayActive(active: Boolean) {
        context.triggerDataStore.edit { it[OVERLAY_ACTIVE] = active }
    }

    suspend fun setPause(active: Boolean, expiry: Long) {
        context.triggerDataStore.edit {
            it[PAUSE_ACTIVE] = active
            it[PAUSE_EXPIRY] = expiry
        }
    }

    suspend fun clearParentPause() {
        context.triggerDataStore.edit {
            it[PAUSE_ACTIVE] = false
            it[PAUSE_EXPIRY] = 0
        }
    }

    suspend fun recordQuizShown(questionId: String, wasCorrect: Boolean, wasDismissed: Boolean) {
        context.triggerDataStore.edit {
            it[LAST_SHOWN_QUESTION_ID] = questionId
            it[LAST_QUIZ_SHOWN_TIME] = System.currentTimeMillis()
            it[QUIZZES_SHOWN_TODAY] = (it[QUIZZES_SHOWN_TODAY] ?: 0) + 1
            it[LAST_WAS_CORRECT] = wasCorrect
            it[LAST_WAS_DISMISSED] = wasDismissed
        }
    }

    suspend fun updateSessionIfNeeded(packageName: String) {
        val currentApp = context.triggerDataStore.data.first()[LAST_FOREGROUND_APP]
        if (currentApp != packageName) {
            context.triggerDataStore.edit {
                it[LAST_FOREGROUND_APP] = packageName
                it[SESSION_START_TIME] = System.currentTimeMillis()
            }
        }
    }

    suspend fun clearActiveSession() {
        context.triggerDataStore.edit {
            it[LAST_FOREGROUND_APP] = ""
            it[SESSION_START_TIME] = 0
        }
    }

    suspend fun getLastForegroundApp(): String = 
        context.triggerDataStore.data.first()[LAST_FOREGROUND_APP] ?: ""
}
