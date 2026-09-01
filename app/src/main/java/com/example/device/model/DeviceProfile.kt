package com.example.device.model

import com.example.settings.model.Preset
import java.util.UUID

/**
 * Indicates how confident the system is in identifying the device hardware.
 */
enum class IdentityStrength(val displayName: String) {
    STRONG("Strong Hardware Identity"),
    WEAK("Product / Type Identity"),
    FALLBACK("Generic Type Fallback")
}

/**
 * Resolved device identity characteristics for safe matching.
 */
data class DeviceIdentity(
    val primaryId: String,
    val productName: String? = null,
    val deviceType: DeviceType,
    val strength: IdentityStrength = IdentityStrength.WEAK,
    val bluetoothAddress: String? = null
)

/**
 * Categorization of matching confidence when resolving profiles.
 */
enum class ProfileMatchType(val displayName: String) {
    EXACT_MATCH("Exact Hardware Match"),
    STRONG_MATCH("Product Identity Match"),
    FALLBACK_MATCH("Generic Type Match"),
    DEFAULT_PROFILE_MATCH("Default Fallback Profile"),
    NO_MATCH("No Matching Profile")
}

/**
 * Status of the profile application pipeline.
 */
enum class ProfileApplyStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    UNSUPPORTED
}

/**
 * Detailed diagnostic breakdown of a profile application event.
 */
data class ProfileApplyResult(
    val status: ProfileApplyStatus,
    val profileId: String?,
    val profileName: String,
    val matchType: ProfileMatchType,
    val deviceName: String,
    val deviceType: DeviceType,
    val appliedParameters: List<String>,
    val skippedParameters: List<String>,
    val reasonForSkipped: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Production Device Profile holding specific acoustic tuning for an audio output device.
 */
data class DeviceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val deviceType: DeviceType,
    val targetDeviceId: String? = null,
    val targetProductName: String? = null,
    val isGenericFallback: Boolean = false,
    val presetId: String = Preset.FLAT.id,
    val bandGainsDb: List<Float> = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
    val bassBoostPercent: Int = 0,
    val trebleGainDb: Float = 0.0f,
    val preampGainDb: Float = 0.0f,
    val balance: Float = 0.0f,
    val autoApplyEnabled: Boolean = true,
    val isDefaultAudioProfile: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val GENERIC_WIRED_FALLBACK = DeviceProfile(
            id = "profile_fallback_wired",
            name = "Generic Wired Audio",
            deviceType = DeviceType.WIRED_EARPHONES,
            isGenericFallback = true,
            presetId = Preset.VOCAL_ENHANCE.id,
            bandGainsDb = Preset.VOCAL_ENHANCE.bandGainsDb,
            bassBoostPercent = 10,
            trebleGainDb = 1.0f,
            autoApplyEnabled = true
        )

        val GENERIC_BLUETOOTH_FALLBACK = DeviceProfile(
            id = "profile_fallback_bluetooth",
            name = "Generic Bluetooth Audio",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            isGenericFallback = true,
            presetId = Preset.BASS_BOOST.id,
            bandGainsDb = Preset.BASS_BOOST.bandGainsDb,
            bassBoostPercent = 30,
            trebleGainDb = 0.0f,
            autoApplyEnabled = true
        )

        val GENERIC_USB_FALLBACK = DeviceProfile(
            id = "profile_fallback_usb",
            name = "Generic USB Audio",
            deviceType = DeviceType.USB_AUDIO,
            isGenericFallback = true,
            presetId = Preset.FLAT.id,
            bandGainsDb = Preset.FLAT.bandGainsDb,
            bassBoostPercent = 0,
            trebleGainDb = 0.0f,
            autoApplyEnabled = true
        )

        fun defaultProfiles(): List<DeviceProfile> = listOf(
            GENERIC_WIRED_FALLBACK,
            GENERIC_BLUETOOTH_FALLBACK,
            GENERIC_USB_FALLBACK
        )
    }
}
