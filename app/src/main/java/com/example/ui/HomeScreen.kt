package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TestToneMode
import com.example.audio.TestToneState
import com.example.audio.model.AudioEngineState
import com.example.audio.model.AudioEngineStatus
import com.example.audio.model.AudioSessionState
import com.example.audio.safety.ClippingRisk
import com.example.core.logging.AudioLogEntry
import com.example.core.logging.LogLevel
import com.example.core.result.AudioCapabilityState
import com.example.core.result.AudioError
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.model.ProfileApplyResult
import com.example.device.model.ProfileMatchType
import com.example.domain.smarteq.SmartEqContext
import com.example.domain.smarteq.SmartEqIntensity
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.HeadroomMode
import com.example.settings.model.ListeningGoal
import com.example.settings.model.ListeningProfile
import com.example.settings.model.Preset
import com.example.ui.components.AboutPrivacyDialog
import com.example.ui.components.CreateDeviceProfileDialog
import com.example.ui.components.DeleteDeviceProfileDialog
import com.example.ui.components.EditDeviceProfileDialog
import com.example.ui.components.EqualizerCurveCanvas
import com.example.ui.components.MyDevicesSheet
import com.example.ui.components.NormalizeConfirmDialog
import com.example.ui.components.PresetSafetyDialog
import com.example.ui.components.SmartEqAssistantDialog
import com.example.ui.components.VerticalBandSlider
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicRuby

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandGainChanged: (Int, Float) -> Unit,
    onBassChanged: (Float) -> Unit,
    onTrebleChanged: (Float) -> Unit,
    onPreampChanged: (Float) -> Unit,
    onBalanceChanged: (Float) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectListeningProfile: (String) -> Unit = {},
    onSelectEarphoneProfile: (String) -> Unit,
    onToggleAutoApplyProfile: (Boolean) -> Unit,
    onSetHeadroomMode: (HeadroomMode) -> Unit = {},
    onSetManualHeadroom: (Float) -> Unit = {},
    onUndo: () -> Unit = {},
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onPlayTestTone: (TestToneMode) -> Unit,
    onStopTestTone: () -> Unit,
    onSetTestToneVolume: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    onDismissNotification: () -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onCloseDiagnostics: () -> Unit,
    onOpenMyDevices: () -> Unit = {},
    onCloseMyDevices: () -> Unit = {},
    onShowCreateDeviceProfile: () -> Unit = {},
    onDismissCreateDeviceProfile: () -> Unit = {},
    onCreateDeviceProfile: (
        name: String,
        deviceType: DeviceType,
        targetDeviceId: String?,
        targetProductName: String?,
        isGenericFallback: Boolean,
        presetId: String,
        bandGainsDb: List<Float>,
        bassBoostPercent: Int,
        trebleGainDb: Float,
        preampGainDb: Float,
        balance: Float,
        autoApplyEnabled: Boolean,
        isDefaultAudioProfile: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onShowEditDeviceProfile: (DeviceProfile) -> Unit = {},
    onDismissEditDeviceProfile: () -> Unit = {},
    onUpdateDeviceProfile: (DeviceProfile) -> Unit = {},
    onShowDeleteDeviceProfile: (DeviceProfile) -> Unit = {},
    onDismissDeleteDeviceProfile: () -> Unit = {},
    onDeleteDeviceProfile: (String) -> Unit = {},
    onToggleProfileAutoApply: (String, Boolean) -> Unit = { _, _ -> },
    onSetDefaultAudioProfile: (String?) -> Unit = {},
    onManualApplyDeviceProfile: (DeviceProfile) -> Unit = {},
    onResetToFlat: () -> Unit = {},
    onToggleAutoHeadroom: (Boolean) -> Unit = {},
    onShowNormalizeConfirm: () -> Unit = {},
    onDismissNormalizeConfirm: () -> Unit = {},
    onConfirmNormalizeCurve: () -> Unit = {},
    onShowSavePresetDialog: () -> Unit = {},
    onDismissSavePresetDialog: () -> Unit = {},
    onCreateCustomPreset: (name: String, desc: String) -> Unit = { _, _ -> },
    onShowRenamePresetDialog: (Preset) -> Unit = {},
    onDismissRenamePresetDialog: () -> Unit = {},
    onRenameCustomPreset: (presetId: String, newName: String) -> Unit = { _, _ -> },
    onUpdateCurrentCustomPreset: (presetId: String) -> Unit = {},
    onShowDeletePresetDialog: (Preset) -> Unit = {},
    onDismissDeletePresetDialog: () -> Unit = {},
    onDeleteCustomPreset: (presetId: String) -> Unit = {},
    onMapDeviceToPreset: (deviceId: String, presetId: String) -> Unit = { _, _ -> },
    // Phase 7 Assistant & Preview
    onShowSmartEq: () -> Unit = {},
    onDismissSmartEq: () -> Unit = {},
    onSmartEqGoalChanged: (ListeningGoal) -> Unit = {},
    onSmartEqContextChanged: (SmartEqContext) -> Unit = {},
    onSmartEqIntensityChanged: (SmartEqIntensity) -> Unit = {},
    onApplySmartEq: () -> Unit = {},
    onStartPreview: (name: String, bandGains: List<Float>, bass: Int, treble: Float, preamp: Float, balance: Float) -> Unit = { _, _, _, _, _, _ -> },
    onToggleAbComparison: () -> Unit = {},
    onCommitPreview: () -> Unit = {},
    onCancelPreview: () -> Unit = {},
    onShowPresetSafetyAudit: (Preset) -> Unit = {},
    onDismissPresetSafetyAudit: () -> Unit = {},
    onRetryRecovery: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onCloseAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onDismissNotification()
        }
    }

    LaunchedEffect(uiState.activeError) {
        uiState.activeError?.let { err ->
            snackbarHostState.showSnackbar("Error: ${err.userFriendlyMessage}")
            onDismissError()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_home"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.settings.isEnabled)
                                        SonicCyan.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (uiState.settings.isEnabled) SonicCyan else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Equalizer",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = uiState.currentDevice?.name ?: "Audio DSP Engine",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Undo Action (Active when undo stack is not empty)
                    if (uiState.canUndo) {
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier.testTag("btn_undo_action")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo EQ change",
                                tint = SonicCyan
                            )
                        }
                    }

                    // Smart EQ Assistant Button
                    IconButton(
                        onClick = onShowSmartEq,
                        modifier = Modifier.testTag("btn_top_smart_eq")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Smart EQ Assistant",
                            tint = ElectricIndigo
                        )
                    }

                    // "My Devices" Button in TopAppBar
                    IconButton(
                        onClick = onOpenMyDevices,
                        modifier = Modifier.testTag("btn_open_my_devices")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = "My Audio Devices & Profiles",
                            tint = if (uiState.activeDeviceProfile != null) SonicCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Audio Diagnostics Button
                    IconButton(
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.testTag("btn_open_diagnostics")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "Audio Diagnostics & DSP Inspector",
                            tint = if (uiState.testToneState.isPlaying) SonicEmerald else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // About & Privacy Button
                    IconButton(
                        onClick = onOpenAbout,
                        modifier = Modifier.testTag("btn_open_about")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About & Privacy Transparency"
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Device Preferences"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A/B Audition & Preview Banner (if preview is active)
            AnimatedVisibility(
                visible = uiState.isPreviewActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AuditionPreviewBanner(
                    previewTargetName = uiState.previewTargetName ?: "Audition Curve",
                    isAuditioningB = uiState.isAuditioningB,
                    onToggleAb = onToggleAbComparison,
                    onCommit = onCommitPreview,
                    onCancel = onCancelPreview
                )
            }

            // Reference Test Tone Active Banner (if active)
            AnimatedVisibility(
                visible = uiState.testToneState.isPlaying,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_test_tone_active"),
                    colors = CardDefaults.cardColors(containerColor = SonicEmerald.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SonicEmerald.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SonicEmerald)
                            )
                            Column {
                                Text(
                                    text = "Reference Test Tone Playing",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SonicEmerald
                                    )
                                )
                                Text(
                                    text = "${uiState.testToneState.activeMode.displayName} (Session #${uiState.testToneState.sessionId})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Button(
                            onClick = onStopTestTone,
                            colors = ButtonDefaults.buttonColors(containerColor = SonicEmerald),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_stop_test_tone_banner")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Current Output Device & Smart Profile Routing Card
            CurrentOutputDeviceCard(
                currentDevice = uiState.currentDevice,
                activeProfile = uiState.activeDeviceProfile,
                matchType = uiState.profileMatchType,
                lastApplyResult = uiState.lastProfileApplyResult,
                onOpenMyDevices = onOpenMyDevices,
                onCreateProfile = onShowCreateDeviceProfile
            )

            // Master Switch & Engine Status Card
            MasterSwitchCard(
                isEnabled = uiState.settings.isEnabled,
                engineState = uiState.engineState,
                capabilities = uiState.capabilities,
                onToggleEnabled = onToggleEnabled
            )

            // Sound Preferences & Presets Section (Phase 7)
            SoundProfilesAndPresetsSection(
                listeningProfiles = uiState.listeningProfiles,
                selectedProfileId = uiState.settings.selectedListeningProfileId,
                presets = uiState.presets,
                selectedPresetId = uiState.settings.selectedPresetId,
                canUndo = uiState.canUndo,
                onSelectListeningProfile = onSelectListeningProfile,
                onSelectPreset = onSelectPreset,
                onResetToFlat = onResetToFlat,
                onUndo = onUndo,
                onShowSmartEq = onShowSmartEq,
                onShowSavePresetDialog = onShowSavePresetDialog,
                onShowRenamePresetDialog = onShowRenamePresetDialog,
                onUpdateCurrentCustomPreset = onUpdateCurrentCustomPreset,
                onShowDeletePresetDialog = onShowDeletePresetDialog,
                onShowPresetSafetyAudit = onShowPresetSafetyAudit
            )

            // 5-Band Equalizer Canvas & Sliders
            EqualizerControlSection(
                bands = uiState.settings.bands,
                isEnabled = uiState.settings.isEnabled,
                onBandGainChanged = onBandGainChanged,
                onResetToFlat = onResetToFlat
            )

            // DSP Digital Headroom & Clipping Safety (Phase 7 User-Controlled Modes)
            DspHeadroomSafetySection(
                headroomAnalysis = uiState.headroomAnalysis,
                headroomMode = uiState.settings.headroomMode,
                manualHeadroomDb = uiState.settings.manualHeadroomDb,
                onSetHeadroomMode = onSetHeadroomMode,
                onSetManualHeadroom = onSetManualHeadroom,
                onShowNormalizeConfirm = onShowNormalizeConfirm
            )

            // Sound Tuning Parameters (Bass Boost, Treble, Preamp, Balance)
            SoundTuningSection(
                settings = uiState.settings,
                capabilities = uiState.capabilities,
                isEnabled = uiState.settings.isEnabled,
                onBassChanged = onBassChanged,
                onTrebleChanged = onTrebleChanged,
                onPreampChanged = onPreampChanged,
                onBalanceChanged = onBalanceChanged
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Modal Sheets & Dialogs

    // 1. Smart EQ Assistant Dialog (Phase 7)
    if (uiState.isSmartEqDialogVisible) {
        SmartEqAssistantDialog(
            selectedGoal = uiState.smartEqGoal,
            selectedContext = uiState.smartEqContext,
            selectedIntensity = uiState.smartEqIntensity,
            smartEqResult = uiState.smartEqResult,
            onGoalChanged = onSmartEqGoalChanged,
            onContextChanged = onSmartEqContextChanged,
            onIntensityChanged = onSmartEqIntensityChanged,
            onApply = onApplySmartEq,
            onAudition = {
                val res = uiState.smartEqResult
                if (res != null) {
                    onStartPreview(
                        "${res.goal.displayName} (${res.context.displayName})",
                        res.bandGainsDb,
                        res.bassBoostPercent,
                        res.trebleGainDb,
                        res.preampGainDb,
                        res.balance
                    )
                }
            },
            onDismiss = onDismissSmartEq
        )
    }

    // 2. Preset Safety & Clipping Audit Dialog (Phase 7)
    if (uiState.isSafetyAuditDialogVisible && uiState.safetyAuditResult != null) {
        PresetSafetyDialog(
            presetName = uiState.safetyAuditPresetName ?: "Preset Audit",
            validationResult = uiState.safetyAuditResult,
            onDismiss = onDismissPresetSafetyAudit
        )
    }

    // 3. Normalization Confirmation Dialog (Phase 7)
    if (uiState.isNormalizeConfirmDialogVisible) {
        NormalizeConfirmDialog(
            onConfirm = onConfirmNormalizeCurve,
            onDismiss = onDismissNormalizeConfirm
        )
    }

    // 4. My Devices Sheet
    if (uiState.isMyDevicesSheetVisible) {
        val myDevicesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        MyDevicesSheet(
            sheetState = myDevicesSheetState,
            currentDevice = uiState.currentDevice,
            profiles = uiState.deviceProfiles,
            activeProfile = uiState.activeDeviceProfile,
            matchType = uiState.profileMatchType,
            defaultProfileId = uiState.defaultProfileId,
            presets = uiState.presets,
            onDismiss = onCloseMyDevices,
            onCreateProfileClick = onShowCreateDeviceProfile,
            onEditProfileClick = onShowEditDeviceProfile,
            onDeleteProfileClick = onShowDeleteDeviceProfile,
            onToggleAutoApply = onToggleProfileAutoApply,
            onSetDefaultProfile = onSetDefaultAudioProfile,
            onApplyProfile = onManualApplyDeviceProfile
        )
    }

    // 5. Settings Sheet
    if (uiState.isSettingsSheetVisible) {
        SettingsModalSheet(
            settings = uiState.settings,
            profiles = uiState.profiles,
            presets = uiState.presets,
            currentDevice = uiState.currentDevice,
            onSelectProfile = onSelectEarphoneProfile,
            onToggleAutoApply = onToggleAutoApplyProfile,
            onMapDeviceToPreset = onMapDeviceToPreset,
            onResetDefaults = onResetDefaults,
            onOpenAbout = onOpenAbout,
            onDismiss = onCloseSettings
        )
    }

    // 6. Diagnostics Sheet
    if (uiState.isDiagnosticsVisible) {
        DiagnosticsModalSheet(
            capabilities = uiState.capabilities,
            engineState = uiState.engineState,
            headroomAnalysis = uiState.headroomAnalysis,
            headroomMode = uiState.settings.headroomMode,
            toneState = uiState.testToneState,
            logs = uiState.recentLogs,
            currentDevice = uiState.currentDevice,
            activeProfile = uiState.activeDeviceProfile,
            matchType = uiState.profileMatchType,
            lastApplyResult = uiState.lastProfileApplyResult,
            onSetHeadroomMode = onSetHeadroomMode,
            onShowNormalizeConfirm = onShowNormalizeConfirm,
            onPlayTone = onPlayTestTone,
            onStopTone = onStopTestTone,
            onSetVolume = onSetTestToneVolume,
            onRetryRecovery = onRetryRecovery,
            onDismiss = onCloseDiagnostics
        )
    }

    // Custom Preset Dialogs
    if (uiState.isSavePresetDialogVisible) {
        SaveCustomPresetDialog(
            onConfirm = { name, desc -> onCreateCustomPreset(name, desc) },
            onDismiss = onDismissSavePresetDialog
        )
    }

    if (uiState.isRenamePresetDialogVisible && uiState.presetToRename != null) {
        RenameCustomPresetDialog(
            preset = uiState.presetToRename,
            onConfirm = { newName -> onRenameCustomPreset(uiState.presetToRename.id, newName) },
            onDismiss = onDismissRenamePresetDialog
        )
    }

    if (uiState.isDeletePresetDialogVisible && uiState.presetToDelete != null) {
        DeleteCustomPresetDialog(
            preset = uiState.presetToDelete,
            onConfirm = { onDeleteCustomPreset(uiState.presetToDelete.id) },
            onDismiss = onDismissDeletePresetDialog
        )
    }

    // Device Profile Dialogs
    if (uiState.isCreateDeviceProfileDialogVisible) {
        CreateDeviceProfileDialog(
            currentDevice = uiState.currentDevice,
            presets = uiState.presets,
            onDismiss = onDismissCreateDeviceProfile,
            onConfirm = onCreateDeviceProfile
        )
    }

    if (uiState.isEditDeviceProfileDialogVisible && uiState.profileToEdit != null) {
        EditDeviceProfileDialog(
            profile = uiState.profileToEdit,
            presets = uiState.presets,
            onDismiss = onDismissEditDeviceProfile,
            onConfirm = onUpdateDeviceProfile
        )
    }

    if (uiState.isDeleteDeviceProfileDialogVisible && uiState.profileToDelete != null) {
        DeleteDeviceProfileDialog(
            profile = uiState.profileToDelete,
            onDismiss = onDismissDeleteDeviceProfile,
            onConfirm = { onDeleteDeviceProfile(uiState.profileToDelete.id) }
        )
    }

    // Phase 9 About & Privacy Dialog
    if (uiState.isAboutDialogVisible) {
        AboutPrivacyDialog(
            onDismiss = onCloseAbout
        )
    }
}

@Composable
private fun AuditionPreviewBanner(
    previewTargetName: String,
    isAuditioningB: Boolean,
    onToggleAb: () -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_ab_audition"),
        colors = CardDefaults.cardColors(containerColor = ElectricIndigo.copy(alpha = 0.2f)),
        border = BorderStroke(1.5.dp, ElectricIndigo),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAuditioningB) SonicAmber else SonicCyan)
                    )
                    Column {
                        Text(
                            text = if (isAuditioningB) "Auditioning [B]: Flat Baseline Reference" else "Auditioning [A]: $previewTargetName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAuditioningB) SonicAmber else SonicCyan
                        )
                        Text(
                            text = "Toggle A/B to compare against 0 dB Flat reference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onToggleAb,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAuditioningB) SonicAmber else SonicCyan,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_toggle_ab_audition")
                ) {
                    Text(if (isAuditioningB) "Switch to [A]" else "Switch to [B] Flat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_cancel_audition")
                ) {
                    Text("Discard", fontSize = 11.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onCommit,
                    colors = ButtonDefaults.buttonColors(containerColor = SonicEmerald),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_commit_audition")
                ) {
                    Text("Keep Tuning", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SoundProfilesAndPresetsSection(
    listeningProfiles: List<ListeningProfile>,
    selectedProfileId: String?,
    presets: List<Preset>,
    selectedPresetId: String,
    canUndo: Boolean,
    onSelectListeningProfile: (String) -> Unit,
    onSelectPreset: (String) -> Unit,
    onResetToFlat: () -> Unit,
    onUndo: () -> Unit,
    onShowSmartEq: () -> Unit,
    onShowSavePresetDialog: () -> Unit,
    onShowRenamePresetDialog: (Preset) -> Unit,
    onUpdateCurrentCustomPreset: (String) -> Unit,
    onShowDeletePresetDialog: (Preset) -> Unit,
    onShowPresetSafetyAudit: (Preset) -> Unit
) {
    val activePreset = presets.find { it.id == selectedPresetId }
    val isCustomTuning = selectedPresetId == "custom"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1 Header & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Acoustic Goals & Presets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Smart EQ Action Button
                Button(
                    onClick = onShowSmartEq,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_open_smart_eq")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Smart EQ", style = MaterialTheme.typography.labelSmall)
                }

                if (activePreset != null && activePreset.isCustom) {
                    IconButton(
                        onClick = { onUpdateCurrentCustomPreset(activePreset.id) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_update_custom_preset")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Update preset", tint = SonicCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onShowRenamePresetDialog(activePreset) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_rename_custom_preset")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename custom preset", tint = SonicCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onShowDeletePresetDialog(activePreset) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_delete_custom_preset")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete custom preset", tint = SonicRuby, modifier = Modifier.size(18.dp))
                    }
                } else if (isCustomTuning) {
                    OutlinedButton(
                        onClick = onShowSavePresetDialog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_save_as_preset")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Preset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Row 2: Sound Preference Goals (Phase 7)
        Text(
            text = "Listening Sound Goals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listeningProfiles.forEach { profile ->
                val isSelected = profile.id == selectedProfileId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectListeningProfile(profile.id) },
                    label = {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = getProfileIcon(profile.goal),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricIndigo.copy(alpha = 0.3f),
                        selectedLabelColor = ElectricIndigo
                    ),
                    modifier = Modifier.testTag("chip_listening_profile_${profile.id}")
                )
            }
        }

        // Row 3: Standard Genre Presets & Safety Audit
        Text(
            text = "Genre & Custom Presets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = preset.id == selectedPresetId
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) SonicCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreset(preset.id) }
                        .testTag("chip_preset_${preset.id}"),
                    color = if (isSelected) SonicCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (preset.isCustom) "★ ${preset.name}" else preset.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SonicCyan else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Preset Safety Audit Icon
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Audit preset safety",
                            tint = if (isSelected) SonicCyan.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onShowPresetSafetyAudit(preset) }
                                .testTag("btn_audit_preset_${preset.id}")
                        )
                    }
                }
            }

            // Quick Reset Flat
            OutlinedButton(
                onClick = onResetToFlat,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("btn_quick_reset_flat")
            ) {
                Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset Flat", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun getProfileIcon(goal: ListeningGoal): ImageVector = when (goal) {
    ListeningGoal.BALANCED -> Icons.Default.GraphicEq
    ListeningGoal.BASS_FOCUS -> Icons.Default.VolumeUp
    ListeningGoal.VOCAL_FOCUS -> Icons.Default.Mic
    ListeningGoal.DETAIL -> Icons.Default.Hearing
    ListeningGoal.WARM -> Icons.Default.MusicNote
    ListeningGoal.BRIGHT -> Icons.Default.Headphones
    ListeningGoal.RELAXED -> Icons.Default.AutoAwesome
}

@Composable
private fun EqualizerControlSection(
    bands: List<com.example.settings.model.BandSetting>,
    isEnabled: Boolean,
    onBandGainChanged: (Int, Float) -> Unit,
    onResetToFlat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_equalizer_canvas_sliders"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "5-Band Response Curve",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Logarithmic acoustic distribution (60 Hz to 14 kHz)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                TextButton(
                    onClick = onResetToFlat,
                    modifier = Modifier.testTag("btn_eq_reset_flat")
                ) {
                    Text("Flat (0 dB)", style = MaterialTheme.typography.labelSmall.copy(color = SonicCyan))
                }
            }

            // Interactive Curve Canvas with Logarithmic grid & Headroom visualization
            EqualizerCurveCanvas(
                bands = bands,
                isEnabled = isEnabled,
                onGainChanged = onBandGainChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("canvas_equalizer_curve")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 5 Vertical Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bands.forEach { band ->
                    VerticalBandSlider(
                        band = band,
                        isEnabled = isEnabled,
                        onGainChanged = { gain -> onBandGainChanged(band.bandIndex, gain) },
                        modifier = Modifier.testTag("slider_band_${band.bandIndex}")
                    )
                }
            }
        }
    }
}

@Composable
private fun DspHeadroomSafetySection(
    headroomAnalysis: com.example.audio.safety.HeadroomAnalysis,
    headroomMode: HeadroomMode,
    manualHeadroomDb: Float,
    onSetHeadroomMode: (HeadroomMode) -> Unit,
    onSetManualHeadroom: (Float) -> Unit,
    onShowNormalizeConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_dsp_headroom_safety"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Digital Headroom & Clipping Protection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time cascaded inter-stage gain & clipping prevention",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                val riskColor = when (headroomAnalysis.clippingRisk) {
                    ClippingRisk.SAFE -> SonicEmerald
                    ClippingRisk.WARNING -> SonicAmber
                    ClippingRisk.HIGH_RISK -> SonicRuby
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = headroomAnalysis.clippingRisk.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Headroom Mode Selector: [Automatic | Manual | Off]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeadroomMode.values().forEach { mode ->
                    val isSelected = (mode == headroomMode)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetHeadroomMode(mode) },
                        label = { Text(mode.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (mode == HeadroomMode.OFF) SonicRuby.copy(alpha = 0.2f) else SonicCyan.copy(alpha = 0.2f),
                            selectedLabelColor = if (mode == HeadroomMode.OFF) SonicRuby else SonicCyan
                        ),
                        modifier = Modifier.testTag("chip_headroom_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Manual Slider (if Manual Mode active)
            if (headroomMode == HeadroomMode.MANUAL) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Manual Attenuation:", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", manualHeadroomDb)} dB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SonicCyan)
                    }
                    Slider(
                        value = manualHeadroomDb,
                        onValueChange = onSetManualHeadroom,
                        valueRange = -12.0f..0.0f,
                        modifier = Modifier.testTag("slider_manual_headroom")
                    )
                }
            }

            // Headroom status readout box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Peak Boost",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = String.format("%+.1f dB", headroomAnalysis.totalAccumulatedGainDb),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Applied Headroom Offset",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (headroomAnalysis.isAutoHeadroomActive) {
                                String.format("%.1f dB (Active)", headroomAnalysis.autoHeadroomOffsetDb)
                            } else {
                                "0.0 dB (Off)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (headroomAnalysis.isAutoHeadroomActive) SonicEmerald else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Effective Output Peak",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = String.format("%+.1f dB", headroomAnalysis.effectivePeakGainDb),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (headroomAnalysis.effectivePeakGainDb > 0.1f) SonicAmber else SonicEmerald
                            )
                        )
                    }
                }
            }

            // Normalization Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtractive Normalization converts positive boosts to cuts, eliminating digital clipping.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onShowNormalizeConfirm,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_normalize_curve")
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Normalize Curve", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SoundTuningSection(
    settings: com.example.settings.model.EqualizerSettings,
    capabilities: com.example.audio.model.AudioCapabilities?,
    isEnabled: Boolean,
    onBassChanged: (Float) -> Unit,
    onTrebleChanged: (Float) -> Unit,
    onPreampChanged: (Float) -> Unit,
    onBalanceChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_sound_tuning"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sound Tuning & Balance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // 1. Bass Boost
            val isBassAvailable = capabilities?.isBassBoostAvailable == true
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bass Boost",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (isBassAvailable) "${(settings.bassLevel * 100).toInt()}%" else "Unavailable on this device",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isBassAvailable) SonicCyan else Color.Gray
                        )
                    )
                }
                Slider(
                    value = settings.bassLevel,
                    onValueChange = onBassChanged,
                    enabled = isEnabled && isBassAvailable,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = SonicCyan,
                        activeTrackColor = SonicCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_bass_boost")
                        .semantics {
                            contentDescription = "Bass Boost intensity slider, currently ${(settings.bassLevel * 100).toInt()} percent"
                        }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 2. Treble Boost (High Shelf 14kHz)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Treble Tone",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "High-shelf enhancement (14 kHz filter path)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = String.format("%+.1f dB", settings.trebleLevel),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SonicCyan
                        )
                    )
                }
                Slider(
                    value = settings.trebleLevel,
                    onValueChange = onTrebleChanged,
                    enabled = isEnabled,
                    valueRange = -10.0f..10.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = SonicCyan,
                        activeTrackColor = SonicCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_treble")
                        .semantics {
                            contentDescription = "Treble boost slider, currently ${String.format("%.1f", settings.trebleLevel)} dB"
                        }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 3. Preamp Gain
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Preamp Gain",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Digital input stage with clipping headroom management",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Text(
                        text = String.format("%+.1f dB", settings.preampLevel),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SonicCyan
                        )
                    )
                }
                Slider(
                    value = settings.preampLevel,
                    onValueChange = onPreampChanged,
                    enabled = isEnabled,
                    valueRange = -12.0f..12.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = SonicCyan,
                        activeTrackColor = SonicCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_preamp")
                        .semantics {
                            contentDescription = "Preamp gain slider, currently ${String.format("%.1f", settings.preampLevel)} dB"
                        }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 4. Stereo Balance
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stereo Balance",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = when {
                            settings.balance < -0.05f -> "Left ${(-settings.balance * 100).toInt()}%"
                            settings.balance > 0.05f -> "Right ${(settings.balance * 100).toInt()}%"
                            else -> "Center"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SonicCyan
                        )
                    )
                }
                Slider(
                    value = settings.balance,
                    onValueChange = onBalanceChanged,
                    enabled = isEnabled,
                    valueRange = -1.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = SonicCyan,
                        activeTrackColor = SonicCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_balance")
                        .semantics {
                            contentDescription = "Stereo balance slider, currently centered or offset"
                        }
                )
            }
        }
    }
}

@Composable
private fun CurrentOutputDeviceCard(
    currentDevice: AudioDevice?,
    activeProfile: DeviceProfile?,
    matchType: ProfileMatchType,
    lastApplyResult: ProfileApplyResult?,
    onOpenMyDevices: () -> Unit,
    onCreateProfile: () -> Unit
) {
    val dev = currentDevice ?: AudioDevice.defaultBuiltinSpeaker()
    val isSpeaker = dev.type == DeviceType.BUILTIN_SPEAKER

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_current_output_routing"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    isSpeaker -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    matchType == ProfileMatchType.EXACT_MATCH -> SonicEmerald.copy(alpha = 0.6f)
                    matchType == ProfileMatchType.STRONG_MATCH -> SonicCyan.copy(alpha = 0.6f)
                    matchType == ProfileMatchType.FALLBACK_MATCH -> SonicAmber.copy(alpha = 0.6f)
                    else -> ElectricIndigo.copy(alpha = 0.5f)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dev.type.isBluetooth -> SonicCyan.copy(alpha = 0.15f)
                                    dev.type == DeviceType.WIRED_HEADPHONES || dev.type == DeviceType.WIRED_EARPHONES -> ElectricIndigo.copy(alpha = 0.15f)
                                    dev.type == DeviceType.USB_AUDIO -> SonicEmerald.copy(alpha = 0.15f)
                                    dev.type == DeviceType.BUILTIN_SPEAKER -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                dev.type.isBluetooth -> Icons.Default.Bluetooth
                                dev.type == DeviceType.WIRED_HEADPHONES || dev.type == DeviceType.WIRED_EARPHONES -> Icons.Default.Headphones
                                dev.type == DeviceType.USB_AUDIO -> Icons.Default.Usb
                                dev.type == DeviceType.BUILTIN_SPEAKER -> Icons.Default.Speaker
                                else -> Icons.Default.Headphones
                            },
                            contentDescription = dev.type.displayName,
                            tint = when {
                                dev.type.isBluetooth -> SonicCyan
                                dev.type == DeviceType.WIRED_HEADPHONES || dev.type == DeviceType.WIRED_EARPHONES -> ElectricIndigo
                                dev.type == DeviceType.USB_AUDIO -> SonicEmerald
                                dev.type == DeviceType.BUILTIN_SPEAKER -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dev.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${dev.type.displayName} • ${dev.bluetoothInfo?.safeIdentifier ?: "System Route"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenMyDevices,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_manage_device_profiles")
                ) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Profiles", style = MaterialTheme.typography.labelSmall)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    activeProfile != null -> SonicEmerald.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (activeProfile != null) Icons.Default.Check else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (activeProfile != null) SonicEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = if (activeProfile != null)
                                    "Profile: ${activeProfile.name}"
                                else
                                    "No custom profile assigned",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeProfile != null) SonicEmerald else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (activeProfile != null) {
                                Text(
                                    text = "${matchType.displayName} • Auto-applied",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (activeProfile == null && !isSpeaker) {
                        TextButton(
                            onClick = onCreateProfile,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Create Profile", style = MaterialTheme.typography.labelSmall.copy(color = SonicCyan))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterSwitchCard(
    isEnabled: Boolean,
    engineState: AudioEngineState,
    capabilities: com.example.audio.model.AudioCapabilities?,
    onToggleEnabled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_master_switch"),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isEnabled) SonicCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) SonicCyan else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Master power toggle",
                        tint = if (isEnabled) Color.Black else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isEnabled) "DSP Engine Active" else "Equalizer Bypass",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when {
                            !isEnabled -> "Audio passed through without modification"
                            engineState.status == AudioEngineStatus.ACTIVE -> "Hardware AudioEffects active (Session #${engineState.activeSession.sessionId})"
                            else -> "DSP ready & operational"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SonicCyan,
                    checkedTrackColor = SonicCyan.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("switch_master_enable")
            )
        }
    }
}

@Composable
private fun SaveCustomPresetDialog(
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_save_preset"),
        title = {
            Text("Save Custom Preset", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Save current 5-band curve, bass boost, and treble settings as a reusable preset.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Preset Name") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_preset_name")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_preset_description")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        isError = true
                    } else {
                        onConfirm(name.trim(), description.trim())
                    }
                },
                modifier = Modifier.testTag("btn_confirm_save_preset")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameCustomPresetDialog(
    preset: Preset,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_rename_preset"),
        title = {
            Text("Rename Preset", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("New Preset Name") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_preset")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        isError = true
                    } else {
                        onConfirm(name.trim())
                    }
                },
                modifier = Modifier.testTag("btn_confirm_rename_preset")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteCustomPresetDialog(
    preset: Preset,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_delete_preset"),
        title = {
            Text("Delete Preset?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Text("Are you sure you want to permanently remove '${preset.name}'?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SonicRuby),
                modifier = Modifier.testTag("btn_confirm_delete_preset")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsModalSheet(
    settings: com.example.settings.model.EqualizerSettings,
    profiles: List<EarphoneProfile>,
    presets: List<Preset>,
    currentDevice: AudioDevice?,
    onSelectProfile: (String) -> Unit,
    onToggleAutoApply: (Boolean) -> Unit,
    onMapDeviceToPreset: (deviceId: String, presetId: String) -> Unit,
    onResetDefaults: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_settings")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer & Device Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Current Device Preset Auto-Apply Mapping
            currentDevice?.let { dev ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Active Device Mapping",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = SonicCyan)
                        )
                        Text(
                            text = "${dev.name} (${dev.type.displayName})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Auto-assign preset when this device connects:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val mappedPreset = settings.devicePresetMap[dev.id] ?: settings.devicePresetMap[dev.type.name]
                            presets.forEach { preset ->
                                val isMapped = mappedPreset == preset.id
                                FilterChip(
                                    selected = isMapped,
                                    onClick = { onMapDeviceToPreset(dev.id, preset.id) },
                                    label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SonicCyan,
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.testTag("chip_map_device_${preset.id}")
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Target Earphone Compensation Profiles",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SonicCyan
                )
            )

            profiles.forEach { profile ->
                val isSelected = profile.id == settings.selectedEarphoneProfileId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectProfile(profile.id) }
                        .testTag("card_profile_${profile.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSelected) SonicCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = profile.targetCompensation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = SonicCyan
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-Apply on Device Connected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Automatically apply mapped preset on Bluetooth / USB connection",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
                Switch(
                    checked = settings.autoApplyProfile,
                    onCheckedChange = onToggleAutoApply,
                    modifier = Modifier.testTag("switch_auto_apply")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenAbout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_settings_about_privacy")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SonicCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("About & Privacy Transparency", color = MaterialTheme.colorScheme.onSurface)
            }

            TextButton(
                onClick = {
                    onResetDefaults()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_reset_defaults")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Reset All Equalizer Settings to Default")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticsModalSheet(
    capabilities: com.example.audio.model.AudioCapabilities?,
    engineState: AudioEngineState,
    headroomAnalysis: com.example.audio.safety.HeadroomAnalysis,
    headroomMode: HeadroomMode,
    toneState: TestToneState,
    logs: List<AudioLogEntry>,
    currentDevice: AudioDevice?,
    activeProfile: DeviceProfile?,
    matchType: ProfileMatchType,
    lastApplyResult: ProfileApplyResult?,
    onSetHeadroomMode: (HeadroomMode) -> Unit = {},
    onShowNormalizeConfirm: () -> Unit = {},
    onPlayTone: (TestToneMode) -> Unit,
    onStopTone: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onRetryRecovery: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("sheet_diagnostics")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Diagnostics & DSP Inspector",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time hardware capability, session lifecycle & headroom safety",
                        style = MaterialTheme.typography.bodySmall.copy(color = SonicCyan)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Phase 8: Session State Machine & Production Lifecycle Card
            val sessionStateColor = when (engineState.sessionState) {
                AudioSessionState.ACTIVE -> SonicEmerald
                AudioSessionState.ATTACHED -> SonicCyan
                AudioSessionState.SESSION_DETECTED,
                AudioSessionState.INITIALIZING,
                AudioSessionState.RECOVERING -> SonicAmber
                AudioSessionState.CONTROL_LOST,
                AudioSessionState.TEMPORARILY_UNAVAILABLE,
                AudioSessionState.ERROR -> SonicRuby
                AudioSessionState.NO_SESSION,
                AudioSessionState.LOST -> Color.Gray
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = sessionStateColor.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, sessionStateColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Session Lifecycle",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = sessionStateColor)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sessionStateColor,
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = engineState.sessionState.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "• Session Status: ${engineState.sessionState.description}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                    Text(
                        text = "• Active Audio Session ID: #${engineState.activeSession.sessionId} ${if (engineState.activeSession.isGlobalMix) "(Global Mix 0)" else "(Direct Media Session)"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• Media Player Package: ${engineState.activePackageName ?: "System Audio Layer"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• DSP Control Ownership: ${if (engineState.hasEffectControl) "Active & Exclusive" else "Relinquished / External"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (engineState.hasEffectControl) SonicEmerald else SonicRuby,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "• Foreground Engine Process: PID ${engineState.processId}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )

                    if (engineState.recoveryAttempts > 0) {
                        Text(
                            text = "• Recovery Backoff Attempts: ${engineState.recoveryAttempts}/3",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SonicAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (engineState.lastFailureReason != null) {
                        Text(
                            text = "• Last Engine Event: ${engineState.lastFailureReason}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SonicRuby,
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (engineState.isRecoverable) {
                        Button(
                            onClick = onRetryRecovery,
                            colors = ButtonDefaults.buttonColors(containerColor = SonicCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("btn_retry_recovery")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Audio Effect Attachment", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // DSP Digital Headroom & Clipping Safety Card (Phase 7)
            val riskColor = when (headroomAnalysis.clippingRisk) {
                ClippingRisk.SAFE -> SonicEmerald
                ClippingRisk.WARNING -> SonicAmber
                ClippingRisk.HIGH_RISK -> SonicRuby
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DSP Headroom & Gain Accumulation",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = riskColor)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = riskColor,
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = headroomAnalysis.clippingRisk.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "• Headroom Mode: ${headroomMode.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• EQ Curve Max Boost: ${String.format("%+.1f dB", headroomAnalysis.maxEqBoostDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• BassBoost Contribution (est.): ${String.format("%+.1f dB", headroomAnalysis.estimatedBassBoostDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• Treble Shelf Gain (est.): ${String.format("%+.1f dB", headroomAnalysis.estimatedTrebleBoostDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• Preamp Level: ${String.format("%+.1f dB", headroomAnalysis.preampGainDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    Text(
                        text = "• Total Cascaded Positive Gain: ${String.format("%+.1f dB", headroomAnalysis.totalAccumulatedGainDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    )
                    Text(
                        text = "• Applied Headroom Offset: ${if (headroomAnalysis.isAutoHeadroomActive) String.format("%.1f dB (Active)", headroomAnalysis.autoHeadroomOffsetDb) else "Disabled (0.0 dB)"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (headroomAnalysis.isAutoHeadroomActive) SonicEmerald else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "• Effective Output Peak: ${String.format("%+.1f dB", headroomAnalysis.effectivePeakGainDb)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (headroomAnalysis.effectivePeakGainDb > 0.1f) SonicAmber else SonicEmerald
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onShowNormalizeConfirm,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Normalize EQ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Reference Test Tone Generator Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Acoustic Test Tone Reference Generator",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = SonicCyan)
                    )
                    Text(
                        text = "Calibrated audio signals (Pure Sines, Pink Noise, Log Sweep 20Hz-20kHz) routed through DSP.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Test Tone Mode Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TestToneMode.values().forEach { mode ->
                            val isPlayingThis = toneState.isPlaying && toneState.activeMode == mode
                            FilterChip(
                                selected = isPlayingThis,
                                onClick = {
                                    if (isPlayingThis) onStopTone() else onPlayTone(mode)
                                },
                                label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SonicEmerald,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("chip_tone_${mode.name.lowercase()}")
                            )
                        }
                    }

                    if (toneState.isPlaying) {
                        Button(
                            onClick = onStopTone,
                            colors = ButtonDefaults.buttonColors(containerColor = SonicRuby),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_stop_test_tone")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop Reference Audio")
                        }
                    }
                }
            }

            // Real-Time Audio Engine Logs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DSP & Event Telemetry Logs",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(logs.reversed()) { log ->
                                val color = when (log.level) {
                                    LogLevel.ERROR -> SonicRuby
                                    LogLevel.WARN -> SonicAmber
                                    LogLevel.INFO -> SonicCyan
                                    LogLevel.DEBUG -> Color.Gray
                                }
                                Text(
                                    text = "[${log.category.name}] ${log.tag}: ${log.message}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = color
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
