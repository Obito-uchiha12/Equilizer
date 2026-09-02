package com.example

import com.example.audio.AudioCapabilityDetector
import com.example.audio.Phase1FoundationAudioEngine
import com.example.audio.TestToneMode
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.EqualizerBand
import com.example.core.result.AudioCapabilityState
import com.example.core.result.AudioResult
import com.example.device.AudioDeviceManager
import com.example.device.model.AudioDevice
import com.example.settings.DefaultSettingsRepository
import com.example.settings.model.Preset
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAudioDeviceManager : AudioDeviceManager {
        private val _availableDevices = MutableStateFlow(listOf(AudioDevice.defaultBuiltinSpeaker()))
        override val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

        private val _currentOutputDevice = MutableStateFlow<AudioDevice?>(AudioDevice.defaultBuiltinSpeaker())
        override val currentOutputDevice: StateFlow<AudioDevice?> = _currentOutputDevice.asStateFlow()

        override fun refreshDevices(): AudioResult<List<AudioDevice>> =
            AudioResult.Success(_availableDevices.value)

        override fun selectDevice(deviceId: String): AudioResult<AudioDevice> {
            val found = _availableDevices.value.find { it.id == deviceId }
            return if (found != null) {
                _currentOutputDevice.value = found
                AudioResult.Success(found)
            } else {
                AudioResult.Failure(com.example.core.result.AudioError.DeviceNotFound(deviceId))
            }
        }

        fun simulateNoDeviceConnected() {
            _availableDevices.value = emptyList()
            _currentOutputDevice.value = null
        }

        override fun release() {}
    }

    private class FakeCapabilityDetector(
        private val state: AudioCapabilityState = AudioCapabilityState.PARTIALLY_SUPPORTED
    ) : AudioCapabilityDetector {
        override fun detectCapabilities(): AudioCapabilities = AudioCapabilities(
            overallState = state,
            isSystemEqualizerAvailable = true,
            isBassBoostAvailable = true,
            isTrebleAvailable = true,
            isVirtualizerAvailable = false,
            isPreampGainAvailable = true,
            supportedBands = EqualizerBand.default5Bands()
        )
    }

    private lateinit var deviceManager: FakeAudioDeviceManager
    private lateinit var settingsRepository: DefaultSettingsRepository
    private lateinit var profileManager: com.example.device.profile.DefaultDeviceProfileManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceManager = FakeAudioDeviceManager()
        val detector = FakeCapabilityDetector()
        val capabilities = detector.detectCapabilities()
        val audioEngine = Phase1FoundationAudioEngine(capabilities)
        settingsRepository = DefaultSettingsRepository()
        profileManager = com.example.device.profile.DefaultDeviceProfileManager(settingsRepository)

        viewModel = MainViewModel(
            deviceManager = deviceManager,
            capabilityDetector = detector,
            settingsRepository = settingsRepository,
            audioEngine = audioEngine,
            profileManager = profileManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewmodel initial state is valid and does not crash`() {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertFalse(state.settings.isEnabled)
        assertNotNull(state.capabilities)
        assertEquals(AudioCapabilityState.PARTIALLY_SUPPORTED, state.capabilities?.overallState)
        assertTrue(state.presets.isNotEmpty())
        assertTrue(state.deviceProfiles.isNotEmpty())
    }

    @Test
    fun `toggling master switch updates state flow`() {
        viewModel.onToggleEnabled(true)
        assertTrue(settingsRepository.settings.value.isEnabled)

        viewModel.onToggleEnabled(false)
        assertFalse(settingsRepository.settings.value.isEnabled)
    }

    @Test
    fun `selecting preset propagates through viewModel`() {
        viewModel.onSelectPreset(Preset.ROCK.id)
        assertEquals(Preset.ROCK.id, settingsRepository.settings.value.selectedPresetId)
    }

    @Test
    fun `changing band gain updates repository`() {
        viewModel.onBandGainChanged(bandIndex = 0, gainDb = 8.0f)
        assertEquals(8.0f, settingsRepository.settings.value.bands[0].gainDb, 0.001f)
    }

    @Test
    fun `no-device state does not crash and handles null gracefully`() {
        deviceManager.simulateNoDeviceConnected()
        val state = viewModel.uiState.value
        assertNotNull(state)
    }

    @Test
    fun `test tone generator actions trigger cleanly`() {
        viewModel.onPlayTestTone(TestToneMode.MID_910HZ)
        viewModel.onSetTestToneVolume(0.5f)
        viewModel.onStopTestTone()
        val state = viewModel.uiState.value
        assertNotNull(state)
    }

    @Test
    fun `custom preset creation, rename, update and deletion lifecycle`() {
        // Create custom preset
        viewModel.onBandGainChanged(0, 4.0f)
        viewModel.onBassChanged(0.6f)
        viewModel.onCreateCustomPreset("My Custom EQ", "Heavy bass and boost")

        val presets = settingsRepository.availablePresets.value
        val created = presets.find { it.name == "My Custom EQ" }
        assertNotNull(created)
        assertTrue(created!!.isCustom)
        assertEquals(created.id, settingsRepository.settings.value.selectedPresetId)

        // Rename custom preset
        viewModel.onRenameCustomPreset(created.id, "Renamed EQ")
        val renamed = settingsRepository.availablePresets.value.find { it.id == created.id }
        assertEquals("Renamed EQ", renamed?.name)

        // Update custom preset with new values
        viewModel.onTrebleChanged(5.0f)
        viewModel.onUpdateCurrentCustomPreset(created.id)
        val updated = settingsRepository.availablePresets.value.find { it.id == created.id }
        assertEquals(5.0f, updated?.trebleGainDb ?: 0f, 0.01f)

        // Delete custom preset
        viewModel.onDeleteCustomPreset(created.id)
        assertFalse(settingsRepository.availablePresets.value.any { it.id == created.id })
    }

    @Test
    fun `modifying values automatically sets preset state to custom`() {
        viewModel.onSelectPreset(Preset.FLAT.id)
        assertEquals(Preset.FLAT.id, settingsRepository.settings.value.selectedPresetId)

        // Alter gain
        viewModel.onBandGainChanged(2, 5.5f)
        assertEquals("custom", settingsRepository.settings.value.selectedPresetId)

        // Reset to flat
        viewModel.onResetToFlat()
        assertEquals(Preset.FLAT.id, settingsRepository.settings.value.selectedPresetId)
        assertEquals(0.0f, settingsRepository.settings.value.bands[2].gainDb, 0.01f)
    }

    @Test
    fun `preamp, treble, and balance controls update repository and sync`() {
        viewModel.onPreampChanged(3.5f)
        assertEquals(3.5f, settingsRepository.settings.value.preampLevel, 0.01f)

        viewModel.onTrebleChanged(4.0f)
        assertEquals(4.0f, settingsRepository.settings.value.trebleLevel, 0.01f)

        viewModel.onBalanceChanged(-0.4f)
        assertEquals(-0.4f, settingsRepository.settings.value.balance, 0.01f)
    }

    @Test
    fun `device to preset mapping functions correctly`() {
        viewModel.onMapDeviceToPreset("bt_headphone_1", Preset.ROCK.id)
        assertEquals(Preset.ROCK.id, settingsRepository.getPresetForDevice("bt_headphone_1"))
    }

    @Test
    fun `device profile management sheet and dialog visibility states`() {
        viewModel.setMyDevicesSheetVisible(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMyDevicesSheetVisible)

        viewModel.setMyDevicesSheetVisible(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isMyDevicesSheetVisible)

        viewModel.showCreateDeviceProfileDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCreateDeviceProfileDialogVisible)

        viewModel.dismissCreateDeviceProfileDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCreateDeviceProfileDialogVisible)
    }

    @Test
    fun `creating and deleting device profile via ViewModel updates state`() {
        viewModel.onCreateDeviceProfile(
            name = "Test Earbuds",
            deviceType = com.example.device.model.DeviceType.BLUETOOTH_EARPHONES,
            targetDeviceId = null,
            targetProductName = "TestBuds",
            isGenericFallback = false,
            presetId = Preset.VOCAL_ENHANCE.id,
            bandGainsDb = listOf(0f, 1f, 2f, 1f, 0f),
            bassBoostPercent = 20,
            trebleGainDb = 1.0f,
            preampGainDb = 0.0f,
            balance = 0.0f,
            autoApplyEnabled = true,
            isDefaultAudioProfile = false
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val created = viewModel.uiState.value.deviceProfiles.find { it.name == "Test Earbuds" }
        assertNotNull(created)

        viewModel.onDeleteDeviceProfile(created!!.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.deviceProfiles.any { it.id == created.id })
    }

    @Test
    fun `toggle auto headroom and normalize curve actions in ViewModel`() {
        // Toggle auto headroom
        viewModel.onToggleAutoHeadroom(false)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.settings.isAutoHeadroomEnabled)
        assertFalse(viewModel.uiState.value.headroomAnalysis.isAutoHeadroomActive)

        viewModel.onToggleAutoHeadroom(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.settings.isAutoHeadroomEnabled)

        // Set aggressive boost
        viewModel.onBandGainChanged(0, 5.0f)
        viewModel.onBandGainChanged(4, 8.0f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.headroomAnalysis.isAutoHeadroomActive)
        assertEquals(-8.0f, viewModel.uiState.value.headroomAnalysis.autoHeadroomOffsetDb, 0.001f)

        // Normalize curve
        viewModel.onConfirmNormalizeCurve()
        testDispatcher.scheduler.advanceUntilIdle()

        val maxGain = viewModel.uiState.value.settings.bands.maxOf { it.gainDb }
        assertEquals(0.0f, maxGain, 0.001f)
    }

    @Test
    fun `onRetryRecovery executes cleanly without errors`() {
        viewModel.onRetryRecovery()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `about dialog visibility toggle in ViewModel`() {
        assertFalse(viewModel.uiState.value.isAboutDialogVisible)
        viewModel.showAboutDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAboutDialogVisible)

        viewModel.dismissAboutDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAboutDialogVisible)
    }
}
