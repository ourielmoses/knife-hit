package com.knifehit.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.model.WorldDef
import com.knifehit.game.render.toComposeColor

@Composable
fun WorldSelectOverlay(
    worlds: List<WorldDef>,
    unlocked: Set<Int>,
    onBack: () -> Unit,
    onStart: (Int) -> Unit,
) {
    val s = LocalStrings.current
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NeonButton(s.back, onBack)
            Spacer(Modifier.height(8.dp))
            Text(s.worlds, color = NeonCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(worlds, key = { it.id }) { world ->
                    val locked = world.id !in unlocked
                    val prevName = worlds.firstOrNull { it.id == world.id - 1 }?.name ?: ""
                    Column(
                        Modifier
                            .graphicsLayer { alpha = if (locked) 0.45f else 1f }
                            .then(
                                if (locked) Modifier.drawWithContent {
                                    val paint = Paint().apply {
                                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                    }
                                    drawIntoCanvas { canvas ->
                                        canvas.saveLayer(size.toRect(), paint)
                                        drawContent()
                                        canvas.restore()
                                    }
                                } else Modifier,
                            )
                            .background(world.theme.bgBottomArgb.toComposeColor(), RoundedCornerShape(14.dp))
                            .border(1.dp, world.theme.accentArgb.toComposeColor(), RoundedCornerShape(14.dp))
                            .clickable(enabled = !locked) { onStart(world.id) }
                            .padding(12.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(world.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(world.boss.name, color = world.theme.glowArgb.toComposeColor(), fontSize = 12.sp)
                        if (locked) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                s.unlockHint.replace("%1\$s", prevName),
                                color = Color(0xFFCFD8E0),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
