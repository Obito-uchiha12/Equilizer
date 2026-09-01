package com.example

import android.app.Application
import com.example.core.EqualizerAppContainer
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory

/**
 * Application entry point for the Equalizer app.
 * Initializes Application-scoped singleton container and DSP engine lifecycle.
 */
class EqualizerApplication : Application() {

    lateinit var container: EqualizerAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(LogCategory.STARTUP, TAG, "EqualizerApplication onCreate")
        container = EqualizerAppContainer(applicationContext)
        container.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        container.release()
    }

    companion object {
        private const val TAG = "EqualizerApp"
    }
}
