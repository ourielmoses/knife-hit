package com.dino.game.engine

import com.dino.game.model.CloudState
import com.dino.game.model.Constants
import com.dino.game.model.GameEvent
import com.dino.game.model.GameSnapshot
import com.dino.game.model.ObstacleKind
import com.dino.game.model.ObstacleState
import com.dino.game.model.ParticleState
import com.dino.game.model.PlayerState
import com.dino.game.model.ScreenState
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private var highScore: Int = 0,
) {
    private var screen = ScreenState.Title
    private var player = idlePlayer()
    private val obstacles = mutableListOf<ObstacleState>()
    private val clouds = mutableListOf<CloudState>()
    private val particles = mutableListOf<ParticleState>()
    private val pendingEvents = mutableListOf<GameEvent>()
    private var groundOffset = 0f
    private var duneOffset = 0f
    private var scoreFloat = 0f
    private var speed = Constants.BASE_SPEED
    private var spawnCooldown = 1.2f
    private var runAnimTimer = 0f
    private var birdAnimTimer = 0f
    private var gameOverLock = 0f
    private var deathPoseRemaining = 0f
    private var isNewRecord = false
    private var duckHeld = false
    private var worldWidth = 800f
    private var nextObstacleId = 1L
    private var nextCloudId = 1L
    private var nextParticleId = 1L
    private var lastMilestone = 0
    private var wasNight = false
    /** 0 = day, 1 = night — smoothed toward target each frame. */
    private var nightBlend = 0f
    private var random = Random(System.currentTimeMillis())

    fun setHighScore(value: Int) {
        highScore = value
    }

    fun setWorldWidth(width: Float) {
        if (width > 0f) worldWidth = width
    }

    fun snapshot(): GameSnapshot {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        return GameSnapshot(
            screen = screen,
            player = player,
            obstacles = obstacles.toList(),
            clouds = clouds.toList(),
            particles = particles.toList(),
            groundOffset = groundOffset,
            duneOffset = duneOffset,
            score = scoreFloat.toInt(),
            highScore = highScore,
            isNewRecord = isNewRecord,
            speed = speed,
            gameOverLockRemaining = gameOverLock,
            deathPoseRemaining = deathPoseRemaining,
            isNight = nightBlend > 0.5f,
            nightBlend = nightBlend,
            events = events,
        )
    }

    /** Title menu Play button (and optional retry). */
    fun onPlayPress() {
        when (screen) {
            ScreenState.Title -> startRun()
            ScreenState.GameOver -> if (gameOverLock <= 0f) startRun()
            ScreenState.Playing, ScreenState.Paused -> Unit
        }
    }

    fun onJumpPress() {
        when (screen) {
            ScreenState.Title, ScreenState.Paused -> Unit
            ScreenState.GameOver -> if (gameOverLock <= 0f) startRun()
            ScreenState.Playing -> {
                if (player.onGround && !player.dead) {
                    player = player.copy(
                        velocityY = Constants.JUMP_VELOCITY,
                        onGround = false,
                        ducking = false,
                        height = Constants.PLAYER_STAND_H,
                        width = Constants.PLAYER_STAND_W,
                        y = Constants.GROUND_Y - Constants.PLAYER_STAND_H,
                    )
                    emit(GameEvent.Jump)
                }
            }
        }
    }

    fun onDuckChanged(held: Boolean) {
        duckHeld = held
        if (screen != ScreenState.Playing || player.dead) return
        if (player.onGround) {
            applyDuckPose(held)
        }
    }

    fun pause() {
        if (screen != ScreenState.Playing) return
        screen = ScreenState.Paused
        duckHeld = false
        if (player.onGround && !player.dead) applyDuckPose(false)
    }

    fun resume() {
        if (screen == ScreenState.Paused) screen = ScreenState.Playing
    }

    /** Return to the title screen as if the app just opened. */
    fun goHome() {
        screen = ScreenState.Title
        player = idlePlayer()
        obstacles.clear()
        particles.clear()
        scoreFloat = 0f
        speed = Constants.BASE_SPEED
        spawnCooldown = 1.2f
        gameOverLock = 0f
        deathPoseRemaining = 0f
        isNewRecord = false
        duckHeld = false
        groundOffset = 0f
        duneOffset = 0f
        lastMilestone = 0
        wasNight = false
        nightBlend = 0f
        ensureDecor(force = true)
    }

    fun update(dt: Float): Int? {
        val clamped = dt.coerceIn(0f, 0.05f)
        updateNightBlend(clamped)
        when (screen) {
            ScreenState.Title -> {
                updateTitle(clamped)
                return null
            }
            ScreenState.Paused -> return null
            ScreenState.GameOver -> {
                updateDeathFall(clamped)
                if (player.onGround) {
                    val clear = !overlapsGroundCactus(player.x, player.width, player.height)
                    if (clear) {
                        // Arm pose timer once we've finished sliding off a cactus.
                        if (deathPoseRemaining > 100f || gameOverLock > 100f) {
                            deathPoseRemaining = Constants.DEATH_POSE_SECONDS
                            gameOverLock = Constants.DEATH_POSE_SECONDS + 0.4f
                        }
                        if (gameOverLock > 0f) gameOverLock = max(0f, gameOverLock - clamped)
                        if (deathPoseRemaining > 0f) {
                            deathPoseRemaining = max(0f, deathPoseRemaining - clamped)
                        }
                    }
                }
                updateParticles(clamped)
                return null
            }
            ScreenState.Playing -> return updatePlaying(clamped)
        }
    }

    private fun updateNightBlend(dt: Float) {
        val target = if (
            (screen == ScreenState.Playing || screen == ScreenState.Paused) &&
            isNightMode(scoreFloat.toInt())
        ) {
            1f
        } else {
            0f
        }
        if (target > nightBlend) {
            nightBlend = min(1f, nightBlend + dt / Constants.NIGHT_BLEND_SECONDS)
        } else if (target < nightBlend) {
            nightBlend = max(0f, nightBlend - dt / Constants.NIGHT_BLEND_SECONDS)
        }
    }

    private fun updateTitle(dt: Float) {
        ensureDecor()
        scrollDecor(dt, Constants.BASE_SPEED * 0.35f)
        duneOffset = (duneOffset + Constants.BASE_SPEED * 0.12f * dt) % Constants.DUNE_PERIOD
        runAnimTimer += dt
        if (runAnimTimer >= Constants.RUN_FRAME_SECONDS * 2f) {
            runAnimTimer = 0f
            player = player.copy(runFrame = if (player.runFrame == 0) 1 else 0)
        }
        updateParticles(dt)
    }

    private fun updatePlaying(dt: Float): Int? {
        ensureDecor()
        speed = min(
            Constants.MAX_SPEED,
            Constants.BASE_SPEED + scoreFloat * Constants.SPEED_PER_SCORE,
        )
        scoreFloat += speed * dt * Constants.SCORE_PER_WORLD_UNIT
        val score = scoreFloat.toInt()

        // Score milestones every 100
        val milestone = score / Constants.SCORE_MILESTONE
        if (milestone > lastMilestone && score > 0) {
            lastMilestone = milestone
            emit(GameEvent.Milestone)
        }

        val night = isNightMode(score)
        if (night != wasNight) {
            wasNight = night
            emit(GameEvent.NightChanged)
        }

        // Physics
        val wasAirborne = !player.onGround
        var vy = player.velocityY
        var y = player.y
        var onGround = player.onGround
        if (!onGround) {
            vy += Constants.GRAVITY * dt
            y += vy * dt
            val groundTop = Constants.GROUND_Y - playerStandingOrDuckHeight()
            if (y >= groundTop) {
                y = groundTop
                vy = 0f
                onGround = true
            }
        }

        if (wasAirborne && onGround) {
            emit(GameEvent.Land)
            spawnDust(player.x + player.width * 0.35f, Constants.GROUND_Y)
        }

        val ducking = onGround && duckHeld
        val width = if (ducking) Constants.PLAYER_DUCK_W else Constants.PLAYER_STAND_W
        val height = if (ducking) Constants.PLAYER_DUCK_H else Constants.PLAYER_STAND_H
        if (onGround) {
            y = Constants.GROUND_Y - height
        }

        runAnimTimer += dt
        var runFrame = player.runFrame
        if (onGround && !ducking) {
            if (runAnimTimer >= Constants.RUN_FRAME_SECONDS) {
                runAnimTimer = 0f
                runFrame = 1 - runFrame
            }
        } else if (ducking) {
            runFrame = 0
        }

        player = player.copy(
            y = y,
            velocityY = vy,
            onGround = onGround,
            ducking = ducking,
            width = width,
            height = height,
            runFrame = runFrame,
        )

        groundOffset = (groundOffset + speed * dt) % 24f
        duneOffset = (duneOffset + speed * 0.35f * dt) % Constants.DUNE_PERIOD
        scrollDecor(dt, speed)
        updateParticles(dt)
        birdAnimTimer += dt
        val birdFrame = if ((birdAnimTimer / Constants.BIRD_FRAME_SECONDS).toInt() % 2 == 0) 0 else 1

        val iterator = obstacles.listIterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            val nx = o.x - speed * dt
            if (nx + o.width < -40f) {
                iterator.remove()
            } else {
                val frame = if (o.kind.isBird()) birdFrame else o.frame
                iterator.set(o.copy(x = nx, frame = frame))
            }
        }

        spawnCooldown -= dt
        if (spawnCooldown <= 0f) {
            spawnObstacle()
            spawnCooldown = nextSpawnDelay()
        }

        // Tiny dust while running
        if (onGround && !ducking && random.nextFloat() < 0.08f) {
            spawnDust(player.x + player.width * 0.2f, Constants.GROUND_Y, count = 1)
        }

        if (checkCollision()) {
            beginDeath()
            return score
        }
        return null
    }

    private fun beginDeath() {
        val score = scoreFloat.toInt()
        val wasAirborne = !player.onGround
        if (wasAirborne) {
            // Keep air position/velocity; fall with gravity, then lie down.
            player = player.copy(
                dead = true,
                ducking = false,
                width = Constants.PLAYER_STAND_W,
                height = Constants.PLAYER_STAND_H,
            )
            gameOverLock = Float.MAX_VALUE
            deathPoseRemaining = Float.MAX_VALUE
        } else {
            settleDeadOnGround(playDust = true)
        }
        screen = ScreenState.GameOver
        isNewRecord = score > highScore
        if (isNewRecord) highScore = score
        emit(GameEvent.Die)
    }

    /**
     * Gravity while dead in the air; slowly slide off any cactus under the
     * fall / landing so the lying pose isn't stuck inside an obstacle.
     */
    private fun updateDeathFall(dt: Float) {
        if (!player.dead) return

        slideOffCacti(dt)

        if (player.onGround) return

        var vy = player.velocityY + Constants.GRAVITY * dt
        var y = player.y + vy * dt
        val groundTop = Constants.GROUND_Y - Constants.PLAYER_STAND_H
        if (y >= groundTop) {
            player = player.copy(
                y = Constants.GROUND_Y - Constants.PLAYER_DEAD_H,
                velocityY = 0f,
                onGround = true,
                width = Constants.PLAYER_DEAD_W,
                height = Constants.PLAYER_DEAD_H,
            )
            settleDeadOnGround(playDust = true)
            // Keep sliding this frame if we landed on a cactus.
            slideOffCacti(dt)
        } else {
            player = player.copy(y = y, velocityY = vy, onGround = false)
        }
    }

    /** Ease the dino left/right away from ground cacti during the death anim. */
    private fun slideOffCacti(dt: Float) {
        val w = if (player.onGround) Constants.PLAYER_DEAD_W else player.width
        val h = if (player.onGround) Constants.PLAYER_DEAD_H else player.height
        if (!overlapsGroundCactus(player.x, w, h)) return

        val targetX = clearDeathX(player.x, w) ?: return
        val maxStep = Constants.DEATH_SLIDE_SPEED * dt
        val nextX = player.x + (targetX - player.x).coerceIn(-maxStep, maxStep)
        player = player.copy(
            x = nextX.coerceAtLeast(8f),
            width = w,
            height = h,
            y = if (player.onGround) Constants.GROUND_Y - h else player.y,
        )
    }

    private fun overlapsGroundCactus(x: Float, w: Float, h: Float): Boolean {
        val y = if (player.onGround) {
            Constants.GROUND_Y - h
        } else {
            player.y
        }
        // While falling, probe the ground landing footprint so we ease aside early.
        val py = if (!player.onGround) Constants.GROUND_Y - Constants.PLAYER_DEAD_H else y
        val ph = if (!player.onGround) Constants.PLAYER_DEAD_H else h
        val pw = if (!player.onGround) Constants.PLAYER_DEAD_W else w
        for (o in obstacles) {
            if (o.kind.isBird()) continue
            if (rectsOverlap(x, py, pw, ph, o.x, o.y, o.width, o.height)) return true
        }
        return false
    }

    private fun clearDeathX(fromX: Float, width: Float): Float? {
        val groundY = Constants.GROUND_Y - Constants.PLAYER_DEAD_H
        val h = Constants.PLAYER_DEAD_H
        val blockers = obstacles.filter { o ->
            !o.kind.isBird() &&
                rectsOverlap(fromX, groundY, width, h, o.x, o.y, o.width, o.height)
        }
        if (blockers.isEmpty()) return null

        // Prefer easing left (behind the cactus); fall back to the right gap.
        var bestLeft = fromX
        var bestRight = fromX
        var hasLeft = false
        var hasRight = false
        for (o in blockers) {
            val left = o.x - width - 6f
            val right = o.x + o.width + 6f
            if (!hasLeft || left < bestLeft) {
                bestLeft = left
                hasLeft = true
            }
            if (!hasRight || right > bestRight) {
                bestRight = right
                hasRight = true
            }
        }
        val leftTarget = bestLeft.coerceAtLeast(8f)
        val rightTarget = bestRight
        return if (kotlin.math.abs(leftTarget - fromX) <= kotlin.math.abs(rightTarget - fromX) + 20f) {
            leftTarget
        } else {
            rightTarget
        }
    }

    private fun rectsOverlap(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float,
    ): Boolean = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    private fun settleDeadOnGround(playDust: Boolean) {
        player = player.copy(
            dead = true,
            ducking = false,
            onGround = true,
            velocityY = 0f,
            width = Constants.PLAYER_DEAD_W,
            height = Constants.PLAYER_DEAD_H,
            y = Constants.GROUND_Y - Constants.PLAYER_DEAD_H,
        )
        // Don't start the GAME OVER chrome timer until clear of cacti.
        if (overlapsGroundCactus(player.x, player.width, player.height)) {
            gameOverLock = Float.MAX_VALUE
            deathPoseRemaining = Float.MAX_VALUE
        } else {
            gameOverLock = Constants.DEATH_POSE_SECONDS + 0.4f
            deathPoseRemaining = Constants.DEATH_POSE_SECONDS
        }
        if (playDust) {
            spawnDust(player.x + player.width * 0.5f, Constants.GROUND_Y, count = 12)
        }
    }

    private fun isNightMode(score: Int): Boolean {
        if (score < Constants.NIGHT_SCORE_PERIOD) return false
        val period = score / Constants.NIGHT_SCORE_PERIOD
        return period % 2 == 1
    }

    private fun emit(event: GameEvent) {
        pendingEvents += event
    }

    private fun spawnDust(x: Float, y: Float, count: Int = 5) {
        repeat(count) {
            if (particles.size >= Constants.MAX_PARTICLES) {
                particles.removeAt(0)
            }
            particles += ParticleState(
                id = nextParticleId++,
                x = x + random.nextFloat() * 10f - 5f,
                y = y - random.nextFloat() * 4f,
                vx = -speed * 0.15f + random.nextFloat() * 40f - 60f,
                vy = -40f - random.nextFloat() * 60f,
                life = 0.25f + random.nextFloat() * 0.25f,
                maxLife = 0.45f,
                size = 1.5f + random.nextFloat() * 2.5f,
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.listIterator()
        while (it.hasNext()) {
            val p = it.next()
            val life = p.life - dt
            if (life <= 0f) {
                it.remove()
            } else {
                it.set(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        vy = p.vy + 220f * dt,
                        life = life,
                    ),
                )
            }
        }
    }

    private fun playerStandingOrDuckHeight(): Float =
        if (duckHeld && player.onGround) Constants.PLAYER_DUCK_H else Constants.PLAYER_STAND_H

    private fun applyDuckPose(held: Boolean) {
        val width = if (held) Constants.PLAYER_DUCK_W else Constants.PLAYER_STAND_W
        val height = if (held) Constants.PLAYER_DUCK_H else Constants.PLAYER_STAND_H
        player = player.copy(
            ducking = held,
            width = width,
            height = height,
            y = Constants.GROUND_Y - height,
        )
    }

    private fun startRun() {
        screen = ScreenState.Playing
        player = idlePlayer().copy(runFrame = 0)
        obstacles.clear()
        particles.clear()
        scoreFloat = 0f
        speed = Constants.BASE_SPEED
        spawnCooldown = 1.0f
        gameOverLock = 0f
        deathPoseRemaining = 0f
        isNewRecord = false
        duckHeld = false
        groundOffset = 0f
        lastMilestone = 0
        wasNight = false
        nightBlend = 0f
        ensureDecor(force = clouds.isEmpty())
    }

    private fun idlePlayer() = PlayerState(
        x = Constants.PLAYER_X,
        y = Constants.GROUND_Y - Constants.PLAYER_STAND_H,
        width = Constants.PLAYER_STAND_W,
        height = Constants.PLAYER_STAND_H,
        velocityY = 0f,
        onGround = true,
        ducking = false,
        dead = false,
        runFrame = 0,
    )

    private fun nextSpawnDelay(): Float {
        val t = (speed - Constants.BASE_SPEED) / (Constants.MAX_SPEED - Constants.BASE_SPEED)
        val minGap = 0.75f - 0.25f * t
        val maxGap = 1.55f - 0.45f * t
        return random.nextFloat() * (maxGap - minGap) + minGap
    }

    private fun spawnObstacle() {
        val score = scoreFloat.toInt()
        val kind = pickKind(score)
        val size = sizeFor(kind)
        val y = when (kind) {
            ObstacleKind.BirdHigh -> Constants.GROUND_Y - 95f
            ObstacleKind.BirdMid -> Constants.GROUND_Y - 70f
            ObstacleKind.BirdLow -> Constants.GROUND_Y - 42f
            else -> Constants.GROUND_Y - size.second
        }
        obstacles += ObstacleState(
            id = nextObstacleId++,
            kind = kind,
            x = worldWidth + 20f,
            y = y,
            width = size.first,
            height = size.second,
        )
    }

    private fun pickKind(score: Int): ObstacleKind {
        val birdsUnlocked = score >= Constants.BIRD_UNLOCK_SCORE
        val bigCactusUnlocked = score >= Constants.BIG_CACTUS_UNLOCK_SCORE
        val roll = random.nextFloat()
        return when {
            birdsUnlocked && roll < 0.18f -> when (random.nextInt(3)) {
                0 -> ObstacleKind.BirdHigh
                1 -> ObstacleKind.BirdMid
                else -> ObstacleKind.BirdLow
            }
            // Before 800: no tall cactus / wide triple clusters.
            !bigCactusUnlocked -> when {
                score < 50 -> ObstacleKind.CactusSmall
                score < 150 -> if (roll < 0.55f) {
                    ObstacleKind.CactusSmall
                } else {
                    ObstacleKind.CactusMedium
                }
                score < 400 -> when {
                    roll < 0.4f -> ObstacleKind.CactusSmall
                    roll < 0.75f -> ObstacleKind.CactusMedium
                    else -> ObstacleKind.CactusCluster2
                }
                else -> when {
                    roll < 0.3f -> ObstacleKind.CactusSmall
                    roll < 0.65f -> ObstacleKind.CactusMedium
                    else -> ObstacleKind.CactusCluster2
                }
            }
            score < 50 -> ObstacleKind.CactusSmall
            score < 150 -> if (roll < 0.55f) ObstacleKind.CactusSmall else ObstacleKind.CactusMedium
            score < 300 -> when {
                roll < 0.35f -> ObstacleKind.CactusSmall
                roll < 0.6f -> ObstacleKind.CactusMedium
                roll < 0.8f -> ObstacleKind.CactusLarge
                else -> ObstacleKind.CactusCluster2
            }
            else -> when {
                roll < 0.25f -> ObstacleKind.CactusMedium
                roll < 0.45f -> ObstacleKind.CactusLarge
                roll < 0.65f -> ObstacleKind.CactusCluster2
                roll < 0.85f -> ObstacleKind.CactusCluster3
                else -> ObstacleKind.CactusSmall
            }
        }
    }

    private fun sizeFor(kind: ObstacleKind): Pair<Float, Float> = when (kind) {
        ObstacleKind.CactusSmall -> 40f to 52f
        ObstacleKind.CactusMedium -> 52f to 72f
        ObstacleKind.CactusLarge -> 60f to 96f
        ObstacleKind.CactusCluster2 -> 96f to 58f
        ObstacleKind.CactusCluster3 -> 140f to 68f
        ObstacleKind.BirdHigh, ObstacleKind.BirdMid, ObstacleKind.BirdLow -> 46f to 34f
    }

    private fun checkCollision(): Boolean {
        val (px, py, pw, ph) = playerHitbox()
        val inset = Constants.OBSTACLE_HIT_INSET
        for (o in obstacles) {
            val ox = o.x + inset
            val oy = o.y + inset
            val ow = (o.width - inset * 2f).coerceAtLeast(8f)
            val oh = (o.height - inset * 2f).coerceAtLeast(8f)
            if (px < ox + ow && px + pw > ox && py < oy + oh && py + ph > oy) {
                return true
            }
        }
        return false
    }

    /** Axis-aligned box trimmed to the dino body (not the full sprite rectangle). */
    private fun playerHitbox(): FloatArray {
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        if (player.ducking) {
            left = player.x + Constants.PLAYER_DUCK_HIT_INSET_L
            right = player.x + player.width - Constants.PLAYER_DUCK_HIT_INSET_R
            top = player.y + Constants.PLAYER_DUCK_HIT_INSET_T
            bottom = player.y + player.height - Constants.PLAYER_DUCK_HIT_INSET_B
        } else {
            left = player.x + Constants.PLAYER_HIT_INSET_L
            right = player.x + player.width - Constants.PLAYER_HIT_INSET_R
            top = player.y + Constants.PLAYER_HIT_INSET_T
            bottom = player.y + player.height - Constants.PLAYER_HIT_INSET_B
        }
        return floatArrayOf(
            left,
            top,
            (right - left).coerceAtLeast(8f),
            (bottom - top).coerceAtLeast(8f),
        )
    }

    private fun ensureDecor(force: Boolean = false) {
        if (!force && clouds.isNotEmpty()) return
        clouds.clear()
        repeat(4) { i ->
            clouds += CloudState(
                id = nextCloudId++,
                x = i * (worldWidth / 3f) + random.nextFloat() * 40f,
                y = 40f + random.nextFloat() * 100f,
                scale = 0.6f + random.nextFloat() * 0.7f,
            )
        }
    }

    private fun scrollDecor(dt: Float, currentSpeed: Float) {
        val cloudSpeed = currentSpeed * 0.25f
        for (i in clouds.indices) {
            val c = clouds[i]
            var x = c.x - cloudSpeed * dt
            if (x < -80f) {
                x = worldWidth + random.nextFloat() * 100f
                clouds[i] = c.copy(
                    x = x,
                    y = 30f + random.nextFloat() * 110f,
                    scale = 0.6f + random.nextFloat() * 0.7f,
                )
            } else {
                clouds[i] = c.copy(x = x)
            }
        }
    }
}

private fun ObstacleKind.isBird(): Boolean = when (this) {
    ObstacleKind.BirdHigh, ObstacleKind.BirdMid, ObstacleKind.BirdLow -> true
    else -> false
}
