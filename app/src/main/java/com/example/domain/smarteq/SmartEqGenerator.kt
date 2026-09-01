package com.example.domain.smarteq

import com.example.audio.model.AudioCapabilities
import com.example.audio.safety.HeadroomCalculator
import com.example.audio.safety.PresetSafetyValidator
import com.example.audio.safety.PresetValidationResult
import com.example.audio.safety.HeadroomAnalysis
import com.example.settings.model.HeadroomMode
import com.example.settings.model.ListeningGoal

enum class SmartEqContext(val displayName: String, val description: String) {
    ALL_AROUND("All-Around Music", "Standard musical response with balanced dynamic range"),
    PODCAST_SPEECH("Podcast & Speech", "Enhanced vocal presence and speech clarity with low-end rumble attenuation"),
    GAMING("Gaming & Immersion", "Crisp high-frequency positional cues and explosive bass texture"),
    CINEMA_MOVIES("Cinema & Movies", "Deep cinematic low-end with centered dialog clarity")
}

enum class SmartEqIntensity(val displayName: String, val factor: Float) {
    SUBTLE("Subtle", 0.6f),
    BALANCED("Balanced", 1.0f),
    DYNAMIC("Dynamic", 1.4f)
}

data class SmartEqResult(
    val goal: ListeningGoal,
    val context: SmartEqContext,
    val intensity: SmartEqIntensity,
    val bandGainsDb: List<Float>, // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostPercent: Int,
    val trebleGainDb: Float,
    val preampGainDb: Float,
    val balance: Float,
    val headroomAnalysis: HeadroomAnalysis,
    val validationResult: PresetValidationResult,
    val summaryText: String,
    val capabilityAdjustments: List<String>
)

object SmartEqGenerator {

    /**
     * Deterministically computes an EQ and DSP parameter set based on user preference,
     * listening context, intensity factor, and device hardware capabilities.
     * Note: This generates tuning configurations from user preference; it does NOT
     * claim to perform acoustic measurement of earphone hardware.
     */
    fun generate(
        goal: ListeningGoal,
        context: SmartEqContext = SmartEqContext.ALL_AROUND,
        intensity: SmartEqIntensity = SmartEqIntensity.BALANCED,
        capabilities: AudioCapabilities? = null,
        headroomMode: HeadroomMode = HeadroomMode.AUTOMATIC
    ): SmartEqResult {
        val f = intensity.factor
        val adjustments = mutableListOf<String>()

        // 1. Base Goal Tuning (gains for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
        var rawBands: MutableList<Float> = when (goal) {
            ListeningGoal.BALANCED -> mutableListOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            ListeningGoal.BASS_FOCUS -> mutableListOf(3.0f * f, 1.8f * f, 0.0f, 0.0f, 0.5f * f)
            ListeningGoal.VOCAL_FOCUS -> mutableListOf(-1.5f * f, 0.5f * f, 2.5f * f, 1.5f * f, 0.0f)
            ListeningGoal.DETAIL -> mutableListOf(0.0f, 0.0f, 0.5f * f, 2.0f * f, 3.0f * f)
            ListeningGoal.WARM -> mutableListOf(2.0f * f, 1.5f * f, 0.5f * f, -0.5f * f, -1.5f * f)
            ListeningGoal.BRIGHT -> mutableListOf(-1.0f * f, 0.0f, 1.0f * f, 2.5f * f, 3.5f * f)
            ListeningGoal.RELAXED -> mutableListOf(-0.5f * f, 0.5f * f, -0.5f * f, -1.0f * f, -1.5f * f)
        }

        var rawBassPercent = when (goal) {
            ListeningGoal.BASS_FOCUS -> (30 * f).toInt().coerceIn(0, 100)
            ListeningGoal.WARM -> (15 * f).toInt().coerceIn(0, 100)
            else -> 0
        }

        var rawTrebleDb = when (goal) {
            ListeningGoal.DETAIL -> 1.5f * f
            ListeningGoal.BRIGHT -> 2.0f * f
            ListeningGoal.WARM -> -1.0f * f
            ListeningGoal.RELAXED -> -0.5f * f
            ListeningGoal.VOCAL_FOCUS -> 0.5f * f
            else -> 0.0f
        }

        // 2. Context Adjustments
        when (context) {
            SmartEqContext.ALL_AROUND -> {
                // Keep base goal
            }
            SmartEqContext.PODCAST_SPEECH -> {
                rawBands[0] = (rawBands[0] - 2.0f).coerceIn(-15.0f, 15.0f) // Cut rumble
                rawBands[2] = (rawBands[2] + 1.5f).coerceIn(-15.0f, 15.0f) // Boost speech band
                rawBassPercent = 0
            }
            SmartEqContext.GAMING -> {
                rawBands[0] = (rawBands[0] + 1.0f).coerceIn(-15.0f, 15.0f)
                rawBands[3] = (rawBands[3] + 1.5f).coerceIn(-15.0f, 15.0f) // Boost footsteps / audio cues
                rawTrebleDb = (rawTrebleDb + 0.5f).coerceIn(-10.0f, 10.0f)
            }
            SmartEqContext.CINEMA_MOVIES -> {
                rawBands[0] = (rawBands[0] + 2.0f).coerceIn(-15.0f, 15.0f)
                rawBands[2] = (rawBands[2] + 1.0f).coerceIn(-15.0f, 15.0f) // Keep dialog audible
                rawBassPercent = (rawBassPercent + 15).coerceIn(0, 100)
            }
        }

        // 3. Capability-Aware Adjustment
        val hasBassBoost = capabilities?.isBassBoostAvailable ?: true
        if (!hasBassBoost && rawBassPercent > 0) {
            // Hardware lacks BassBoost filter: redirect bass emphasis into low-frequency EQ bands (60Hz & 230Hz)
            val redirectedBoost = (rawBassPercent / 100.0f) * 3.5f
            rawBands[0] = (rawBands[0] + redirectedBoost).coerceIn(-15.0f, 15.0f)
            rawBands[1] = (rawBands[1] + redirectedBoost * 0.5f).coerceIn(-15.0f, 15.0f)
            rawBassPercent = 0
            adjustments.add("BassBoost unavailable on current hardware: redirected low-end contouring into 60 Hz and 230 Hz bands.")
        }

        val hasTreble = capabilities?.isTrebleAvailable ?: true
        if (!hasTreble && rawTrebleDb != 0.0f) {
            // Hardware lacks Treble shelf: redirect treble into 3.6kHz and 14kHz bands
            rawBands[3] = (rawBands[3] + rawTrebleDb * 0.5f).coerceIn(-15.0f, 15.0f)
            rawBands[4] = (rawBands[4] + rawTrebleDb).coerceIn(-15.0f, 15.0f)
            rawTrebleDb = 0.0f
            adjustments.add("Treble shelf effect unavailable: redirected high-frequency shaping into 3.6 kHz and 14 kHz bands.")
        }

        // Round band gains to 0.5 dB steps for clean readability
        val finalBands = rawBands.map { Math.round(it * 2.0f) / 2.0f }
        val finalTreble = Math.round(rawTrebleDb * 2.0f) / 2.0f
        val finalPreamp = 0.0f
        val finalBalance = 0.0f

        // 4. Headroom Analysis & Validation
        val headroomAnalysis = HeadroomCalculator.analyze(
            bandGainsDb = finalBands,
            bassStrengthPercent = rawBassPercent,
            trebleGainDb = finalTreble,
            preampGainDb = finalPreamp,
            headroomMode = headroomMode
        )

        val validation = PresetSafetyValidator.validateCustomCurve(
            bandGainsDb = finalBands,
            bassBoostPercent = rawBassPercent,
            trebleGainDb = finalTreble,
            preampGainDb = finalPreamp,
            balance = finalBalance
        )

        val summary = "Tuned for '${goal.displayName}' in ${context.displayName} mode with ${intensity.displayName} intensity. Peak boost: ${String.format("%+.1f", headroomAnalysis.totalAccumulatedGainDb)} dB."

        return SmartEqResult(
            goal = goal,
            context = context,
            intensity = intensity,
            bandGainsDb = finalBands,
            bassBoostPercent = rawBassPercent,
            trebleGainDb = finalTreble,
            preampGainDb = finalPreamp,
            balance = finalBalance,
            headroomAnalysis = headroomAnalysis,
            validationResult = validation,
            summaryText = summary,
            capabilityAdjustments = adjustments
        )
    }
}
