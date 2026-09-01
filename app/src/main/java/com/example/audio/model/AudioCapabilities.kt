package com.example.audio.model

/**
 * Audio Session information abstraction.
 * Android audio effects require an audioSessionId (e.g. 0 for global output mix,
 * or specific session ID from media players).
 */
data class AudioSessionInfo(
    val sessionId: Int = GLOBAL_SESSION_ID,
    val isGlobalMix: Boolean = (sessionId == GLOBAL_SESSION_ID),
    val priority: Int = 0,
    val targetAudioAttributes: String? = "USAGE_MEDIA"
) {
    companion object {
        const val GLOBAL_SESSION_ID = 0
        val GLOBAL = AudioSessionInfo(sessionId = GLOBAL_SESSION_ID, isGlobalMix = true)
    }
}

/**
 * Represents a single frequency band in the equalizer.
 */
data class EqualizerBand(
    val index: Int,
    val centerFrequencyHz: Int,
    val minGainMilliBels: Int = -1500, // -15.0 dB
    val maxGainMilliBels: Int = 1500,  // +15.0 dB
    val currentGainMilliBels: Int = 0,
    val label: String = formatFrequency(centerFrequencyHz)
) {
    val gainDb: Float
        get() = currentGainMilliBels / 100.0f

    companion object {
        fun formatFrequency(freqHz: Int): String {
            return if (freqHz >= 1000) {
                val kHz = freqHz / 1000.0
                if (kHz % 1.0 == 0.0) "${kHz.toInt()}kHz" else "${kHz}kHz"
            } else {
                "${freqHz}Hz"
            }
        }

        fun default5Bands(): List<EqualizerBand> = listOf(
            EqualizerBand(index = 0, centerFrequencyHz = 60),
            EqualizerBand(index = 1, centerFrequencyHz = 230),
            EqualizerBand(index = 2, centerFrequencyHz = 910),
            EqualizerBand(index = 3, centerFrequencyHz = 3600),
            EqualizerBand(index = 4, centerFrequencyHz = 14000)
        )
    }
}

/**
 * Full audio capability assessment for the device and Android OS version.
 */
data class AudioCapabilities(
    val overallState: com.example.core.result.AudioCapabilityState,
    val isSystemEqualizerAvailable: Boolean,
    val isBassBoostAvailable: Boolean,
    val isTrebleAvailable: Boolean,
    val isVirtualizerAvailable: Boolean,
    val isPreampGainAvailable: Boolean,
    val maxBands: Int = 5,
    val minGainDb: Float = -15.0f,
    val maxGainDb: Float = 15.0f,
    val supportedBands: List<EqualizerBand> = EqualizerBand.default5Bands(),
    val androidApiLevel: Int = 34,
    val hardwareManufacturer: String = "Android Device",
    val deviceModel: String = "Generic Model",
    val limitationNote: String = "Phase 1 Foundation: Architectural abstraction verified. Real DSP engine will attach in Phase 2."
)
