package com.example.settings.model

enum class HeadroomMode(val displayName: String, val description: String) {
    AUTOMATIC("Automatic", "Dynamically calculates safe digital attenuation offset to eliminate digital clipping"),
    MANUAL("Manual", "User-specified fixed headroom attenuation level"),
    OFF("Protection Off", "Raw DSP gain output without attenuation. Caution: aggressive boosts may cause digital clipping.")
}
