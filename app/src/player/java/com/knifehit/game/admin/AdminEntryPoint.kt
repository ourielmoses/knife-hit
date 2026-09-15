package com.knifehit.game.admin

import androidx.compose.runtime.Composable

@Composable
fun AdminEntryPoint(
    onClose: () -> Unit,
    content: AdminHost,
) {
    // Player flavor: admin UI is absent from this APK.
}

data class AdminHost(
    val exportJson: () -> String = { "" },
    val importJson: (String) -> Unit = {},
    val reset: () -> Unit = {},
    val addSkin: (String, String, String, String, String, String, String) -> String? = { _, _, _, _, _, _, _ -> null },
    val addWorld: (String, String, String) -> String? = { _, _, _ -> null },
    val priceRows: List<Triple<String, Int, Int>> = emptyList(),
    val commitPrices: (List<Triple<String, Int, Int>>) -> String? = { null },
    val mint: (Int, Int) -> Unit = { _, _ -> },
    val unlockAll: () -> Unit = {},
    val passcodeHash: String = "",
    val unlocked: Boolean = false,
    val onUnlock: (String) -> Boolean = { false },
    val cooldown: Boolean = false,
)
