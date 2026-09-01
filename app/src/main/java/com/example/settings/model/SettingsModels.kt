package com.example.settings.model

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

data class BandSetting(
    val bandIndex: Int,
    val centerFrequencyHz: Int,
    val gainDb: Float = 0.0f
)

data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val bandGainsDb: List<Float>, // Gains corresponding to 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostPercent: Int = 0,
    val trebleGainDb: Float = 0.0f,
    val preampGainDb: Float = 0.0f,
    val balance: Float = 0.0f,
    val isCustom: Boolean = false
) {
    val maxPositiveBoostDb: Float
        get() = (bandGainsDb.filter { it > 0f }.maxOrNull() ?: 0f) +
                ((bassBoostPercent / 100f) * 6.0f) +
                trebleGainDb.coerceAtLeast(0f) +
                preampGainDb.coerceAtLeast(0f)

    val clippingRisk: com.example.audio.safety.ClippingRisk
        get() = when {
            maxPositiveBoostDb > com.example.audio.safety.HeadroomCalculator.HIGH_RISK_THRESHOLD_DB -> com.example.audio.safety.ClippingRisk.HIGH_RISK
            maxPositiveBoostDb > com.example.audio.safety.HeadroomCalculator.WARNING_THRESHOLD_DB -> com.example.audio.safety.ClippingRisk.WARNING
            else -> com.example.audio.safety.ClippingRisk.SAFE
        }

    companion object {
        val FLAT = Preset(
            id = "preset_flat",
            name = "Flat",
            description = "Reference flat response without frequency alteration",
            bandGainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            bassBoostPercent = 0,
            trebleGainDb = 0.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val BASS_BOOST = Preset(
            id = "preset_bass_boost",
            name = "Bass Boost",
            description = "Enhanced low-end punch for deep sub-bass and kick drums",
            bandGainsDb = listOf(6.0f, 4.0f, 1.0f, 0.0f, -1.0f),
            bassBoostPercent = 50,
            trebleGainDb = 0.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val VOCAL_ENHANCE = Preset(
            id = "preset_vocal",
            name = "Vocal Clarity",
            description = "Emphasized mid-range for clear speech and crisp vocals",
            bandGainsDb = listOf(-2.0f, 1.0f, 4.5f, 3.0f, 1.0f),
            bassBoostPercent = 0,
            trebleGainDb = 1.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val TREBLE_BOOST = Preset(
            id = "preset_treble_boost",
            name = "Treble Boost",
            description = "Crisp and airy high frequency brightness for acoustic details",
            bandGainsDb = listOf(-2.0f, 0.0f, 1.0f, 4.0f, 6.0f),
            bassBoostPercent = 0,
            trebleGainDb = 4.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val ROCK = Preset(
            id = "preset_rock",
            name = "Rock",
            description = "Classic V-curve with pronounced punch and aggressive top end",
            bandGainsDb = listOf(5.0f, 2.5f, -1.5f, 3.0f, 4.5f),
            bassBoostPercent = 25,
            trebleGainDb = 2.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val ACOUSTIC = Preset(
            id = "preset_acoustic",
            name = "Acoustic",
            description = "Smooth natural warmth and wide instrumental staging",
            bandGainsDb = listOf(2.0f, 1.5f, 1.0f, 2.5f, 3.0f),
            bassBoostPercent = 10,
            trebleGainDb = 1.5f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )
        val ELECTRONIC = Preset(
            id = "preset_electronic",
            name = "Electronic",
            description = "Deep sub-bass extension and sparkling synth brilliance",
            bandGainsDb = listOf(6.5f, 3.0f, 0.0f, 2.0f, 5.0f),
            bassBoostPercent = 40,
            trebleGainDb = 2.5f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            isCustom = false
        )

        fun defaultPresets(): List<Preset> = listOf(
            FLAT, BASS_BOOST, VOCAL_ENHANCE, TREBLE_BOOST, ROCK, ACOUSTIC, ELECTRONIC
        )
    }
}

data class EarphoneProfile(
    val id: String,
    val name: String,
    val category: String,
    val targetCompensation: String,
    val recommendedPresetId: String,
    val defaultBassBoost: Int = 0,
    val defaultTrebleGain: Float = 0.0f
) {
    companion object {
        val GENERIC_IN_EAR = EarphoneProfile(
            id = "profile_generic_in_ear",
            name = "Generic In-Ear (IEM)",
            category = "In-Ear Monitors",
            targetCompensation = "Harman In-Ear Target",
            recommendedPresetId = "preset_flat",
            defaultBassBoost = 15
        )
        val GENERIC_OVER_EAR = EarphoneProfile(
            id = "profile_generic_over_ear",
            name = "Generic Over-Ear / Circumaural",
            category = "Over-Ear Headphones",
            targetCompensation = "Diffuse Field Target",
            recommendedPresetId = "preset_acoustic"
        )
        val GENERIC_EARBUDS = EarphoneProfile(
            id = "profile_generic_earbuds",
            name = "Open-Fit Earbuds",
            category = "Open Earphones",
            targetCompensation = "Low-end compensation",
            recommendedPresetId = "preset_bass_boost",
            defaultBassBoost = 35
        )
        val GENERIC_BT_SPEAKER = EarphoneProfile(
            id = "profile_generic_speaker",
            name = "Bluetooth Speaker / Portable",
            category = "External Speaker",
            targetCompensation = "Room compensation",
            recommendedPresetId = "preset_vocal"
        )

        fun defaultProfiles(): List<EarphoneProfile> = listOf(
            GENERIC_IN_EAR, GENERIC_OVER_EAR, GENERIC_EARBUDS, GENERIC_BT_SPEAKER
        )
    }
}

data class EqualizerSettings(
    val isEnabled: Boolean = false,
    val selectedPresetId: String = Preset.FLAT.id,
    val bands: List<BandSetting> = listOf(
        BandSetting(0, 60, 0.0f),
        BandSetting(1, 230, 0.0f),
        BandSetting(2, 910, 0.0f),
        BandSetting(3, 3600, 0.0f),
        BandSetting(4, 14000, 0.0f)
    ),
    val bassLevel: Float = 0.0f,          // 0.0f to 1.0f (0% to 100%)
    val trebleLevel: Float = 0.0f,        // -10.0f to +10.0f dB
    val preampLevel: Float = 0.0f,        // -12.0f to +12.0f dB
    val balance: Float = 0.0f,            // -1.0f (Left) to +1.0f (Right)
    val headroomMode: HeadroomMode = HeadroomMode.AUTOMATIC,
    val manualHeadroomDb: Float = 0.0f,   // 0.0f to -12.0f dB
    val isAutoHeadroomEnabled: Boolean = true,
    val selectedListeningProfileId: String? = null,
    val selectedEarphoneProfileId: String = EarphoneProfile.GENERIC_IN_EAR.id,
    val autoApplyProfile: Boolean = true,
    val devicePresetMap: Map<String, String> = emptyMap(), // deviceId or type -> presetId
    val themePreference: ThemePreference = ThemePreference.SYSTEM
)
