package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.example.model.Drug
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

class DosageActivity : ComponentActivity() {
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
                    DosageCalculatorScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosageCalculatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    val isHighContrast by remember { ThemeSettings.isHighContrast }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var drugs by remember { mutableStateOf<List<Drug>>(emptyList()) }
    var selectedDrug by remember { mutableStateOf<Drug?>(null) }

    // Weight and calculations inputs
    var patientWeight by remember { mutableStateOf("70") }

    // Reload list on search / category change
    LaunchedEffect(searchQuery, selectedCategory) {
        val rawList = dbHelper.getAllDrugs(searchQuery)
        drugs = if (selectedCategory == "الكل") {
            rawList
        } else {
            rawList.filter { it.category.contains(selectedCategory) }
        }
    }

    val categories = listOf(
        "الكل",
        "الطوارئ",
        "المضادات",
        "الآلام",
        "الصرع",
        "المدرات",
        "الجهاز الهضمي",
        "التنفس",
        "الترياقات"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "حاسبة جرعات الأدوية المتقدمة 💊",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "حساب دقيق لعدد 47 عقار عسكري وطبي طارئ",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Real-time Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الدواء العلمي أو الـدواعي...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "بحث", tint = TextGold) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("drug_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Category selection Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) TextGold
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                // Main Layout: Splits into Active drug calculator details vs List of drugs
                if (selectedDrug != null) {
                    val drug = selectedDrug!!
                    // Expandable focused dosage card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHighContrast) Color(0xFF111111) else Color(0x30162540)
                        ),
                        border = BorderStroke(1.dp, if (isHighContrast) Color.Yellow else Color.White.copy(alpha = 0.12f))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = drug.scientificName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )
                                        Text(text = drug.category, fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Button(
                                        onClick = { selectedDrug = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                                    ) {
                                        Text("إغلاق", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                            }

                            // Description & Mechanism
                            item {
                                Column {
                                    Text("الـتعريف وبنية الدواء 🧪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.definition, fontSize = 11.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("آلية العمل الفسيولوجية ⚙️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.mechanism, fontSize = 11.sp, color = TextPrimary)
                                }
                            }

                            // Interactive Calculations Block
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "قسم الحساب الميداني الكاشف 🧮",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )

                                        if (drug.weightBased == "yes" || drug.weightBased == "نعم") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "وزن البدن (كجم):",
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = patientWeight,
                                                    onValueChange = { patientWeight = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.width(90.dp).height(50.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = TextGold,
                                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White
                                                    )
                                                )
                                            }

                                            // Calc Result
                                            val weightVal = patientWeight.toFloatOrNull() ?: 0f
                                            val dosePerKgVal = drug.dosePerKg.toFloatOrNull() ?: 0f
                                            val calculatedDose = weightVal * dosePerKgVal
                                            
                                            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(TextGold.copy(alpha = 0.1f))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("الجرعة المحسوبة للحقن:", fontSize = 12.sp, color = TextPrimary)
                                                    Text(
                                                        text = String.format("%.2f ملجم", calculatedDose),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextOrange
                                                    )
                                                }

                                                // Warning check
                                                if (drug.maxDailyDose.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "⚠️ مـلاحظة هامة (الحد الأعلى): ${drug.maxDailyDose}",
                                                        fontSize = 10.sp,
                                                        color = Color.Yellow,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Save to log button
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        dbHelper.saveCalculation(
                                                            type = "جرعة: ${drug.scientificName}",
                                                            inputs = "الوزن: ${weightVal}كجم، المعيار: ${drug.dosePerKg}ملجم/كجم",
                                                            result = "الجرعة المقدرة: ${String.format("%.2f", calculatedDose)} ملجم"
                                                        )
                                                        Toast.makeText(context, "تم حفظ الحسبة والجرعة محلياً بنجاح! 💾", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                                                ) {
                                                    Text("تخزين الحسبة في سجل الميدان 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            // Non-weight based instructions
                                            Text(
                                                text = "عقار معياري غير مرتبط بالوزن مباشرة.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TextGold.copy(alpha = 0.08f))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "الجرعة العامة: ${drug.dosageGeneral}",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                            // Save static calculation
                                            Button(
                                                onClick = {
                                                    dbHelper.saveCalculation(
                                                        type = "جرعة: ${drug.scientificName}",
                                                        inputs = "معياري / عام",
                                                        result = "الجرعة العامة: ${drug.dosageGeneral}"
                                                    )
                                                    Toast.makeText(context, "تم حفظ الحسبة للأرشيف والرجوع الميداني! 💾", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                                            ) {
                                                Text("أرشفة الجرعة العادية 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Dosage Forms & Administration
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("الأشكال والأحجام الدوائية 📦", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.dosageForms, fontSize = 11.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("طرق التعاطي والتزريق 💉", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.administration, fontSize = 11.sp, color = TextPrimary)
                                }
                            }

                            // Contraindications & Side Effects
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("موانع الاستعمال الحرجة 🚫", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.contraindications, fontSize = 11.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("التأثيرات الجانبية السامة ⚠️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.sideEffects, fontSize = 11.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("الاحتياطات والتحذيرات السريرية 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = drug.precautions, fontSize = 11.sp, color = TextPrimary)
                                }
                            }

                            // Age dependent Info
                            if (drug.ageDependent == "yes" || drug.ageDependent == "نعم") {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Yellow.copy(alpha = 0.08f))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("محددات الفئات العمرية والصغار 👶:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
                                            Text(text = drug.ageFormula, fontSize = 10.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty list state or display list of drugs
                    Text(
                        text = "اختر عقار لبدء وحساب التفاصيل والـجرعة الميدانية 💊:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (drugs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد أدوية تطابق معطيات هذا البحث.", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(drugs) { drug ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDrug = drug }
                                        .testTag("drug_item_${drug.scientificName.replace(" ", "_")}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isHighContrast) Color(0xFF141414) else Color(0x15FFFFFF)
                                    ),
                                    border = BorderStroke(1.dp, if (isHighContrast) Color.Yellow.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = drug.scientificName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            val categoryTag = drug.category.substringBefore(" ").ifBlank { "📌" }
                                            Text(text = categoryTag, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الدواعي: ${drug.uses}",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 2
                                        )
                                        if (drug.weightBased == "yes" || drug.weightBased == "نعم") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TextOrange.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "يعتمد على الوزن: ${drug.dosePerKg} ملجم/كجم",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextOrange
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
