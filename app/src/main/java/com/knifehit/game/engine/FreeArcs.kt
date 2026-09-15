package com.knifehit.game.engine

import com.knifehit.game.model.COLLECTIBLE_CLEARANCE_FLOOR
import com.knifehit.game.model.TAU
import kotlin.math.max
import kotlin.random.Random

data class Arc(val start: Float, val end: Float) {
    val length: Float get() {
        val s = GameMath.normalize(start)
        val e = GameMath.normalize(end)
        return if (e >= s) e - s else e + TAU - s
    }
}

object FreeArcs {
    fun occupiedIntervals(
        angles: List<Float>,
        halfWidth: Float,
    ): List<Pair<Float, Float>> {
        if (angles.isEmpty()) return emptyList()
        val raw = angles.map { a ->
            val n = GameMath.normalize(a)
            (n - halfWidth) to (n + halfWidth)
        }.sortedBy { GameMath.normalize(it.first) }

        val linearized = ArrayList<Pair<Float, Float>>()
        for ((from, to) in raw) {
            val s = from
            val e = to
            if (s < 0f) {
                linearized += (s + TAU) to TAU
                linearized += 0f to e
            } else if (e > TAU) {
                linearized += s to TAU
                linearized += 0f to (e - TAU)
            } else {
                linearized += s to e
            }
        }
        linearized.sortBy { it.first }
        val merged = ArrayList<Pair<Float, Float>>()
        for (iv in linearized) {
            if (merged.isEmpty() || iv.first > merged.last().second) {
                merged += iv
            } else {
                val last = merged.removeAt(merged.lastIndex)
                merged += last.first to max(last.second, iv.second)
            }
        }
        return merged
    }

    fun gaps(occupied: List<Pair<Float, Float>>): List<Arc> {
        if (occupied.isEmpty()) return listOf(Arc(0f, TAU))
        val gaps = ArrayList<Arc>()
        for (i in occupied.indices) {
            val end = occupied[i].second
            val startNext = if (i + 1 < occupied.size) occupied[i + 1].first else occupied[0].first + TAU
            if (startNext > end) gaps += Arc(end, startNext)
        }
        return gaps
    }

    /**
     * How many additional knives can land given occupied angles.
     * Each gap of width G holds floor(G / w) - 1 knives (needs clearance on both sides).
     */
    fun placeableCount(occupiedAngles: List<Float>, hiltWidth: Float): Int {
        if (occupiedAngles.isEmpty()) {
            return max(0, kotlin.math.floor(TAU / hiltWidth).toInt() - 1)
        }
        val intervals = occupiedIntervals(occupiedAngles, hiltWidth * 0.5f)
        val gapList = gaps(intervals)
        return gapList.sumOf { placeableIn(it.length, hiltWidth) }
    }

    fun placeableIn(gap: Float, width: Float): Int {
        if (width <= 0f) return 0
        return max(0, kotlin.math.floor(gap / width).toInt() - 1)
    }

    fun pickInFreeArc(
        occupied: List<Float>,
        itemHalfWidth: Float,
        clearance: Float,
        random: Random,
    ): Float? {
        val half = itemHalfWidth + max(clearance, COLLECTIBLE_CLEARANCE_FLOOR) * 0.5f
        val intervals = occupiedIntervals(occupied, half)
        val gapList = gaps(intervals).filter { it.length > itemHalfWidth * 2f + 0.02f }
        if (gapList.isEmpty()) return null
        val total = gapList.sumOf { it.length.toDouble() }.toFloat()
        var tick = random.nextFloat() * total
        for (g in gapList) {
            if (tick <= g.length) {
                val s = GameMath.normalize(g.start)
                return GameMath.normalize(s + tick)
            }
            tick -= g.length
        }
        return GameMath.normalize(gapList.last().start + gapList.last().length * 0.5f)
    }
}
