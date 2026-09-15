package com.knifehit.game

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.knifehit.game.admin.AdminEntryPoint
import com.knifehit.game.admin.AdminHost
import com.knifehit.game.ads.AdKind
import com.knifehit.game.ads.AdResult
import com.knifehit.game.ads.MockAdProvider
import com.knifehit.game.audio.AudioEngine
import com.knifehit.game.audio.Haptics
import com.knifehit.game.data.ContentRepository
import com.knifehit.game.data.SaveRepository
import com.knifehit.game.engine.GameEngine
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.i18n.StringsEn
import com.knifehit.game.i18n.StringsHe
import com.knifehit.game.model.AD_CURRENCY_DAILY_CAP
import com.knifehit.game.model.AD_CURRENCY_REWARD
import com.knifehit.game.model.BossBlueprint
import com.knifehit.game.model.INTERSTITIAL_MIN_INTERVAL_MS
import com.knifehit.game.model.KnifeSkin
import com.knifehit.game.model.ParticleKind
import com.knifehit.game.model.PlayState
import com.knifehit.game.model.PlayerProfile
import com.knifehit.game.model.Position
import com.knifehit.game.model.STARTER_SKIN_ID
import com.knifehit.game.model.Timbre
import com.knifehit.game.model.WorldDef
import com.knifehit.game.model.WorldTheme
import com.knifehit.game.render.GameWorld
import com.knifehit.game.ui.BossIntroBanner
import com.knifehit.game.ui.CloudTransition
import com.knifehit.game.ui.FloatingPrecision
import com.knifehit.game.ui.GameOverOverlay
import com.knifehit.game.ui.HomeOverlay
import com.knifehit.game.ui.LockerOverlay
import com.knifehit.game.ui.MockAdOverlay
import com.knifehit.game.ui.PauseOverlay
import com.knifehit.game.ui.ReviveCountdown
import com.knifehit.game.ui.Screen
import com.knifehit.game.ui.SettingsOverlay
import com.knifehit.game.ui.ShopOverlay
import com.knifehit.game.ui.StageClearBanner
import com.knifehit.game.ui.WorldSelectOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Calendar

class KnifeHitSession(
    val save: SaveRepository,
    val content: ContentRepository,
    val audio: AudioEngine,
    val haptics: Haptics,
) {
    val ads = MockAdProvider()
    val engine = GameEngine(
        skinsLookup = { content.skinMap() },
        worldsLookup = { content.worlds() },
        targetsLookup = { content.targetMap() },
    )
    var profile by mutableStateOf(PlayerProfile())
    var screen by mutableStateOf<Screen>(Screen.Home)
    var shopScrollId by mutableStateOf<String?>(null)
    var adKind by mutableStateOf<AdKind?>(null)
    var adCallback: ((AdResult) -> Unit)? = null
    var cloudPlay by mutableStateOf(false)
    var cloudCovered: () -> Unit = {}
    var pendingAdvance: GameEngine.AdvanceResult? = null
    var lastInterstitialAt = 0L
    var matchesExited = 0
    var adminUnlocked by mutableStateOf(false)
    var adminAttempts = 0
    var adminCooldownUntil = 0L
    var backgroundedAt = 0L
    var hydrated = false
    private var persistJob: Job? = null

    fun hydrate(scope: CoroutineScope) {
        scope.launch {
            content.load()
            profile = save.load()
            applyProfileToEngine()
            audio.setVolumes(profile.ambientVolume, profile.sfxVolume)
            audio.startAmbient(profile.active.world)
            engine.startAttract()
            hydrated = true
        }
    }

    fun applyProfileToEngine() {
        engine.equippedKnifeId = profile.equippedKnifeId
        engine.unlockedWorlds = profile.unlockedWorlds
        engine.furthest = profile.furthest
        engine.position = profile.active
    }

    fun persist(scope: CoroutineScope, debounceMs: Long = 0L) {
        persistJob?.cancel()
        persistJob = scope.launch {
            if (debounceMs > 0) delay(debounceMs)
            save.save(profile)
        }
    }

    fun creditPending() {
        val (r, d) = engine.drainPendingCurrency()
        if (r != 0 || d != 0) {
            profile = profile.copy(coins = profile.coins + r, diamonds = profile.diamonds + d)
        }
    }

    fun maybeHighScore() {
        if (!engine.isCurrentRunCheated && engine.runScore > profile.bestScore) {
            profile = profile.copy(bestScore = engine.runScore)
        }
    }

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
        creditPending()
        audio.pauseAll()
        engine.paused = true
    }

    fun onForeground() {
        audio.resumeAll()
        if (screen == Screen.Playing) engine.paused = false
        if (BuildConfig.IS_ADMIN_BUILD && backgroundedAt > 0L &&
            SystemClock.elapsedRealtime() - backgroundedAt > 120_000L
        ) {
            adminUnlocked = false
        }
    }
}

@Composable
fun KnifeHitApp(session: KnifeHitSession) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) { session.hydrate(scope) }

    val strings = if (session.profile.language == "he") StringsHe else StringsEn
    val overlayDir = if (session.profile.language == "he") LayoutDirection.Rtl else LayoutDirection.Ltr
    val reduceMotion = session.profile.motionEnabled.not() || isReduceMotion(context)

    val playing = session.screen == Screen.Playing || session.screen == Screen.Home ||
        session.screen == Screen.GameOver || session.screen == Screen.Pause
    val throttle = session.screen != Screen.Playing
    val pauseLoop = session.screen == Screen.Settings

    Box(Modifier.fillMaxSize()) {
        GameWorld(
            engine = session.engine,
            running = !pauseLoop,
            throttle = throttle,
            motionEnabled = session.profile.motionEnabled && !reduceMotion,
            particlesEnabled = session.profile.particlesEnabled,
            batterySaver = session.profile.batterySaver,
            onTap = { handleTap(session, scope) },
        )

        CompositionLocalProvider(
            LocalStrings provides strings,
            LocalLayoutDirection provides overlayDir,
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                when (session.screen) {
                    Screen.Home -> HomeOverlay(
                        coins = session.profile.coins,
                        diamonds = session.profile.diamonds,
                        onPlay = { startPlay(session, session.profile.active.world) },
                        onShop = { session.screen = Screen.Shop },
                        onLocker = { session.screen = Screen.Locker },
                        onWorlds = { session.screen = Screen.Worlds },
                        onSettings = { session.screen = Screen.Settings },
                        onAdmin = { session.screen = Screen.Admin },
                    )
                    Screen.Playing -> Unit
                    Screen.GameOver -> GameOverOverlay(
                        score = session.engine.runScore,
                        best = session.profile.bestScore,
                        isNewBest = session.engine.runScore > session.profile.bestScore && !session.engine.isCurrentRunCheated,
                        cheated = session.engine.isCurrentRunCheated,
                        worldName = session.engine.stage?.world?.name.orEmpty(),
                        stage = session.engine.position.stageInWorld,
                        onTryAgain = { startPlay(session, session.engine.position.world) },
                        onRevive = { offerRevive(session, scope) },
                        onGiveUp = { giveUp(session, scope) },
                        showRevive = session.engine.showReviveOffer,
                    )
                    Screen.Settings -> SettingsOverlay(
                        profile = session.profile,
                        ownedSkins = session.content.skins().filter { it.id in session.profile.ownedKnives },
                        onBack = { session.screen = Screen.Home },
                        onAmbient = {
                            session.profile = session.profile.copy(ambientVolume = it)
                            session.audio.setVolumes(it, session.profile.sfxVolume)
                            session.persist(scope, 400)
                        },
                        onSfx = {
                            session.profile = session.profile.copy(sfxVolume = it)
                            session.audio.setVolumes(session.profile.ambientVolume, it)
                            session.persist(scope, 400)
                        },
                        onToggle = {
                            session.profile = it
                            session.persist(scope)
                        },
                        onEquip = { id ->
                            session.profile = session.profile.copy(equippedKnifeId = id)
                            session.engine.equippedKnifeId = id
                            session.persist(scope)
                        },
                        onLanguage = {
                            session.profile = session.profile.copy(language = it)
                            session.persist(scope)
                        },
                        onClearSave = {
                            session.engine.startAttract()
                            session.screen = Screen.Home
                            scope.launch {
                                session.profile = session.save.clearAndSeed()
                                session.applyProfileToEngine()
                            }
                        },
                    )
                    Screen.Shop -> ShopOverlay(
                        skins = session.content.skins(),
                        owned = session.profile.ownedKnives,
                        coins = session.profile.coins,
                        diamonds = session.profile.diamonds,
                        adRemaining = adsLeft(session.profile),
                        scrollToId = session.shopScrollId,
                        onBack = { session.screen = Screen.Home; session.shopScrollId = null },
                        onBuyCoins = { buy(session, scope, it, coins = true) },
                        onBuyDiamonds = { buy(session, scope, it, coins = false) },
                        onAdUnlock = { skin ->
                            showAd(session, AdKind.REWARDED) { res ->
                                if (res == AdResult.Completed) {
                                    session.profile = session.profile.copy(
                                        ownedKnives = session.profile.ownedKnives + skin.id,
                                    )
                                    session.persist(scope)
                                }
                            }
                        },
                        onAdCurrency = {
                            if (adsLeft(session.profile) <= 0) return@ShopOverlay
                            showAd(session, AdKind.REWARDED) { res ->
                                if (res == AdResult.Completed) {
                                    val today = todayId()
                                    val used = if (session.profile.adCurrencyDay == today) session.profile.adCurrencyUsed + 1 else 1
                                    session.profile = session.profile.copy(
                                        coins = session.profile.coins + AD_CURRENCY_REWARD,
                                        adCurrencyDay = today,
                                        adCurrencyUsed = used,
                                    )
                                    session.persist(scope)
                                }
                            }
                        },
                    )
                    Screen.Locker -> LockerOverlay(
                        skins = session.content.skins(),
                        owned = session.profile.ownedKnives,
                        equipped = session.profile.equippedKnifeId,
                        onBack = { session.screen = Screen.Home },
                        onEquip = {
                            session.profile = session.profile.copy(equippedKnifeId = it)
                            session.engine.equippedKnifeId = it
                            session.persist(scope)
                        },
                        onOpenShop = {
                            session.shopScrollId = it
                            session.screen = Screen.Shop
                        },
                    )
                    Screen.Worlds -> WorldSelectOverlay(
                        worlds = session.content.worlds(),
                        unlocked = session.profile.unlockedWorlds,
                        onBack = { session.screen = Screen.Home },
                        onStart = { startPlay(session, it) },
                    )
                    Screen.Pause -> PauseOverlay(
                        onResume = {
                            session.engine.paused = false
                            session.screen = Screen.Playing
                        },
                        onHome = {
                            session.creditPending()
                            session.persist(scope)
                            session.engine.startAttract()
                            session.screen = Screen.Home
                        },
                    )
                    Screen.Admin -> AdminEntryPoint(
                        onClose = { session.screen = Screen.Home },
                        content = adminHost(session, scope),
                    )
                }

                if (session.screen == Screen.Playing) {
                    StageClearBanner(
                        visible = session.engine.playState == PlayState.StageClear ||
                            session.engine.playState == PlayState.Destroying ||
                            session.engine.playState == PlayState.WorldVictory,
                        world = session.engine.playState == PlayState.WorldVictory,
                    )
                    BossIntroBanner(
                        visible = session.engine.playState == PlayState.BossIntro,
                        name = session.engine.bossName,
                    )
                    FloatingPrecision(session.engine)
                    ReviveCountdown(session.engine)
                }

                CloudTransition(
                    tint = session.engine.worldTheme().cloudTintArgb,
                    play = session.cloudPlay,
                    reduceMotion = reduceMotion,
                    onCovered = { session.cloudCovered() },
                    onFinished = {
                        session.cloudPlay = false
                        session.engine.paused = false
                    },
                )

                session.adKind?.let { kind ->
                    MockAdOverlay(kind) { result ->
                        session.adKind = null
                        session.audio.duck(false)
                        session.engine.paused = session.screen != Screen.Playing
                        session.adCallback?.invoke(result)
                        session.adCallback = null
                    }
                }
            }
        }
    }

    BackHandler(enabled = session.screen != Screen.Home || session.adKind != null) {
        if (session.adKind != null) return@BackHandler
        when (session.screen) {
            Screen.Playing -> {
                session.engine.paused = true
                session.screen = Screen.Pause
            }
            Screen.Pause -> {
                session.engine.paused = false
                session.screen = Screen.Playing
            }
            Screen.GameOver -> Unit
            else -> session.screen = Screen.Home
        }
    }

    LaunchedEffect(session.engine.playState, session.screen) {
        if (session.screen != Screen.Playing) return@LaunchedEffect
        when (session.engine.playState) {
            PlayState.GameOver -> {
                session.maybeHighScore()
                session.creditPending()
                session.persist(scope)
                session.haptics.fail(session.profile.hapticsEnabled)
                session.audio.playFail()
                session.screen = Screen.GameOver
            }
            PlayState.StageClear, PlayState.WorldVictory -> {
                session.audio.playClear()
                session.creditPending()
                val result = session.engine.advanceAfterClear()
                session.profile = session.profile.copy(
                    furthest = session.engine.furthest,
                    unlockedWorlds = session.engine.unlockedWorlds,
                    active = session.engine.position,
                    coins = session.profile.coins,
                    diamonds = session.profile.diamonds,
                    bestScore = session.profile.bestScore,
                )
                session.persist(scope)
                delay(900)
                beginTransition(session, result)
            }
            PlayState.BossIntro -> session.haptics.boss(session.profile.hapticsEnabled)
            else -> Unit
        }
    }
}

private fun handleTap(session: KnifeHitSession, scope: CoroutineScope) {
    when (session.engine.playState) {
        PlayState.Ready -> {
            if (session.engine.ammoRemaining > 0 && !session.engine.attractMode) {
                session.engine.onTap()
                session.audio.playThrow(session.engine.equippedSkin().timbre, session.engine.equippedSkin().throwSpeed)
                session.haptics.tick(session.profile.hapticsEnabled)
            }
        }
        PlayState.StageClear, PlayState.WorldVictory -> Unit
        else -> session.engine.onTap()
    }
}

private fun startPlay(session: KnifeHitSession, world: Int) {
    val pos = Position(world, 1)
    session.profile = session.profile.copy(active = pos)
    session.engine.equippedKnifeId = session.profile.equippedKnifeId
    session.engine.startRun(pos)
    session.audio.startAmbient(world)
    session.screen = Screen.Playing
}

private fun beginTransition(session: KnifeHitSession, result: GameEngine.AdvanceResult) {
    session.engine.paused = true
    session.cloudCovered = {
        when (result) {
            is GameEngine.AdvanceResult.NextStage -> {
                session.engine.loadStage(result.position, refill = true, attract = false)
                session.profile = session.profile.copy(active = result.position)
            }
            is GameEngine.AdvanceResult.WorldCleared -> {
                session.engine.loadStage(result.nextWorldStart, refill = true, attract = false)
                session.profile = session.profile.copy(active = result.nextWorldStart)
                session.audio.startAmbient(result.nextWorldStart.world)
            }
            GameEngine.AdvanceResult.GameComplete -> {
                session.engine.startAttract()
                session.screen = Screen.Home
            }
            GameEngine.AdvanceResult.Stay -> Unit
        }
    }
    session.cloudPlay = true
}

private fun offerRevive(session: KnifeHitSession, scope: CoroutineScope) {
    showAd(session, AdKind.REWARDED) { res ->
        if (res == AdResult.Completed) {
            session.engine.applyRevive()
            session.screen = Screen.Playing
        }
    }
}

private fun giveUp(session: KnifeHitSession, scope: CoroutineScope) {
    session.maybeHighScore()
    session.creditPending()
    val start = session.engine.position.worldStart()
    session.profile = session.profile.copy(active = start)
    session.engine.declineReviveAndResetWorld()
    session.persist(scope)
    session.matchesExited++
    val now = SystemClock.elapsedRealtime()
    val showAdNow = session.matchesExited > 1 && now - session.lastInterstitialAt >= INTERSTITIAL_MIN_INTERVAL_MS
    if (showAdNow) {
        session.lastInterstitialAt = now
        showAd(session, AdKind.INTERSTITIAL) {
            session.screen = Screen.Playing
        }
    } else {
        session.screen = Screen.Playing
    }
}

private fun showAd(session: KnifeHitSession, kind: AdKind, onResult: (AdResult) -> Unit) {
    session.engine.paused = true
    session.audio.duck(true)
    session.adCallback = onResult
    session.adKind = kind
}

private fun buy(session: KnifeHitSession, scope: CoroutineScope, skin: KnifeSkin, coins: Boolean): String? {
    if (skin.id in session.profile.ownedKnives) return null
    val strings = if (session.profile.language == "he") StringsHe else StringsEn
    if (coins) {
        if (!skin.soldForCoins()) return strings.comingSoon
        val need = skin.priceInCoins - session.profile.coins
        if (need > 0) return "${strings.shortBy} $need ${strings.rings}"
        session.profile = session.profile.copy(
            coins = session.profile.coins - skin.priceInCoins,
            ownedKnives = session.profile.ownedKnives + skin.id,
        )
    } else {
        if (!skin.soldForDiamonds()) return strings.comingSoon
        val need = skin.priceInDiamonds - session.profile.diamonds
        if (need > 0) return "${strings.shortBy} $need ${strings.diamonds}"
        session.profile = session.profile.copy(
            diamonds = session.profile.diamonds - skin.priceInDiamonds,
            ownedKnives = session.profile.ownedKnives + skin.id,
        )
    }
    session.persist(scope)
    return null
}

private fun todayId(): String {
    val c = Calendar.getInstance()
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
}

private fun adsLeft(p: PlayerProfile): Int {
    val today = todayId()
    val used = if (p.adCurrencyDay == today) p.adCurrencyUsed else 0
    return (AD_CURRENCY_DAILY_CAP - used).coerceAtLeast(0)
}

private fun adminHost(session: KnifeHitSession, scope: CoroutineScope) = AdminHost(
    exportJson = { session.content.exportJson() },
    importJson = {
        session.content.importJson(it)
        scope.launch { session.content.persist() }
    },
    reset = {
        session.content.resetOverrides()
        scope.launch { session.content.persist() }
    },
    addSkin = { id, name, color, speed, coins, diamonds, timbre ->
        val argb = parseHex(color) ?: return@AdminHost "Bad color"
        val spd = speed.toFloatOrNull()?.coerceIn(0.4f, 2.5f) ?: return@AdminHost "Bad speed"
        val c = coins.toIntOrNull()?.coerceAtLeast(0) ?: return@AdminHost "Bad coins"
        val d = diamonds.toIntOrNull()?.coerceAtLeast(0) ?: return@AdminHost "Bad diamonds"
        val t = runCatching { Timbre.valueOf(timbre.uppercase()) }.getOrDefault(Timbre.BLADE)
        val err = session.content.addCustomSkin(
            KnifeSkin(id.trim(), name.ifBlank { id }, argb, spd, c, d, false, t),
        )
        if (err == null) scope.launch { session.content.persist() }
        err
    },
    addWorld = { name, top, bot ->
        val t = parseHex(top) ?: return@AdminHost "Bad top"
        val b = parseHex(bot) ?: return@AdminHost "Bad bottom"
        val err = session.content.addCustomWorld(
            WorldDef(
                id = 0,
                name = name,
                theme = WorldTheme(t, b, 0xFF00F5FF, 0xFFFF2BD6, 0xFF39FF14, t, ParticleKind.NEON),
                targetIds = listOf("old-tree", "oak-stump", "bamboo", "moss-disc", "pine-ring"),
                boss = BossBlueprint("generic-$name", "$name Titan", "old-tree", 1.2f),
                custom = true,
            ),
        )
        if (err == null) scope.launch { session.content.persist() }
        err
    },
    priceRows = session.content.skins().map { Triple(it.id, it.priceInCoins, it.priceInDiamonds) },
    commitPrices = { rows ->
        val err = session.content.applyPriceBatch(rows.associate { it.first to (it.second to it.third) })
        if (err == null) scope.launch { session.content.persist() }
        err
    },
    mint = { c, d ->
        session.profile = session.profile.copy(coins = session.profile.coins + c, diamonds = session.profile.diamonds + d)
        session.persist(scope)
    },
    unlockAll = {
        session.profile = session.profile.copy(ownedKnives = session.content.skins().map { it.id }.toSet())
        session.persist(scope)
    },
    passcodeHash = BuildConfig.ADMIN_PASSCODE_HASH,
    unlocked = session.adminUnlocked,
    onUnlock = { input ->
        val now = SystemClock.elapsedRealtime()
        if (now < session.adminCooldownUntil) return@AdminHost false
        val ok = sha256(input) == BuildConfig.ADMIN_PASSCODE_HASH
        if (ok) {
            session.adminUnlocked = true
            session.adminAttempts = 0
        } else {
            session.adminAttempts++
            if (session.adminAttempts >= 5) {
                session.adminCooldownUntil = now + 8_000L
                session.adminAttempts = 0
            }
        }
        ok
    },
    cooldown = SystemClock.elapsedRealtime() < session.adminCooldownUntil,
)

private fun parseHex(raw: String): Long? {
    val h = raw.trim().removePrefix("#").removePrefix("0x")
    val full = when (h.length) {
        6 -> "FF$h"
        8 -> h
        else -> return null
    }
    return full.toLongOrNull(16)
}

private fun sha256(value: String): String {
    val d = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return d.joinToString("") { "%02x".format(it) }
}

private fun isReduceMotion(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale == 0f
    } catch (_: Throwable) {
        false
    }
}
