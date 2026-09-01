package com.example.audio.model

enum class AudioEngineStatus {
    UNINITIALIZED,
    INITIALIZING,
    READY_FOUNDATION,
    ACTIVE,
    DISABLED,
    ERROR
}

data class AudioEngineState(
    val status: AudioEngineStatus = AudioEngineStatus.UNINITIALIZED,
    val isEnabled: Boolean = false,
    val activeSession: AudioSessionInfo = AudioSessionInfo.GLOBAL,
    val appliedBandsCount: Int = 0,
    val errorMessage: String? = null,
    // Session State Machine & Production Lifecycle (Phase 8)
    val sessionState: AudioSessionState = AudioSessionState.NO_SESSION,
    val activePackageName: String? = null,
    val recoveryAttempts: Int = 0,
    val lastFailureReason: String? = null,
    val lastRecoveryTimestamp: Long = 0L,
    val isServiceRunning: Boolean = false,
    val processId: Int = android.os.Process.myPid(),
    // Real DSP Diagnostics
    val engineImplementation: String = "Phase 1 Foundation",
    val isHardwareAttached: Boolean = false,
    val hasEffectControl: Boolean = false,
    val hardwareBandsCount: Int = 0,
    val hardwareMinGainMilliBels: Int = -1500,
    val hardwareMaxGainMilliBels: Int = 1500,
    val bandMappings: List<String> = emptyList(),
    val isBassBoostSupported: Boolean = false,
    val isBassBoostActive: Boolean = false,
    val bassBoostStrength: Int = 0,
    val lastAppliedTimestamp: Long = 0L,
    // DSP Safety & Headroom Diagnostics (Phase 6)
    val maxPositiveBoostDb: Float = 0.0f,
    val estimatedPeakGainDb: Float = 0.0f,
    val recommendedHeadroomDb: Float = 0.0f,
    val autoHeadroomOffsetDb: Float = 0.0f,
    val clippingRisk: String = "SAFE",
    val isAutoHeadroomEnabled: Boolean = true
) {
    val isAutoHeadroomActive: Boolean
        get() = autoHeadroomOffsetDb < -0.01f

    val isRecoverable: Boolean
        get() = sessionState == AudioSessionState.CONTROL_LOST ||
                sessionState == AudioSessionState.TEMPORARILY_UNAVAILABLE ||
                sessionState == AudioSessionState.ERROR
}

data class AudioEngineConfig(
    val isEnabled: Boolean,
    val bandGainsDb: Map<Int, Float>, // bandIndex -> dB
    val bassStrengthPercent: Int,     // 0..100
    val trebleGainDb: Float,          // -10..+10 dB
    val preampGainDb: Float,          // -12..+12 dB
    val stereoBalance: Float,         // -1.0 (Left) .. 1.0 (Right)
    val autoHeadroomOffsetDb: Float = 0.0f // Negative attenuation in dB (e.g. -6.0 dB)
)
