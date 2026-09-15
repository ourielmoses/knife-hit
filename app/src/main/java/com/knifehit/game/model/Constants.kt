package com.knifehit.game.model

const val TAU = (Math.PI * 2.0).toFloat()
const val MAX_KNIVES = 10
const val STARTER_SKIN_ID = "classic"
const val SAVE_VERSION = 1
const val REFERENCE_RADIUS_DP = 65f
const val STAGE_CLEAR_BONUS = 100
const val SCORE_GOOD = 10
const val SCORE_PERFECT = 50
const val SCORE_FIRST_KNIFE = 10
const val RINGS_PER_STAGE = 10
const val RINGS_PER_STAGE_REPLAY = 3
const val DIAMONDS_PER_BOSS = 2
const val DIAMONDS_PER_BOSS_REPLAY = 0
const val AD_CURRENCY_REWARD = 50
const val AD_CURRENCY_DAILY_CAP = 5
const val INTERSTITIAL_MIN_INTERVAL_MS = 90_000L
const val SHAKE_DURATION_SEC = 0.090f
const val REVIVE_COUNTDOWN_SEC = 3f
const val BOSS_INTRO_SEC = 1.6f
const val SPEED_MODE_MULTIPLIER = 2f
const val SAFETY_MARGIN = 0.85f
const val COLLECTIBLE_CLEARANCE_FLOOR = 0.30f

val STAGES_PER_WORLD = intArrayOf(3, 3, 3, 5, 5, 6, 7, 8, 9, 10)

fun stagesIn(world: Int): Int {
    val idx = (world - 1).coerceIn(0, STAGES_PER_WORLD.lastIndex)
    return STAGES_PER_WORLD[idx]
}

fun worldCount(): Int = STAGES_PER_WORLD.size
