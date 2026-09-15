package com.knifehit.game.model

@kotlinx.serialization.Serializable
data class RotationProfile(
    val baseSpeed: Float,
    val speedVariance: Float,
    val pauseChance: Float,
    val pauseDuration: Float,
    val reversalChance: Float,
    val reversalEase: Float,
)

data class StageSpec(
    val position: Position,
    val isBoss: Boolean,
    val target: TargetBlueprint,
    val knifeCount: Int,
    val obstacleCount: Int,
    val rotation: RotationProfile,
    val world: WorldDef,
)
