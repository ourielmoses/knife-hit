package com.dino.game.render

import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.dino.game.R
import com.dino.game.model.SkinId

data class SkinSprites(
    val stand: ImageBitmap,
    val run2: ImageBitmap,
    val jump: ImageBitmap,
    val duck: ImageBitmap,
    val dead: ImageBitmap,
)

object SkinSpriteLoader {
    fun load(res: Resources, skin: SkinId): SkinSprites {
        val (stand, run2, jump, duck, dead) = when (skin) {
            SkinId.Classic -> listOf(
                R.drawable.skin_classic_stand,
                R.drawable.skin_classic_run2,
                R.drawable.skin_classic_jump,
                R.drawable.skin_classic_duck,
                R.drawable.skin_classic_dead,
            )
            SkinId.Arcade -> listOf(
                R.drawable.skin_arcade_stand,
                R.drawable.skin_arcade_run2,
                R.drawable.skin_arcade_jump,
                R.drawable.skin_arcade_duck,
                R.drawable.skin_arcade_dead,
            )
            SkinId.Outline -> listOf(
                R.drawable.skin_outline_stand,
                R.drawable.skin_outline_run2,
                R.drawable.skin_outline_jump,
                R.drawable.skin_outline_duck,
                R.drawable.skin_outline_dead,
            )
            SkinId.Spiky -> listOf(
                R.drawable.skin_spiky_stand,
                R.drawable.skin_spiky_run2,
                R.drawable.skin_spiky_jump,
                R.drawable.skin_spiky_duck,
                R.drawable.skin_spiky_dead,
            )
            SkinId.Cute -> listOf(
                R.drawable.skin_cute_stand,
                R.drawable.skin_cute_run2,
                R.drawable.skin_cute_jump,
                R.drawable.skin_cute_duck,
                R.drawable.skin_cute_dead,
            )
            SkinId.Robot -> listOf(
                R.drawable.skin_robot_stand,
                R.drawable.skin_robot_run2,
                R.drawable.skin_robot_jump,
                R.drawable.skin_robot_duck,
                R.drawable.skin_robot_dead,
            )
            SkinId.Ghost -> listOf(
                R.drawable.skin_ghost_stand,
                R.drawable.skin_ghost_run2,
                R.drawable.skin_ghost_jump,
                R.drawable.skin_ghost_duck,
                R.drawable.skin_ghost_dead,
            )
            SkinId.Knight -> listOf(
                R.drawable.skin_knight_stand,
                R.drawable.skin_knight_run2,
                R.drawable.skin_knight_jump,
                R.drawable.skin_knight_duck,
                R.drawable.skin_knight_dead,
            )
        }
        return SkinSprites(
            stand = ImageBitmap.imageResource(res, stand),
            run2 = ImageBitmap.imageResource(res, run2),
            jump = ImageBitmap.imageResource(res, jump),
            duck = ImageBitmap.imageResource(res, duck),
            dead = ImageBitmap.imageResource(res, dead),
        )
    }
}
