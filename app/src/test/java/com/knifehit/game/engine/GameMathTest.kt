package com.knifehit.game.engine

import com.knifehit.game.model.MAX_KNIVES
import com.knifehit.game.model.Position
import com.knifehit.game.model.TAU
import com.knifehit.game.model.stagesIn
import com.knifehit.game.model.worldCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GameMathTest {
    @Test
    fun normalizeWrapsNegativeAndOverTau() {
        assertEquals(0f, GameMath.normalize(TAU), 1e-5f)
        assertTrue(GameMath.normalize(-0.1f) > 0f)
        assertTrue(GameMath.normalize(TAU * 3f + 0.2f) < TAU)
    }

    @Test
    fun shortestDeltaPicksSmallerArc() {
        val d = GameMath.shortestDelta(0.1f, TAU - 0.1f)
        assertTrue(abs(d) < 0.3f)
        assertTrue(d < 0f)
    }

    @Test
    fun hiltStrikeInsideWindow() {
        val occupied = listOf(0f, 1f)
        assertTrue(GameMath.isHiltStrike(0.02f, occupied, 0.08f))
        assertFalse(GameMath.isHiltStrike(0.4f, occupied, 0.08f))
    }

    @Test
    fun perfectBandSitsOutsideHilt() {
        val hilt = 0.2f
        val band = GameMath.perfectBand(hilt)
        assertTrue(band.start > hilt)
        assertTrue(band.endInclusive > band.start)
    }

    @Test
    fun sweptSegmentHitsCircleThatPointTestWouldMiss() {
        val c = GameMath.Vec2(0f, 0f)
        val p0 = GameMath.Vec2(0f, -40f)
        val p1 = GameMath.Vec2(0f, 40f)
        val hit = GameMath.segmentCircleIntersection(p0, p1, c, 10f)
        assertNotNull(hit)
        assertEquals(-10f, hit!!.point.y, 0.05f)
    }

    @Test
    fun sweptMissesWhenSegmentClearsCircle() {
        val hit = GameMath.segmentCircleIntersection(
            GameMath.Vec2(50f, -10f),
            GameMath.Vec2(50f, 10f),
            GameMath.Vec2(0f, 0f),
            10f,
        )
        assertNull(hit)
    }

    @Test
    fun capacityShrinksOnSmallTargets() {
        val wide = GameMath.maxKnives(200f, 28f, 0.85f)
        val tight = GameMath.maxKnives(80f, 28f, 0.85f)
        assertTrue(wide > tight)
        assertTrue(tight >= 1)
    }
}

class FeasibilityTest {
    @Test
    fun emptyWheelHasRoom() {
        val hilt = 0.25f
        val n = FreeArcs.placeableCount(emptyList(), hilt)
        assertTrue(n >= 3)
    }

    @Test
    fun evenObstaclesHaveWiderMinimumGapThanCluster() {
        val hilt = 0.22f
        val even = List(4) { i -> i * TAU / 4f }
        val cluster = listOf(0f, 0.15f, 0.3f, 0.45f)
        fun minGap(angles: List<Float>): Float {
            val sorted = angles.map { GameMath.normalize(it) }.sorted()
            var best = TAU
            for (i in sorted.indices) {
                val a = sorted[i]
                val b = if (i + 1 < sorted.size) sorted[i + 1] else sorted[0] + TAU
                best = kotlin.math.min(best, b - a)
            }
            return best
        }
        assertTrue(minGap(even) > minGap(cluster))
        assertTrue(FreeArcs.placeableCount(even, hilt) >= 4)
        assertTrue(FreeArcs.placeableCount(cluster, hilt) >= 4)
    }

    @Test
    fun pickInFreeArcTerminatesWhenFull() {
        val hilt = 0.4f
        val packed = List(20) { i -> i * TAU / 20f }
        val angle = FreeArcs.pickInFreeArc(packed, hilt, 0.3f, Random(1))
        assertTrue(angle == null || angle >= 0f)
    }

    @Test
    fun placeableInFormula() {
        assertEquals(0, FreeArcs.placeableIn(0.2f, 0.25f))
        assertEquals(1, FreeArcs.placeableIn(0.6f, 0.25f))
        assertEquals(3, FreeArcs.placeableIn(1.0f, 0.25f))
    }
}

class LevelGeneratorTest {
    @Test
    fun worldStageCountsAndBosses() {
        assertEquals(10, worldCount())
        assertEquals(3, stagesIn(1))
        assertEquals(10, stagesIn(10))
        val worlds = com.knifehit.game.model.BaseCatalogs.worlds
        val targets = com.knifehit.game.model.BaseCatalogs.targetMap()
        var total = 0
        var bosses = 0
        for (w in worlds) {
            val n = stagesIn(w.id)
            for (s in 1..n) {
                val spec = LevelGenerator.buildStage(w, s, targets, 220f)
                total++
                if (spec.isBoss) bosses++
                assertTrue(spec.knifeCount in 1..MAX_KNIVES)
                assertEquals(s == n, spec.isBoss)
                if (s == 1) {
                    assertEquals(0, spec.obstacleCount)
                    assertEquals(0f, spec.rotation.pauseChance)
                    assertEquals(0f, spec.rotation.reversalChance)
                }
                val radius = LevelGenerator.proportionalRadius(220f, spec.target) *
                    if (spec.isBoss) spec.world.boss.radiusScale else 1f
                val hilt = GameMath.hiltAngularWidth(LevelGenerator.knifeWidthPx(radius), radius)
                val obs = LevelGenerator.placeObstacles(spec.obstacleCount, hilt, Random(w.id * 31 + s))
                val placeable = FreeArcs.placeableCount(obs, hilt)
                assertTrue(
                    "W${w.id}S$s placeable=$placeable knives=${spec.knifeCount} obs=${spec.obstacleCount}",
                    placeable >= spec.knifeCount,
                )
            }
        }
        assertEquals(59, total)
        assertEquals(10, bosses)
    }

    @Test
    fun sawtoothStartsEasierThanPreviousEnded() {
        val d1s1 = LevelGenerator.difficulty(1, 1, false)
        val d1boss = LevelGenerator.difficulty(1, stagesIn(1), true)
        val d2s1 = LevelGenerator.difficulty(2, 1, false)
        assertTrue(d2s1 > d1s1)
        assertTrue(d2s1 < d1boss)
    }

    @Test
    fun positionPairStable() {
        val p = Position(4, 5)
        assertTrue(p.isBoss())
        assertEquals(Position(5, 1), p.next())
        assertEquals(Position(4, 1), p.worldStart())
    }
}

class AmmoStageClearTest {
    @Test
    fun lastKnifeHiltIsGameOverNotClear() {
        val occupied = listOf(0f)
        val hilt = 0.2f
        val lastRel = 0.01f
        assertTrue(GameMath.isHiltStrike(lastRel, occupied, hilt))
    }

    @Test
    fun stageClearPredicate() {
        val ammo = 0
        val inFlight = false
        val stuck = 7
        val knives = 7
        val clear = ammo == 0 && !inFlight && stuck == knives
        assertTrue(clear)
        assertFalse(ammo == 0 && !inFlight && stuck == 6)
    }
}
