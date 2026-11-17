package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.SoundEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEffectsManager {
    companion object {
        private const val SAMPLE_RATE = 22_050
        private const val MAX_CONCURRENT_TRACKS = 6
    }

    @Volatile
    var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastPlayedMs = ConcurrentHashMap<SoundEffect, Long>()
    private val activeTracks = AtomicInteger(0)

    private val pcmCache: Map<SoundEffect, ShortArray> by lazy {
        mapOf(
            SoundEffect.EAT_ORB to synthesizeSweep(
                durationMs = 60,
                startFreq = 540.0,
                endFreq = 920.0,
                amplitude = 0.28,
                harmonicMix = 0.20
            ),
            SoundEffect.EAT_LOOT to synthesizeArpeggio(
                notesHz = listOf(740.0, 1108.73),
                noteDurationMs = 55,
                amplitude = 0.32
            ),
            SoundEffect.BOOST_START to synthesizeSweep(
                durationMs = 115,
                startFreq = 220.0,
                endFreq = 520.0,
                amplitude = 0.26,
                harmonicMix = 0.35
            ),
            SoundEffect.POWERUP_SPEED to synthesizeArpeggio(
                notesHz = listOf(523.25, 659.25, 783.99, 1046.50),
                noteDurationMs = 60,
                amplitude = 0.34
            ),
            SoundEffect.POWERUP_MAGNET to synthesizeArpeggio(
                notesHz = listOf(440.0, 554.37, 659.25, 880.0),
                noteDurationMs = 65,
                amplitude = 0.34
            ),
            SoundEffect.ELIMINATION_KILL to synthesizeArpeggio(
                notesHz = listOf(392.0, 587.33, 783.99),
                noteDurationMs = 78,
                amplitude = 0.38
            ),
            SoundEffect.PLAYER_CRASH to synthesizeCrash(durationMs = 360, amplitude = 0.40),
            SoundEffect.UI_CLICK to synthesizeSweep(
                durationMs = 28,
                startFreq = 820.0,
                endFreq = 540.0,
                amplitude = 0.22,
                harmonicMix = 0.10
            ),
            SoundEffect.REWARD_CLAIM to synthesizeArpeggio(
                notesHz = listOf(523.25, 659.25, 783.99, 1046.50),
                noteDurationMs = 92,
                amplitude = 0.36
            )
        )
    }

    fun play(effect: SoundEffect) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        val cooldown = when (effect) {
            SoundEffect.EAT_ORB -> 65L
            SoundEffect.EAT_LOOT -> 80L
            SoundEffect.BOOST_START -> 260L
            SoundEffect.UI_CLICK -> 40L
            else -> 90L
        }
        val prev = lastPlayedMs[effect] ?: 0L
        if (now - prev < cooldown) return
        lastPlayedMs[effect] = now

        if (activeTracks.get() >= MAX_CONCURRENT_TRACKS) return

        val samples = pcmCache[effect] ?: return
        scope.launch {
            playPcm(samples)
        }
    }

    private suspend fun playPcm(samples: ShortArray) {
        if (activeTracks.incrementAndGet() > MAX_CONCURRENT_TRACKS) {
            activeTracks.decrementAndGet()
            return
        }
        var track: AudioTrack? = null
        try {
            val byteCount = samples.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()

            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            delay(durationMs + 45L)
        } catch (_: Throwable) {
            // Safely ignore on headless test environments without audio hardware
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
            activeTracks.decrementAndGet()
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun synthesizeSweep(
        durationMs: Int,
        startFreq: Double,
        endFreq: Double,
        amplitude: Double,
        harmonicMix: Double
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val out = ShortArray(totalSamples)
        var phase = 0.0
        val attackSamples = (SAMPLE_RATE * 0.004).coerceAtLeast(1.0)
        for (i in 0 until totalSamples) {
            val tNorm = i.toDouble() / totalSamples.toDouble()
            val freq = startFreq + (endFreq - startFreq) * tNorm
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val wave = sin(phase) + harmonicMix * sin(phase * 2.0)
            val attack = (i / attackSamples).coerceAtMost(1.0)
            val decay = exp(-3.6 * tNorm)
            val sample = (wave / (1.0 + harmonicMix)) * amplitude * attack * decay
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthesizeArpeggio(
        notesHz: List<Double>,
        noteDurationMs: Int,
        amplitude: Double
    ): ShortArray {
        val noteSamples = (SAMPLE_RATE * noteDurationMs) / 1000
        val totalSamples = noteSamples * notesHz.size
        val out = ShortArray(totalSamples)
        val attackSamples = (SAMPLE_RATE * 0.004).coerceAtLeast(1.0)
        for ((idx, freq) in notesHz.withIndex()) {
            var phase = 0.0
            val offset = idx * noteSamples
            for (j in 0 until noteSamples) {
                val localNorm = j.toDouble() / noteSamples.toDouble()
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val wave = 0.80 * sin(phase) + 0.20 * sin(phase * 2.0)
                val attack = (j / attackSamples).coerceAtMost(1.0)
                val decay = exp(-2.8 * localNorm)
                val sample = wave * amplitude * attack * decay
                out[offset + j] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private fun synthesizeCrash(durationMs: Int, amplitude: Double): ShortArray {
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val out = ShortArray(totalSamples)
        val rng = Random(42)
        var phase = 0.0
        var lowPassNoise = 0.0
        for (i in 0 until totalSamples) {
            val tNorm = i.toDouble() / totalSamples.toDouble()
            val freq = 320.0 * exp(-2.4 * tNorm) + 55.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val white = rng.nextDouble() * 2.0 - 1.0
            lowPassNoise = lowPassNoise * 0.82 + white * 0.18
            val tone = sin(phase)
            val decay = exp(-3.8 * tNorm)
            val sample = (0.65 * tone + 0.35 * lowPassNoise) * amplitude * decay
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
