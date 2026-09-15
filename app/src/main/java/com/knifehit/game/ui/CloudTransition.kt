package com.knifehit.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.knifehit.game.render.toComposeColor
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CloudTransition(
    tint: Long,
    play: Boolean,
    reduceMotion: Boolean,
    onCovered: () -> Unit,
    onFinished: () -> Unit,
) {
    if (!play) return
    val cover = remember { Animatable(0f) }
    val launched = remember { AtomicBoolean(false) }
    LaunchedEffect(play, reduceMotion) {
        try {
            if (reduceMotion) {
                onCovered()
                onFinished()
                return@LaunchedEffect
            }
            cover.snapTo(0f)
            cover.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            onCovered()
            delay(700)
            cover.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            if (launched.compareAndSet(false, true)) onFinished()
        } finally {
            if (!launched.get()) {
                launched.set(true)
                onFinished()
            }
        }
    }
    val color = tint.toComposeColor().copy(alpha = 0.92f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val shift = with(density) { (w * (1f - cover.value)).toDp() }
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .offset(x = -shift)
                .background(color),
        )
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .align(androidx.compose.ui.Alignment.CenterEnd)
                .offset(x = shift)
                .background(color),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = cover.value * 0.35f }
                .background(Color.White.copy(alpha = 0.15f)),
        )
    }
}
