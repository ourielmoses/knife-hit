package com.knifehit.game.model

enum class PickupKind { COIN, DIAMOND, SPEED }

sealed interface WheelAttachment {
    val relAngle: Float
}

data class StuckKnife(
    override val relAngle: Float,
    val skinId: String,
    val isObstacle: Boolean = false,
) : WheelAttachment

data class Collectible(
    override val relAngle: Float,
    val kind: PickupKind,
) : WheelAttachment

data class ReviveSnapshot(
    val attachments: List<WheelAttachment>,
    val struckIndex: Int,
    val rotation: Float,
    val ammoRemaining: Int,
    val score: Int,
    val speedMode: Boolean,
)
