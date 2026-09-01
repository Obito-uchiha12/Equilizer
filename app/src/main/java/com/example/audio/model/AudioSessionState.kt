package com.example.audio.model

/**
 * Formalized audio session lifecycle states for production DSP engine management.
 */
enum class AudioSessionState(val displayName: String, val description: String) {
    /** No media session detected yet; standing by on Global Mix session 0. */
    NO_SESSION("No Session", "No active media audio session detected. Standing by on Global Mix 0."),

    /** New audio session broadcast received from an external media player. */
    SESSION_DETECTED("Session Detected", "New audio session detected from media application."),

    /** Native Equalizer & BassBoost AudioEffects are being created and bound. */
    INITIALIZING("Initializing", "Binding DSP effects to target audio session."),

    /** Native AudioEffects instantiated and attached to target audio session. */
    ATTACHED("Attached", "Hardware Equalizer and BassBoost attached to session."),

    /** Audio effects enabled and actively filtering audio in real time. */
    ACTIVE("Active", "DSP processing audio in real time."),

    /** Another audio application claimed exclusive control ownership. */
    CONTROL_LOST("Control Lost", "AudioEffect control claimed by another audio component."),

    /** Media session closed by player; reverting to global mix baseline. */
    LOST("Session Closed", "Media session closed by source player. Reverting to baseline."),

    /** Attempting bounded exponential backoff recovery to regain effect control. */
    RECOVERING("Recovering", "Attempting bounded exponential backoff recovery."),

    /** Maximum recovery retries reached; ready to reattach on next session or route event. */
    TEMPORARILY_UNAVAILABLE("Temporarily Unavailable", "Effect control unavailable. Ready to reattach on next session/route event."),

    /** Native AudioEffect instantiation failed or unrecoverable DSP error. */
    ERROR("Error", "Native AudioEffect instantiation failed.")
}
