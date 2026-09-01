package com.example.audio

import com.example.audio.model.AudioCapabilities
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineState
import com.example.audio.model.AudioSessionInfo
import com.example.core.result.AudioResult
import com.example.device.model.AudioDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Audio Engine contract for sound equalization and audio enhancement.
 * Real DSP implementations attach to Android's AudioEffect system.
 */
interface AudioEngine {
    val engineState: StateFlow<AudioEngineState>
    val capabilities: AudioCapabilities

    fun initialize(sessionInfo: AudioSessionInfo = AudioSessionInfo.GLOBAL): AudioResult<Unit>
    fun initialize(sessionInfo: AudioSessionInfo, packageName: String?): AudioResult<Unit> = initialize(sessionInfo)
    fun setEnabled(enabled: Boolean): AudioResult<Boolean>
    fun applyConfiguration(config: AudioEngineConfig): AudioResult<Unit>
    fun onDeviceChanged(device: AudioDevice) { /* Default no-op */ }
    fun retryRecovery(): AudioResult<Unit> { return AudioResult.Success(Unit) }
    fun release()
}
