package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppLookTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val lookThemeKey = stringPreferencesKey("app_look_theme")
    private val staggeredKey = booleanPreferencesKey("is_staggered_layout")

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
}
