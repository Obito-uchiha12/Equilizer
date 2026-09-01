package com.example.core.result

/**
 * Audio capability support state according to architectural guidelines.
 */
enum class AudioCapabilityState {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    UNSUPPORTED,
    ERROR;

    val isUsable: Boolean
        get() = this == SUPPORTED || this == PARTIALLY_SUPPORTED
}

/**
 * Centralized Audio/Device error hierarchy.
 * App will handle these gracefully and never crash.
 */
sealed class AudioError(
    val userFriendlyMessage: String,
    val technicalDetails: String? = null
) {
    data object NoDeviceConnected : AudioError(
        userFriendlyMessage = "No audio output device connected.",
        technicalDetails = "No external or built-in audio routing detected."
    )

    data object BluetoothDisabled : AudioError(
        userFriendlyMessage = "Bluetooth is currently disabled on this device.",
        technicalDetails = "Bluetooth adapter is turned off or unavailable."
    )

    data class AudioApiUnsupported(
        val apiLevel: Int,
        val reason: String
    ) : AudioError(
        userFriendlyMessage = "Audio effect API is not fully supported on this device version.",
        technicalDetails = "API level $apiLevel lacks support: $reason"
    )

    data class AudioSessionUnavailable(
        val reason: String
    ) : AudioError(
        userFriendlyMessage = "Audio session is not currently accessible.",
        technicalDetails = "Audio session failure: $reason"
    )

    data class EffectUnsupported(
        val effectName: String
    ) : AudioError(
        userFriendlyMessage = "Effect '$effectName' is not supported by device audio hardware.",
        technicalDetails = "AudioEffect $effectName not found or rejected by system."
    )

    data class DeviceNotFound(
        val deviceId: String
    ) : AudioError(
        userFriendlyMessage = "Audio device was disconnected or could not be found.",
        technicalDetails = "Target device id: $deviceId"
    )

    data class PermissionDenied(
        val permission: String
    ) : AudioError(
        userFriendlyMessage = "Permission required for this audio feature was denied.",
        technicalDetails = "Missing permission: $permission"
    )

    data class EngineNotInitialized(
        val reason: String = "Phase 1: Equalizer engine not yet attached."
    ) : AudioError(
        userFriendlyMessage = "Equalizer engine is not active.",
        technicalDetails = reason
    )

    data class InvalidParameter(
        val reason: String
    ) : AudioError(
        userFriendlyMessage = reason,
        technicalDetails = "Invalid parameter: $reason"
    )

    data class InternalError(
        val message: String,
        val cause: Throwable? = null
    ) : AudioError(
        userFriendlyMessage = "An audio system error occurred. Settings were preserved.",
        technicalDetails = "${cause?.javaClass?.simpleName ?: "Error"}: $message"
    )
}

/**
 * Generic result wrapper for audio and device operations.
 */
sealed class AudioResult<out T> {
    data class Success<out T>(val data: T) : AudioResult<T>()
    data class Failure(val error: AudioError) : AudioResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    inline fun onSuccess(action: (T) -> Unit): AudioResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (AudioError) -> Unit): AudioResult<T> {
        if (this is Failure) action(error)
        return this
    }
}
