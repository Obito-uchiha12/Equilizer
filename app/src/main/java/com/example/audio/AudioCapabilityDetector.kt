package com.example.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.os.Build
import com.example.audio.model.AudioCapabilities
import com.example.audio.model.EqualizerBand
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.core.result.AudioCapabilityState

interface AudioCapabilityDetector {
    fun detectCapabilities(): AudioCapabilities
}

class DefaultAudioCapabilityDetector(
    private val context: Context
) : AudioCapabilityDetector {

    override fun detectCapabilities(): AudioCapabilities {
        AppLogger.i(LogCategory.CAPABILITY, TAG, "Starting audio capability detection on Android API ${Build.VERSION.SDK_INT}")

        var hasEqualizerDescriptor = false
        var hasBassBoostDescriptor = false
        var hasVirtualizerDescriptor = false
        var hasPreampDescriptor = false

        try {
            val availableEffects = AudioEffect.queryEffects()
            if (availableEffects != null) {
                for (descriptor in availableEffects) {
                    val type = descriptor.type?.toString()?.uppercase() ?: ""
                    val name = descriptor.name ?: ""

                    if (type.contains("EQUALIZER") || name.contains("Equalizer", ignoreCase = true)) {
                        hasEqualizerDescriptor = true
                    }
                    if (type.contains("BASS") || name.contains("Bass", ignoreCase = true)) {
                        hasBassBoostDescriptor = true
                    }
                    if (type.contains("VIRTUALIZER") || name.contains("Virtualizer", ignoreCase = true)) {
                        hasVirtualizerDescriptor = true
                    }
                    if (type.contains("LOUDNESS") || type.contains("DYNAMICS") || name.contains("Gain", ignoreCase = true)) {
                        hasPreampDescriptor = true
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(LogCategory.CAPABILITY, TAG, "Could not query AudioEffect descriptors: ${e.message}")
        }

        // Test actual hardware Equalizer creation on session 0 to check if platform allows active effect instantiation
        var canInstantiateEqualizer = false
        var detectedBands: List<EqualizerBand> = EqualizerBand.default5Bands()
        var minGainDb = -15.0f
        var maxGainDb = 15.0f

        try {
            val testEq = Equalizer(0, 0)
            canInstantiateEqualizer = true
            val numBands = testEq.numberOfBands.toInt()
            val levelRange = testEq.bandLevelRange
            minGainDb = levelRange[0] / 100.0f
            maxGainDb = levelRange[1] / 100.0f

            val bands = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val centerFreqHz = (testEq.getCenterFreq(i.toShort()) / 1000).coerceAtLeast(20)
                bands.add(
                    EqualizerBand(
                        index = i,
                        centerFrequencyHz = centerFreqHz,
                        minGainMilliBels = levelRange[0].toInt(),
                        maxGainMilliBels = levelRange[1].toInt(),
                        currentGainMilliBels = 0
                    )
                )
            }
            if (bands.isNotEmpty()) {
                detectedBands = bands
            }
            testEq.release()
        } catch (e: Throwable) {
            AppLogger.d(LogCategory.CAPABILITY, TAG, "Probe Equalizer instantiation result: ${e.message}")
        }

        val hasEqualizer = canInstantiateEqualizer || hasEqualizerDescriptor

        val overallState = when {
            canInstantiateEqualizer -> AudioCapabilityState.SUPPORTED
            hasEqualizerDescriptor -> AudioCapabilityState.PARTIALLY_SUPPORTED
            else -> AudioCapabilityState.UNSUPPORTED
        }

        val capabilities = AudioCapabilities(
            overallState = overallState,
            isSystemEqualizerAvailable = hasEqualizer,
            isBassBoostAvailable = hasBassBoostDescriptor,
            isTrebleAvailable = hasEqualizer,
            isVirtualizerAvailable = hasVirtualizerDescriptor,
            isPreampGainAvailable = hasPreampDescriptor,
            maxBands = detectedBands.size,
            minGainDb = minGainDb,
            maxGainDb = maxGainDb,
            supportedBands = detectedBands,
            androidApiLevel = Build.VERSION.SDK_INT,
            hardwareManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            limitationNote = when (overallState) {
                AudioCapabilityState.SUPPORTED -> "Hardware Equalizer active and verified via Android AudioEffect framework."
                AudioCapabilityState.PARTIALLY_SUPPORTED -> "Audio effect descriptors found. Effect attachment may depend on active audio session."
                AudioCapabilityState.UNSUPPORTED -> "Hardware Equalizer effect is not supported or accessible on this device/environment."
                AudioCapabilityState.ERROR -> "Audio system error during capability detection."
            }
        )

        AppLogger.i(
            LogCategory.CAPABILITY,
            TAG,
            "Detected capabilities: state=${capabilities.overallState}, eq=$hasEqualizer, bass=$hasBassBoostDescriptor, bands=${detectedBands.size}"
        )

        return capabilities
    }

    companion object {
        private const val TAG = "AudioCapDetect"
    }
}
