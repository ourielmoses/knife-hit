package com.knifehit.game.engine

import com.knifehit.game.model.RotationProfile
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class RotationController(
    private val random: Random = Random.Default,
) {
    private var elapsed = 0f
    private var velocity = 0f
    private var targetSign = 1f
    private var reversing = false
    private var reverseT = 0f
    private var reverseFrom = 0f
    private var reverseTo = 0f
    private var paused = false
    private var pauseLeft = 0f
    private var sinceChange = 999f
    private var dwell = 999f
    private var profile: RotationProfile = RotationProfile(1f, 0f, 0f, 0f, 0f, 0.3f)
    private var suppressWindow = 0f

    fun reset(profile: RotationProfile, randomize: Boolean = true) {
        this.profile = profile
        elapsed = 0f
        targetSign = 1f
        reversing = false
        paused = false
        pauseLeft = 0f
        sinceChange = 999f
        dwell = 999f
        suppressWindow = 0f
        velocity = profile.baseSpeed
        if (randomize && random.nextBoolean() && profile.reversalChance > 0f) {
            targetSign = -1f
            velocity = -profile.baseSpeed
        }
    }

    fun notifyThrowApproaching(secondsToImpact: Float) {
        if (secondsToImpact in 0f..0.28f) {
            suppressWindow = secondsToImpact
        }
    }

    fun step(dt: Float): Float {
        elapsed += dt
        sinceChange += dt
        dwell += dt
        if (suppressWindow > 0f) suppressWindow -= dt

        val minChangeInterval = 0.85f
        val minDwell = 0.55f
        val canChange = suppressWindow <= 0f && sinceChange > minChangeInterval && dwell > minDwell

        if (paused) {
            pauseLeft -= dt
            if (pauseLeft <= 0f) {
                paused = false
                dwell = 0f
                sinceChange = 0f
            }
            return 0f
        }

        if (reversing) {
            reverseT += dt / profile.reversalEase.coerceAtLeast(0.05f)
            val u = reverseT.coerceIn(0f, 1f)
            val eased = u * u * (3f - 2f * u)
            velocity = reverseFrom + (reverseTo - reverseFrom) * eased
            if (u >= 1f) {
                reversing = false
                velocity = reverseTo
                dwell = 0f
                sinceChange = 0f
            }
            return velocity
        }

        val wobble = if (profile.speedVariance == 0f) 0f else
            sin(elapsed * 1.7f) * profile.speedVariance
        velocity = targetSign * (profile.baseSpeed + wobble)

        if (canChange && profile.pauseChance > 0f && random.nextFloat() < profile.pauseChance * dt) {
            paused = true
            pauseLeft = profile.pauseDuration
            sinceChange = 0f
            return 0f
        }
        if (canChange && profile.reversalChance > 0f && random.nextFloat() < profile.reversalChance * dt) {
            reversing = true
            reverseT = 0f
            reverseFrom = velocity
            targetSign = -targetSign
            reverseTo = targetSign * profile.baseSpeed
            sinceChange = 0f
        }
        return velocity
    }

    fun currentVelocity(): Float = velocity
}
