package com.reeled.quizoverlay.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.reeled.quizoverlay.trigger.TriggerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.triggerDataStore by preferencesDataStore(name = "trigger_prefs")

class TriggerPrefs(private val context: Context) {

    companion object {
        private val PAUSE_ACTIVE = booleanPreferencesKey("pause_active")
        private val PAUSE_EXPIRY = longPreferencesKey("pause_expiry")
        private val APP_PAUSE_ENTRIES = stringSetPreferencesKey("app_pause_entries")
        private val OVERLAY_ACTIVE = booleanPreferencesKey("overlay_active")
        private val QUIZZES_SHOWN_TODAY = intPreferencesKey("quizzes_shown_today")
        private val LAST_QUIZ_SHOWN_TIME = longPreferencesKey("last_quiz_shown_time")
        private val SESSION_START_TIME = longPreferencesKey("session_start_time")
        private val LAST_WAS_DISMISSED = booleanPreferencesKey("last_was_dismissed")
        private val LAST_WAS_CORRECT = booleanPreferencesKey("last_was_correct")
        private val LAST_SHOWN_QUESTION_ID = stringPreferencesKey("last_shown_question_id")
        private val LAST_FOREGROUND_APP = stringPreferencesKey("last_foreground_app")
        private val LAST_SKIP_REASON = stringPreferencesKey("last_skip_reason")
        private val DAILY_CAP_DAY_START = longPreferencesKey("daily_cap_day_start")
    }

    suspend fun getTriggerState(): TriggerState {
        ensureDailyCounterForToday()
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

    suspend fun setLastSkipReason(reason: String) {
        context.triggerDataStore.edit { it[LAST_SKIP_REASON] = reason }
    }

    val lastSkipReason: Flow<String?> = context.triggerDataStore.data.map { it[LAST_SKIP_REASON] }

    val lastForegroundApp: Flow<String?> = context.triggerDataStore.data.map { it[LAST_FOREGROUND_APP] }
    
    val sessionStartTime: Flow<Long> = context.triggerDataStore.data.map { it[SESSION_START_TIME] ?: 0L }

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

    suspend fun setAppPause(packageName: String, expiryMs: Long) {
        context.triggerDataStore.edit { prefs ->
            val current = prefs[APP_PAUSE_ENTRIES].orEmpty()
            val updated = current
                .filterNot { decodePackageName(it) == packageName }
                .toMutableSet()
            if (expiryMs > System.currentTimeMillis()) {
                updated += encodeAppPause(packageName, expiryMs)
            }
            prefs[APP_PAUSE_ENTRIES] = updated
        }
    }

    suspend fun clearAppPause(packageName: String) {
        context.triggerDataStore.edit { prefs ->
            val current = prefs[APP_PAUSE_ENTRIES].orEmpty()
            prefs[APP_PAUSE_ENTRIES] = current.filterNot { decodePackageName(it) == packageName }.toSet()
        }
    }

    suspend fun clearAllAppPauses() {
        context.triggerDataStore.edit { prefs ->
            prefs[APP_PAUSE_ENTRIES] = emptySet()
        }
    }

    suspend fun hasActiveAppPause(): Boolean {
        val now = System.currentTimeMillis()
        val entries = context.triggerDataStore.data.first()[APP_PAUSE_ENTRIES].orEmpty()
        val activeEntries = entries.filter { decodeExpiryMs(it) > now }.toSet()
        if (activeEntries.size != entries.size) {
            context.triggerDataStore.edit { prefs ->
                prefs[APP_PAUSE_ENTRIES] = activeEntries
            }
        }
        return activeEntries.isNotEmpty()
    }

    suspend fun getAppPauseExpiry(packageName: String): Long {
        val entries = context.triggerDataStore.data.first()[APP_PAUSE_ENTRIES].orEmpty()
        val now = System.currentTimeMillis()
        val matched = entries.firstOrNull { decodePackageName(it) == packageName }
        val expiry = matched?.let(::decodeExpiryMs) ?: 0L
        if (expiry in 1 until now) {
            clearAppPause(packageName)
            return 0L
        }
        return expiry
    }

    suspend fun markQuizShown(questionId: String) {
        ensureDailyCounterForToday()
        context.triggerDataStore.edit {
            it[LAST_SHOWN_QUESTION_ID] = questionId
            it[LAST_QUIZ_SHOWN_TIME] = System.currentTimeMillis()
            it[QUIZZES_SHOWN_TODAY] = (it[QUIZZES_SHOWN_TODAY] ?: 0) + 1
        }
    }

    suspend fun recordQuizOutcome(wasCorrect: Boolean, wasDismissed: Boolean) {
        context.triggerDataStore.edit {
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

    private suspend fun ensureDailyCounterForToday() {
        val startOfToday = getStartOfLocalDay()
        context.triggerDataStore.edit { prefs ->
            val storedDayStart = prefs[DAILY_CAP_DAY_START] ?: 0L
            if (storedDayStart != startOfToday) {
                prefs[DAILY_CAP_DAY_START] = startOfToday
                prefs[QUIZZES_SHOWN_TODAY] = 0
            }
        }
    }

    private fun getStartOfLocalDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun encodeAppPause(packageName: String, expiryMs: Long): String =
        "$packageName::$expiryMs"

    private fun decodePackageName(entry: String): String =
        entry.substringBeforeLast("::", "")

    private fun decodeExpiryMs(entry: String): Long =
        entry.substringAfterLast("::", "0").toLongOrNull() ?: 0L
}
