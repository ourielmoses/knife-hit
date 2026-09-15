package com.knifehit.game.engine

import com.knifehit.game.model.BOSS_INTRO_SEC
import com.knifehit.game.model.COLLECTIBLE_CLEARANCE_FLOOR
import com.knifehit.game.model.Collectible
import com.knifehit.game.model.DIAMONDS_PER_BOSS
import com.knifehit.game.model.DIAMONDS_PER_BOSS_REPLAY
import com.knifehit.game.model.KnifeSkin
import com.knifehit.game.model.MAX_KNIVES
import com.knifehit.game.model.PickupKind
import com.knifehit.game.model.PlayState
import com.knifehit.game.model.Position
import com.knifehit.game.model.PrecisionBand
import com.knifehit.game.model.REVIVE_COUNTDOWN_SEC
import com.knifehit.game.model.RINGS_PER_STAGE
import com.knifehit.game.model.RINGS_PER_STAGE_REPLAY
import com.knifehit.game.model.ReviveSnapshot
import com.knifehit.game.model.SCORE_FIRST_KNIFE
import com.knifehit.game.model.SCORE_GOOD
import com.knifehit.game.model.SCORE_PERFECT
import com.knifehit.game.model.SHAKE_DURATION_SEC
import com.knifehit.game.model.SPEED_MODE_MULTIPLIER
import com.knifehit.game.model.STAGE_CLEAR_BONUS
import com.knifehit.game.model.STARTER_SKIN_ID
import com.knifehit.game.model.StageSpec
import com.knifehit.game.model.StuckKnife
import com.knifehit.game.model.TargetBlueprint
import com.knifehit.game.model.WheelAttachment
import com.knifehit.game.model.WorldDef
import com.knifehit.game.model.stagesIn
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val skinsLookup: () -> Map<String, KnifeSkin>,
    private val worldsLookup: () -> List<WorldDef>,
    private val targetsLookup: () -> Map<String, TargetBlueprint>,
    private val random: Random = Random.Default,
) {
    var playState: PlayState = PlayState.Attract
        private set
    var paused: Boolean = false
    var attractMode: Boolean = true

    var canvasW = 1080f
        private set
    var canvasH = 1920f
        private set
    var cx = 540f
        private set
    var cy = 480f
        private set
    var radius = 200f
        private set
    var knifeLength = 170f
        private set
    var knifeWidth = 44f
        private set
    var restY = 1700f
        private set
    var hiltAngularWidth = 0.22f
        private set

    var rotation = 0f
        private set
    var knifeY = 1700f
        private set
    var knifeSpin = 0f
        private set
    var bounceVx = 0f
        private set
    var bounceVy = 0f
        private set
    var bouncing = false
        private set

    var frameEpoch = 0L
    var elapsed = 0f
        private set

    var attachments: List<WheelAttachment> = emptyList()
        private set
    val stuckKnives: List<StuckKnife> get() = attachments.filterIsInstance<StuckKnife>()

    var ammoRemaining = 7
        private set
    var knivesPerRound = 7
        private set
    var score = 0
        private set
    var runScore = 0
        private set
    var equippedKnifeId = STARTER_SKIN_ID
    var speedMode = false
        private set
    var isCurrentRunCheated = false
        private set
    var pendingRings = 0
        private set
    var pendingDiamonds = 0
        private set
    var collectedThisStageRings = 0
        private set
    var collectedThisStageDiamonds = 0
        private set

    var stage: StageSpec? = null
        private set
    var position: Position = Position(1, 1)
    var furthest: Position = Position(1, 1)
    var unlockedWorlds: Set<Int> = setOf(1)
    var replayPayout: Boolean = false

    var shakeT = 0f
        private set
    var shakeAmp = 0f
        private set
    var introT = 0f
        private set
    var reviveT = 0f
        private set
    var overlayT = 0f
        private set
    var lastPrecision: PrecisionBand? = null
        private set
    var lastScoreEvent: ScoreEventInternal? = null
        private set
    var pendingTap = false
        private set

    var reviveSnapshot: ReviveSnapshot? = null
        private set
    var showReviveOffer = false
    var bossName: String = ""
        private set

    val particles = ParticlePool()
    val destruction = DestructionSim()
    val rotationController = RotationController(random)

    private var prevTipX = 0f
    private var prevTipY = 0f
    private var speedSpawnTimer = -1f
    private var collectibleSpawnTimer = 1.8f
    private var layoutDirty = true

    data class ScoreEventInternal(
        val band: PrecisionBand,
        val points: Int,
        val x: Float,
        val y: Float,
        val born: Float,
    )

    fun equippedSkin(): KnifeSkin =
        skinsLookup()[equippedKnifeId] ?: skinsLookup()[STARTER_SKIN_ID] ?: skinsLookup().values.first()

    fun onTap() {
        pendingTap = true
    }

    fun consumeTap(): Boolean {
        val t = pendingTap
        pendingTap = false
        return t
    }

    fun resize(w: Float, h: Float) {
        if (w <= 1f || h <= 1f) return
        if (w != canvasW || h != canvasH) {
            canvasW = w
            canvasH = h
            layoutDirty = true
            applyLayout()
        }
    }

    private fun applyLayout() {
        cx = canvasW / 2f
        cy = canvasH * 0.25f
        val base = min(canvasW * 0.30f, canvasH * 0.17f)
        val bp = stage?.target
        val scale = if (bp != null) bp.radiusDp / com.knifehit.game.model.REFERENCE_RADIUS_DP else 1f
        val bossScale = if (stage?.isBoss == true) stage!!.world.boss.radiusScale else 1f
        radius = base * scale * bossScale
        knifeLength = radius * 0.85f
        knifeWidth = radius * 0.14f
        restY = canvasH - canvasH * 0.12f
        hiltAngularWidth = GameMath.hiltAngularWidth(knifeWidth, radius)
        if (!bouncing && playState != PlayState.Throwing) {
            knifeY = restY
        }
        layoutDirty = false
    }

    fun startAttract() {
        attractMode = true
        playState = PlayState.Attract
        paused = false
        bouncing = false
        speedMode = false
        loadStage(Position(1, 1), refill = true, attract = true)
    }

    fun startRun(from: Position, cheatedReset: Boolean = true) {
        attractMode = false
        if (cheatedReset) isCurrentRunCheated = false
        runScore = 0
        score = 0
        pendingRings = 0
        pendingDiamonds = 0
        bouncing = false
        speedMode = false
        showReviveOffer = false
        reviveSnapshot = null
        loadStage(from, refill = true, attract = false)
    }

    fun loadStage(pos: Position, refill: Boolean, attract: Boolean) {
        position = pos
        val worlds = worldsLookup()
        val world = worlds.firstOrNull { it.id == pos.world } ?: worlds.first()
        val targets = targetsLookup()
        val spec = LevelGenerator.buildStage(world, pos.stageInWorld, targets, min(canvasW * 0.30f, canvasH * 0.17f).coerceAtLeast(160f))
        stage = spec
        knivesPerRound = spec.knifeCount
        ammoRemaining = spec.knifeCount
        applyLayout()
        val hilt = hiltAngularWidth
        val obsAngles = LevelGenerator.placeObstacles(spec.obstacleCount, hilt, random)
        attachments = obsAngles.map { StuckKnife(it, STARTER_SKIN_ID, isObstacle = true) }
        rotation = 0f
        rotationController.reset(spec.rotation, randomize = !attract)
        knifeY = restY
        knifeSpin = 0f
        bouncing = false
        speedMode = false
        overlayT = 0f
        collectedThisStageRings = 0
        collectedThisStageDiamonds = 0
        lastPrecision = null
        lastScoreEvent = null
        particles.clear()
        destruction.stop()
        speedSpawnTimer = if (attract) -1f else 5f + random.nextFloat() * 5f
        collectibleSpawnTimer = if (attract) 999f else 1.2f + random.nextFloat() * 1.4f
        bossName = if (spec.isBoss) spec.world.boss.name else ""
        playState = when {
            attract -> PlayState.Attract
            spec.isBoss -> {
                introT = BOSS_INTRO_SEC
                PlayState.BossIntro
            }
            else -> PlayState.Ready
        }
    }

    fun update(dt: Float) {
        val clamped = dt.coerceAtMost(0.05f)
        elapsed += clamped
        frameEpoch++
        if (paused) return

        val omega = rotationController.step(clamped)
        rotation = GameMath.normalize(rotation + omega * clamped)

        particles.update(clamped, 520f)
        if (shakeT > 0f) shakeT = (shakeT - clamped).coerceAtLeast(0f)

        when (playState) {
            PlayState.Attract -> {
                if (consumeTap()) pendingTap = false
            }
            PlayState.BossIntro -> {
                introT -= clamped
                consumeTap()
                if (introT <= 0f) playState = PlayState.Ready
            }
            PlayState.Ready -> handleReady(clamped)
            PlayState.Throwing -> handleThrowing(clamped)
            PlayState.Destroying -> handleDestroying(clamped)
            PlayState.StageClear, PlayState.WorldVictory -> overlayT += clamped
            PlayState.GameOver -> handleGameOver(clamped)
            PlayState.ReviveCountdown -> {
                reviveT -= clamped
                consumeTap()
                if (reviveT <= 0f) playState = PlayState.Ready
            }
        }

        maybeSpawnPickups(clamped)
    }

    private fun handleReady(dt: Float) {
        if (consumeTap() && ammoRemaining > 0 && !attractMode) {
            launch()
        }
    }

    private fun launch() {
        val skin = equippedSkin()
        ammoRemaining = (ammoRemaining - 1).coerceAtLeast(0)
        playState = PlayState.Throwing
        bouncing = false
        knifeY = restY
        prevTipX = cx
        prevTipY = knifeY - knifeLength
        val dist = (knifeY - knifeLength) - (cy + radius)
        val speed = throwSpeedPx()
        if (speed > 1f) rotationController.notifyThrowApproaching(dist / speed)
    }

    private fun throwSpeedPx(): Float {
        val base = canvasH * 1.85f
        val mul = equippedSkin().throwSpeed * if (speedMode) SPEED_MODE_MULTIPLIER else 1f
        return base * mul
    }

    private fun handleThrowing(dt: Float) {
        consumeTap()
        val speed = throwSpeedPx()
        val prev = GameMath.Vec2(cx, knifeY - knifeLength)
        knifeY -= speed * dt
        val curr = GameMath.Vec2(cx, knifeY - knifeLength)
        prevTipX = curr.x
        prevTipY = curr.y
        val hit = GameMath.segmentCircleIntersection(prev, curr, GameMath.Vec2(cx, cy), radius)
        if (hit != null) {
            resolveImpact(hit.point.x, hit.point.y)
            return
        }
        if (knifeY < -knifeLength) {
            fail(struckIndex = -1)
        }
    }

    private fun resolveImpact(ix: Float, iy: Float) {
        val impact = GameMath.impactAngle(ix, iy, cx, cy)
        val rel = GameMath.normalize(impact - rotation)
        collectAt(rel, ix, iy)
        val knives = attachments.filterIsInstance<StuckKnife>()
        val occupied = knives.map { it.relAngle }
        if (GameMath.isHiltStrike(rel, occupied, hiltAngularWidth)) {
            val idx = knives.indexOfFirst { GameMath.angularDistance(rel, it.relAngle) < hiltAngularWidth }
            fail(struckIndex = idx, rel = rel)
            return
        }
        stick(rel, ix, iy)
    }

    private fun collectAt(rel: Float, ix: Float, iy: Float) {
        val keep = ArrayList<WheelAttachment>(attachments.size)
        var changed = false
        for (a in attachments) {
            if (a is Collectible && GameMath.angularDistance(rel, a.relAngle) < hiltAngularWidth * 1.6f) {
                changed = true
                when (a.kind) {
                    PickupKind.COIN -> {
                        collectedThisStageRings += 5
                        pendingRings += 5
                    }
                    PickupKind.DIAMOND -> {
                        collectedThisStageDiamonds += 1
                        pendingDiamonds += 1
                    }
                    PickupKind.SPEED -> speedMode = true
                }
                particles.spawnBurst(ix, iy, rel + rotation, 10, pickupColor(a.kind), 380f, random)
            } else {
                keep += a
            }
        }
        if (changed) attachments = keep
    }

    private fun pickupColor(kind: PickupKind): Int = when (kind) {
        PickupKind.COIN -> 0xFFFFD24A.toInt()
        PickupKind.DIAMOND -> 0xFF7AF0FF.toInt()
        PickupKind.SPEED -> 0xFF39FF14.toInt()
    }

    private fun stick(rel: Float, ix: Float, iy: Float) {
        val knives = attachments.filterIsInstance<StuckKnife>()
        val occupied = knives.map { it.relAngle }
        val nearest = GameMath.nearestOccupied(rel, occupied)
        val band: PrecisionBand
        val points: Int
        if (nearest == null) {
            band = PrecisionBand.FIRST
            points = SCORE_FIRST_KNIFE
        } else {
            val dist = GameMath.angularDistance(rel, nearest)
            val perfect = GameMath.perfectBand(hiltAngularWidth)
            if (dist in perfect) {
                band = PrecisionBand.PERFECT
                points = SCORE_PERFECT
            } else {
                band = PrecisionBand.GOOD
                points = SCORE_GOOD
            }
        }
        score += points
        runScore += points
        lastPrecision = band
        lastScoreEvent = ScoreEventInternal(band, points, ix, iy, elapsed)
        attachments = attachments + StuckKnife(rel, equippedKnifeId, isObstacle = false)
        particles.spawnBurst(ix, iy, rel + rotation, 10, equippedSkin().colorArgb.toInt(), 420f, random)
        triggerShake(10f)
        knifeY = restY
        val playerStuck = attachments.filterIsInstance<StuckKnife>().count { !it.isObstacle }
        if (ammoRemaining == 0 && playerStuck >= knivesPerRound) {
            beginClear()
        } else {
            playState = PlayState.Ready
        }
    }

    private fun beginClear() {
        score += STAGE_CLEAR_BONUS
        runScore += STAGE_CLEAR_BONUS
        val replay = compareFurthest()
        val rings = if (replay) RINGS_PER_STAGE_REPLAY else RINGS_PER_STAGE
        pendingRings += rings
        val spec = stage
        if (spec?.isBoss == true) {
            pendingDiamonds += if (replay) DIAMONDS_PER_BOSS_REPLAY else DIAMONDS_PER_BOSS
        }
        destruction.start(spec?.target?.style ?: com.knifehit.game.model.TargetStyle.WOOD, cx, cy, rotation, stuckKnives, random)
        overlayT = 0f
        playState = PlayState.Destroying
        triggerShake(18f)
    }

    private fun compareFurthest(): Boolean {
        val f = furthest
        val p = position
        return p.world < f.world || (p.world == f.world && p.stageInWorld < f.stageInWorld)
    }

    private fun handleDestroying(dt: Float) {
        consumeTap()
        destruction.update(dt)
        overlayT += dt
        if (overlayT > 0.55f) {
            playState = if (stage?.isBoss == true) PlayState.WorldVictory else PlayState.StageClear
        }
    }

    fun advanceAfterClear(): AdvanceResult {
        val spec = stage ?: return AdvanceResult.Stay
        val next = position.next()
        furthest = com.knifehit.game.model.maxPosition(furthest, position)
        return if (spec.isBoss) {
            if (next == null) {
                AdvanceResult.GameComplete
            } else {
                unlockedWorlds = unlockedWorlds + next.world
                AdvanceResult.WorldCleared(next)
            }
        } else {
            AdvanceResult.NextStage(next ?: position)
        }
    }

    sealed interface AdvanceResult {
        data object Stay : AdvanceResult
        data class NextStage(val position: Position) : AdvanceResult
        data class WorldCleared(val nextWorldStart: Position) : AdvanceResult
        data object GameComplete : AdvanceResult
    }

    private fun fail(struckIndex: Int, rel: Float = 0f) {
        reviveSnapshot = ReviveSnapshot(
            attachments = attachments.toList(),
            struckIndex = struckIndex,
            rotation = rotation,
            ammoRemaining = ammoRemaining,
            score = score,
            speedMode = speedMode,
        )
        bouncing = true
        bounceVx = if (random.nextBoolean()) 220f else -220f
        bounceVy = 80f
        knifeSpin = 8f * if (bounceVx > 0) 1f else -1f
        playState = PlayState.GameOver
        overlayT = 0f
        showReviveOffer = !attractMode
        triggerShake(22f)
        particles.spawnBurst(cx, cy + radius, rel + rotation, 14, 0xFFFF5577.toInt(), 500f, random)
    }

    private fun handleGameOver(dt: Float) {
        overlayT += dt
        if (bouncing) {
            bounceVy += 1400f * dt
            knifeY += bounceVy * dt
            // knifeX offset stored in bounceVx as translation; rotation of knife
            knifeSpin += bounceVx * dt * 0.02f
            if (knifeY > canvasH + knifeLength) bouncing = false
        }
    }

    fun applyRevive() {
        val snap = reviveSnapshot ?: return
        isCurrentRunCheated = true
        val list = snap.attachments.toMutableList()
        if (snap.struckIndex in list.indices && list[snap.struckIndex] is StuckKnife) {
            list.removeAt(snap.struckIndex)
        }
        attachments = list
        rotation = snap.rotation
        ammoRemaining = snap.ammoRemaining
        score = snap.score
        speedMode = snap.speedMode
        bouncing = false
        knifeY = restY
        showReviveOffer = false
        reviveT = REVIVE_COUNTDOWN_SEC
        playState = PlayState.ReviveCountdown
    }

    fun declineReviveAndResetWorld() {
        showReviveOffer = false
        reviveSnapshot = null
        bouncing = false
        val start = position.worldStart()
        loadStage(start, refill = true, attract = false)
    }

    fun tryAgainSameWorld() {
        isCurrentRunCheated = false
        runScore = 0
        score = 0
        pendingRings = 0
        pendingDiamonds = 0
        startRun(position.worldStart())
    }

    fun drainPendingCurrency(): Pair<Int, Int> {
        val r = pendingRings
        val d = pendingDiamonds
        pendingRings = 0
        pendingDiamonds = 0
        return r to d
    }

    fun shakeOffset(motionEnabled: Boolean): Pair<Float, Float> {
        if (!motionEnabled || shakeT <= 0f) return 0f to 0f
        val u = (shakeT / SHAKE_DURATION_SEC).coerceIn(0f, 1f)
        val mag = shakeAmp * u
        val a = elapsed * 73.1f
        return (cos(a) * mag) to (sin(a * 1.37f) * mag)
    }

    private fun triggerShake(amp: Float) {
        shakeAmp = amp
        shakeT = SHAKE_DURATION_SEC
    }

    private fun maybeSpawnPickups(dt: Float) {
        if (attractMode) return
        if (playState != PlayState.Ready && playState != PlayState.Throwing) return
        collectibleSpawnTimer -= dt
        if (collectibleSpawnTimer <= 0f) {
            collectibleSpawnTimer = 3.5f + random.nextFloat() * 3f
            val kind = if (random.nextFloat() < 0.12f) PickupKind.DIAMOND else PickupKind.COIN
            spawnCollectible(kind)
        }
        val playerStuck = attachments.filterIsInstance<StuckKnife>().count { !it.isObstacle }
        if (speedSpawnTimer >= 0f && playerStuck >= 2) {
            speedSpawnTimer -= dt
            if (speedSpawnTimer <= 0f) {
                speedSpawnTimer = -1f
                spawnCollectible(PickupKind.SPEED)
            }
        }
    }

    private fun spawnCollectible(kind: PickupKind) {
        val occupied = attachments.map { it.relAngle }
        val half = hiltAngularWidth * 0.7f
        val angle = FreeArcs.pickInFreeArc(occupied, half, COLLECTIBLE_CLEARANCE_FLOOR, random) ?: return
        attachments = attachments + Collectible(angle, kind)
    }

    fun skinColor(id: String): Long =
        skinsLookup()[id]?.colorArgb ?: 0xFFD7E3EC

    fun worldTheme(): com.knifehit.game.model.WorldTheme =
        stage?.world?.theme ?: worldsLookup().first().theme
}
