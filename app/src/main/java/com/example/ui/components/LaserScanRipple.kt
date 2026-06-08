package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Custom High-Tech Golden Laser Scan Sweep Modifier.
 * Sweeps a glowing gold horizontal line down across the entire card component
 * to signify high-tech data scanning and tactical terminal interaction.
 */
fun Modifier.laserScanSweep(
    selected: Boolean,
    scanColor: Color = Color(0xFFFFD700) // Gold
): Modifier = this.composed {
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(selected) {
        if (selected) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)
            ) { value, _ ->
                animationTrigger = value
            }
        } else {
            animationTrigger = 0f
        }
    }

    this.drawWithContent {
        drawContent() // Draw original content first

        if (animationTrigger > 0f && animationTrigger < 1f) {
            val h = size.height
            val w = size.width
            val scanY = h * animationTrigger
            val gradientHeight = (h * 0.3f).coerceAtMost(100.dp.toPx())

            // 1. Draw glowing background trailing behind the laser scan line
            if (scanY > gradientHeight) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            scanColor.copy(alpha = 0.02f),
                            scanColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = scanY - gradientHeight,
                        endY = scanY
                    ),
                    topLeft = Offset(0f, scanY - gradientHeight),
                    size = Size(w, gradientHeight)
                )
            }

            // 2. Draw the high-contrast horizontal laser scan line
            drawLine(
                color = scanColor.copy(alpha = 0.9f),
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 2.dp.toPx()
            )

            // 3. Draw a secondary, smaller sub-beam right next to it for neon separation
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(0f, scanY - 2.dp.toPx()),
                end = Offset(w, scanY - 2.dp.toPx()),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

/**
 * Custom Radar/Laser Circular Scan Ripple Modifier.
 * Expands a golden high-tech scanning ring from the center of the clicked item outwards,
 * creating a sophisticated ripple of tactical telemetry data feedback.
 */
fun Modifier.laserScanRipple(
    selected: Boolean,
    scanColor: Color = Color(0xFFFFD700)
): Modifier = this.composed {
    var animationTrigger by remember { mutableStateOf(0f) }

    LaunchedEffect(selected) {
        if (selected) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing)
            ) { value, _ ->
                animationTrigger = value
            }
        } else {
            animationTrigger = 0f
        }
    }

    this.drawWithContent {
        drawContent()

        if (animationTrigger > 0f && animationTrigger < 1f) {
            val h = size.height
            val w = size.width
            val maxRadius = Math.hypot(w.toDouble(), h.toDouble()).toFloat() / 2f
            val currentRadius = maxRadius * animationTrigger
            val center = Offset(w / 2f, h / 2f)
            val alpha = (1f - animationTrigger) * 0.25f

            // Draw expanding circular golden scan wave
            drawCircle(
                color = scanColor.copy(alpha = alpha),
                radius = currentRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.5.dp.toPx()
                )
            )

            // Draw interior soft alpha fill
            drawCircle(
                color = scanColor.copy(alpha = alpha * 0.15f),
                radius = currentRadius,
                center = center
            )
        }
    }
}
