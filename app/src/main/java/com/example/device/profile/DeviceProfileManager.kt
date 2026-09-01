package com.example.device.profile

import com.example.audio.AudioEngine
import com.example.audio.model.AudioEngineConfig
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.core.result.AudioError
import com.example.core.result.AudioResult
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceIdentity
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.model.IdentityStrength
import com.example.device.model.ProfileApplyResult
import com.example.device.model.ProfileApplyStatus
import com.example.device.model.ProfileMatchType
import com.example.settings.SettingsRepository
import com.example.settings.model.BandSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface DeviceProfileManager {
    val profiles: StateFlow<List<DeviceProfile>>
    val lastApplyResult: StateFlow<ProfileApplyResult?>
    val defaultProfileId: StateFlow<String?>

    fun resolveDeviceIdentity(device: AudioDevice): DeviceIdentity
    fun matchProfileForDevice(device: AudioDevice): Pair<DeviceProfile?, ProfileMatchType>
    fun saveProfile(profile: DeviceProfile): AudioResult<DeviceProfile>
    fun deleteProfile(profileId: String): AudioResult<Unit>
    fun setDefaultProfile(profileId: String?): AudioResult<Unit>
    fun toggleAutoApply(profileId: String, enabled: Boolean): AudioResult<DeviceProfile>
    fun applyProfile(
        device: AudioDevice,
        profile: DeviceProfile,
        audioEngine: AudioEngine,
        settingsRepo: SettingsRepository
    ): ProfileApplyResult
    fun onRouteChanged(
        device: AudioDevice,
        audioEngine: AudioEngine,
        settingsRepo: SettingsRepository
    ): ProfileApplyResult?
}

class DefaultDeviceProfileManager(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : DeviceProfileManager {

    private val _profiles = MutableStateFlow<List<DeviceProfile>>(emptyList())
    override val profiles: StateFlow<List<DeviceProfile>> = _profiles.asStateFlow()

    private val _lastApplyResult = MutableStateFlow<ProfileApplyResult?>(null)
    override val lastApplyResult: StateFlow<ProfileApplyResult?> = _lastApplyResult.asStateFlow()

    private val _defaultProfileId = MutableStateFlow<String?>(null)
    override val defaultProfileId: StateFlow<String?> = _defaultProfileId.asStateFlow()

    // Rapid route transition tracking to prevent race conditions
    private var lastRouteDevice: AudioDevice? = null
    private var lastRouteTimestamp = 0L

    init {
        loadProfilesFromSettings()
    }

    private fun loadProfilesFromSettings() {
        val loaded = settingsRepository.getDeviceProfiles()
        _profiles.value = if (loaded.isEmpty()) {
            val defaults = DeviceProfile.defaultProfiles()
            defaults.forEach { settingsRepository.saveDeviceProfile(it) }
            defaults
        } else {
            loaded
        }
        _defaultProfileId.value = settingsRepository.getDefaultProfileId()
        AppLogger.i(LogCategory.DEVICE, TAG, "Loaded ${_profiles.value.size} device profiles")
    }

    override fun resolveDeviceIdentity(device: AudioDevice): DeviceIdentity {
        val btInfo = device.bluetoothInfo
        return when {
            btInfo != null && btInfo.safeIdentifier.isNotBlank() -> {
                DeviceIdentity(
                    primaryId = btInfo.safeIdentifier,
                    productName = device.name.takeIf { it != device.type.displayName },
                    deviceType = device.type,
                    strength = IdentityStrength.STRONG,
                    bluetoothAddress = btInfo.safeIdentifier
                )
            }
            device.name.isNotBlank() && device.name != device.type.displayName -> {
                DeviceIdentity(
                    primaryId = device.id,
                    productName = device.name,
                    deviceType = device.type,
                    strength = IdentityStrength.WEAK
                )
            }
            else -> {
                DeviceIdentity(
                    primaryId = device.id,
                    productName = null,
                    deviceType = device.type,
                    strength = IdentityStrength.FALLBACK
                )
            }
        }
    }

    override fun matchProfileForDevice(device: AudioDevice): Pair<DeviceProfile?, ProfileMatchType> {
        val allProfiles = _profiles.value
        val identity = resolveDeviceIdentity(device)

        // 1. Exact Match: targetDeviceId matches primaryId or Bluetooth identifier
        val exactMatch = allProfiles.firstOrNull { profile ->
            !profile.isGenericFallback &&
                    profile.targetDeviceId != null &&
                    (profile.targetDeviceId == identity.primaryId ||
                            (identity.bluetoothAddress != null && profile.targetDeviceId == identity.bluetoothAddress) ||
                            profile.targetDeviceId == device.id)
        }
        if (exactMatch != null) {
            AppLogger.d(LogCategory.DEVICE, TAG, "Found EXACT_MATCH: ${exactMatch.name} for device ${device.name}")
            return Pair(exactMatch, ProfileMatchType.EXACT_MATCH)
        }

        // 2. Strong / Product Name Match
        if (!identity.productName.isNullOrBlank()) {
            val cleanIdentityProduct = identity.productName.trim()
            val productMatch = allProfiles.firstOrNull { profile ->
                if (profile.isGenericFallback || profile.targetProductName.isNullOrBlank()) {
                    false
                } else {
                    val cleanTarget = profile.targetProductName.trim()
                    (cleanTarget.equals(cleanIdentityProduct, ignoreCase = true) ||
                            cleanIdentityProduct.contains(cleanTarget, ignoreCase = true) ||
                            cleanTarget.contains(cleanIdentityProduct, ignoreCase = true)) &&
                            profile.deviceType == device.type
                }
            }
            if (productMatch != null) {
                AppLogger.d(LogCategory.DEVICE, TAG, "Found STRONG_MATCH: ${productMatch.name} for product ${identity.productName}")
                return Pair(productMatch, ProfileMatchType.STRONG_MATCH)
            }
        }

        // 3. Fallback Profile Match for specific DeviceType (e.g. Generic Wired Earphones)
        val fallbackMatch = allProfiles.firstOrNull { profile ->
            profile.isGenericFallback && profile.deviceType == device.type
        } ?: allProfiles.firstOrNull { profile ->
            // If device is wired headset/headphones, match generic wired
            profile.isGenericFallback &&
                    device.type.isHeadphoneOrEarphone &&
                    profile.deviceType.isHeadphoneOrEarphone &&
                    (profile.deviceType.isBluetooth == device.type.isBluetooth)
        }
        if (fallbackMatch != null) {
            AppLogger.d(LogCategory.DEVICE, TAG, "Found FALLBACK_MATCH: ${fallbackMatch.name} for type ${device.type}")
            return Pair(fallbackMatch, ProfileMatchType.FALLBACK_MATCH)
        }

        // 4. Global Default Audio Profile (if set and device is an earphone/headphone/USB)
        val defaultId = _defaultProfileId.value
        if (defaultId != null && device.type != DeviceType.BUILTIN_SPEAKER) {
            val defaultProfile = allProfiles.firstOrNull { it.id == defaultId }
            if (defaultProfile != null) {
                AppLogger.d(LogCategory.DEVICE, TAG, "Found DEFAULT_PROFILE_MATCH: ${defaultProfile.name}")
                return Pair(defaultProfile, ProfileMatchType.DEFAULT_PROFILE_MATCH)
            }
        }

        // 5. No Match Found
        AppLogger.d(LogCategory.DEVICE, TAG, "NO_MATCH for device ${device.name} (${device.type})")
        return Pair(null, ProfileMatchType.NO_MATCH)
    }

    override fun saveProfile(profile: DeviceProfile): AudioResult<DeviceProfile> {
        val sanitizedGains = profile.bandGainsDb.map { it.coerceIn(-12.0f, 12.0f) }
        val finalProfile = profile.copy(
            bandGainsDb = if (sanitizedGains.size == 5) sanitizedGains else listOf(0f, 0f, 0f, 0f, 0f),
            bassBoostPercent = profile.bassBoostPercent.coerceIn(0, 100),
            trebleGainDb = profile.trebleGainDb.coerceIn(-10.0f, 10.0f),
            preampGainDb = profile.preampGainDb.coerceIn(-12.0f, 12.0f),
            balance = profile.balance.coerceIn(-1.0f, 1.0f),
            lastUsedTimestamp = System.currentTimeMillis()
        )

        val currentList = _profiles.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == finalProfile.id }
        if (index >= 0) {
            currentList[index] = finalProfile
        } else {
            currentList.add(finalProfile)
        }
        _profiles.value = currentList
        settingsRepository.saveDeviceProfile(finalProfile)

        if (finalProfile.isDefaultAudioProfile) {
            setDefaultProfile(finalProfile.id)
        }

        AppLogger.i(LogCategory.DEVICE, TAG, "Saved device profile: ${finalProfile.name} (id=${finalProfile.id})")
        return AudioResult.Success(finalProfile)
    }

    override fun deleteProfile(profileId: String): AudioResult<Unit> {
        val currentList = _profiles.value.toMutableList()
        val profile = currentList.find { it.id == profileId }
            ?: return AudioResult.Failure(AudioError.InvalidParameter("Profile not found: $profileId"))

        if (profile.isGenericFallback) {
            return AudioResult.Failure(AudioError.InvalidParameter("Generic system fallback profiles cannot be deleted"))
        }

        currentList.removeAll { it.id == profileId }
        _profiles.value = currentList
        settingsRepository.deleteDeviceProfile(profileId)

        if (_defaultProfileId.value == profileId) {
            setDefaultProfile(null)
        }

        AppLogger.i(LogCategory.DEVICE, TAG, "Deleted device profile: ${profile.name}")
        return AudioResult.Success(Unit)
    }

    override fun setDefaultProfile(profileId: String?): AudioResult<Unit> {
        _defaultProfileId.value = profileId
        settingsRepository.setDefaultProfileId(profileId)
        // Update isDefaultAudioProfile flag on list
        val updated = _profiles.value.map {
            it.copy(isDefaultAudioProfile = (it.id == profileId))
        }
        _profiles.value = updated
        AppLogger.i(LogCategory.DEVICE, TAG, "Set default audio profile to: $profileId")
        return AudioResult.Success(Unit)
    }

    override fun toggleAutoApply(profileId: String, enabled: Boolean): AudioResult<DeviceProfile> {
        val found = _profiles.value.find { it.id == profileId }
            ?: return AudioResult.Failure(AudioError.InvalidParameter("Profile not found: $profileId"))

        val updated = found.copy(autoApplyEnabled = enabled)
        return saveProfile(updated)
    }

    override fun applyProfile(
        device: AudioDevice,
        profile: DeviceProfile,
        audioEngine: AudioEngine,
        settingsRepo: SettingsRepository
    ): ProfileApplyResult {
        AppLogger.i(
            LogCategory.DEVICE,
            TAG,
            "Applying profile '${profile.name}' to device '${device.name}' (autoApply=${profile.autoApplyEnabled})"
        )

        val appliedParams = mutableListOf<String>()
        val skippedParams = mutableListOf<String>()
        var skippedReason: String? = null

        // 1. Validate & clamp values
        val capabilities = audioEngine.capabilities
        val clampedGains = (if (profile.bandGainsDb.size == 5) profile.bandGainsDb else listOf(0f, 0f, 0f, 0f, 0f))
            .map { it.coerceIn(capabilities.minGainDb, capabilities.maxGainDb) }
        val clampedBass = profile.bassBoostPercent.coerceIn(0, 100)
        val clampedTreble = profile.trebleGainDb.coerceIn(-10.0f, 10.0f)
        val clampedPreamp = profile.preampGainDb.coerceIn(-12.0f, 12.0f)
        val clampedBalance = profile.balance.coerceIn(-1.0f, 1.0f)

        // 2. Evaluate Capabilities & Parameter Application

        // Select the associated preset in repository first as a base
        settingsRepo.selectPreset(profile.presetId)

        // Equalizer bands
        if (capabilities.supportedBands.isNotEmpty()) {
            clampedGains.forEachIndexed { idx, gain ->
                settingsRepo.updateBand(idx, gain)
            }
            appliedParams.add("5-Band EQ (${clampedGains.map { String.format("%+.1fdB", it) }.joinToString()})")
        } else {
            skippedParams.add("5-Band EQ")
            skippedReason = "Audio hardware does not report supported equalizer bands"
        }

        // BassBoost
        if (clampedBass > 0) {
            if (capabilities.isBassBoostAvailable) {
                settingsRepo.setBassLevel(clampedBass / 100.0f)
                appliedParams.add("Bass Boost ($clampedBass%)")
            } else {
                skippedParams.add("Bass Boost ($clampedBass%)")
                val reason = "Hardware BassBoost audio effect is unavailable on this device HAL"
                skippedReason = if (skippedReason == null) reason else "$skippedReason; $reason"
                // Set repository bass to 0 to prevent misleading UI
                settingsRepo.setBassLevel(0.0f)
            }
        } else {
            settingsRepo.setBassLevel(0.0f)
            appliedParams.add("Bass Boost (0%)")
        }

        // Treble Tone
        settingsRepo.setTrebleLevel(clampedTreble)
        appliedParams.add(String.format("Treble Tone (%+.1f dB)", clampedTreble))

        // Preamp
        settingsRepo.setPreampLevel(clampedPreamp)
        appliedParams.add(String.format("Preamp (%+.1f dB)", clampedPreamp))

        // Balance
        settingsRepo.setBalance(clampedBalance)
        appliedParams.add(String.format("Stereo Balance (%+.2f)", clampedBalance))

        // 3. Coordinate with AudioEngine
        val currentSettings = settingsRepo.settings.value
        val headroomAnalysis = com.example.audio.safety.HeadroomCalculator.analyze(
            bandGainsDb = clampedGains,
            bassStrengthPercent = if (capabilities.isBassBoostAvailable) clampedBass else 0,
            trebleGainDb = clampedTreble,
            preampGainDb = clampedPreamp,
            isAutoHeadroomEnabled = currentSettings.isAutoHeadroomEnabled
        )

        if (headroomAnalysis.isAutoHeadroomActive) {
            appliedParams.add(String.format("Auto Headroom (%+.1f dB attenuation)", headroomAnalysis.autoHeadroomOffsetDb))
        }

        val isEngineEnabled = currentSettings.isEnabled
        val engineConfig = AudioEngineConfig(
            isEnabled = isEngineEnabled,
            bandGainsDb = clampedGains.mapIndexed { index, gain -> index to gain }.toMap(),
            bassStrengthPercent = if (capabilities.isBassBoostAvailable) clampedBass else 0,
            trebleGainDb = clampedTreble,
            preampGainDb = clampedPreamp,
            stereoBalance = clampedBalance,
            autoHeadroomOffsetDb = headroomAnalysis.autoHeadroomOffsetDb
        )
        val engineResult = audioEngine.applyConfiguration(engineConfig)

        val finalStatus = when {
            engineResult is AudioResult.Failure -> ProfileApplyStatus.FAILED
            skippedParams.isNotEmpty() -> ProfileApplyStatus.PARTIAL
            else -> ProfileApplyStatus.SUCCESS
        }

        val result = ProfileApplyResult(
            status = finalStatus,
            profileId = profile.id,
            profileName = profile.name,
            matchType = matchProfileForDevice(device).second,
            deviceName = device.name,
            deviceType = device.type,
            appliedParameters = appliedParams,
            skippedParameters = skippedParams,
            reasonForSkipped = skippedReason
        )

        _lastApplyResult.value = result
        AppLogger.i(
            LogCategory.DEVICE,
            TAG,
            "Profile apply complete: status=$finalStatus, applied=${appliedParams.size}, skipped=${skippedParams.size}"
        )

        return result
    }

    override fun onRouteChanged(
        device: AudioDevice,
        audioEngine: AudioEngine,
        settingsRepo: SettingsRepository
    ): ProfileApplyResult? {
        val now = System.currentTimeMillis()
        if (lastRouteDevice?.id == device.id && now - lastRouteTimestamp < 150) {
            AppLogger.d(LogCategory.DEVICE, TAG, "Ignoring duplicate rapid route event for device: ${device.name}")
            return _lastApplyResult.value
        }
        lastRouteDevice = device
        lastRouteTimestamp = now

        AppLogger.i(LogCategory.DEVICE, TAG, "Handling route change -> ${device.name} (${device.type})")
        audioEngine.onDeviceChanged(device)

        // Don't auto-apply earphone profiles to Phone Speaker unless explicitly configured
        if (device.type == DeviceType.BUILTIN_SPEAKER) {
            AppLogger.i(LogCategory.DEVICE, TAG, "Active route is Phone Speaker; retaining current speaker settings.")
            val speakerResult = ProfileApplyResult(
                status = ProfileApplyStatus.SUCCESS,
                profileId = null,
                profileName = "Built-in Speaker Reference",
                matchType = ProfileMatchType.NO_MATCH,
                deviceName = device.name,
                deviceType = device.type,
                appliedParameters = listOf("Built-in Speaker Safe Routing"),
                skippedParameters = emptyList()
            )
            _lastApplyResult.value = speakerResult
            return speakerResult
        }

        val (matchedProfile, matchType) = matchProfileForDevice(device)
        if (matchedProfile != null && matchedProfile.autoApplyEnabled) {
            return applyProfile(device, matchedProfile, audioEngine, settingsRepo)
        } else if (matchedProfile != null && !matchedProfile.autoApplyEnabled) {
            AppLogger.i(LogCategory.DEVICE, TAG, "Found profile '${matchedProfile.name}' but autoApply is OFF")
            val skippedResult = ProfileApplyResult(
                status = ProfileApplyStatus.SUCCESS,
                profileId = matchedProfile.id,
                profileName = matchedProfile.name,
                matchType = matchType,
                deviceName = device.name,
                deviceType = device.type,
                appliedParameters = emptyList(),
                skippedParameters = listOf("Auto-apply disabled for this profile"),
                reasonForSkipped = "User disabled automatic profile application for '${matchedProfile.name}'"
            )
            _lastApplyResult.value = skippedResult
            return skippedResult
        } else {
            AppLogger.i(LogCategory.DEVICE, TAG, "No auto-applicable profile found for device: ${device.name}")
            return null
        }
    }

    companion object {
        private const val TAG = "DeviceProfileMgr"

        fun validateProfileSafety(profile: DeviceProfile): DeviceProfile {
            val clampedGains = (if (profile.bandGainsDb.size == 5) profile.bandGainsDb else listOf(0f, 0f, 0f, 0f, 0f))
                .map { it.coerceIn(-15.0f, 15.0f) }
            return profile.copy(
                bandGainsDb = clampedGains,
                bassBoostPercent = profile.bassBoostPercent.coerceIn(0, 100),
                trebleGainDb = profile.trebleGainDb.coerceIn(-10.0f, 10.0f),
                preampGainDb = profile.preampGainDb.coerceIn(-12.0f, 6.0f),
                balance = profile.balance.coerceIn(-1.0f, 1.0f)
            )
        }
    }
}
