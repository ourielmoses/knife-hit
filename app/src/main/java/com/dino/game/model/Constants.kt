package com.dino.game.model

object Constants {
    const val WORLD_HEIGHT = 400f
    const val GROUND_Y = 330f

    const val PLAYER_X = 72f
    const val PLAYER_STAND_W = 56f
    const val PLAYER_STAND_H = 56f
    // Duck: lower crouch + wider (head leans forward) so mid-birds clear.
    const val PLAYER_DUCK_W = 88f
    const val PLAYER_DUCK_H = 28f

    /** Inset hitbox so near-misses feel fair (Chrome-like). */
    const val HITBOX_INSET = 4f

    const val JUMP_VELOCITY = -640f
    const val GRAVITY = 1850f

    const val BASE_SPEED = 340f
    const val MAX_SPEED = 780f
    const val SPEED_PER_SCORE = 0.55f

    const val SCORE_PER_WORLD_UNIT = 0.08f

    const val BIRD_UNLOCK_SCORE = 200
    const val GAME_OVER_INPUT_LOCK = 0.45f

    const val RUN_FRAME_SECONDS = 0.1f
    const val BIRD_FRAME_SECONDS = 0.18f

    /** Phase 2: night mode toggles every N score points (Chrome-like). */
    const val NIGHT_SCORE_PERIOD = 700
    const val SCORE_MILESTONE = 100

    const val MAX_PARTICLES = 24
    const val DUNE_PERIOD = 180f
}
