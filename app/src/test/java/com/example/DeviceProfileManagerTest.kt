package com.example

import com.example.audio.Phase1FoundationAudioEngine
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.EqualizerBand
import com.example.core.result.AudioCapabilityState
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.model.ProfileApplyStatus
import com.example.device.model.ProfileMatchType
import com.example.device.profile.DefaultDeviceProfileManager
import com.example.settings.DefaultSettingsRepository
import com.example.settings.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceProfileManagerTest {

    private lateinit var settingsRepository: DefaultSettingsRepository
    private lateinit var profileManager: DefaultDeviceProfileManager
    private lateinit var audioEngine: Phase1FoundationAudioEngine

    private val testCapabilities = AudioCapabilities(
        overallState = AudioCapabilityState.SUPPORTED,
        isSystemEqualizerAvailable = true,
        isBassBoostAvailable = true,
        isTrebleAvailable = true,
        isVirtualizerAvailable = false,
        isPreampGainAvailable = true,
        supportedBands = EqualizerBand.default5Bands()
    )

    @Before
    fun setup() {
        settingsRepository = DefaultSettingsRepository()
        audioEngine = Phase1FoundationAudioEngine(testCapabilities)
        profileManager = DefaultDeviceProfileManager(settingsRepository)
    }

    @Test
    fun `initial profile state contains default fallback profiles`() {
        val profiles = profileManager.profiles.value
        assertTrue(profiles.isNotEmpty())
        assertTrue(profiles.any { it.isGenericFallback && it.deviceType == DeviceType.WIRED_EARPHONES })
        assertTrue(profiles.any { it.isGenericFallback && it.deviceType == DeviceType.BLUETOOTH_HEADPHONES })
        assertTrue(profiles.any { it.isGenericFallback && it.deviceType == DeviceType.USB_AUDIO })
    }

    @Test
    fun `custom profile creation saves and emits in profile list`() {
        val newProfile = DeviceProfile(
            name = "Sony WH-1000XM5",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            targetProductName = "WH-1000XM5",
            presetId = Preset.BASS_BOOST.id,
            bandGainsDb = listOf(5.0f, 3.0f, 0.0f, 2.0f, 4.0f),
            bassBoostPercent = 40,
            trebleGainDb = 2.5f,
            preampGainDb = -1.0f,
            balance = 0.0f,
            autoApplyEnabled = true
        )

        profileManager.saveProfile(newProfile)

        val retrieved = profileManager.profiles.value.find { it.id == newProfile.id }
        assertNotNull(retrieved)
        assertEquals("Sony WH-1000XM5", retrieved?.name)
        assertEquals(40, retrieved?.bassBoostPercent)
    }

    @Test
    fun `matching logic resolves exact device ID first`() {
        val exactProfile = DeviceProfile(
            name = "My Specific AirPods",
            deviceType = DeviceType.BLUETOOTH_EARPHONES,
            targetDeviceId = "bt_addr_00_11_22",
            targetProductName = "AirPods Pro",
            autoApplyEnabled = true
        )
        profileManager.saveProfile(exactProfile)

        val device = AudioDevice(
            id = "bt_addr_00_11_22",
            name = "AirPods Pro",
            type = DeviceType.BLUETOOTH_EARPHONES
        )

        val (matched, matchType) = profileManager.matchProfileForDevice(device)
        assertNotNull(matched)
        assertEquals(exactProfile.id, matched?.id)
        assertEquals(ProfileMatchType.EXACT_MATCH, matchType)
    }

    @Test
    fun `matching logic falls back to product name match when exact ID not found`() {
        val nameProfile = DeviceProfile(
            name = "Bose QuietComfort",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            targetDeviceId = null,
            targetProductName = "QuietComfort 45",
            autoApplyEnabled = true
        )
        profileManager.saveProfile(nameProfile)

        val device = AudioDevice(
            id = "bt_addr_99_88_77",
            name = "Bose QuietComfort 45",
            type = DeviceType.BLUETOOTH_HEADPHONES
        )

        val (matched, matchType) = profileManager.matchProfileForDevice(device)
        assertNotNull(matched)
        assertEquals(nameProfile.id, matched?.id)
        assertEquals(ProfileMatchType.STRONG_MATCH, matchType)
    }

    @Test
    fun `matching logic falls back to generic fallback profile for device type`() {
        val wiredDevice = AudioDevice(
            id = "wired_35mm_jack",
            name = "Wired 3.5mm Earphones",
            type = DeviceType.WIRED_EARPHONES
        )

        val (matched, matchType) = profileManager.matchProfileForDevice(wiredDevice)
        assertNotNull(matched)
        assertTrue(matched!!.isGenericFallback)
        assertEquals(DeviceType.WIRED_EARPHONES, matched.deviceType)
        assertEquals(ProfileMatchType.FALLBACK_MATCH, matchType)
    }

    @Test
    fun `matching logic does not apply earphone profiles to builtin phone speaker`() {
        val speakerDevice = AudioDevice.defaultBuiltinSpeaker()

        val (matched, matchType) = profileManager.matchProfileForDevice(speakerDevice)
        assertNull(matched)
        assertEquals(ProfileMatchType.NO_MATCH, matchType)
    }

    @Test
    fun `applying profile clamps out-of-range values safely`() {
        val extremeProfile = DeviceProfile(
            name = "Extreme Overdrive",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            bandGainsDb = listOf(50.0f, -50.0f, 99.0f, 0.0f, 0.0f), // Beyond ±15 dB
            bassBoostPercent = 250, // Beyond 100%
            trebleGainDb = 40.0f,   // Beyond ±10 dB
            preampGainDb = -99.0f,  // Beyond -12 dB
            balance = 5.0f          // Beyond +1.0
        )
        profileManager.saveProfile(extremeProfile)

        val device = AudioDevice(
            id = "test_headphone_id",
            name = "Studio Monitor",
            type = DeviceType.BLUETOOTH_HEADPHONES
        )

        val result = profileManager.applyProfile(device, extremeProfile, audioEngine, settingsRepository)
        assertEquals(ProfileApplyStatus.SUCCESS, result.status)

        val appliedSettings = settingsRepository.settings.value
        assertEquals(15.0f, appliedSettings.bands[0].gainDb, 0.01f)
        assertEquals(-15.0f, appliedSettings.bands[1].gainDb, 0.01f)
        assertEquals(1.0f, appliedSettings.bassLevel, 0.01f)
        assertEquals(10.0f, appliedSettings.trebleLevel, 0.01f)
        assertEquals(-12.0f, appliedSettings.preampLevel, 0.01f)
        assertEquals(1.0f, appliedSettings.balance, 0.01f)
    }

    @Test
    fun `applying profile on device without bass boost reports partial application`() {
        val noBassCapabilities = testCapabilities.copy(isBassBoostAvailable = false)
        val noBassEngine = Phase1FoundationAudioEngine(noBassCapabilities)

        val profileWithBass = DeviceProfile(
            name = "Bass Master",
            deviceType = DeviceType.WIRED_HEADPHONES,
            bassBoostPercent = 80
        )
        profileManager.saveProfile(profileWithBass)

        val device = AudioDevice(
            id = "wired_audiophile",
            name = "Audiophile Open Back",
            type = DeviceType.WIRED_HEADPHONES
        )

        val result = profileManager.applyProfile(device, profileWithBass, noBassEngine, settingsRepository)
        assertEquals(ProfileApplyStatus.PARTIAL, result.status)
        assertTrue(result.skippedParameters.any { it.contains("Bass Boost") })
    }

    @Test
    fun `auto-apply disabled profile is skipped during route changes`() {
        val manualOnlyProfile = DeviceProfile(
            name = "Manual EQ Only",
            deviceType = DeviceType.BLUETOOTH_HEADPHONES,
            targetProductName = "Studio 3",
            autoApplyEnabled = false
        )
        profileManager.saveProfile(manualOnlyProfile)

        val device = AudioDevice(
            id = "bt_studio_3",
            name = "Beats Studio 3",
            type = DeviceType.BLUETOOTH_HEADPHONES
        )

        val result = profileManager.onRouteChanged(device, audioEngine, settingsRepository)
        assertNotNull(result)
        assertTrue(result!!.skippedParameters.any { it.contains("Auto-apply disabled") })
        assertTrue(result.appliedParameters.isEmpty())
    }

    @Test
    fun `profile deletion removes from list and updates active state`() {
        val profile = DeviceProfile(
            name = "To Be Deleted",
            deviceType = DeviceType.USB_AUDIO
        )
        profileManager.saveProfile(profile)
        assertNotNull(profileManager.profiles.value.find { it.id == profile.id })

        profileManager.deleteProfile(profile.id)
        assertNull(profileManager.profiles.value.find { it.id == profile.id })
    }
}
