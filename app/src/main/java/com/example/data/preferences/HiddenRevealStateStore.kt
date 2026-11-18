package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hiddenRevealDataStore: DataStore<Preferences> by preferencesDataStore(name = "justus_hidden_reveals")

@JsonClass(generateAdapter = true)
data class HiddenRevealRecord(
    val key: String,
    val expiresAtEpochMs: Long,
    val isExpired: Boolean = false,
)

/**
 * Tracks per-device 40-second reveal timers for remote Firestore-synced hidden photos,
 * keyed by "${imageId}_${refreshEpochMs}". When an admin refreshes a profile,
 * every device sees a fresh key and gets the reveal option again.
 */
class HiddenRevealStateStore(private val context: Context) {
    private val key = stringPreferencesKey("reveal_records_json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        HiddenRevealRecord::class.java
    )
    private val adapter = moshi.adapter<Map<String, HiddenRevealRecord>>(mapType)

    val revealStates: Flow<Map<String, HiddenRevealRecord>> = context.hiddenRevealDataStore.data.map { prefs ->
        decodeMap(prefs[key])
    }

    suspend fun startCountdown(imageId: String, refreshEpochMs: Long, ttlMs: Long = 40_000L): Long {
        val stateKey = buildKey(imageId, refreshEpochMs)
        var resolvedExpiry = 0L
        context.hiddenRevealDataStore.edit { prefs ->
            val current = decodeMap(prefs[key]).toMutableMap()
            val existing = current[stateKey]
            resolvedExpiry = if (existing != null && existing.expiresAtEpochMs > 0L) {
                existing.expiresAtEpochMs
            } else {
                System.currentTimeMillis() + ttlMs
            }
            if (existing == null) {
                current[stateKey] = HiddenRevealRecord(
                    key = stateKey,
                    expiresAtEpochMs = resolvedExpiry,
                    isExpired = false,
                )
                prefs[key] = adapter.toJson(current)
            }
        }
        return resolvedExpiry
    }

    suspend fun markExpired(imageId: String, refreshEpochMs: Long) {
        val stateKey = buildKey(imageId, refreshEpochMs)
        context.hiddenRevealDataStore.edit { prefs ->
            val current = decodeMap(prefs[key]).toMutableMap()
            val existing = current[stateKey]
            if (existing != null && !existing.isExpired) {
                current[stateKey] = existing.copy(isExpired = true)
                prefs[key] = adapter.toJson(current)
            }
        }
    }

    private fun decodeMap(raw: String?): Map<String, HiddenRevealRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyMap())
    }

    companion object {
        fun buildKey(imageId: String, refreshEpochMs: Long): String = "${imageId}_$refreshEpochMs"
    }
}
