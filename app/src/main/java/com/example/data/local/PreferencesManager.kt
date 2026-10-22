package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluemez_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_PEER_ID = stringPreferencesKey("peer_id")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        private val KEY_MAX_IMAGE_MB = intPreferencesKey("max_image_mb")
        private val KEY_MAX_VIDEO_MB = intPreferencesKey("max_video_mb")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val displayNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISPLAY_NAME] ?: "BlueMez User"
    }

    val myPeerIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PEER_ID] ?: ""
    }

    val autoReconnectFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RECONNECT] ?: true
    }

    val readReceiptsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_READ_RECEIPTS] ?: true
    }

    val maxImageMbFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_IMAGE_MB] ?: 25
    }

    val maxVideoMbFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAX_VIDEO_MB] ?: 100
    }

    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun getOrCreatePeerId(): String {
        val current = context.dataStore.data.first()[KEY_PEER_ID]
        if (!current.isNullOrBlank()) return current
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[KEY_PEER_ID] = newId
        }
        return newId
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISPLAY_NAME] = name.trim()
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_RECONNECT] = enabled
        }
    }

    suspend fun setReadReceipts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_READ_RECEIPTS] = enabled
        }
    }

    suspend fun setMaxImageMb(mb: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_IMAGE_MB] = mb
        }
    }

    suspend fun setMaxVideoMb(mb: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_VIDEO_MB] = mb
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
