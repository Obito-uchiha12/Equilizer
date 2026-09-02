package com.example

import com.example.settings.DefaultSettingsRepository
import com.example.settings.model.EarphoneProfile
import com.example.settings.model.Preset
import com.example.settings.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EqualizerSettingsTest {

    private lateinit var repository: DefaultSettingsRepository

    @Before
    fun setup() {
        repository = DefaultSettingsRepository()
    }

    @Test
    fun `initial settings have default values`() {
        val settings = repository.settings.value
        assertFalse(settings.isEnabled)
        assertEquals(Preset.FLAT.id, settings.selectedPresetId)
        assertEquals(5, settings.bands.size)
        assertEquals(0.0f, settings.bassLevel, 0.001f)
        assertEquals(0.0f, settings.trebleLevel, 0.001f)
        assertEquals(0.0f, settings.preampLevel, 0.001f)
        assertEquals(0.0f, settings.balance, 0.001f)
        assertTrue(settings.autoApplyProfile)
        assertEquals(ThemePreference.SYSTEM, settings.themePreference)
    }

    @Test
    fun `selecting preset updates bands and gains`() {
        val result = repository.selectPreset(Preset.BASS_BOOST.id)
        assertTrue(result.isSuccess)

        val settings = repository.settings.value
        assertEquals(Preset.BASS_BOOST.id, settings.selectedPresetId)
        assertEquals(6.0f, settings.bands[0].gainDb, 0.001f)
        assertEquals(4.0f, settings.bands[1].gainDb, 0.001f)
        assertEquals(0.5f, settings.bassLevel, 0.001f)
    }

    @Test
    fun `updating single band sets custom preset`() {
        repository.updateBand(bandIndex = 2, gainDb = 5.5f)
        val settings = repository.settings.value
        assertEquals(5.5f, settings.bands[2].gainDb, 0.001f)
        assertEquals("custom", settings.selectedPresetId)
    }

    @Test
    fun `sound parameters are clamped safely`() {
        repository.setBassLevel(2.5f) // Should clamp to 1.0f
        assertEquals(1.0f, repository.settings.value.bassLevel, 0.001f)

        repository.setBassLevel(-1.0f) // Should clamp to 0.0f
        assertEquals(0.0f, repository.settings.value.bassLevel, 0.001f)

        repository.setTrebleLevel(20.0f) // Should clamp to +10.0f
        assertEquals(10.0f, repository.settings.value.trebleLevel, 0.001f)

        repository.setPreampLevel(-25.0f) // Should clamp to -12.0f
        assertEquals(-12.0f, repository.settings.value.preampLevel, 0.001f)

        repository.setBalance(3.0f) // Should clamp to 1.0f
        assertEquals(1.0f, repository.settings.value.balance, 0.001f)
    }

    @Test
    fun `earphone profile auto-applies recommended preset`() {
        val result = repository.selectEarphoneProfile(EarphoneProfile.GENERIC_EARBUDS.id)
        assertTrue(result.isSuccess)

        val settings = repository.settings.value
        assertEquals(EarphoneProfile.GENERIC_EARBUDS.id, settings.selectedEarphoneProfileId)
        assertEquals(Preset.BASS_BOOST.id, settings.selectedPresetId)
    }

    @Test
    fun `reset to defaults restores clean state`() {
        repository.setEnabled(true)
        repository.setBassLevel(0.8f)
        repository.updateBand(0, 10.0f)

        repository.resetToDefaults()
        val settings = repository.settings.value
        assertFalse(settings.isEnabled)
        assertEquals(0.0f, settings.bassLevel, 0.001f)
        assertEquals(0.0f, settings.bands[0].gainDb, 0.001f)
    }
}
