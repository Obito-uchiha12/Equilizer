package com.example.audio.safety

import com.example.settings.model.ListeningProfile
import com.example.settings.model.Preset

data class PresetValidationResult(
    val isValid: Boolean,
    val clippingRisk: ClippingRisk,
    val totalPositiveGainDb: Float,
    val recommendedHeadroomDb: Float,
    val issues: List<String>,
    val requiresAutoHeadroom: Boolean
)

/**
 * Validates audio presets and listening profiles against hardware safety,
 * mathematical clipping risk, and valid gain boundaries.
 */
object PresetSafetyValidator {
    const val MIN_BAND_GAIN_DB = -15.0f
    const val MAX_BAND_GAIN_DB = 15.0f
    const val MIN_TREBLE_GAIN_DB = -10.0f
    const val MAX_TREBLE_GAIN_DB = 10.0f
    const val MIN_PREAMP_GAIN_DB = -12.0f
    const val MAX_PREAMP_GAIN_DB = 6.0f

    fun validateCustomCurve(
        bandGainsDb: List<Float>,
        bassBoostPercent: Int,
        trebleGainDb: Float,
        preampGainDb: Float,
        balance: Float = 0.0f
    ): PresetValidationResult {
        val issues = mutableListOf<String>()

        bandGainsDb.forEachIndexed { index, gain ->
            if (gain.isNaN() || gain.isInfinite()) {
                issues.add("Band $index gain is invalid (NaN/Infinite)")
            } else if (gain < MIN_BAND_GAIN_DB || gain > MAX_BAND_GAIN_DB) {
                issues.add("Band $index gain ($gain dB) exceeds valid range [$MIN_BAND_GAIN_DB, $MAX_BAND_GAIN_DB] dB")
            }
        }

        if (bassBoostPercent !in 0..100) {
            issues.add("BassBoost ($bassBoostPercent%) exceeds valid range [0, 100]%")
        }

        if (trebleGainDb.isNaN() || trebleGainDb.isInfinite() || trebleGainDb < MIN_TREBLE_GAIN_DB || trebleGainDb > MAX_TREBLE_GAIN_DB) {
            issues.add("Treble gain ($trebleGainDb dB) exceeds valid range [$MIN_TREBLE_GAIN_DB, $MAX_TREBLE_GAIN_DB] dB")
        }

        if (preampGainDb.isNaN() || preampGainDb.isInfinite() || preampGainDb < MIN_PREAMP_GAIN_DB || preampGainDb > MAX_PREAMP_GAIN_DB) {
            issues.add("Preamp gain ($preampGainDb dB) exceeds valid range [$MIN_PREAMP_GAIN_DB, $MAX_PREAMP_GAIN_DB] dB")
        }

        if (balance.isNaN() || balance.isInfinite() || balance < -1.0f || balance > 1.0f) {
            issues.add("Balance ($balance) exceeds valid range [-1.0, 1.0]")
        }

        val analysis = HeadroomCalculator.analyze(
            bandGainsDb = bandGainsDb,
            bassStrengthPercent = bassBoostPercent,
            trebleGainDb = trebleGainDb,
            preampGainDb = preampGainDb,
            isAutoHeadroomEnabled = false
        )

        val totalPositiveGain = analysis.totalAccumulatedGainDb
        val clippingRisk = when {
            totalPositiveGain > HeadroomCalculator.HIGH_RISK_THRESHOLD_DB -> ClippingRisk.HIGH_RISK
            totalPositiveGain > HeadroomCalculator.WARNING_THRESHOLD_DB -> ClippingRisk.WARNING
            else -> ClippingRisk.SAFE
        }

        val requiresAutoHeadroom = totalPositiveGain > 0.0f

        return PresetValidationResult(
            isValid = issues.isEmpty(),
            clippingRisk = clippingRisk,
            totalPositiveGainDb = totalPositiveGain,
            recommendedHeadroomDb = analysis.recommendedHeadroomDb,
            issues = issues,
            requiresAutoHeadroom = requiresAutoHeadroom
        )
    }

    fun validatePreset(preset: Preset): PresetValidationResult =
        validateCustomCurve(
            bandGainsDb = preset.bandGainsDb,
            bassBoostPercent = preset.bassBoostPercent,
            trebleGainDb = preset.trebleGainDb,
            preampGainDb = preset.preampGainDb,
            balance = preset.balance
        )

    fun validateProfile(profile: ListeningProfile): PresetValidationResult =
        validateCustomCurve(
            bandGainsDb = profile.bandGainsDb,
            bassBoostPercent = profile.bassBoostPercent,
            trebleGainDb = profile.trebleGainDb,
            preampGainDb = profile.preampGainDb,
            balance = profile.balance
        )

    fun auditPresets(presets: List<Preset>): Map<String, PresetValidationResult> {
        return presets.associate { it.id to validatePreset(it) }
    }

    fun auditProfiles(profiles: List<ListeningProfile>): Map<String, PresetValidationResult> {
        return profiles.associate { it.id to validateProfile(it) }
    }
}
