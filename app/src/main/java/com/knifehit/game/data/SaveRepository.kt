package com.knifehit.game.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knifehit.game.model.PlayerProfile
import com.knifehit.game.model.Position
import com.knifehit.game.model.SAVE_VERSION
import com.knifehit.game.model.STARTER_SKIN_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.knifeHitStore by preferencesDataStore(name = "knife_hit_save")

class SaveRepository(private val context: Context) {
    private val mutex = Mutex()

    suspend fun load(): PlayerProfile = mutex.withLock {
        val prefs = context.knifeHitStore.data.first()
        if (prefs[Keys.saveVersion] == null) {
            val seeded = PlayerProfile()
            writeUnlocked(seeded)
            return seeded
        }
        PlayerProfile(
            saveVersion = prefs[Keys.saveVersion] ?: SAVE_VERSION,
            coins = prefs[Keys.coins] ?: 0,
            diamonds = prefs[Keys.diamonds] ?: 0,
            bestScore = prefs[Keys.bestScore] ?: 0,
            unlockedWorlds = parseInts(prefs[Keys.unlockedWorlds]).ifEmpty { setOf(1) },
            furthest = Position(
                prefs[Keys.furthestWorld] ?: 1,
                prefs[Keys.furthestStage] ?: 1,
            ),
            active = Position(
                prefs[Keys.activeWorld] ?: 1,
                prefs[Keys.activeStage] ?: 1,
            ),
            ownedKnives = parseCsv(prefs[Keys.owned]).ifEmpty { setOf(STARTER_SKIN_ID) },
            equippedKnifeId = prefs[Keys.equipped] ?: STARTER_SKIN_ID,
            hapticsEnabled = prefs[Keys.haptics] ?: true,
            particlesEnabled = prefs[Keys.particles] ?: true,
            batterySaver = prefs[Keys.battery] ?: false,
            motionEnabled = prefs[Keys.motion] ?: true,
            ambientVolume = prefs[Keys.ambient] ?: 0.7f,
            sfxVolume = prefs[Keys.sfx] ?: 0.9f,
            language = prefs[Keys.lang] ?: "en",
            adCurrencyDay = prefs[Keys.adDay] ?: "",
            adCurrencyUsed = prefs[Keys.adUsed] ?: 0,
        )
    }

    suspend fun save(profile: PlayerProfile) = mutex.withLock {
        writeUnlocked(profile)
    }

    suspend fun clearAndSeed(): PlayerProfile = mutex.withLock {
        context.knifeHitStore.edit { it.clear() }
        val seeded = PlayerProfile()
        writeUnlocked(seeded)
        seeded
    }

    private suspend fun writeUnlocked(profile: PlayerProfile) {
        context.knifeHitStore.edit { p ->
            apply(p, profile)
        }
    }

    companion object {
        fun apply(p: MutablePreferences, profile: PlayerProfile) {
            p[Keys.saveVersion] = profile.saveVersion
            p[Keys.coins] = profile.coins
            p[Keys.diamonds] = profile.diamonds
            p[Keys.bestScore] = profile.bestScore
            p[Keys.unlockedWorlds] = profile.unlockedWorlds.sorted().joinToString(",")
            p[Keys.furthestWorld] = profile.furthest.world
            p[Keys.furthestStage] = profile.furthest.stageInWorld
            p[Keys.activeWorld] = profile.active.world
            p[Keys.activeStage] = profile.active.stageInWorld
            p[Keys.owned] = profile.ownedKnives.joinToString(",")
            p[Keys.equipped] = profile.equippedKnifeId
            p[Keys.haptics] = profile.hapticsEnabled
            p[Keys.particles] = profile.particlesEnabled
            p[Keys.battery] = profile.batterySaver
            p[Keys.motion] = profile.motionEnabled
            p[Keys.ambient] = profile.ambientVolume
            p[Keys.sfx] = profile.sfxVolume
            p[Keys.lang] = profile.language
            p[Keys.adDay] = profile.adCurrencyDay
            p[Keys.adUsed] = profile.adCurrencyUsed
        }

        private fun parseCsv(raw: String?): Set<String> =
            raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

        private fun parseInts(raw: String?): Set<Int> =
            parseCsv(raw).mapNotNull { it.toIntOrNull() }.toSet()
    }

    private object Keys {
        val saveVersion = intPreferencesKey("saveVersion")
        val coins = intPreferencesKey("coins")
        val diamonds = intPreferencesKey("diamonds")
        val bestScore = intPreferencesKey("bestScore")
        val unlockedWorlds = stringPreferencesKey("unlockedWorlds")
        val furthestWorld = intPreferencesKey("furthestWorld")
        val furthestStage = intPreferencesKey("furthestStage")
        val activeWorld = intPreferencesKey("activeWorld")
        val activeStage = intPreferencesKey("activeStage")
        val owned = stringPreferencesKey("ownedKnives")
        val equipped = stringPreferencesKey("equippedKnifeId")
        val haptics = booleanPreferencesKey("haptics")
        val particles = booleanPreferencesKey("particles")
        val battery = booleanPreferencesKey("battery")
        val motion = booleanPreferencesKey("motion")
        val ambient = floatPreferencesKey("ambientVolume")
        val sfx = floatPreferencesKey("sfxVolume")
        val lang = stringPreferencesKey("language")
        val adDay = stringPreferencesKey("adCurrencyDay")
        val adUsed = intPreferencesKey("adCurrencyUsed")
    }
}
