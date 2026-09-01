package com.example.device.model

enum class DeviceType(val displayName: String) {
    WIRED_EARPHONES("Wired Earphones"),
    WIRED_HEADPHONES("Wired Headphones"),
    BLUETOOTH_EARPHONES("Bluetooth Earphones"),
    BLUETOOTH_HEADPHONES("Bluetooth Headphones"),
    BLUETOOTH_SPEAKER("Bluetooth Speaker"),
    BUILTIN_SPEAKER("Phone Speaker"),
    USB_AUDIO("USB Audio Output"),
    OTHER("Audio Output Device");

    val isHeadphoneOrEarphone: Boolean
        get() = this == WIRED_EARPHONES ||
                this == WIRED_HEADPHONES ||
                this == BLUETOOTH_EARPHONES ||
                this == BLUETOOTH_HEADPHONES

    val isBluetooth: Boolean
        get() = this == BLUETOOTH_EARPHONES ||
                this == BLUETOOTH_HEADPHONES ||
                this == BLUETOOTH_SPEAKER
}

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    DISCONNECTING
}

enum class DeviceAudioCapability(val label: String) {
    STEREO("Stereo 2.0"),
    SPATIAL_AUDIO("Spatial Audio"),
    HIGH_RES_AUDIO("Hi-Res Audio"),
    HARDWARE_EQ("Hardware Tuning"),
    VOLUME_SYNC("Absolute Volume"),
    LOW_LATENCY("Low Latency")
}

data class BluetoothDeviceInfo(
    val safeIdentifier: String,
    val profile: String = "A2DP",
    val codec: String? = null,
    val isLeAudio: Boolean = false,
    val isLowLatencySupported: Boolean = false
)

data class AudioDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
    val isCurrentOutput: Boolean = false,
    val bluetoothInfo: BluetoothDeviceInfo? = null,
    val capabilities: Set<DeviceAudioCapability> = setOf(DeviceAudioCapability.STEREO),
    val customProfileId: String? = null
) {
    companion object {
        fun defaultBuiltinSpeaker(): AudioDevice = AudioDevice(
            id = "builtin_speaker_default",
            name = "Built-in Phone Speaker",
            type = DeviceType.BUILTIN_SPEAKER,
            connectionState = ConnectionState.CONNECTED,
            isCurrentOutput = true,
            capabilities = setOf(DeviceAudioCapability.STEREO)
        )
    }
}
