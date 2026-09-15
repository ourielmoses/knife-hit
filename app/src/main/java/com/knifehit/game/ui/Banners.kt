package com.knifehit.game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.engine.GameEngine
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.model.PlayState
import com.knifehit.game.model.PrecisionBand

@Composable
fun StageClearBanner(visible: Boolean, world: Boolean) {
    val s = LocalStrings.current
    val scale by animateFloatAsState(if (visible) 1f else 0.4f, spring(), label = "clear")
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "clearA")
    if (alpha < 0.02f) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (world) s.worldComplete else s.stageClear,
            color = NeonLime,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        )
    }
}

@Composable
fun BossIntroBanner(visible: Boolean, name: String) {
    val s = LocalStrings.current
    if (!visible) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NeonTitle(s.boss)
        Text(name, color = Color.White, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center).offset(y = 48.dp))
    }
}

@Composable
fun FloatingPrecision(engine: GameEngine) {
    val s = LocalStrings.current
    val ev = engine.lastScoreEvent ?: return
    val age = engine.elapsed - ev.born
    if (age !in 0f..0.9f) return
    val a = (1f - age / 0.9f).coerceIn(0f, 1f)
    val label = when (ev.band) {
        PrecisionBand.PERFECT -> s.perfect
        PrecisionBand.GOOD -> s.goodTry
        PrecisionBand.FIRST -> s.goodTry
    }
    Text(
        "$label +${ev.points}",
        color = if (ev.band == PrecisionBand.PERFECT) NeonLime else Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.graphicsLayer {
            translationX = ev.x - 40f
            translationY = ev.y - 30f - age * 80f
            alpha = a
        },
    )
}

@Composable
fun ReviveCountdown(engine: GameEngine) {
    if (engine.playState != PlayState.ReviveCountdown) return
    val n = engine.reviveT.toInt() + 1
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        NeonTitle("$n", color = NeonCyan)
    }
}
