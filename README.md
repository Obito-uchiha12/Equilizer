# Equalizer

A privacy-focused, high-precision Android audio equalizer application designed for branded and non-branded earphones, headphones, Bluetooth audio devices, wired earphones, and supported USB audio devices.

---

## 🎧 Features

* **Real Android `AudioEffect` DSP**: Directly interacts with Android's native audio effects HAL (Equalizer, BassBoost, Virtualizer).
* **5-Band Equalizer**: Precision bi-quadratic peaking filters with center frequencies at `60 Hz`, `230 Hz`, `910 Hz`, `3.6 kHz`, and `14.0 kHz` (±15 dB gain range).
* **BassBoost & Treble Control**: Hardware-accelerated bass boosting where supported, along with treble shelving adjustment.
* **Preamp & Stereo Balance**: Fine-grain pre-amplification headroom and stereo channel panning balance.
* **Automatic Headroom Protection**: Real-time compound gain analysis with automatic negative pre-gain attenuation to prevent digital clipping distortion.
* **Manual Headroom Mode**: User-configurable fixed attenuation levels (0 to -12 dB) with live clipping risk metrics.
* **Smart EQ Tuning Assistant**: Goal-driven acoustic curve generator tailored to listening preferences (e.g. Bass Punch, Vocal Clarity, Gaming, Acoustic Warmth) and device acoustic signatures.
* **Curated & Custom Presets**: Built-in genre curves (Flat, Bass Boost, Vocal Clarity, Treble Boost, Rock, Acoustic, Electronic) with custom preset creation, editing, safety audits, and persistence.
* **Per-Device Audio Profiles**: Automatic device profile detection and instant EQ switching when connecting Bluetooth earbuds, over-ear headphones, wired 3.5mm jacks, or USB audio adapters.
* **A/B Instant Comparison Mode**: Seamless, low-latency one-tap toggle between active equalization and a neutral flat baseline.
* **Integrated Calibration Tones**: Diagnostic sine wave generator (60 Hz, 230 Hz, 910 Hz, 3.6 kHz, 14 kHz), pink noise, and 20 Hz–20 kHz logarithmic sweeps with automatic safety shutoff.
* **Real-Time Audio Diagnostics**: Transparent telemetry on audio session control, HAL support level, and OEM audio policy state.
* **100% Offline & Sandboxed**: All settings, profiles, and custom curves are stored exclusively in local device-encrypted storage.

---

## 🔒 Privacy Architecture

Equalizer is engineered from the ground up to respect user privacy:

* **Zero Internet Access**: No `android.permission.INTERNET` permission requested. Zero remote network calls, zero web requests.
* **Zero Microphone Access**: No `RECORD_AUDIO` permission required. Processing is applied directly to the audio playback output pipeline.
* **Zero Location / Storage Access**: Does not require device location, media, or external storage access.
* **Zero Telemetry / Tracking**: No third-party analytics SDKs, no crash reporting beacons, no advertising frameworks.
* **100% Local Storage**: All presets and preferences remain strictly on your device.

---

## ⚠️ Known Platform & OEM Limitations

Audio effect processing on Android relies on the underlying device HAL and manufacturer implementation:

1. **OEM Audio Policy Overrides**: Certain manufacturers (such as Samsung SoundAlive/OneUI, Xiaomi Dirac, OnePlus Dolby Atmos, or Sony DSEE) may enforce system-level audio routing policies that limit third-party audio effects on specific system apps.
2. **Hardware Effect Availability**: BassBoost and Virtualizer effects are restricted by Android's audio architecture to headphone/headset output routes and may be unavailable on internal phone speakers.
3. **Session Control Ownership**: If another active audio application requests exclusive audio focus or manages its own internal effect chain, system audio effect priority may temporarily shift. The app provides live diagnostics and a one-tap recovery retry flow.

---

## 🚀 CI / CD Pipeline

The repository uses automated GitHub Actions workflows for continuous integration and production releases:

### 🛠️ Development (Continuous Integration)
* **Trigger**: Push or Pull Request to `main`, or manual `workflow_dispatch`.
* **Action**: Automatically tests (`testDebugUnitTest`), compiles, and builds `Equilizer-debug.apk`.
* **Artifact**: Uploads the debug APK as a GitHub Actions workflow artifact (`Equilizer-CI-APK`).

### 📦 Production Release
* **Trigger**: Push a version tag matching `v*` (e.g. `git tag v1.0.0 && git push origin v1.0.0`), or manual `workflow_dispatch`.
* **Action**: Runs tests, builds the production release APK, dynamically verifies the artifact, computes SHA-256 checksums, and creates a GitHub Release.
* **Release Assets**:
  * `Equilizer-vX.Y.Z.apk` (Signed release binary)
  * `Equilizer-vX.Y.Z.apk.sha256` (Integrity checksum)

---

## 🛠️ Building & Releasing Locally

### Prerequisites
* JDK 17+
* Android SDK (API Level 36 / Android 16)
* Gradle 8.x

### Build Release APK
```bash
gradle assembleRelease
```
The resulting release APK will be located at:
`app/build/outputs/apk/release/app-release.apk` (or configured release filename).

---

## 📄 License & Notices

Licensed under the Apache License, Version 2.0. Built with Kotlin, Jetpack Compose, AndroidX, and Material 3 components.
