package com.knifehit.game.engine

import com.knifehit.game.model.TAU
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

object GameMath {
    fun normalize(angle: Float): Float {
        var a = angle % TAU
        if (a < 0f) a += TAU
        return a
    }

    fun shortestDelta(a: Float, b: Float): Float {
        var d = normalize(b) - normalize(a)
        if (d > PI) d -= TAU
        if (d < -PI) d += TAU
        return d
    }

    fun angularDistance(a: Float, b: Float): Float = abs(shortestDelta(a, b))

    fun hiltAngularWidth(knifeWidthPx: Float, radiusPx: Float): Float {
        val r = max(radiusPx, 1f)
        return (knifeWidthPx / r).coerceIn(0.04f, 1.2f)
    }

    fun maxKnives(radiusPx: Float, knifeWidthPx: Float, safety: Float): Int {
        val width = hiltAngularWidth(knifeWidthPx, radiusPx)
        return max(1, floor((TAU / width) * safety).toInt())
    }

    fun perfectBand(hilt: Float): ClosedFloatingPointRange<Float> {
        val low = hilt * 1.05f
        val high = hilt * 1.40f
        return low..high
    }

    fun impactAngle(tipX: Float, tipY: Float, cx: Float, cy: Float): Float =
        atan2(tipY - cy, tipX - cx)

    data class Vec2(val x: Float, val y: Float) {
        fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
        fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
        fun times(s: Float) = Vec2(x * s, y * s)
        fun length(): Float = sqrt(x * x + y * y)
        fun distanceTo(o: Vec2): Float = minus(o).length()
    }

    data class Hit(val t: Float, val point: Vec2)

    /**
     * Swept segment-vs-circle. Returns the first intersection of segment p0→p1
     * with circle (center, radius), or null if none.
     */
    fun segmentCircleIntersection(
        p0: Vec2,
        p1: Vec2,
        center: Vec2,
        radius: Float,
    ): Hit? {
        val d = p1.minus(p0)
        val f = p0.minus(center)
        val a = d.x * d.x + d.y * d.y
        if (a < 1e-8f) {
            return if (p0.distanceTo(center) <= radius) Hit(0f, p0) else null
        }
        val b = 2f * (f.x * d.x + f.y * d.y)
        val c = f.x * f.x + f.y * f.y - radius * radius
        val disc = b * b - 4f * a * c
        if (disc < 0f) return null
        val sqrtDisc = sqrt(disc)
        val t1 = (-b - sqrtDisc) / (2f * a)
        val t2 = (-b + sqrtDisc) / (2f * a)
        val t = when {
            t1 in 0f..1f -> t1
            t2 in 0f..1f && p0.distanceTo(center) > radius -> t2
            t1 < 0f && t2 > 0f && p0.distanceTo(center) <= radius -> 0f
            else -> return null
        }
        val point = Vec2(p0.x + d.x * t, p0.y + d.y * t)
        return Hit(t, point)
    }

    fun isHiltStrike(rel: Float, occupied: List<Float>, hiltWidth: Float): Boolean {
        return occupied.any { angularDistance(rel, it) < hiltWidth }
    }

    fun nearestOccupied(rel: Float, occupied: List<Float>): Float? {
        if (occupied.isEmpty()) return null
        return occupied.minBy { angularDistance(rel, it) }
    }
}
