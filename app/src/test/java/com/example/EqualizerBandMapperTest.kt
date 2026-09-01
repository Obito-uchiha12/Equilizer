package com.example

import com.example.audio.EqualizerBandMapper
import com.example.audio.model.EqualizerBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerBandMapperTest {

    private val standard5Bands = listOf(
        EqualizerBand(index = 0, centerFrequencyHz = 60, minGainMilliBels = -1500, maxGainMilliBels = 1500),
        EqualizerBand(index = 1, centerFrequencyHz = 230, minGainMilliBels = -1500, maxGainMilliBels = 1500),
        EqualizerBand(index = 2, centerFrequencyHz = 910, minGainMilliBels = -1500, maxGainMilliBels = 1500),
        EqualizerBand(index = 3, centerFrequencyHz = 3600, minGainMilliBels = -1500, maxGainMilliBels = 1500),
        EqualizerBand(index = 4, centerFrequencyHz = 14000, minGainMilliBels = -1500, maxGainMilliBels = 1500)
    )

    private val device10Bands = listOf(
        EqualizerBand(index = 0, centerFrequencyHz = 31, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 1, centerFrequencyHz = 62, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 2, centerFrequencyHz = 125, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 3, centerFrequencyHz = 250, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 4, centerFrequencyHz = 500, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 5, centerFrequencyHz = 1000, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 6, centerFrequencyHz = 2000, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 7, centerFrequencyHz = 4000, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 8, centerFrequencyHz = 8000, minGainMilliBels = -1200, maxGainMilliBels = 1200),
        EqualizerBand(index = 9, centerFrequencyHz = 16000, minGainMilliBels = -1200, maxGainMilliBels = 1200)
    )

    @Test
    fun `findClosestHardwareBand maps exact frequencies correctly`() {
        assertEquals(0, EqualizerBandMapper.findClosestHardwareBand(60, standard5Bands))
        assertEquals(1, EqualizerBandMapper.findClosestHardwareBand(230, standard5Bands))
        assertEquals(2, EqualizerBandMapper.findClosestHardwareBand(910, standard5Bands))
        assertEquals(3, EqualizerBandMapper.findClosestHardwareBand(3600, standard5Bands))
        assertEquals(4, EqualizerBandMapper.findClosestHardwareBand(14000, standard5Bands))
    }

    @Test
    fun `findClosestHardwareBand maps 5 UI bands to 10 hardware bands accurately`() {
        // UI 60Hz -> closest in 10-band is 62Hz (index 1)
        assertEquals(1, EqualizerBandMapper.findClosestHardwareBand(60, device10Bands))
        // UI 230Hz -> closest in 10-band is 250Hz (index 3)
        assertEquals(3, EqualizerBandMapper.findClosestHardwareBand(230, device10Bands))
        // UI 910Hz -> closest in 10-band is 1000Hz (index 5)
        assertEquals(5, EqualizerBandMapper.findClosestHardwareBand(910, device10Bands))
        // UI 3600Hz -> closest in 10-band is 4000Hz (index 7)
        assertEquals(7, EqualizerBandMapper.findClosestHardwareBand(3600, device10Bands))
        // UI 14000Hz -> closest in 10-band is 16000Hz (index 9)
        assertEquals(9, EqualizerBandMapper.findClosestHardwareBand(14000, device10Bands))
    }

    @Test
    fun `clampGainToMilliBels correctly converts dB and clamps to limits`() {
        // 0.0 dB -> 0 mB
        assertEquals(0, EqualizerBandMapper.clampGainToMilliBels(0.0f))
        // +6.5 dB -> +650 mB
        assertEquals(650, EqualizerBandMapper.clampGainToMilliBels(6.5f))
        // -12.0 dB -> -1200 mB
        assertEquals(-1200, EqualizerBandMapper.clampGainToMilliBels(-12.0f))
        // +20.0 dB (exceeds +15dB limit) -> clamped to +1500 mB
        assertEquals(1500, EqualizerBandMapper.clampGainToMilliBels(20.0f, -1500, 1500))
        // -25.0 dB (exceeds -15dB limit) -> clamped to -1500 mB
        assertEquals(-1500, EqualizerBandMapper.clampGainToMilliBels(-25.0f, -1500, 1500))
    }

    @Test
    fun `mapGainsToHardware produces valid non-empty mappings with summary strings`() {
        val requestedGains = mapOf(
            0 to 5.0f,
            1 to 2.5f,
            2 to -1.0f,
            3 to 3.0f,
            4 to 6.0f
        )

        val mappings = EqualizerBandMapper.mapGainsToHardware(
            requestedGains = requestedGains,
            hardwareBands = standard5Bands
        )

        assertEquals(5, mappings.size)
        assertEquals(500, mappings[0].clampedGainMilliBels)
        assertEquals(250, mappings[1].clampedGainMilliBels)
        assertEquals(-100, mappings[2].clampedGainMilliBels)
        assertEquals(300, mappings[3].clampedGainMilliBels)
        assertEquals(600, mappings[4].clampedGainMilliBels)

        for (m in mappings) {
            assertTrue(m.diagnosticSummary.isNotBlank())
        }
    }
}
