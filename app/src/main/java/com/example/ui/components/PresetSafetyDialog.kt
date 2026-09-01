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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.safety.ClippingRisk
import com.example.audio.safety.PresetValidationResult
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicRuby

@Composable
fun PresetSafetyDialog(
    presetName: String,
    validationResult: PresetValidationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_safety_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val iconColor = when (validationResult.clippingRisk) {
                    ClippingRisk.SAFE -> SonicEmerald
                    ClippingRisk.WARNING -> SonicAmber
                    ClippingRisk.HIGH_RISK -> SonicRuby
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (validationResult.clippingRisk == ClippingRisk.SAFE) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = "Safety",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Safety & Clipping Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = presetName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Risk Badge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = when (validationResult.clippingRisk) {
                        ClippingRisk.SAFE -> SonicEmerald.copy(alpha = 0.12f)
                        ClippingRisk.WARNING -> SonicAmber.copy(alpha = 0.12f)
                        ClippingRisk.HIGH_RISK -> SonicRuby.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (validationResult.clippingRisk) {
                                ClippingRisk.SAFE -> Icons.Default.CheckCircle
                                ClippingRisk.WARNING -> Icons.Default.Warning
                                ClippingRisk.HIGH_RISK -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = when (validationResult.clippingRisk) {
                                ClippingRisk.SAFE -> SonicEmerald
                                ClippingRisk.WARNING -> SonicAmber
                                ClippingRisk.HIGH_RISK -> SonicRuby
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Risk Level: ${validationResult.clippingRisk.name}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (validationResult.clippingRisk) {
                                    ClippingRisk.SAFE -> SonicEmerald
                                    ClippingRisk.WARNING -> SonicAmber
                                    ClippingRisk.HIGH_RISK -> SonicRuby
                                }
                            )
                            Text(
                                text = when (validationResult.clippingRisk) {
                                    ClippingRisk.SAFE -> "Signal output is within clean headroom bounds."
                                    ClippingRisk.WARNING -> "Moderate positive gain. Auto Headroom recommended."
                                    ClippingRisk.HIGH_RISK -> "Heavy positive boost. Auto Headroom attenuation active."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Metrics Table
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricRow(
                            label = "Total Positive Accumulated Gain",
                            value = "${String.format("%.1f", validationResult.totalPositiveGainDb)} dB"
                        )
                        MetricRow(
                            label = "Recommended Headroom",
                            value = "${String.format("%.1f", validationResult.recommendedHeadroomDb)} dB"
                        )
                        MetricRow(
                            label = "Auto Headroom Required",
                            value = if (validationResult.requiresAutoHeadroom) "Yes" else "No"
                        )
                        MetricRow(
                            label = "Preset Validity Bounds",
                            value = if (validationResult.isValid) "Pass (All within bounds)" else "Fail (Exceeds hardware limit)"
                        )
                    }
                }

                if (validationResult.issues.isNotEmpty()) {
                    Text(
                        text = "Boundary Warnings:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SonicRuby,
                        fontWeight = FontWeight.Bold
                    )
                    validationResult.issues.forEach { issue ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SonicRuby,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = SonicRuby
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("preset_safety_close_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
