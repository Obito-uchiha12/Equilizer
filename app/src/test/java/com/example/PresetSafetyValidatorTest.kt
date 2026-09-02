package com.example

import com.example.audio.safety.ClippingRisk
import com.example.audio.safety.PresetSafetyValidator
import com.example.settings.model.ListeningGoal
import com.example.settings.model.ListeningProfile
import com.example.settings.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PresetSafetyValidatorTest {

    @Test
    fun `flat preset passes validation with zero clipping risk and valid state`() {
        val result = PresetSafetyValidator.validatePreset(Preset.FLAT)
        assertTrue(result.isValid)
        assertEquals(ClippingRisk.SAFE, result.clippingRisk)
        assertEquals(0.0f, result.totalPositiveGainDb, 0.001f)
        assertFalse(result.requiresAutoHeadroom)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `all default presets are strictly valid and safe or warning level`() {
        val presets = Preset.defaultPresets()
        val audit = PresetSafetyValidator.auditPresets(presets)

        assertEquals(presets.size, audit.size)
        audit.forEach { (presetId, result) ->
            assertTrue("Preset $presetId must be mathematically valid", result.isValid)
            assertTrue("Preset $presetId should have no out-of-bounds parameter errors", result.issues.isEmpty())
        }
    }

    @Test
    fun `all default listening profiles are valid and have non-negative recommended headroom`() {
        val profiles = ListeningProfile.defaultProfiles()
        val audit = PresetSafetyValidator.auditProfiles(profiles)

        assertEquals(profiles.size, audit.size)
        audit.forEach { (profileId, result) ->
            assertTrue("Profile $profileId must be valid", result.isValid)
            assertTrue("Profile $profileId issues should be empty", result.issues.isEmpty())
            assertTrue("Recommended headroom attenuation should be <= 0.0 dB", result.recommendedHeadroomDb <= 0.0f)
        }
    }

    @Test
    fun `excessive positive boost triggers warning or high risk clipping`() {
        val highBoost = PresetSafetyValidator.validateCustomCurve(
            bandGainsDb = listOf(12.0f, 10.0f, 6.0f, 4.0f, 8.0f),
            bassBoostPercent = 90,
            trebleGainDb = 8.0f,
            preampGainDb = 4.0f,
            balance = 0.0f
        )

        assertTrue(highBoost.isValid) // Within valid ranges, but high gain
        assertEquals(ClippingRisk.HIGH_RISK, highBoost.clippingRisk)
        assertTrue(highBoost.totalPositiveGainDb > 10.0f)
        assertTrue(highBoost.requiresAutoHeadroom)
    }

    @Test
    fun `out of range band gain is flagged as invalid with issue descriptions`() {
        val invalidResult = PresetSafetyValidator.validateCustomCurve(
            bandGainsDb = listOf(25.0f, 0.0f, 0.0f, 0.0f, -20.0f), // >15 dB and <-15 dB
            bassBoostPercent = 150, // >100%
            trebleGainDb = 15.0f,   // >10 dB
            preampGainDb = 12.0f,   // >6 dB
            balance = 2.5f          // >1.0
        )

        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.issues.size >= 4)
    }

    @Test
    fun `nan or infinite gains are detected and flagged`() {
        val nanResult = PresetSafetyValidator.validateCustomCurve(
            bandGainsDb = listOf(Float.NaN, 0.0f, Float.POSITIVE_INFINITY, 0.0f, 0.0f),
            bassBoostPercent = 0,
            trebleGainDb = Float.NaN,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        assertFalse(nanResult.isValid)
        assertTrue(nanResult.issues.any { it.contains("NaN") || it.contains("Infinite") })
    }
}
