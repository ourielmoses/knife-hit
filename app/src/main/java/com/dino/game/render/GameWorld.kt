package com.dino.game.render

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.dino.game.R
import com.dino.game.model.SkinId
import com.dino.game.model.Constants
import com.dino.game.model.GameSnapshot
import com.dino.game.model.ObstacleKind
import com.dino.game.model.ObstacleState
import com.dino.game.model.PlayerState
import com.dino.game.model.ScreenState
import kotlin.math.sin

private val DayInk = Color(0xFF535353)
private val DaySky = Color(0xFFF7F7F7)
private val NightInk = Color(0xFFD7D7D7)
private val NightSky = Color(0xFF1B1B1B)

private data class WorldSprites(
    val player: SkinSprites,
    val cloud: ImageBitmap,
    val cactusSmall: ImageBitmap,
    val cactusMedium: ImageBitmap,
    val cactusLarge: ImageBitmap,
)

@Composable
fun GameWorld(
    snapshot: GameSnapshot,
    textMeasurer: TextMeasurer,
    skin: SkinId,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sprites = remember(context.resources, skin) {
        val res = context.resources
        WorldSprites(
            player = SkinSpriteLoader.load(res, skin),
            cloud = ImageBitmap.imageResource(res, R.drawable.pixel_cloud),
            cactusSmall = ImageBitmap.imageResource(res, R.drawable.pixel_cactus_small),
            cactusMedium = ImageBitmap.imageResource(res, R.drawable.pixel_cactus_medium),
            cactusLarge = ImageBitmap.imageResource(res, R.drawable.pixel_cactus_large),
        )
    }

    Canvas(modifier = modifier) {
        val scale = size.height / Constants.WORLD_HEIGHT
        val t = snapshot.nightBlend.coerceIn(0f, 1f)
        val ink = lerpColor(DayInk, NightInk, t)
        val sky = lerpColor(DaySky, NightSky, t)
        val spriteFilter = nightBlendFilter(t)

        drawRect(sky)

        drawDunes(snapshot.duneOffset * scale, scale, ink.copy(alpha = 0.22f))

        val groundY = Constants.GROUND_Y * scale
        drawLine(
            color = ink,
            start = Offset(0f, groundY),
            end = Offset(size.width, groundY),
            strokeWidth = 2f * scale,
        )
        val offset = snapshot.groundOffset * scale
        var gx = -offset
        while (gx < size.width + 24f * scale) {
            drawLine(
                color = ink,
                start = Offset(gx, groundY + 6f * scale),
                end = Offset(gx + 8f * scale, groundY + 6f * scale),
                strokeWidth = 2f * scale,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = ink,
                start = Offset(gx + 14f * scale, groundY + 11f * scale),
                end = Offset(gx + 18f * scale, groundY + 11f * scale),
                strokeWidth = 2f * scale,
            )
            gx += 24f * scale
        }

        snapshot.clouds.forEach { cloud ->
            drawCloudSprite(
                cloud.x * scale,
                cloud.y * scale,
                cloud.scale * scale,
                sprites.cloud,
                spriteFilter,
            )
        }

        snapshot.obstacles.forEach { o ->
            drawObstacle(o, scale, ink, sprites, spriteFilter)
        }

        drawPlayer(snapshot.player, scale, sprites, spriteFilter)

        snapshot.particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = ink.copy(alpha = alpha * 0.7f),
                radius = p.size * scale,
                center = Offset(p.x * scale, p.y * scale),
            )
        }

        if (
            snapshot.screen == ScreenState.Playing ||
            snapshot.screen == ScreenState.Paused ||
            snapshot.screen == ScreenState.GameOver
        ) {
            drawHud(snapshot, textMeasurer, scale, ink)
        }

        when (snapshot.screen) {
            ScreenState.Title -> {
                // Menu chrome is drawn in Compose (Play / Skins). Soft title only.
                drawCenteredLabel(
                    textMeasurer,
                    "DINO",
                    size.width / 2f,
                    size.height * 0.28f,
                    36,
                    ink,
                )
                if (snapshot.highScore > 0) {
                    drawCenteredLabel(
                        textMeasurer,
                        "HI  ${snapshot.highScore.toString().padStart(5, '0')}",
                        size.width / 2f,
                        size.height * 0.38f,
                        16,
                        ink,
                    )
                }
            }
            ScreenState.GameOver -> {
                // Hold on lying dead pose; hide GAME OVER until landed + pose timer.
                if (snapshot.deathPoseRemaining > 0f || !snapshot.player.onGround) {
                    Unit
                } else {
                    drawCenteredLabel(
                        textMeasurer,
                        "GAME OVER",
                        size.width / 2f,
                        size.height * 0.34f,
                        28,
                        ink,
                    )
                    val scoreLine = "SCORE  ${snapshot.score.toString().padStart(5, '0')}"
                    val hiLine = "HI  ${snapshot.highScore.toString().padStart(5, '0')}"
                    drawCenteredLabel(textMeasurer, scoreLine, size.width / 2f, size.height * 0.46f, ink = ink)
                    drawCenteredLabel(textMeasurer, hiLine, size.width / 2f, size.height * 0.54f, ink = ink)
                    if (snapshot.isNewRecord) {
                        drawCenteredLabel(
                            textMeasurer,
                            "NEW RECORD!",
                            size.width / 2f,
                            size.height * 0.62f,
                            16,
                            ink,
                        )
                    }
                    if (snapshot.gameOverLockRemaining <= 0f) {
                        drawCenteredLabel(
                            textMeasurer,
                            "TAP JUMP TO RETRY",
                            size.width / 2f,
                            size.height * 0.72f,
                            14,
                            ink,
                        )
                    }
                }
            }
            ScreenState.Playing, ScreenState.Paused -> Unit
        }

        if (snapshot.screen == ScreenState.Playing) {
            drawSideHints(textMeasurer, ink)
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u,
    )
}

/** Partial invert so sprites fade smoothly with the night blend. */
private fun nightBlendFilter(t: Float): ColorFilter? {
    if (t <= 0.001f) return null
    val u = t.coerceIn(0f, 1f)
    val m = 1f - 2f * u
    val o = 255f * u
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                m, 0f, 0f, 0f, o,
                0f, m, 0f, 0f, o,
                0f, 0f, m, 0f, o,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

private fun DrawScope.drawDunes(offset: Float, scale: Float, color: Color) {
    val groundY = Constants.GROUND_Y * scale
    val period = Constants.DUNE_PERIOD * scale
    var x = -offset % period - period
    val path = Path()
    path.moveTo(x, groundY)
    while (x < size.width + period) {
        val mid = x + period * 0.5f
        val peak = groundY - (18f + 10f * sin(x * 0.01f).toFloat()) * scale
        path.quadraticTo(mid, peak, x + period, groundY)
        x += period
    }
    path.lineTo(size.width, groundY)
    path.lineTo(size.width, groundY + 1f)
    path.lineTo(0f, groundY + 1f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawHud(
    snapshot: GameSnapshot,
    textMeasurer: TextMeasurer,
    scale: Float,
    ink: Color,
) {
    val style = TextStyle(
        color = ink,
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
    ink: Color,
) {
    val style = TextStyle(
        color = ink,
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

private fun DrawScope.drawSideHints(textMeasurer: TextMeasurer, ink: Color) {
    val style = TextStyle(
        color = ink.copy(alpha = 0.28f),
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

private fun DrawScope.drawCloudSprite(
    x: Float,
    y: Float,
    scaleFactor: Float,
    bitmap: ImageBitmap,
    colorFilter: ColorFilter?,
) {
    // Larger on-screen clouds (was ~36px logical).
    val base = 72f * scaleFactor.coerceIn(0.7f, 1.6f)
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val dh = base * 0.55f
    val dw = dh * aspect
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(x.toInt(), y.toInt()),
        dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
        colorFilter = colorFilter,
        filterQuality = FilterQuality.None,
    )
}

private fun DrawScope.drawPlayer(
    player: PlayerState,
    scale: Float,
    sprites: WorldSprites,
    colorFilter: ColorFilter?,
) {
    val x = player.x * scale
    val y = player.y * scale
    val w = player.width * scale
    val h = player.height * scale

    val bitmap = when {
        // Airborne death: keep falling with the jump frame until landing.
        player.dead && !player.onGround -> sprites.player.jump
        player.dead -> sprites.player.dead
        player.ducking -> sprites.player.duck
        !player.onGround -> sprites.player.jump
        player.runFrame == 1 -> sprites.player.run2
        else -> sprites.player.stand
    }

    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()

    val fit = minOf(w / srcW, h / srcH)
    val dw = when {
        player.ducking -> w * 0.98f
        player.dead && player.onGround -> w * 0.98f
        else -> srcW * fit
    }
    val dh = when {
        player.ducking -> h * 0.98f
        player.dead && player.onGround -> h * 0.98f
        else -> srcH * fit
    }

    val dx = x + (w - dw) / 2f
    val groundY = y + h
    val dy = groundY - dh

    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(dx.toInt(), dy.toInt()),
        dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
        colorFilter = colorFilter,
        filterQuality = FilterQuality.None,
    )
}

private fun DrawScope.drawObstacle(
    o: ObstacleState,
    scale: Float,
    ink: Color,
    sprites: WorldSprites,
    colorFilter: ColorFilter?,
) {
    val x = o.x * scale
    val y = o.y * scale
    val w = o.width * scale
    val h = o.height * scale
    when (o.kind) {
        ObstacleKind.BirdHigh, ObstacleKind.BirdMid, ObstacleKind.BirdLow ->
            drawBird(x, y, w, h, o.frame, ink)
        ObstacleKind.CactusCluster2 -> {
            drawCactusSprite(x, y + h * 0.1f, w * 0.48f, h * 0.9f, sprites.cactusSmall, colorFilter)
            drawCactusSprite(x + w * 0.5f, y, w * 0.48f, h, sprites.cactusMedium, colorFilter)
        }
        ObstacleKind.CactusCluster3 -> {
            drawCactusSprite(x, y + h * 0.15f, w * 0.3f, h * 0.85f, sprites.cactusSmall, colorFilter)
            drawCactusSprite(x + w * 0.32f, y, w * 0.34f, h, sprites.cactusMedium, colorFilter)
            drawCactusSprite(x + w * 0.66f, y + h * 0.08f, w * 0.32f, h * 0.92f, sprites.cactusLarge, colorFilter)
        }
        ObstacleKind.CactusSmall ->
            drawCactusSprite(x, y, w, h, sprites.cactusSmall, colorFilter)
        ObstacleKind.CactusMedium ->
            drawCactusSprite(x, y, w, h, sprites.cactusMedium, colorFilter)
        ObstacleKind.CactusLarge ->
            drawCactusSprite(x, y, w, h, sprites.cactusLarge, colorFilter)
    }
}

private fun DrawScope.drawCactusSprite(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    bitmap: ImageBitmap,
    colorFilter: ColorFilter?,
) {
    // Fill the obstacle hitbox fully so cactuses read large and sharp.
    val dw = w
    val dh = h
    val dx = x
    val dy = y + (h - dh)
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(dx.toInt(), dy.toInt()),
        dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
        colorFilter = colorFilter,
        filterQuality = FilterQuality.None,
    )
}

private fun DrawScope.drawBird(x: Float, y: Float, w: Float, h: Float, frame: Int, ink: Color) {
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
    drawPath(path, ink)
    val wingY = if (frame == 0) y + h * 0.15f else y + h * 0.55f
    drawLine(ink, Offset(x + w * 0.4f, y + h * 0.5f), Offset(x + w * 0.15f, wingY), 3f, StrokeCap.Round)
    drawLine(
        ink,
        Offset(x + w * 0.45f, y + h * 0.5f),
        Offset(x + w * 0.55f, wingY + h * 0.05f),
        3f,
        StrokeCap.Round,
    )
}
