package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color(0xFF2ECC71),
    speedMs: Long = 40,
    cursorColor: Color = Color(0xFF2ECC71)
) {
    var visibleText by remember(text) { mutableStateOf("") }
    
    // Animate character extraction
    LaunchedEffect(text) {
        visibleText = ""
        for (char in text) {
            delay(speedMs)
            visibleText += char
        }
    }

    // Blinking cursor state
    val infiniteTransition = rememberInfiniteTransition(label = "terminal_cursor")
    val isCursorVisible by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_visible"
    )

    Row(modifier = modifier) {
        Text(
            text = visibleText + if (isCursorVisible > 0.5f) "_" else " ",
            style = style,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
