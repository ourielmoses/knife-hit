package com.knifehit.game.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.knifehit.game.engine.DestructionKind
import com.knifehit.game.engine.GameEngine
import com.knifehit.game.model.Collectible
import com.knifehit.game.model.PickupKind
import com.knifehit.game.model.PlayState
import com.knifehit.game.model.StuckKnife
import com.knifehit.game.model.WorldTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameWorld(
    engine: GameEngine,
    running: Boolean,
    throttle: Boolean,
    motionEnabled: Boolean,
    particlesEnabled: Boolean,
    batterySaver: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(running, throttle, batterySaver) {
        var last = withFrameNanos { it }
        val minDt = if (throttle || batterySaver) 1f / 30f else 0f
        var acc = 0f
        while (running) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                acc += dt
                if (acc >= minDt) {
                    engine.update(if (minDt == 0f) dt else acc)
                    acc = 0f
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { sz: IntSize -> engine.resize(sz.width.toFloat(), sz.height.toFloat()) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onTap()
                }
            },
    ) {
        val epoch = engine.frameEpoch
        @Suppress("UNUSED_VARIABLE")
        val read = epoch
        drawGame(engine, motionEnabled, particlesEnabled, batterySaver)
    }
}

fun DrawScope.drawGame(
    engine: GameEngine,
    motionEnabled: Boolean,
    particlesEnabled: Boolean,
    batterySaver: Boolean,
) {
    val theme = engine.worldTheme()
    drawBackground(theme, engine.elapsed, engine.canvasW, engine.canvasH)
    val (sx, sy) = engine.shakeOffset(motionEnabled)
    translate(sx, sy) {
        drawPlayfield(engine, particlesEnabled, batterySaver, theme)
    }
    drawAmmo(engine)
    drawScoreDigits(engine)
}

private fun DrawScope.drawBackground(theme: WorldTheme, t: Float, w: Float, h: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(theme.bgTopArgb.toComposeColor(), theme.bgBottomArgb.toComposeColor()),
        ),
    )
    val glow = theme.glowArgb.toComposeColor().copy(alpha = 0.08f)
    val ox = sin(t * 0.15f) * 24f
    drawCircle(glow, min(w, h) * 0.55f, Offset(w * 0.5f + ox, h * 0.22f))
}

private fun DrawScope.drawPlayfield(
    engine: GameEngine,
    particlesEnabled: Boolean,
    batterySaver: Boolean,
    theme: WorldTheme,
) {
    val center = Offset(engine.cx, engine.cy)
    val spec = engine.stage
    val pulse = (0.5f + 0.5f * sin(engine.elapsed * 6f)).toFloat()
    val dest = engine.destruction
    if (dest.active && dest.kind != DestructionKind.SPIN_OFF) {
        val bp = spec?.target
        if (bp != null) {
            for (f in dest.fragments) {
                val path = Path()
                path.moveTo(f.x, f.y)
                val a0 = f.angle + f.rot
                val a1 = f.angle + f.span + f.rot
                path.lineTo(f.x + cos(a0) * engine.radius, f.y + sin(a0) * engine.radius)
                path.lineTo(f.x + cos(a1) * engine.radius, f.y + sin(a1) * engine.radius)
                path.close()
                drawPath(path, bp.baseColorArgb.toComposeColor())
                for (k in f.knives) {
                    val skin = engine.equippedSkin().let { if (k.isObstacle) engine.let { e ->
                        com.knifehit.game.model.KnifeSkin(
                            k.skinId, "", e.skinColor(k.skinId), 1f, 0, 0, true,
                            com.knifehit.game.model.Timbre.HEAVY,
                        )
                    } else it }
                    drawStuckKnife(skin, Offset(f.x, f.y), engine.radius, k.relAngle, f.rot, engine.knifeLength, engine.knifeWidth)
                }
            }
        }
    } else {
        val rot = if (dest.active) dest.targetRot else engine.rotation
        val c = if (dest.active) Offset(dest.targetX, dest.targetY) else center
        if (spec != null) {
            drawTarget(spec.target, c, engine.radius, rot, spec.isBoss, pulse)
            if (spec.isBoss) drawBossExtras(spec.world.id, c, engine.radius, rot, engine.elapsed)
        }
        for (a in engine.attachments) {
            when (a) {
                is StuckKnife -> {
                    val skinCol = engine.skinColor(a.skinId)
                    val skin = com.knifehit.game.model.KnifeSkin(
                        a.skinId, "", skinCol, 1f, 0, 0, true, com.knifehit.game.model.Timbre.HEAVY,
                    )
                    drawStuckKnife(skin, c, engine.radius, a.relAngle, rot, engine.knifeLength, engine.knifeWidth)
                }
                is Collectible -> drawCollectible(a, c, engine.radius, rot, engine.elapsed)
            }
        }
    }

    if (engine.playState == PlayState.Throwing || engine.playState == PlayState.Ready ||
        engine.playState == PlayState.BossIntro || engine.playState == PlayState.ReviveCountdown ||
        engine.playState == PlayState.Attract
    ) {
        translate(engine.cx, engine.knifeY) {
            drawKnife(engine.equippedSkin(), engine.knifeLength, engine.knifeWidth, tipUp = true)
        }
    } else if (engine.playState == PlayState.GameOver && engine.bouncing) {
        translate(engine.cx + engine.bounceVx * 0.15f, engine.knifeY) {
            rotate(Math.toDegrees(engine.knifeSpin.toDouble()).toFloat()) {
                drawKnife(engine.equippedSkin(), engine.knifeLength, engine.knifeWidth, tipUp = true)
            }
        }
    }

    if (particlesEnabled) {
        val max = if (batterySaver) engine.particles.x.size / 2 else engine.particles.x.size
        for (i in 0 until max) {
            val life = engine.particles.life[i]
            if (life <= 0f) continue
            val a = (life / engine.particles.maxLife[i]).coerceIn(0f, 1f)
            val x = engine.particles.x[i]
            val y = engine.particles.y[i]
            val px = x - engine.particles.vx[i] * 0.02f
            val py = y - engine.particles.vy[i] * 0.02f
            drawLine(
                color = engine.particles.argb[i].toComposeColor().copy(alpha = a),
                start = Offset(px, py),
                end = Offset(x, y),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawCollectible(
    c: Collectible,
    center: Offset,
    radius: Float,
    rot: Float,
    t: Float,
) {
    val a = c.relAngle + rot
    val p = Offset(center.x + cos(a) * radius, center.y + sin(a) * radius)
    when (c.kind) {
        PickupKind.COIN -> {
            rotate(t * 120f, pivot = p) {
                drawCircle(0xFFFFD24A.toComposeColor(), 16f, p)
                drawCircle(0xFFFFF0B0.toComposeColor(), 16f, p, style = Stroke(3f))
            }
        }
        PickupKind.DIAMOND -> {
            val path = Path()
            path.moveTo(p.x, p.y - 18f)
            path.lineTo(p.x + 12f, p.y)
            path.lineTo(p.x, p.y + 18f)
            path.lineTo(p.x - 12f, p.y)
            path.close()
            drawPath(path, 0xFF7AF0FF.toComposeColor().copy(alpha = 0.7f + 0.3f * ((sin(t * 8f) + 1f) * 0.5f)))
        }
        PickupKind.SPEED -> {
            val s = 1f + 0.12f * sin(t * 8f)
            drawLine(0xFF39FF14.toComposeColor(), Offset(p.x - 10f * s, p.y + 8f), Offset(p.x, p.y - 12f * s), 5f, StrokeCap.Round)
            drawLine(0xFF39FF14.toComposeColor(), Offset(p.x, p.y - 12f * s), Offset(p.x + 10f * s, p.y + 8f), 5f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawAmmo(engine: GameEngine) {
    val iconH = size.height * 0.035f
    val x = size.width * 0.08f
    val baseY = size.height * 0.93f
    val n = engine.ammoRemaining
    val compact = n > 10
    val shown = if (compact) 1 else n
    val skin = engine.equippedSkin()
    for (i in 0 until shown) {
        val y = baseY - i * iconH * 1.25f
        translate(x, y) {
            rotate(-25f) {
                drawKnife(skin, iconH * 1.6f, iconH * 0.35f, tipUp = true)
            }
        }
    }
    if (compact) {
        // numeric remainder drawn as ticks rather than glyphs
        val extra = n
        val yy = baseY - iconH * 1.6f
        repeat(extra.coerceAtMost(12)) { i ->
            drawCircle(skin.colorArgb.toComposeColor(), 3f, Offset(x + 22f + i * 7f, yy))
        }
    }
}

private fun DrawScope.drawScoreDigits(engine: GameEngine) {
    val s = engine.runScore.toString()
    val cx = size.width * 0.5f
    val y = size.height * 0.06f
    val w = 10f
    val gap = 16f
    val total = s.length * gap
    s.forEachIndexed { i, ch ->
        drawDigit(ch, Offset(cx - total / 2f + i * gap, y), w, 0xFFEEF7FF.toComposeColor())
    }
}

private fun DrawScope.drawDigit(ch: Char, origin: Offset, u: Float, color: Color) {
    val d = ch - '0'
    if (d !in 0..9) return
    val segs = arrayOf(
        listOf(1, 1, 1, 1, 1, 1, 0),
        listOf(0, 1, 1, 0, 0, 0, 0),
        listOf(1, 1, 0, 1, 1, 0, 1),
        listOf(1, 1, 1, 1, 0, 0, 1),
        listOf(0, 1, 1, 0, 0, 1, 1),
        listOf(1, 0, 1, 1, 0, 1, 1),
        listOf(1, 0, 1, 1, 1, 1, 1),
        listOf(1, 1, 1, 0, 0, 0, 0),
        listOf(1, 1, 1, 1, 1, 1, 1),
        listOf(1, 1, 1, 1, 0, 1, 1),
    )[d]
    fun seg(a: Offset, b: Offset) = drawLine(color, origin + a, origin + b, 3f, StrokeCap.Square)
    if (segs[0] == 1) seg(Offset(1f, 0f), Offset(u, 0f))
    if (segs[1] == 1) seg(Offset(u, 0f), Offset(u, u))
    if (segs[2] == 1) seg(Offset(u, u), Offset(u, u * 2))
    if (segs[3] == 1) seg(Offset(1f, u * 2), Offset(u, u * 2))
    if (segs[4] == 1) seg(Offset(0f, u), Offset(0f, u * 2))
    if (segs[5] == 1) seg(Offset(0f, 0f), Offset(0f, u))
    if (segs[6] == 1) seg(Offset(1f, u), Offset(u, u))
}
