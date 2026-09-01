package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCapabilityDetector
import com.example.audio.AudioEngine
import com.example.audio.TestToneGenerator
import com.example.audio.TestToneMode
import com.example.audio.TestToneState
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineState
import com.example.audio.model.AudioSessionInfo
import com.example.audio.safety.HeadroomAnalysis
import com.example.audio.safety.HeadroomCalculator
import com.example.audio.safety.PresetSafetyValidator
import com.example.audio.safety.PresetValidationResult
import com.example.core.logging.AppLogger
import com.example.core.logging.AudioLogEntry
import com.example.core.logging.LogCategory
import com.example.core.result.AudioError
import com.example.device.AudioDeviceManager
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.model.ProfileApplyResult
import com.example.device.model.ProfileMatchType
import com.example.device.profile.DefaultDeviceProfileManager
import com.example.device.profile.DeviceProfileManager
import com.example.domain.smarteq.SmartEqContext
import com.example.domain.smarteq.SmartEqGenerator
import com.example.domain.smarteq.SmartEqIntensity
import com.example.domain.smarteq.SmartEqResult
import com.example.settings.SettingsRepository
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.EqualizerSettings
import com.example.settings.model.HeadroomMode
import com.example.settings.model.ListeningGoal
import com.example.settings.model.ListeningProfile
import com.example.settings.model.Preset
import com.example.settings.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isInitialized: Boolean = false,
    val currentDevice: AudioDevice? = null,
    val availableDevices: List<AudioDevice> = emptyList(),
    val capabilities: AudioCapabilities? = null,
    val settings: EqualizerSettings = EqualizerSettings(),
    val presets: List<Preset> = emptyList(),
    val profiles: List<EarphoneProfile> = emptyList(),
    val listeningProfiles: List<ListeningProfile> = emptyList(),
    val canUndo: Boolean = false,
    val deviceProfiles: List<DeviceProfile> = emptyList(),
    val activeDeviceProfile: DeviceProfile? = null,
    val profileMatchType: ProfileMatchType = ProfileMatchType.NO_MATCH,
    val lastProfileApplyResult: ProfileApplyResult? = null,
    val defaultProfileId: String? = null,
    val engineState: AudioEngineState = AudioEngineState(),
    val testToneState: TestToneState = TestToneState(),
    val recentLogs: List<AudioLogEntry> = emptyList(),
    // Dialog & Sheet States
    val isSettingsSheetVisible: Boolean = false,
    val isDiagnosticsVisible: Boolean = false,
    val isMyDevicesSheetVisible: Boolean = false,
    val isCreateDeviceProfileDialogVisible: Boolean = false,
    val isEditDeviceProfileDialogVisible: Boolean = false,
    val profileToEdit: DeviceProfile? = null,
    val isDeleteDeviceProfileDialogVisible: Boolean = false,
    val profileToDelete: DeviceProfile? = null,
    val isSavePresetDialogVisible: Boolean = false,
    val isRenamePresetDialogVisible: Boolean = false,
    val presetToRename: Preset? = null,
    val isDeletePresetDialogVisible: Boolean = false,
    val presetToDelete: Preset? = null,
    // Phase 7 Intelligent EQ & Safety States
    val isSmartEqDialogVisible: Boolean = false,
    val smartEqGoal: ListeningGoal = ListeningGoal.BALANCED,
    val smartEqContext: SmartEqContext = SmartEqContext.ALL_AROUND,
    val smartEqIntensity: SmartEqIntensity = SmartEqIntensity.BALANCED,
    val smartEqResult: SmartEqResult? = null,
    val isPreviewActive: Boolean = false,
    val previewTargetName: String? = null,
    val isAbComparing: Boolean = false,
    val isAuditioningB: Boolean = false, // false = A (Auditioning new EQ), true = B (Flat baseline)
    val isNormalizeConfirmDialogVisible: Boolean = false,
    val isSafetyAuditDialogVisible: Boolean = false,
    val safetyAuditResult: PresetValidationResult? = null,
    val safetyAuditPresetName: String? = null,
    val isAboutDialogVisible: Boolean = false,
    val userNotification: String? = null,
    val activeError: AudioError? = null
) {
    val activePreset: Preset?
        get() = presets.find { it.id == settings.selectedPresetId }

    val activeListeningProfile: ListeningProfile?
        get() = listeningProfiles.find { it.id == settings.selectedListeningProfileId }

    val isCustomTuning: Boolean
        get() = settings.selectedPresetId == "custom"

    val headroomAnalysis: HeadroomAnalysis
        get() = HeadroomCalculator.analyze(
            bandGainsDb = settings.bands.map { it.gainDb },
            bassStrengthPercent = (settings.bassLevel * 100).toInt(),
            trebleGainDb = settings.trebleLevel,
            preampGainDb = settings.preampLevel,
            headroomMode = settings.headroomMode,
            manualHeadroomDb = settings.manualHeadroomDb
        )
}

class MainViewModel(
    private val deviceManager: AudioDeviceManager,
    private val capabilityDetector: AudioCapabilityDetector,
    private val settingsRepository: SettingsRepository,
    private val audioEngine: AudioEngine,
    private val toneGenerator: TestToneGenerator = TestToneGenerator(),
    val profileManager: DeviceProfileManager = DefaultDeviceProfileManager(settingsRepository)
) : ViewModel() {

    private val _isSettingsSheetVisible = MutableStateFlow(false)
    private val _isDiagnosticsVisible = MutableStateFlow(false)
    private val _isMyDevicesSheetVisible = MutableStateFlow(false)
    private val _isCreateDeviceProfileDialogVisible = MutableStateFlow(false)
    private val _isEditDeviceProfileDialogVisible = MutableStateFlow(false)
    private val _profileToEdit = MutableStateFlow<DeviceProfile?>(null)
    private val _isDeleteDeviceProfileDialogVisible = MutableStateFlow(false)
    private val _profileToDelete = MutableStateFlow<DeviceProfile?>(null)

    private val _isSavePresetDialogVisible = MutableStateFlow(false)
    private val _isRenamePresetDialogVisible = MutableStateFlow(false)
    private val _presetToRename = MutableStateFlow<Preset?>(null)
    private val _isDeletePresetDialogVisible = MutableStateFlow(false)
    private val _presetToDelete = MutableStateFlow<Preset?>(null)

    // Phase 7 States
    private val _isSmartEqDialogVisible = MutableStateFlow(false)
    private val _smartEqGoal = MutableStateFlow(ListeningGoal.BALANCED)
    private val _smartEqContext = MutableStateFlow(SmartEqContext.ALL_AROUND)
    private val _smartEqIntensity = MutableStateFlow(SmartEqIntensity.BALANCED)
    private val _smartEqResult = MutableStateFlow<SmartEqResult?>(null)

    private val _isPreviewActive = MutableStateFlow(false)
    private val _previewTargetName = MutableStateFlow<String?>(null)
    private var snapshotBeforePreview: EqualizerSettings? = null
    private val _isAbComparing = MutableStateFlow(false)
    private val _isAuditioningB = MutableStateFlow(false)

    private val _isNormalizeConfirmDialogVisible = MutableStateFlow(false)
    private val _isSafetyAuditDialogVisible = MutableStateFlow(false)
    private val _safetyAuditResult = MutableStateFlow<PresetValidationResult?>(null)
    private val _safetyAuditPresetName = MutableStateFlow<String?>(null)
    private val _isAboutDialogVisible = MutableStateFlow(false)

    private val _userNotification = MutableStateFlow<String?>(null)
    private val _activeError = MutableStateFlow<AudioError?>(null)

    private data class CorePart1(
        val devGroup: DeviceSettingsState,
        val engGroup: EngineAndProfileState,
        val s1: SheetStates1,
        val s2: SheetStates2
    )

    private data class FeaturePart2(
        val s3: SheetStates3,
        val smartEq: SmartEqState,
        val previewState: PreviewAndAbState,
        val auditState: AuditAndNotifState
    )

    private val coreFlows = combine(
        combine(
            deviceManager.currentOutputDevice,
            deviceManager.availableDevices,
            settingsRepository.settings,
            settingsRepository.availablePresets,
            profileManager.profiles
        ) { device, available, settings, presets, devProfiles ->
            val match = if (device != null) profileManager.matchProfileForDevice(device) else Pair(null, ProfileMatchType.NO_MATCH)
            DeviceSettingsState(
                currentDevice = device,
                availableDevices = available,
                settings = settings,
                presets = presets,
                deviceProfiles = devProfiles,
                activeProfile = match.first,
                matchType = match.second
            )
        },
        combine(
            audioEngine.engineState,
            toneGenerator.state,
            AppLogger.logs,
            profileManager.lastApplyResult,
            profileManager.defaultProfileId
        ) { engineState, toneState, logs, applyRes, defProfId ->
            EngineAndProfileState(engineState, toneState, logs, applyRes, defProfId)
        },
        combine(
            settingsRepository.availableListeningProfiles,
            settingsRepository.canUndo,
            _isSettingsSheetVisible,
            _isDiagnosticsVisible,
            _isMyDevicesSheetVisible
        ) { listeningProfs, canUndo, setVis, diagVis, myDevVis ->
            SheetStates1(listeningProfs, canUndo, setVis, diagVis, myDevVis)
        },
        combine(
            _isCreateDeviceProfileDialogVisible,
            _isEditDeviceProfileDialogVisible,
            _profileToEdit,
            _isDeleteDeviceProfileDialogVisible,
            _profileToDelete
        ) { createDevVis, editDevVis, editProf, delDevVis, delProf ->
            SheetStates2(createDevVis, editDevVis, editProf, delDevVis, delProf)
        }
    ) { devGroup, engGroup, s1, s2 ->
        CorePart1(devGroup, engGroup, s1, s2)
    }

    private val modalAndFeatureFlows = combine(
        combine(
            _isSavePresetDialogVisible,
            _isRenamePresetDialogVisible,
            _presetToRename,
            _isDeletePresetDialogVisible,
            _presetToDelete
        ) { savePresetVis, renPresetVis, renPreset, delPresetVis, delPreset ->
            SheetStates3(savePresetVis, renPresetVis, renPreset, delPresetVis, delPreset)
        },
        combine(
            _isSmartEqDialogVisible,
            _smartEqGoal,
            _smartEqContext,
            _smartEqIntensity,
            _smartEqResult
        ) { smartVis, goal, ctx, intensity, result ->
            SmartEqState(smartVis, goal, ctx, intensity, result)
        },
        combine(
            _isPreviewActive,
            _previewTargetName,
            _isAbComparing,
            _isAuditioningB,
            _isNormalizeConfirmDialogVisible
        ) { isPreview, previewName, abComp, isB, normVis ->
            PreviewAndAbState(isPreview, previewName, abComp, isB, normVis)
        },
        combine(
            _isSafetyAuditDialogVisible,
            _safetyAuditResult,
            _safetyAuditPresetName,
            _isAboutDialogVisible,
            combine(_userNotification, _activeError) { notif, err -> Pair(notif, err) }
        ) { auditVis, auditRes, auditName, aboutVis, (notif, err) ->
            AuditAndNotifState(auditVis, auditRes, auditName, aboutVis, notif, err)
        }
    ) { s3, smartEq, previewState, auditState ->
        FeaturePart2(s3, smartEq, previewState, auditState)
    }

    val uiState: StateFlow<HomeUiState> = combine(coreFlows, modalAndFeatureFlows) { p1, p2 ->
        val devGroup = p1.devGroup
        val engGroup = p1.engGroup
        val s1 = p1.s1
        val s2 = p1.s2
        val s3 = p2.s3
        val smartEq = p2.smartEq
        val previewState = p2.previewState
        val auditState = p2.auditState
        HomeUiState(
            isInitialized = true,
            currentDevice = devGroup.currentDevice,
            availableDevices = devGroup.availableDevices,
            capabilities = audioEngine.capabilities,
            settings = devGroup.settings,
            presets = devGroup.presets,
            profiles = EarphoneProfile.defaultProfiles(),
            listeningProfiles = s1.listeningProfiles,
            canUndo = s1.canUndo,
            deviceProfiles = devGroup.deviceProfiles,
            activeDeviceProfile = devGroup.activeProfile,
            profileMatchType = devGroup.matchType,
            lastProfileApplyResult = engGroup.lastApplyResult,
            defaultProfileId = engGroup.defaultProfileId,
            engineState = engGroup.engineState,
            testToneState = engGroup.toneState,
            recentLogs = engGroup.logs.takeLast(40),
            isSettingsSheetVisible = s1.isSettingsVisible,
            isDiagnosticsVisible = s1.isDiagnosticsVisible,
            isMyDevicesSheetVisible = s1.isMyDevicesVisible,
            isCreateDeviceProfileDialogVisible = s2.isCreateDeviceVisible,
            isEditDeviceProfileDialogVisible = s2.isEditDeviceVisible,
            profileToEdit = s2.profileToEdit,
            isDeleteDeviceProfileDialogVisible = s2.isDeleteDeviceVisible,
            profileToDelete = s2.profileToDelete,
            isSavePresetDialogVisible = s3.isSavePresetVisible,
            isRenamePresetDialogVisible = s3.isRenamePresetVisible,
            presetToRename = s3.presetToRename,
            isDeletePresetDialogVisible = s3.isDeletePresetVisible,
            presetToDelete = s3.presetToDelete,
            // Phase 7 States
            isSmartEqDialogVisible = smartEq.isDialogVisible,
            smartEqGoal = smartEq.goal,
            smartEqContext = smartEq.context,
            smartEqIntensity = smartEq.intensity,
            smartEqResult = smartEq.result,
            isPreviewActive = previewState.isPreviewActive,
            previewTargetName = previewState.previewTargetName,
            isAbComparing = previewState.isAbComparing,
            isAuditioningB = previewState.isAuditioningB,
            isNormalizeConfirmDialogVisible = previewState.isNormalizeConfirmDialogVisible,
            isSafetyAuditDialogVisible = auditState.isSafetyAuditDialogVisible,
            safetyAuditResult = auditState.safetyAuditResult,
            safetyAuditPresetName = auditState.safetyAuditPresetName,
            isAboutDialogVisible = auditState.isAboutDialogVisible,
            userNotification = auditState.userNotification,
            activeError = auditState.activeError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(
            capabilities = audioEngine.capabilities,
            presets = Preset.defaultPresets(),
            listeningProfiles = ListeningProfile.defaultProfiles(),
            deviceProfiles = DeviceProfile.defaultProfiles()
        )
    )

    private data class DeviceSettingsState(
        val currentDevice: AudioDevice?,
        val availableDevices: List<AudioDevice>,
        val settings: EqualizerSettings,
        val presets: List<Preset>,
        val deviceProfiles: List<DeviceProfile>,
        val activeProfile: DeviceProfile?,
        val matchType: ProfileMatchType
    )

    private data class EngineAndProfileState(
        val engineState: AudioEngineState,
        val toneState: TestToneState,
        val logs: List<AudioLogEntry>,
        val lastApplyResult: ProfileApplyResult?,
        val defaultProfileId: String?
    )

    private data class SheetStates1(
        val listeningProfiles: List<ListeningProfile>,
        val canUndo: Boolean,
        val isSettingsVisible: Boolean,
        val isDiagnosticsVisible: Boolean,
        val isMyDevicesVisible: Boolean
    )

    private data class SheetStates2(
        val isCreateDeviceVisible: Boolean,
        val isEditDeviceVisible: Boolean,
        val profileToEdit: DeviceProfile?,
        val isDeleteDeviceVisible: Boolean,
        val profileToDelete: DeviceProfile?
    )

    private data class SheetStates3(
        val isSavePresetVisible: Boolean,
        val isRenamePresetVisible: Boolean,
        val presetToRename: Preset?,
        val isDeletePresetVisible: Boolean,
        val presetToDelete: Preset?
    )

    private data class SmartEqState(
        val isDialogVisible: Boolean,
        val goal: ListeningGoal,
        val context: SmartEqContext,
        val intensity: SmartEqIntensity,
        val result: SmartEqResult?
    )

    private data class PreviewAndAbState(
        val isPreviewActive: Boolean,
        val previewTargetName: String?,
        val isAbComparing: Boolean,
        val isAuditioningB: Boolean,
        val isNormalizeConfirmDialogVisible: Boolean
    )

    private data class AuditAndNotifState(
        val isSafetyAuditDialogVisible: Boolean,
        val safetyAuditResult: PresetValidationResult?,
        val safetyAuditPresetName: String?,
        val isAboutDialogVisible: Boolean,
        val userNotification: String?,
        val activeError: AudioError?
    )

    init {
        AppLogger.i(LogCategory.STARTUP, TAG, "MainViewModel initialized")
        observeDeviceChanges()
        syncEngineConfig()
        recomputeSmartEq()
    }

    private fun observeDeviceChanges() {
        viewModelScope.launch {
            deviceManager.currentOutputDevice.collect { device ->
                if (device != null) {
                    AppLogger.i(LogCategory.DEVICE, TAG, "Observed device route update: ${device.name} (${device.type})")
                    val result = profileManager.onRouteChanged(device, audioEngine, settingsRepository)
                    if (result != null && result.profileId != null) {
                        _userNotification.value = "Loaded profile '${result.profileName}' (${result.matchType.displayName})"
                    }
                }
            }
        }
    }

    fun onToggleEnabled(enabled: Boolean) {
        AppLogger.i(LogCategory.SETTINGS, TAG, "User toggled equalizer master switch: $enabled")
        val result = audioEngine.setEnabled(enabled)
        result.onSuccess { actualEnabled ->
            settingsRepository.setEnabled(actualEnabled)
            syncEngineConfig()
        }.onFailure { error ->
            settingsRepository.setEnabled(false)
            _activeError.value = error
        }
    }

    fun onBandGainChanged(bandIndex: Int, gainDb: Float) {
        settingsRepository.updateBand(bandIndex, gainDb)
        syncEngineConfig()
    }

    fun onBassChanged(bassPercent: Float) {
        settingsRepository.setBassLevel(bassPercent)
        syncEngineConfig()
    }

    fun onTrebleChanged(trebleGainDb: Float) {
        settingsRepository.setTrebleLevel(trebleGainDb)
        syncEngineConfig()
    }

    fun onPreampChanged(preampGainDb: Float) {
        settingsRepository.setPreampLevel(preampGainDb)
        syncEngineConfig()
    }

    fun onBalanceChanged(balance: Float) {
        settingsRepository.setBalance(balance)
        syncEngineConfig()
    }

    fun onSelectPreset(presetId: String) {
        settingsRepository.selectPreset(presetId)
        syncEngineConfig()
    }

    fun onSelectListeningProfile(profileId: String) {
        settingsRepository.selectListeningProfile(profileId)
        syncEngineConfig()
        val prof = settingsRepository.availableListeningProfiles.value.find { it.id == profileId }
        _userNotification.value = "Applied sound preference: ${prof?.name ?: profileId}"
    }

    fun onSetHeadroomMode(mode: HeadroomMode) {
        settingsRepository.setHeadroomMode(mode)
        syncEngineConfig()
        _userNotification.value = when (mode) {
            HeadroomMode.AUTOMATIC -> "Auto Headroom: Automatically preventing digital clipping"
            HeadroomMode.MANUAL -> "Manual Headroom: User-controlled attenuation"
            HeadroomMode.OFF -> "Headroom Protection OFF: Full unattenuated output (Caution: clipping risk)"
        }
    }

    fun onSetManualHeadroom(headroomDb: Float) {
        settingsRepository.setManualHeadroomDb(headroomDb)
        syncEngineConfig()
    }

    fun onUndo() {
        val result = settingsRepository.undo()
        result.onSuccess {
            syncEngineConfig()
            _userNotification.value = "Restored previous equalizer state."
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun onResetToFlat() {
        settingsRepository.resetToFlat()
        syncEngineConfig()
        _userNotification.value = "Equalizer reset to Flat reference."
    }

    fun onToggleAutoHeadroom(enabled: Boolean) {
        onSetHeadroomMode(if (enabled) HeadroomMode.AUTOMATIC else HeadroomMode.OFF)
    }

    fun showNormalizeConfirmDialog() {
        _isNormalizeConfirmDialogVisible.value = true
    }

    fun dismissNormalizeConfirmDialog() {
        _isNormalizeConfirmDialogVisible.value = false
    }

    fun onConfirmNormalizeCurve() {
        _isNormalizeConfirmDialogVisible.value = false
        settingsRepository.normalizeEqualizerCurve()
        syncEngineConfig()
        _userNotification.value = "Subtractive normalization applied: peak boost adjusted to 0.0 dB without changing relative curve balance."
    }

    // ==========================================
    // Smart EQ Assistant (Phase 7)
    // ==========================================

    fun showSmartEqDialog() {
        recomputeSmartEq()
        _isSmartEqDialogVisible.value = true
    }

    fun dismissSmartEqDialog() {
        _isSmartEqDialogVisible.value = false
    }

    fun onSmartEqGoalChanged(goal: ListeningGoal) {
        _smartEqGoal.value = goal
        recomputeSmartEq()
    }

    fun onSmartEqContextChanged(context: SmartEqContext) {
        _smartEqContext.value = context
        recomputeSmartEq()
    }

    fun onSmartEqIntensityChanged(intensity: SmartEqIntensity) {
        _smartEqIntensity.value = intensity
        recomputeSmartEq()
    }

    private fun recomputeSmartEq() {
        val res = SmartEqGenerator.generate(
            goal = _smartEqGoal.value,
            context = _smartEqContext.value,
            intensity = _smartEqIntensity.value,
            capabilities = audioEngine.capabilities,
            headroomMode = settingsRepository.settings.value.headroomMode
        )
        _smartEqResult.value = res
    }

    fun onApplySmartEqResult() {
        val res = _smartEqResult.value ?: return
        _isSmartEqDialogVisible.value = false

        // Apply generated parameters directly to settings
        res.bandGainsDb.forEachIndexed { index, gain ->
            settingsRepository.updateBand(index, gain)
        }
        settingsRepository.setBassLevel(res.bassBoostPercent / 100.0f)
        settingsRepository.setTrebleLevel(res.trebleGainDb)
        settingsRepository.setPreampLevel(res.preampGainDb)
        settingsRepository.setBalance(res.balance)

        syncEngineConfig()
        _userNotification.value = "Applied smart sound preference: ${res.goal.displayName} (${res.context.displayName})"
    }

    // ==========================================
    // Preset & Profile Preview / A-B Auditioning
    // ==========================================

    fun onStartPreview(name: String, bandGains: List<Float>, bassPercent: Int, trebleDb: Float, preampDb: Float, balance: Float) {
        if (!_isPreviewActive.value) {
            snapshotBeforePreview = settingsRepository.settings.value.copy()
        }
        _isPreviewActive.value = true
        _previewTargetName.value = name
        _isAbComparing.value = true
        _isAuditioningB.value = false // A = Audition target

        // Temporarily apply preview parameters to DSP engine without saving
        applyTemporaryDspGains(bandGains, bassPercent, trebleDb, preampDb, balance)
        _userNotification.value = "Previewing '$name' (Use A/B toggle to compare with Flat baseline)"
    }

    fun onToggleAbComparison() {
        val nowAuditioningB = !_isAuditioningB.value
        _isAuditioningB.value = nowAuditioningB

        if (nowAuditioningB) {
            // B = Flat Baseline (0 dB across all bands, no boosts)
            applyTemporaryDspGains(listOf(0f, 0f, 0f, 0f, 0f), 0, 0f, 0f, 0f)
            _userNotification.value = "Auditioning [B]: Flat Baseline Reference"
        } else {
            // A = Auditioning target curve
            val snap = settingsRepository.settings.value
            applyTemporaryDspGains(
                snap.bands.map { it.gainDb },
                (snap.bassLevel * 100).toInt(),
                snap.trebleLevel,
                snap.preampLevel,
                snap.balance
            )
            _userNotification.value = "Auditioning [A]: Selected EQ Profile"
        }
    }

    fun onCommitPreview() {
        _isPreviewActive.value = false
        _previewTargetName.value = null
        _isAbComparing.value = false
        _isAuditioningB.value = false
        snapshotBeforePreview = null
        syncEngineConfig()
        _userNotification.value = "Tuning saved permanently."
    }

    fun onCancelPreview() {
        _isPreviewActive.value = false
        _previewTargetName.value = null
        _isAbComparing.value = false
        _isAuditioningB.value = false
        val snap = snapshotBeforePreview
        if (snap != null) {
            snap.bands.forEachIndexed { i, b -> settingsRepository.updateBand(i, b.gainDb) }
            settingsRepository.setBassLevel(snap.bassLevel)
            settingsRepository.setTrebleLevel(snap.trebleLevel)
            settingsRepository.setPreampLevel(snap.preampLevel)
            settingsRepository.setBalance(snap.balance)
            settingsRepository.setHeadroomMode(snap.headroomMode)
            settingsRepository.setManualHeadroomDb(snap.manualHeadroomDb)
            snapshotBeforePreview = null
        }
        syncEngineConfig()
        _userNotification.value = "Preview canceled: restored original tuning."
    }

    private fun applyTemporaryDspGains(
        bandGainsDb: List<Float>,
        bassBoostPercent: Int,
        trebleDb: Float,
        preampDb: Float,
        balance: Float
    ) {
        val current = settingsRepository.settings.value
        val bandMap = bandGainsDb.mapIndexed { idx, gain -> idx to gain }.toMap()
        val headroom = HeadroomCalculator.analyze(
            bandGainsDb = bandGainsDb,
            bassStrengthPercent = bassBoostPercent,
            trebleGainDb = trebleDb,
            preampGainDb = preampDb,
            headroomMode = current.headroomMode,
            manualHeadroomDb = current.manualHeadroomDb
        )
        val config = AudioEngineConfig(
            isEnabled = current.isEnabled,
            bandGainsDb = bandMap,
            bassStrengthPercent = bassBoostPercent,
            trebleGainDb = trebleDb,
            preampGainDb = preampDb,
            stereoBalance = balance,
            autoHeadroomOffsetDb = headroom.autoHeadroomOffsetDb
        )
        audioEngine.applyConfiguration(config)
    }

    // ==========================================
    // Preset Safety Audit Dialog
    // ==========================================

    fun showPresetSafetyAudit(preset: Preset) {
        val result = PresetSafetyValidator.validatePreset(preset)
        _safetyAuditPresetName.value = preset.name
        _safetyAuditResult.value = result
        _isSafetyAuditDialogVisible.value = true
    }

    fun dismissPresetSafetyAudit() {
        _isSafetyAuditDialogVisible.value = false
        _safetyAuditResult.value = null
        _safetyAuditPresetName.value = null
    }

    fun showAboutDialog() {
        _isAboutDialogVisible.value = true
    }

    fun dismissAboutDialog() {
        _isAboutDialogVisible.value = false
    }

    // ==========================================
    // Device Profile Management
    // ==========================================

    fun setMyDevicesSheetVisible(visible: Boolean) {
        _isMyDevicesSheetVisible.value = visible
    }

    fun showCreateDeviceProfileDialog() {
        _isCreateDeviceProfileDialogVisible.value = true
    }

    fun dismissCreateDeviceProfileDialog() {
        _isCreateDeviceProfileDialogVisible.value = false
    }

    fun onCreateDeviceProfile(
        name: String,
        deviceType: DeviceType,
        targetDeviceId: String? = null,
        targetProductName: String? = null,
        isGenericFallback: Boolean = false,
        presetId: String = Preset.FLAT.id,
        bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
        bassBoostPercent: Int = 0,
        trebleGainDb: Float = 0.0f,
        preampGainDb: Float = 0.0f,
        balance: Float = 0.0f,
        autoApplyEnabled: Boolean = true,
        isDefaultAudioProfile: Boolean = false
    ) {
        _isCreateDeviceProfileDialogVisible.value = false
        val profile = DeviceProfile(
            name = name.trim().ifEmpty { "My Audio Profile" },
            deviceType = deviceType,
            targetDeviceId = targetDeviceId,
            targetProductName = targetProductName,
            isGenericFallback = isGenericFallback,
            presetId = presetId,
            bandGainsDb = bandGainsDb,
            bassBoostPercent = bassBoostPercent,
            trebleGainDb = trebleGainDb,
            preampGainDb = preampGainDb,
            balance = balance,
            autoApplyEnabled = autoApplyEnabled,
            isDefaultAudioProfile = isDefaultAudioProfile
        )

        val result = profileManager.saveProfile(profile)
        result.onSuccess { saved ->
            _userNotification.value = "Created device profile '${saved.name}'"
            val currentDev = deviceManager.currentOutputDevice.value
            if (currentDev != null && (saved.targetDeviceId == currentDev.id || saved.deviceType == currentDev.type)) {
                profileManager.applyProfile(currentDev, saved, audioEngine, settingsRepository)
            }
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun showEditDeviceProfileDialog(profile: DeviceProfile) {
        _profileToEdit.value = profile
        _isEditDeviceProfileDialogVisible.value = true
    }

    fun dismissEditDeviceProfileDialog() {
        _isEditDeviceProfileDialogVisible.value = false
        _profileToEdit.value = null
    }

    fun onUpdateDeviceProfile(profile: DeviceProfile) {
        _isEditDeviceProfileDialogVisible.value = false
        _profileToEdit.value = null
        val result = profileManager.saveProfile(profile)
        result.onSuccess { updated ->
            _userNotification.value = "Updated profile '${updated.name}'"
            val currentDev = deviceManager.currentOutputDevice.value
            if (currentDev != null && (updated.targetDeviceId == currentDev.id || updated.deviceType == currentDev.type)) {
                profileManager.applyProfile(currentDev, updated, audioEngine, settingsRepository)
            }
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun showDeleteDeviceProfileDialog(profile: DeviceProfile) {
        _profileToDelete.value = profile
        _isDeleteDeviceProfileDialogVisible.value = true
    }

    fun dismissDeleteDeviceProfileDialog() {
        _isDeleteDeviceProfileDialogVisible.value = false
        _profileToDelete.value = null
    }

    fun onDeleteDeviceProfile(profileId: String) {
        _isDeleteDeviceProfileDialogVisible.value = false
        _profileToDelete.value = null
        val result = profileManager.deleteProfile(profileId)
        result.onSuccess {
            _userNotification.value = "Device profile deleted."
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun onToggleProfileAutoApply(profileId: String, enabled: Boolean) {
        profileManager.toggleAutoApply(profileId, enabled)
    }

    fun onSetDefaultAudioProfile(profileId: String?) {
        profileManager.setDefaultProfile(profileId)
        _userNotification.value = if (profileId != null) "Set as default fallback audio profile." else "Cleared default profile."
    }

    fun onManualApplyDeviceProfile(profile: DeviceProfile) {
        val currentDev = deviceManager.currentOutputDevice.value ?: AudioDevice.defaultBuiltinSpeaker()
        val result = profileManager.applyProfile(currentDev, profile, audioEngine, settingsRepository)
        _userNotification.value = "Applied '${profile.name}' (${result.status})"
    }

    // ==========================================
    // Custom Preset Management
    // ==========================================

    fun showSavePresetDialog() {
        _isSavePresetDialogVisible.value = true
    }

    fun dismissSavePresetDialog() {
        _isSavePresetDialogVisible.value = false
    }

    fun onCreateCustomPreset(name: String, description: String = "") {
        _isSavePresetDialogVisible.value = false
        val result = settingsRepository.createCustomPreset(name, description)
        result.onSuccess { preset ->
            _userNotification.value = "Saved preset '${preset.name}'"
            syncEngineConfig()
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun showRenamePresetDialog(preset: Preset) {
        _presetToRename.value = preset
        _isRenamePresetDialogVisible.value = true
    }

    fun dismissRenamePresetDialog() {
        _isRenamePresetDialogVisible.value = false
        _presetToRename.value = null
    }

    fun onRenameCustomPreset(presetId: String, newName: String) {
        _isRenamePresetDialogVisible.value = false
        _presetToRename.value = null
        val result = settingsRepository.updateCustomPreset(presetId, newName)
        result.onSuccess { updated ->
            _userNotification.value = "Preset renamed to '${updated.name}'"
            syncEngineConfig()
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun onUpdateCurrentCustomPreset(presetId: String) {
        val result = settingsRepository.updateCustomPreset(presetId)
        result.onSuccess { updated ->
            _userNotification.value = "Updated preset '${updated.name}' with current settings"
            syncEngineConfig()
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun showDeletePresetDialog(preset: Preset) {
        _presetToDelete.value = preset
        _isDeletePresetDialogVisible.value = true
    }

    fun dismissDeletePresetDialog() {
        _isDeletePresetDialogVisible.value = false
        _presetToDelete.value = null
    }

    fun onDeleteCustomPreset(presetId: String) {
        _isDeletePresetDialogVisible.value = false
        _presetToDelete.value = null
        val result = settingsRepository.deleteCustomPreset(presetId)
        result.onSuccess {
            _userNotification.value = "Custom preset removed."
            syncEngineConfig()
        }.onFailure { error ->
            _activeError.value = error
        }
    }

    fun onMapDeviceToPreset(deviceIdOrType: String, presetId: String) {
        settingsRepository.saveDevicePresetMapping(deviceIdOrType, presetId)
        _userNotification.value = "Associated device with preset."
    }

    fun onSelectEarphoneProfile(profileId: String) {
        settingsRepository.selectEarphoneProfile(profileId)
        syncEngineConfig()
    }

    fun onToggleAutoApplyProfile(autoApply: Boolean) {
        settingsRepository.setAutoApplyProfile(autoApply)
    }

    fun onSelectTheme(theme: ThemePreference) {
        settingsRepository.setThemePreference(theme)
    }

    fun onRefreshDevices() {
        AppLogger.i(LogCategory.DEVICE, TAG, "Manual device refresh requested")
        val result = deviceManager.refreshDevices()
        result.onFailure { error ->
            _activeError.value = error
        }
    }

    fun onSelectDevice(deviceId: String) {
        val result = deviceManager.selectDevice(deviceId)
        result.onFailure { error ->
            _activeError.value = error
        }
    }

    // Audio Test Tone Control Actions
    fun onPlayTestTone(mode: TestToneMode) {
        toneGenerator.startTone(mode)
        val toneSession = toneGenerator.state.value.sessionId
        if (toneSession != 0) {
            AppLogger.i(LogCategory.AUDIO, TAG, "Attaching AudioEngine to test tone session #$toneSession")
            audioEngine.initialize(AudioSessionInfo(sessionId = toneSession, isGlobalMix = false))
            syncEngineConfig()
        }
    }

    fun onStopTestTone() {
        toneGenerator.stopTone()
        AppLogger.i(LogCategory.AUDIO, TAG, "Returning AudioEngine to Global Mix session 0")
        audioEngine.initialize(AudioSessionInfo.GLOBAL)
        syncEngineConfig()
    }

    fun onSetTestToneVolume(volume: Float) {
        toneGenerator.setVolume(volume)
    }

    fun onResetDefaults() {
        settingsRepository.resetToDefaults()
        syncEngineConfig()
        _userNotification.value = "Settings reset to default factory flat profile."
    }

    fun onDismissNotification() {
        _userNotification.value = null
    }

    fun onDismissError() {
        _activeError.value = null
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _isSettingsSheetVisible.value = visible
    }

    fun setDiagnosticsVisible(visible: Boolean) {
        _isDiagnosticsVisible.value = visible
    }

    private fun syncEngineConfig() {
        val current = settingsRepository.settings.value
        val bandMap = current.bands.associate { it.bandIndex to it.gainDb }
        val headroom = HeadroomCalculator.analyze(
            bandGainsDb = current.bands.map { it.gainDb },
            bassStrengthPercent = (current.bassLevel * 100).toInt(),
            trebleGainDb = current.trebleLevel,
            preampGainDb = current.preampLevel,
            headroomMode = current.headroomMode,
            manualHeadroomDb = current.manualHeadroomDb
        )
        val config = AudioEngineConfig(
            isEnabled = current.isEnabled,
            bandGainsDb = bandMap,
            bassStrengthPercent = (current.bassLevel * 100).toInt(),
            trebleGainDb = current.trebleLevel,
            preampGainDb = current.preampLevel,
            stereoBalance = current.balance,
            autoHeadroomOffsetDb = headroom.autoHeadroomOffsetDb
        )
        val result = audioEngine.applyConfiguration(config)
        result.onFailure { error ->
            AppLogger.w(LogCategory.ENGINE, TAG, "Engine rejected configuration: ${error.userFriendlyMessage}")
        }
    }

    fun onRetryRecovery() {
        viewModelScope.launch {
            AppLogger.i(LogCategory.ENGINE, TAG, "User requested manual DSP recovery retry")
            val result = audioEngine.retryRecovery()
            result.onSuccess {
                _userNotification.value = "DSP audio effects reattached successfully."
            }.onFailure { err ->
                _userNotification.value = "Recovery retry: ${err.userFriendlyMessage}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.i(LogCategory.STARTUP, TAG, "MainViewModel onCleared")
        // Note: Singleton audioEngine and deviceManager are preserved at Application container scope
        toneGenerator.stopTone()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
