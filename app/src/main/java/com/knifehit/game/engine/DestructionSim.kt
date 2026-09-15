package com.knifehit.game.engine

import com.knifehit.game.model.StuckKnife
import com.knifehit.game.model.TAU
import com.knifehit.game.model.TargetStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class DestructionKind { SHATTER, SPIN_OFF, SLICES }

data class Fragment(
    var angle: Float,
    var span: Float,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var spin: Float,
    var rot: Float,
    var alive: Boolean = true,
    var knives: List<StuckKnife> = emptyList(),
)

class DestructionSim {
    val fragments = ArrayList<Fragment>(16)
    var kind: DestructionKind = DestructionKind.SHATTER
    var spinVel = 0f
    var targetX = 0f
    var targetY = 0f
    var targetRot = 0f
    var age = 0f
    var active = false
        private set

    fun stop() {
        active = false
        fragments.clear()
        age = 0f
    }

    fun start(
        style: TargetStyle,
        cx: Float,
        cy: Float,
        rotation: Float,
        knives: List<StuckKnife>,
        random: Random,
    ) {
        fragments.clear()
        age = 0f
        active = true
        targetX = cx
        targetY = cy
        targetRot = rotation
        spinVel = 0f
        kind = when (style) {
            TargetStyle.WOOD, TargetStyle.STONE -> DestructionKind.SHATTER
            TargetStyle.METAL, TargetStyle.SECTORS -> DestructionKind.SPIN_OFF
            TargetStyle.WEDGES -> DestructionKind.SLICES
        }
        val n = when (kind) {
            DestructionKind.SPIN_OFF -> 1
            DestructionKind.SLICES -> 8
            DestructionKind.SHATTER -> 10
        }
        if (kind == DestructionKind.SPIN_OFF) {
            spinVel = 8f
            fragments += Fragment(0f, TAU, cx, cy, 80f, -40f, 0f, rotation, true, knives)
            return
        }
        val span = TAU / n
        for (i in 0 until n) {
            val a = i * span
            val mid = a + span * 0.5f
            val riding = knives.filter { k ->
                val d = GameMath.normalize(k.relAngle - a)
                d >= 0f && d < span
            }
            val speed = 220f + random.nextFloat() * 180f
            val outward = if (kind == DestructionKind.SLICES) 140f else speed
            fragments += Fragment(
                angle = a,
                span = span,
                x = cx,
                y = cy,
                vx = cos(mid + rotation) * outward * 0.35f,
                vy = sin(mid + rotation) * outward * 0.35f + 40f,
                spin = (random.nextFloat() - 0.5f) * 4f,
                rot = rotation,
                knives = riding,
            )
        }
    }

    fun update(dt: Float): Boolean {
        if (!active) return false
        age += dt
        when (kind) {
            DestructionKind.SPIN_OFF -> {
                spinVel *= 1f + 3.2f * dt
                val f = fragments.firstOrNull() ?: return false
                f.rot += spinVel * dt
                f.x += f.vx * dt * (1f + age * 4f)
                f.y += 420f * dt * age
                targetRot = f.rot
                targetX = f.x
                targetY = f.y
            }
            else -> {
                for (f in fragments) {
                    f.vy += 980f * dt
                    f.x += f.vx * dt
                    f.y += f.vy * dt
                    f.rot += f.spin * dt
                }
            }
        }
        if (age > 1.35f) {
            active = false
            return false
        }
        return true
    }
}
