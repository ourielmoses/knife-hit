package com.knifehit.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class ParticleKind { LEAVES, SPARKS, BUBBLES, SAND, SNOW, EMBERS, CRUMBS, GEARS, NEON, VOID }

@Serializable
data class WorldTheme(
    val bgTopArgb: Long,
    val bgBottomArgb: Long,
    val primaryArgb: Long,
    val accentArgb: Long,
    val glowArgb: Long,
    val cloudTintArgb: Long,
    val particle: ParticleKind,
)

@Serializable
data class BossBlueprint(
    val id: String,
    val name: String,
    val targetId: String,
    val radiusScale: Float = 1.22f,
)

@Serializable
data class WorldDef(
    val id: Int,
    val name: String,
    val theme: WorldTheme,
    val targetIds: List<String>,
    val boss: BossBlueprint,
    val custom: Boolean = false,
)
