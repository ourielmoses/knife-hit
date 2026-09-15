package com.knifehit.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.knifehit.game.model.KnifeSkin

fun Int.toComposeColor(): Color = (this.toLong() and 0xFFFFFFFFL).toComposeColor()

fun Long.toComposeColor(): Color {
    val c = this.toInt()
    return Color(
        red = ((c ushr 16) and 0xFF) / 255f,
        green = ((c ushr 8) and 0xFF) / 255f,
        blue = (c and 0xFF) / 255f,
        alpha = ((c ushr 24) and 0xFF) / 255f,
    )
}

fun DrawScope.drawKnife(
    skin: KnifeSkin,
    length: Float,
    width: Float,
    tipUp: Boolean = true,
) {
    val blade = skin.colorArgb.toComposeColor()
    val hilt = Color(
        red = blade.red * 0.45f,
        green = blade.green * 0.45f,
        blue = blade.blue * 0.55f,
        alpha = 1f,
    )
    val dir = if (tipUp) -1f else 1f
    val bladeLen = length * 0.72f
    val hiltLen = length * 0.28f
    drawLine(
        color = blade,
        start = Offset(0f, 0f),
        end = Offset(0f, dir * bladeLen),
        strokeWidth = width * 0.55f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(-width * 0.12f, dir * length * 0.08f),
        end = Offset(-width * 0.12f, dir * bladeLen * 0.85f),
        strokeWidth = width * 0.12f,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = hilt,
        topLeft = Offset(-width * 0.55f, dir * bladeLen - if (tipUp) hiltLen else 0f),
        size = Size(width * 1.1f, hiltLen),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.12f, width * 0.12f),
    )
    drawCircle(hilt, width * 0.22f, Offset(0f, dir * (bladeLen + hiltLen * 0.15f)))
}

fun DrawScope.drawStuckKnife(
    skin: KnifeSkin,
    center: Offset,
    radius: Float,
    relAngle: Float,
    rotation: Float,
    length: Float,
    width: Float,
) {
    val deg = Math.toDegrees((relAngle + rotation).toDouble()).toFloat()
    rotate(deg, pivot = center) {
        translate(center.x + radius, center.y) {
            rotate(90f, pivot = Offset.Zero) {
                drawKnife(skin, length * 0.92f, width * 0.85f, tipUp = true)
            }
        }
    }
}
