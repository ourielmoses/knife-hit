package com.knifehit.game.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticlePool(private val capacity: Int = 256) {
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val vx = FloatArray(capacity)
    val vy = FloatArray(capacity)
    val life = FloatArray(capacity)
    val maxLife = FloatArray(capacity)
    val argb = IntArray(capacity)
    private var cursor = 0
    var active = 0
        private set

    fun clear() {
        life.fill(0f)
        active = 0
        cursor = 0
    }

    fun spawnBurst(
        cx: Float,
        cy: Float,
        angle: Float,
        count: Int,
        color: Int,
        speed: Float,
        random: Random,
    ) {
        repeat(count) {
            val i = nextSlot()
            val spread = (random.nextFloat() - 0.5f) * 1.4f
            val a = angle + spread
            val s = speed * (0.55f + random.nextFloat() * 0.8f)
            x[i] = cx
            y[i] = cy
            vx[i] = cos(a) * s
            vy[i] = sin(a) * s
            val ml = 0.28f + random.nextFloat() * 0.32f
            maxLife[i] = ml
            life[i] = ml
            argb[i] = color
        }
    }

    fun update(dt: Float, gravity: Float) {
        var live = 0
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= dt
            if (life[i] <= 0f) continue
            vx[i] *= 0.985f
            vy[i] += gravity * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            live++
        }
        active = live
    }

    private fun nextSlot(): Int {
        var i = cursor
        repeat(capacity) {
            if (life[i] <= 0f) {
                cursor = (i + 1) % capacity
                return i
            }
            i = (i + 1) % capacity
        }
        cursor = (cursor + 1) % capacity
        return cursor
    }
}
