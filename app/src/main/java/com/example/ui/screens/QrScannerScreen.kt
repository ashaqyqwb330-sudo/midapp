package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.DataProvider
import com.example.model.BookEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldButton
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class EquipmentInfo(
    val tag: String,
    val arabicName: String,
    val englishName: String,
    val model: String,
    val securityLevel: String,
    val currentStatus: String,
    val statusColor: Color,
    val batteryLevel: String,
    val specDetails: List<String>,
    val fileKeywords: List<String>,
    val titleKeywords: List<String>
)

object EquipmentDatabase {
    val items = listOf(
        EquipmentInfo(
            tag = "MML-EQ-ECG",
            arabicName = "جهاز تخطيط القلب العسكري المحمول",
            englishName = "Tactical Electrocardiograph (ECG)",
            model = "ECG-MML-Alpha",
            securityLevel = "سري - للاستخدام الطبي المرخص",
            currentStatus = "نشط وجاهز للخدمة الكلية",
            statusColor = Color(0xFF2ECC71),
            batteryLevel = "92%",
            specDetails = listOf(
                "سجل ورصد 12 قناة تلوينية بالوقت الحقيقي",
                "مقاومة معززة للصدمات والاهتزازات الميدانية فئة MIL-STD",
                "بطارية مدمجة سريعة الشحن تدوم لغاية 8 ساعات عمل ميداني متصل",
                "مزود بتقنية الفحص الذاتي التلقائي ومرشحات إشارة التردد العالي"
            ),
            fileKeywords = listOf("ecg"),
            titleKeywords = listOf("تخطيط القلب")
        ),
        EquipmentInfo(
            tag = "MML-EQ-DEFIB",
            arabicName = "جهاز الصدمات وإنقاذ الحياة ثنائي الطور",
            englishName = "Biphasic Defibrillator Monitor",
            model = "DEFIB-MML-Rescue",
            securityLevel = "سري للغاية - طواقم الرعاية الفورية",
            currentStatus = "جاهز ومعاير فسيولوجياً",
            statusColor = Color(0xFF2ECC71),
            batteryLevel = "100%",
            specDetails = listOf(
                "نطاق طاقة قابل للضبط من 2 إلى 200 جول ثنائي الطور بالكامل",
                "مراقب متكامل لنظم القلب ومساعد صوتي تكتيكي ثنائي اللغة",
                "فترة شحن فائقة السرعة أقل من 5 ثوانٍ لأقصى طاقة تفريغية",
                "أقطاب صدرية ذكية ومستشعر فحص المعايرة المسبقة للأقطاب"
            ),
            fileKeywords = listOf("defib"),
            titleKeywords = listOf("الصدمات الكهربائية")
        ),
        EquipmentInfo(
            tag = "MML-EQ-VENT",
            arabicName = "جهاز التنفس الاصطناعي للرعاية الحرجة",
            englishName = "Critical Care Field Ventilator",
            model = "VENT-MML-Tactical",
            securityLevel = "سري - طواقم التخدير والعناية المركزة",
            currentStatus = "نشط - بحاجة لمعايرة الأكسجين بعد 40 ساعة",
            statusColor = Color(0xFFF1C40F),
            batteryLevel = "85%",
            specDetails = listOf(
                "أنماط تهوية متعددة ومتكاملة: VCV, PCV, SIMV, CPAP/PSV",
                "ضاغط هواء مدمج فسيولوجي للتشغيل المستقل دون إمداد مركزي",
                "استهلاك ذكي وموفر للأكسجين مع إنذارات ضغط وهواء متطورة",
                "هيكل خارجي صلب محكم مقاوم للغبار والقطرات المائية IP33"
            ),
            fileKeywords = listOf("vent"),
            titleKeywords = listOf("التنفس الاصطناعي", "التنفس")
        ),
        EquipmentInfo(
            tag = "MML-EQ-MONITOR",
            arabicName = "جهاز مراقبة المؤشرات الحيوية المتكامل",
            englishName = "Multi-Parameter Patient Monitor",
            model = "MON-MML-Sentinel",
            securityLevel = "سري - كوادر المراقبة الطبية بالمستشفيات الميدانية",
            currentStatus = "نشط وجاهز للتشغيل والربط الكلي",
            statusColor = Color(0xFF2ECC71),
            batteryLevel = "78%",
            specDetails = listOf(
                "مراقبة متزامنة لمؤشرات: ECG, SpO2, NIBP, Respiration, Temp",
                "شاشة عرض تباينية عالية السطوع تتيح القراءة تحت أشعة الشمس",
                "وحدة اتصالات لاسلكية مشفرة لرفع المؤشرات إلى القيادة المركزية",
                "إنذارات حيوية صوتية ومرئية ملونة قابلة للتعديل والضبط التكتيكي"
            ),
            fileKeywords = listOf("monitor"),
            titleKeywords = listOf("المؤشرات الحيوية", "مراقبة المريض")
        ),
        EquipmentInfo(
            tag = "MML-EQ-MRI",
            arabicName = "جهاز الرنين المغناطيسي الميداني المتنقل",
            englishName = "Mobile MRI Spec Unit",
            model = "MRI-MML-Quantum",
            securityLevel = "سري للغاية - قسم الفيزياء الهندسية والأشعة",
            currentStatus = "خامل - بحاجة لمعايرة التبريد الهيليومي المسبق",
            statusColor = Color(0xFFE74C3C),
            batteryLevel = "شحن عبر الشبكة المستمرة",
            specDetails = listOf(
                "شدة حقل مغناطيسي 1.5 تسلا مدمجة بهيكل مقطورة متنقل عسكري",
                "تقنيات تصوير فائقة الدقة بتغذية وتخميد كهرومغناطيسي ذاتي",
                "معالج تصوير كمي سريع لتقليل فترات انتظار المرضى في ساحة العمليات",
                "برامج حماية ومزامنة لصد التشويش والحقول المحيطية الضارة"
            ),
            fileKeywords = listOf("mri"),
            titleKeywords = listOf("الرنين المغناطيسي")
        ),
        EquipmentInfo(
            tag = "MML-EQ-CT",
            arabicName = "جهاز التصوير المقطعي CT المتنقل",
            englishName = "Mobile Tomography CT System",
            model = "CT-MML-Scout",
            securityLevel = "سري للغاية - الخدمات الطبية والتشخيص المتقدم",
            currentStatus = "نشط وجاهز لالتقاط المقاطع السريرية فوراً",
            statusColor = Color(0xFF2ECC71),
            batteryLevel = "مولد طاقة مساعد مستقل",
            specDetails = listOf(
                "نظام تصوير متعدد الكواشف ذو 64 مقطعًا سريعًا للمسح الشامل",
                "حماية قصوى مدمجة ضد الإشعاع للمستخدمين في الغلاف الميداني",
                "برمجيات معالجة ومطابقة فسيولوجية للأوتار وصدمات الأنف العلوية",
                "توافق كلي مع أنظمة التصوير الطبي المشتركة وحفظ الصيغة الطبية PACS"
            ),
            fileKeywords = listOf("ct"),
            titleKeywords = listOf("التصوير المقطعي")
        )
    )

    fun findMatch(scannedValue: String): EquipmentInfo? {
        val cleaned = scannedValue.trim().uppercase()
        return items.firstOrNull {
            it.tag.uppercase() == cleaned ||
            cleaned.contains(it.tag.uppercase()) ||
            it.englishName.uppercase() == cleaned ||
            cleaned.contains(it.englishName.uppercase()) ||
            it.arabicName == scannedValue ||
            scannedValue.contains(it.arabicName)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onNavigateToPdf: (BookEntry) -> Unit
) {
    val context = LocalContext.current
    val dataProvider = remember { DataProvider(context) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var matchedEquipment by remember { mutableStateOf<EquipmentInfo?>(null) }
    var relatedBooks by remember { mutableStateOf<List<BookEntry>>(emptyList()) }
    var scanActive by remember { mutableStateOf(true) }

    // Sound and custom haptic functions for realistic scan
    val playBeep = {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Reaction when QR is parsed or mocked
    val handleScanSuccess: (String) -> Unit = { result ->
        if (scanActive) {
            scanActive = false
            scannedResult = result
            playBeep()
            
            val match = EquipmentDatabase.findMatch(result)
            matchedEquipment = match
            
            if (match != null) {
                // Find matching books/documents using keywords
                relatedBooks = dataProvider.allBooks.filter { book ->
                    val titleNormal = book.title.lowercase()
                    val fileNormal = book.file.lowercase()
                    
                    val matchFile = match.fileKeywords.any { kw -> fileNormal.contains(kw) }
                    val matchTitle = match.titleKeywords.any { kw -> titleNormal.contains(kw) }
                    
                    matchFile || matchTitle
                }
            } else {
                relatedBooks = emptyList()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qr_scanner_container"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📸",
                            fontSize = 22.sp
                        )
                        Column {
                            Text(
                                text = "حاقن المسح والأكواد التكتيكي 📸",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                            Text(
                                text = "مطابقة ملصقات الأجهزة الطبية وجلب كتيبات الصيانة الميدانية والأدلة",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary.copy(alpha = 0.95f),
                    titleContentColor = TextGold
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Primary, Color(0xFF070F1A))
                    )
                )
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Area / Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, Secondary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    if (scanActive) {
                        CameraXScannerView(
                            onQrCodeScanned = { qr ->
                                handleScanSuccess(qr)
                            }
                        )
                        // Neon tactical scan HUD frame
                        TacticalScanOverlay()
                    } else {
                        // Scan paused or success state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PrimaryLight.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("🎯", fontSize = 48.sp)
                                Text(
                                    text = "تم التقاط وقراءة الكود بالكامل بنجاح!",
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = scannedResult ?: "",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        scanActive = true
                                        scannedResult = null
                                        matchedEquipment = null
                                        relatedBooks = emptyList()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                                ) {
                                    Text("مسح ملصق جديد 🔄", color = Primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Camera privilege required screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("📷", fontSize = 42.sp)
                        Text(
                            text = "كاميرا الجهاز مطلوبة لتشغيل فاحص الـ QR",
                            color = TextGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "يرجى منح صلاحية استخدام الكاميرا لمطابقة باركود الأجهزة والمقررات الطبية في الميدان.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("السماح باستخدام الكاميرا 🔐", color = Primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Results and Spec Sheet Section OR Simulator Options
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (scannedResult == null) {
                    // MOCK INTERACTION SCREEN
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Secondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚡ لوحة محاكاة وتدريب فحص الملصقات الميدانية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold
                                )
                                Text(
                                    text = "اضغط على أي من كود الأجهزة المسبق جردها أدناه لمحاكاة عملية المسح الضوئي وقراءة التجهيزات الفنية وتحميل كتيبات الصيانة والوقاية فورًا:",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Grid of simulator buttons
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val items = EquipmentDatabase.items
                            items(items) { eqItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            handleScanSuccess(eqItem.tag)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Secondary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Secondary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🛡️", fontSize = 18.sp)
                                            }
                                            Column {
                                                Text(
                                                    text = eqItem.arabicName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${eqItem.tag} • Model: ${eqItem.model}",
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "محاكاة المسح 📲",
                                            fontSize = 9.sp,
                                            color = TextGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(0.5.dp, TextGold, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // SCAN SHOW DATA
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            val eq = matchedEquipment
                            if (eq != null) {
                                // Real hardware specs matched!
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Secondary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = eq.arabicName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextGold
                                                )
                                                Text(
                                                    text = eq.englishName,
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(eq.statusColor.copy(alpha = 0.15f))
                                                    .border(0.5.dp, eq.statusColor, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = eq.currentStatus,
                                                    color = eq.statusColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Divider(color = Secondary.copy(alpha = 0.15f))

                                        // Detailed Stats block
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("رمز المتابعة الفنية (Label):", fontSize = 10.sp, color = TextSecondary)
                                                Text(eq.tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("رقم الموديل الفسيولوجي:", fontSize = 10.sp, color = TextSecondary)
                                                Text(eq.model, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("مستوى السرية العسكرية:", fontSize = 10.sp, color = TextSecondary)
                                                Text(eq.securityLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE74C3C))
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("شحن البطارية الحالي:", fontSize = 10.sp, color = TextSecondary)
                                                Text(eq.batteryLevel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                                            }
                                        }

                                        Divider(color = Secondary.copy(alpha = 0.15f))

                                        // Specifications bullets
                                        Text(
                                            text = "📋 المواصفات الطبية والهندسية الفورية للجهاز:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            eq.specDetails.forEach { spec ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text("•", color = Secondary, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = spec,
                                                        fontSize = 10.sp,
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }

                                        Divider(color = Secondary.copy(alpha = 0.15f))

                                        // Matching manuals in Library list
                                        Text(
                                            text = "📚 الكتيبات والمقررات والتعليمات التشغيلية المطابقة بالجهاز (${relatedBooks.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )

                                        if (relatedBooks.isEmpty()) {
                                            Text(
                                                text = "ملاحظة: لا توجد أدلة ومقررات PDF صريحة مرابطة في قاعدة المنهج الحالي.",
                                                fontSize = 9.5.sp,
                                                color = TextSecondary
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                relatedBooks.forEach { book ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Secondary.copy(alpha = 0.1f))
                                                            .border(0.5.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                onNavigateToPdf(book)
                                                            }
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("📄", fontSize = 16.sp)
                                                            Column {
                                                                Text(
                                                                    text = book.title,
                                                                    fontSize = 10.5.sp,
                                                                    color = TextPrimary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = "صيغة: PDF • ملف: ${book.file}",
                                                                    fontSize = 8.sp,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "فتح الدليل 📖",
                                                            fontSize = 9.sp,
                                                            color = TextGold,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Raw barcode scanning result fallback
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Secondary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "🔍 كود ممسوح غير مصنف بقاعدة العتاد الطبية",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )
                                        Text(
                                            text = "تم التقاط وفك تشفير رمز مخصص أو رابط خارجي من ملصق عسكري. القيمة المستخلصة هي:",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = scannedResult ?: "",
                                                color = Color(0xFF2ECC71),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Start
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Google search fallback
                                        val isUrl = scannedResult?.startsWith("http") == true
                                        Button(
                                            onClick = {
                                                try {
                                                    val urlToOpen = if (isUrl) scannedResult!! else "https://www.google.com/search?q=${Uri.encode(scannedResult)}"
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "لا يمكن تصفح الملف الخارجي", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isUrl) "تصفح الرابط المفتوح 🌐" else "البحث عن هذا الرمز عبر محرك القياسة 🌐",
                                                color = Primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
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

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraXScannerView(
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    analyzeImage(imageProxy, onQrCodeScanned)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@androidx.camera.core.ExperimentalGetImage
private fun analyzeImage(imageProxy: ImageProxy, onQrCodeScanned: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && rawValue.isNotEmpty()) {
                        onQrCodeScanned(rawValue)
                        break
                    }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun TacticalScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    
    // Animate scanning bar up and down
    val lineOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_bar"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val laserY = maxHeight * lineOffsetFraction

        // Target box highlight
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .border(2.dp, Secondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            // Corner Reticles for military theme vibe
            val reticleColor = Secondary
            val strokeW = 4.dp
            val lineL = 16.dp

            // Top Left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(lineL)
                    .border(strokeW, reticleColor, RoundedCornerShape(topStart = 6.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            )
            // Top Right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(lineL)
                    .border(strokeW, reticleColor, RoundedCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            )
            // Bottom Left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(lineL)
                    .border(strokeW, reticleColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 6.dp, bottomEnd = 0.dp))
            )
            // Bottom Right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(lineL)
                    .border(strokeW, reticleColor, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 6.dp))
            )
        }

        // Active sweep laser bar
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .offset(y = laserY)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFD700).copy(alpha = 0.15f),
                            Color(0xFFFFD700),
                            Color(0xFFFFD700).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
