package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Reusable high-fidelity staggered entrance modifier (Framer Motion equivalent).
 * Delay is computed dynamically based on the item index.
 * Animates both translation and alpha on enter.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 80
): Modifier = this.composed {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * baseDelayMs.toLong())
        isVisible = true
    }

    // High-tech responsive spring curves
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "stagger_alpha"
    )

    val translateY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "stagger_translate"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translateY.toPx()
    }
}
