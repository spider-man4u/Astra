package com.spider.astra.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.astraDataStore: DataStore<Preferences> by preferencesDataStore(name = "astra.preferences")

object AstraSettingsKey {
    val USER_NAME = stringPreferencesKey("user_name")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
    val LAST_GREETING = stringPreferencesKey("last_greeting")
}

class AstraSettings(private val dataStore: DataStore<Preferences>) {
    val userName: Flow<String?> = dataStore.data.map { it[AstraSettingsKey.USER_NAME] }
    val themeMode: Flow<String?> = dataStore.data.map { it[AstraSettingsKey.THEME_MODE] }
    val voiceEnabled: Flow<Boolean> = dataStore.data.map { it[AstraSettingsKey.VOICE_ENABLED] ?: false }

    suspend fun updateUserName(name: String) {
        dataStore.edit { it[AstraSettingsKey.USER_NAME] = name }
    }

    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { it[AstraSettingsKey.THEME_MODE] = mode }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[AstraSettingsKey.VOICE_ENABLED] = enabled }
    }

    companion object {
        fun create(context: Context) = AstraSettings(context.astraDataStore)
    }
}