package com.example.device

import com.example.core.result.AudioResult
import com.example.device.model.AudioDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction layer for detecting and observing audio output devices.
 * Supports wired earphones, wired headphones, Bluetooth earphones/headphones/speakers,
 * built-in speakers, and USB DACs without hardcoding specific brands.
 */
interface AudioDeviceManager {
    /**
     * Flow of all currently available audio output devices.
     */
    val availableDevices: StateFlow<List<AudioDevice>>

    /**
     * Flow of the primary active output device.
     */
    val currentOutputDevice: StateFlow<AudioDevice?>

    /**
     * Refresh currently detected audio devices.
     */
    fun refreshDevices(): AudioResult<List<AudioDevice>>

    /**
     * Select preferred active audio device profile if applicable.
     */
    fun selectDevice(deviceId: String): AudioResult<AudioDevice>

    /**
     * Release listener resources.
     */
    fun release()
}
