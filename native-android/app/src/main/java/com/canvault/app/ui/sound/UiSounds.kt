package com.canvault.app.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class UiSoundEffect(val playbackVolume: Float) {
    STANDARD(0.52f),
    NAVIGATION(0.48f),
    PRIMARY(0.56f),
    SCAN(0.54f),
    SHAKE(0.50f),
    COLOR(0.50f),
    ARCHIVE(0.48f),
    RESTORE(0.50f),
    SUCCESS(0.52f),
    DESTRUCTIVE(0.46f),
}

class CanVaultSoundController(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val sampleIds = Collections.synchronizedMap(EnumMap<UiSoundEffect, Int>(UiSoundEffect::class.java))
    private val loadedSampleIds = Collections.synchronizedSet(mutableSetOf<Int>())
    private val loader = Executors.newSingleThreadExecutor()
    @Volatile private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && !released) loadedSampleIds += sampleId
        }
        loader.execute(::preload)
    }

    fun play(effect: UiSoundEffect = UiSoundEffect.STANDARD) {
        if (released || !systemTouchSoundsEnabled()) return
        val sampleId = sampleIds[effect] ?: return
        if (sampleId !in loadedSampleIds) return
        soundPool.play(
            sampleId,
            effect.playbackVolume,
            effect.playbackVolume,
            1,
            0,
            1f,
        )
    }

    fun release() {
        released = true
        loader.shutdownNow()
        soundPool.release()
        sampleIds.clear()
        loadedSampleIds.clear()
    }

    private fun preload() {
        val soundDirectory = File(appContext.cacheDir, "ui-sounds-v1").apply { mkdirs() }
        UiSoundEffect.entries.forEach { effect ->
            if (released || Thread.currentThread().isInterrupted) return
            val file = File(soundDirectory, "${effect.name.lowercase()}.wav")
            val wav = CanVaultSoundSynthesis.wav(effect)
            if (!file.exists() || file.length() != wav.size.toLong()) file.writeBytes(wav)
            val sampleId = soundPool.load(file.absolutePath, 1)
            sampleIds[effect] = sampleId
        }
    }

    private fun systemTouchSoundsEnabled(): Boolean = runCatching {
        Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED,
            1,
        ) != 0
    }.getOrDefault(true)
}

val LocalCanVaultSounds = staticCompositionLocalOf<CanVaultSoundController> {
    error("CanVaultSoundController is not available")
}

@Composable
fun CanVaultSoundProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val controller = androidx.compose.runtime.remember(context.applicationContext) {
        CanVaultSoundController(context.applicationContext)
    }
    DisposableEffect(controller) {
        onDispose(controller::release)
    }
    CompositionLocalProvider(LocalCanVaultSounds provides controller, content = content)
}

@Composable
fun soundClick(
    effect: UiSoundEffect = UiSoundEffect.STANDARD,
    onClick: () -> Unit,
): () -> Unit {
    val sounds = LocalCanVaultSounds.current
    val currentClick = androidx.compose.runtime.rememberUpdatedState(onClick)
    return androidx.compose.runtime.remember(sounds, effect) {
        {
            sounds.play(effect)
            currentClick.value()
        }
    }
}

internal object CanVaultSoundSynthesis {
    const val sampleRate = 22_050

    fun render(effect: UiSoundEffect): ShortArray {
        val duration = when (effect) {
            UiSoundEffect.STANDARD -> 0.055
            UiSoundEffect.NAVIGATION -> 0.065
            UiSoundEffect.PRIMARY -> 0.095
            UiSoundEffect.SCAN -> 0.130
            UiSoundEffect.SHAKE -> 0.160
            UiSoundEffect.COLOR -> 0.145
            UiSoundEffect.ARCHIVE -> 0.090
            UiSoundEffect.RESTORE -> 0.105
            UiSoundEffect.SUCCESS -> 0.140
            UiSoundEffect.DESTRUCTIVE -> 0.080
        }
        val samples = ShortArray((sampleRate * duration).toInt())
        var noiseState = effect.ordinal * 1_103_515_245 + 12_345
        samples.indices.forEach { index ->
            noiseState = noiseState * 1_103_515_245 + 12_345
            val noise = ((noiseState ushr 16) and 0x7fff) / 16_383.5 - 1.0
            val time = index / sampleRate.toDouble()
            val progress = time / duration
            val attack = min(1.0, time / 0.0025)
            val signal = when (effect) {
                UiSoundEffect.STANDARD ->
                    0.105 * chirp(time, duration, 1_050.0, 720.0) * exp(-time * 52.0) +
                        0.030 * noise * exp(-time * 150.0)
                UiSoundEffect.NAVIGATION ->
                    0.075 * sin(2.0 * PI * 620.0 * time) * exp(-time * 38.0) +
                        0.045 * sin(2.0 * PI * 930.0 * time) * exp(-time * 48.0)
                UiSoundEffect.PRIMARY ->
                    0.115 * chirp(time, duration, 220.0, 105.0) * exp(-time * 24.0) +
                        0.055 * sin(2.0 * PI * 1_350.0 * time) * exp(-time * 52.0) +
                        0.020 * noise * exp(-time * 90.0)
                UiSoundEffect.SCAN -> scanSignal(time)
                UiSoundEffect.SHAKE -> shakeSignal(time, noise)
                UiSoundEffect.COLOR -> steppedNotes(time, doubleArrayOf(523.25, 659.25, 783.99), 0.043, 0.090)
                UiSoundEffect.ARCHIVE ->
                    0.100 * chirp(time, duration, 430.0, 175.0) * (1.0 - progress).pow(1.8)
                UiSoundEffect.RESTORE ->
                    0.095 * chirp(time, duration, 300.0, 720.0) * (1.0 - progress).pow(1.4) +
                        0.025 * noise * exp(-time * 110.0)
                UiSoundEffect.SUCCESS -> steppedNotes(time, doubleArrayOf(659.25, 987.77), 0.064, 0.095)
                UiSoundEffect.DESTRUCTIVE ->
                    0.085 * chirp(time, duration, 210.0, 120.0) * exp(-time * 32.0) +
                        0.018 * noise * exp(-time * 80.0)
            }
            samples[index] = (signal * attack).coerceIn(-0.22, 0.22).times(Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    fun wav(effect: UiSoundEffect): ByteArray {
        val samples = render(effect)
        val dataSize = samples.size * 2
        return ByteArray(44 + dataSize).also { bytes ->
            putAscii(bytes, 0, "RIFF")
            putIntLe(bytes, 4, 36 + dataSize)
            putAscii(bytes, 8, "WAVE")
            putAscii(bytes, 12, "fmt ")
            putIntLe(bytes, 16, 16)
            putShortLe(bytes, 20, 1)
            putShortLe(bytes, 22, 1)
            putIntLe(bytes, 24, sampleRate)
            putIntLe(bytes, 28, sampleRate * 2)
            putShortLe(bytes, 32, 2)
            putShortLe(bytes, 34, 16)
            putAscii(bytes, 36, "data")
            putIntLe(bytes, 40, dataSize)
            samples.forEachIndexed { index, sample -> putShortLe(bytes, 44 + index * 2, sample.toInt()) }
        }
    }

    private fun scanSignal(time: Double): Double = when {
        time < 0.055 -> 0.095 * chirp(time, 0.055, 690.0, 1_280.0) * sin(PI * time / 0.055)
        time in 0.064..0.124 -> {
            val local = time - 0.064
            0.090 * chirp(local, 0.060, 920.0, 1_720.0) * sin(PI * local / 0.060)
        }
        else -> 0.0
    }

    private fun shakeSignal(time: Double, noise: Double): Double {
        val starts = doubleArrayOf(0.0, 0.052, 0.104)
        var local: Double? = null
        for (start in starts) {
            val offset = time - start
            if (offset in 0.0..0.038) {
                local = offset
                break
            }
        }
        val segmentTime = local ?: return 0.0
        val envelope = sin(PI * segmentTime / 0.038).coerceAtLeast(0.0).pow(1.4)
        return noise * 0.095 * envelope +
            sin(2.0 * PI * 2_250.0 * time) * 0.032 * envelope +
            sin(2.0 * PI * 390.0 * time) * 0.025 * envelope
    }

    private fun steppedNotes(time: Double, notes: DoubleArray, step: Double, amplitude: Double): Double {
        val index = (time / step).toInt()
        if (index !in notes.indices) return 0.0
        val local = time - index * step
        val envelope = sin(PI * (local / step).coerceIn(0.0, 1.0)).pow(1.35)
        return amplitude * sin(2.0 * PI * notes[index] * time) * envelope
    }

    private fun chirp(time: Double, duration: Double, startHz: Double, endHz: Double): Double {
        val slope = (endHz - startHz) / duration
        val phase = 2.0 * PI * (startHz * time + 0.5 * slope * time * time)
        return sin(phase)
    }

    private fun putAscii(target: ByteArray, offset: Int, value: String) {
        value.forEachIndexed { index, char -> target[offset + index] = char.code.toByte() }
    }

    private fun putIntLe(target: ByteArray, offset: Int, value: Int) {
        repeat(4) { byte -> target[offset + byte] = (value ushr (byte * 8)).toByte() }
    }

    private fun putShortLe(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }
}
