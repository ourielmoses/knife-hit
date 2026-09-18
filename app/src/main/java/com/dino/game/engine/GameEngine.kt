package com.dino.game.engine

import com.dino.game.model.CloudState
import com.dino.game.model.Constants
import com.dino.game.model.GameSnapshot
import com.dino.game.model.ObstacleKind
import com.dino.game.model.ObstacleState
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
    private var groundOffset = 0f
    private var scoreFloat = 0f
    private var speed = Constants.BASE_SPEED
    private var spawnCooldown = 1.2f
    private var runAnimTimer = 0f
    private var birdAnimTimer = 0f
    private var gameOverLock = 0f
    private var isNewRecord = false
    private var duckHeld = false
    private var worldWidth = 800f
    private var nextObstacleId = 1L
    private var nextCloudId = 1L
    private var random = Random(System.currentTimeMillis())

    fun setHighScore(value: Int) {
        highScore = value
    }

    fun setWorldWidth(width: Float) {
        if (width > 0f) worldWidth = width
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        screen = screen,
        player = player,
        obstacles = obstacles.toList(),
        clouds = clouds.toList(),
        groundOffset = groundOffset,
        score = scoreFloat.toInt(),
        highScore = highScore,
        isNewRecord = isNewRecord,
        speed = speed,
        gameOverLockRemaining = gameOverLock,
    )

    fun onJumpPress() {
        when (screen) {
            ScreenState.Title -> startRun()
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
                }
            }
        }
    }

    fun onDuckChanged(held: Boolean) {
        duckHeld = held
        if (screen != ScreenState.Playing || player.dead) return
        // Duck only while grounded — no air-duck.
        if (player.onGround) {
            applyDuckPose(held)
        }
    }

    fun update(dt: Float): Int? {
        val clamped = dt.coerceIn(0f, 0.05f)
        when (screen) {
            ScreenState.Title -> {
                updateTitle(clamped)
                return null
            }
            ScreenState.GameOver -> {
                if (gameOverLock > 0f) gameOverLock = max(0f, gameOverLock - clamped)
                return null
            }
            ScreenState.Playing -> return updatePlaying(clamped)
        }
    }

    private fun updateTitle(dt: Float) {
        ensureDecor()
        scrollDecor(dt, Constants.BASE_SPEED * 0.35f)
        runAnimTimer += dt
        if (runAnimTimer >= Constants.RUN_FRAME_SECONDS * 2f) {
            runAnimTimer = 0f
            // Idle blink via runFrame 0/1 swap slowly
            player = player.copy(runFrame = if (player.runFrame == 0) 1 else 0)
        }
    }

    private fun updatePlaying(dt: Float): Int? {
        ensureDecor()
        speed = min(
            Constants.MAX_SPEED,
            Constants.BASE_SPEED + scoreFloat * Constants.SPEED_PER_SCORE,
        )
        scoreFloat += speed * dt * Constants.SCORE_PER_WORLD_UNIT

        // Physics
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
        scrollDecor(dt, speed)
        birdAnimTimer += dt
        val birdFrame = if ((birdAnimTimer / Constants.BIRD_FRAME_SECONDS).toInt() % 2 == 0) 0 else 1

        // Move obstacles
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

        if (checkCollision()) {
            player = player.copy(dead = true, velocityY = 0f, ducking = false)
            screen = ScreenState.GameOver
            gameOverLock = Constants.GAME_OVER_INPUT_LOCK
            val score = scoreFloat.toInt()
            isNewRecord = score > highScore
            if (isNewRecord) highScore = score
            return score
        }
        return null
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
        scoreFloat = 0f
        speed = Constants.BASE_SPEED
        spawnCooldown = 1.0f
        gameOverLock = 0f
        isNewRecord = false
        duckHeld = false
        groundOffset = 0f
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
            ObstacleKind.BirdMid -> Constants.GROUND_Y - 62f
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
        val roll = random.nextFloat()
        return when {
            birdsUnlocked && roll < 0.18f -> when (random.nextInt(3)) {
                0 -> ObstacleKind.BirdHigh
                1 -> ObstacleKind.BirdMid
                else -> ObstacleKind.BirdLow
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
        ObstacleKind.CactusSmall -> 18f to 35f
        ObstacleKind.CactusMedium -> 25f to 50f
        ObstacleKind.CactusLarge -> 30f to 70f
        ObstacleKind.CactusCluster2 -> 48f to 35f
        ObstacleKind.CactusCluster3 -> 72f to 40f
        ObstacleKind.BirdHigh, ObstacleKind.BirdMid, ObstacleKind.BirdLow -> 46f to 34f
    }

    private fun checkCollision(): Boolean {
        val inset = Constants.HITBOX_INSET
        val px = player.x + inset
        val py = player.y + inset
        val pw = player.width - inset * 2f
        val ph = player.height - inset * 2f
        for (o in obstacles) {
            val ox = o.x + inset * 0.5f
            val oy = o.y + inset * 0.5f
            val ow = o.width - inset
            val oh = o.height - inset
            if (px < ox + ow && px + pw > ox && py < oy + oh && py + ph > oy) {
                return true
            }
        }
        return false
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
