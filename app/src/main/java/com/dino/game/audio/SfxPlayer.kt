package com.dino.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny procedural SFX (no asset files). Mute is controlled by the caller.
 */
class SfxPlayer {
    @Volatile
    var muted: Boolean = false

    fun playJump() = playTone(880.0, 0.06, volume = 0.28)
    fun playLand() = playTone(220.0, 0.04, volume = 0.18)
    fun playDie() = playTone(120.0, 0.22, volume = 0.35, sweepDown = true)
    fun playMilestone() = playTone(660.0, 0.08, volume = 0.22)
    fun playNight() = playTone(440.0, 0.05, volume = 0.15)

    private fun playTone(
        frequencyHz: Double,
        durationSec: Double,
        volume: Double,
        sweepDown: Boolean = false,
    ) {
        if (muted) return
        Thread {
            try {
                val sampleRate = 22050
                val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = if (sweepDown) {
                        frequencyHz * (1.0 - 0.7 * (i.toDouble() / count))
                    } else {
                        frequencyHz
                    }
                    val envelope = sin(PI * (i.toDouble() / count)).coerceIn(0.0, 1.0)
                    val sample = sin(2.0 * PI * freq * t) * volume * envelope
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((durationSec * 1000).toLong() + 30)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio device issues
            }
        }.start()
    }
}
