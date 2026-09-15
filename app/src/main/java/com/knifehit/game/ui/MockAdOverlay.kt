package com.knifehit.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.ads.AdKind
import com.knifehit.game.ads.AdResult
import com.knifehit.game.i18n.LocalStrings
import kotlinx.coroutines.delay

@Composable
fun MockAdOverlay(
    kind: AdKind,
    onResult: (AdResult) -> Unit,
) {
    val s = LocalStrings.current
    val wait = if (kind == AdKind.INTERSTITIAL) 3 else 5
    var remaining by remember { mutableIntStateOf(wait) }
    var closable by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(kind) {
        val start = System.currentTimeMillis()
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        closable = true
        delay(12_000)
        if (!finished) {
            finished = true
            closable = true
        }
        @Suppress("UNUSED_VARIABLE")
        val elapsed = System.currentTimeMillis() - start
    }
    BackHandler(enabled = remaining > 0 && !closable) { }
    Box(Modifier.fillMaxSize().background(Color(0xF2181C24)).padding(24.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(36.dp))
            NeonTitle(s.mockAdTitle, color = NeonMagenta)
            Spacer(Modifier.height(12.dp))
            Text(s.mockSponsor, color = Color(0xFFCFD8E0))
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF2A3140)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { 1f - remaining / wait.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("${remaining}s", color = Color.White)
            Spacer(Modifier.weight(1f))
            if (closable) {
                NeonButton(s.skipAd, {
                    if (finished) return@NeonButton
                    finished = true
                    onResult(if (remaining <= 0) AdResult.Completed else AdResult.Dismissed)
                })
            }
            NeonButton(
                if (remaining <= 0) "X" else "${remaining}",
                {
                    if (!closable) return@NeonButton
                    finished = true
                    onResult(AdResult.Completed)
                },
                enabled = closable,
                accent = NeonCyan,
            )
        }
    }
}
