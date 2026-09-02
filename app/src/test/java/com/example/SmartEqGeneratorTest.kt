package com.example

import com.example.audio.model.AudioCapabilities
import com.example.domain.smarteq.SmartEqContext
import com.example.domain.smarteq.SmartEqGenerator
import com.example.domain.smarteq.SmartEqIntensity
import com.example.settings.model.HeadroomMode
import com.example.settings.model.ListeningGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartEqGeneratorTest {

    @Test
    fun `balanced goal generates flat neutral curve`() {
        val res = SmartEqGenerator.generate(
            goal = ListeningGoal.BALANCED,
            context = SmartEqContext.ALL_AROUND,
            intensity = SmartEqIntensity.BALANCED
        )

        assertEquals(listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f), res.bandGainsDb)
        assertEquals(0, res.bassBoostPercent)
        assertEquals(0.0f, res.trebleGainDb, 0.001f)
        assertTrue(res.validationResult.isValid)
    }

    @Test
    fun `bass focus goal boosts low frequencies and bass strength`() {
        val res = SmartEqGenerator.generate(
            goal = ListeningGoal.BASS_FOCUS,
            context = SmartEqContext.ALL_AROUND,
            intensity = SmartEqIntensity.BALANCED
        )

        assertTrue(res.bandGainsDb[0] > 0f) // 60 Hz boosted
        assertTrue(res.bandGainsDb[1] > 0f) // 230 Hz boosted
        assertTrue(res.bassBoostPercent > 0)
        assertTrue(res.validationResult.isValid)
    }

    @Test
    fun `vocal focus with podcast context reduces low rumble and enhances speech band`() {
        val res = SmartEqGenerator.generate(
            goal = ListeningGoal.VOCAL_FOCUS,
            context = SmartEqContext.PODCAST_SPEECH,
            intensity = SmartEqIntensity.BALANCED
        )

        assertTrue("Low end rumble should be attenuated", res.bandGainsDb[0] < 0f)
        assertTrue("Speech midrange band (910Hz) should be boosted", res.bandGainsDb[2] > 2.0f)
        assertEquals(0, res.bassBoostPercent)
        assertTrue(res.validationResult.isValid)
    }

    @Test
    fun `intensity factor scales frequency gains properly`() {
        val subtle = SmartEqGenerator.generate(
            goal = ListeningGoal.DETAIL,
            context = SmartEqContext.ALL_AROUND,
            intensity = SmartEqIntensity.SUBTLE
        )
        val dynamic = SmartEqGenerator.generate(
            goal = ListeningGoal.DETAIL,
            context = SmartEqContext.ALL_AROUND,
            intensity = SmartEqIntensity.DYNAMIC
        )

        assertTrue(dynamic.bandGainsDb[4] > subtle.bandGainsDb[4])
        assertTrue(dynamic.trebleGainDb > subtle.trebleGainDb)
    }

    @Test
    fun `hardware without bass boost redirects energy into equalizer bands`() {
        val capabilitiesNoBass = AudioCapabilities(
            overallState = com.example.core.result.AudioCapabilityState.SUPPORTED,
            isSystemEqualizerAvailable = true,
            isBassBoostAvailable = false,
            isTrebleAvailable = true,
            isVirtualizerAvailable = true,
            isPreampGainAvailable = true,
            maxBands = 5
        )

        val res = SmartEqGenerator.generate(
            goal = ListeningGoal.BASS_FOCUS,
            context = SmartEqContext.ALL_AROUND,
            intensity = SmartEqIntensity.BALANCED,
            capabilities = capabilitiesNoBass
        )

        assertEquals(0, res.bassBoostPercent)
        assertTrue(res.bandGainsDb[0] >= 3.5f) // Redirected boost in 60Hz band
        assertTrue(res.capabilityAdjustments.isNotEmpty())
    }

    @Test
    fun `headroom mode is analyzed and auto headroom offset is calculated`() {
        val res = SmartEqGenerator.generate(
            goal = ListeningGoal.BASS_FOCUS,
            context = SmartEqContext.CINEMA_MOVIES,
            intensity = SmartEqIntensity.DYNAMIC,
            headroomMode = HeadroomMode.AUTOMATIC
        )

        assertTrue(res.headroomAnalysis.isAutoHeadroomActive)
        assertTrue(res.headroomAnalysis.autoHeadroomOffsetDb < 0f)
        assertTrue(res.validationResult.isValid)
    }
}
