package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.HapticEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class HapticsManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastTriggeredMs = ConcurrentHashMap<HapticEvent, Long>()

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val hasHardware: Boolean = runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)
    private val supportsAmplitude: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { vibrator?.hasAmplitudeControl() == true }.getOrDefault(false)
    } else {
        false
    }

    fun trigger(event: HapticEvent) {
        if (!enabled || !hasHardware) return
        val vib = vibrator ?: return

        val now = System.currentTimeMillis()
        val cooldown = when (event) {
            HapticEvent.FOOD_ORB -> 75L
            HapticEvent.FOOD_LOOT -> 95L
            HapticEvent.BOOST_START -> 240L
            HapticEvent.BOOST_PULSE -> 340L
            HapticEvent.POWERUP_PICKUP -> 160L
            HapticEvent.ELIMINATION_KILL -> 160L
            HapticEvent.PLAYER_CRASH -> 300L
        }
        val prev = lastTriggeredMs[event] ?: 0L
        if (now - prev < cooldown) return
        lastTriggeredMs[event] = now

        scope.launch {
            runCatching { vibrateForEvent(vib, event) }
        }
    }

    private fun vibrateForEvent(vib: Vibrator, event: HapticEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (event) {
                HapticEvent.FOOD_ORB -> oneShot(durationMs = 14L, amplitude = 42)
                HapticEvent.FOOD_LOOT -> oneShot(durationMs = 24L, amplitude = 96)
                HapticEvent.BOOST_START -> oneShot(durationMs = 28L, amplitude = 115)
                HapticEvent.BOOST_PULSE -> oneShot(durationMs = 12L, amplitude = 32)
                HapticEvent.POWERUP_PICKUP -> waveform(
                    timings = longArrayOf(0L, 22L, 38L, 34L),
                    amplitudes = intArrayOf(0, 110, 0, 165)
                )
                HapticEvent.ELIMINATION_KILL -> oneShot(durationMs = 42L, amplitude = 185)
                HapticEvent.PLAYER_CRASH -> waveform(
                    timings = longArrayOf(0L, 50L, 40L, 80L),
                    amplitudes = intArrayOf(0, 210, 0, 140)
                )
            }
            vib.vibrate(effect)
        } else {
            val ms = when (event) {
                HapticEvent.FOOD_ORB -> 12L
                HapticEvent.FOOD_LOOT -> 22L
                HapticEvent.BOOST_START -> 26L
                HapticEvent.BOOST_PULSE -> 10L
                HapticEvent.POWERUP_PICKUP -> 45L
                HapticEvent.ELIMINATION_KILL -> 42L
                HapticEvent.PLAYER_CRASH -> 90L
            }
            @Suppress("DEPRECATION")
            vib.vibrate(ms)
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        val safeAmp = if (supportsAmplitude) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(durationMs, safeAmp)
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect {
        return if (supportsAmplitude) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
    }

    fun release() {
        scope.cancel()
    }
}
