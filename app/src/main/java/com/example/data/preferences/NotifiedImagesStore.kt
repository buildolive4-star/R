package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifyStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_notify_state")

class NotifiedImagesStore(private val context: Context) {
    private val knownIdsKey = stringSetPreferencesKey("known_image_ids")
    private val backgroundSyncKey = booleanPreferencesKey("background_sync_enabled")
    private val lastCheckedKey = longPreferencesKey("last_sync_check_epoch_ms")

    val backgroundSyncEnabled: Flow<Boolean> = context.notifyStateDataStore.data.map { prefs ->
        prefs[backgroundSyncKey] ?: true
    }

    val lastSyncCheckEpochMs: Flow<Long> = context.notifyStateDataStore.data.map { prefs ->
        prefs[lastCheckedKey] ?: 0L
    }

    suspend fun isBackgroundSyncEnabled(): Boolean = backgroundSyncEnabled.first()

    suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        context.notifyStateDataStore.edit { prefs ->
            prefs[backgroundSyncKey] = enabled
        }
    }

    suspend fun getKnownIds(): Set<String> =
        context.notifyStateDataStore.data.first()[knownIdsKey].orEmpty()

    suspend fun markIdsKnown(ids: Collection<String>) {
        val cleaned = ids.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        context.notifyStateDataStore.edit { prefs ->
            val updated = (prefs[knownIdsKey].orEmpty() + cleaned).toList().takeLast(MAX_TRACKED_IDS).toSet()
            prefs[knownIdsKey] = updated
        }
    }

    suspend fun updateLastSyncCheck(epochMs: Long) {
        context.notifyStateDataStore.edit { prefs ->
            prefs[lastCheckedKey] = epochMs
        }
    }

    companion object {
        private const val MAX_TRACKED_IDS = 500
    }
}
