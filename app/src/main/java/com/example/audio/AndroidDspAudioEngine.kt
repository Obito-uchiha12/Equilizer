package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.AudioEngineConfig
import com.example.audio.model.AudioEngineState
import com.example.audio.model.AudioEngineStatus
import com.example.audio.model.AudioSessionInfo
import com.example.audio.model.AudioSessionState
import com.example.audio.model.EqualizerBand
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.core.result.AudioCapabilityState
import com.example.core.result.AudioError
import com.example.core.result.AudioResult
import com.example.device.model.AudioDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real Android DSP Audio Engine implementation utilizing Android's [Equalizer]
 * and [BassBoost] AudioEffect infrastructure.
 *
 * Hardened for Phase 8 Production Lifecycle:
 * - Formal AudioSessionState lifecycle state machine (NO_SESSION -> SESSION_DETECTED -> INITIALIZING -> ATTACHED -> ACTIVE -> CONTROL_LOST -> RECOVERING -> TEMPORARILY_UNAVAILABLE)
 * - Bounded exponential backoff recovery on ownership loss (500ms, 1500ms, 4000ms, max 3 retries)
 * - Safe parameter debouncing (25ms) preventing IPC binder flooding
 * - Seamless audio route change re-application without audio glitching
 * - Memory-leak free lifecycle with explicit JNI & Binder teardown
 * - Honest control status reporting (never claiming ACTIVE if native control is lost)
 */
class AndroidDspAudioEngine(
    private val context: Context,
    initialCapabilities: AudioCapabilities,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AudioEngine {

    private val mutex = Mutex()

    private var activeEqualizer: Equalizer? = null
    private var activeBassBoost: BassBoost? = null
    private var activeSessionId: Int = AudioSessionInfo.GLOBAL_SESSION_ID
    private var activePackageName: String? = null
    private var isReleased = false
    private var lastConfig: AudioEngineConfig? = null

    // Debounce job for rapid slider updates
    private var pendingConfigJob: Job? = null
    private var lastApplyTimestamp = 0L

    // Reconnection & Recovery Backoff tracking
    private var lastInitAttemptTime = 0L
    private var consecutiveInitFailures = 0
    private var recoveryJob: Job? = null
    private var currentRecoveryAttempt = 0
    private val maxRecoveryAttempts = 3
    private val backoffDelaysMs = listOf(500L, 1500L, 4000L)

    private var _capabilities: AudioCapabilities = initialCapabilities
    override val capabilities: AudioCapabilities
        get() = _capabilities

    private val _engineState = MutableStateFlow(
        AudioEngineState(
            status = AudioEngineStatus.INITIALIZING,
            sessionState = AudioSessionState.INITIALIZING,
            isEnabled = false,
            activeSession = AudioSessionInfo.GLOBAL,
            appliedBandsCount = initialCapabilities.supportedBands.size,
            engineImplementation = "Android System AudioEffect DSP"
        )
    )
    override val engineState: StateFlow<AudioEngineState> = _engineState.asStateFlow()

    init {
        AppLogger.i(LogCategory.ENGINE, TAG, "Initializing AndroidDspAudioEngine on Global Session 0...")
        initialize(AudioSessionInfo.GLOBAL, null)
    }

    override fun initialize(sessionInfo: AudioSessionInfo): AudioResult<Unit> {
        return initialize(sessionInfo, null)
    }

    override fun initialize(sessionInfo: AudioSessionInfo, packageName: String?): AudioResult<Unit> {
        val now = System.currentTimeMillis()
        if (now - lastInitAttemptTime < 200 && consecutiveInitFailures > 2) {
            AppLogger.w(LogCategory.ENGINE, TAG, "Throttling initialization to prevent rapid spin-loops")
            return AudioResult.Failure(AudioError.AudioSessionUnavailable("Initialization throttled due to rapid retries"))
        }
        lastInitAttemptTime = now

        // Cancel any pending recovery jobs as a new explicit session/init event arrived
        recoveryJob?.cancel()
        recoveryJob = null
        currentRecoveryAttempt = 0

        _engineState.value = _engineState.value.copy(
            sessionState = AudioSessionState.SESSION_DETECTED,
            activePackageName = packageName
        )

        return try {
            AppLogger.i(
                LogCategory.ENGINE,
                TAG,
                "Initializing DSP effects on audioSessionId=${sessionInfo.sessionId} (isGlobalMix=${sessionInfo.isGlobalMix}, package=$packageName)"
            )

            // Safe release of previous effects if existing
            releaseInternalEffects()
            isReleased = false
            activeSessionId = sessionInfo.sessionId
            activePackageName = packageName

            _engineState.value = _engineState.value.copy(
                sessionState = AudioSessionState.INITIALIZING
            )

            // Broadcast session open intent to notify Android audio framework
            broadcastSessionControl(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, sessionInfo.sessionId)

            // Attempt Equalizer creation
            val (eq, detectedBands, minGainMb, maxGainMb) = tryCreateEqualizer(sessionInfo.sessionId)
            activeEqualizer = eq

            // Attempt BassBoost creation
            val (bb, bbSupported) = tryCreateBassBoost(sessionInfo.sessionId)
            activeBassBoost = bb

            // Set up control ownership and enable listeners
            setupEffectListeners(eq, bb)

            val overallState = when {
                eq != null && bbSupported -> AudioCapabilityState.SUPPORTED
                eq != null -> AudioCapabilityState.PARTIALLY_SUPPORTED
                else -> AudioCapabilityState.UNSUPPORTED
            }

            _capabilities = _capabilities.copy(
                overallState = overallState,
                isSystemEqualizerAvailable = (eq != null),
                isBassBoostAvailable = bbSupported,
                isTrebleAvailable = (eq != null),
                isPreampGainAvailable = (eq != null),
                maxBands = detectedBands.size,
                minGainDb = minGainMb / 100.0f,
                maxGainDb = maxGainMb / 100.0f,
                supportedBands = detectedBands.ifEmpty { EqualizerBand.default5Bands() },
                limitationNote = if (eq != null) {
                    if (sessionInfo.isGlobalMix) {
                        "Hardware Equalizer attached (Global Mix Session 0). Note: Global effect routing depends on OEM firmware support."
                    } else {
                        "Hardware Equalizer attached to active media session #${sessionInfo.sessionId}${if (packageName != null) " ($packageName)" else ""}."
                    }
                } else {
                    "System Equalizer effect unavailable in current audio session."
                }
            )

            val initialStatus = if (eq != null) AudioEngineStatus.DISABLED else AudioEngineStatus.ERROR
            val controlOwnership = (eq != null)
            val initialSessionState = if (eq != null) {
                AudioSessionState.ATTACHED
            } else {
                AudioSessionState.ERROR
            }

            _engineState.value = _engineState.value.copy(
                status = initialStatus,
                sessionState = initialSessionState,
                isEnabled = false,
                activeSession = sessionInfo,
                activePackageName = packageName,
                appliedBandsCount = detectedBands.size,
                errorMessage = if (eq == null) "Equalizer AudioEffect could not be created on session ${sessionInfo.sessionId}" else null,
                isHardwareAttached = (eq != null),
                hasEffectControl = controlOwnership,
                hardwareBandsCount = detectedBands.size,
                hardwareMinGainMilliBels = minGainMb,
                hardwareMaxGainMilliBels = maxGainMb,
                isBassBoostSupported = bbSupported,
                isBassBoostActive = false,
                recoveryAttempts = 0
            )

            if (eq != null) {
                consecutiveInitFailures = 0
                // Re-apply any previous config
                lastConfig?.let { cfg ->
                    applyConfigurationInternal(cfg)
                }
                AudioResult.Success(Unit)
            } else {
                consecutiveInitFailures++
                _engineState.value = _engineState.value.copy(
                    sessionState = AudioSessionState.ERROR,
                    lastFailureReason = "Failed to attach Equalizer AudioEffect to audio session ${sessionInfo.sessionId}"
                )
                AudioResult.Failure(
                    AudioError.AudioSessionUnavailable("Failed to attach Equalizer AudioEffect to audio session ${sessionInfo.sessionId}")
                )
            }
        } catch (e: Throwable) {
            consecutiveInitFailures++
            AppLogger.e(LogCategory.ENGINE, TAG, "Fatal error initializing AudioEngine", e)
            _engineState.value = _engineState.value.copy(
                status = AudioEngineStatus.ERROR,
                sessionState = AudioSessionState.ERROR,
                isEnabled = false,
                isHardwareAttached = false,
                lastFailureReason = e.message ?: "Unknown audio effect initialization error",
                errorMessage = e.message ?: "Unknown audio effect initialization error"
            )
            AudioResult.Failure(AudioError.InternalError("Failed to initialize Android DSP engine", e))
        }
    }

    private fun setupEffectListeners(eq: Equalizer?, bb: BassBoost?) {
        eq?.setControlStatusListener { _, hasControl ->
            AppLogger.i(LogCategory.ENGINE, TAG, "Equalizer control ownership changed: hasControl=$hasControl")
            if (hasControl) {
                recoveryJob?.cancel()
                recoveryJob = null
                currentRecoveryAttempt = 0
                val wasEnabled = lastConfig?.isEnabled == true
                _engineState.value = _engineState.value.copy(
                    hasEffectControl = true,
                    sessionState = if (wasEnabled) AudioSessionState.ACTIVE else AudioSessionState.ATTACHED,
                    status = if (wasEnabled) AudioEngineStatus.ACTIVE else AudioEngineStatus.DISABLED,
                    lastRecoveryTimestamp = System.currentTimeMillis()
                )
                // Re-apply parameters now that control is restored
                lastConfig?.let { applyConfigurationInternal(it) }
            } else {
                handleControlLost()
            }
        }

        eq?.setEnableStatusListener { _, enabled ->
            AppLogger.d(LogCategory.ENGINE, TAG, "Equalizer enable status changed externally: enabled=$enabled")
            _engineState.value = _engineState.value.copy(
                isEnabled = enabled,
                status = if (enabled) AudioEngineStatus.ACTIVE else AudioEngineStatus.DISABLED,
                sessionState = if (enabled) AudioSessionState.ACTIVE else AudioSessionState.ATTACHED
            )
        }

        bb?.setControlStatusListener { _, hasControl ->
            AppLogger.d(LogCategory.ENGINE, TAG, "BassBoost control ownership changed: hasControl=$hasControl")
        }
    }

    /**
     * Handles control loss event using bounded exponential backoff recovery.
     */
    private fun handleControlLost() {
        _engineState.value = _engineState.value.copy(
            hasEffectControl = false,
            isEnabled = false,
            status = AudioEngineStatus.DISABLED,
            sessionState = AudioSessionState.CONTROL_LOST,
            lastFailureReason = "AudioEffect control claimed by another application/component."
        )

        recoveryJob?.cancel()
        recoveryJob = scope.launch(Dispatchers.Default) {
            while (isActive && currentRecoveryAttempt < maxRecoveryAttempts) {
                val delayMs = backoffDelaysMs.getOrElse(currentRecoveryAttempt) { 4000L }
                currentRecoveryAttempt++

                _engineState.value = _engineState.value.copy(
                    sessionState = AudioSessionState.RECOVERING,
                    recoveryAttempts = currentRecoveryAttempt
                )

                AppLogger.w(
                    LogCategory.ENGINE,
                    TAG,
                    "Scheduling recovery attempt #$currentRecoveryAttempt in ${delayMs}ms..."
                )
                delay(delayMs)

                if (!isActive) break

                // Test if control is restored on active equalizer
                val eq = activeEqualizer
                if (eq != null && eq.hasControl()) {
                    AppLogger.i(LogCategory.ENGINE, TAG, "Equalizer control successfully regained on attempt #$currentRecoveryAttempt!")
                    _engineState.value = _engineState.value.copy(
                        hasEffectControl = true,
                        sessionState = if (lastConfig?.isEnabled == true) AudioSessionState.ACTIVE else AudioSessionState.ATTACHED,
                        lastRecoveryTimestamp = System.currentTimeMillis()
                    )
                    lastConfig?.let { applyConfigurationInternal(it) }
                    return@launch
                } else {
                    // Try reattaching session cleanly
                    AppLogger.d(LogCategory.ENGINE, TAG, "Recovery attempt #$currentRecoveryAttempt: reattaching effects...")
                    val result = initialize(AudioSessionInfo(activeSessionId, activeSessionId == 0), activePackageName)
                    if (result is AudioResult.Success && activeEqualizer?.hasControl() == true) {
                        AppLogger.i(LogCategory.ENGINE, TAG, "Equalizer reattachment recovery succeeded!")
                        return@launch
                    }
                }
            }

            // Exceeded max recovery attempts
            AppLogger.e(LogCategory.ENGINE, TAG, "Maximum recovery attempts ($maxRecoveryAttempts) reached. Transitioning to TEMPORARILY_UNAVAILABLE.")
            _engineState.value = _engineState.value.copy(
                sessionState = AudioSessionState.TEMPORARILY_UNAVAILABLE,
                lastFailureReason = "Effect control permanently claimed by external audio process. Ready to reattach on next session or route change."
            )
        }
    }

    override fun retryRecovery(): AudioResult<Unit> {
        AppLogger.i(LogCategory.ENGINE, TAG, "Manual recovery retry initiated by user/diagnostics.")
        currentRecoveryAttempt = 0
        return initialize(AudioSessionInfo(activeSessionId, activeSessionId == 0), activePackageName)
    }

    override fun setEnabled(enabled: Boolean): AudioResult<Boolean> {
        if (isReleased) {
            return AudioResult.Failure(AudioError.EngineNotInitialized("AudioEngine is released"))
        }

        val eq = activeEqualizer
        if (eq == null) {
            AppLogger.w(LogCategory.ENGINE, TAG, "Cannot enable EQ: Hardware Equalizer is null (Unsupported on session $activeSessionId)")
            _engineState.value = _engineState.value.copy(
                status = AudioEngineStatus.ERROR,
                sessionState = AudioSessionState.ERROR,
                isEnabled = false,
                errorMessage = "Equalizer AudioEffect is unavailable"
            )
            return AudioResult.Failure(AudioError.EffectUnsupported("android.media.audiofx.Equalizer"))
        }

        return try {
            eq.enabled = enabled
            val actualEnabled = eq.enabled

            val bb = activeBassBoost
            if (bb != null && bb.hasControl()) {
                val shouldEnableBb = actualEnabled && ((lastConfig?.bassStrengthPercent ?: 0) > 0)
                try {
                    bb.enabled = shouldEnableBb
                } catch (e: Exception) {
                    AppLogger.w(LogCategory.ENGINE, TAG, "Could not set BassBoost enabled state: ${e.message}")
                }
            }

            _engineState.value = _engineState.value.copy(
                isEnabled = actualEnabled,
                status = if (actualEnabled) AudioEngineStatus.ACTIVE else AudioEngineStatus.DISABLED,
                sessionState = if (actualEnabled) AudioSessionState.ACTIVE else AudioSessionState.ATTACHED,
                isBassBoostActive = (activeBassBoost?.enabled == true),
                lastAppliedTimestamp = System.currentTimeMillis()
            )

            AppLogger.i(LogCategory.ENGINE, TAG, "Hardware Equalizer enabled state set to: $actualEnabled")
            AudioResult.Success(actualEnabled)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.ENGINE, TAG, "Error toggling Equalizer enabled state", e)
            _engineState.value = _engineState.value.copy(
                status = AudioEngineStatus.ERROR,
                sessionState = AudioSessionState.ERROR,
                isEnabled = false,
                errorMessage = "Failed to toggle Equalizer effect: ${e.message}"
            )
            AudioResult.Failure(AudioError.InternalError("Failed to set equalizer enabled state", e))
        }
    }

    override fun applyConfiguration(config: AudioEngineConfig): AudioResult<Unit> {
        if (isReleased) {
            return AudioResult.Failure(AudioError.EngineNotInitialized("AudioEngine is released"))
        }

        lastConfig = config
        val now = System.currentTimeMillis()

        // Immediate application if enough time has passed; otherwise debounce rapid slider gestures (throttling IPC)
        if (now - lastApplyTimestamp > 25) {
            lastApplyTimestamp = now
            return applyConfigurationInternal(config)
        } else {
            pendingConfigJob?.cancel()
            pendingConfigJob = scope.launch(Dispatchers.Default) {
                delay(25)
                mutex.withLock {
                    applyConfigurationInternal(config)
                }
            }
            return AudioResult.Success(Unit)
        }
    }

    private fun applyConfigurationInternal(config: AudioEngineConfig): AudioResult<Unit> {
        val eq = activeEqualizer
        if (eq == null) {
            _engineState.value = _engineState.value.copy(
                isEnabled = false,
                status = AudioEngineStatus.ERROR,
                sessionState = AudioSessionState.ERROR,
                errorMessage = "Equalizer effect not attached"
            )
            return AudioResult.Failure(AudioError.EffectUnsupported("android.media.audiofx.Equalizer"))
        }

        return try {
            if (eq.enabled != config.isEnabled) {
                eq.enabled = config.isEnabled
            }

            val hwBands = _capabilities.supportedBands
            val mappings = EqualizerBandMapper.mapGainsToHardware(
                requestedGains = config.bandGainsDb,
                targetFrequenciesHz = EqualizerBandMapper.DEFAULT_TARGET_FREQUENCIES_HZ,
                hardwareBands = hwBands,
                minGainMilliBels = _capabilities.minGainDb.toInt() * 100,
                maxGainMilliBels = _capabilities.maxGainDb.toInt() * 100
            )

            val mappingSummaries = mutableListOf<String>()

            for (mapping in mappings) {
                var adjustedMilliBels = mapping.clampedGainMilliBels

                // Preamp digital gain offset applied across all filter bands
                if (config.preampGainDb != 0.0f) {
                    val preampOffsetMb = (config.preampGainDb * 100.0f).toInt()
                    adjustedMilliBels += preampOffsetMb
                }

                // Automatic Headroom digital attenuation offset to prevent digital clipping
                if (config.autoHeadroomOffsetDb != 0.0f) {
                    val autoHeadroomMb = (config.autoHeadroomOffsetDb * 100.0f).toInt()
                    adjustedMilliBels += autoHeadroomMb
                }

                // Treble shelf offset applied to the high-frequency band (Band 4 / 14kHz)
                if (mapping.requestedBandIndex == 4 && config.trebleGainDb != 0.0f) {
                    val trebleOffsetMb = (config.trebleGainDb * 100.0f).toInt()
                    adjustedMilliBels += trebleOffsetMb
                }

                val finalMilliBels = EqualizerBandMapper.clampGainToMilliBels(
                    gainDb = adjustedMilliBels / 100.0f,
                    minGainMilliBels = _engineState.value.hardwareMinGainMilliBels,
                    maxGainMilliBels = _engineState.value.hardwareMaxGainMilliBels
                )

                try {
                    eq.setBandLevel(
                        mapping.mappedHardwareBandIndex.toShort(),
                        finalMilliBels.toShort()
                    )
                } catch (e: Exception) {
                    AppLogger.w(LogCategory.ENGINE, TAG, "Error setting band ${mapping.mappedHardwareBandIndex} level: ${e.message}")
                }
                mappingSummaries.add(mapping.diagnosticSummary)
            }

            val bb = activeBassBoost
            var isBbActive = false
            if (bb != null && _capabilities.isBassBoostAvailable) {
                try {
                    val strength = (config.bassStrengthPercent * 10).coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                    val shouldEnable = config.isEnabled && config.bassStrengthPercent > 0
                    bb.enabled = shouldEnable
                    isBbActive = bb.enabled
                } catch (e: Exception) {
                    AppLogger.w(LogCategory.ENGINE, TAG, "BassBoost parameter update warning: ${e.message}")
                }
            }

            val maxEqGain = config.bandGainsDb.values.filter { it > 0f }.maxOrNull() ?: 0.0f
            val estimatedBassGain = (config.bassStrengthPercent / 100.0f) * 6.0f
            val estimatedTrebleGain = config.trebleGainDb.coerceAtLeast(0.0f)
            val totalAccumulated = (maxEqGain + estimatedBassGain + estimatedTrebleGain + config.preampGainDb).coerceAtLeast(0.0f)
            val effectivePeak = (totalAccumulated + config.autoHeadroomOffsetDb).coerceAtLeast(0.0f)
            val riskStr = when {
                config.autoHeadroomOffsetDb < 0.0f -> "SAFE (Auto Headroom Active)"
                totalAccumulated > 6.0f -> "HIGH_RISK"
                totalAccumulated > 1.0f -> "WARNING"
                else -> "SAFE"
            }

            val actualEnabled = eq.enabled
            _engineState.value = _engineState.value.copy(
                isEnabled = actualEnabled,
                status = if (actualEnabled) AudioEngineStatus.ACTIVE else AudioEngineStatus.DISABLED,
                sessionState = if (actualEnabled) AudioSessionState.ACTIVE else AudioSessionState.ATTACHED,
                appliedBandsCount = mappings.size,
                bandMappings = mappingSummaries,
                isBassBoostActive = isBbActive,
                bassBoostStrength = config.bassStrengthPercent,
                maxPositiveBoostDb = totalAccumulated,
                estimatedPeakGainDb = effectivePeak,
                recommendedHeadroomDb = if (totalAccumulated > 0f) -totalAccumulated else 0.0f,
                autoHeadroomOffsetDb = config.autoHeadroomOffsetDb,
                clippingRisk = riskStr,
                lastAppliedTimestamp = System.currentTimeMillis()
            )

            AudioResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.ENGINE, TAG, "Failed applying Equalizer configuration to hardware", e)
            _engineState.value = _engineState.value.copy(
                status = AudioEngineStatus.ERROR,
                sessionState = AudioSessionState.ERROR,
                errorMessage = "DSP error applying parameters: ${e.message}"
            )
            AudioResult.Failure(AudioError.InternalError("Failed applying EQ parameters to hardware", e))
        }
    }

    override fun onDeviceChanged(device: AudioDevice) {
        AppLogger.i(LogCategory.ENGINE, TAG, "Audio output route changed to: ${device.name} (${device.type})")
        // If engine was in temporary error or lost state, route change is a valid reset trigger
        if (_engineState.value.sessionState == AudioSessionState.TEMPORARILY_UNAVAILABLE) {
            currentRecoveryAttempt = 0
            initialize(AudioSessionInfo(activeSessionId, activeSessionId == 0), activePackageName)
        }

        val config = lastConfig
        if (config != null && !isReleased) {
            scope.launch {
                mutex.withLock {
                    applyConfigurationInternal(config)
                }
            }
        }
    }

    override fun release() {
        if (isReleased) {
            AppLogger.d(LogCategory.ENGINE, TAG, "AudioEngine already released. Ignoring duplicate call.")
            return
        }

        AppLogger.i(LogCategory.ENGINE, TAG, "Releasing Android DSP AudioEngine...")
        isReleased = true
        recoveryJob?.cancel()
        recoveryJob = null
        pendingConfigJob?.cancel()
        pendingConfigJob = null
        releaseInternalEffects()

        _engineState.value = _engineState.value.copy(
            status = AudioEngineStatus.UNINITIALIZED,
            sessionState = AudioSessionState.NO_SESSION,
            isEnabled = false,
            isHardwareAttached = false,
            hasEffectControl = false,
            isBassBoostActive = false,
            bandMappings = emptyList()
        )
    }

    private fun releaseInternalEffects() {
        try {
            activeEqualizer?.let { eq ->
                try {
                    eq.setControlStatusListener(null)
                    eq.setEnableStatusListener(null)
                    eq.enabled = false
                } catch (_: Exception) {}
                try {
                    eq.release()
                } catch (e: Exception) {
                    AppLogger.w(LogCategory.ENGINE, TAG, "Error releasing Equalizer: ${e.message}")
                }
            }
            activeEqualizer = null

            activeBassBoost?.let { bb ->
                try {
                    bb.setControlStatusListener(null)
                    bb.setEnableStatusListener(null)
                    bb.enabled = false
                } catch (_: Exception) {}
                try {
                    bb.release()
                } catch (e: Exception) {
                    AppLogger.w(LogCategory.ENGINE, TAG, "Error releasing BassBoost: ${e.message}")
                }
            }
            activeBassBoost = null

            if (activeSessionId != AudioSessionInfo.GLOBAL_SESSION_ID) {
                broadcastSessionControl(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, activeSessionId)
            }
        } catch (e: Exception) {
            AppLogger.e(LogCategory.ENGINE, TAG, "Exception in releaseInternalEffects", e)
        }
    }

    private fun tryCreateEqualizer(sessionId: Int): EqualizerCreationResult {
        return try {
            val eq = Equalizer(0, sessionId)
            val numBands = eq.numberOfBands.toInt()
            val levelRange = try {
                eq.bandLevelRange
            } catch (e: Exception) {
                shortArrayOf(-1500, 1500)
            }
            val minGainMb = levelRange[0].toInt()
            val maxGainMb = levelRange[1].toInt()

            val detectedBands = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val centerFreqMilliHz = eq.getCenterFreq(i.toShort())
                val centerFreqHz = (centerFreqMilliHz / 1000).coerceAtLeast(20)
                val currentMb = try {
                    eq.getBandLevel(i.toShort()).toInt()
                } catch (_: Exception) {
                    0
                }
                detectedBands.add(
                    EqualizerBand(
                        index = i,
                        centerFrequencyHz = centerFreqHz,
                        minGainMilliBels = minGainMb,
                        maxGainMilliBels = maxGainMb,
                        currentGainMilliBels = currentMb
                    )
                )
            }

            EqualizerCreationResult(
                equalizer = eq,
                bands = detectedBands,
                minGainMilliBels = minGainMb,
                maxGainMilliBels = maxGainMb
            )
        } catch (e: Throwable) {
            AppLogger.w(LogCategory.ENGINE, TAG, "Hardware Equalizer creation failed on session $sessionId: ${e.message}")
            EqualizerCreationResult(
                equalizer = null,
                bands = emptyList(),
                minGainMilliBels = -1500,
                maxGainMilliBels = 1500
            )
        }
    }

    private fun tryCreateBassBoost(sessionId: Int): Pair<BassBoost?, Boolean> {
        return try {
            val bb = BassBoost(0, sessionId)
            val supported = bb.strengthSupported
            Pair(bb, supported)
        } catch (e: Throwable) {
            AppLogger.w(LogCategory.ENGINE, TAG, "BassBoost creation failed on session $sessionId: ${e.message}")
            Pair(null, false)
        }
    }

    private fun broadcastSessionControl(action: String, sessionId: Int) {
        try {
            val intent = Intent(action).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
            }
            context.sendBroadcast(intent)
            AppLogger.d(LogCategory.ENGINE, TAG, "Broadcasted session intent: $action for session $sessionId")
        } catch (e: Exception) {
            AppLogger.w(LogCategory.ENGINE, TAG, "Failed sending session broadcast $action: ${e.message}")
        }
    }

    private data class EqualizerCreationResult(
        val equalizer: Equalizer?,
        val bands: List<EqualizerBand>,
        val minGainMilliBels: Int,
        val maxGainMilliBels: Int
    )

    companion object {
        private const val TAG = "DspAudioEngine"
    }
}
