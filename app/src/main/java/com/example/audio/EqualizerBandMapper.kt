package com.example.audio

import com.example.audio.model.EqualizerBand
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Result of mapping a single requested frequency and gain to hardware parameters.
 */
data class BandMapping(
    val requestedBandIndex: Int,
    val requestedFreqHz: Int,
    val targetGainDb: Float,
    val mappedHardwareBandIndex: Int,
    val actualHardwareCenterFreqHz: Int,
    val clampedGainMilliBels: Int
) {
    val appliedGainDb: Float
        get() = clampedGainMilliBels / 100.0f

    val diagnosticSummary: String
        get() = "${requestedFreqHz}Hz -> Band #$mappedHardwareBandIndex (${actualHardwareCenterFreqHz}Hz) @ ${String.format(java.util.Locale.US, "%.1f", appliedGainDb)}dB"
}

/**
 * Pure, testable mapper between UI equalizer frequencies and real Android hardware equalizer bands.
 *
 * Android hardware equalizers expose a variable number of bands (e.g. 5, 8, or 10 bands)
 * with device-specific center frequencies (e.g. 60Hz vs 63Hz, 14kHz vs 16kHz)
 * and level ranges in milliBels (e.g. -1500 to +1500 mB).
 */
object EqualizerBandMapper {

    // Default 5 standard UI target frequencies in Hz
    val DEFAULT_TARGET_FREQUENCIES_HZ = listOf(60, 230, 910, 3600, 14000)

    /**
     * Finds the closest hardware band index for a given target frequency (in Hz).
     * Uses logarithmic frequency distance as human auditory perception is logarithmic.
     */
    fun findClosestHardwareBand(
        targetFreqHz: Int,
        hardwareBands: List<EqualizerBand>
    ): Int {
        if (hardwareBands.isEmpty()) return 0
        if (hardwareBands.size == 1) return hardwareBands.first().index

        var bestBandIndex = hardwareBands.first().index
        var smallestLogDistance = Double.MAX_VALUE
        val logTarget = ln(targetFreqHz.toDouble().coerceAtLeast(1.0))

        for (band in hardwareBands) {
            val logBand = ln(band.centerFrequencyHz.toDouble().coerceAtLeast(1.0))
            val logDist = abs(logTarget - logBand)
            if (logDist < smallestLogDistance) {
                smallestLogDistance = logDist
                bestBandIndex = band.index
            }
        }

        return bestBandIndex
    }

    /**
     * Converts a gain in decibels (dB) to milliBels (mB) and clamps it strictly
     * within the hardware band's supported range.
     */
    fun clampGainToMilliBels(
        gainDb: Float,
        minGainMilliBels: Int = -1500,
        maxGainMilliBels: Int = 1500
    ): Int {
        val milliBels = (gainDb * 100.0f).roundToInt()
        return milliBels.coerceIn(minGainMilliBels, maxGainMilliBels)
    }

    /**
     * Maps a full set of requested UI band gains to the hardware equalizer bands.
     *
     * @param requestedGains Map of UI band index -> gain in dB
     * @param targetFrequenciesHz List of target frequencies corresponding to UI band indices
     * @param hardwareBands List of actual bands exposed by the Android Equalizer effect
     * @param minGainMilliBels Minimum gain supported by the hardware effect (e.g. -1500)
     * @param maxGainMilliBels Maximum gain supported by the hardware effect (e.g. +1500)
     */
    fun mapGainsToHardware(
        requestedGains: Map<Int, Float>,
        targetFrequenciesHz: List<Int> = DEFAULT_TARGET_FREQUENCIES_HZ,
        hardwareBands: List<EqualizerBand>,
        minGainMilliBels: Int = -1500,
        maxGainMilliBels: Int = 1500
    ): List<BandMapping> {
        if (hardwareBands.isEmpty()) return emptyList()

        val mappings = mutableListOf<BandMapping>()

        for ((uiIndex, targetFreq) in targetFrequenciesHz.withIndex()) {
            val gainDb = requestedGains[uiIndex] ?: 0.0f
            val mappedHwIndex = findClosestHardwareBand(targetFreq, hardwareBands)
            val hwBand = hardwareBands.find { it.index == mappedHwIndex } ?: hardwareBands.first()

            val clampedMilliBels = clampGainToMilliBels(
                gainDb = gainDb,
                minGainMilliBels = hwBand.minGainMilliBels.coerceAtLeast(minGainMilliBels),
                maxGainMilliBels = hwBand.maxGainMilliBels.coerceAtMost(maxGainMilliBels)
            )

            mappings.add(
                BandMapping(
                    requestedBandIndex = uiIndex,
                    requestedFreqHz = targetFreq,
                    targetGainDb = gainDb,
                    mappedHardwareBandIndex = mappedHwIndex,
                    actualHardwareCenterFreqHz = hwBand.centerFrequencyHz,
                    clampedGainMilliBels = clampedMilliBels
                )
            )
        }

        return mappings
    }
}
