package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.settings.model.BandSetting
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import kotlin.math.abs

@Composable
fun EqualizerCurveCanvas(
    bands: List<BandSetting>,
    isEnabled: Boolean,
    onGainChanged: ((bandIndex: Int, gainDb: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isEnabled) SonicCyan else Color.Gray.copy(alpha = 0.5f)
    val gradientColor = if (isEnabled) ElectricIndigo.copy(alpha = 0.35f) else Color.Gray.copy(alpha = 0.08f)
    var draggedBandIndex by remember { mutableIntStateOf(-1) }

    val curveDescription = "Interactive Equalizer Curve. " +
            bands.joinToString(", ") { "${it.centerFrequencyHz} Hz at ${String.format("%.1f", it.gainDb)} dB" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .semantics { contentDescription = curveDescription }
            .then(
                if (isEnabled && onGainChanged != null) {
                    Modifier
                        .pointerInput(bands) {
                            detectTapGestures { offset ->
                                val count = bands.size
                                if (count == 0) return@detectTapGestures
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                val stepX = width / (count + 1)
                                val midY = height / 2f

                                // Find nearest band
                                var bestIndex = -1
                                var bestDist = Float.MAX_VALUE
                                for (i in 0 until count) {
                                    val bx = stepX * (i + 1)
                                    val dist = abs(offset.x - bx)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        bestIndex = i
                                    }
                                }

                                if (bestIndex in 0 until count && bestDist < stepX * 0.9f) {
                                    val clampedY = offset.y.coerceIn(height * 0.1f, height * 0.9f)
                                    val calculatedGain = -((clampedY - midY) / (height * 0.4f)) * 12.0f
                                    val finalGain = calculatedGain.coerceIn(-12.0f, 12.0f)
                                    onGainChanged(bands[bestIndex].bandIndex, (finalGain * 2).toInt() / 2.0f) // snap to 0.5 dB
                                }
                            }
                        }
                        .pointerInput(bands) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val count = bands.size
                                    if (count == 0) return@detectDragGestures
                                    val width = size.width.toFloat()
                                    val stepX = width / (count + 1)
                                    var bestIndex = -1
                                    var bestDist = Float.MAX_VALUE
                                    for (i in 0 until count) {
                                        val bx = stepX * (i + 1)
                                        val dist = abs(offset.x - bx)
                                        if (dist < bestDist) {
                                            bestDist = dist
                                            bestIndex = i
                                        }
                                    }
                                    if (bestIndex in 0 until count && bestDist < stepX * 0.9f) {
                                        draggedBandIndex = bestIndex
                                    }
                                },
                                onDragEnd = {
                                    draggedBandIndex = -1
                                },
                                onDragCancel = {
                                    draggedBandIndex = -1
                                },
                                onDrag = { change, _ ->
                                    if (draggedBandIndex in 0 until bands.size) {
                                        val height = size.height.toFloat()
                                        val midY = height / 2f
                                        val clampedY = change.position.y.coerceIn(height * 0.1f, height * 0.9f)
                                        val calculatedGain = -((clampedY - midY) / (height * 0.4f)) * 12.0f
                                        val finalGain = calculatedGain.coerceIn(-12.0f, 12.0f)
                                        onGainChanged(bands[draggedBandIndex].bandIndex, (finalGain * 2).toInt() / 2.0f)
                                    }
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            // Top (+12dB) guide
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, height * 0.1f),
                end = Offset(width, height * 0.1f),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
            // Mid (0dB) reference center line
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.5f
            )
            // Bottom (-12dB) guide
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, height * 0.9f),
                end = Offset(width, height * 0.9f),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            if (bands.isEmpty()) return@Canvas

            val count = bands.size
            val stepX = width / (count + 1)

            val points = mutableListOf<Offset>()

            // Edge left anchor
            val firstGain = if (isEnabled) bands.first().gainDb else 0f
            val firstY = midY - (firstGain.coerceIn(-15f, 15f) / 12f) * (height * 0.4f)
            points.add(Offset(0f, firstY))

            for (i in 0 until count) {
                val x = stepX * (i + 1)
                val gain = if (isEnabled) bands[i].gainDb else 0f
                val clampedGain = gain.coerceIn(-15f, 15f)
                val y = midY - (clampedGain / 12f) * (height * 0.4f)
                points.add(Offset(x, y))
            }

            // Edge right anchor
            val lastGain = if (isEnabled) bands.last().gainDb else 0f
            val lastY = midY - (lastGain.coerceIn(-15f, 15f) / 12f) * (height * 0.4f)
            points.add(Offset(width, lastY))

            // Build smooth Bezier Curve
            val curvePath = Path()
            val fillPath = Path()

            curvePath.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                curvePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(width, height)
            fillPath.lineTo(0f, height)
            fillPath.close()

            // Draw Area Fill Gradient under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        gradientColor,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw Curve Line
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.8f),
                        activeColor,
                        ElectricIndigo,
                        activeColor
                    )
                ),
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Draw Point Nodes
            for (i in 1..count) {
                val pt = points[i]
                val bandIdx = i - 1
                val isBeingDragged = (draggedBandIndex == bandIdx)
                val nodeColor = if (isBeingDragged) SonicEmerald else activeColor

                // Outer glow
                drawCircle(
                    color = nodeColor.copy(alpha = if (isBeingDragged) 0.5f else 0.25f),
                    radius = (if (isBeingDragged) 12.dp else 8.dp).toPx(),
                    center = pt
                )
                // Solid center dot
                drawCircle(
                    color = nodeColor,
                    radius = (if (isBeingDragged) 6.dp else 4.dp).toPx(),
                    center = pt
                )
            }
        }
    }
}
