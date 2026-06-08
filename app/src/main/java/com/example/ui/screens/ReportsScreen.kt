package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.model.Drug
import com.example.data.DrugDatabaseHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Data Classes for Medical Reports (Fully Backward Compatible)
data class ClinicalReport(
    val id: String,
    val patientName: String,
    val patientId: String,
    val date: String,
    val deviceName: String,
    val gcsScore: Int,
    val gcsSeverity: String,
    val egfrVal: Double,
    val egfrInterpretation: String,
    val abgDiagnosis: String,
    val details: String,
    val heartRate: String = "",
    val bloodPressure: String = "",
    val spo2: String = "",
    val selectedDrug: String = "",
    val calculatedDose: String = "",
    val rankUnit: String = "غير محدد",
    val triageColor: String = "أخضر 🟢",
    val temperature: String = "37.0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    val sharedPreferences = remember { context.getSharedPreferences("clinical_reports_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val clipboardManager = LocalClipboardManager.current
    
    // Saved Reports List State
    var savedReports by remember { mutableStateOf<List<ClinicalReport>>(emptyList()) }
    
    // Load existing reports
    LaunchedEffect(Unit) {
        val json = sharedPreferences.getString("reports_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<ClinicalReport>>() {}.type
                savedReports = gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Save function
    fun saveReportsToLocal(list: List<ClinicalReport>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(list)
        editor.putString("reports_list", json)
        editor.apply()
        savedReports = list
    }

    // Patients / Form basic details
    var patientName by remember { mutableStateOf("") }
    var patientId by remember { mutableStateOf("") }
    var rankUnit by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("37.0") }
    var selectedDeviceReport by remember { mutableStateOf("جهاز التنفس الاصطناعي 🫁") }
    
    // SOAP Structure Details
    var subjectiveSymptoms by remember { mutableStateOf("") } // S
    var clinicalNotes by remember { mutableStateOf("") } // A - Notes and clinical assessment

    // Vitals
    var hrVal by remember { mutableStateOf("") }
    var bpVal by remember { mutableStateOf("") }
    var spo2Val by remember { mutableStateOf("") }

    // Integrated fast diagnostics values for linkage
    var linkGcsSum by remember { mutableStateOf("15") }
    var linkeGFR by remember { mutableStateOf("95") }
    var linkABG by remember { mutableStateOf("توازن حمضي قاعدي طبيعي ومستقر (Normal Acid-Base Balance) 🟢") }

    // Prescription link
    var linkedDrugName by remember { mutableStateOf("") }
    var linkedDrugDose by remember { mutableStateOf("") }
    var showDrugPicker by remember { mutableStateOf(false) }

    // Automatically determine Triage Code (كود الفرز) based on Modified Early Warning Score (MEWS)
    val triageColorCode = remember(hrVal, bpVal, spo2Val, linkGcsSum, temperature) {
        val hr = hrVal.toIntOrNull() ?: 75
        val spo2 = spo2Val.toIntOrNull() ?: 98
        val gcs = linkGcsSum.toIntOrNull() ?: 15
        val temp = temperature.toDoubleOrNull() ?: 37.0
        
        val systolic = try {
            val bp = bpVal.trim()
            if (bp.contains("/")) {
                bp.substringBefore("/").trim().toIntOrNull() ?: 120
            } else {
                bp.toIntOrNull() ?: 120
            }
        } catch (e: Exception) {
            120
        }

        // Calculate MEWS Score
        var mewsPoints = 0

        // Heart Rate points
        mewsPoints += when {
            hr <= 40 -> 3
            hr in 41..50 -> 1
            hr in 51..100 -> 0
            hr in 101..110 -> 1
            hr in 111..129 -> 2
            else -> 3 // >= 130
        }

        // Systolic Blood Pressure points
        mewsPoints += when {
            systolic <= 70 -> 3
            systolic in 71..80 -> 2
            systolic in 81..100 -> 1
            systolic in 101..199 -> 0
            else -> 2 // >= 200
        }

        // Temperature points
        mewsPoints += when {
            temp < 35.0 -> 2
            temp in 35.0..38.4 -> 0
            else -> 2 // >= 38.5 / high fever
        }

        // SpO2 points
        mewsPoints += when {
            spo2 >= 95 -> 0
            spo2 in 90..94 -> 1
            spo2 in 85..89 -> 2
            else -> 3 // < 85%
        }

        // GCS points
        mewsPoints += when {
            gcs >= 15 -> 0
            gcs in 13..14 -> 1
            gcs in 9..12 -> 2
            else -> 3 // <= 8
        }

        when {
            mewsPoints >= 5 || gcs <= 8 || spo2 < 88 -> "أحمر 🔴 (حالة حرجة عاجلة - MEWS: $mewsPoints) - إنعاش وتأمين سريع"
            mewsPoints in 3..4 || gcs in 9..12 || spo2 in 88..94 -> "أصفر 🟡 (حالة متوسطة مستقرة جزئياً - MEWS: $mewsPoints) - مراقبة مكثفة"
            else -> "أخضر 🟢 (حالة طفيفة مستقرة - MEWS: $mewsPoints) - متابعة اعتيادية"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "توثيق التقارير الطبية العسكرية",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryLight)
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF02070F),
                            Primary,
                            Color(0xFF0C1929)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Cinematic description
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "🗒️ نظام التقرير الطبي الميداني الموحد (SOAP Note)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                "تقارير طبية مبنية فسيولوجياً وفق الهيكلية الطبية الاحترافية. يدمج الفحص السريري مع فئات الفرز الطبية (Triage Priority) لنقل سلس للمعلومات إلى غرف القيادة والتحكم.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Form Entry
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "👤 [بيانات المجند / المصاب والموقع التكتيكي]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )

                            OutlinedTextField(
                                value = patientName,
                                onValueChange = { patientName = it },
                                label = { Text("اسم المريض بالكامل", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = patientId,
                                    onValueChange = { patientId = it },
                                    label = { Text("الرقم العسكري / المعرف الوطني", fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = rankUnit,
                                    onValueChange = { rankUnit = it },
                                    label = { Text("الرتبة والوحدة (كتيبة)", fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "🌡️ [العلامات الحيوية والتقييم الفسيولوجي المباشر]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hrVal,
                                    onValueChange = { hrVal = it },
                                    label = { Text("نبض (bpm)", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = bpVal,
                                    onValueChange = { bpVal = it },
                                    label = { Text("الضغط (mmHg)", fontSize = 10.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = spo2Val,
                                    onValueChange = { spo2Val = it },
                                    label = { Text("الأكسجين SpO2%", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = temperature,
                                    onValueChange = { temperature = it },
                                    label = { Text("الحرارة °C", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(0.9f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = linkGcsSum,
                                    onValueChange = { linkGcsSum = it },
                                    label = { Text("درجة غيبوبة GCS", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = linkeGFR,
                                    onValueChange = { linkeGFR = it },
                                    label = { Text("تصفية الكلى eGFR", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = linkABG,
                                onValueChange = { linkABG = it },
                                label = { Text("غازات الدم ABG", fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Triage Auto status indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F2034), RoundedCornerShape(8.dp))
                                    .border(1.dp, Secondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("كود فرز الحالة التلقائي (Triage Code):", fontSize = 10.sp, color = TextSecondary)
                                    Text(triageColorCode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "🧠 [صياغة تشخيص المريض - SOAP Diagnostic Structure]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )

                            OutlinedTextField(
                                value = subjectiveSymptoms,
                                onValueChange = { subjectiveSymptoms = it },
                                label = { Text("الأعراض والشكوى الذاتية (S - Subjective / شكوى الجسد)", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                modifier = Modifier.fillMaxWidth().height(70.dp),
                                maxLines = 3
                            )

                            OutlinedTextField(
                                value = clinicalNotes,
                                onValueChange = { clinicalNotes = it },
                                label = { Text("التقييم والتدابير السريرية المتخذة (A - Assessment / خطة الميدان)", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                modifier = Modifier.fillMaxWidth().height(70.dp),
                                maxLines = 3
                            )

                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                "💊 [الخطة الدوائية والعلاجية - Plan (P)]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )

                            if (linkedDrugName.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Secondary, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("العلاج المحدد الموصوف للمصادقة:", fontSize = 9.sp, color = TextSecondary)
                                            Text(linkedDrugName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                            Text("الجرعة المقدرة: $linkedDrugDose", fontSize = 11.sp, color = Color.White)
                                        }
                                        TextButton(onClick = {
                                            linkedDrugName = ""
                                            linkedDrugDose = ""
                                        }) {
                                            Text("إزالة الدواء ❌", fontSize = 11.sp, color = TextOrange)
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showDrugPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔍 ربط ووصف دواء من دليل الـ 47 مستحضر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (patientName.isBlank() || patientId.isBlank()) {
                                        Toast.makeText(context, "الرجاء تعبئة اسم المريض ورقم الهوية لتوثيق التسجيل العلمي الكلي!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val newReport = ClinicalReport(
                                            id = "REP_${System.currentTimeMillis()}",
                                            patientName = patientName,
                                            patientId = patientId,
                                            date = java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
                                            deviceName = selectedDeviceReport,
                                            gcsScore = linkGcsSum.toIntOrNull() ?: 15,
                                            gcsSeverity = if (linkGcsSum.toIntOrNull() ?: 15 >= 13) "خفيفة 🟢" else "شديدة 🔴",
                                            egfrVal = linkeGFR.toDoubleOrNull() ?: 90.0,
                                            egfrInterpretation = if (linkeGFR.toDoubleOrNull() ?: 90.0 >= 60.0) "كافية ومطابقة" else "قصور كلوي محتمل",
                                            abgDiagnosis = linkABG,
                                            details = "S: $subjectiveSymptoms\nA: $clinicalNotes",
                                            heartRate = hrVal,
                                            bloodPressure = bpVal,
                                            spo2 = spo2Val,
                                            selectedDrug = linkedDrugName,
                                            calculatedDose = linkedDrugDose,
                                            rankUnit = if (rankUnit.isNotBlank()) rankUnit else "غير محدد",
                                            triageColor = triageColorCode,
                                            temperature = temperature
                                        )

                                        val updatedList = listOf(newReport) + savedReports
                                        saveReportsToLocal(updatedList)

                                        // Reset
                                        patientName = ""
                                        patientId = ""
                                        rankUnit = ""
                                        temperature = "37.0"
                                        hrVal = ""
                                        bpVal = ""
                                        spo2Val = ""
                                        subjectiveSymptoms = ""
                                        clinicalNotes = ""
                                        linkedDrugName = ""
                                        linkedDrugDose = ""
                                        Toast.makeText(context, "✅ تم حفظ التقرير العلمي الميداني ودمج القياسات بنجاح!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                modifier = Modifier.fillMaxWidth().testTag("save_report_button")
                            ) {
                                Text("صياغة وتأكيد حفظ التقرير 💾", fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Historical Archive
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 أرشيف التقارير الطبية المسجلة (${savedReports.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextGold)
                        if (savedReports.isNotEmpty()) {
                            Text(
                                "تفريغ الأرشيف 🗑️",
                                fontSize = 11.sp,
                                color = TextOrange,
                                modifier = Modifier
                                    .clickable {
                                        saveReportsToLocal(emptyList())
                                        Toast.makeText(context, "تم إفراغ أرشيف التقارير بالكامل.", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                if (savedReports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا يوجد تقارير علمية محفوظة حالياً في الأرشيف الميداني المحلي.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                } else {
                    items(savedReports) { rep ->
                        var isExpanded by remember { mutableStateOf(false) }
                        
                        val formattedDispatchText = remember(rep) {
                            """
                            ========= تقرير طبي عسكري ميداني رسمي =========
                            التاريخ والوقت: ${rep.date}
                            معرف السجل الثنائي: ${rep.id}
                            
                            [1] المعرف الشخصي والموقع التكتيكي:
                            • الاسم: ${rep.patientName}
                            • الرقم العسكري: ${rep.patientId}
                            • الرتبة/الوحدة: ${rep.rankUnit}
                            
                            [2] فرز وتصنيف الحالة (Triage Status):
                            • كود التصنيف الميداني: ${rep.triageColor}
                            • استجابة غيبوبة GCS: ${rep.gcsScore} / 15
                            
                            [3] قياس العلامات المخططة (Physiologic Vitals):
                            • نبض القلب: ${rep.heartRate.ifBlank { "غير مسجل" }} bpm
                            • ضغط الدم: ${rep.bloodPressure.ifBlank { "غير مسجل" }} mmHg
                            • نسبة الأكسجين SpO2: ${rep.spo2.ifBlank { "غير مسجل" }}%
                            • درجة الحرارة: ${rep.temperature}°C
                            
                            [4] الفحوصات الفسيولوجية المدمجة:
                            • التصفية الكلوية المقدرة (eGFR): ${rep.egfrVal} mL/min
                            • غازات الدم الشرياني (ABG): ${rep.abgDiagnosis}
                            
                            [5] التقييم الإكلينيكي والتشخيص (SOAP Note):
                            • التفاصيل والملاحظات: ${rep.details}
                            
                            [6] الخطة العلاجية والدوائية המיועדת (Plan):
                            • دواء مستخدم: ${rep.selectedDrug.ifBlank { "لم يصرف" }}
                            • الجرعات المتخذة: ${rep.calculatedDose.ifBlank { "لا توجد" }}
                            ==============================================
                            """.trimIndent()
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x18FFFFFF)),
                            border = BorderStroke(1.dp, Secondary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth().animateContentSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rep.patientName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                        Text("عسكري: ${rep.patientId} | رتبة: ${rep.rankUnit}", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(rep.date.substringBefore(" "), fontSize = 9.sp, color = TextSecondary)
                                        Box(
                                            modifier = Modifier
                                                .background(Secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(rep.triageColor.substringBefore(" "), fontSize = 9.sp, color = TextGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color.White.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("غيبوبة GCS", fontSize = 8.sp, color = TextSecondary)
                                        Text("${rep.gcsScore} / 15", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("التثبيت الكلوي", fontSize = 8.sp, color = TextSecondary)
                                        Text("${rep.egfrVal} mL/m", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("نبض / ضغط", fontSize = 8.sp, color = TextSecondary)
                                        Text("${rep.heartRate} bpm / ${rep.bloodPressure}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("الأكسجين", fontSize = 8.sp, color = TextSecondary)
                                        Text("${rep.spo2}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Divider(color = Color.White.copy(alpha = 0.05f))
                                        
                                        Text("📋 تفاصيل الهيكلية الطبية (SOAP Note):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                        
                                        // Scientific content formatting
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0x0AFFFFFF), RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("• التشخيص الكيميائي والغازات: ${rep.abgDiagnosis}", fontSize = 10.sp, color = Color.White)
                                                Text("• التفاصيل الطبية: ${rep.details}", fontSize = 10.sp, color = Color.White, lineHeight = 14.sp)
                                                if (rep.selectedDrug.isNotBlank()) {
                                                    Text("• الدواء الموصوف المرفق: ${rep.selectedDrug}", fontSize = 10.sp, color = TextGold, fontWeight = FontWeight.Bold)
                                                    Text("• الجرعة المقدرة الكلية: ${rep.calculatedDose}", fontSize = 10.sp, color = Secondary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // Scientific Dispatch Options
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(formattedDispatchText))
                                                        Toast.makeText(context, "✅ تم كشط ونسخ تشفير التقرير العلمي للذاكرة بنجاح للأجهزة اللاسلكية!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.2f)),
                                                    border = BorderStroke(1.dp, Secondary),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("نسخ التقرير العلمي العاجل 📋", fontSize = 9.sp, color = Secondary, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    val uList = savedReports.filter { it.id != rep.id }
                                                    saveReportsToLocal(uList)
                                                    Toast.makeText(context, "تم إزالة السجل المحدد.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف التقرير", tint = TextOrange, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                if (!isExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "انقر لمعاينة التقرير الطبي العلمي العسكري المكتمل ونقله للذاكرة 👀",
                                        fontSize = 9.sp,
                                        color = Secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Drug Picker Dialog to easily search and link to prescription
            if (showDrugPicker) {
                var searchQ by remember { mutableStateOf("") }
                var selectedCat by remember { mutableStateOf("الكل") }
                var pickList by remember { mutableStateOf<List<Drug>>(emptyList()) }

                LaunchedEffect(searchQ, selectedCat) {
                    val all = dbHelper.getAllDrugs(searchQ)
                    pickList = if (selectedCat == "الكل") {
                        all
                    } else {
                        all.filter { it.category.contains(selectedCat) }
                    }
                }

                Dialog(
                    onDismissRequest = { showDrugPicker = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF020914)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.statusBars.asPaddingValues())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🧬 دليل الـ 47 مستحضر الدوائي لربط الجرعة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                TextButton(onClick = { showDrugPicker = false }) {
                                    Text("عودة والتلخيص ❌", fontSize = 11.sp, color = TextOrange)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // Horizontal categories
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 2.dp)
                            ) {
                                val cats = listOf("الكل", "مسكنات (NSAIDs)", "خافضات الحرارة", "المضادات الحيوية", "مضادات الفطريات", "مضادات الهيستامين", "الكورتيزونات", "مضادات الملاريا", "الجهاز الهضمي", "الجهاز التنفسي", "العناية بالجلد", "الفيتامينات")
                                cats.forEach { cat ->
                                    val isSel = selectedCat == cat
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSel) Secondary else Color(0x19FFFFFF), RoundedCornerShape(16.dp))
                                            .border(1.dp, if (isSel) Secondary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                            .clickable { selectedCat = cat }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(cat, fontSize = 10.sp, color = if (isSel) Primary else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Input search
                            OutlinedTextField(
                                value = searchQ,
                                onValueChange = { searchQ = it },
                                placeholder = { Text("ابحث باسم الدواء أو دواعي الاستخدام...", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Secondary, modifier = Modifier.size(16.dp))
                                }
                            )

                            // List
                            Box(modifier = Modifier.weight(1f)) {
                                if (pickList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("لا يوجد أدوية مطابقة للبحث", fontSize = 11.sp, color = TextSecondary)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(pickList) { drug ->
                                            var pickCustomWeight by remember { mutableStateOf("75") }
                                            var inputCustomConc by remember { mutableStateOf("10") }
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(drug.scientificName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                                            Text(drug.category, fontSize = 9.sp, color = TextSecondary)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                linkedDrugName = drug.scientificName
                                                                
                                                                // Calculate correct pre-fill dose
                                                                val defaultDval = drug.dosePerKg.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                                                val weightFloat = pickCustomWeight.toDoubleOrNull() ?: 75.0
                                                                val totalMg = defaultDval * weightFloat
                                                                
                                                                linkedDrugDose = if (drug.weightBased == "نعم" && defaultDval > 0.0) {
                                                                    val cappedDoseFloat = drug.maxDailyDose.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                                                    val finalDoseMg = if (cappedDoseFloat > 0.0 && totalMg > cappedDoseFloat) cappedDoseFloat else totalMg
                                                                    val rounded = Math.round(finalDoseMg * 100.0) / 100.0
                                                                    val concVal = inputCustomConc.toDoubleOrNull() ?: 10.0
                                                                    val volMl = if (concVal > 0.0) Math.round((rounded / concVal) * 100.0) / 100.0 else 0.0
                                                                    "$rounded mg (سحب $volMl mL بتركيز $concVal mg/mL)"
                                                                } else {
                                                                    drug.dosageGeneral
                                                                }
                                                                
                                                                showDrugPicker = false
                                                                Toast.makeText(context, "تم ربط ${drug.scientificName} بالوصفة!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("ربط وتبويب ⚡", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Text("دواعي الاستعمال: " + drug.uses, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                                    
                                                    // Quick sliders/fields inside pick card to precalculate dosage before linking
                                                    if (drug.weightBased == "نعم") {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().background(Color(0xFF0F2034), RoundedCornerShape(6.dp)).padding(6.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("معيار الوزن والحسابة المسبقة:", fontSize = 8.sp, color = TextSecondary)
                                                            OutlinedTextField(
                                                                value = pickCustomWeight,
                                                                onValueChange = { pickCustomWeight = it },
                                                                label = { Text("الوزن كجم", fontSize = 7.sp) },
                                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                                                modifier = Modifier.width(65.dp).height(38.dp),
                                                                singleLine = true
                                                            )
                                                            OutlinedTextField(
                                                                value = inputCustomConc,
                                                                onValueChange = { inputCustomConc = it },
                                                                label = { Text("التركيز", fontSize = 7.sp) },
                                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Secondary),
                                                                modifier = Modifier.width(60.dp).height(38.dp),
                                                                singleLine = true
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
        }
    }
}
