package com.example

import com.example.device.model.AudioDevice
import com.example.device.model.BluetoothDeviceInfo
import com.example.device.model.ConnectionState
import com.example.device.model.DeviceAudioCapability
import com.example.device.model.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioDeviceModelTest {

    @Test
    fun `default built-in speaker device is properly configured`() {
        val speaker = AudioDevice.defaultBuiltinSpeaker()
        assertEquals("builtin_speaker_default", speaker.id)
        assertEquals(DeviceType.BUILTIN_SPEAKER, speaker.type)
        assertEquals(ConnectionState.CONNECTED, speaker.connectionState)
        assertTrue(speaker.isCurrentOutput)
        assertFalse(speaker.type.isHeadphoneOrEarphone)
        assertFalse(speaker.type.isBluetooth)
    }

    @Test
    fun `wired earphones device identification`() {
        val wired = AudioDevice(
            id = "wired_1",
            name = "Studio In-Ear Monitors",
            type = DeviceType.WIRED_EARPHONES,
            capabilities = setOf(DeviceAudioCapability.STEREO, DeviceAudioCapability.SPATIAL_AUDIO)
        )
        assertTrue(wired.type.isHeadphoneOrEarphone)
        assertFalse(wired.type.isBluetooth)
        assertTrue(wired.capabilities.contains(DeviceAudioCapability.SPATIAL_AUDIO))
    }

    @Test
    fun `bluetooth headphones model contains safe bluetooth info`() {
        val btDevice = AudioDevice(
            id = "bt_101",
            name = "Wireless Over-Ear ANC",
            type = DeviceType.BLUETOOTH_HEADPHONES,
            bluetoothInfo = BluetoothDeviceInfo(
                safeIdentifier = "bt_safe_id_101",
                profile = "A2DP",
                isLowLatencySupported = true
            )
        )
        assertTrue(btDevice.type.isBluetooth)
        assertTrue(btDevice.type.isHeadphoneOrEarphone)
        assertNotNull(btDevice.bluetoothInfo)
        assertEquals("A2DP", btDevice.bluetoothInfo?.profile)
        assertTrue(btDevice.bluetoothInfo?.isLowLatencySupported == true)
    }
}
