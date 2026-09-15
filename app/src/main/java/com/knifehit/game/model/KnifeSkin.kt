package com.knifehit.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class Timbre { HEAVY, BLADE, LASER, ARCANE }

@Serializable
data class KnifeSkin(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val throwSpeed: Float,
    val priceInCoins: Int,
    val priceInDiamonds: Int,
    val isUnlocked: Boolean,
    val timbre: Timbre,
    val adUnlockable: Boolean = false,
) {
    fun soldForCoins(): Boolean = priceInCoins > 0
    fun soldForDiamonds(): Boolean = priceInDiamonds > 0
}
