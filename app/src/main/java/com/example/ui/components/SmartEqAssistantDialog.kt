package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.safety.ClippingRisk
import com.example.domain.smarteq.SmartEqContext
import com.example.domain.smarteq.SmartEqIntensity
import com.example.domain.smarteq.SmartEqResult
import com.example.settings.model.ListeningGoal
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.SonicRuby

@Composable
fun SmartEqAssistantDialog(
    selectedGoal: ListeningGoal,
    selectedContext: SmartEqContext,
    selectedIntensity: SmartEqIntensity,
    smartEqResult: SmartEqResult?,
    onGoalChanged: (ListeningGoal) -> Unit,
    onContextChanged: (SmartEqContext) -> Unit,
    onIntensityChanged: (SmartEqIntensity) -> Unit,
    onApply: () -> Unit,
    onAudition: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_eq_assistant_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricIndigo.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart EQ",
                        tint = ElectricIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Smart EQ Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Acoustic preference & context tuning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Sound Goal
                Text(
                    text = "1. Listening Goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListeningGoal.values().forEach { goal ->
                        val isSelected = (goal == selectedGoal)
                        val icon = getGoalIcon(goal)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) SonicCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onGoalChanged(goal) }
                                .testTag("smart_goal_${goal.name.lowercase()}"),
                            color = if (isSelected) SonicCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = goal.displayName,
                                    tint = if (isSelected) SonicCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SonicCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = goal.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = SonicCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Listening Context
                Text(
                    text = "2. Audio Context",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmartEqContext.values().forEach { ctx ->
                        val isSelected = (ctx == selectedContext)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onContextChanged(ctx) },
                            label = { Text(ctx.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getContextIcon(ctx),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigo.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricIndigo
                            ),
                            modifier = Modifier.testTag("smart_context_${ctx.name.lowercase()}")
                        )
                    }
                }

                // Section 3: Intensity
                Text(
                    text = "3. Tuning Intensity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmartEqIntensity.values().forEach { intensity ->
                        val isSelected = (intensity == selectedIntensity)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) SonicEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onIntensityChanged(intensity) }
                                .testTag("smart_intensity_${intensity.name.lowercase()}"),
                            color = if (isSelected) SonicEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = intensity.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SonicEmerald else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Section 4: Result Preview Card
                if (smartEqResult != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = SonicCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Generated Curve Contour",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 5-band gain readouts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val labels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
                                smartEqResult.bandGainsDb.forEachIndexed { i, gain ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = labels.getOrElse(i) { "$i" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format("%+.1f", gain),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (gain > 0) SonicCyan else if (gain < 0) SonicAmber else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // DSP Boost & Headroom Safety
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val risk = smartEqResult.validationResult.clippingRisk
                                    val riskColor = when (risk) {
                                        ClippingRisk.SAFE -> SonicEmerald
                                        ClippingRisk.WARNING -> SonicAmber
                                        ClippingRisk.HIGH_RISK -> SonicRuby
                                    }
                                    Icon(
                                        imageVector = if (risk == ClippingRisk.SAFE) Icons.Default.Shield else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = riskColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Risk: ${risk.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = riskColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (smartEqResult.headroomAnalysis.autoHeadroomOffsetDb < 0) {
                                    Text(
                                        text = "Auto Headroom: ${String.format("%.1f", smartEqResult.headroomAnalysis.autoHeadroomOffsetDb)} dB",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = SonicCyan
                                    )
                                }
                            }

                            // Hardware adjustments if any
                            if (smartEqResult.capabilityAdjustments.isNotEmpty()) {
                                smartEqResult.capabilityAdjustments.forEach { adj ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = SonicAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = adj,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = SonicAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Disclaimer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Calibrated mathematically based on user sound goals and digital signal processing. No fake acoustic sensor measurement is claimed.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("smart_eq_apply_button")
            ) {
                Text("Apply Tuning")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAudition,
                    modifier = Modifier.testTag("smart_eq_audition_button")
                ) {
                    Text("Audition (A/B)")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("smart_eq_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun getGoalIcon(goal: ListeningGoal): ImageVector = when (goal) {
    ListeningGoal.BALANCED -> Icons.Default.GraphicEq
    ListeningGoal.BASS_FOCUS -> Icons.Default.VolumeUp
    ListeningGoal.VOCAL_FOCUS -> Icons.Default.Mic
    ListeningGoal.DETAIL -> Icons.Default.Hearing
    ListeningGoal.WARM -> Icons.Default.MusicNote
    ListeningGoal.BRIGHT -> Icons.Default.Headphones
    ListeningGoal.RELAXED -> Icons.Default.AutoAwesome
}

private fun getContextIcon(ctx: SmartEqContext): ImageVector = when (ctx) {
    SmartEqContext.ALL_AROUND -> Icons.Default.MusicNote
    SmartEqContext.PODCAST_SPEECH -> Icons.Default.Mic
    SmartEqContext.GAMING -> Icons.Default.SportsEsports
    SmartEqContext.CINEMA_MOVIES -> Icons.Default.Movie
}
