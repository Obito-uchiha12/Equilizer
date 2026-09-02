package com.example

import com.example.audio.TestToneGenerator
import com.example.audio.TestToneMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TestToneGeneratorTest {

    @Test
    fun `initial state of tone generator is stopped`() {
        val generator = TestToneGenerator()
        val state = generator.state.value

        assertFalse(state.isPlaying)
        assertEquals(TestToneMode.MID_910HZ, state.activeMode)
        assertEquals(0.5f, state.volume, 0.001f)
    }

    @Test
    fun `setting volume updates state`() {
        val generator = TestToneGenerator()
        generator.setVolume(0.42f)
        assertEquals(0.42f, generator.state.value.volume, 0.001f)

        // Clamping check
        generator.setVolume(2.0f)
        assertEquals(1.0f, generator.state.value.volume, 0.001f)

        generator.setVolume(-0.5f)
        assertEquals(0.0f, generator.state.value.volume, 0.001f)
    }

    @Test
    fun `release releases resources safely`() {
        val generator = TestToneGenerator()
        generator.startTone(TestToneMode.PINK_NOISE)
        generator.release()
        assertFalse(generator.state.value.isPlaying)
    }
}
