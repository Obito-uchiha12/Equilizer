package com.example.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.EqualizerApplication
import com.example.MainActivity
import com.example.R
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Minimal, transparent Foreground Service for Android audio DSP processing.
 *
 * Responsibilities:
 * - Keeps the audio effect engine active and binder connection alive when backgrounded or screen locked.
 * - Displays a clear, honest notification detailing the current audio output route and active preset.
 * - Stops immediately and dismisses notification when Equalizer is disabled, consuming zero background battery.
 */
class EqualizerAudioService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(LogCategory.ENGINE, TAG, "EqualizerAudioService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        AppLogger.d(LogCategory.ENGINE, TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_STOP_SERVICE -> {
                val container = (application as? EqualizerApplication)?.container
                container?.audioEngine?.setEnabled(false)
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START_SERVICE -> {
                startForegroundWithNotification()
                observeEngineState()
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                observeEngineState()
                return START_STICKY
            }
        }
    }

    private fun startForegroundWithNotification() {
        val container = (application as? EqualizerApplication)?.container
        val deviceName = container?.deviceManager?.currentOutputDevice?.value?.name ?: "Current Audio Output"
        val presetName = container?.settingsRepository?.settings?.value?.selectedPresetId?.replaceFirstChar { it.uppercase() } ?: "Custom"

        val notification = buildNotification(deviceName, presetName)
        try {
            startForeground(NOTIFICATION_ID, notification)
            AppLogger.i(LogCategory.ENGINE, TAG, "EqualizerAudioService running in foreground")
        } catch (e: Exception) {
            AppLogger.e(LogCategory.ENGINE, TAG, "Failed starting foreground service", e)
        }
    }

    private fun observeEngineState() {
        stateObserverJob?.cancel()
        val container = (application as? EqualizerApplication)?.container ?: return

        stateObserverJob = serviceScope.launch {
            combine(
                container.audioEngine.engineState,
                container.deviceManager.currentOutputDevice,
                container.settingsRepository.settings
            ) { engineState, currentDevice, settings ->
                Triple(engineState, currentDevice, settings)
            }.collect { (engineState, currentDevice, settings) ->
                if (!engineState.isEnabled) {
                    AppLogger.i(LogCategory.ENGINE, TAG, "Equalizer disabled in background; stopping foreground service")
                    stopForegroundService()
                } else {
                    val deviceName = currentDevice?.name ?: "Current Audio Output"
                    val presetName = settings.selectedPresetId.replaceFirstChar { it.uppercase() }
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.notify(NOTIFICATION_ID, buildNotification(deviceName, presetName))
                }
            }
        }
    }

    private fun buildNotification(deviceName: String, presetName: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val turnOffIntent = Intent(this, EqualizerAudioService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val turnOffPendingIntent = PendingIntent.getService(
            this,
            1,
            turnOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Equalizer Active")
            .setContentText("Processing audio for $deviceName • $presetName")
            .setSubText("DSP Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Turn Off", turnOffPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun stopForegroundService() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        AppLogger.i(LogCategory.ENGINE, TAG, "EqualizerAudioService stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        stateObserverJob = null
        AppLogger.i(LogCategory.ENGINE, TAG, "EqualizerAudioService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Equalizer DSP Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time audio equalization processing status"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "EqAudioService"
        const val CHANNEL_ID = "equalizer_dsp_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_SERVICE = "com.example.action.START_EQUALIZER_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_EQUALIZER_SERVICE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, EqualizerAudioService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AppLogger.w(LogCategory.ENGINE, TAG, "Could not start EqualizerAudioService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, EqualizerAudioService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                AppLogger.w(LogCategory.ENGINE, TAG, "Could not stop EqualizerAudioService: ${e.message}")
            }
        }
    }
}
