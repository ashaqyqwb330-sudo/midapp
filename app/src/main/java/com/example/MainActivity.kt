package com.example

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.NavGraph
import com.example.ui.screens.MilitaryLockScreen
import com.example.ui.theme.MedicalLibraryTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MedicalLibraryTheme {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE) }
        
        // Security gate enabled by default in tactical deployments
        var isBiometricEnabled by remember {
          mutableStateOf(prefs.getBoolean("biometric_lock_enabled", true))
        }
        
        var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

        if (!isUnlocked) {
          MilitaryLockScreen(
            activity = this@MainActivity,
            onUnlockSuccess = { isUnlocked = true }
          )
        } else {
          val navController = rememberNavController()
          NavGraph(navController = navController)
        }
      }
    }
  }
}
