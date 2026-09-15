package com.knifehit.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.i18n.GameFontFamily
import com.knifehit.game.i18n.LocalStrings

@Composable
fun GameOverOverlay(
    score: Int,
    best: Int,
    isNewBest: Boolean,
    cheated: Boolean,
    worldName: String,
    stage: Int,
    onTryAgain: () -> Unit,
    onRevive: () -> Unit,
    onGiveUp: () -> Unit,
    showRevive: Boolean,
) {
    val s = LocalStrings.current
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            NeonTitle(s.gameOver, color = NeonMagenta)
            Spacer(Modifier.height(16.dp))
            Text(
                "${s.score} $score",
                color = Color.White,
                fontFamily = GameFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${s.highScore} $best",
                color = Color(0xFF9AA8B8),
                fontFamily = GameFontFamily,
                fontSize = 16.sp,
            )
            if (isNewBest && !cheated) {
                Spacer(Modifier.height(8.dp))
                Text(s.newHighScore, color = NeonLime, fontFamily = GameFontFamily, fontWeight = FontWeight.Black)
            }
            if (cheated) {
                Text("Revive run — high score skipped", color = Color(0xFFFFC04A), fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
            if (showRevive) {
                Text(
                    s.reviveStakes,
                    color = Color(0xFFD0D8E0),
                    textAlign = TextAlign.Center,
                    fontFamily = GameFontFamily,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    s.watchRevive.replace("%1\$d", "$stage") + " · $worldName",
                    color = Color(0xFF9AA8B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                NeonButton(s.revive, onRevive, Modifier.fillMaxWidth(0.85f), accent = NeonLime)
                Spacer(Modifier.height(10.dp))
                NeonButton(s.giveUp, onGiveUp, Modifier.fillMaxWidth(0.85f), accent = NeonMagenta)
            } else {
                NeonButton(s.tryAgain, onTryAgain, Modifier.fillMaxWidth(0.75f))
            }
        }
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onHome: () -> Unit) {
    val s = LocalStrings.current
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(120.dp))
            NeonTitle(s.pause)
            Spacer(Modifier.height(24.dp))
            NeonButton(s.resume, onResume, Modifier.fillMaxWidth(0.7f))
            Spacer(Modifier.height(12.dp))
            NeonButton(s.quitToHome, onHome, Modifier.fillMaxWidth(0.7f), accent = NeonMagenta)
        }
    }
}
