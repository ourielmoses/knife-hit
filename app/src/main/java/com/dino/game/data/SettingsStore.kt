package com.dino.game.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dino_prefs")

class SettingsStore(private val context: Context) {
    private val highScoreKey = intPreferencesKey("high_score")
    private val mutedKey = booleanPreferencesKey("muted")

    val highScore: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[highScoreKey] ?: 0
    }

    val muted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[mutedKey] ?: false
    }

    suspend fun saveIfBest(score: Int): Int {
        var best = 0
        context.dataStore.edit { prefs ->
            val current = prefs[highScoreKey] ?: 0
            best = maxOf(current, score)
            prefs[highScoreKey] = best
        }
        return best
    }

    suspend fun setMuted(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[mutedKey] = value
        }
    }
}
