package com.knifehit.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick(enabled: Boolean) {
        if (!enabled) return
        pulse(18, 40)
    }

    fun fail(enabled: Boolean) {
        if (!enabled) return
        pulse(40, 80)
    }

    fun boss(enabled: Boolean) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 50), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(120)
        }
    }

    private fun pulse(ms: Long, amp: Int) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(ms)
        }
    }
}
