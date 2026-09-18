package com.dino.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dino.game.data.HighScoreStore
import com.dino.game.engine.GameEngine
import com.dino.game.model.Constants
import com.dino.game.model.ScreenState
import com.dino.game.render.GameWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val store = remember { HighScoreStore(context.applicationContext) }
    val savedHigh by store.highScore.collectAsStateWithLifecycle(initialValue = 0)
    val engine = remember { GameEngine() }
    var snapshot by remember { mutableStateOf(engine.snapshot()) }
    var lastSavedScore by remember { mutableIntStateOf(-1) }

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
                    snapshot = engine.snapshot()
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
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val latestEngine = rememberUpdatedState(engine)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .onSizeChanged { size ->
                if (size.height > 0) {
                    val worldW = size.width.toFloat() /
                        (size.height.toFloat() / Constants.WORLD_HEIGHT)
                    latestEngine.value.setWorldWidth(worldW)
                }
            }
            .pointerInput(Unit) {
                // Left half = duck (hold). Right half = jump (press).
                awaitPointerEventScope {
                    val duckPointers = mutableSetOf<Long>()
                    while (true) {
                        val event = awaitPointerEvent()
                        val width = size.width
                        val mid = width / 2f
                        var jumpPressed = false

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

                        latestEngine.value.onDuckChanged(duckPointers.isNotEmpty())
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
            modifier = Modifier.fillMaxSize(),
        )
    }
}
