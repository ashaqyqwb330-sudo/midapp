package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Secondary
import kotlinx.coroutines.launch

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enablePersistentLaser: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val scanProgress = remember { Animatable(0f) }
    var isScanning by remember { mutableStateOf(false) }

    // Persistent laser simulation
    val persistentProgress = rememberInfiniteTransition(label = "laser_persistent")
    val animLaserY by persistentProgress.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_y"
    )

    val shape = RoundedCornerShape(16.dp)

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable {
                if (!isScanning) {
                    isScanning = true
                    scope.launch {
                        scanProgress.snapTo(0f)
                        scanProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                        )
                        isScanning = false
                    }
                }
                onClick()
            }
    } else Modifier

    Column(
        modifier = modifier
            .shadow(8.dp, shape)
            .background(Color(0x20FFFFFF), shape)
            .border(1.5.dp, Secondary, shape)
            .then(clickableModifier)
            .drawWithContent {
                drawContent()
                val goldColor = Color(0xFFD4AF37)
                
                // 1. Interactive Laser Scan
                if (isScanning) {
                    val y = size.height * scanProgress.value
                    val trailHeight = 50.dp.toPx()
                    if (y > 0) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, goldColor.copy(alpha = 0.25f), goldColor.copy(alpha = 0.02f)),
                                startY = (y - trailHeight).coerceAtLeast(0f),
                                endY = y
                            ),
                            topLeft = Offset(0f, (y - trailHeight).coerceAtLeast(0f)),
                            size = androidx.compose.ui.geometry.Size(size.width, trailHeight.coerceAtMost(y))
                        )
                    }
                    drawLine(
                        color = goldColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 2. Persistent scan sweep (low opacity hologram overlay style)
                if (enablePersistentLaser) {
                    val y = size.height * animLaserY
                    val trailHeight = 35.dp.toPx()
                    if (y > 0) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, goldColor.copy(alpha = 0.1f), Color.Transparent),
                                startY = (y - trailHeight).coerceAtLeast(0f),
                                endY = y
                            ),
                            topLeft = Offset(0f, (y - trailHeight).coerceAtLeast(0f)),
                            size = androidx.compose.ui.geometry.Size(size.width, trailHeight.coerceAtMost(y))
                        )
                    }
                    drawLine(
                        color = goldColor.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
            .padding(16.dp),
        content = content
    )
}

