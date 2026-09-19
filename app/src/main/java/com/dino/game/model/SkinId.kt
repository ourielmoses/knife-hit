package com.dino.game.model

/**
 * T-Rex looks from the skin picker.
 * Each has stand / run2 / jump / duck / dead drawables: skin_<id>_<pose>.
 */
enum class SkinId(val id: String, val label: String) {
    Classic("classic", "CLASSIC"),
    Arcade("arcade", "ARCADE"),
    Outline("outline", "OUTLINE"),
    Spiky("spiky", "SPIKY"),
    Cute("cute", "CUTE"),
    Robot("robot", "ROBOT"),
    Ghost("ghost", "GHOST"),
    Knight("knight", "KNIGHT");

    companion object {
        fun fromId(value: String?): SkinId =
            entries.firstOrNull { it.id == value } ?: Cute
    }
}
