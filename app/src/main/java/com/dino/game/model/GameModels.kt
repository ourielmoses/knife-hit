package com.dino.game.model

enum class ScreenState {
    Title,
    Playing,
    GameOver,
}

enum class ObstacleKind {
    CactusSmall,
    CactusMedium,
    CactusLarge,
    CactusCluster2,
    CactusCluster3,
    BirdHigh,
    BirdMid,
    BirdLow,
}

data class PlayerState(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val velocityY: Float,
    val onGround: Boolean,
    val ducking: Boolean,
    val dead: Boolean,
    val runFrame: Int,
)

data class ObstacleState(
    val id: Long,
    val kind: ObstacleKind,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val frame: Int = 0,
)

data class CloudState(
    val id: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
)

data class GameSnapshot(
    val screen: ScreenState,
    val player: PlayerState,
    val obstacles: List<ObstacleState>,
    val clouds: List<CloudState>,
    val groundOffset: Float,
    val score: Int,
    val highScore: Int,
    val isNewRecord: Boolean,
    val speed: Float,
    val gameOverLockRemaining: Float,
)
