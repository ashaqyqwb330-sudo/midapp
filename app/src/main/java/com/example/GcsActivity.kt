package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrugDatabaseHelper
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

class GcsActivity : ComponentActivity() {
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
                    GcsCalculatorScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GcsCalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    val isHighContrast by remember { ThemeSettings.isHighContrast }

    // GCS Score selections
    var selectedEye by remember { mutableIntStateOf(4) }
    var selectedVerbal by remember { mutableIntStateOf(5) }
    var selectedMotor by remember { mutableIntStateOf(6) }

    val eyeOptions = listOf(
        Pair(4, "استجابة تلقائية طبيعية (Spontaneous) - 4"),
        Pair(3, "فتح العين للاستدعاء الصوتي (To speech) - 3"),
        Pair(2, "فتح العين للألم والحفز الـمؤلم (To pain) - 2"),
        Pair(1, "لا توجد أي استجابة كاشفة (None) - 1")
    )

    val verbalOptions = listOf(
        Pair(5, "متجاوب ومنتبة للمكان والزمان (Oriented) - 5"),
        Pair(4, "مشوش ومضطرب محادثة مبعثرة (Confused) - 4"),
        Pair(3, "كلمات منفردة غير مترابطة (Inappropriate) - 3"),
        Pair(2, "أصوات غير مفهومة وهمهمة بالألم (Incomprehensible) - 2"),
        Pair(1, "لا توجد أي استجابة صوتية كاشفة (None) - 1")
    )

    val motorOptions = listOf(
        Pair(6, "يطيع الأوامر الحركية بدقة (Obeys commands) - 6"),
        Pair(5, "يحدد موقع المسبب المؤلم ويدفعه (Localizes pain) - 5"),
        Pair(4, "انسحاب حركي سريع هارب من الألم (Withdraws) - 4"),
        Pair(3, "انثناء مرضي غير طبيعي قشر العطل (Decorticate) - 3"),
        Pair(2, "انبساط مرضي غير طبيعي دماغ العطل (Decerebrate) - 2"),
        Pair(1, "لا توجد أي استجابة حركية كاشفة (None) - 1")
    )

    val totalScore = selectedEye + selectedVerbal + selectedMotor
    val classification = when {
        totalScore in 13..15 -> "إصابة دماغية طفيفة (Mild Injury) 👍"
        totalScore in 9..12 -> "إصابة دماغية متوسطة الخطورة (Moderate) ⚠️"
        else -> "إصابة دماغية شديدة وعجز حرج (Severe Injury - Coma) 🚨"
    }

    val classificationColor = when {
        totalScore in 13..15 -> Color(0xFF2ECC71)
        totalScore in 9..12 -> TextOrange
        else -> Color.Red
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مقياس غيبوبة غلاسكو GCS 🧠",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "معاينة الاستجابة العصبية لدى المصابين",
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
                // Computed Score Visual Box
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "حصـيلة نقاط غلاسكو الإجمالية:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalScore / 15",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = classificationColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = classification,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = classificationColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "E$selectedEye V$selectedVerbal M$selectedMotor",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )

                            // Save button
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    dbHelper.saveCalculation(
                                        type = "GCS",
                                        inputs = "فتح العين: $selectedEye, اللفظي: $selectedVerbal, الحركي: $selectedMotor",
                                        result = "العلامة: $totalScore/15 ($classification)"
                                    )
                                    Toast.makeText(context, "تم حفظ النتيجة العصبية بنجاح بنجاح! 💾", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                            ) {
                                Text("تخزين النتيجة العصبية في السجل محلياً 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Eye Opening Option Selection
                item {
                    Text(
                        text = "1. استجابة فتح العين (Eye Opening / E) 👀",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }
                items(eyeOptions) { opt ->
                    val isSel = selectedEye == opt.first
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEye = opt.first }
                            .testTag("eye_opt_${opt.first}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) TextGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) TextGold else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSel, onClick = { selectedEye = opt.first })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = opt.second, fontSize = 12.sp, color = if (isSel) TextGold else TextPrimary)
                        }
                    }
                }

                // Verbal Response Option Selection
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "2. الاستجابة الصوتية / اللفظية (Verbal / V) 🗣️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }
                items(verbalOptions) { opt ->
                    val isSel = selectedVerbal == opt.first
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVerbal = opt.first }
                            .testTag("verbal_opt_${opt.first}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) TextGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) TextGold else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSel, onClick = { selectedVerbal = opt.first })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = opt.second, fontSize = 12.sp, color = if (isSel) TextGold else TextPrimary)
                        }
                    }
                }

                // Motor Response Option Selection
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "3. الاستجابة الحركية (Motor / M) 💪",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }
                items(motorOptions) { opt ->
                    val isSel = selectedMotor == opt.first
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMotor = opt.first }
                            .testTag("motor_opt_${opt.first}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) TextGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) TextGold else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSel, onClick = { selectedMotor = opt.first })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = opt.second, fontSize = 12.sp, color = if (isSel) TextGold else TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
