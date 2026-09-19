package com.dino.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dino.game.audio.Haptics
import com.dino.game.audio.SfxPlayer
import com.dino.game.data.SettingsStore
import com.dino.game.engine.GameEngine
import com.dino.game.model.Constants
import com.dino.game.model.GameEvent
import com.dino.game.model.ScreenState
import com.dino.game.model.SkinId
import com.dino.game.render.GameWorld
import com.dino.game.render.SkinSpriteLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUi()
        setContent {
            DinoApp()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
fun DinoApp() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    val sfx = remember { SfxPlayer() }
    val haptics = remember { Haptics(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val savedHigh by store.highScore.collectAsStateWithLifecycle(initialValue = 0)
    val muted by store.muted.collectAsStateWithLifecycle(initialValue = false)
    val selectedSkin by store.selectedSkin.collectAsStateWithLifecycle(initialValue = SkinId.Cute)

    val engine = remember { GameEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var lastSavedScore by remember { mutableIntStateOf(-1) }
    var showSkinPicker by remember { mutableStateOf(false) }

    LaunchedEffect(muted) {
        sfx.muted = muted
    }

    LaunchedEffect(savedHigh) {
        engine.setHighScore(maxOf(engine.snapshot().highScore, savedHigh))
        snapshot = engine.snapshot()
    }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    engine.update(dt)
                    val snap = engine.snapshot()
                    for (event in snap.events) {
                        when (event) {
                            GameEvent.Jump -> sfx.playJump()
                            GameEvent.Land -> {
                                sfx.playLand()
                                haptics.tickLand()
                            }
                            GameEvent.Die -> {
                                sfx.playDie()
                                haptics.hit()
                            }
                            GameEvent.Milestone -> sfx.playMilestone()
                            GameEvent.NightChanged -> sfx.playNight()
                        }
                    }
                    snapshot = snap
                }
                lastNanos = now
            }
        }
    }

    LaunchedEffect(snapshot.screen, snapshot.score, snapshot.isNewRecord) {
        if (snapshot.screen == ScreenState.GameOver &&
            snapshot.isNewRecord &&
            snapshot.score != lastSavedScore
        ) {
            lastSavedScore = snapshot.score
            withContext(Dispatchers.IO) {
                store.saveIfBest(snapshot.score)
            }
        }
        if (snapshot.screen == ScreenState.Playing) {
            lastSavedScore = -1
            showSkinPicker = false
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val latestEngine = rememberUpdatedState(engine)
    val t = snapshot.nightBlend.coerceIn(0f, 1f)
    val daySky = Color(0xFFF7F7F7)
    val nightSky = Color(0xFF1B1B1B)
    val dayInk = Color(0xFF535353)
    val nightInk = Color(0xFFD7D7D7)
    val sky = Color(
        red = daySky.red + (nightSky.red - daySky.red) * t,
        green = daySky.green + (nightSky.green - daySky.green) * t,
        blue = daySky.blue + (nightSky.blue - daySky.blue) * t,
    )
    val ink = Color(
        red = dayInk.red + (nightInk.red - dayInk.red) * t,
        green = dayInk.green + (nightInk.green - dayInk.green) * t,
        blue = dayInk.blue + (nightInk.blue - dayInk.blue) * t,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sky)
            .onSizeChanged { size ->
                if (size.height > 0) {
                    val worldW = size.width.toFloat() /
                        (size.height.toFloat() / Constants.WORLD_HEIGHT)
                    latestEngine.value.setWorldWidth(worldW)
                }
            }
            .pointerInput(snapshot.screen) {
                // Jump/duck only while actively playing or retrying after game over.
                if (snapshot.screen == ScreenState.Title ||
                    snapshot.screen == ScreenState.Paused
                ) {
                    return@pointerInput
                }
                awaitPointerEventScope {
                    val duckPointers = mutableSetOf<Long>()
                    while (true) {
                        val event = awaitPointerEvent()
                        val mid = size.width / 2f
                        var jumpPressed = false
                        val screenNow = latestEngine.value.snapshot().screen
                        if (screenNow == ScreenState.Paused || screenNow == ScreenState.Title) {
                            duckPointers.clear()
                            continue
                        }

                        for (change in event.changes) {
                            val onLeft = change.position.x < mid
                            if (onLeft) {
                                if (change.pressed) {
                                    duckPointers.add(change.id.value)
                                } else {
                                    duckPointers.remove(change.id.value)
                                }
                            } else if (change.pressed && !change.previousPressed) {
                                jumpPressed = true
                            }
                            change.consume()
                        }

                        if (screenNow == ScreenState.Playing) {
                            latestEngine.value.onDuckChanged(duckPointers.isNotEmpty())
                        }
                        if (jumpPressed) {
                            latestEngine.value.onJumpPress()
                        }
                        snapshot = latestEngine.value.snapshot()
                    }
                }
            },
    ) {
        GameWorld(
            snapshot = snapshot,
            textMeasurer = textMeasurer,
            skin = selectedSkin,
            modifier = Modifier.fillMaxSize(),
        )

        // Sound toggle: title menu only.
        if (snapshot.screen == ScreenState.Title) {
            Text(
                text = if (muted) "SOUND OFF" else "SOUND ON",
                color = ink.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clickable {
                        scope.launch {
                            store.setMuted(!muted)
                        }
                    },
            )
        }

        // Pause button: playing only (hidden while pause menu is open).
        if (snapshot.screen == ScreenState.Playing) {
            Text(
                text = "PAUSE",
                color = ink.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 16.dp)
                    .clickable {
                        latestEngine.value.pause()
                        snapshot = latestEngine.value.snapshot()
                    },
            )
        }

        when (snapshot.screen) {
            ScreenState.Title -> {
                if (showSkinPicker) {
                    SkinPicker(
                        selected = selectedSkin,
                        ink = ink,
                        sky = sky,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        onSelect = { skin ->
                            scope.launch { store.setSkin(skin) }
                        },
                        onClose = { showSkinPicker = false },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(48.dp))
                        MenuButton(
                            label = "PLAY",
                            ink = ink,
                            fontSize = 22,
                            onClick = {
                                latestEngine.value.onPlayPress()
                                snapshot = latestEngine.value.snapshot()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        MenuButton(
                            label = "SKINS",
                            ink = ink,
                            fontSize = 16,
                            onClick = { showSkinPicker = true },
                        )
                    }
                }
            }
            ScreenState.Paused -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "PAUSED",
                        color = ink,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(20.dp))
                    MenuButton(
                        label = "BACK",
                        ink = ink,
                        fontSize = 18,
                        onClick = {
                            latestEngine.value.resume()
                            snapshot = latestEngine.value.snapshot()
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    MenuButton(
                        label = "HOME",
                        ink = ink,
                        fontSize = 18,
                        onClick = {
                            showSkinPicker = false
                            latestEngine.value.goHome()
                            snapshot = latestEngine.value.snapshot()
                        },
                    )
                }
            }
            ScreenState.Playing, ScreenState.GameOver -> Unit
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    ink: Color,
    fontSize: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .border(2.dp, ink, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = ink,
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SkinPicker(
    selected: SkinId,
    ink: Color,
    sky: Color,
    modifier: Modifier = Modifier,
    onSelect: (SkinId) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val previews = remember(context.resources) {
        SkinId.entries.associateWith { skin ->
            SkinSpriteLoader.load(context.resources, skin).stand
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SKINS",
            color = ink,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        val rows = SkinId.entries.chunked(4)
        rows.forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                row.forEach { skin ->
                    SkinOption(
                        skin = skin,
                        preview = previews.getValue(skin),
                        selected = skin == selected,
                        ink = ink,
                        sky = sky,
                        onClick = { onSelect(skin) },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        MenuButton(
            label = "BACK",
            ink = ink,
            fontSize = 14,
            onClick = onClose,
        )
    }
}

@Composable
private fun SkinOption(
    skin: SkinId,
    preview: ImageBitmap,
    selected: Boolean,
    ink: Color,
    sky: Color,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) ink else ink.copy(alpha = 0.35f)
    val borderWidth = if (selected) 3.dp else 1.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .background(sky)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Image(
            painter = BitmapPainter(
                image = preview,
                filterQuality = FilterQuality.None,
            ),
            contentDescription = skin.label,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = skin.label,
            color = ink,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
