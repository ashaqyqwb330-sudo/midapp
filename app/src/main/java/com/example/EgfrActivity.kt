package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrugDatabaseHelper
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

class EgfrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalLibraryTheme {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(400)),
                    exit = fadeOut()
                ) {
                    EgfrCalculatorScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EgfrCalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    val isHighContrast by remember { ThemeSettings.isHighContrast }

    // State Variables
    var ageField by remember { mutableStateOf("45") }
    var weightField by remember { mutableStateOf("70") }
    var creatinineField by remember { mutableStateOf("1.0") }
    var isMale by remember { mutableStateOf(true) }

    // Calculations
    val age = ageField.toFloatOrNull() ?: 0f
    val weight = weightField.toFloatOrNull() ?: 0f
    val creatinine = creatinineField.toFloatOrNull() ?: 0f

    val calculatedEgfr = remember(age, weight, creatinine, isMale) {
        if (age > 0f && weight > 0f && creatinine > 0f) {
            val genderF = if (isMale) 1.0f else 0.85f
            ((140f - age) * weight) / (72f * creatinine) * genderF
        } else {
            0f
        }
    }

    val (stageText, stageColor) = remember(calculatedEgfr) {
        when {
            calculatedEgfr <= 0f -> Pair("أدخل بيانات صحيحة للبدء", TextSecondary)
            calculatedEgfr >= 90f -> Pair("المرحلة 1 - وظائف كلوية طبيعية (eGFR >= 90) ✨", Color(0xFF2ECC71))
            calculatedEgfr >= 60f -> Pair("المرحلة 2 - قصور كلوي طفيف (eGFR 60 - 89) 👍", Color(0xFF82E0AA))
            calculatedEgfr >= 45f -> Pair("المرحلة 3أ - قصور متوسط خفيف (eGFR 45 - 59) ⚠️", TextOrange)
            calculatedEgfr >= 30f -> Pair("المرحلة 3ب - قصور متوسط شديد (eGFR 30 - 44) ⚠️", TextOrange)
            calculatedEgfr >= 15f -> Pair("المرحلة 4 - قصور كلوي شديد جداً (eGFR 15 - 29) 🚨", Color.Red)
            else -> Pair("المرحلة 5 - فشل كلوي مطبق نهائي (eGFR < 15) 🚨", Color.Red)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "معدل ترشيح الكلى eGFR 🧪",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "حساب تصفية الكرياتينين لتعديل جرعات الأدوية",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = TextGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextGold
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (isHighContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black))
                    else Brush.verticalGradient(listOf(Primary, Color(0xFF040B15), Color(0xFF0F1F33)))
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 30.dp)
            ) {
                // Computed Visual Result Box
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "معدل ترشيح الكلى المقدر Cockcroft-Gault:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (calculatedEgfr > 0f) String.format("%.1f مل/دقيقة", calculatedEgfr) else "--",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (calculatedEgfr > 0f) stageColor else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stageText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = stageColor,
                                textAlign = TextAlign.Center
                            )

                            if (calculatedEgfr > 0f) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        dbHelper.saveCalculation(
                                            type = "eGFR",
                                            inputs = "العمر: $ageField، الوزن: ${weightField}كجم، الكرياتينين: $creatinineField، الجنس: ${if (isMale) "ذكر" else "أنثى"}",
                                            result = "الترشيح: ${String.format("%.1f", calculatedEgfr)} مل/دقيقة ($stageText)"
                                        )
                                        Toast.makeText(context, "تم حفظ النتيجة الكلوية في السجل محلياً بنجاح! 💾", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                                ) {
                                    Text("تخزين معدل ترشيح الكلى في السجل 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Inputs Field Set
                item {
                    Text(
                        text = "بيانات المريض العينية الحركية 📋",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }

                // Select Gender Button Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isMale = true }
                                .testTag("gender_male"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMale) TextGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
                            ),
                            border = BorderStroke(1.dp, if (isMale) TextGold else Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("ذكر 👨", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isMale) TextGold else TextPrimary)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isMale = false }
                                .testTag("gender_female"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isMale) TextGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
                            ),
                            border = BorderStroke(1.dp, if (!isMale) TextGold else Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("أنثى 👩", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (!isMale) TextGold else TextPrimary)
                            }
                        }
                    }
                }

                // Input Age, Weight, Creatinine
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Age Input
                        Column {
                            Text("العمر (بالسنوات):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = ageField,
                                onValueChange = { ageField = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("egfr_age_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // Weight Input
                        Column {
                            Text("الوزن (كجم):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = weightField,
                                onValueChange = { weightField = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("egfr_weight_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // Serum Creatinine
                        Column {
                            Text("كرياتينين الدم (Serum Creatinine / mg/dL):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = creatinineField,
                                onValueChange = { creatinineField = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("egfr_creat_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Medical Clinical Reminders
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "💡 تذكرة تعديل جرعات المضادات الحيوية العسكرية:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أدوية مثل Gentamicin و Ciprofloxacin و Levofloxacin تتطلب خفض الجرعة أو تباعد فترات الإعطاء بشدة عندما يتراجع الترشيح eGFR عن 50 مل/دقيقة وخصوصاً تحت الـ 30 تفادياً لأعطال الكلوية والسمية السمعية.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
