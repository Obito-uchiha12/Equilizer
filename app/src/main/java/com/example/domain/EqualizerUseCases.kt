package com.example.domain

import com.example.audio.AudioCapabilityDetector
import com.example.audio.model.AudioCapabilities
import com.example.core.result.AudioResult
import com.example.device.AudioDeviceManager
import com.example.device.model.AudioDevice
import com.example.settings.SettingsRepository
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.EqualizerSettings
import com.example.settings.model.Preset
import kotlinx.coroutines.flow.StateFlow

class GetAudioOutputDeviceUseCase(
    private val deviceManager: AudioDeviceManager
) {
    val currentDevice: StateFlow<AudioDevice?> = deviceManager.currentOutputDevice
    val availableDevices: StateFlow<List<AudioDevice>> = deviceManager.availableDevices

    fun refresh(): AudioResult<List<AudioDevice>> = deviceManager.refreshDevices()
    fun selectDevice(deviceId: String): AudioResult<AudioDevice> = deviceManager.selectDevice(deviceId)
}

class GetAudioCapabilitiesUseCase(
    private val capabilityDetector: AudioCapabilityDetector
) {
    fun execute(): AudioCapabilities = capabilityDetector.detectCapabilities()
}

class GetEqualizerSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    val settings: StateFlow<EqualizerSettings> = settingsRepository.settings
    val presets: StateFlow<List<Preset>> = settingsRepository.availablePresets
    val profiles: StateFlow<List<EarphoneProfile>> = settingsRepository.availableProfiles
}

class UpdateEqualizerSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun setEnabled(enabled: Boolean): AudioResult<Unit> = settingsRepository.setEnabled(enabled)
    fun updateBand(bandIndex: Int, gainDb: Float): AudioResult<Unit> = settingsRepository.updateBand(bandIndex, gainDb)
    fun setBassLevel(level: Float): AudioResult<Unit> = settingsRepository.setBassLevel(level)
    fun setTrebleLevel(level: Float): AudioResult<Unit> = settingsRepository.setTrebleLevel(level)
    fun setPreampLevel(level: Float): AudioResult<Unit> = settingsRepository.setPreampLevel(level)
    fun setBalance(balance: Float): AudioResult<Unit> = settingsRepository.setBalance(balance)
    fun setAutoApplyProfile(autoApply: Boolean): AudioResult<Unit> = settingsRepository.setAutoApplyProfile(autoApply)
    fun resetToDefaults(): AudioResult<Unit> = settingsRepository.resetToDefaults()
}

class SelectPresetUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun execute(presetId: String): AudioResult<Preset> = settingsRepository.selectPreset(presetId)
}

class ApplyEarphoneProfileUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun execute(profileId: String): AudioResult<EarphoneProfile> = settingsRepository.selectEarphoneProfile(profileId)
}
