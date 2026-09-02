package com.example

import com.example.audio.safety.ClippingRisk
import com.example.audio.safety.HeadroomCalculator
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.profile.DefaultDeviceProfileManager
import com.example.settings.DefaultSettingsRepository
import com.example.settings.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeadroomSafetyTest {

    @Test
    fun `flat mode is perfectly neutral and zero risk`() {
        val flatGains = listOf(0f, 0f, 0f, 0f, 0f)
        val analysis = HeadroomCalculator.analyze(
            bandGainsDb = flatGains,
            bassStrengthPercent = 0,
            trebleGainDb = 0f,
            preampGainDb = 0f,
            isAutoHeadroomEnabled = true
        )

        assertEquals(0f, analysis.maxEqBoostDb, 0.001f)
        assertEquals(0f, analysis.estimatedBassBoostDb, 0.001f)
        assertEquals(0f, analysis.estimatedTrebleBoostDb, 0.001f)
        assertEquals(0f, analysis.preampGainDb, 0.001f)
        assertEquals(0f, analysis.totalAccumulatedGainDb, 0.001f)
        assertEquals(0f, analysis.recommendedHeadroomDb, 0.001f)
        assertEquals(0f, analysis.autoHeadroomOffsetDb, 0.001f)
        assertFalse(analysis.isAutoHeadroomActive)
        assertEquals(0f, analysis.effectivePeakGainDb, 0.001f)
        assertEquals(ClippingRisk.SAFE, analysis.clippingRisk)
    }

    @Test
    fun `moderate EQ boost stays within safe limits`() {
        val gains = listOf(1.0f, 0.5f, 0.0f, 0.0f, 0.5f)
        val analysis = HeadroomCalculator.analyze(
            bandGainsDb = gains,
            bassStrengthPercent = 0,
            trebleGainDb = 0f,
            preampGainDb = 0f,
            isAutoHeadroomEnabled = false
        )

        assertEquals(1.0f, analysis.maxEqBoostDb, 0.001f)
        assertEquals(1.0f, analysis.totalAccumulatedGainDb, 0.001f)
        assertEquals(ClippingRisk.SAFE, analysis.clippingRisk)
        assertEquals(0.0f, analysis.autoHeadroomOffsetDb, 0.001f)
    }

    @Test
    fun `heavy compound boost triggers warning and auto headroom applies full attenuation`() {
        val gains = listOf(4.0f, 2.0f, 0.0f, 1.0f, 3.0f)
        val analysisWithAuto = HeadroomCalculator.analyze(
            bandGainsDb = gains,
            bassStrengthPercent = 50, // +3.0 dB est.
            trebleGainDb = 2.0f,      // +2.0 dB
            preampGainDb = 1.0f,      // +1.0 dB
            isAutoHeadroomEnabled = true
        )

        // Total accumulated = 4.0 (EQ) + 3.0 (Bass) + 2.0 (Treble) + 1.0 (Preamp) = 10.0 dB
        assertEquals(4.0f, analysisWithAuto.maxEqBoostDb, 0.001f)
        assertEquals(3.0f, analysisWithAuto.estimatedBassBoostDb, 0.001f)
        assertEquals(2.0f, analysisWithAuto.estimatedTrebleBoostDb, 0.001f)
        assertEquals(1.0f, analysisWithAuto.preampGainDb, 0.001f)
        assertEquals(10.0f, analysisWithAuto.totalAccumulatedGainDb, 0.001f)
        assertEquals(-10.0f, analysisWithAuto.recommendedHeadroomDb, 0.001f)
        assertEquals(ClippingRisk.SAFE, analysisWithAuto.clippingRisk)

        // Auto Headroom active -> -10.0 dB offset applied -> effective peak = 0.0 dB
        assertTrue(analysisWithAuto.isAutoHeadroomActive)
        assertEquals(-10.0f, analysisWithAuto.autoHeadroomOffsetDb, 0.001f)
        assertEquals(0.0f, analysisWithAuto.effectivePeakGainDb, 0.001f)

        // When auto headroom is disabled:
        val analysisWithoutAuto = HeadroomCalculator.analyze(
            bandGainsDb = gains,
            bassStrengthPercent = 50,
            trebleGainDb = 2.0f,
            preampGainDb = 1.0f,
            isAutoHeadroomEnabled = false
        )
        assertFalse(analysisWithoutAuto.isAutoHeadroomActive)
        assertEquals(0.0f, analysisWithoutAuto.autoHeadroomOffsetDb, 0.001f)
        assertEquals(10.0f, analysisWithoutAuto.effectivePeakGainDb, 0.001f)
        assertEquals(ClippingRisk.HIGH_RISK, analysisWithoutAuto.clippingRisk)
    }

    @Test
    fun `preset safety properties calculate accurately`() {
        val electronicPreset = Preset.defaultPresets().find { it.id == Preset.ELECTRONIC.id }!!
        assertTrue(electronicPreset.maxPositiveBoostDb > 0f)
        assertEquals(ClippingRisk.HIGH_RISK, electronicPreset.clippingRisk)

        val flatPreset = Preset.defaultPresets().find { it.id == Preset.FLAT.id }!!
        assertEquals(0f, flatPreset.maxPositiveBoostDb, 0.001f)
        assertEquals(ClippingRisk.SAFE, flatPreset.clippingRisk)
    }

    @Test
    fun `equalizer curve normalization converts boost curve to purely subtractive`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = DefaultSettingsRepository(context)

        // Set an aggressive positive boost curve
        repo.updateBand(0, 4.0f)
        repo.updateBand(1, 2.0f)
        repo.updateBand(2, 0.0f)
        repo.updateBand(3, -1.0f)
        repo.updateBand(4, 6.0f) // Peak is +6.0 dB

        repo.normalizeEqualizerCurve()

        val normalizedBands = repo.settings.value.bands
        assertEquals(-2.0f, normalizedBands[0].gainDb, 0.001f) // 4 - 6 = -2
        assertEquals(-4.0f, normalizedBands[1].gainDb, 0.001f) // 2 - 6 = -4
        assertEquals(-6.0f, normalizedBands[2].gainDb, 0.001f) // 0 - 6 = -6
        assertEquals(-7.0f, normalizedBands[3].gainDb, 0.001f) // -1 - 6 = -7
        assertEquals(0.0f, normalizedBands[4].gainDb, 0.001f)  // 6 - 6 = 0

        // Peak is now exactly 0.0 dB
        val maxNormalizedGain = normalizedBands.maxOf { it.gainDb }
        assertEquals(0.0f, maxNormalizedGain, 0.001f)
    }

    @Test
    fun `device profile safety validation clamps extreme out-of-spec parameters`() {
        val unsafeProfile = DeviceProfile(
            id = "test_unsafe",
            name = "Extreme Unsafe",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            presetId = "custom",
            bandGainsDb = listOf(25.0f, -25.0f, 15.0f, 0.0f, 30.0f),
            bassBoostPercent = 150,
            trebleGainDb = 15.0f,
            preampGainDb = 20.0f,
            balance = 2.5f
        )

        val clamped = DefaultDeviceProfileManager.validateProfileSafety(unsafeProfile)

        // Gains clamped to [-15.0, +15.0]
        assertEquals(15.0f, clamped.bandGainsDb[0], 0.001f)
        assertEquals(-15.0f, clamped.bandGainsDb[1], 0.001f)
        assertEquals(15.0f, clamped.bandGainsDb[2], 0.001f)
        assertEquals(0.0f, clamped.bandGainsDb[3], 0.001f)
        assertEquals(15.0f, clamped.bandGainsDb[4], 0.001f)

        // BassBoost clamped to [0, 100]
        assertEquals(100, clamped.bassBoostPercent)

        // Treble clamped to [-10.0, +10.0]
        assertEquals(10.0f, clamped.trebleGainDb, 0.001f)

        // Preamp clamped to [-12.0, +6.0]
        assertEquals(6.0f, clamped.preampGainDb, 0.001f)

        // Balance clamped to [-1.0, +1.0]
        assertEquals(1.0f, clamped.balance, 0.001f)
    }
}
