package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MedicalLibraryTheme

class ReportsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalLibraryTheme {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ReportsScreen(onBack = { finish() })
                    }
                }
            }
        }
    }
}
