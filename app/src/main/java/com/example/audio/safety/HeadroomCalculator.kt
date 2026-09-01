package com.example.audio.safety

import com.example.settings.model.HeadroomMode
import kotlin.math.max

/**
 * Categorized DSP clipping risk evaluation model.
 */
enum class ClippingRisk(val displayName: String, val level: Int) {
    SAFE("Safe", 0),
    WARNING("Moderate Boost Warning", 1),
    HIGH_RISK("High Clipping Risk", 2)
}

/**
 * Immutable analysis results from [HeadroomCalculator].
 * Clearly distinguishes estimated gain from measured waveform clipping.
 */
data class HeadroomAnalysis(
    val maxEqBoostDb: Float,
    val minEqCutDb: Float,
    val estimatedBassBoostDb: Float,
    val estimatedTrebleBoostDb: Float,
    val preampGainDb: Float,
    val totalAccumulatedGainDb: Float,
    val recommendedHeadroomDb: Float,
    val autoHeadroomOffsetDb: Float,
    val effectivePeakGainDb: Float,
    val clippingRisk: ClippingRisk,
    val riskExplanation: String,
    val isAutoHeadroomActive: Boolean,
    val headroomMode: HeadroomMode = HeadroomMode.AUTOMATIC
)

/**
 * Pure, testable calculation engine for DSP gain accumulation and headroom management.
 * Protects Android AudioEffect pipelines against inter-sample and digital DAC clipping
 * without altering the user's relative frequency response curve.
 */
object HeadroomCalculator {

    /**
     * Estimated maximum acoustic/electrical gain added by native BassBoost (0..100%).
     * Android's native BassBoost is a low-frequency resonant filter (~60-150Hz);
     * we model its contribution conservatively as up to +6.0 dB at 100% strength.
     */
    private const val MAX_ESTIMATED_BASS_BOOST_DB = 6.0f

    /**
     * Threshold in dB above which total positive gain is classified as WARNING.
     */
    const val WARNING_THRESHOLD_DB = 1.0f

    /**
     * Threshold in dB above which total positive gain is classified as HIGH_RISK.
     */
    const val HIGH_RISK_THRESHOLD_DB = 6.0f

    /**
     * Analyzes current EQ bands, BassBoost, Treble shelf, Preamp, and Headroom mode.
     */
    fun analyze(
        bandGainsDb: List<Float>?,
        bassStrengthPercent: Int,
        trebleGainDb: Float,
        preampGainDb: Float,
        headroomMode: HeadroomMode,
        manualHeadroomDb: Float = 0.0f
    ): HeadroomAnalysis {
        // Sanitize and validate inputs (handle NaN / Infinity / nulls)
        val sanitizedBands = (bandGainsDb ?: emptyList()).map { gain ->
            if (gain.isNaN() || gain.isInfinite()) 0.0f else gain.coerceIn(-24.0f, 24.0f)
        }

        val sanitizedBass = bassStrengthPercent.coerceIn(0, 100)
        val sanitizedTreble = if (trebleGainDb.isNaN() || trebleGainDb.isInfinite()) 0.0f else trebleGainDb.coerceIn(-20.0f, 20.0f)
        val sanitizedPreamp = if (preampGainDb.isNaN() || preampGainDb.isInfinite()) 0.0f else preampGainDb.coerceIn(-24.0f, 24.0f)
        val sanitizedManualHeadroom = if (manualHeadroomDb.isNaN() || manualHeadroomDb.isInfinite()) 0.0f else manualHeadroomDb.coerceIn(-24.0f, 0.0f)

        // 1. Calculate individual stage gains
        val maxEqBoost = sanitizedBands.filter { it > 0.0f }.maxOrNull() ?: 0.0f
        val minEqCut = sanitizedBands.filter { it < 0.0f }.minOrNull() ?: 0.0f

        // Estimated BassBoost contribution
        val estimatedBassBoost = (sanitizedBass / 100.0f) * MAX_ESTIMATED_BASS_BOOST_DB

        // Treble High-Shelf contribution (only positive boost adds to peak risk)
        val estimatedTrebleBoost = sanitizedTreble.coerceAtLeast(0.0f)

        // Total accumulated potential positive gain across all cascaded DSP stages
        val totalPositivePeakGain = (maxEqBoost + estimatedBassBoost + estimatedTrebleBoost + sanitizedPreamp).coerceAtLeast(0.0f)

        // 2. Recommended Headroom Attenuation (always <= 0.0 dB)
        val recommendedHeadroom = if (totalPositivePeakGain > 0.0f) -totalPositivePeakGain else 0.0f

        // 3. Auto / Manual Headroom Offset to apply
        val autoHeadroomOffset = when (headroomMode) {
            HeadroomMode.AUTOMATIC -> if (totalPositivePeakGain > 0.0f) -totalPositivePeakGain else 0.0f
            HeadroomMode.MANUAL -> sanitizedManualHeadroom
            HeadroomMode.OFF -> 0.0f
        }

        // Effective peak gain after auto/manual headroom attenuation
        val effectivePeakGain = (totalPositivePeakGain + autoHeadroomOffset).coerceAtLeast(0.0f)

        // 4. Determine Clipping Risk
        val risk = when {
            effectivePeakGain > HIGH_RISK_THRESHOLD_DB -> ClippingRisk.HIGH_RISK
            effectivePeakGain > WARNING_THRESHOLD_DB -> ClippingRisk.WARNING
            else -> ClippingRisk.SAFE
        }

        val explanation = when (headroomMode) {
            HeadroomMode.AUTOMATIC -> {
                if (autoHeadroomOffset < 0.0f) {
                    "Automatic Headroom active (${String.format("%.1f", autoHeadroomOffset)} dB digital attenuation applied). Signal level is protected from 0 dBFS clipping."
                } else {
                    "Signal headroom is optimal (0 dBFS safe margin maintained)."
                }
            }
            HeadroomMode.MANUAL -> {
                if (risk == ClippingRisk.HIGH_RISK) {
                    "High risk of digital clipping (+${String.format("%.1f", effectivePeakGain)} dB net peak). Increase manual headroom attenuation or reduce positive boosts."
                } else if (risk == ClippingRisk.WARNING) {
                    "Moderate boost warning (+${String.format("%.1f", effectivePeakGain)} dB net peak). Manual headroom offset applied: ${String.format("%.1f", autoHeadroomOffset)} dB."
                } else {
                    "Manual headroom attenuation (${String.format("%.1f", autoHeadroomOffset)} dB) keeps net output within safe bounds."
                }
            }
            HeadroomMode.OFF -> {
                if (risk == ClippingRisk.HIGH_RISK) {
                    "Headroom protection is OFF. High risk of digital DAC/DSP clipping (+${String.format("%.1f", totalPositivePeakGain)} dB boost)."
                } else if (risk == ClippingRisk.WARNING) {
                    "Headroom protection is OFF. Moderate boost (+${String.format("%.1f", totalPositivePeakGain)} dB) may cause inter-sample clipping on hot mixes."
                } else {
                    "Headroom protection is OFF. Current EQ curve is neutral/subtractive (safe)."
                }
            }
        }

        return HeadroomAnalysis(
            maxEqBoostDb = maxEqBoost,
            minEqCutDb = minEqCut,
            estimatedBassBoostDb = estimatedBassBoost,
            estimatedTrebleBoostDb = estimatedTrebleBoost,
            preampGainDb = sanitizedPreamp,
            totalAccumulatedGainDb = totalPositivePeakGain,
            recommendedHeadroomDb = recommendedHeadroom,
            autoHeadroomOffsetDb = autoHeadroomOffset,
            effectivePeakGainDb = effectivePeakGain,
            clippingRisk = risk,
            riskExplanation = explanation,
            isAutoHeadroomActive = autoHeadroomOffset < -0.01f,
            headroomMode = headroomMode
        )
    }

    /**
     * Backward-compatible overload accepting a boolean flag.
     */
    fun analyze(
        bandGainsDb: List<Float>?,
        bassStrengthPercent: Int,
        trebleGainDb: Float,
        preampGainDb: Float,
        isAutoHeadroomEnabled: Boolean
    ): HeadroomAnalysis {
        val mode = if (isAutoHeadroomEnabled) HeadroomMode.AUTOMATIC else HeadroomMode.OFF
        return analyze(
            bandGainsDb = bandGainsDb,
            bassStrengthPercent = bassStrengthPercent,
            trebleGainDb = trebleGainDb,
            preampGainDb = preampGainDb,
            headroomMode = mode,
            manualHeadroomDb = 0.0f
        )
    }

    /**
     * Normalizes an EQ curve by shifting all band gains downward by the maximum positive peak gain.
     * The resulting acoustic curve has identical relative frequency shaping, but with a maximum peak
     * of exactly 0.0 dB (pure subtractive equalization).
     */
    fun normalizeEqCurve(bandGainsDb: List<Float>): List<Float> {
        val sanitized = bandGainsDb.map { if (it.isNaN() || it.isInfinite()) 0.0f else it }
        val maxGain = sanitized.maxOrNull() ?: 0.0f
        if (maxGain <= 0.0f) {
            // Already 0 dB peak or entirely cuts
            return sanitized
        }
        return sanitized.map { gain -> (gain - maxGain) }
    }
}
