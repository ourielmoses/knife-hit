package com.knifehit.game.engine

import com.knifehit.game.model.MAX_KNIVES
import com.knifehit.game.model.Position
import com.knifehit.game.model.RotationProfile
import com.knifehit.game.model.SAFETY_MARGIN
import com.knifehit.game.model.StageSpec
import com.knifehit.game.model.TAU
import com.knifehit.game.model.TargetBlueprint
import com.knifehit.game.model.WorldDef
import com.knifehit.game.model.stagesIn
import com.knifehit.game.model.worldCount
import kotlin.math.roundToInt
import kotlin.random.Random

object LevelGenerator {
    const val WORLD_STEP = 1f / 7f
    const val INTRA_RANGE = 1f
    const val BOSS_SPIKE = 0.35f
    const val BASE_KNIVES = 5
    const val KNIFE_WIDTH_FACTOR = 0.22f

    fun difficulty(world: Int, stageInWorld: Int, isBoss: Boolean): Float {
        val stages = stagesIn(world).coerceAtLeast(1)
        val intraStep = if (stages <= 1) 0f else INTRA_RANGE / (stages - 1)
        return (world - 1) * WORLD_STEP +
            (stageInWorld - 1) * intraStep +
            if (isBoss) BOSS_SPIKE else 0f
    }

    fun desiredObstacles(world: Int, stageInWorld: Int): Int {
        if (stageInWorld == 1) return 0
        val stages = stagesIn(world).coerceAtLeast(2)
        val p = (stageInWorld - 1f) / (stages - 1f)
        val maxForWorld = 2 + (world / 4)
        return (p * maxForWorld).roundToInt()
    }

    fun proportionalRadius(baseRadiusPx: Float, blueprint: TargetBlueprint): Float =
        baseRadiusPx * (blueprint.radiusDp / com.knifehit.game.model.REFERENCE_RADIUS_DP)

    fun knifeWidthPx(radiusPx: Float): Float = radiusPx * KNIFE_WIDTH_FACTOR

    fun capacityFor(radiusPx: Float): Int {
        val w = knifeWidthPx(radiusPx)
        return GameMath.maxKnives(radiusPx, w, SAFETY_MARGIN).coerceAtLeast(3)
    }

    fun rotationProfile(world: Int, stageInWorld: Int, isBoss: Boolean, plain: Boolean): RotationProfile {
        if (plain) {
            val base = 0.85f + (world - 1) * 0.08f
            return RotationProfile(
                baseSpeed = base,
                speedVariance = 0f,
                pauseChance = 0f,
                pauseDuration = 0f,
                reversalChance = 0f,
                reversalEase = 0.35f,
            )
        }
        val d = difficulty(world, stageInWorld, isBoss)
        val base = 0.9f + d * 1.15f
        return RotationProfile(
            baseSpeed = base,
            speedVariance = 0.12f + d * 0.35f,
            pauseChance = (0.04f + d * 0.12f).coerceAtMost(0.28f),
            pauseDuration = 0.35f + d * 0.15f,
            reversalChance = (0.05f + d * 0.14f).coerceAtMost(0.32f),
            reversalEase = (0.42f - d * 0.08f).coerceAtLeast(0.22f),
        )
    }

    fun buildStage(
        worldDef: WorldDef,
        stageInWorld: Int,
        targets: Map<String, TargetBlueprint>,
        layoutBaseRadius: Float = 220f,
    ): StageSpec {
        val world = worldDef.id
        val isBoss = stageInWorld == stagesIn(world)
        val ids = worldDef.targetIds
        val targetId = if (isBoss) worldDef.boss.targetId else ids[(stageInWorld - 1) % ids.size]
        val target = targets[targetId] ?: targets.values.first()
        val radius = proportionalRadius(layoutBaseRadius, target) *
            if (isBoss) worldDef.boss.radiusScale else 1f
        val capacity = capacityFor(radius)
        val d = difficulty(world, stageInWorld, isBoss)
        val desiredKnives = (BASE_KNIVES + (d * 4f).toInt()).coerceAtLeast(3)
        val knifeCount = minOf(desiredKnives, capacity, MAX_KNIVES)
        val plain = stageInWorld == 1
        var obstacles = if (plain) 0 else desiredObstacles(world, stageInWorld)
        val hilt = GameMath.hiltAngularWidth(knifeWidthPx(radius), radius)
        while (obstacles > 0) {
            val placed = placeholderObstacles(obstacles, hilt, Random(world * 100 + stageInWorld))
            val placeable = FreeArcs.placeableCount(placed, hilt)
            if (placeable >= knifeCount) break
            obstacles--
        }
        return StageSpec(
            position = Position(world, stageInWorld),
            isBoss = isBoss,
            target = target,
            knifeCount = knifeCount,
            obstacleCount = obstacles,
            rotation = rotationProfile(world, stageInWorld, isBoss, plain),
            world = worldDef,
        )
    }

    fun placeholderObstacles(count: Int, hilt: Float, random: Random): List<Float> {
        if (count <= 0) return emptyList()
        val sector = TAU / count
        val maxJitter = (sector * 0.18f).coerceAtMost(hilt * 0.4f)
        return List(count) { i ->
            GameMath.normalize(i * sector + (random.nextFloat() * 2f - 1f) * maxJitter)
        }
    }

    fun placeObstacles(count: Int, hilt: Float, random: Random): List<Float> =
        placeholderObstacles(count, hilt, random)

    fun allStages(
        worlds: List<WorldDef>,
        targets: Map<String, TargetBlueprint>,
        layoutBaseRadius: Float = 220f,
    ): List<StageSpec> {
        val out = ArrayList<StageSpec>()
        for (w in worlds) {
            val n = if (w.id in 1..worldCount()) stagesIn(w.id) else 3
            for (s in 1..n) out += buildStage(w, s, targets, layoutBaseRadius)
        }
        return out
    }
}
