package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AndroidDspAudioEngine
import com.example.audio.AudioCapabilityDetector
import com.example.audio.DefaultAudioCapabilityDetector
import com.example.audio.TestToneGenerator
import com.example.audio.TestToneMode
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineStatus
import com.example.audio.model.AudioSessionInfo
import com.example.audio.model.AudioSessionState
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EqualizerProductionLifecycleTest {

    private lateinit var context: Context
    private lateinit var capabilityDetector: AudioCapabilityDetector
    private lateinit var engine: AndroidDspAudioEngine
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        capabilityDetector = DefaultAudioCapabilityDetector(context)
        val capabilities = capabilityDetector.detectCapabilities()
        engine = AndroidDspAudioEngine(context, capabilities, testScope)
    }

    @Test
    fun `session state machine begins in initializing and transitions to attached`() {
        val state = engine.engineState.value
        assertNotNull(state)
        assertTrue(state.isHardwareAttached)
        assertEquals(AudioSessionState.ATTACHED, state.sessionState)
        assertFalse(state.isEnabled)
    }

    @Test
    fun `enabling equalizer transitions state to active`() {
        val enableResult = engine.setEnabled(true)
        assertTrue(enableResult.isSuccess)

        val state = engine.engineState.value
        assertTrue(state.isEnabled)
        assertEquals(AudioEngineStatus.ACTIVE, state.status)
        assertEquals(AudioSessionState.ACTIVE, state.sessionState)
    }

    @Test
    fun `disabling equalizer transitions state back to attached without losing session`() {
        engine.setEnabled(true)
        val disableResult = engine.setEnabled(false)
        assertTrue(disableResult.isSuccess)

        val state = engine.engineState.value
        assertFalse(state.isEnabled)
        assertEquals(AudioEngineStatus.DISABLED, state.status)
        assertEquals(AudioSessionState.ATTACHED, state.sessionState)
    }

    @Test
    fun `switching external media session updates package name and maintains session state`() {
        val spotifySession = AudioSessionInfo(sessionId = 42, isGlobalMix = false)
        val initResult = engine.initialize(spotifySession, "com.spotify.music")
        assertTrue(initResult.isSuccess)

        val state = engine.engineState.value
        assertEquals(42, state.activeSession.sessionId)
        assertFalse(state.activeSession.isGlobalMix)
        assertEquals("com.spotify.music", state.activePackageName)
        assertEquals(AudioSessionState.ATTACHED, state.sessionState)
    }

    @Test
    fun `device output route change re-applies configuration without error`() {
        engine.setEnabled(true)
        val config = AudioEngineConfig(
            isEnabled = true,
            bandGainsDb = mapOf(0 to 3.0f, 1 to 1.5f, 2 to 0.0f, 3 to 2.0f, 4 to 4.0f),
            bassStrengthPercent = 40,
            trebleGainDb = 2.0f,
            preampGainDb = -1.0f,
            stereoBalance = 0.0f,
            autoHeadroomOffsetDb = -3.0f
        )
        engine.applyConfiguration(config)
        testScope.advanceUntilIdle()

        val btDevice = AudioDevice(
            id = "bt_sony",
            name = "Sony WH-1000XM5",
            type = DeviceType.BLUETOOTH_HEADPHONES
        )
        engine.onDeviceChanged(btDevice)
        testScope.advanceUntilIdle()

        val state = engine.engineState.value
        assertTrue(state.isEnabled)
        assertEquals(5, state.appliedBandsCount)
        assertEquals(40, state.bassBoostStrength)
    }

    @Test
    fun `test tone generator safely stops and cleans up audio track on route disconnect`() {
        val toneGen = TestToneGenerator(testScope)
        toneGen.startTone(TestToneMode.MID_910HZ)
        testScope.advanceUntilIdle()

        assertTrue(toneGen.state.value.isPlaying)
        assertEquals(910, toneGen.state.value.activeMode.frequencyHz)

        toneGen.stopTone()
        testScope.advanceUntilIdle()

        assertFalse(toneGen.state.value.isPlaying)
        toneGen.release()
    }

    @Test
    fun `duplicate release does not crash and marks state uninitialized`() {
        engine.release()
        val state = engine.engineState.value
        assertEquals(AudioEngineStatus.UNINITIALIZED, state.status)
        assertEquals(AudioSessionState.NO_SESSION, state.sessionState)
        assertFalse(state.isHardwareAttached)

        // Duplicate call should be gracefully handled
        engine.release()
        assertEquals(AudioEngineStatus.UNINITIALIZED, engine.engineState.value.status)
    }
}
