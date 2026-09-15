package com.knifehit.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class TargetStyle { WOOD, METAL, WEDGES, STONE, SECTORS }

@Serializable
data class TargetBlueprint(
    val id: String,
    val name: String,
    val radiusDp: Float,
    val baseColorArgb: Long,
    val accentColorArgb: Long,
    val style: TargetStyle,
    val ringCount: Int = 4,
    val notchCount: Int = 8,
)
