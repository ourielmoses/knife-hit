package com.knifehit.game.model

enum class PlayState {
    Attract,
    BossIntro,
    Ready,
    Throwing,
    Destroying,
    StageClear,
    WorldVictory,
    GameOver,
    ReviveCountdown,
}

enum class PrecisionBand { FIRST, PERFECT, GOOD }

data class ScoreEvent(
    val band: PrecisionBand,
    val points: Int,
    val worldX: Float,
    val worldY: Float,
    val bornAt: Float,
)

data class CurrencyPack(
    val id: String,
    val priceLabel: String,
    val coins: Int,
    val diamonds: Int,
)
