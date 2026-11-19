package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppUpdateConfig
import com.example.ui.theme.AppLookTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_prefs")

data class UserEngagementPrefs(
    val dateCoins: Int = 50,
    val streakCount: Int = 0,
    val lastCheckInEpochDay: Long = 0L,
    val lastSpinRewardTitle: String = "",
    val vibePersonality: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val recentlyViewedIds: List<String> = emptyList(),
) {
    fun hasClaimedToday(todayEpochDay: Long): Boolean = lastCheckInEpochDay == todayEpochDay
}

class ThemePreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val lookThemeKey = stringPreferencesKey("app_look_theme")
    private val staggeredKey = booleanPreferencesKey("is_staggered_layout")
    private val dateCoinsKey = intPreferencesKey("date_coins_balance")
    private val streakCountKey = intPreferencesKey("checkin_streak_count")
    private val lastCheckInDayKey = longPreferencesKey("last_checkin_epoch_day")
    private val lastSpinRewardKey = stringPreferencesKey("last_spin_reward_title")
    private val vibePersonalityKey = stringPreferencesKey("user_vibe_personality")
    private val emergencyNameKey = stringPreferencesKey("sos_contact_name")
    private val emergencyPhoneKey = stringPreferencesKey("sos_contact_phone")
    private val recentlyViewedKey = stringPreferencesKey("recently_viewed_profile_ids")

    // Offline anti-bypass cache for Mandatory Force App Update lock
    private val cachedUpdateCodeKey = intPreferencesKey("cached_update_version_code")
    private val cachedUpdateNameKey = stringPreferencesKey("cached_update_version_name")
    private val cachedUpdateUrlKey = stringPreferencesKey("cached_update_apk_url")
    private val cachedUpdateNotesKey = stringPreferencesKey("cached_update_release_notes")
    private val cachedUpdateForceKey = booleanPreferencesKey("cached_update_force_lock")
    private val cachedUpdateByKey = stringPreferencesKey("cached_update_by_email")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[themeKey]
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.LIGHT
    }

    val appLookTheme: Flow<AppLookTheme> = context.dataStore.data.map { prefs ->
        val raw = prefs[lookThemeKey]
        AppLookTheme.entries.firstOrNull { it.name == raw } ?: AppLookTheme.DEFAULT
    }

    val isStaggeredLayout: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[staggeredKey] ?: false
    }

    val engagementPrefs: Flow<UserEngagementPrefs> = context.dataStore.data.map { prefs ->
        val recentRaw = prefs[recentlyViewedKey].orEmpty()
        val recentList = recentRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
        UserEngagementPrefs(
            dateCoins = prefs[dateCoinsKey] ?: 50,
            streakCount = prefs[streakCountKey] ?: 0,
            lastCheckInEpochDay = prefs[lastCheckInDayKey] ?: 0L,
            lastSpinRewardTitle = prefs[lastSpinRewardKey].orEmpty(),
            vibePersonality = prefs[vibePersonalityKey].orEmpty(),
            emergencyContactName = prefs[emergencyNameKey].orEmpty(),
            emergencyContactPhone = prefs[emergencyPhoneKey].orEmpty(),
            recentlyViewedIds = recentList,
        )
    }

    val cachedAppUpdateConfig: Flow<AppUpdateConfig> = context.dataStore.data.map { prefs ->
        AppUpdateConfig(
            id = if (prefs[cachedUpdateUrlKey].orEmpty().isNotBlank()) "cached_app_update" else "",
            latestVersionCode = prefs[cachedUpdateCodeKey] ?: 1,
            latestVersionName = prefs[cachedUpdateNameKey].orEmpty().ifBlank { "1.0" },
            apkDownloadUrl = prefs[cachedUpdateUrlKey].orEmpty(),
            releaseNotes = prefs[cachedUpdateNotesKey].orEmpty(),
            forceUpdate = prefs[cachedUpdateForceKey] ?: false,
            updatedByEmail = prefs[cachedUpdateByKey].orEmpty(),
        )
    }

    suspend fun cacheAppUpdateConfig(config: AppUpdateConfig) {
        context.dataStore.edit { prefs ->
            prefs[cachedUpdateCodeKey] = config.latestVersionCode.coerceAtLeast(1)
            prefs[cachedUpdateNameKey] = config.latestVersionName.trim().take(32)
            prefs[cachedUpdateUrlKey] = config.apkDownloadUrl.trim().take(600)
            prefs[cachedUpdateNotesKey] = config.releaseNotes.trim().take(600)
            prefs[cachedUpdateForceKey] = config.forceUpdate
            prefs[cachedUpdateByKey] = config.updatedByEmail.trim().take(120)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setAppLookTheme(lookTheme: AppLookTheme) {
        context.dataStore.edit { prefs ->
            prefs[lookThemeKey] = lookTheme.name
        }
    }

    suspend fun setStaggeredLayout(staggered: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[staggeredKey] = staggered
        }
    }

    suspend fun claimDailySpinReward(
        coinsWon: Int,
        rewardTitle: String,
        todayEpochDay: Long,
    ): Boolean {
        var claimed = false
        context.dataStore.edit { prefs ->
            val prevDay = prefs[lastCheckInDayKey] ?: 0L
            if (prevDay == todayEpochDay) return@edit
            val prevStreak = prefs[streakCountKey] ?: 0
            val nextStreak = if (prevDay == todayEpochDay - 1L) prevStreak + 1 else 1
            val currentCoins = prefs[dateCoinsKey] ?: 50
            prefs[dateCoinsKey] = (currentCoins + coinsWon.coerceAtLeast(0)).coerceAtMost(999_999)
            prefs[streakCountKey] = nextStreak
            prefs[lastCheckInDayKey] = todayEpochDay
            prefs[lastSpinRewardKey] = rewardTitle.take(64)
            claimed = true
        }
        return claimed
    }

    suspend fun spendDateCoins(cost: Int): Boolean {
        if (cost <= 0) return true
        var ok = false
        context.dataStore.edit { prefs ->
            val current = prefs[dateCoinsKey] ?: 50
            if (current >= cost) {
                prefs[dateCoinsKey] = current - cost
                ok = true
            }
        }
        return ok
    }

    suspend fun setVibePersonality(vibe: String) {
        context.dataStore.edit { prefs ->
            prefs[vibePersonalityKey] = vibe.trim().take(64)
        }
    }

    suspend fun setEmergencyContact(name: String, phone: String) {
        context.dataStore.edit { prefs ->
            prefs[emergencyNameKey] = name.trim().take(64)
            prefs[emergencyPhoneKey] = phone.trim().take(24)
        }
    }

    suspend fun recordRecentlyViewed(profileId: String) {
        val cleanId = profileId.trim()
        if (cleanId.isBlank()) return
        context.dataStore.edit { prefs ->
            val existing = prefs[recentlyViewedKey].orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it != cleanId }
            val updated = (listOf(cleanId) + existing).take(8)
            prefs[recentlyViewedKey] = updated.joinToString(",")
        }
    }
}
