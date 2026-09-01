package com.example.settings.model

import com.example.audio.safety.ClippingRisk
import com.example.audio.safety.HeadroomCalculator

enum class ListeningGoal(val displayName: String, val subtitle: String) {
    BALANCED("Balanced", "Neutral, transparent response preserving original audio mastering"),
    BASS_FOCUS("Bass Focus", "Punchy low-end and kick drum impact with safe acoustic balance"),
    VOCAL_FOCUS("Vocal Focus", "Clear speech & midrange presence for podcasts and vocal tracks"),
    DETAIL("Detail", "High-frequency brilliance, air, and micro-detail resolution"),
    WARM("Warm", "Rich low-mid fundamentals with smooth, relaxed highs to reduce fatigue"),
    BRIGHT("Bright", "Crisp, energetic high-end profile highlighting acoustic sparkle"),
    RELAXED("Relaxed", "Gentle, non-fatiguing curve designed for long listening sessions")
}

data class ListeningProfile(
    val id: String,
    val name: String,
    val description: String,
    val goal: ListeningGoal,
    val bandGainsDb: List<Float>, // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoostPercent: Int = 0,
    val trebleGainDb: Float = 0.0f,
    val preampGainDb: Float = 0.0f,
    val balance: Float = 0.0f
) {
    val maxPositiveBoostDb: Float
        get() = (bandGainsDb.filter { it > 0f }.maxOrNull() ?: 0f) +
                ((bassBoostPercent / 100f) * 6.0f) +
                trebleGainDb.coerceAtLeast(0f) +
                preampGainDb.coerceAtLeast(0f)

    val clippingRisk: ClippingRisk
        get() = when {
            maxPositiveBoostDb > HeadroomCalculator.HIGH_RISK_THRESHOLD_DB -> ClippingRisk.HIGH_RISK
            maxPositiveBoostDb > HeadroomCalculator.WARNING_THRESHOLD_DB -> ClippingRisk.WARNING
            else -> ClippingRisk.SAFE
        }

    companion object {
        val BALANCED = ListeningProfile(
            id = "profile_balanced",
            name = "Balanced Reference",
            description = "Neutral, transparent reference response preserving artist intent and dynamic range",
            goal = ListeningGoal.BALANCED,
            bandGainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            bassBoostPercent = 0,
            trebleGainDb = 0.0f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val BASS_FOCUS = ListeningProfile(
            id = "profile_bass_focus",
            name = "Bass Focus",
            description = "Controlled low-frequency energy emphasizing sub-bass kick and bassline punch",
            goal = ListeningGoal.BASS_FOCUS,
            bandGainsDb = listOf(3.0f, 1.5f, 0.0f, 0.0f, 0.5f),
            bassBoostPercent = 30,
            trebleGainDb = 0.0f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val VOCAL_FOCUS = ListeningProfile(
            id = "profile_vocal_focus",
            name = "Vocal Focus",
            description = "Enhanced speech intelligibility and lead vocal presence for podcasts and songs",
            goal = ListeningGoal.VOCAL_FOCUS,
            bandGainsDb = listOf(-1.0f, 0.5f, 2.5f, 1.5f, 0.5f),
            bassBoostPercent = 0,
            trebleGainDb = 0.5f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val DETAIL = ListeningProfile(
            id = "profile_detail",
            name = "Detail & Air",
            description = "Subtle high-frequency extension and fine micro-detail resolution for acoustic clarity",
            goal = ListeningGoal.DETAIL,
            bandGainsDb = listOf(0.0f, 0.0f, 0.5f, 2.0f, 3.0f),
            bassBoostPercent = 0,
            trebleGainDb = 1.5f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val WARM = ListeningProfile(
            id = "profile_warm",
            name = "Warm & Smooth",
            description = "Rich lower-mid fundamentals with a gentle top-end rolloff to eliminate listening fatigue",
            goal = ListeningGoal.WARM,
            bandGainsDb = listOf(2.0f, 1.5f, 0.5f, -0.5f, -1.5f),
            bassBoostPercent = 15,
            trebleGainDb = -1.0f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val BRIGHT = ListeningProfile(
            id = "profile_bright",
            name = "Bright & Crisp",
            description = "Crisp, open brilliance highlighting acoustic guitars, cymbals, and percussion shimmer",
            goal = ListeningGoal.BRIGHT,
            bandGainsDb = listOf(-1.0f, 0.0f, 1.0f, 2.5f, 3.5f),
            bassBoostPercent = 0,
            trebleGainDb = 2.0f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        val RELAXED = ListeningProfile(
            id = "profile_relaxed",
            name = "Relaxed Listening",
            description = "Soft, non-fatiguing contour designed for extended background listening sessions",
            goal = ListeningGoal.RELAXED,
            bandGainsDb = listOf(-0.5f, 0.5f, -0.5f, -1.0f, -1.5f),
            bassBoostPercent = 0,
            trebleGainDb = -0.5f,
            preampGainDb = 0.0f,
            balance = 0.0f
        )

        fun defaultProfiles(): List<ListeningProfile> = listOf(
            BALANCED, BASS_FOCUS, VOCAL_FOCUS, DETAIL, WARM, BRIGHT, RELAXED
        )
    }
}
