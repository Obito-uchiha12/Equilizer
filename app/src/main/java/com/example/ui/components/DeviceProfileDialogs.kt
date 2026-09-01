package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.settings.model.Preset
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicCyan

@Composable
fun CreateDeviceProfileDialog(
    currentDevice: AudioDevice?,
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        deviceType: DeviceType,
        targetDeviceId: String?,
        targetProductName: String?,
        isGenericFallback: Boolean,
        presetId: String,
        bandGainsDb: List<Float>,
        bassBoostPercent: Int,
        trebleGainDb: Float,
        preampGainDb: Float,
        balance: Float,
        autoApplyEnabled: Boolean,
        isDefaultAudioProfile: Boolean
    ) -> Unit
) {
    val initialType = currentDevice?.type ?: DeviceType.BLUETOOTH_HEADPHONES
    val initialName = currentDevice?.name?.takeIf { it.isNotBlank() && it != currentDevice.type.displayName }
        ?: "My ${initialType.displayName}"

    var profileName by remember { mutableStateOf(initialName) }
    var selectedType by remember { mutableStateOf(initialType) }
    var matchCurrentDeviceOnly by remember { mutableStateOf(currentDevice != null && currentDevice.type != DeviceType.BUILTIN_SPEAKER) }
    var selectedPresetId by remember { mutableStateOf(Preset.FLAT.id) }
    var bassBoostPercent by remember { mutableIntStateOf(0) }
    var trebleGainDb by remember { mutableFloatStateOf(0.0f) }
    var preampGainDb by remember { mutableFloatStateOf(0.0f) }
    var balance by remember { mutableFloatStateOf(0.0f) }
    var autoApplyEnabled by remember { mutableStateOf(true) }
    var isDefaultProfile by remember { mutableStateOf(false) }

    val currentPreset = presets.find { it.id == selectedPresetId } ?: Preset.FLAT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Device Audio Profile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_profile_name_input")
                )

                Text(
                    "Output Device Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val supportedTypes = listOf(
                        DeviceType.BLUETOOTH_HEADPHONES,
                        DeviceType.BLUETOOTH_EARPHONES,
                        DeviceType.WIRED_EARPHONES,
                        DeviceType.WIRED_HEADPHONES,
                        DeviceType.USB_AUDIO
                    )
                    supportedTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when {
                                        type.isBluetooth -> Icons.Filled.Bluetooth
                                        type == DeviceType.USB_AUDIO -> Icons.Filled.Usb
                                        else -> Icons.Filled.Headphones
                                    },
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                if (currentDevice != null && currentDevice.type != DeviceType.BUILTIN_SPEAKER) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = matchCurrentDeviceOnly,
                            onCheckedChange = { matchCurrentDeviceOnly = it }
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text("Bind to Current Device", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Matches '${currentDevice.name}' specifically",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Text(
                    "Acoustic Base Preset",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPresetId == preset.id,
                            onClick = {
                                selectedPresetId = preset.id
                                bassBoostPercent = preset.bassBoostPercent
                                trebleGainDb = preset.trebleGainDb
                                preampGainDb = preset.preampGainDb
                                balance = preset.balance
                            },
                            label = { Text(preset.name, fontSize = 12.sp) }
                        )
                    }
                }

                // BassBoost Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Boost", style = MaterialTheme.typography.labelSmall)
                        Text("$bassBoostPercent%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = bassBoostPercent.toFloat(),
                        onValueChange = { bassBoostPercent = it.toInt() },
                        valueRange = 0f..100f
                    )
                }

                // Treble Tone Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Treble Tone", style = MaterialTheme.typography.labelSmall)
                        Text(String.format("%+.1f dB", trebleGainDb), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = trebleGainDb,
                        onValueChange = { trebleGainDb = (it * 2).toInt() / 2.0f },
                        valueRange = -10.0f..10.0f
                    )
                }

                // Preamp Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Preamp Gain", style = MaterialTheme.typography.labelSmall)
                        Text(String.format("%+.1f dB", preampGainDb), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = preampGainDb,
                        onValueChange = { preampGainDb = (it * 2).toInt() / 2.0f },
                        valueRange = -12.0f..12.0f
                    )
                }

                // Balance Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stereo Balance", style = MaterialTheme.typography.labelSmall)
                        Text(
                            when {
                                balance < -0.05f -> "L ${(-balance * 100).toInt()}%"
                                balance > 0.05f -> "R ${(balance * 100).toInt()}%"
                                else -> "Center"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan)
                        )
                    }
                    Slider(
                        value = balance,
                        onValueChange = { balance = (it * 20).toInt() / 20.0f },
                        valueRange = -1.0f..1.0f
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = autoApplyEnabled,
                        onCheckedChange = { autoApplyEnabled = it }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Auto-Apply on Connect", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Automatically activates this profile when device connects",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefaultProfile,
                        onCheckedChange = { isDefaultProfile = it }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text("Set as Global Default Fallback", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Applies when no device-specific profile exists",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetId = if (matchCurrentDeviceOnly) currentDevice?.id else null
                    val targetProduct = if (matchCurrentDeviceOnly) currentDevice?.name else null
                    onConfirm(
                        profileName,
                        selectedType,
                        targetId,
                        targetProduct,
                        false,
                        selectedPresetId,
                        currentPreset.bandGainsDb,
                        bassBoostPercent,
                        trebleGainDb,
                        preampGainDb,
                        balance,
                        autoApplyEnabled,
                        isDefaultProfile
                    )
                },
                modifier = Modifier.testTag("confirm_create_profile_button")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditDeviceProfileDialog(
    profile: DeviceProfile,
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onConfirm: (DeviceProfile) -> Unit
) {
    var profileName by remember { mutableStateOf(profile.name) }
    var selectedPresetId by remember { mutableStateOf(profile.presetId) }
    var bassBoostPercent by remember { mutableIntStateOf(profile.bassBoostPercent) }
    var trebleGainDb by remember { mutableFloatStateOf(profile.trebleGainDb) }
    var preampGainDb by remember { mutableFloatStateOf(profile.preampGainDb) }
    var balance by remember { mutableFloatStateOf(profile.balance) }
    var autoApplyEnabled by remember { mutableStateOf(profile.autoApplyEnabled) }
    var isDefaultProfile by remember { mutableStateOf(profile.isDefaultAudioProfile) }

    val currentPreset = presets.find { it.id == selectedPresetId } ?: Preset.FLAT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Audio Profile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Device Type: ${profile.deviceType.displayName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Text(
                    "Base Equalizer Preset",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPresetId == preset.id,
                            onClick = {
                                selectedPresetId = preset.id
                                bassBoostPercent = preset.bassBoostPercent
                                trebleGainDb = preset.trebleGainDb
                                preampGainDb = preset.preampGainDb
                                balance = preset.balance
                            },
                            label = { Text(preset.name, fontSize = 12.sp) }
                        )
                    }
                }

                // BassBoost Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Boost", style = MaterialTheme.typography.labelSmall)
                        Text("$bassBoostPercent%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = bassBoostPercent.toFloat(),
                        onValueChange = { bassBoostPercent = it.toInt() },
                        valueRange = 0f..100f
                    )
                }

                // Treble Tone Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Treble Tone", style = MaterialTheme.typography.labelSmall)
                        Text(String.format("%+.1f dB", trebleGainDb), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = trebleGainDb,
                        onValueChange = { trebleGainDb = (it * 2).toInt() / 2.0f },
                        valueRange = -10.0f..10.0f
                    )
                }

                // Preamp Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Preamp Gain", style = MaterialTheme.typography.labelSmall)
                        Text(String.format("%+.1f dB", preampGainDb), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan))
                    }
                    Slider(
                        value = preampGainDb,
                        onValueChange = { preampGainDb = (it * 2).toInt() / 2.0f },
                        valueRange = -12.0f..12.0f
                    )
                }

                // Balance Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stereo Balance", style = MaterialTheme.typography.labelSmall)
                        Text(
                            when {
                                balance < -0.05f -> "L ${(-balance * 100).toInt()}%"
                                balance > 0.05f -> "R ${(balance * 100).toInt()}%"
                                else -> "Center"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SonicCyan)
                        )
                    }
                    Slider(
                        value = balance,
                        onValueChange = { balance = (it * 20).toInt() / 20.0f },
                        valueRange = -1.0f..1.0f
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = autoApplyEnabled,
                        onCheckedChange = { autoApplyEnabled = it }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Auto-Apply on Connect", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Automatically activates this profile when device connects",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefaultProfile,
                        onCheckedChange = { isDefaultProfile = it }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text("Set as Global Default Fallback", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Applies when no device-specific profile exists",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        profile.copy(
                            name = profileName.trim().ifEmpty { profile.name },
                            presetId = selectedPresetId,
                            bandGainsDb = currentPreset.bandGainsDb,
                            bassBoostPercent = bassBoostPercent,
                            trebleGainDb = trebleGainDb,
                            preampGainDb = preampGainDb,
                            balance = balance,
                            autoApplyEnabled = autoApplyEnabled,
                            isDefaultAudioProfile = isDefaultProfile,
                            lastUsedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteDeviceProfileDialog(
    profile: DeviceProfile,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Audio Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Text(
                "Are you sure you want to delete profile '${profile.name}'? Built-in equalizer presets will not be affected.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
