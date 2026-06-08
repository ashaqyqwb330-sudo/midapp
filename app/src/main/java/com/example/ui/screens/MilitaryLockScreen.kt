package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.helper.BiometricHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MilitaryLockScreen(
    activity: FragmentActivity,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE) }
    
    // Check custom passcode or default
    val targetPasscode = remember { prefs.getString("custom_security_pin", "1447") ?: "1447" }
    
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isPinMode by remember { mutableStateOf(false) }
    var authAttempts by remember { mutableStateOf(0) }
    
    // Status message for screen feedback
    var statusText by remember { mutableStateOf("يرجى فك التشفير للمتابعة") }
    
    // Biometric launch effect
    LaunchedEffect(Unit) {
        // Register callbacks
        BiometricHelper.onAuthSuccess = {
            scope.launch {
                statusText = "تم تخويل الوصول بنجاح 🟢"
                Toast.makeText(context, "تم تخويل الوصول الطبي بنجاح 🛡️", Toast.LENGTH_SHORT).show()
                delay(400)
                onUnlockSuccess()
            }
        }
        
        BiometricHelper.onAuthError = { errorMsg ->
            statusText = "تنبيه: $errorMsg"
            errorMessage = errorMsg
            // If biometric isn't enrolled or supported, automatically toggle PIN mode for the user
            if (!BiometricHelper.isBiometricAvailable(context)) {
                isPinMode = true
            }
        }
        
        // Trigger prompt automatically on launch
        if (BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.triggerBiometricPrompt(activity)
        } else {
            isPinMode = true
            statusText = "الرجوع لخيار الرمز اللامركزي لعدم توفر البصمة"
        }
    }
    
    // Beautiful backdrop with gradient mapping
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary,
                        Color(0xFF030710),
                        Color(0xFF0C172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative radar grid in background using Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2f
            val maxRadius = size.width.coerceAtLeast(size.height)
            
            // Draw tactical grid lines
            for (i in 1..4) {
                drawCircle(
                    color = Secondary.copy(alpha = 0.03f * i),
                    radius = (maxRadius / 8f) * i,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            // Vertical & Horizontal Crosshairs
            drawLine(
                color = Secondary.copy(alpha = 0.05f),
                start = androidx.compose.ui.geometry.Offset(center.width, 0f),
                end = androidx.compose.ui.geometry.Offset(center.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Secondary.copy(alpha = 0.05f),
                start = androidx.compose.ui.geometry.Offset(0f, center.height),
                end = androidx.compose.ui.geometry.Offset(size.width, center.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Tactical glass Card overlay
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(1.dp, Secondary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header badge - secure locker icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Secondary.copy(alpha = 0.1f), CircleShape)
                        .border(1.5.dp, Secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل عسكري",
                        tint = Secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "جدار الحماية الفسيولوجي الموحد 🛡️",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "بروتوكول أمن المناهج الطبية والتقارير الميدانية",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Divider(color = Secondary.copy(alpha = 0.15f))

                // Authentication Status Monitor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (statusText.contains("بنجاح")) Color(0xFF2ECC71) else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                AnimatedContent(
                    targetState = isPinMode,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "LockScreenStateTransition"
                ) { showPin ->
                    if (showPin) {
                        // NumPad Passcode Mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Display dots representing PIN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = enteredPin.length > i
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp)
                                            .background(
                                                if (isFilled) Secondary else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(1.5.dp, Secondary, CircleShape)
                                    )
                                }
                            }

                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFE74C3C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "الرمز العسكري الافتراضي: $targetPasscode",
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Beautiful Tactile 3x4 NumPad Layout
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val buttonClass = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                val listButtons = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("مسح", "0", "◀")
                                )

                                for (row in listButtons) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (key in row) {
                                            Box(
                                                modifier = buttonClass
                                                    .background(
                                                        if (key == "مسح" || key == "◀") Color.White.copy(alpha = 0.05f)
                                                        else Secondary.copy(alpha = 0.08f)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (key == "مسح" || key == "◀") Color.White.copy(alpha = 0.1f)
                                                        else Secondary.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        if (key == "مسح") {
                                                            enteredPin = ""
                                                            errorMessage = ""
                                                        } else if (key == "◀") {
                                                            if (enteredPin.isNotEmpty()) {
                                                                enteredPin = enteredPin.dropLast(1)
                                                                errorMessage = ""
                                                            }
                                                        } else {
                                                            if (enteredPin.length < 4) {
                                                                enteredPin += key
                                                                errorMessage = ""
                                                                
                                                                // Fast autoverify when 4 chars are entered
                                                                if (enteredPin.length == 4) {
                                                                    if (enteredPin == targetPasscode) {
                                                                        statusText = "رمز التخويل مقبول 🟢"
                                                                        scope.launch {
                                                                            delay(300)
                                                                            onUnlockSuccess()
                                                                        }
                                                                    } else {
                                                                        authAttempts++
                                                                        enteredPin = ""
                                                                        errorMessage = "الرمز غير صحيح! محاولة ($authAttempts)"
                                                                        if (authAttempts >= 3 && BiometricHelper.isBiometricAvailable(context)) {
                                                                            errorMessage += "\nينصح بتسجيل الدخول الحيوي لتفادي الحظر"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = if (key.length > 1) 12.sp else 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (key == "مسح" || key == "◀") TextPrimary else Secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Fingerprint/Face Wave Indicator Mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "اضغط على البصمة أدناه لتفعيل ماسح المقاييس الحيوية المدمج في جهازك اللوحي 📲",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Interactive Fingerprint Area
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Secondary.copy(alpha = 0.04f), CircleShape)
                                    .border(2.dp, Secondary.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        BiometricHelper.triggerBiometricPrompt(activity)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🧬",
                                    fontSize = 42.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Button(
                                onClick = { BiometricHelper.triggerBiometricPrompt(activity) },
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(
                                    text = "بدء المسح الحيوي 👤",
                                    color = Primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Divider(color = Secondary.copy(alpha = 0.15f))

                // Footer Mode Switching
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isPinMode = !isPinMode },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = if (isPinMode) "استعمال التحقق الحيوي 🧬" else "أدخل الرمز الميداني اليدوي 🔐",
                            color = Secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "رتبة عسكرية: معتمدة",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
