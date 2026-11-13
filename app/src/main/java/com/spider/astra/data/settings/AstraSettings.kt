package com.spider.astra.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.rxjava3.asPreferenceDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateFlow
import kotlinx.coroutines.flow.update

object AstraSettingsKey {
    val USER_NAME = stringPreferencesKey("user_name")
    val THEME_MODE = stringPreferencesKey("theme_mode") // "dark" | "light" | "system"
    val VOICE_ENABLED = preferencesKey("voice_enabled", false) // BooleanPreferencesKey via preferencesKey<T>(name, default)
    val LAST_GREETING = stringPreferencesKey("last_greeting")
}

/**
 * Thin wrapper around DataStore Preferences for ASTRA user preferences.
 *
 * All reads/writes are async and flow-based, so the UI observes changes without
 * manual polling.
 */
class AstraSettings(private val dataStore: androidx.datastore.preferences.core.PreferencesDataStore) {

    val userName: Flow<String?> = dataStore.data.map { it[AstraSettingsKey.USER_NAME] }

    fun updateUserName(name: String) = dataStore.updateData { settings ->
        settings[AstraSettingsKey.USER_NAME] = name
    }

    val themeMode: Flow<String?> = dataStore.data.map { it[AstraSettingsKey.THEME_MODE] }

    fun updateThemeMode(mode: String) = dataStore.updateData { settings ->
        settings[AstraSettingsKey.THEME_MODE] = mode
    }

    val voiceEnabled: Flow<Boolean> = dataStore.data.map { it[AstraSettingsKey.VOICE_ENABLED] ?: false }

    fun setVoiceEnabled(enabled: Boolean) = dataStore.updateData { settings ->
        settings[AstraSettingsKey.VOICE_ENABLED] = enabled
    }

    companion object {
        /** Build an [AstraSettings] backed by the application DataStore. */
        fun create(context: Context): AstraSettings {
            val store = preferencesDataStore(context, "astra.preferences")
            return AstraSettings(store)
        }
    }
}
