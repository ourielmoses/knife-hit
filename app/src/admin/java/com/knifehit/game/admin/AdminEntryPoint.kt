package com.knifehit.game.admin

import androidx.compose.runtime.Composable

@Composable
fun AdminEntryPoint(
    onClose: () -> Unit,
    content: AdminHost,
) {
    AdminPanel(onClose, content)
}

data class AdminHost(
    val exportJson: () -> String,
    val importJson: (String) -> Unit,
    val reset: () -> Unit,
    val addSkin: (id: String, name: String, color: String, speed: String, coins: String, diamonds: String, timbre: String) -> String?,
    val addWorld: (name: String, top: String, bottom: String) -> String?,
    val priceRows: List<Triple<String, Int, Int>>,
    val commitPrices: (List<Triple<String, Int, Int>>) -> String?,
    val mint: (coins: Int, diamonds: Int) -> Unit,
    val unlockAll: () -> Unit,
    val passcodeHash: String,
    val unlocked: Boolean,
    val onUnlock: (String) -> Boolean,
    val cooldown: Boolean,
)
