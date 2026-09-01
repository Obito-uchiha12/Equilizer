package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.core.logging.AppLogger
import com.example.core.logging.LogCategory
import com.example.core.result.AudioError
import com.example.core.result.AudioResult
import com.example.settings.model.BandSetting
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.EqualizerSettings
import com.example.settings.model.HeadroomMode
import com.example.settings.model.ListeningProfile
import com.example.settings.model.Preset
import com.example.settings.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.UUID

data class SettingsSnapshot(
    val selectedPresetId: String,
    val selectedListeningProfileId: String?,
    val bands: List<BandSetting>,
    val bassLevel: Float,
    val trebleLevel: Float,
    val preampLevel: Float,
    val balance: Float,
    val headroomMode: HeadroomMode,
    val manualHeadroomDb: Float
)

interface SettingsRepository {
    val settings: StateFlow<EqualizerSettings>
    val availablePresets: StateFlow<List<Preset>>
    val availableProfiles: StateFlow<List<EarphoneProfile>>
    val availableListeningProfiles: StateFlow<List<ListeningProfile>>
    val canUndo: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean): AudioResult<Unit>
    fun selectPreset(presetId: String): AudioResult<Preset>
    fun selectListeningProfile(profileId: String): AudioResult<ListeningProfile>
    fun updateBand(bandIndex: Int, gainDb: Float): AudioResult<Unit>
    fun setBassLevel(level: Float): AudioResult<Unit>
    fun setTrebleLevel(level: Float): AudioResult<Unit>
    fun setPreampLevel(level: Float): AudioResult<Unit>
    fun setBalance(balance: Float): AudioResult<Unit>
    fun setHeadroomMode(mode: HeadroomMode): AudioResult<Unit>
    fun setManualHeadroomDb(levelDb: Float): AudioResult<Unit>
    fun setAutoHeadroomEnabled(enabled: Boolean): AudioResult<Unit>
    fun normalizeEqualizerCurve(): AudioResult<Unit>
    fun undo(): AudioResult<Unit>
    fun selectEarphoneProfile(profileId: String): AudioResult<EarphoneProfile>
    fun setAutoApplyProfile(autoApply: Boolean): AudioResult<Unit>
    fun saveDevicePresetMapping(deviceIdOrType: String, presetId: String): AudioResult<Unit>
    fun getPresetForDevice(deviceIdOrType: String): String?
    fun createCustomPreset(name: String, description: String = ""): AudioResult<Preset>
    fun updateCustomPreset(presetId: String, name: String? = null): AudioResult<Preset>
    fun deleteCustomPreset(presetId: String): AudioResult<Unit>
    fun getDeviceProfiles(): List<com.example.device.model.DeviceProfile>
    fun saveDeviceProfile(profile: com.example.device.model.DeviceProfile)
    fun deleteDeviceProfile(profileId: String)
    fun getDefaultProfileId(): String?
    fun setDefaultProfileId(profileId: String?)
    fun setThemePreference(theme: ThemePreference): AudioResult<Unit>
    fun resetToDefaults(): AudioResult<Unit>
    fun resetToFlat(): AudioResult<Unit>
}

class DefaultSettingsRepository(
    private val context: Context? = null
) : SettingsRepository {

    private val prefs: SharedPreferences? by lazy {
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            AppLogger.w(LogCategory.SETTINGS, TAG, "Could not open SharedPreferences: ${e.message}")
            null
        }
    }

    private val _availablePresets = MutableStateFlow(loadPresets())
    override val availablePresets: StateFlow<List<Preset>> = _availablePresets.asStateFlow()

    private val _availableProfiles = MutableStateFlow(EarphoneProfile.defaultProfiles())
    override val availableProfiles: StateFlow<List<EarphoneProfile>> = _availableProfiles.asStateFlow()

    private val _availableListeningProfiles = MutableStateFlow(ListeningProfile.defaultProfiles())
    override val availableListeningProfiles: StateFlow<List<ListeningProfile>> = _availableListeningProfiles.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<EqualizerSettings> = _settings.asStateFlow()

    private val undoStack = ArrayDeque<SettingsSnapshot>()
    private val _canUndo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val MAX_UNDO_DEPTH = 15

    init {
        AppLogger.i(LogCategory.SETTINGS, TAG, "Initialized DefaultSettingsRepository (Persistence: ${prefs != null})")
    }

    private fun pushUndoSnapshot(current: EqualizerSettings) {
        if (undoStack.size >= MAX_UNDO_DEPTH) {
            undoStack.removeLast()
        }
        undoStack.push(
            SettingsSnapshot(
                selectedPresetId = current.selectedPresetId,
                selectedListeningProfileId = current.selectedListeningProfileId,
                bands = current.bands.map { it.copy() },
                bassLevel = current.bassLevel,
                trebleLevel = current.trebleLevel,
                preampLevel = current.preampLevel,
                balance = current.balance,
                headroomMode = current.headroomMode,
                manualHeadroomDb = current.manualHeadroomDb
            )
        )
        _canUndo.value = undoStack.isNotEmpty()
    }

    override fun undo(): AudioResult<Unit> {
        if (undoStack.isEmpty()) {
            return AudioResult.Failure(AudioError.InvalidParameter("No undo history available"))
        }
        val snapshot = undoStack.pop()
        _canUndo.value = undoStack.isNotEmpty()

        val restored = _settings.value.copy(
            selectedPresetId = snapshot.selectedPresetId,
            selectedListeningProfileId = snapshot.selectedListeningProfileId,
            bands = snapshot.bands,
            bassLevel = snapshot.bassLevel,
            trebleLevel = snapshot.trebleLevel,
            preampLevel = snapshot.preampLevel,
            balance = snapshot.balance,
            headroomMode = snapshot.headroomMode,
            manualHeadroomDb = snapshot.manualHeadroomDb,
            isAutoHeadroomEnabled = snapshot.headroomMode == HeadroomMode.AUTOMATIC
        )
        _settings.value = restored
        persistSettings(restored)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Undid previous tuning change (Restored preset: ${snapshot.selectedPresetId})")
        return AudioResult.Success(Unit)
    }

    private fun Float.sanitize(min: Float, max: Float, default: Float = 0.0f): Float {
        if (this.isNaN() || this.isInfinite()) return default
        return this.coerceIn(min, max)
    }

    private fun loadPresets(): List<Preset> {
        val defaultList = Preset.defaultPresets()
        val customJson = prefs?.getString(KEY_CUSTOM_PRESETS, null) ?: return defaultList
        return try {
            val jsonArray = JSONArray(customJson)
            val customPresets = mutableListOf<Preset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val description = obj.optString("description", "")
                val gainsArray = obj.optJSONArray("gains")
                val gains = mutableListOf<Float>()
                if (gainsArray != null) {
                    for (g in 0 until gainsArray.length()) {
                        val v = gainsArray.optDouble(g, 0.0).toFloat().sanitize(-15.0f, 15.0f, 0.0f)
                        gains.add(v)
                    }
                }
                while (gains.size < 5) gains.add(0.0f)
                val finalGains = gains.take(5)

                val bass = obj.optInt("bass", 0).coerceIn(0, 100)
                val treble = obj.optDouble("treble", 0.0).toFloat().sanitize(-10.0f, 10.0f, 0.0f)
                val preamp = obj.optDouble("preamp", 0.0).toFloat().sanitize(-12.0f, 12.0f, 0.0f)
                val balance = obj.optDouble("balance", 0.0).toFloat().sanitize(-1.0f, 1.0f, 0.0f)
                customPresets.add(
                    Preset(
                        id = id,
                        name = name,
                        description = description,
                        bandGainsDb = finalGains,
                        bassBoostPercent = bass,
                        trebleGainDb = treble,
                        preampGainDb = preamp,
                        balance = balance,
                        isCustom = true
                    )
                )
            }
            defaultList + customPresets
        } catch (e: Exception) {
            AppLogger.w(LogCategory.SETTINGS, TAG, "Error deserializing custom presets: ${e.message}")
            defaultList
        }
    }

    private fun saveCustomPresets() {
        val customOnly = _availablePresets.value.filter { it.isCustom }
        try {
            val jsonArray = JSONArray()
            for (p in customOnly) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    val gainsArray = JSONArray()
                    p.bandGainsDb.forEach { gainsArray.put(it.toDouble()) }
                    put("gains", gainsArray)
                    put("bass", p.bassBoostPercent)
                    put("treble", p.trebleGainDb.toDouble())
                    put("preamp", p.preampGainDb.toDouble())
                    put("balance", p.balance.toDouble())
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_CUSTOM_PRESETS, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SETTINGS, TAG, "Failed to persist custom presets", e)
        }
    }

    private fun loadSettings(): EqualizerSettings {
        val p = prefs ?: return EqualizerSettings()
        val isEnabled = p.getBoolean(KEY_IS_ENABLED, false)
        val presetId = p.getString(KEY_SELECTED_PRESET_ID, Preset.FLAT.id) ?: Preset.FLAT.id
        val bassLevel = p.getFloat(KEY_BASS_LEVEL, 0.0f).sanitize(0.0f, 100.0f, 0.0f)
        val trebleLevel = p.getFloat(KEY_TREBLE_LEVEL, 0.0f).sanitize(-10.0f, 10.0f, 0.0f)
        val preampLevel = p.getFloat(KEY_PREAMP_LEVEL, 0.0f).sanitize(-12.0f, 12.0f, 0.0f)
        val balance = p.getFloat(KEY_BALANCE, 0.0f).sanitize(-1.0f, 1.0f, 0.0f)
        val profileId = p.getString(KEY_EARPHONE_PROFILE_ID, EarphoneProfile.GENERIC_IN_EAR.id) ?: EarphoneProfile.GENERIC_IN_EAR.id
        val listeningProfileId = p.getString(KEY_LISTENING_PROFILE_ID, null)
        val autoApply = p.getBoolean(KEY_AUTO_APPLY, true)
        val autoHeadroom = p.getBoolean(KEY_AUTO_HEADROOM, true)
        val headroomModeStr = p.getString(KEY_HEADROOM_MODE, if (autoHeadroom) HeadroomMode.AUTOMATIC.name else HeadroomMode.OFF.name)
        val headroomMode = try {
            HeadroomMode.valueOf(headroomModeStr ?: HeadroomMode.AUTOMATIC.name)
        } catch (e: Exception) {
            HeadroomMode.AUTOMATIC
        }
        val manualHeadroom = p.getFloat(KEY_MANUAL_HEADROOM, 0.0f).sanitize(0.0f, 12.0f, 0.0f)

        val bands = listOf(
            BandSetting(0, 60, p.getFloat("band_gain_0", 0.0f).sanitize(-15.0f, 15.0f, 0.0f)),
            BandSetting(1, 230, p.getFloat("band_gain_1", 0.0f).sanitize(-15.0f, 15.0f, 0.0f)),
            BandSetting(2, 910, p.getFloat("band_gain_2", 0.0f).sanitize(-15.0f, 15.0f, 0.0f)),
            BandSetting(3, 3600, p.getFloat("band_gain_3", 0.0f).sanitize(-15.0f, 15.0f, 0.0f)),
            BandSetting(4, 14000, p.getFloat("band_gain_4", 0.0f).sanitize(-15.0f, 15.0f, 0.0f))
        )

        val deviceMappings = mutableMapOf<String, String>()
        val mappingsJson = p.getString(KEY_DEVICE_MAPPINGS, null)
        if (!mappingsJson.isNullOrEmpty()) {
            try {
                val obj = JSONObject(mappingsJson)
                obj.keys().forEach { key ->
                    deviceMappings[key] = obj.getString(key)
                }
            } catch (e: Exception) {
                AppLogger.w(LogCategory.SETTINGS, TAG, "Failed to parse device mappings: ${e.message}")
            }
        }

        return EqualizerSettings(
            isEnabled = isEnabled,
            selectedPresetId = presetId,
            bands = bands,
            bassLevel = bassLevel,
            trebleLevel = trebleLevel,
            preampLevel = preampLevel,
            balance = balance,
            headroomMode = headroomMode,
            manualHeadroomDb = manualHeadroom,
            isAutoHeadroomEnabled = headroomMode == HeadroomMode.AUTOMATIC,
            selectedListeningProfileId = listeningProfileId,
            selectedEarphoneProfileId = profileId,
            autoApplyProfile = autoApply,
            devicePresetMap = deviceMappings
        )
    }

    private fun persistSettings(settings: EqualizerSettings) {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_ENABLED, settings.isEnabled)
            putString(KEY_SELECTED_PRESET_ID, settings.selectedPresetId)
            putFloat(KEY_BASS_LEVEL, settings.bassLevel)
            putFloat(KEY_TREBLE_LEVEL, settings.trebleLevel)
            putFloat(KEY_PREAMP_LEVEL, settings.preampLevel)
            putFloat(KEY_BALANCE, settings.balance)
            putString(KEY_HEADROOM_MODE, settings.headroomMode.name)
            putFloat(KEY_MANUAL_HEADROOM, settings.manualHeadroomDb)
            putBoolean(KEY_AUTO_HEADROOM, settings.headroomMode == HeadroomMode.AUTOMATIC)
            putString(KEY_LISTENING_PROFILE_ID, settings.selectedListeningProfileId)
            putString(KEY_EARPHONE_PROFILE_ID, settings.selectedEarphoneProfileId)
            putBoolean(KEY_AUTO_APPLY, settings.autoApplyProfile)
            settings.bands.forEach { band ->
                putFloat("band_gain_${band.bandIndex}", band.gainDb)
            }
            val mappingObj = JSONObject()
            settings.devicePresetMap.forEach { (k, v) -> mappingObj.put(k, v) }
            putString(KEY_DEVICE_MAPPINGS, mappingObj.toString())
            apply()
        }
    }

    override fun setEnabled(enabled: Boolean): AudioResult<Unit> {
        val updated = _settings.value.copy(isEnabled = enabled)
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Setting EQ isEnabled=$enabled")
        return AudioResult.Success(Unit)
    }

    override fun selectPreset(presetId: String): AudioResult<Preset> {
        val preset = _availablePresets.value.find { it.id == presetId }
            ?: return AudioResult.Failure(AudioError.InternalError("Preset not found: $presetId"))

        pushUndoSnapshot(_settings.value)

        val currentBands = _settings.value.bands.mapIndexed { index, band ->
            val newGain = preset.bandGainsDb.getOrElse(index) { 0.0f }
            band.copy(gainDb = newGain)
        }

        val updated = _settings.value.copy(
            selectedPresetId = preset.id,
            selectedListeningProfileId = null,
            bands = currentBands,
            bassLevel = preset.bassBoostPercent / 100.0f,
            trebleLevel = preset.trebleGainDb,
            preampLevel = preset.preampGainDb,
            balance = preset.balance
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Applied preset '${preset.name}'")
        return AudioResult.Success(preset)
    }

    override fun selectListeningProfile(profileId: String): AudioResult<ListeningProfile> {
        val profile = _availableListeningProfiles.value.find { it.id == profileId }
            ?: return AudioResult.Failure(AudioError.InternalError("Listening profile not found: $profileId"))

        pushUndoSnapshot(_settings.value)

        val currentBands = _settings.value.bands.mapIndexed { index, band ->
            val newGain = profile.bandGainsDb.getOrElse(index) { 0.0f }
            band.copy(gainDb = newGain)
        }

        val updated = _settings.value.copy(
            selectedPresetId = "custom",
            selectedListeningProfileId = profile.id,
            bands = currentBands,
            bassLevel = profile.bassBoostPercent / 100.0f,
            trebleLevel = profile.trebleGainDb,
            preampLevel = profile.preampGainDb,
            balance = profile.balance
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Applied listening profile '${profile.name}'")
        return AudioResult.Success(profile)
    }

    override fun updateBand(bandIndex: Int, gainDb: Float): AudioResult<Unit> {
        val clampedGain = gainDb.coerceIn(-15.0f, 15.0f)
        val currentBands = _settings.value.bands.toMutableList()
        val existingIndex = currentBands.indexOfFirst { it.bandIndex == bandIndex }
        if (existingIndex != -1) {
            pushUndoSnapshot(_settings.value)
            currentBands[existingIndex] = currentBands[existingIndex].copy(gainDb = clampedGain)
            val updated = _settings.value.copy(
                bands = currentBands,
                selectedPresetId = determinePresetState(currentBands, _settings.value.bassLevel, _settings.value.trebleLevel, _settings.value.preampLevel, _settings.value.balance),
                selectedListeningProfileId = null
            )
            _settings.value = updated
            persistSettings(updated)
            AppLogger.d(LogCategory.SETTINGS, TAG, "Updated band $bandIndex to ${clampedGain}dB (Preset: ${updated.selectedPresetId})")
            return AudioResult.Success(Unit)
        }
        return AudioResult.Failure(AudioError.InternalError("Invalid bandIndex: $bandIndex"))
    }

    override fun setBassLevel(level: Float): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val clamped = level.coerceIn(0.0f, 1.0f)
        val updated = _settings.value.copy(
            bassLevel = clamped,
            selectedPresetId = determinePresetState(_settings.value.bands, clamped, _settings.value.trebleLevel, _settings.value.preampLevel, _settings.value.balance),
            selectedListeningProfileId = null
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set bass level: $clamped")
        return AudioResult.Success(Unit)
    }

    override fun setTrebleLevel(level: Float): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val clamped = level.coerceIn(-10.0f, 10.0f)
        val updated = _settings.value.copy(
            trebleLevel = clamped,
            selectedPresetId = determinePresetState(_settings.value.bands, _settings.value.bassLevel, clamped, _settings.value.preampLevel, _settings.value.balance),
            selectedListeningProfileId = null
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set treble level: $clamped")
        return AudioResult.Success(Unit)
    }

    override fun setPreampLevel(level: Float): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val clamped = level.coerceIn(-12.0f, 12.0f)
        val updated = _settings.value.copy(
            preampLevel = clamped,
            selectedPresetId = determinePresetState(_settings.value.bands, _settings.value.bassLevel, _settings.value.trebleLevel, clamped, _settings.value.balance),
            selectedListeningProfileId = null
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set preamp level: $clamped")
        return AudioResult.Success(Unit)
    }

    override fun setBalance(balance: Float): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val clamped = balance.coerceIn(-1.0f, 1.0f)
        val updated = _settings.value.copy(
            balance = clamped,
            selectedPresetId = determinePresetState(_settings.value.bands, _settings.value.bassLevel, _settings.value.trebleLevel, _settings.value.preampLevel, clamped),
            selectedListeningProfileId = null
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set balance: $clamped")
        return AudioResult.Success(Unit)
    }

    override fun setHeadroomMode(mode: HeadroomMode): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val updated = _settings.value.copy(
            headroomMode = mode,
            isAutoHeadroomEnabled = mode == HeadroomMode.AUTOMATIC
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Headroom mode set to: ${mode.name}")
        return AudioResult.Success(Unit)
    }

    override fun setManualHeadroomDb(levelDb: Float): AudioResult<Unit> {
        pushUndoSnapshot(_settings.value)
        val clamped = levelDb.coerceIn(-12.0f, 0.0f)
        val updated = _settings.value.copy(manualHeadroomDb = clamped)
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Manual headroom level set to: ${clamped}dB")
        return AudioResult.Success(Unit)
    }

    override fun setAutoHeadroomEnabled(enabled: Boolean): AudioResult<Unit> {
        return setHeadroomMode(if (enabled) HeadroomMode.AUTOMATIC else HeadroomMode.OFF)
    }

    override fun normalizeEqualizerCurve(): AudioResult<Unit> {
        val current = _settings.value
        pushUndoSnapshot(current)
        val normalizedGains = com.example.audio.safety.HeadroomCalculator.normalizeEqCurve(current.bands.map { it.gainDb })
        val updatedBands = current.bands.mapIndexed { index, band ->
            band.copy(gainDb = normalizedGains.getOrElse(index) { 0.0f })
        }
        val updated = current.copy(
            bands = updatedBands,
            selectedPresetId = determinePresetState(updatedBands, current.bassLevel, current.trebleLevel, current.preampLevel, current.balance),
            selectedListeningProfileId = null
        )
        _settings.value = updated
        persistSettings(updated)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Normalized EQ curve to 0 dB maximum peak")
        return AudioResult.Success(Unit)
    }

    override fun selectEarphoneProfile(profileId: String): AudioResult<EarphoneProfile> {
        val profile = _availableProfiles.value.find { it.id == profileId }
            ?: return AudioResult.Failure(AudioError.InternalError("Profile not found: $profileId"))

        val updated = _settings.value.copy(selectedEarphoneProfileId = profile.id)
        _settings.value = updated
        persistSettings(updated)

        if (_settings.value.autoApplyProfile) {
            selectPreset(profile.recommendedPresetId)
        }
        AppLogger.i(LogCategory.SETTINGS, TAG, "Selected earphone profile '${profile.name}'")
        return AudioResult.Success(profile)
    }

    override fun setAutoApplyProfile(autoApply: Boolean): AudioResult<Unit> {
        val updated = _settings.value.copy(autoApplyProfile = autoApply)
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set autoApplyProfile: $autoApply")
        return AudioResult.Success(Unit)
    }

    override fun saveDevicePresetMapping(deviceIdOrType: String, presetId: String): AudioResult<Unit> {
        val currentMappings = _settings.value.devicePresetMap.toMutableMap()
        currentMappings[deviceIdOrType] = presetId
        val updated = _settings.value.copy(devicePresetMap = currentMappings)
        _settings.value = updated
        persistSettings(updated)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Mapped device '$deviceIdOrType' to preset '$presetId'")
        return AudioResult.Success(Unit)
    }

    override fun getPresetForDevice(deviceIdOrType: String): String? {
        return _settings.value.devicePresetMap[deviceIdOrType]
    }

    override fun createCustomPreset(name: String, description: String): AudioResult<Preset> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            return AudioResult.Failure(AudioError.InvalidParameter("Preset name cannot be empty"))
        }

        val current = _settings.value
        val newPreset = Preset(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            description = description.ifEmpty { "User-defined custom equalizer tuning" },
            bandGainsDb = current.bands.map { it.gainDb },
            bassBoostPercent = (current.bassLevel * 100).toInt(),
            trebleGainDb = current.trebleLevel,
            preampGainDb = current.preampLevel,
            balance = current.balance,
            isCustom = true
        )

        val updatedList = _availablePresets.value + newPreset
        _availablePresets.value = updatedList
        saveCustomPresets()

        // Select the newly created preset
        val updatedSettings = current.copy(selectedPresetId = newPreset.id)
        _settings.value = updatedSettings
        persistSettings(updatedSettings)

        AppLogger.i(LogCategory.SETTINGS, TAG, "Created new custom preset '${newPreset.name}' (id: ${newPreset.id})")
        return AudioResult.Success(newPreset)
    }

    override fun updateCustomPreset(presetId: String, name: String?): AudioResult<Preset> {
        val target = _availablePresets.value.find { it.id == presetId }
            ?: return AudioResult.Failure(AudioError.InternalError("Preset not found: $presetId"))

        if (!target.isCustom) {
            return AudioResult.Failure(AudioError.InvalidParameter("Cannot modify built-in preset '${target.name}'"))
        }

        val current = _settings.value
        val updatedPreset = target.copy(
            name = name?.trim()?.takeIf { it.isNotEmpty() } ?: target.name,
            bandGainsDb = current.bands.map { it.gainDb },
            bassBoostPercent = (current.bassLevel * 100).toInt(),
            trebleGainDb = current.trebleLevel,
            preampGainDb = current.preampLevel,
            balance = current.balance
        )

        val updatedList = _availablePresets.value.map {
            if (it.id == presetId) updatedPreset else it
        }
        _availablePresets.value = updatedList
        saveCustomPresets()

        val updatedSettings = current.copy(selectedPresetId = updatedPreset.id)
        _settings.value = updatedSettings
        persistSettings(updatedSettings)

        AppLogger.i(LogCategory.SETTINGS, TAG, "Updated custom preset '${updatedPreset.name}'")
        return AudioResult.Success(updatedPreset)
    }

    override fun deleteCustomPreset(presetId: String): AudioResult<Unit> {
        val target = _availablePresets.value.find { it.id == presetId }
            ?: return AudioResult.Failure(AudioError.InternalError("Preset not found: $presetId"))

        if (!target.isCustom) {
            return AudioResult.Failure(AudioError.InvalidParameter("Cannot delete built-in preset '${target.name}'"))
        }

        val updatedList = _availablePresets.value.filterNot { it.id == presetId }
        _availablePresets.value = updatedList
        saveCustomPresets()

        // If the active preset was deleted, fall back to Flat
        if (_settings.value.selectedPresetId == presetId) {
            selectPreset(Preset.FLAT.id)
        }

        AppLogger.i(LogCategory.SETTINGS, TAG, "Deleted custom preset '${target.name}'")
        return AudioResult.Success(Unit)
    }

    private val inMemoryProfiles = mutableListOf<com.example.device.model.DeviceProfile>()
    private var inMemoryDefaultProfileId: String? = null

    override fun getDeviceProfiles(): List<com.example.device.model.DeviceProfile> {
        val json = prefs?.getString(KEY_DEVICE_PROFILES, null)
        if (json == null) {
            return inMemoryProfiles.toList()
        }
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<com.example.device.model.DeviceProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val typeStr = obj.getString("deviceType")
                val deviceType = try {
                    com.example.device.model.DeviceType.valueOf(typeStr)
                } catch (e: Exception) {
                    com.example.device.model.DeviceType.OTHER
                }
                val targetDeviceId = obj.optString("targetDeviceId", "").takeIf { it.isNotEmpty() }
                val targetProductName = obj.optString("targetProductName", "").takeIf { it.isNotEmpty() }
                val isGenericFallback = obj.optBoolean("isGenericFallback", false)
                val presetId = obj.optString("presetId", Preset.FLAT.id)
                val gainsArr = obj.optJSONArray("bandGainsDb")
                val gains = mutableListOf<Float>()
                if (gainsArr != null) {
                    for (g in 0 until gainsArr.length()) {
                        gains.add(gainsArr.optDouble(g, 0.0).toFloat().sanitize(-15.0f, 15.0f, 0.0f))
                    }
                }
                while (gains.size < 5) gains.add(0.0f)
                val finalGains = gains.take(5)

                val bass = obj.optInt("bassBoostPercent", 0).coerceIn(0, 100)
                val treble = obj.optDouble("trebleGainDb", 0.0).toFloat().sanitize(-10.0f, 10.0f, 0.0f)
                val preamp = obj.optDouble("preampGainDb", 0.0).toFloat().sanitize(-12.0f, 12.0f, 0.0f)
                val balance = obj.optDouble("balance", 0.0).toFloat().sanitize(-1.0f, 1.0f, 0.0f)
                val autoApply = obj.optBoolean("autoApplyEnabled", true)
                val isDefault = obj.optBoolean("isDefaultAudioProfile", false)
                val lastUsed = obj.optLong("lastUsedTimestamp", System.currentTimeMillis())

                list.add(
                    com.example.device.model.DeviceProfile(
                        id = id,
                        name = name,
                        deviceType = deviceType,
                        targetDeviceId = targetDeviceId,
                        targetProductName = targetProductName,
                        isGenericFallback = isGenericFallback,
                        presetId = presetId,
                        bandGainsDb = finalGains,
                        bassBoostPercent = bass,
                        trebleGainDb = treble,
                        preampGainDb = preamp,
                        balance = balance,
                        autoApplyEnabled = autoApply,
                        isDefaultAudioProfile = isDefault,
                        lastUsedTimestamp = lastUsed
                    )
                )
            }
            inMemoryProfiles.clear()
            inMemoryProfiles.addAll(list)
            list
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SETTINGS, TAG, "Error loading device profiles: ${e.message}", e)
            inMemoryProfiles.toList()
        }
    }

    override fun saveDeviceProfile(profile: com.example.device.model.DeviceProfile) {
        val current = inMemoryProfiles.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        inMemoryProfiles.clear()
        inMemoryProfiles.addAll(current)
        persistDeviceProfiles(current)
    }

    override fun deleteDeviceProfile(profileId: String) {
        inMemoryProfiles.removeAll { it.id == profileId }
        persistDeviceProfiles(inMemoryProfiles)
    }

    private fun persistDeviceProfiles(profiles: List<com.example.device.model.DeviceProfile>) {
        try {
            val arr = JSONArray()
            for (p in profiles) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("deviceType", p.deviceType.name)
                    if (p.targetDeviceId != null) put("targetDeviceId", p.targetDeviceId)
                    if (p.targetProductName != null) put("targetProductName", p.targetProductName)
                    put("isGenericFallback", p.isGenericFallback)
                    put("presetId", p.presetId)
                    val gainsArr = JSONArray()
                    p.bandGainsDb.forEach { gainsArr.put(it.toDouble()) }
                    put("bandGainsDb", gainsArr)
                    put("bassBoostPercent", p.bassBoostPercent)
                    put("trebleGainDb", p.trebleGainDb.toDouble())
                    put("preampGainDb", p.preampGainDb.toDouble())
                    put("balance", p.balance.toDouble())
                    put("autoApplyEnabled", p.autoApplyEnabled)
                    put("isDefaultAudioProfile", p.isDefaultAudioProfile)
                    put("lastUsedTimestamp", p.lastUsedTimestamp)
                }
                arr.put(obj)
            }
            prefs?.edit()?.putString(KEY_DEVICE_PROFILES, arr.toString())?.apply()
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SETTINGS, TAG, "Failed to persist device profiles", e)
        }
    }

    override fun getDefaultProfileId(): String? {
        return prefs?.getString(KEY_DEFAULT_PROFILE_ID, null) ?: inMemoryDefaultProfileId
    }

    override fun setDefaultProfileId(profileId: String?) {
        inMemoryDefaultProfileId = profileId
        if (profileId == null) {
            prefs?.edit()?.remove(KEY_DEFAULT_PROFILE_ID)?.apply()
        } else {
            prefs?.edit()?.putString(KEY_DEFAULT_PROFILE_ID, profileId)?.apply()
        }
    }

    override fun setThemePreference(theme: ThemePreference): AudioResult<Unit> {
        val updated = _settings.value.copy(themePreference = theme)
        _settings.value = updated
        persistSettings(updated)
        AppLogger.d(LogCategory.SETTINGS, TAG, "Set theme preference: $theme")
        return AudioResult.Success(Unit)
    }

    override fun resetToDefaults(): AudioResult<Unit> {
        val defaultSettings = EqualizerSettings()
        _settings.value = defaultSettings
        persistSettings(defaultSettings)
        AppLogger.i(LogCategory.SETTINGS, TAG, "Reset settings to factory defaults")
        return AudioResult.Success(Unit)
    }

    override fun resetToFlat(): AudioResult<Unit> {
        val result = selectPreset(Preset.FLAT.id)
        return if (result is AudioResult.Success) AudioResult.Success(Unit) else AudioResult.Failure((result as AudioResult.Failure).error)
    }

    private fun determinePresetState(
        bands: List<BandSetting>,
        bassLevel: Float,
        trebleLevel: Float,
        preampLevel: Float,
        balance: Float
    ): String {
        // Compare with all available presets
        for (preset in _availablePresets.value) {
            var matches = true
            for (i in 0 until 5) {
                val currentGain = bands.getOrNull(i)?.gainDb ?: 0.0f
                val presetGain = preset.bandGainsDb.getOrElse(i) { 0.0f }
                if (kotlin.math.abs(currentGain - presetGain) > 0.05f) {
                    matches = false
                    break
                }
            }
            if (matches) {
                val presetBass = preset.bassBoostPercent / 100.0f
                if (kotlin.math.abs(bassLevel - presetBass) <= 0.05f &&
                    kotlin.math.abs(trebleLevel - preset.trebleGainDb) <= 0.05f &&
                    kotlin.math.abs(preampLevel - preset.preampGainDb) <= 0.05f &&
                    kotlin.math.abs(balance - preset.balance) <= 0.05f
                ) {
                    return preset.id
                }
            }
        }
        return "custom"
    }

    companion object {
        private const val TAG = "SettingsRepo"
        private const val PREFS_NAME = "equalizer_app_preferences"
        private const val KEY_IS_ENABLED = "key_is_enabled"
        private const val KEY_SELECTED_PRESET_ID = "key_selected_preset_id"
        private const val KEY_BASS_LEVEL = "key_bass_level"
        private const val KEY_TREBLE_LEVEL = "key_treble_level"
        private const val KEY_PREAMP_LEVEL = "key_preamp_level"
        private const val KEY_BALANCE = "key_balance"
        private const val KEY_AUTO_HEADROOM = "key_auto_headroom"
        private const val KEY_HEADROOM_MODE = "key_headroom_mode"
        private const val KEY_MANUAL_HEADROOM = "key_manual_headroom"
        private const val KEY_LISTENING_PROFILE_ID = "key_listening_profile_id"
        private const val KEY_EARPHONE_PROFILE_ID = "key_profile_id"
        private const val KEY_AUTO_APPLY = "key_auto_apply"
        private const val KEY_DEVICE_MAPPINGS = "key_device_mappings"
        private const val KEY_CUSTOM_PRESETS = "key_custom_presets"
        private const val KEY_DEVICE_PROFILES = "key_device_profiles"
        private const val KEY_DEFAULT_PROFILE_ID = "key_default_profile_id"
    }
}

