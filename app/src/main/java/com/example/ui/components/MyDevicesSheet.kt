package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.device.model.AudioDevice
import com.example.device.model.DeviceProfile
import com.example.device.model.DeviceType
import com.example.device.model.ProfileMatchType
import com.example.settings.model.Preset
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDevicesSheet(
    sheetState: SheetState,
    currentDevice: AudioDevice?,
    profiles: List<DeviceProfile>,
    activeProfile: DeviceProfile?,
    matchType: ProfileMatchType,
    defaultProfileId: String?,
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onCreateProfileClick: () -> Unit,
    onEditProfileClick: (DeviceProfile) -> Unit,
    onDeleteProfileClick: (DeviceProfile) -> Unit,
    onToggleAutoApply: (String, Boolean) -> Unit,
    onSetDefaultProfile: (String?) -> Unit,
    onApplyProfile: (DeviceProfile) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "My Audio Devices & Profiles",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Automatic acoustic tuning for headphones & earphones",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_my_devices_sheet")
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Create Profile
            Button(
                onClick = onCreateProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_new_device_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Device Profile", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Section 1: Connected Active Device
                item {
                    Text(
                        "CURRENT ACTIVE OUTPUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ActiveDeviceCard(
                        currentDevice = currentDevice,
                        activeProfile = activeProfile,
                        matchType = matchType,
                        onEditProfile = onEditProfileClick,
                        onCreateProfile = onCreateProfileClick
                    )
                }

                // Section 2: Configured Custom Profiles
                val customProfiles = profiles.filter { !it.isGenericFallback }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "CONFIGURED PROFILES (${customProfiles.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (customProfiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "No custom device profiles created yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    "Generic fallbacks will be used until you create a dedicated profile.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                } else {
                    items(customProfiles, key = { it.id }) { profile ->
                        DeviceProfileCard(
                            profile = profile,
                            isActive = activeProfile?.id == profile.id,
                            isDefault = defaultProfileId == profile.id || profile.isDefaultAudioProfile,
                            presets = presets,
                            onApply = { onApplyProfile(profile) },
                            onEdit = { onEditProfileClick(profile) },
                            onDelete = { onDeleteProfileClick(profile) },
                            onToggleAutoApply = { onToggleAutoApply(profile.id, it) },
                            onToggleDefault = {
                                if (defaultProfileId == profile.id) {
                                    onSetDefaultProfile(null)
                                } else {
                                    onSetDefaultProfile(profile.id)
                                }
                            }
                        )
                    }
                }

                // Section 3: Generic Fallbacks
                val fallbackProfiles = profiles.filter { it.isGenericFallback }
                if (fallbackProfiles.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GENERIC FALLBACK PROFILES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    items(fallbackProfiles, key = { it.id }) { profile ->
                        DeviceProfileCard(
                            profile = profile,
                            isActive = activeProfile?.id == profile.id,
                            isDefault = defaultProfileId == profile.id || profile.isDefaultAudioProfile,
                            presets = presets,
                            onApply = { onApplyProfile(profile) },
                            onEdit = { onEditProfileClick(profile) },
                            onDelete = null, // Fallbacks cannot be deleted
                            onToggleAutoApply = { onToggleAutoApply(profile.id, it) },
                            onToggleDefault = {
                                if (defaultProfileId == profile.id) {
                                    onSetDefaultProfile(null)
                                } else {
                                    onSetDefaultProfile(profile.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDeviceCard(
    currentDevice: AudioDevice?,
    activeProfile: DeviceProfile?,
    matchType: ProfileMatchType,
    onEditProfile: (DeviceProfile) -> Unit,
    onCreateProfile: () -> Unit
) {
    val dev = currentDevice ?: AudioDevice.defaultBuiltinSpeaker()
    val isSpeaker = dev.type == DeviceType.BUILTIN_SPEAKER

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpeaker) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else ElectricIndigo.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dev.type.isBluetooth -> SonicCyan.copy(alpha = 0.2f)
                                    dev.type.isHeadphoneOrEarphone -> SonicEmerald.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                dev.type.isBluetooth -> Icons.Filled.Bluetooth
                                dev.type == DeviceType.USB_AUDIO -> Icons.Filled.Usb
                                dev.type.isHeadphoneOrEarphone -> Icons.Filled.Headphones
                                else -> Icons.Filled.Speaker
                            },
                            contentDescription = null,
                            tint = when {
                                dev.type.isBluetooth -> SonicCyan
                                dev.type.isHeadphoneOrEarphone -> SonicEmerald
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = dev.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dev.type.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // Match Status Badge
                val badgeColor = when (matchType) {
                    ProfileMatchType.EXACT_MATCH -> SonicEmerald
                    ProfileMatchType.STRONG_MATCH -> SonicCyan
                    ProfileMatchType.FALLBACK_MATCH -> SonicAmber
                    ProfileMatchType.DEFAULT_PROFILE_MATCH -> ElectricIndigo
                    ProfileMatchType.NO_MATCH -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSpeaker) "PHONE SPEAKER" else matchType.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (isSpeaker) {
                Text(
                    "Built-in phone speaker uses flat reference EQ without automatic earphone profile adjustments.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else if (activeProfile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Profile: ${activeProfile.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Bass ${activeProfile.bassBoostPercent}%  •  Treble ${String.format("%+.1fdB", activeProfile.trebleGainDb)}  •  Auto-Apply: ${if (activeProfile.autoApplyEnabled) "ON" else "OFF"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    OutlinedButton(
                        onClick = { onEditProfile(activeProfile) },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "No profile assigned to this device.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Button(
                        onClick = onCreateProfile,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                    ) {
                        Text("Create Profile", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceProfileCard(
    profile: DeviceProfile,
    isActive: Boolean,
    isDefault: Boolean,
    presets: List<Preset>,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onToggleAutoApply: (Boolean) -> Unit,
    onToggleDefault: () -> Unit
) {
    val preset = presets.find { it.id == profile.presetId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.border(1.5.dp, SonicCyan, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SonicCyan.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title and Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            profile.deviceType.isBluetooth -> Icons.Filled.Bluetooth
                            profile.deviceType == DeviceType.USB_AUDIO -> Icons.Filled.Usb
                            else -> Icons.Filled.Headphones
                        },
                        contentDescription = null,
                        tint = if (isActive) SonicCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SonicAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("DEFAULT", style = MaterialTheme.typography.labelSmall.copy(color = SonicAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                        Text(
                            text = if (profile.isGenericFallback) "Generic ${profile.deviceType.displayName} Fallback"
                            else "${profile.deviceType.displayName} • Preset: ${preset?.name ?: "Custom"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // Default Toggle Button (Star)
                IconButton(
                    onClick = onToggleDefault,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Set Default",
                        tint = if (isDefault) SonicAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Parameter Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bass: ${profile.bassBoostPercent}%", style = MaterialTheme.typography.labelSmall)
                Text("Treble: ${String.format("%+.1fdB", profile.trebleGainDb)}", style = MaterialTheme.typography.labelSmall)
                Text("Preamp: ${String.format("%+.1fdB", profile.preampGainDb)}", style = MaterialTheme.typography.labelSmall)
                Text("Balance: ${if (kotlin.math.abs(profile.balance) < 0.05f) "0" else String.format("%+.1f", profile.balance)}", style = MaterialTheme.typography.labelSmall)
            }

            // Controls & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = profile.autoApplyEnabled,
                        onCheckedChange = onToggleAutoApply,
                        modifier = Modifier.testTag("switch_profile_auto_apply_${profile.id}")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Auto-Apply",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onApply,
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
