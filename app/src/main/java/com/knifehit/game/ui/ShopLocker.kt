package com.knifehit.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.model.KnifeSkin
import com.knifehit.game.render.drawKnife
import com.knifehit.game.render.toComposeColor

@Composable
fun LockerOverlay(
    skins: List<KnifeSkin>,
    owned: Set<String>,
    equipped: String,
    onBack: () -> Unit,
    onEquip: (String) -> Unit,
    onOpenShop: (String) -> Unit,
) {
    val s = LocalStrings.current
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NeonButton(s.back, onBack)
            Spacer(Modifier.height(8.dp))
            Text(s.locker, color = NeonCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(skins, key = { it.id }) { skin ->
                    val isOwned = skin.id in owned
                    KnifeCard(
                        skin = skin,
                        owned = isOwned,
                        equipped = skin.id == equipped,
                        onClick = {
                            if (isOwned) onEquip(skin.id) else onOpenShop(skin.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ShopOverlay(
    skins: List<KnifeSkin>,
    owned: Set<String>,
    coins: Int,
    diamonds: Int,
    adRemaining: Int,
    scrollToId: String?,
    onBack: () -> Unit,
    onBuyCoins: (KnifeSkin) -> String?,
    onBuyDiamonds: (KnifeSkin) -> String?,
    onAdUnlock: (KnifeSkin) -> Unit,
    onAdCurrency: () -> Unit,
) {
    val s = LocalStrings.current
    val grid = rememberLazyGridState()
    var pendingDiamond by remember { mutableStateOf<KnifeSkin?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(scrollToId, skins) {
        val idx = skins.indexOfFirst { it.id == scrollToId }
        if (idx >= 0) grid.scrollToItem(idx)
    }
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NeonButton(s.back, onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WalletChip(s.rings, coins, Color(0xFFFFD24A))
                    WalletChip(s.diamonds, diamonds, Color(0xFF7AF0FF))
                }
            }
            Spacer(Modifier.height(8.dp))
            NeonButton(
                "${s.watchAdCurrency} ($adRemaining ${s.adCap})",
                onAdCurrency,
                enabled = adRemaining > 0,
            )
            Spacer(Modifier.height(8.dp))
            if (error != null) Text(error!!, color = NeonMagenta, fontSize = 13.sp)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                state = grid,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(skins, key = { it.id }) { skin ->
                    val isOwned = skin.id in owned
                    Column(
                        Modifier
                            .background(Color(0xFF151A24), RoundedCornerShape(12.dp))
                            .border(1.dp, skin.colorArgb.toComposeColor().copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                    ) {
                        KnifePreview(skin)
                        Text(skin.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (isOwned) {
                            Text(s.owned, color = NeonLime, fontSize = 12.sp)
                        } else {
                            if (skin.soldForCoins()) {
                                NeonButton(s.buyCoins + " ${skin.priceInCoins}", {
                                    error = onBuyCoins(skin)
                                }, Modifier.fillMaxWidth())
                            }
                            if (skin.soldForDiamonds()) {
                                NeonButton(s.buyDiamonds + " ${skin.priceInDiamonds}", {
                                    pendingDiamond = skin
                                }, Modifier.fillMaxWidth(), accent = Color(0xFF7AF0FF))
                            }
                            if (skin.adUnlockable) {
                                NeonButton(s.watchAd, { onAdUnlock(skin) }, Modifier.fillMaxWidth(), accent = NeonMagenta)
                            }
                            if (!skin.soldForCoins() && !skin.soldForDiamonds() && !skin.adUnlockable && !skin.isUnlocked) {
                                Text(s.comingSoon, color = Color(0xFF9AA8B8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDiamond?.let { skin ->
        AlertDialog(
            onDismissRequest = { pendingDiamond = null },
            confirmButton = {
                TextButton(onClick = {
                    error = onBuyDiamonds(skin)
                    pendingDiamond = null
                }) { Text(s.commit) }
            },
            dismissButton = { TextButton(onClick = { pendingDiamond = null }) { Text(s.back) } },
            title = { Text(s.confirmBuy) },
            text = { Text("${skin.name} · ${skin.priceInDiamonds}") },
        )
    }
}

@Composable
fun KnifeCard(skin: KnifeSkin, owned: Boolean, equipped: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    Column(
        Modifier
            .alpha(if (owned) 1f else 0.45f)
            .background(Color(0xFF151A24), RoundedCornerShape(12.dp))
            .border(
                width = if (equipped) 2.dp else 1.dp,
                color = if (equipped) NeonCyan else Color(0xFF33404C),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KnifePreview(skin)
        Text(skin.name, color = Color.White, fontSize = 12.sp)
        if (!owned) Text(s.locked, color = Color(0xFF9AA8B8), fontSize = 11.sp)
        else if (equipped) Text(s.equipped, color = NeonLime, fontSize = 11.sp)
    }
}

@Composable
fun KnifePreview(skin: KnifeSkin) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
        translateKnife(skin)
    }
}

private fun DrawScope.translateKnife(skin: KnifeSkin) {
    translate(left = size.width / 2f, top = size.height * 0.85f) {
        drawKnife(skin, size.height * 0.75f, size.width * 0.18f, tipUp = true)
    }
}
