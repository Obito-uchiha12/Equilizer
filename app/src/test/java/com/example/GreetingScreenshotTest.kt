package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.EqualizerBand
import com.example.core.result.AudioCapabilityState
import com.example.device.model.AudioDevice
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.EqualizerSettings
import com.example.settings.model.Preset
import com.example.ui.HomeScreen
import com.example.ui.HomeUiState
import com.example.ui.theme.EqualizerAppTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun equalizer_home_screen_screenshot() {
        val sampleUiState = HomeUiState(
            isInitialized = true,
            currentDevice = AudioDevice.defaultBuiltinSpeaker(),
            availableDevices = listOf(AudioDevice.defaultBuiltinSpeaker()),
            capabilities = AudioCapabilities(
                overallState = AudioCapabilityState.PARTIALLY_SUPPORTED,
                isSystemEqualizerAvailable = true,
                isBassBoostAvailable = true,
                isTrebleAvailable = true,
                isVirtualizerAvailable = false,
                isPreampGainAvailable = true,
                supportedBands = EqualizerBand.default5Bands()
            ),
            settings = EqualizerSettings(isEnabled = true),
            presets = Preset.defaultPresets(),
            profiles = EarphoneProfile.defaultProfiles()
        )

        composeTestRule.setContent {
            EqualizerAppTheme {
                HomeScreen(
                    uiState = sampleUiState,
                    onToggleEnabled = {},
                    onBandGainChanged = { _, _ -> },
                    onBassChanged = {},
                    onTrebleChanged = {},
                    onPreampChanged = {},
                    onBalanceChanged = {},
                    onSelectPreset = {},
                    onSelectEarphoneProfile = {},
                    onToggleAutoApplyProfile = {},
                    onRefreshDevices = {},
                    onSelectDevice = {},
                    onPlayTestTone = {},
                    onStopTestTone = {},
                    onSetTestToneVolume = {},
                    onResetDefaults = {},
                    onDismissNotification = {},
                    onDismissError = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onOpenDiagnostics = {},
                    onCloseDiagnostics = {},
                    onResetToFlat = {},
                    onShowSavePresetDialog = {},
                    onDismissSavePresetDialog = {},
                    onCreateCustomPreset = { _, _ -> },
                    onShowRenamePresetDialog = {},
                    onDismissRenamePresetDialog = {},
                    onRenameCustomPreset = { _, _ -> },
                    onUpdateCurrentCustomPreset = {},
                    onShowDeletePresetDialog = {},
                    onDismissDeletePresetDialog = {},
                    onDeleteCustomPreset = {},
                    onMapDeviceToPreset = { _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
