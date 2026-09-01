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
import com.example.ui.components.AboutPrivacyDialog
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        composeTestRule.setContent {
            EqualizerAppTheme {
                AboutPrivacyDialog(
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
