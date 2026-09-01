package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.model.BandSetting
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald

@Composable
fun VerticalBandSlider(
    band: BandSetting,
    isEnabled: Boolean,
    onGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val freqText = formatFrequency(band.centerFrequencyHz)
    val gainText = String.format("%+.1f", band.gainDb)
    val gainColor = when {
        !isEnabled -> Color.Gray
        band.gainDb > 0.05f -> SonicCyan
        band.gainDb < -0.05f -> SonicAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sliderDescription = "$freqText band, ${String.format("%.1f", band.gainDb)} decibels. Range minus 12 dB to plus 12 dB."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        // Gain Readout dB
        Text(
            text = gainText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = gainColor
            ),
            maxLines = 1
        )

        // Vertical Track Box
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(130.dp)
                .semantics { contentDescription = sliderDescription }
                .then(
                    if (isEnabled) {
                        Modifier
                            .pointerInput(band.gainDb) {
                                detectTapGestures { offset ->
                                    val height = size.height.toFloat()
                                    val midY = height / 2f
                                    val clampedY = offset.y.coerceIn(height * 0.08f, height * 0.92f)
                                    val calculated = -((clampedY - midY) / (height * 0.42f)) * 12.0f
                                    val finalGain = calculated.coerceIn(-12.0f, 12.0f)
                                    onGainChanged((finalGain * 2).toInt() / 2.0f)
                                }
                            }
                            .pointerInput(band.gainDb) {
                                detectDragGestures { change, _ ->
                                    val height = size.height.toFloat()
                                    val midY = height / 2f
                                    val clampedY = change.position.y.coerceIn(height * 0.08f, height * 0.92f)
                                    val calculated = -((clampedY - midY) / (height * 0.42f)) * 12.0f
                                    val finalGain = calculated.coerceIn(-12.0f, 12.0f)
                                    onGainChanged((finalGain * 2).toInt() / 2.0f)
                                }
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val trackWidth = 6.dp.toPx()
                val trackX = (width - trackWidth) / 2f

                // Inactive Background Track
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(trackX, height * 0.08f),
                    size = Size(trackWidth, height * 0.84f),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                )

                // Zero Center Detent Tick
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(width * 0.2f, midY),
                    end = Offset(width * 0.8f, midY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Active Level Indicator Bar
                val clampedGain = if (isEnabled) band.gainDb.coerceIn(-12f, 12f) else 0f
                val thumbY = midY - (clampedGain / 12f) * (height * 0.42f)

                if (clampedGain != 0f) {
                    val barTop = if (thumbY < midY) thumbY else midY
                    val barHeight = kotlin.math.abs(thumbY - midY)
                    drawRoundRect(
                        color = if (isEnabled) SonicCyan else Color.Gray,
                        topLeft = Offset(trackX, barTop),
                        size = Size(trackWidth, barHeight),
                        cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                    )
                }

                // Thumb Glow & Circle
                val thumbRadius = 10.dp.toPx()
                val thumbColor = if (isEnabled) SonicCyan else Color.Gray

                drawCircle(
                    color = thumbColor.copy(alpha = 0.35f),
                    radius = thumbRadius + 4.dp.toPx(),
                    center = Offset(width / 2f, thumbY)
                )
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(width / 2f, thumbY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(width / 2f, thumbY)
                )
            }
        }

        // Frequency Label (e.g. 60Hz, 3.6k)
        Text(
            text = freqText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1
        )
    }
}

private fun formatFrequency(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val k = freqHz / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()}k" else "${k}k"
    } else {
        "${freqHz}Hz"
    }
}
