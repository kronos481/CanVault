package com.canvault.app.ui.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.absoluteValue

class UiSoundsTest {
    @Test
    fun everySoundIsShortAudibleAndQuiet() {
        UiSoundEffect.entries.forEach { effect ->
            val samples = CanVaultSoundSynthesis.render(effect)
            val durationMillis = samples.size * 1_000.0 / CanVaultSoundSynthesis.sampleRate
            val peak = samples.maxOf { it.toInt().absoluteValue }

            assertTrue("$effect is too short", durationMillis >= 50.0)
            assertTrue("$effect is too long", durationMillis <= 165.0)
            assertTrue("$effect is inaudible", peak >= 2_000)
            assertTrue("$effect is too loud", peak <= (Short.MAX_VALUE * 0.70).toInt() + 1)
        }
    }

    @Test
    fun generatedFilesAreValidMonoPcmWav() {
        UiSoundEffect.entries.forEach { effect ->
            val samples = CanVaultSoundSynthesis.render(effect)
            val wav = CanVaultSoundSynthesis.wav(effect)

            assertEquals("RIFF", wav.copyOfRange(0, 4).toString(Charsets.US_ASCII))
            assertEquals("WAVE", wav.copyOfRange(8, 12).toString(Charsets.US_ASCII))
            assertEquals("data", wav.copyOfRange(36, 40).toString(Charsets.US_ASCII))
            assertEquals(44 + samples.size * 2, wav.size)
        }
    }
}
