package com.knifehit.game.model

data class Position(
    val world: Int = 1,
    val stageInWorld: Int = 1,
) {
    fun isBoss(): Boolean = stageInWorld == stagesIn(world)

    fun next(): Position? {
        val max = stagesIn(world)
        return if (stageInWorld < max) {
            copy(stageInWorld = stageInWorld + 1)
        } else {
            val n = world + 1
            if (n > worldCount()) null else Position(n, 1)
        }
    }

    fun worldStart(): Position = copy(stageInWorld = 1)
}

fun compareProgress(a: Position, b: Position): Int {
    val worldCmp = a.world.compareTo(b.world)
    return if (worldCmp != 0) worldCmp else a.stageInWorld.compareTo(b.stageInWorld)
}

fun maxPosition(a: Position, b: Position): Position =
    if (compareProgress(a, b) >= 0) a else b
