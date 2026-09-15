package com.knifehit.game.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knifehit.game.model.BaseCatalogs
import com.knifehit.game.model.KnifeSkin
import com.knifehit.game.model.STARTER_SKIN_ID
import com.knifehit.game.model.Timbre
import com.knifehit.game.model.WorldDef
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.contentStore by preferencesDataStore(name = "knife_hit_content")

@Serializable
data class SkinPatch(
    val name: String? = null,
    val colorArgb: Long? = null,
    val throwSpeed: Float? = null,
    val priceInCoins: Int? = null,
    val priceInDiamonds: Int? = null,
    val timbre: Timbre? = null,
    val adUnlockable: Boolean? = null,
)

@Serializable
data class ContentOverrides(
    val skinPatches: Map<String, SkinPatch> = emptyMap(),
    val customSkins: List<KnifeSkin> = emptyList(),
    val customWorlds: List<WorldDef> = emptyList(),
)

class ContentRepository(private val context: Context? = null) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    val skinsState = mutableStateOf(BaseCatalogs.skins)
    val worldsState = mutableStateOf(BaseCatalogs.worlds)
    var overrides: ContentOverrides = ContentOverrides()
        private set

    fun skins(): List<KnifeSkin> = skinsState.value
    fun worlds(): List<WorldDef> = worldsState.value
    fun skinMap(): Map<String, KnifeSkin> = skins().associateBy { it.id }
    fun targetMap() = BaseCatalogs.targetMap()

    fun skin(id: String): KnifeSkin =
        skinMap()[id] ?: skinMap()[STARTER_SKIN_ID] ?: skins().first()

    suspend fun load() {
        val ctx = context ?: return
        val raw = ctx.contentStore.data.first()[KEY] ?: return
        runCatching { importJson(raw) }
    }

    suspend fun persist() {
        val ctx = context ?: return
        ctx.contentStore.edit { it[KEY] = json.encodeToString(overrides) }
    }

    fun exportJson(): String = json.encodeToString(overrides)

    fun importJson(raw: String) {
        overrides = json.decodeFromString(ContentOverrides.serializer(), raw)
        rebuild()
    }

    fun resetOverrides() {
        overrides = ContentOverrides()
        rebuild()
    }

    fun applySkinPatch(id: String, patch: SkinPatch) {
        overrides = overrides.copy(skinPatches = overrides.skinPatches + (id to patch))
        rebuild()
    }

    fun applyPriceBatch(prices: Map<String, Pair<Int, Int>>): String? {
        val next = overrides.skinPatches.toMutableMap()
        for ((id, pair) in prices) {
            if (pair.first < 0 || pair.second < 0) return "Prices must be non-negative"
            val existing = next[id] ?: SkinPatch()
            next[id] = existing.copy(priceInCoins = pair.first, priceInDiamonds = pair.second)
        }
        overrides = overrides.copy(skinPatches = next)
        rebuild()
        return null
    }

    fun addCustomSkin(skin: KnifeSkin): String? {
        if (skin.id.isBlank()) return "ID required"
        if (skinMap().containsKey(skin.id)) return "ID already exists"
        if (skin.throwSpeed <= 0f) return "Throw speed must be > 0"
        overrides = overrides.copy(customSkins = overrides.customSkins + skin)
        rebuild()
        return null
    }

    fun addCustomWorld(world: WorldDef): String? {
        if (world.name.isBlank()) return "Name required"
        val nextId = (worlds().maxOfOrNull { it.id } ?: 10) + 1
        overrides = overrides.copy(customWorlds = overrides.customWorlds + world.copy(id = nextId, custom = true))
        rebuild()
        return null
    }

    private fun rebuild() {
        val mergedSkins = BaseCatalogs.skins.map { base ->
            val p = overrides.skinPatches[base.id] ?: return@map base
            base.copy(
                name = p.name ?: base.name,
                colorArgb = p.colorArgb ?: base.colorArgb,
                throwSpeed = (p.throwSpeed ?: base.throwSpeed).coerceIn(0.4f, 2.5f),
                priceInCoins = p.priceInCoins ?: base.priceInCoins,
                priceInDiamonds = p.priceInDiamonds ?: base.priceInDiamonds,
                timbre = p.timbre ?: base.timbre,
                adUnlockable = p.adUnlockable ?: base.adUnlockable,
            )
        } + overrides.customSkins
        skinsState.value = mergedSkins
        worldsState.value = BaseCatalogs.worlds + overrides.customWorlds
    }

    companion object {
        private val KEY = stringPreferencesKey("overrides_json")
    }
}
