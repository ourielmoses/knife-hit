package com.knifehit.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.knifehit.game.model.TargetBlueprint
import com.knifehit.game.model.TargetStyle
import com.knifehit.game.model.TAU
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawTarget(
    bp: TargetBlueprint,
    center: Offset,
    radius: Float,
    rotationRad: Float,
    boss: Boolean,
    pulse: Float,
) {
    val deg = Math.toDegrees(rotationRad.toDouble()).toFloat()
    rotate(deg, pivot = center) {
        drawCircle(bp.baseColorArgb.toComposeColor(), radius, center)
        when (bp.style) {
            TargetStyle.WOOD -> drawGrain(bp, center, radius)
            TargetStyle.METAL -> drawRivets(bp, center, radius)
            TargetStyle.WEDGES -> drawWedges(bp, center, radius)
            TargetStyle.STONE -> drawCracks(bp, center, radius)
            TargetStyle.SECTORS -> drawSectors(bp, center, radius)
        }
        drawCircle(
            color = bp.accentColorArgb.toComposeColor().copy(alpha = 0.85f),
            radius = radius * 0.12f,
            center = center,
        )
        if (boss) {
            val rim = radius * (1.04f + 0.03f * pulse)
            drawCircle(
                color = bp.accentColorArgb.toComposeColor().copy(alpha = 0.55f + 0.35f * pulse),
                radius = rim,
                center = center,
                style = Stroke(width = radius * 0.045f),
            )
        }
    }
}

private fun DrawScope.drawGrain(bp: TargetBlueprint, c: Offset, r: Float) {
    val accent = bp.accentColorArgb.toComposeColor().copy(alpha = 0.35f)
    for (i in 1..bp.ringCount) {
        drawCircle(accent, r * (i / (bp.ringCount + 1f)), c, style = Stroke(r * 0.03f))
    }
    drawCircle(Color.Black.copy(alpha = 0.25f), r, c, style = Stroke(r * 0.06f))
}

private fun DrawScope.drawRivets(bp: TargetBlueprint, c: Offset, r: Float) {
    val accent = bp.accentColorArgb.toComposeColor()
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.92f, c, style = Stroke(r * 0.08f))
    val n = bp.notchCount.coerceAtLeast(6)
    for (i in 0 until n) {
        val a = i * TAU / n
        val p = Offset(c.x + cos(a) * r * 0.82f, c.y + sin(a) * r * 0.82f)
        drawCircle(accent, r * 0.045f, p)
        drawCircle(Color.Black.copy(alpha = 0.4f), r * 0.018f, p)
    }
}

private fun DrawScope.drawWedges(bp: TargetBlueprint, c: Offset, r: Float) {
    val n = bp.ringCount.coerceAtLeast(6)
    val a = bp.accentColorArgb.toComposeColor()
    val b = bp.baseColorArgb.toComposeColor()
    for (i in 0 until n) {
        val start = Math.toDegrees((i * TAU / n).toDouble()).toFloat()
        drawArc(
            color = if (i % 2 == 0) a.copy(alpha = 0.55f) else b.copy(alpha = 0.2f),
            startAngle = start,
            sweepAngle = 360f / n,
            useCenter = true,
            topLeft = Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        )
    }
    drawCircle(Color.Black.copy(alpha = 0.2f), r, c, style = Stroke(r * 0.04f))
}

private fun DrawScope.drawCracks(bp: TargetBlueprint, c: Offset, r: Float) {
    val accent = bp.accentColorArgb.toComposeColor().copy(alpha = 0.55f)
    for (i in 0 until bp.ringCount + 3) {
        val a = i * 0.9f
        drawLine(
            accent,
            Offset(c.x + cos(a) * r * 0.2f, c.y + sin(a) * r * 0.2f),
            Offset(c.x + cos(a + 0.2f) * r * 0.92f, c.y + sin(a + 0.15f) * r * 0.92f),
            strokeWidth = r * 0.018f,
        )
    }
}

private fun DrawScope.drawSectors(bp: TargetBlueprint, c: Offset, r: Float) {
    val n = bp.notchCount.coerceAtLeast(8)
    val a = bp.accentColorArgb.toComposeColor()
    for (i in 0 until n) {
        if (i % 2 != 0) continue
        val start = Math.toDegrees((i * TAU / n).toDouble()).toFloat()
        drawArc(
            color = a.copy(alpha = 0.4f),
            startAngle = start,
            sweepAngle = 360f / n,
            useCenter = true,
            topLeft = Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        )
    }
    for (i in 0 until n) {
        val ang = i * TAU / n
        drawLine(
            Color.Black.copy(alpha = 0.25f),
            c,
            Offset(c.x + cos(ang) * r, c.y + sin(ang) * r),
            strokeWidth = r * 0.012f,
        )
    }
}

fun DrawScope.drawBossExtras(
    worldId: Int,
    center: Offset,
    radius: Float,
    rotationRad: Float,
    t: Float,
) {
    val pulse = (0.5f + 0.5f * kotlin.math.sin(t * 4f))
    when (worldId) {
        1 -> {
            drawCircle(Color(0xFF2A5A20).copy(alpha = 0.35f), radius * 1.08f, center, style = Stroke(radius * 0.06f))
        }
        2 -> {
            for (i in 0 until 8) {
                val a = i * TAU / 8f + rotationRad
                drawCircle(Color(0xFFE8C36A), radius * 0.06f, Offset(center.x + cos(a) * radius * 0.7f, center.y + sin(a) * radius * 0.7f))
            }
        }
        3 -> {
            drawCircle(Color(0xFFFF5577).copy(alpha = 0.5f), radius * (0.28f + 0.04f * pulse), center)
            drawCircle(Color(0xFF080810), radius * 0.12f, center)
        }
        4 -> {
            drawWedges(
                TargetBlueprint("x", "", 70f, 0xFFD45A2A, 0xFFFFE08A, TargetStyle.WEDGES, 10),
                center,
                radius * 0.9f,
            )
        }
        5 -> {
            drawCircle(Color(0xFFE8FFFF).copy(alpha = 0.25f), radius * 1.12f, center, style = Stroke(radius * 0.08f))
        }
        6 -> {
            drawCircle(Color(0xFFFF6A3D).copy(alpha = 0.35f + 0.2f * pulse), radius * 0.4f, center)
        }
        7 -> {
            val n = 8
            for (i in 0 until n) {
                val a = i * TAU / n + t * 0.4f
                drawCircle(Color(0xFFE03A3A), radius * 0.07f, Offset(center.x + cos(a) * radius * 0.45f, center.y + sin(a) * radius * 0.45f))
            }
        }
        8 -> {
            val hand = rotationRad * 12f
            drawLine(
                Color(0xFF2A2A28),
                center,
                Offset(center.x + cos(hand) * radius * 0.72f, center.y + sin(hand) * radius * 0.72f),
                strokeWidth = radius * 0.04f,
            )
            drawLine(
                Color(0xFF8B1E2D),
                center,
                Offset(center.x + cos(-hand * 0.2f) * radius * 0.5f, center.y + sin(-hand * 0.2f) * radius * 0.5f),
                strokeWidth = radius * 0.03f,
            )
        }
        9 -> {
            drawCircle(Color(0xFF00F5FF).copy(alpha = 0.4f * pulse), radius * 1.06f, center, style = Stroke(radius * 0.03f))
            drawCircle(Color(0xFFFF2BD6).copy(alpha = 0.25f), radius * 0.5f, center, style = Stroke(radius * 0.02f))
        }
        else -> {
            drawCircle(Color(0xFFB388FF).copy(alpha = 0.2f + 0.3f * pulse), radius * (0.35f * (1f - 0.15f * pulse)), center)
            drawCircle(Color(0xFFFFE08A).copy(alpha = 0.6f), radius * 0.08f, center)
        }
    }
}
