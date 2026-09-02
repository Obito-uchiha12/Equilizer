package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AndroidDspAudioEngine
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineStatus
import com.example.audio.model.AudioSessionInfo
import com.example.audio.model.EqualizerBand
import com.example.core.result.AudioCapabilityState
import com.example.device.model.AudioDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidDspAudioEngineTest {

    private lateinit var context: Context
    private lateinit var defaultCapabilities: AudioCapabilities
    private lateinit var engine: AndroidDspAudioEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        defaultCapabilities = AudioCapabilities(
            overallState = AudioCapabilityState.SUPPORTED,
            isSystemEqualizerAvailable = true,
            isBassBoostAvailable = true,
            isTrebleAvailable = true,
            isVirtualizerAvailable = false,
            isPreampGainAvailable = false,
            supportedBands = EqualizerBand.default5Bands()
        )
        engine = AndroidDspAudioEngine(context, defaultCapabilities)
    }

    @Test
    fun `engine initializes and reports non-null state without throwing exceptions`() {
        val state = engine.engineState.value
        assertNotNull(state)
        assertEquals("Android System AudioEffect DSP", state.engineImplementation)
    }

    @Test
    fun `engine handles enable and disable transitions safely`() {
        val enableResult = engine.setEnabled(true)
        assertNotNull(enableResult)

        val disableResult = engine.setEnabled(false)
        assertNotNull(disableResult)
        assertFalse(engine.engineState.value.isEnabled)
    }

    @Test
    fun `engine applies audio configuration safely`() {
        val config = AudioEngineConfig(
            isEnabled = true,
            bandGainsDb = mapOf(0 to 4.0f, 1 to 2.0f, 2 to 0.0f, 3 to -2.0f, 4 to 5.0f),
            bassStrengthPercent = 40,
            trebleGainDb = 3.0f,
            preampGainDb = 0.0f,
            stereoBalance = 0.0f
        )

        val result = engine.applyConfiguration(config)
        assertNotNull(result)
    }

    @Test
    fun `engine handles audio device routing changes`() {
        val device = AudioDevice.defaultBuiltinSpeaker()
        engine.onDeviceChanged(device)
        assertNotNull(engine.engineState.value)
    }

    @Test
    fun `engine release and duplicate release are completely safe`() {
        engine.release()
        assertEquals(AudioEngineStatus.UNINITIALIZED, engine.engineState.value.status)
        assertFalse(engine.engineState.value.isEnabled)

        // Duplicate release call should be a no-op and not crash
        engine.release()
        assertEquals(AudioEngineStatus.UNINITIALIZED, engine.engineState.value.status)
    }

    @Test
    fun `post-release operations fail gracefully without throwing crashes`() {
        engine.release()

        val enableResult = engine.setEnabled(true)
        assertTrue(enableResult.isFailure)

        val config = AudioEngineConfig(
            isEnabled = true,
            bandGainsDb = emptyMap(),
            bassStrengthPercent = 0,
            trebleGainDb = 0.0f,
            preampGainDb = 0.0f,
            stereoBalance = 0.0f
        )
        val configResult = engine.applyConfiguration(config)
        assertTrue(configResult.isFailure)
    }

    @Test
    fun `re-initialization with specific session id cleans up and re-attaches safely`() {
        val customSession = AudioSessionInfo(sessionId = 42, isGlobalMix = false)
        val result = engine.initialize(customSession)
        assertNotNull(result)
        assertEquals(42, engine.engineState.value.activeSession.sessionId)
    }

    @Test
    fun `applying configuration with auto headroom offset updates diagnostics state`() {
        val config = AudioEngineConfig(
            isEnabled = true,
            bandGainsDb = mapOf(0 to 6.0f, 1 to 4.0f, 2 to 2.0f, 3 to 1.0f, 4 to 5.0f),
            bassStrengthPercent = 60,
            trebleGainDb = 3.0f,
            preampGainDb = 0.0f,
            stereoBalance = 0.0f,
            autoHeadroomOffsetDb = -8.5f
        )
        val result = engine.applyConfiguration(config)
        assertNotNull(result)
        assertEquals(-8.5f, engine.engineState.value.autoHeadroomOffsetDb, 0.001f)
        assertTrue(engine.engineState.value.isAutoHeadroomActive)
        assertEquals("SAFE (Auto Headroom Active)", engine.engineState.value.clippingRisk)
    }

    @Test
    fun `session state machine tracks package name and handles retry recovery`() {
        val session = AudioSessionInfo(sessionId = 101, isGlobalMix = false)
        val initResult = engine.initialize(session, "com.spotify.music")
        assertTrue(initResult.isSuccess)

        val state = engine.engineState.value
        assertEquals("com.spotify.music", state.activePackageName)
        assertEquals(101, state.activeSession.sessionId)
        assertTrue(state.processId >= 0)

        val retryResult = engine.retryRecovery()
        assertTrue(retryResult.isSuccess)
    }
}
