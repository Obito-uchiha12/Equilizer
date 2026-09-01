package com.example.device

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.core.result.AudioError
import com.example.core.result.AudioResult
import com.example.device.model.AudioDevice
import com.example.device.model.BluetoothDeviceInfo
import com.example.device.model.ConnectionState
import com.example.device.model.DeviceAudioCapability
import com.example.device.model.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidAudioDeviceManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AudioDeviceManager {

    private val audioManager: AudioManager? = try {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    } catch (e: Exception) {
        AppLogger.e(LogCategory.DEVICE, TAG, "Failed to get AudioManager service", e)
        null
    }

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    override val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

    private val _currentOutputDevice = MutableStateFlow<AudioDevice?>(null)
    override val currentOutputDevice: StateFlow<AudioDevice?> = _currentOutputDevice.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null

    init {
        AppLogger.i(LogCategory.DEVICE, TAG, "Initializing AndroidAudioDeviceManager")
        registerDeviceCallback()
        refreshDevices()
    }

    private fun registerDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            try {
                deviceCallback = object : AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                        AppLogger.i(LogCategory.DEVICE, TAG, "Audio devices added: ${addedDevices?.size ?: 0}")
                        refreshDevices()
                    }

                    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                        AppLogger.i(LogCategory.DEVICE, TAG, "Audio devices removed: ${removedDevices?.size ?: 0}")
                        refreshDevices()
                    }
                }
                audioManager.registerAudioDeviceCallback(deviceCallback, null)
                AppLogger.d(LogCategory.DEVICE, TAG, "Registered AudioDeviceCallback successfully")
            } catch (e: Exception) {
                AppLogger.w(LogCategory.DEVICE, TAG, "Could not register AudioDeviceCallback: ${e.message}")
            }
        }
    }

    override fun refreshDevices(): AudioResult<List<AudioDevice>> {
        return try {
            val detected = queryAudioDevices()
            val finalDevices = if (detected.isEmpty()) {
                // Graceful fallback to default built-in speaker
                listOf(AudioDevice.defaultBuiltinSpeaker())
            } else {
                detected
            }

            _availableDevices.value = finalDevices

            // Find current active output device
            val active = finalDevices.firstOrNull { it.isCurrentOutput }
                ?: finalDevices.firstOrNull { it.type.isHeadphoneOrEarphone }
                ?: finalDevices.firstOrNull()
                ?: AudioDevice.defaultBuiltinSpeaker()

            _currentOutputDevice.value = active

            AppLogger.i(
                LogCategory.DEVICE,
                TAG,
                "Refreshed devices: count=${finalDevices.size}, active='${active.name}' (${active.type})"
            )

            AudioResult.Success(finalDevices)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.DEVICE, TAG, "Error during refreshDevices", e)
            val fallback = listOf(AudioDevice.defaultBuiltinSpeaker())
            _availableDevices.value = fallback
            _currentOutputDevice.value = fallback.first()
            AudioResult.Failure(AudioError.InternalError("Device query error", e))
        }
    }

    private fun queryAudioDevices(): List<AudioDevice> {
        val am = audioManager ?: return emptyList()
        val deviceList = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val audioDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (info in audioDevices) {
                    val mappedType = mapAudioDeviceType(info.type)
                    val productName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        info.productName?.toString()?.takeIf { it.isNotBlank() }
                    } else null

                    val displayName = productName ?: mappedType.displayName
                    val isBluetooth = mappedType.isBluetooth
                    val btInfo = if (isBluetooth) {
                        BluetoothDeviceInfo(
                            safeIdentifier = "bt_${info.id}",
                            profile = if (info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) "A2DP (Stereo Media)" else "SCO/BLE",
                            codec = null,
                            isLeAudio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 &&
                                    info.type == AudioDeviceInfo.TYPE_BLE_HEADSET,
                            isLowLatencySupported = false
                        )
                    } else null

                    val capabilities = mutableSetOf(DeviceAudioCapability.STEREO)
                    if (mappedType.isHeadphoneOrEarphone) {
                        capabilities.add(DeviceAudioCapability.SPATIAL_AUDIO)
                        capabilities.add(DeviceAudioCapability.HARDWARE_EQ)
                    }

                    deviceList.add(
                        AudioDevice(
                            id = "device_${info.id}_${info.type}",
                            name = displayName,
                            type = mappedType,
                            connectionState = ConnectionState.CONNECTED,
                            isCurrentOutput = isDeviceCurrentlyActive(info, mappedType),
                            bluetoothInfo = btInfo,
                            capabilities = capabilities
                        )
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(LogCategory.DEVICE, TAG, "Failed querying modern AudioDeviceInfo", e)
            }
        }

        // Check legacy wired headset state if list is empty
        if (deviceList.isEmpty()) {
            @Suppress("DEPRECATION")
            val isWiredOn = am.isWiredHeadsetOn
            if (isWiredOn) {
                deviceList.add(
                    AudioDevice(
                        id = "legacy_wired_earphones",
                        name = "Wired Earphones",
                        type = DeviceType.WIRED_EARPHONES,
                        connectionState = ConnectionState.CONNECTED,
                        isCurrentOutput = true,
                        capabilities = setOf(DeviceAudioCapability.STEREO, DeviceAudioCapability.SPATIAL_AUDIO)
                    )
                )
            } else {
                deviceList.add(AudioDevice.defaultBuiltinSpeaker())
            }
        }

        return deviceList
    }

    private fun mapAudioDeviceType(type: Int): DeviceType {
        return when (type) {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> DeviceType.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceType.WIRED_EARPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> DeviceType.BLUETOOTH_HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH_EARPHONES
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> DeviceType.BUILTIN_SPEAKER
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceType.USB_AUDIO
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                ) {
                    if (type == AudioDeviceInfo.TYPE_BLE_HEADSET) DeviceType.BLUETOOTH_EARPHONES
                    else DeviceType.BLUETOOTH_SPEAKER
                } else {
                    DeviceType.OTHER
                }
            }
        }
    }

    private fun isDeviceCurrentlyActive(info: AudioDeviceInfo, mappedType: DeviceType): Boolean {
        val am = audioManager ?: return false
        @Suppress("DEPRECATION")
        return when (mappedType) {
            DeviceType.WIRED_EARPHONES, DeviceType.WIRED_HEADPHONES -> am.isWiredHeadsetOn
            DeviceType.BLUETOOTH_HEADPHONES, DeviceType.BLUETOOTH_EARPHONES, DeviceType.BLUETOOTH_SPEAKER -> am.isBluetoothA2dpOn
            DeviceType.BUILTIN_SPEAKER -> !am.isWiredHeadsetOn && !am.isBluetoothA2dpOn && !am.isSpeakerphoneOn
            else -> false
        }
    }

    override fun selectDevice(deviceId: String): AudioResult<AudioDevice> {
        val found = _availableDevices.value.find { it.id == deviceId }
        return if (found != null) {
            _currentOutputDevice.value = found
            AppLogger.i(LogCategory.DEVICE, TAG, "Selected device manually: ${found.name}")
            AudioResult.Success(found)
        } else {
            AudioResult.Failure(AudioError.DeviceNotFound(deviceId))
        }
    }

    override fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            try {
                audioManager?.unregisterAudioDeviceCallback(deviceCallback)
                AppLogger.d(LogCategory.DEVICE, TAG, "Unregistered AudioDeviceCallback")
            } catch (e: Exception) {
                AppLogger.w(LogCategory.DEVICE, TAG, "Error unregistering callback: ${e.message}")
            }
            deviceCallback = null
        }
    }

    companion object {
        private const val TAG = "AudioDeviceMgr"
    }
}
