package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scamshield_prefs")

class LocalPreferencesManager(private val context: Context) {

    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val languageCodeKey = stringPreferencesKey("language_code")

    val onboardingSeen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingSeenKey] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "system"
    }

    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[languageCodeKey] ?: "en"
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[onboardingSeenKey] = seen }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[languageCodeKey] = code }
    }
}
