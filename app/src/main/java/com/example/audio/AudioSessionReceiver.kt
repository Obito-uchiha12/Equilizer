package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import com.example.audio.model.AudioSessionInfo
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory

/**
 * Broadcast receiver listening for system and media player session events:
 * - AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
 * - AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
 */
class AudioSessionReceiver(
    private val onSessionOpened: (AudioSessionInfo, String?) -> Unit,
    private val onSessionClosed: (Int) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioSessionInfo.GLOBAL_SESSION_ID)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)

        AppLogger.i(LogCategory.AUDIO, TAG, "Received audio session broadcast: $action, sessionId=$sessionId, package=$packageName")

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                val sessionInfo = AudioSessionInfo(
                    sessionId = sessionId,
                    isGlobalMix = (sessionId == AudioSessionInfo.GLOBAL_SESSION_ID)
                )
                onSessionOpened(sessionInfo, packageName)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                onSessionClosed(sessionId)
            }
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }
        AppLogger.d(LogCategory.AUDIO, TAG, "Registered AudioSessionReceiver")
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            AppLogger.d(LogCategory.AUDIO, TAG, "Unregistered AudioSessionReceiver")
        } catch (e: Exception) {
            AppLogger.w(LogCategory.AUDIO, TAG, "Error unregistering AudioSessionReceiver: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AudioSessionReceiver"
    }
}
