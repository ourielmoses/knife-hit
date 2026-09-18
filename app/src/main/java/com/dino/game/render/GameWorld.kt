package com.dino.game.render

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dino.game.model.Constants
import com.dino.game.model.GameSnapshot
import com.dino.game.model.ObstacleKind
import com.dino.game.model.ObstacleState
import com.dino.game.model.PlayerState
import com.dino.game.model.ScreenState

private val Ink = Color(0xFF535353)
private val Sky = Color(0xFFF7F7F7)

@Composable
fun GameWorld(
    snapshot: GameSnapshot,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val scale = size.height / Constants.WORLD_HEIGHT

        drawRect(Sky)

        // Ground line
        val groundY = Constants.GROUND_Y * scale
        drawLine(
            color = Ink,
            start = Offset(0f, groundY),
            end = Offset(size.width, groundY),
            strokeWidth = 2f * scale,
        )
        // Speckles
        val offset = snapshot.groundOffset * scale
        var x = -offset
        while (x < size.width + 24f * scale) {
            drawLine(
                color = Ink,
                start = Offset(x, groundY + 6f * scale),
                end = Offset(x + 8f * scale, groundY + 6f * scale),
                strokeWidth = 2f * scale,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = Ink,
                start = Offset(x + 14f * scale, groundY + 11f * scale),
                end = Offset(x + 18f * scale, groundY + 11f * scale),
                strokeWidth = 2f * scale,
            )
            x += 24f * scale
        }

        snapshot.clouds.forEach { cloud ->
            drawCloud(cloud.x * scale, cloud.y * scale, cloud.scale * scale)
        }

        snapshot.obstacles.forEach { o ->
            drawObstacle(o, scale)
        }

        drawPlayer(snapshot.player, scale)

        drawHud(snapshot, textMeasurer, scale)

        when (snapshot.screen) {
            ScreenState.Title -> drawCenteredLabel(
                textMeasurer,
                "TAP JUMP TO START",
                size.width / 2f,
                size.height * 0.42f,
            )
            ScreenState.GameOver -> {
                drawCenteredLabel(
                    textMeasurer,
                    "GAME OVER",
                    size.width / 2f,
                    size.height * 0.34f,
                    28,
                )
                val scoreLine = "SCORE  ${snapshot.score.toString().padStart(5, '0')}"
                val hiLine = "HI  ${snapshot.highScore.toString().padStart(5, '0')}"
                drawCenteredLabel(textMeasurer, scoreLine, size.width / 2f, size.height * 0.46f)
                drawCenteredLabel(textMeasurer, hiLine, size.width / 2f, size.height * 0.54f)
                if (snapshot.isNewRecord) {
                    drawCenteredLabel(
                        textMeasurer,
                        "NEW RECORD!",
                        size.width / 2f,
                        size.height * 0.62f,
                        16,
                    )
                }
                if (snapshot.gameOverLockRemaining <= 0f) {
                    drawCenteredLabel(
                        textMeasurer,
                        "TAP JUMP TO RETRY",
                        size.width / 2f,
                        size.height * 0.72f,
                        14,
                    )
                }
            }
            ScreenState.Playing -> Unit
        }

        // Soft side hints (non-interactive labels)
        drawSideHints(textMeasurer)
    }
}

private fun DrawScope.drawHud(
    snapshot: GameSnapshot,
    textMeasurer: TextMeasurer,
    scale: Float,
) {
    val style = TextStyle(
        color = Ink,
        fontSize = 16.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    val hi = "HI ${snapshot.highScore.toString().padStart(5, '0')}"
    val sc = snapshot.score.toString().padStart(5, '0')
    val hiLayout = textMeasurer.measure(hi, style)
    val scLayout = textMeasurer.measure(sc, style)
    val top = 16f * scale.coerceAtLeast(0.8f)
    val right = size.width - 20f
    drawText(scLayout, topLeft = Offset(right - scLayout.size.width, top))
    drawText(
        hiLayout,
        topLeft = Offset(right - scLayout.size.width - hiLayout.size.width - 24f, top),
    )
}

private fun DrawScope.drawCenteredLabel(
    textMeasurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    sizeSp: Int = 18,
) {
    val style = TextStyle(
        color = Ink,
        fontSize = sizeSp.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    val layout = textMeasurer.measure(text, style)
    drawText(
        layout,
        topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
    )
}

private fun DrawScope.drawSideHints(textMeasurer: TextMeasurer) {
    val style = TextStyle(
        color = Ink.copy(alpha = 0.28f),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    val duck = textMeasurer.measure("DUCK", style)
    val jump = textMeasurer.measure("JUMP", style)
    drawText(duck, topLeft = Offset(16f, size.height - duck.size.height - 12f))
    drawText(
        jump,
        topLeft = Offset(size.width - jump.size.width - 16f, size.height - jump.size.height - 12f),
    )
}

private fun DrawScope.drawCloud(x: Float, y: Float, scale: Float) {
    val s = 18f * scale
    drawRoundRect(
        color = Ink,
        topLeft = Offset(x, y),
        size = Size(s * 2.2f, s * 0.7f),
        cornerRadius = CornerRadius(s * 0.4f, s * 0.4f),
        style = Stroke(width = 2f),
    )
    drawCircle(Ink, radius = s * 0.45f, center = Offset(x + s * 0.55f, y), style = Stroke(2f))
    drawCircle(Ink, radius = s * 0.55f, center = Offset(x + s * 1.3f, y - s * 0.15f), style = Stroke(2f))
}

private fun DrawScope.drawPlayer(player: PlayerState, scale: Float) {
    val x = player.x * scale
    val y = player.y * scale
    val w = player.width * scale
    val h = player.height * scale

    if (player.dead) {
        // Dead: body + X eyes
        drawRoundRect(Ink, Offset(x, y), Size(w, h), CornerRadius(4f, 4f), style = Stroke(2.5f))
        drawLine(Ink, Offset(x + w * 0.55f, y + h * 0.2f), Offset(x + w * 0.75f, y + h * 0.35f), 2.5f)
        drawLine(Ink, Offset(x + w * 0.75f, y + h * 0.2f), Offset(x + w * 0.55f, y + h * 0.35f), 2.5f)
        return
    }

    if (player.ducking) {
        drawDinoDuck(x, y, w, h)
    } else {
        drawDinoStand(x, y, w, h, player.runFrame, !player.onGround)
    }
}

private fun DrawScope.drawDinoStand(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    frame: Int,
    jumping: Boolean,
) {
    val path = Path().apply {
        // Simplified Chrome-like silhouette
        moveTo(x + w * 0.15f, y + h)
        lineTo(x + w * 0.15f, y + h * 0.45f)
        lineTo(x + w * 0.05f, y + h * 0.55f)
        lineTo(x, y + h * 0.45f)
        lineTo(x + w * 0.2f, y + h * 0.35f)
        lineTo(x + w * 0.35f, y + h * 0.15f)
        lineTo(x + w * 0.55f, y)
        lineTo(x + w * 0.95f, y)
        lineTo(x + w, y + h * 0.12f)
        lineTo(x + w * 0.78f, y + h * 0.12f)
        lineTo(x + w * 0.78f, y + h * 0.28f)
        lineTo(x + w * 0.55f, y + h * 0.28f)
        lineTo(x + w * 0.5f, y + h * 0.55f)
        // Legs
        if (jumping) {
            lineTo(x + w * 0.45f, y + h)
            lineTo(x + w * 0.35f, y + h)
            lineTo(x + w * 0.4f, y + h * 0.7f)
            lineTo(x + w * 0.3f, y + h)
            lineTo(x + w * 0.2f, y + h)
        } else if (frame == 0) {
            lineTo(x + w * 0.42f, y + h)
            lineTo(x + w * 0.32f, y + h)
            lineTo(x + w * 0.38f, y + h * 0.7f)
            lineTo(x + w * 0.28f, y + h * 0.85f)
            lineTo(x + w * 0.18f, y + h * 0.85f)
        } else {
            lineTo(x + w * 0.48f, y + h * 0.85f)
            lineTo(x + w * 0.38f, y + h * 0.85f)
            lineTo(x + w * 0.4f, y + h * 0.7f)
            lineTo(x + w * 0.3f, y + h)
            lineTo(x + w * 0.2f, y + h)
        }
        close()
    }
    drawPath(path, Ink)
    // Eye
    drawRect(Sky, Offset(x + w * 0.72f, y + h * 0.06f), Size(w * 0.08f, h * 0.08f))
}

private fun DrawScope.drawDinoDuck(x: Float, y: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(x, y + h)
        lineTo(x, y + h * 0.4f)
        lineTo(x + w * 0.55f, y)
        lineTo(x + w * 0.95f, y + h * 0.15f)
        lineTo(x + w, y + h * 0.35f)
        lineTo(x + w * 0.7f, y + h * 0.35f)
        lineTo(x + w * 0.65f, y + h)
        lineTo(x + w * 0.45f, y + h)
        lineTo(x + w * 0.4f, y + h * 0.55f)
        lineTo(x + w * 0.25f, y + h)
        close()
    }
    drawPath(path, Ink)
    drawRect(Sky, Offset(x + w * 0.78f, y + h * 0.18f), Size(w * 0.06f, h * 0.14f))
}

private fun DrawScope.drawObstacle(o: ObstacleState, scale: Float) {
    val x = o.x * scale
    val y = o.y * scale
    val w = o.width * scale
    val h = o.height * scale
    when (o.kind) {
        ObstacleKind.BirdHigh, ObstacleKind.BirdMid, ObstacleKind.BirdLow -> drawBird(x, y, w, h, o.frame)
        ObstacleKind.CactusCluster2 -> {
            drawCactus(x, y + h * 0.15f, w * 0.42f, h * 0.85f)
            drawCactus(x + w * 0.52f, y, w * 0.42f, h)
        }
        ObstacleKind.CactusCluster3 -> {
            drawCactus(x, y + h * 0.2f, w * 0.28f, h * 0.8f)
            drawCactus(x + w * 0.34f, y, w * 0.3f, h)
            drawCactus(x + w * 0.68f, y + h * 0.12f, w * 0.28f, h * 0.88f)
        }
        else -> drawCactus(x, y, w, h)
    }
}

private fun DrawScope.drawCactus(x: Float, y: Float, w: Float, h: Float) {
    val trunk = w * 0.34f
    val cx = x + w / 2f - trunk / 2f
    drawRect(Ink, Offset(cx, y), Size(trunk, h))
    // Arms
    drawRect(Ink, Offset(x, y + h * 0.35f), Size(w * 0.4f, trunk * 0.7f))
    drawRect(Ink, Offset(x, y + h * 0.2f), Size(trunk * 0.7f, h * 0.25f))
    drawRect(Ink, Offset(x + w * 0.55f, y + h * 0.45f), Size(w * 0.45f, trunk * 0.7f))
    drawRect(Ink, Offset(x + w - trunk * 0.7f, y + h * 0.28f), Size(trunk * 0.7f, h * 0.28f))
}

private fun DrawScope.drawBird(x: Float, y: Float, w: Float, h: Float, frame: Int) {
    val path = Path().apply {
        moveTo(x + w * 0.2f, y + h * 0.55f)
        lineTo(x + w * 0.55f, y + h * 0.45f)
        lineTo(x + w * 0.85f, y + h * 0.5f)
        lineTo(x + w, y + h * 0.4f)
        lineTo(x + w * 0.75f, y + h * 0.6f)
        lineTo(x + w * 0.5f, y + h * 0.65f)
        lineTo(x + w * 0.25f, y + h * 0.7f)
        close()
    }
    drawPath(path, Ink)
    // Wings
    val wingY = if (frame == 0) y + h * 0.15f else y + h * 0.55f
    drawLine(
        Ink,
        Offset(x + w * 0.4f, y + h * 0.5f),
        Offset(x + w * 0.15f, wingY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
    drawLine(
        Ink,
        Offset(x + w * 0.45f, y + h * 0.5f),
        Offset(x + w * 0.55f, wingY + h * 0.05f),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
}
