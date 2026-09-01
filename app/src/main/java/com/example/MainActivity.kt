package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AndroidDspAudioEngine
import com.example.audio.AudioSessionReceiver
import com.example.audio.DefaultAudioCapabilityDetector
import com.example.audio.TestToneGenerator
import com.example.audio.model.AudioSessionInfo
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.device.AndroidAudioDeviceManager
import com.example.device.profile.DefaultDeviceProfileManager
import com.example.settings.DefaultSettingsRepository
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.EqualizerAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as EqualizerApplication
                val container = app.container

                return MainViewModel(
                    deviceManager = container.deviceManager,
                    capabilityDetector = container.capabilityDetector,
                    settingsRepository = container.settingsRepository,
                    audioEngine = container.audioEngine,
                    toneGenerator = container.toneGenerator,
                    profileManager = container.profileManager
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(LogCategory.STARTUP, TAG, "MainActivity onCreate launched")
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            EqualizerAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        uiState = uiState,
                        onToggleEnabled = viewModel::onToggleEnabled,
                        onBandGainChanged = viewModel::onBandGainChanged,
                        onBassChanged = viewModel::onBassChanged,
                        onTrebleChanged = viewModel::onTrebleChanged,
                        onPreampChanged = viewModel::onPreampChanged,
                        onBalanceChanged = viewModel::onBalanceChanged,
                        onSelectPreset = viewModel::onSelectPreset,
                        onSelectListeningProfile = viewModel::onSelectListeningProfile,
                        onSelectEarphoneProfile = viewModel::onSelectEarphoneProfile,
                        onToggleAutoApplyProfile = viewModel::onToggleAutoApplyProfile,
                        onSetHeadroomMode = viewModel::onSetHeadroomMode,
                        onSetManualHeadroom = viewModel::onSetManualHeadroom,
                        onUndo = viewModel::onUndo,
                        onRefreshDevices = viewModel::onRefreshDevices,
                        onSelectDevice = viewModel::onSelectDevice,
                        onPlayTestTone = viewModel::onPlayTestTone,
                        onStopTestTone = viewModel::onStopTestTone,
                        onSetTestToneVolume = viewModel::onSetTestToneVolume,
                        onResetDefaults = viewModel::onResetDefaults,
                        onDismissNotification = viewModel::onDismissNotification,
                        onDismissError = viewModel::onDismissError,
                        onOpenSettings = { viewModel.setSettingsSheetVisible(true) },
                        onCloseSettings = { viewModel.setSettingsSheetVisible(false) },
                        onOpenDiagnostics = { viewModel.setDiagnosticsVisible(true) },
                        onCloseDiagnostics = { viewModel.setDiagnosticsVisible(false) },
                        onOpenMyDevices = { viewModel.setMyDevicesSheetVisible(true) },
                        onCloseMyDevices = { viewModel.setMyDevicesSheetVisible(false) },
                        onShowCreateDeviceProfile = viewModel::showCreateDeviceProfileDialog,
                        onDismissCreateDeviceProfile = viewModel::dismissCreateDeviceProfileDialog,
                        onCreateDeviceProfile = viewModel::onCreateDeviceProfile,
                        onShowEditDeviceProfile = viewModel::showEditDeviceProfileDialog,
                        onDismissEditDeviceProfile = viewModel::dismissEditDeviceProfileDialog,
                        onUpdateDeviceProfile = viewModel::onUpdateDeviceProfile,
                        onShowDeleteDeviceProfile = viewModel::showDeleteDeviceProfileDialog,
                        onDismissDeleteDeviceProfile = viewModel::dismissDeleteDeviceProfileDialog,
                        onDeleteDeviceProfile = viewModel::onDeleteDeviceProfile,
                        onToggleProfileAutoApply = viewModel::onToggleProfileAutoApply,
                        onSetDefaultAudioProfile = viewModel::onSetDefaultAudioProfile,
                        onManualApplyDeviceProfile = viewModel::onManualApplyDeviceProfile,
                        onResetToFlat = viewModel::onResetToFlat,
                        onToggleAutoHeadroom = viewModel::onToggleAutoHeadroom,
                        onShowNormalizeConfirm = viewModel::showNormalizeConfirmDialog,
                        onDismissNormalizeConfirm = viewModel::dismissNormalizeConfirmDialog,
                        onConfirmNormalizeCurve = viewModel::onConfirmNormalizeCurve,
                        onShowSavePresetDialog = viewModel::showSavePresetDialog,
                        onDismissSavePresetDialog = viewModel::dismissSavePresetDialog,
                        onCreateCustomPreset = viewModel::onCreateCustomPreset,
                        onShowRenamePresetDialog = viewModel::showRenamePresetDialog,
                        onDismissRenamePresetDialog = viewModel::dismissRenamePresetDialog,
                        onRenameCustomPreset = viewModel::onRenameCustomPreset,
                        onUpdateCurrentCustomPreset = viewModel::onUpdateCurrentCustomPreset,
                        onShowDeletePresetDialog = viewModel::showDeletePresetDialog,
                        onDismissDeletePresetDialog = viewModel::dismissDeletePresetDialog,
                        onDeleteCustomPreset = viewModel::onDeleteCustomPreset,
                        onMapDeviceToPreset = viewModel::onMapDeviceToPreset,
                        // Phase 7 Smart EQ & Audition
                        onShowSmartEq = viewModel::showSmartEqDialog,
                        onDismissSmartEq = viewModel::dismissSmartEqDialog,
                        onSmartEqGoalChanged = viewModel::onSmartEqGoalChanged,
                        onSmartEqContextChanged = viewModel::onSmartEqContextChanged,
                        onSmartEqIntensityChanged = viewModel::onSmartEqIntensityChanged,
                        onApplySmartEq = viewModel::onApplySmartEqResult,
                        onStartPreview = viewModel::onStartPreview,
                        onToggleAbComparison = viewModel::onToggleAbComparison,
                        onCommitPreview = viewModel::onCommitPreview,
                        onCancelPreview = viewModel::onCancelPreview,
                        onShowPresetSafetyAudit = viewModel::showPresetSafetyAudit,
                        onDismissPresetSafetyAudit = viewModel::dismissPresetSafetyAudit,
                        // Phase 8 Recovery Retry
                        onRetryRecovery = viewModel::onRetryRecovery,
                        // Phase 9 About & Privacy
                        onOpenAbout = viewModel::showAboutDialog,
                        onCloseAbout = viewModel::dismissAboutDialog
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d(LogCategory.STARTUP, TAG, "MainActivity onDestroy (App container & background DSP continue running)")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
