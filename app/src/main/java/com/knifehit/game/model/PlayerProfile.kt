package com.knifehit.game.model

data class PlayerProfile(
    val saveVersion: Int = SAVE_VERSION,
    val coins: Int = 0,
    val diamonds: Int = 0,
    val bestScore: Int = 0,
    val unlockedWorlds: Set<Int> = setOf(1),
    val furthest: Position = Position(1, 1),
    val active: Position = Position(1, 1),
    val ownedKnives: Set<String> = setOf(STARTER_SKIN_ID),
    val equippedKnifeId: String = STARTER_SKIN_ID,
    val hapticsEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val batterySaver: Boolean = false,
    val motionEnabled: Boolean = true,
    val ambientVolume: Float = 0.7f,
    val sfxVolume: Float = 0.9f,
    val language: String = "en",
    val adCurrencyDay: String = "",
    val adCurrencyUsed: Int = 0,
)
