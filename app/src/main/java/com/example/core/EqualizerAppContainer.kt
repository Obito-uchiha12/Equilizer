package com.example.core

import android.content.Context
import com.example.audio.AndroidDspAudioEngine
import com.example.audio.AudioCapabilityDetector
import com.example.audio.AudioEngine
import com.example.audio.AudioSessionReceiver
import com.example.audio.DefaultAudioCapabilityDetector
import com.example.audio.TestToneGenerator
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioSessionInfo
import com.example.audio.service.EqualizerAudioService
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.device.AndroidAudioDeviceManager
import com.example.device.AudioDeviceManager
import com.example.device.profile.DefaultDeviceProfileManager
import com.example.device.profile.DeviceProfileManager
import com.example.settings.DefaultSettingsRepository
import com.example.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Application-level dependency container and singleton DSP lifecycle manager.
 *
 * Ensures:
 * 1. AudioEngine and AudioEffects live at Application scope, surviving Activity recreation and backgrounding.
 * 2. AudioSessionReceiver listens continuously for external media players (Spotify, YouTube Music, etc.).
 * 3. EqualizerAudioService is automatically launched in foreground when EQ is active and stopped when bypassed.
 * 4. Audio output route changes (Bluetooth, Wired, USB) auto-trigger appropriate EQ profile re-application.
 * 5. Test tones and background tasks are safely controlled without memory leaks or audio focus contention.
 */
class EqualizerAppContainer(
    val context: Context
) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val capabilityDetector: AudioCapabilityDetector by lazy {
        DefaultAudioCapabilityDetector(context)
    }

    val capabilities by lazy {
        capabilityDetector.detectCapabilities()
    }

    val audioEngine: AudioEngine by lazy {
        AndroidDspAudioEngine(context, capabilities, appScope)
    }

    val deviceManager: AudioDeviceManager by lazy {
        AndroidAudioDeviceManager(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(context)
    }

    val profileManager: DeviceProfileManager by lazy {
        DefaultDeviceProfileManager(settingsRepository)
    }

    val toneGenerator: TestToneGenerator by lazy {
        TestToneGenerator(appScope)
    }

    private var sessionReceiver: AudioSessionReceiver? = null

    fun initialize() {
        AppLogger.i(LogCategory.STARTUP, TAG, "Initializing EqualizerAppContainer at Application level...")

        // Register Application-scoped session receiver
        sessionReceiver = AudioSessionReceiver(
            onSessionOpened = { sessionInfo, packageName ->
                AppLogger.i(
                    LogCategory.AUDIO,
                    TAG,
                    "Application container received session #${sessionInfo.sessionId} (package=$packageName)"
                )
                audioEngine.initialize(sessionInfo, packageName)
            },
            onSessionClosed = { closedSessionId ->
                AppLogger.i(LogCategory.AUDIO, TAG, "Session #$closedSessionId closed, falling back to Global session 0")
                audioEngine.initialize(AudioSessionInfo.GLOBAL, null)
            }
        ).also { it.register(context) }

        // Observe device changes and re-apply configuration
        appScope.launch {
            deviceManager.currentOutputDevice.collectLatest { device ->
                AppLogger.d(LogCategory.DEVICE, TAG, "Device changed event received in AppContainer: ${device?.name}")
                if (device != null) {
                    audioEngine.onDeviceChanged(device)
                }

                // If test tone is playing and route disconnects, stop tone safely
                if (toneGenerator.state.value.isPlaying && (device == null || device.isDisconnectedOrBuiltIn())) {
                    AppLogger.i(LogCategory.AUDIO, TAG, "Audio route changed; stopping active test tone safely")
                    toneGenerator.stopTone()
                }
            }
        }

        // Observe settings changes and synchronize foreground service lifecycle
        appScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                if (settings.isEnabled) {
                    EqualizerAudioService.start(context)
                } else {
                    EqualizerAudioService.stop(context)
                }
            }
        }

        // Restore initial configuration from persistent repository
        appScope.launch {
            val initialSettings = settingsRepository.settings.value
            val config = AudioEngineConfig(
                isEnabled = initialSettings.isEnabled,
                bandGainsDb = initialSettings.bands.associate { it.bandIndex to it.gainDb },
                bassStrengthPercent = (initialSettings.bassLevel * 100).toInt(),
                trebleGainDb = initialSettings.trebleLevel,
                preampGainDb = initialSettings.preampLevel,
                stereoBalance = initialSettings.balance,
                autoHeadroomOffsetDb = if (initialSettings.headroomMode == com.example.settings.model.HeadroomMode.AUTOMATIC) {
                    val analysis = com.example.audio.safety.HeadroomCalculator.analyze(
                        bandGainsDb = initialSettings.bands.map { it.gainDb },
                        bassStrengthPercent = (initialSettings.bassLevel * 100).toInt(),
                        trebleGainDb = initialSettings.trebleLevel,
                        preampGainDb = initialSettings.preampLevel,
                        headroomMode = initialSettings.headroomMode
                    )
                    analysis.autoHeadroomOffsetDb
                } else if (initialSettings.headroomMode == com.example.settings.model.HeadroomMode.MANUAL) {
                    initialSettings.manualHeadroomDb
                } else 0.0f
            )
            audioEngine.applyConfiguration(config)
        }
    }

    private fun com.example.device.model.AudioDevice.isDisconnectedOrBuiltIn(): Boolean {
        return type == com.example.device.model.DeviceType.BUILTIN_SPEAKER ||
                type == com.example.device.model.DeviceType.OTHER
    }

    fun release() {
        AppLogger.i(LogCategory.ENGINE, TAG, "Releasing EqualizerAppContainer")
        sessionReceiver?.unregister(context)
        sessionReceiver = null
        toneGenerator.release()
        audioEngine.release()
    }

    companion object {
        private const val TAG = "EqualizerAppContainer"
    }
}
