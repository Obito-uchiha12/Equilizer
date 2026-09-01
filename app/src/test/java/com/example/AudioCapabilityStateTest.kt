package com.example

import com.example.audio.Phase1FoundationAudioEngine
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineStatus
import com.example.audio.model.EqualizerBand
import com.example.core.result.AudioCapabilityState
import com.example.core.result.AudioError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AudioCapabilityStateTest {

    @Test
    fun `audio capability states evaluate correctly`() {
        assertTrue(AudioCapabilityState.SUPPORTED.isUsable)
        assertTrue(AudioCapabilityState.PARTIALLY_SUPPORTED.isUsable)
        assertFalse(AudioCapabilityState.UNSUPPORTED.isUsable)
        assertFalse(AudioCapabilityState.ERROR.isUsable)
    }

    @Test
    fun `phase 1 audio engine initializes cleanly with capability reporting`() {
        val capabilities = AudioCapabilities(
            overallState = AudioCapabilityState.PARTIALLY_SUPPORTED,
            isSystemEqualizerAvailable = true,
            isBassBoostAvailable = true,
            isTrebleAvailable = true,
            isVirtualizerAvailable = false,
            isPreampGainAvailable = true,
            maxBands = 5,
            supportedBands = EqualizerBand.default5Bands()
        )

        val engine = Phase1FoundationAudioEngine(capabilities)
        assertEquals(AudioEngineStatus.READY_FOUNDATION, engine.engineState.value.status)
        assertFalse(engine.engineState.value.isEnabled)
        assertEquals(5, engine.engineState.value.appliedBandsCount)

        // Test enable
        val enableResult = engine.setEnabled(true)
        assertTrue(enableResult.isSuccess)
        assertTrue(engine.engineState.value.isEnabled)
        assertEquals(AudioEngineStatus.ACTIVE, engine.engineState.value.status)

        // Test applying configuration
        val configResult = engine.applyConfiguration(
            AudioEngineConfig(
                isEnabled = true,
                bandGainsDb = mapOf(0 to 3.0f, 1 to 2.0f, 2 to 0.0f, 3 to 1.0f, 4 to 4.0f),
                bassStrengthPercent = 30,
                trebleGainDb = 2.0f,
                preampGainDb = 1.5f,
                stereoBalance = 0.0f
            )
        )
        assertTrue(configResult.isSuccess)
        assertEquals(5, engine.engineState.value.appliedBandsCount)

        // Test release
        engine.release()
        assertEquals(AudioEngineStatus.UNINITIALIZED, engine.engineState.value.status)
    }

    @Test
    fun `unsupported audio state error formatting`() {
        val error = AudioError.AudioApiUnsupported(apiLevel = 23, reason = "Legacy API lacks DynamicsProcessing")
        assertTrue(error.userFriendlyMessage.isNotBlank())
        assertTrue(error.technicalDetails?.contains("API level 23") == true)
    }
}
