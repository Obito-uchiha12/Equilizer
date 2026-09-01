package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Built-in test tone modes for repeatable physical device audio validation.
 */
enum class TestToneMode(val displayName: String, val frequencyHz: Int, val description: String) {
    SUB_BASS_60HZ("60 Hz", 60, "Sub-Bass & BassBoost verification (Band 1)"),
    WARMTH_230HZ("230 Hz", 230, "Mid-Bass / Body & Warmth (Band 2)"),
    MID_910HZ("910 Hz", 910, "Midrange / Vocal clarity (Band 3)"),
    PRESENCE_3600HZ("3.6 kHz", 3600, "Presence / Upper Treble (Band 4)"),
    AIR_14000HZ("14 kHz", 14000, "Air / High-frequency brilliance (Band 5)"),
    SINE_SWEEP_20HZ_20KHZ("Sine Sweep", -2, "Logarithmic 20Hz-20kHz acoustic frequency sweep"),
    MULTI_TONE("Multi-Tone", 0, "Simultaneous harmonic test tones for full spectrum EQ checks"),
    PINK_NOISE("Pink Noise", -1, "Equal energy per octave for linear frequency response analysis")
}

data class TestToneState(
    val isPlaying: Boolean = false,
    val activeMode: TestToneMode = TestToneMode.MID_910HZ,
    val volume: Float = 0.5f,
    val sessionId: Int = 0,
    val sampleRate: Int = 44100
)

/**
 * Clean, lightweight, zero-external-dependency AudioTrack-based test tone generator.
 * Uses real-time 16-bit PCM mathematical synthesis to produce precise reference tones.
 */
class TestToneGenerator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(TestToneState())
    val state: StateFlow<TestToneState> = _state.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startTone(mode: TestToneMode, volume: Float = _state.value.volume) {
        stopTone()

        AppLogger.i(LogCategory.AUDIO, TAG, "Starting test tone: ${mode.displayName} (${mode.frequencyHz}Hz) at volume $volume")

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build()

            AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioTrack = track
        val assignedSessionId = track.audioSessionId

        _state.value = _state.value.copy(
            isPlaying = true,
            activeMode = mode,
            volume = volume,
            sessionId = assignedSessionId,
            sampleRate = sampleRate
        )

        try {
            track.setVolume(volume)
            track.play()
        } catch (e: Exception) {
            AppLogger.e(LogCategory.AUDIO, TAG, "Failed to start AudioTrack playback", e)
            stopTone()
            return
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            runToneLoop(track, mode, bufferSize)
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        try {
            audioTrack?.setVolume(clamped)
        } catch (e: Exception) {
            AppLogger.w(LogCategory.AUDIO, TAG, "Failed updating test tone volume: ${e.message}")
        }
    }

    fun stopTone() {
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            AppLogger.w(LogCategory.AUDIO, TAG, "Error releasing AudioTrack: ${e.message}")
        }
        audioTrack = null

        _state.value = _state.value.copy(
            isPlaying = false,
            sessionId = 0
        )
        AppLogger.d(LogCategory.AUDIO, TAG, "Test tone stopped.")
    }

    private fun runToneLoop(track: AudioTrack, mode: TestToneMode, bufferSize: Int) {
        val shortBuffer = ShortArray(bufferSize / 2)
        var phase = 0.0
        var phaseMulti = DoubleArray(5) { 0.0 }
        val multiFreqs = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 14000.0)

        // Pink noise generator state (Paul Kellet's filter method)
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        var b3 = 0.0
        var b4 = 0.0
        var b5 = 0.0
        var b6 = 0.0

        // Sine sweep parameters (20Hz to 20,000Hz over 5 seconds)
        val sweepStartFreq = 20.0
        val sweepEndFreq = 20000.0
        val sweepDurationSeconds = 5.0
        val totalSweepSamples = (sweepDurationSeconds * sampleRate).toLong()
        var sweepSampleIndex = 0L
        val lnFreqRatio = kotlin.math.ln(sweepEndFreq / sweepStartFreq)

        while (scope.isActive && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            when (mode) {
                TestToneMode.SINE_SWEEP_20HZ_20KHZ -> {
                    for (i in shortBuffer.indices) {
                        val t = (sweepSampleIndex % totalSweepSamples).toDouble() / sampleRate
                        val instantaneousPhase = 2.0 * PI * sweepStartFreq * (kotlin.math.exp(t / sweepDurationSeconds * lnFreqRatio) - 1.0) / (lnFreqRatio / sweepDurationSeconds)
                        val sample = (sin(instantaneousPhase) * 0.75 * Short.MAX_VALUE).toInt()
                        shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        sweepSampleIndex++
                    }
                }
                TestToneMode.MULTI_TONE -> {
                    for (i in shortBuffer.indices) {
                        var sum = 0.0
                        for (f in multiFreqs.indices) {
                            val freq = multiFreqs[f]
                            val phaseIncrement = (2.0 * PI * freq) / sampleRate
                            sum += sin(phaseMulti[f])
                            phaseMulti[f] = (phaseMulti[f] + phaseIncrement) % (2.0 * PI)
                        }
                        val sample = (sum / multiFreqs.size * 0.7 * Short.MAX_VALUE).toInt()
                        shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                TestToneMode.PINK_NOISE -> {
                    for (i in shortBuffer.indices) {
                        val white = (Math.random() * 2.0 - 1.0)
                        b0 = 0.99886 * b0 + white * 0.0555179
                        b1 = 0.99332 * b1 + white * 0.0750759
                        b2 = 0.96900 * b2 + white * 0.1538520
                        b3 = 0.86650 * b3 + white * 0.3104856
                        b4 = 0.55000 * b4 + white * 0.5329522
                        b5 = -0.7616 * b5 - white * 0.0168980
                        val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
                        b6 = white * 0.115926
                        val sample = (pink * Short.MAX_VALUE * 0.6).toInt()
                        shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
                else -> {
                    // Pure Sine Wave for specific band frequency
                    val freq = mode.frequencyHz.toDouble().coerceAtLeast(20.0)
                    val phaseIncrement = (2.0 * PI * freq) / sampleRate
                    for (i in shortBuffer.indices) {
                        val sample = (sin(phase) * 0.75 * Short.MAX_VALUE).toInt()
                        shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        phase = (phase + phaseIncrement) % (2.0 * PI)
                    }
                }
            }

            try {
                val written = track.write(shortBuffer, 0, shortBuffer.size)
                if (written < 0) {
                    AppLogger.w(LogCategory.AUDIO, TAG, "AudioTrack write error: $written")
                    break
                }
            } catch (e: Exception) {
                AppLogger.e(LogCategory.AUDIO, TAG, "Exception writing to AudioTrack", e)
                break
            }
        }
    }

    fun release() {
        stopTone()
    }

    companion object {
        private const val TAG = "TestToneGen"
    }
}
