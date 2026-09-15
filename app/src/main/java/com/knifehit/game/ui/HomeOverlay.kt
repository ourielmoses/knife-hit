package com.knifehit.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.BuildConfig
import com.knifehit.game.i18n.GameFontFamily
import com.knifehit.game.i18n.LocalStrings

@Composable
fun HomeOverlay(
    coins: Int,
    diamonds: Int,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onLocker: () -> Unit,
    onWorlds: () -> Unit,
    onSettings: () -> Unit,
    onAdmin: () -> Unit,
) {
    val s = LocalStrings.current
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(top = 48.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WalletChip(s.rings, coins, Color(0xFFFFD24A))
                WalletChip(s.diamonds, diamonds, Color(0xFF7AF0FF))
            }
            Spacer(Modifier.height(36.dp))
            NeonTitle(s.appTitle)
            Text(
                "WORLD > STAGE",
                color = Color(0xFF9AA8B8),
                fontFamily = GameFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            NeonButton(s.play, onPlay, Modifier.fillMaxWidth(0.7f).height(56.dp), accent = NeonLime)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(s.shop, onShop)
                NeonButton(s.locker, onLocker)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(s.worlds, onWorlds)
                NeonButton(s.settings, onSettings)
            }
            if (BuildConfig.IS_ADMIN_BUILD) {
                Spacer(Modifier.height(18.dp))
                NeonButton(s.developerPanel, onAdmin, accent = NeonMagenta)
            }
        }
    }
}
