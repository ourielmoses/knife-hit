package com.knifehit.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.knifehit.game.render.toComposeColor
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.knifehit.game.audio.AudioEngine
import com.knifehit.game.audio.Haptics
import com.knifehit.game.data.ContentRepository
import com.knifehit.game.data.SaveRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var session: KnifeHitSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        session = KnifeHitSession(
            save = SaveRepository(this),
            content = ContentRepository(this),
            audio = AudioEngine(this),
            haptics = Haptics(this),
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = 0xFF00F5FF.toComposeColor(),
                    background = 0xFF070B14.toComposeColor(),
                    surface = 0xFF10141C.toComposeColor(),
                ),
            ) {
                KnifeHitApp(session)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::session.isInitialized) {
            session.onBackground()
            lifecycleScope.launch { session.save.save(session.profile) }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::session.isInitialized) session.onForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::session.isInitialized) session.audio.release()
    }
}
