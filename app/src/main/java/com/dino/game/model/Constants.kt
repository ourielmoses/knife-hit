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

    /**
     * Player hitbox insets — tighter than the sprite so collisions follow the
     * body silhouette (ignore empty corners / spikes / tail fluff).
     */
    const val PLAYER_HIT_INSET_L = 14f
    const val PLAYER_HIT_INSET_R = 10f
    const val PLAYER_HIT_INSET_T = 14f
    const val PLAYER_HIT_INSET_B = 2f
    const val PLAYER_DUCK_HIT_INSET_L = 16f
    const val PLAYER_DUCK_HIT_INSET_R = 12f
    const val PLAYER_DUCK_HIT_INSET_T = 6f
    const val PLAYER_DUCK_HIT_INSET_B = 1f

    /** Obstacle hitbox inset so cactus arms / bird tips feel fair. */
    const val OBSTACLE_HIT_INSET = 8f

    const val JUMP_VELOCITY = -640f
    const val GRAVITY = 1850f

    const val BASE_SPEED = 340f
    const val MAX_SPEED = 780f
    const val SPEED_PER_SCORE = 0.55f

    const val SCORE_PER_WORLD_UNIT = 0.08f

    const val BIRD_UNLOCK_SCORE = 200
    /** Tall / wide cactus clusters unlock after this score. */
    const val BIG_CACTUS_UNLOCK_SCORE = 800
    const val GAME_OVER_INPUT_LOCK = 0.55f
    /** Seconds to show the lying dead pose before GAME OVER chrome. */
    const val DEATH_POSE_SECONDS = 0.75f
    /** How fast the dino eases sideways off a cactus during the death fall. */
    const val DEATH_SLIDE_SPEED = 120f
    /** Hitbox / draw size once the dino is lying on the ground. */
    const val PLAYER_DEAD_W = 78f
    const val PLAYER_DEAD_H = 36f

    const val RUN_FRAME_SECONDS = 0.1f
    const val BIRD_FRAME_SECONDS = 0.18f

    /** Phase 2: night mode toggles every N score points (Chrome-like). */
    const val NIGHT_SCORE_PERIOD = 700
    /** Seconds to fade between day and night palettes. */
    const val NIGHT_BLEND_SECONDS = 1.6f
    const val SCORE_MILESTONE = 100

    const val MAX_PARTICLES = 24
    const val DUNE_PERIOD = 180f
}
