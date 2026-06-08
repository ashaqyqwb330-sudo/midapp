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

class AbgActivity : ComponentActivity() {
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
                    AbgCalculatorScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbgCalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    val isHighContrast by remember { ThemeSettings.isHighContrast }

    // States for inputs
    var phField by remember { mutableStateOf("7.40") }
    var pco2Field by remember { mutableStateOf("40") }
    var hco3Field by remember { mutableStateOf("24") }

    val ph = phField.toFloatOrNull() ?: 7.4f
    val pco2 = pco2Field.toFloatOrNull() ?: 40f
    val hco3 = hco3Field.toFloatOrNull() ?: 24f

    // ABG Interpretation engine
    val interpretation = remember(ph, pco2, hco3) {
        val phState = when {
            ph < 7.35f -> "حماض دمي شرياني (Acidemia) ⚠️"
            ph > 7.45f -> "قلاء دمي شرياني (Alkalemia) ⚠️"
            else -> "مستوى الحموضة الإجمالي طبيعي (Normal pH) ✨"
        }

        val primaryDisorder: String
        val compensation: String

        if (ph < 7.35f) {
            // Acidosis
            val respContrib = pco2 > 45f
            val metContrib = hco3 < 24f
            
            primaryDisorder = when {
                respContrib && metContrib -> "حماض مختلط (تنفسي واستقلابي معاً) 🚨"
                respContrib -> "حماض تنفسي أولي (Primary Respiratory Acidosis) 🫁"
                metContrib -> "حماض استقلابي / أميري أولي (Primary Metabolic Acidosis) 🧪"
                else -> "خلل حرج غير معرف"
            }

            compensation = when {
                respContrib && metContrib -> "عدم وجود تعويض (تفاقم مدمج)"
                respContrib -> if (hco3 > 26f) "تعويض استقلابي كلوي جزئي أو ممتد" else "غير معوض حتى الآن"
                else -> if (pco2 < 35f) "تعويض تنفسي رئوي سريع (زفير تنفس معجل)" else "غير معوض حتى الآن"
            }
        } else if (ph > 7.45f) {
            // Alkalosis
            val respContrib = pco2 < 35f
            val metContrib = hco3 > 26f

            primaryDisorder = when {
                respContrib && metContrib -> "قلاء مختلط (تنفسي واستقلابي معاً) 🚨"
                respContrib -> "قلاء تنفسي أولي (Primary Respiratory Alkalosis) 🫁"
                metContrib -> "قلاء استقلابي أولي (Primary Metabolic Alkalosis) 🧪"
                else -> "خلل حرج غير معرف"
            }

            compensation = when {
                respContrib && metContrib -> "عدم وجود تعويض"
                respContrib -> if (hco3 < 22f) "تعويض استقلابي كلوي جزئي" else "غير معوض حتى الآن"
                else -> if (pco2 > 45f) "تعويض تنفسي رئوي (نقص التنفس المعوض)" else "غير معوض حتى الآن"
            }
        } else {
            // Normal PH but can have compensated disorder
            if (pco2 != 40f || hco3 != 24f) {
                primaryDisorder = "خلل حمضي قاعدي معاوض بالكامل (Fully Compensated)"
                compensation = "معاوض بسلامة حيوية"
            } else {
                primaryDisorder = "الحالة الأساسية طبيعية ومتوازنة بنجاح (Euboxia) ✨"
                compensation = "توازن فيزيولوجي ممتاز"
            }
        }

        Triple(phState, primaryDisorder, compensation)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "غازات الدم الشرياني ABG 🫁",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "تشخيص الحماض والقلاء والـتعويض الرئوي الكلوي",
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
                // computed result display card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "تفسير وتشخيص غازات الشريان ABG:", fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = interpretation.second,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ph < 7.35f) Color.Red else if (ph > 7.45f) TextOrange else Color(0xFF2ECC71),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "حالة الحموضة: ${interpretation.first}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "التعويض الفسيولوجي: ${interpretation.third}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            // Save to Local history log button
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    dbHelper.saveCalculation(
                                        type = "ABG",
                                        inputs = "pH: $phField, pCO2: $pco2Field, HCO3: $hco3Field",
                                        result = "التشخيص: ${interpretation.second} [التعويض: ${interpretation.third}]"
                                    )
                                    Toast.makeText(context, "تم حفظ تشخيص غازات الدم بنجاح في سجل الميدان! 💾", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                            ) {
                                Text("تخزين التشخيص الغازي في السجل محلياً 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Normal Reference guide values
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("القيم المعيارية للغازات الشريانية الطبيعية 🩺:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• درجة الحموضة (pH): 7.35 - 7.45", fontSize = 11.sp, color = TextPrimary)
                        Text("• ضغط غاز ثاني أكسيد الكربون (pCO2): 35 - 45 مم زئبق", fontSize = 11.sp, color = TextPrimary)
                        Text("• شارد بيكربونات الصوديوم (HCO3): 22 - 26 mEq/L", fontSize = 11.sp, color = TextPrimary)
                    }
                }

                // inputs form fields
                item {
                    Text(
                        text = "أدخل قراءات المخبر / المحلل الميداني 📊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // PH Input
                        Column {
                            Text("درجة الحموضة (pH / 7.00 - 7.80):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phField,
                                onValueChange = { phField = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("abg_ph_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // pCO2 Input
                        Column {
                            Text("ضغط ثاني أكسيد الكربون (pCO2 / mmHg):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = pco2Field,
                                onValueChange = { pco2Field = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("abg_pco2_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextGold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // HCO3 Input
                        Column {
                            Text("البيكربونات المذابة (HCO3 / mEq/L):", fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = hco3Field,
                                onValueChange = { hco3Field = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("abg_hco3_input"),
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
            }
        }
    }
}
