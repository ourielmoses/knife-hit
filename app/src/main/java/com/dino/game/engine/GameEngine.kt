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
    private var isNewRecord = false
    private var duckHeld = false
    private var worldWidth = 800f
    private var nextObstacleId = 1L
    private var nextCloudId = 1L
    private var nextParticleId = 1L
    private var lastMilestone = 0
    private var wasNight = false
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
            isNight = isNightMode(scoreFloat.toInt()),
            events = events,
        )
    }

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

    fun update(dt: Float): Int? {
        val clamped = dt.coerceIn(0f, 0.05f)
        when (screen) {
            ScreenState.Title -> {
                updateTitle(clamped)
                return null
            }
            ScreenState.GameOver -> {
                if (gameOverLock > 0f) gameOverLock = max(0f, gameOverLock - clamped)
                updateParticles(clamped)
                return null
            }
            ScreenState.Playing -> return updatePlaying(clamped)
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
            player = player.copy(dead = true, velocityY = 0f, ducking = false)
            screen = ScreenState.GameOver
            gameOverLock = Constants.GAME_OVER_INPUT_LOCK
            isNewRecord = score > highScore
            if (isNewRecord) highScore = score
            emit(GameEvent.Die)
            spawnDust(player.x + player.width * 0.5f, Constants.GROUND_Y, count = 10)
            return score
        }
        return null
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
        isNewRecord = false
        duckHeld = false
        groundOffset = 0f
        lastMilestone = 0
        wasNight = false
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
        ObstacleKind.CactusSmall -> 40f to 52f
        ObstacleKind.CactusMedium -> 52f to 72f
        ObstacleKind.CactusLarge -> 60f to 96f
        ObstacleKind.CactusCluster2 -> 96f to 58f
        ObstacleKind.CactusCluster3 -> 140f to 68f
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
