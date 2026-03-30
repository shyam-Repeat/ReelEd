package com.reeled.quizoverlay.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

class AppPrefs(private val context: Context) {

    companion object {
        private val TESTER_ID = stringPreferencesKey("tester_id")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val PIN_SET = booleanPreferencesKey("pin_set")
        private val CONSENT_GIVEN = booleanPreferencesKey("consent_given")
        private val MONITORED_APPS = stringSetPreferencesKey("monitored_apps")
        private val NICKNAME = stringPreferencesKey("nickname")
        private val IS_TEST_MODE = booleanPreferencesKey("is_test_mode")
        private val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    }

    val overlayEnabled: Flow<Boolean> = context.appDataStore.data.map { it[OVERLAY_ENABLED] ?: true }
    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[OVERLAY_ENABLED] = enabled }
    }

    val nickname: Flow<String?> = context.appDataStore.data.map { it[NICKNAME] }
    suspend fun setNickname(name: String) {
        context.appDataStore.edit { it[NICKNAME] = name }
    }

    val isTestMode: Flow<Boolean> = context.appDataStore.data.map { it[IS_TEST_MODE] ?: false }
    suspend fun setTestMode(enabled: Boolean) {
        context.appDataStore.edit { it[IS_TEST_MODE] = enabled }
    }

    suspend fun getTesterId(): String {
        val currentId = context.appDataStore.data.map { it[TESTER_ID] }.first()
        if (currentId != null) return currentId

        val newId = UUID.randomUUID().toString()
        context.appDataStore.edit { it[TESTER_ID] = newId }
        return newId
    }

    val onboardingComplete: Flow<Boolean> = context.appDataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
    suspend fun setOnboardingComplete(complete: Boolean) {
        context.appDataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    val pinSet: Flow<Boolean> = context.appDataStore.data.map { it[PIN_SET] ?: false }
    suspend fun setPinSet(set: Boolean) {
        context.appDataStore.edit { it[PIN_SET] = set }
    }

    val consentGiven: Flow<Boolean> = context.appDataStore.data.map { it[CONSENT_GIVEN] ?: false }
    suspend fun setConsentGiven(given: Boolean) {
        context.appDataStore.edit { it[CONSENT_GIVEN] = given }
    }

    val monitoredApps: Flow<Set<String>> = context.appDataStore.data.map { it[MONITORED_APPS] ?: emptySet() }
    suspend fun setMonitoredApps(apps: Set<String>) {
        context.appDataStore.edit { it[MONITORED_APPS] = apps }
    }
}
