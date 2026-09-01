package com.example.core.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

enum class LogCategory {
    STARTUP,
    CAPABILITY,
    DEVICE,
    ENGINE,
    SETTINGS,
    AUDIO,
    ERROR
}

data class AudioLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: LogCategory,
    val tag: String,
    val message: String,
    val details: String? = null
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object AppLogger {
    private const val MAX_LOGS = 200
    private val _logs = MutableStateFlow<List<AudioLogEntry>>(emptyList())
    val logs: StateFlow<List<AudioLogEntry>> = _logs.asStateFlow()

    fun d(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.DEBUG, category, tag, message, details)
        try {
            Log.d("EQ_${tag}", "[$category] $message ${details?.let { "($it)" } ?: ""}")
        } catch (_: Throwable) {}
    }

    fun i(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.INFO, category, tag, message, details)
        try {
            Log.i("EQ_${tag}", "[$category] $message ${details?.let { "($it)" } ?: ""}")
        } catch (_: Throwable) {}
    }

    fun w(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.WARN, category, tag, message, details)
        try {
            Log.w("EQ_${tag}", "[$category] $message ${details?.let { "($it)" } ?: ""}")
        } catch (_: Throwable) {}
    }

    fun e(category: LogCategory, tag: String, message: String, throwable: Throwable? = null) {
        val details = throwable?.let { "${it.javaClass.simpleName}: ${it.message}" }
        log(LogLevel.ERROR, category, tag, message, details)
        try {
            Log.e("EQ_${tag}", "[$category] $message", throwable)
        } catch (_: Throwable) {}
    }

    private fun log(
        level: LogLevel,
        category: LogCategory,
        tag: String,
        message: String,
        details: String? = null
    ) {
        val entry = AudioLogEntry(
            level = level,
            category = category,
            tag = tag,
            message = message,
            details = details
        )
        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOGS) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
