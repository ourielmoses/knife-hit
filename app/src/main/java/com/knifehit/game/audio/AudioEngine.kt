package com.knifehit.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.knifehit.game.model.TargetStyle
import com.knifehit.game.model.Timbre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

class AudioEngine(context: Context) {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)
    private val voices = Array(8) { Voice() }
    private var cursor = 0
    private var ambientTrack: AudioTrack? = null
    private var ambientJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null
    var sfxGain = 0.81f
    var ambientGain = 0.49f
    var ducked = false
        private set
    private var muted = false
    private var worldId = 1

    init {
        requestFocus()
    }

    fun sliderToGain(pct: Float): Float = (pct.coerceIn(0f, 1f)).let { it * it }

    fun setVolumes(ambientPct: Float, sfxPct: Float) {
        ambientGain = sliderToGain(ambientPct)
        sfxGain = sliderToGain(sfxPct)
        applyAmbientVolume()
    }

    fun duck(on: Boolean) {
        ducked = on
        applyAmbientVolume()
    }

    fun pauseAll() {
        muted = true
        voices.forEach { it.pause() }
        ambientTrack?.pause()
    }

    fun resumeAll() {
        muted = false
        requestFocus()
        applyAmbientVolume()
        runCatching { ambientTrack?.play() }
    }

    fun release() {
        ambientJob?.cancel()
        voices.forEach { it.release() }
        ambientTrack?.release()
        ambientTrack = null
        abandonFocus()
    }

    fun playThrow(timbre: Timbre, throwSpeed: Float) {
        val pitch = 220f + throwSpeed * 160f
        val decay = (0.12f / throwSpeed).coerceIn(0.06f, 0.16f)
        play(renderTone(timbre, pitch, decay, 0.9f))
    }

    fun playImpact(style: TargetStyle) {
        val (hz, decay, kind) = when (style) {
            TargetStyle.WOOD -> Triple(140f, 0.09f, Timbre.HEAVY)
            TargetStyle.METAL -> Triple(620f, 0.22f, Timbre.BLADE)
            TargetStyle.WEDGES -> Triple(180f, 0.07f, Timbre.HEAVY)
            TargetStyle.STONE -> Triple(110f, 0.12f, Timbre.HEAVY)
            TargetStyle.SECTORS -> Triple(480f, 0.16f, Timbre.LASER)
        }
        play(renderTone(kind, hz, decay, 1f))
    }

    fun playFail() {
        play(renderTone(Timbre.HEAVY, 90f, 0.28f, 1f))
    }

    fun playPickup() {
        play(renderTone(Timbre.LASER, 880f, 0.08f, 0.6f))
    }

    fun playClear() {
        play(renderTone(Timbre.ARCANE, 520f, 0.24f, 0.85f))
    }

    fun startAmbient(world: Int) {
        worldId = world
        ambientJob?.cancel()
        ambientTrack?.release()
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val attrs = attributes()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        ambientTrack = track
        applyAmbientVolume()
        track.play()
        ambientJob = scope.launch {
            val buf = ShortArray(1024)
            var t = 0
            val base = 40f + world * 4f
            while (isActive) {
                for (i in buf.indices) {
                    val sec = (t + i) / SAMPLE_RATE.toFloat()
                    val pad = sin(2.0 * PI * base * sec) * 0.18 +
                        sin(2.0 * PI * (base * 1.5) * sec) * 0.08 +
                        sin(2.0 * PI * (base * 0.5 + 0.2 * world) * sec) * 0.06
                    val n = ((sec * 8.0).toInt() % 11 - 5) * 0.002
                    buf[i] = ( ((pad + n) * Short.MAX_VALUE * 0.35).toInt() )
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                t += buf.size
                track.write(buf, 0, buf.size)
            }
        }
    }

    private fun applyAmbientVolume() {
        val g = if (muted) 0f else ambientGain * if (ducked) 0.15f else 1f
        ambientTrack?.setVolume(g)
    }

    private fun play(pcm: ShortArray) {
        if (muted) return
        val v = voices[cursor]
        cursor = (cursor + 1) % voices.size
        v.play(pcm, sfxGain * if (ducked) 0.2f else 1f)
    }

    private fun attributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= 26) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes())
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    private inner class Voice {
        private var track: AudioTrack? = null

        fun play(pcm: ShortArray, gain: Float) {
            runCatching { track?.release() }
            val attrs = attributes()
            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val builder = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            val t = builder.build()
            t.write(pcm, 0, pcm.size)
            t.setVolume(gain.coerceIn(0f, 1f))
            t.play()
            track = t
        }

        fun pause() {
            runCatching { track?.pause() }
        }

        fun release() {
            runCatching { track?.release() }
            track = null
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050

        fun renderTone(timbre: Timbre, hz: Float, decaySec: Float, amp: Float): ShortArray {
            val n = (SAMPLE_RATE * decaySec).toInt().coerceAtLeast(64)
            val out = ShortArray(n)
            for (i in 0 until n) {
                val t = i / SAMPLE_RATE.toFloat()
                val env = exp(-t / (decaySec * 0.35f)).toFloat()
                val phase = (2.0 * PI * hz * t).toFloat()
                val wave = when (timbre) {
                    Timbre.HEAVY -> sin(phase) * 0.7f + (if (i % 17 == 0) 0.2f else 0f)
                    Timbre.BLADE -> sin(phase) * 0.4f + sin(phase * 2.03f) * 0.3f + sin(phase * 3.1f) * 0.15f
                    Timbre.LASER -> {
                        val sweep = hz * (1f - t / decaySec * 0.6f)
                        sin((2.0 * PI * sweep * t).toFloat()).toFloat() *
                            if ((t * sweep).toInt() % 2 == 0) 0.8f else -0.4f
                    }
                    Timbre.ARCANE -> sin(phase) * 0.35f + sin(phase * 1.5f) * 0.25f + sin(phase * 4.2f) * 0.12f
                }
                val s = (wave * env * amp * Short.MAX_VALUE * 0.55f).toInt()
                out[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }
    }
}
