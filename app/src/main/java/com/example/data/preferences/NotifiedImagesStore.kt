package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifyStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_notify_state")

class NotifiedImagesStore(private val context: Context) {
    private val knownIdsKey = stringSetPreferencesKey("known_image_ids")
    private val knownChatTimestampsKey = stringPreferencesKey("known_chat_timestamps_json")
    private val backgroundSyncKey = booleanPreferencesKey("background_sync_enabled")
    private val lastCheckedKey = longPreferencesKey("last_sync_check_epoch_ms")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val chatMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Long::class.javaObjectType
    )
    private val chatMapAdapter = moshi.adapter<Map<String, Long>>(chatMapType)

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

    suspend fun getLastNotifiedChatEpoch(chatId: String): Long {
        val raw = context.notifyStateDataStore.data.first()[knownChatTimestampsKey]
        return decodeChatMap(raw)[chatId] ?: 0L
    }

    suspend fun markChatNotified(chatId: String, updatedAtMs: Long) {
        if (chatId.isBlank() || updatedAtMs <= 0L) return
        context.notifyStateDataStore.edit { prefs ->
            val current = decodeChatMap(prefs[knownChatTimestampsKey]).toMutableMap()
            if (current[chatId] != updatedAtMs) {
                current[chatId] = updatedAtMs
                // Keep most recent 200 threads bounded
                val trimmed = if (current.size > 200) {
                    current.entries.toList().takeLast(200).associate { it.key to it.value }
                } else {
                    current
                }
                prefs[knownChatTimestampsKey] = chatMapAdapter.toJson(trimmed)
            }
        }
    }

    suspend fun updateLastSyncCheck(epochMs: Long) {
        context.notifyStateDataStore.edit { prefs ->
            prefs[lastCheckedKey] = epochMs
        }
    }

    private fun decodeChatMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { chatMapAdapter.fromJson(raw).orEmpty() }.getOrDefault(emptyMap())
    }

    companion object {
        private const val MAX_TRACKED_IDS = 500

        fun buildProfileAlertToken(profileId: String, refreshEpochMs: Long): String =
            "${profileId}_r$refreshEpochMs"

        fun buildChatAlertStamp(
            updatedMs: Long,
            lastMessage: String,
            unreadCount: Int,
            lastSenderRole: String,
        ): Long {
            val msgHash = (lastMessage.trim() + "|" + lastSenderRole + "|" + unreadCount).hashCode().toLong() and 0xFFFFFFFFL
            val timePart = (updatedMs / 1000L) and 0x7FFFFFFFL
            return ((timePart shl 32) or msgHash).coerceAtLeast(1L)
        }
    }
}
