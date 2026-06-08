package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.TerminalText
import com.example.ui.components.laserScanSweep
import com.example.ui.components.laserScanRipple
import com.example.ui.components.staggeredEntrance
import com.example.ui.components.IntubationSimulator
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random

// ----------------------------------------------------
// Core Data Models
// ----------------------------------------------------
data class SimulationScenario(
    val id: String,
    val title: String,
    val description: String,
    val initialHR: Int,
    val initialSpO2: Int,
    val initialBP: String,
    val initialRR: Int,
    val initialStatus: String,
    val targetHR: Int,
    val targetSpO2: Int,
    val targetBP: String,
    val targetRR: Int,
    val diagnosticDetails: String,
    val steps: List<SimulationCommand>,
    val educationalNotes: String
)

data class SimulationCommand(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val feedback: String,
    val effectDescription: String
)

data class VRHeadset(
    val id: String,
    val name: String,
    val type: String,
    val details: String,
    val signalIcon: String
)

data class HazardAgent(
    val key: String,
    val name: String,
    val latinName: String,
    val levelCode: String,
    val description: String,
    val pulseSpeed: Int,
    val antidoteName: String,
    val idealDosePrompt: String,
    val correctDoseIdx: Int,
    val doseOptions: List<String>,
    val ppeRequired: String,
    val tacticalSteps: List<String>
)

data class AnatomyHotspot(
    val id: String,
    val arName: String,
    val engName: String,
    val xPct: Float,
    val yPct: Float,
    val x3d: Float = 0f,
    val y3d: Float = 0f,
    val z3d: Float = 0f,
    val symptoms: String,
    val safetyStatus: String = "غير مستقر ⚠️",
    val urgentIntervention: String = "",
    val fieldKit: String = "",
    val surgicalSteps: List<String> = emptyList(),
    val procedure: String,
    val tools: List<String>,
    val triageClass: String,
    val triageColor: Color
)

// ----------------------------------------------------
// Case Studies Interactive Models
// ----------------------------------------------------
data class CombatCaseStudy(
    val id: String,
    val title: String,
    val category: String,
    val background: String,
    val startNodeId: String,
    val nodes: Map<String, CaseNode>
)

data class CaseNode(
    val id: String,
    val prompt: String,
    val dangerDelta: Int,      // change in hazard meter
    val stabilityDelta: Int,   // Patient health delta
    val options: List<CaseOption>
)

data class CaseOption(
    val label: String,
    val feedback: String,
    val targetNodeId: String?,  // null means end (Success or Defeat)
    val isFatal: Boolean = false,
    val isVictory: Boolean = false,
    val msg: String = ""
)

// ----------------------------------------------------
// Main Composable Screen
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Tone generator for sci-fi audible beeps
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (e: Exception) {
            null
        }
    }

    fun playBeep(type: Int = ToneGenerator.TONE_PROP_BEEP) {
        scope.launch {
            try {
                toneGenerator?.startTone(type, 110)
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    // Tab categories matching requirements
    var activeTab by remember { mutableStateOf(0) } // 0: Patient Simulator, 1: Dashboard, 2: Case Studies, 3: VR & 3D Devices

    // Transition tracking
    var transitionTriggered by remember { mutableStateOf(0f) }
    LaunchedEffect(activeTab) {
        playBeep(ToneGenerator.TONE_CDMA_PIP)
        transitionTriggered = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            transitionTriggered = value
        }
    }

    val scenarios = remember {
        listOf(
            SimulationScenario(
                id = "SC_01",
                title = "💥 الصدمة النزفية وجرح الصدر المفتوح (Tension Pneumothorax)",
                description = "إسعاف جندي مصاب بشظية بالصدر مع زراق شديد (Cyanosis)، وصعوبة بالغة بالتنفس، وضياع أصوات التنفس في الرئة اليسرى.",
                initialHR = 135,
                initialSpO2 = 82,
                initialBP = "82/46",
                initialRR = 28,
                initialStatus = "حرج جداً - نزيف صدري حاد وتراكم ضغط الهواء",
                targetHR = 88,
                targetSpO2 = 98,
                targetBP = "118/72",
                targetRR = 16,
                diagnosticDetails = "معاينة صديرية سريعة تكشف عن انحراف الغضروف الدرقي الجانبي (Tracheal Deviation) مع ارتفاع ضغط الوريد الوداجي.",
                educationalNotes = "عند تفريغ الهواء المحتبس، يتم فوراً إطلاق الضغط الجانبي الرئوي وتدفق الأكسجين وتخفيض نبضات القلب وإعادة الإنعاش التلقائي.",
                steps = listOf(
                    SimulationCommand(
                        "ST_1_1",
                        "تطبيق صمام تنفيس الصدر وتخفيف الضغط بإبرة مخصصة (Needle Decompression)",
                        true,
                        "عمل بطولي رائع! تم إدخال الإبرة في الضلع الثاني بالجهة اليسرى وتصريف الهواء المحتبس فوراً.",
                        "استقرار سريع للغاية لضغط الغشاء الجانبي وتحسن الأكسجين SpO2 إلى 98%."
                    ),
                    SimulationCommand(
                        "ST_1_2",
                        "عمل جبيرة صدرية لاصقة مغلقة عادية دون منفذ (Standard Non-venting Seal)",
                        false,
                        "تنبيه! الصمام المغلق بالكامل يزيد من اختناق المريض وتراكم ضغطه الرئوي، الصمام ذو المنافذ هو الأفضل لشظايا جدران الصدر.",
                        "ارتفاع طفيف في نبض القلب وهبوط إضافي لعمل الرئة."
                    ),
                    SimulationCommand(
                        "ST_1_3",
                        "حقن جرعة المورفين لتسكين الآلام فوراً (Morphine Administration)",
                        false,
                        "خطأ فادح! المورفين يسبب ثبيط تنفسي حاد (Respiratory Depression) لمريض يعاني أصلاً من فشل تنفسي صدري وصدمة حادة.",
                        "انخفاض حرج بمعدل الإنعاش والتنفس."
                    )
                )
            ),
            SimulationScenario(
                id = "SC_02",
                title = "🧪 التطهير الميداني لهجوم كيميائي (CBRN Nerve Agent)",
                description = "تدفق مصابين من موقع هجوم كيماوي لغاز السارين/الأعصاب مع سيلان حاد باللعاب، انقباض حدقة العين (Miosis)، واختناق رئوي.",
                initialHR = 42,
                initialSpO2 = 78,
                initialBP = "70/40",
                initialRR = 8,
                initialStatus = "تسمم حاد مركزي بمستقبلات الكولين",
                targetHR = 85,
                targetSpO2 = 96,
                targetBP = "115/75",
                targetRR = 18,
                diagnosticDetails = "تثبيط كامل لإنزيم الكولينستريز (Cholinesterase Inhibitor). غاز السيلان يعطل الرئتين والقلب ويؤدي لغيبوبة سريعة.",
                educationalNotes = "الوقاية الفردية للمسعف أولاً، ثم التطهير الجاف التكتيكي، يتبعه حقن الأتروبين والتوكسوجونين لإلغاء مفعول الغاز الكيميائي.",
                steps = listOf(
                    SimulationCommand(
                        "ST_2_1",
                        "البدء الفوري بالتنفس من الفم لإنقاذ المصاب سريعاً (Mouth-to-Mouth Ventilation)",
                        false,
                        "كارثة تكتيكية! انتقال التسمم الكيميائي التبخيري للمسعف مباشرة وموته فوراً. الأمن الذاتي هو المطلب الأول في حوادث CBRN.",
                        "إصابة مميتة للمسعف وإيقاف كامل للمحاكاة."
                    ),
                    SimulationCommand(
                        "ST_2_2",
                        "إعطاء ترياق الأتروبين الفولاذي المزدوج وحاقن الأوتوبين (Atropine + Oxime Autoinjector)",
                        true,
                        "أحسنت جداً! تم حقن المصاب بترياق الأتروبين المزدوج (Atropine 2mg) لإلغاء تثبيط القلب والمفرزات وتجفيف القصبات الهوائية.",
                        "ارتفاع معدل ضربات القلب وتحسن الضغط والتنفس واستعادة وعي المصاب تدريجياً."
                    ),
                    SimulationCommand(
                        "ST_2_3",
                        "غسل المصاب بماء عادي دون إزالة الملابس (Immediate Unstripped Water Wash)",
                        false,
                        "غير دقيق! الماء مع الملابس الملوثة يعجل من نفاذ وتغلغل الغاز الكيميائي عبر الجلد (Glowworm effect). يجب قص وإخلاء الملابس أولاً.",
                        "زيادة استثارة التسمم عبر الأوعية الدموية الجلدية."
                    )
                )
            ),
            SimulationScenario(
                id = "SC_03",
                title = "⚡ عطل كهربائي طارئ بجهاز مزيل الرجفان بمستشفى ميداني",
                description = "وصول مريض يعاني من ذبذبة بطينية مميتة (Ventricular Fibrillation) مع صدمة كهربائية معطلة وتفريغ غير منسجم.",
                initialHR = 210,
                initialSpO2 = 65,
                initialBP = "50/20",
                initialRR = 6,
                initialStatus = "توقف قلب وشيك وتذبذب بطيني مهلك",
                targetHR = 78,
                targetSpO2 = 98,
                targetBP = "120/80",
                targetRR = 14,
                diagnosticDetails = "فشل شحن المكثف الأساسي بالجهاز، يحتاج إلى تحويل لتغذية البطارية الاحتياطية وتأكيد المعايرة الذاتية السريعة.",
                educationalNotes = "في حالات الرجفان البطيني (VF)، كل دقيقة تأخير بالصدمة الكهربائية تقلل فرصة النجاة بنسبة 10%. المعايرة ضرورية لضمان تسليم الطاقة المطلوبة.",
                steps = listOf(
                    SimulationCommand(
                        "ST_3_1",
                        "تحويل الطاقة لخط البطارية المزدوج وإجراء تفريغ شحنة اختباري معاير (Paddle Sync Test)",
                        true,
                        "استجابة ممتازة وسريعة! تم تحويل التغذية وتأكيد مزامنة تفريغ الشحنة (Sync Mode ON) وصدم المريض بقوة 200 جول ثنائية الطور.",
                        "عودة فورية لنظم القلب الجيبي الطبيعي (Sinus Rhythm) وتوقف التذبذب القاتل."
                    ),
                    SimulationCommand(
                        "ST_3_2",
                        "الاستمرار بالإنعاش اليدوي التقليدي فقط وتجاهل الجهاز (CPR-Only Progression)",
                        false,
                        "تنبيه! الإنعاش CPR بمفرده لا يعالج التذبذب البطيني (VF). الرجفان البطيني يعالج فقط بصدمة مزيل الرجفان الفورية الصاعقة.",
                        "فشل الإنعاش التدريجي مع زيادة الإعياء على المريض."
                    ),
                    SimulationCommand(
                        "ST_3_3",
                        "تغيير جل الأقطاب بجل تبريد حروق عادي (Burn Gel on Paddles)",
                        false,
                        "خطأ جراحي! جل الحروق يسبب تولد حروق ومقاومة تيار كهربائي مرتفع يمنع وصول الشحنة لعضلة القلب.",
                        "ممانعة مرتفعة في تسليم فولتية الصدمة الكهربائية لعضلة الصدر."
                    )
                )
            )
        )
    }

    val vrHeadsets = remember {
        listOf(
            VRHeadset(
                "VR_01",
                "نظارة الرأس التكتيكية (MilSpecs HoloLens Pro)",
                "AR Mixed Reality",
                "المعاينة الميدانية المصاحبة للأجهزة والتتبع البصري التراكمي",
                "📶"
            ),
            VRHeadset(
                "VR_02",
                "نظارة المحاكاة الجراحية (BioTactical VR Spec V4)",
                "Full Immersive VR",
                "محاكاة التدخل الجراحي والإنعاش التكتيكي الافتراضي بالكامل",
                "🥽"
            )
        )
    }

    // ----------------------------------------------------
    // Dynamic Simulation States
    // ----------------------------------------------------
    var selectedScenarioIndex by remember { mutableStateOf(0) }
    val currentScenario = scenarios[selectedScenarioIndex]

    var liveHR by remember { mutableStateOf(currentScenario.initialHR) }
    var liveSpO2 by remember { mutableStateOf(currentScenario.initialSpO2) }
    var liveBP by remember { mutableStateOf(currentScenario.initialBP) }
    var liveRR by remember { mutableStateOf(currentScenario.initialRR) }
    var liveStatusText by remember { mutableStateOf(currentScenario.initialStatus) }

    var logs by remember { mutableStateOf(listOf("بداية الجلسة: تم تحميل السيناريو الميداني بنجاح.")) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    var scoreXP by remember { mutableStateOf(100) }
    var activeCommandFeedback by remember { mutableStateOf("") }
    var isCommandSuccess by remember { mutableStateOf<Boolean?>(null) }

    var isVRConnecting by remember { mutableStateOf(false) }
    var isVRConnected by remember { mutableStateOf(false) }
    var connectedHeadsetName by remember { mutableStateOf("") }
    var vrStatusDetails by remember { mutableStateOf("النظارات الافتراضية مطفأة أو غير مزامنة") }

    // 3D medical device rotation degrees
    var rotation3dX by remember { mutableStateOf(35f) }
    var rotation3dY by remember { mutableStateOf(45f) }

    // Holographic Medical dial variables adjusted by physician
    var flowRate by remember { mutableStateOf(10f) }      // L/min
    var pressureThreshold by remember { mutableStateOf(24f) } // cmH2O
    var o2MixPct by remember { mutableStateOf(40f) }      // Oxygen percentage

    // Reset simulator variables upon changing scenarios
    LaunchedEffect(selectedScenarioIndex) {
        liveHR = currentScenario.initialHR
        liveSpO2 = currentScenario.initialSpO2
        liveBP = currentScenario.initialBP
        liveRR = currentScenario.initialRR
        liveStatusText = currentScenario.initialStatus
        activeCommandFeedback = ""
        isCommandSuccess = null
        scoreXP = 100
        logs = listOf("تغيير المحاكاة: تم تحميل السجل الطبي لـ ${currentScenario.title}.")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ecg_monitor")
    val ecgProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecg_progress"
    )

    // Layout Root Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040914), Primary, Color(0xFF0F1F33))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Upper cyber military title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x3F050A15))
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x1F162540), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "عودة للخلف",
                    tint = TextGold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مركز المحاكاة ومعمل العمليات الحربي 🥽",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGold,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "الأنظمة التكتيكية المتقدمة ومزامنة المعالجات الجراحية ثلاثية الأبعاد",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Right
                )
            }
        }

        // Professional Sci-fi Segmented Tab bar with Brackets (Framer-motion vibe) - Adaptive Scrollable LazyRow
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabTitles = listOf(
                "📟 الاستجابة السريرية",
                "📈 مؤشرات التقدم",
                "🛡️ دراسات الحالات والنزاع",
                "🥽 نظارات VR والـ 3D",
                "🧪 معمل الأدوات التكتيكية والتوليدية"
            )

            items(tabTitles.size) { index ->
                val title = tabTitles[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TextGold.copy(alpha = 0.15f) else Color.Transparent)
                        .laserScanSweep(isSelected)
                        .clickable { activeTab = index }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextGold else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Transition content wrapper (Simulation of Framer motion snappy spring entrance + sweeping scan overlay)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            // Apply spring transform and alpha drift
            val alphaAnim by animateFloatAsState(
                targetValue = transitionTriggered,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "alpha"
            )
            val translateY by animateDpAsState(
                targetValue = ((1.0f - transitionTriggered) * 20).dp,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
                label = "translate"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = translateY)
                    .graphicsLayer {
                        alpha = alphaAnim
                        scaleX = 0.96f + 0.04f * alphaAnim
                        scaleY = 0.96f + 0.04f * alphaAnim
                    }
                    .drawBehind {
                        // Drawing professional military coordinates lines or scanning scan grids
                        if (transitionTriggered > 0f && transitionTriggered < 1.0f) {
                            val scanY = size.height * transitionTriggered
                            drawLine(
                                color = TextGold.copy(alpha = 0.45f),
                                start = Offset(0f, scanY),
                                end = Offset(size.width, scanY),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawRect(
                                color = TextGold.copy(alpha = 0.03f),
                                size = size.copy(height = scanY)
                            )
                        }
                    }
                    .padding(top = 4.dp)
            ) {
                when (activeTab) {
                    0 -> PatientSimulatorTab(
                        scenarios = scenarios,
                        selectedScenarioIndex = selectedScenarioIndex,
                        onSelectScenario = { selectedScenarioIndex = it },
                        currentScenario = currentScenario,
                        liveHR = liveHR,
                        liveSpO2 = liveSpO2,
                        liveBP = liveBP,
                        liveRR = liveRR,
                        liveStatusText = liveStatusText,
                        scoreXP = scoreXP,
                        ecgProgress = ecgProgress,
                        activeCommandFeedback = activeCommandFeedback,
                        isCommandSuccess = isCommandSuccess,
                        logs = logs,
                        onExecuteCommand = { isCorrect, feedback, effectDesc, cmdText ->
                            playBeep(if (isCorrect) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_SUP_ERROR)
                            if (isCorrect) {
                                scoreXP = if (scoreXP < 100) scoreXP + 10 else 100
                                liveHR = currentScenario.targetHR
                                liveSpO2 = currentScenario.targetSpO2
                                liveBP = currentScenario.targetBP
                                liveRR = currentScenario.targetRR
                                liveStatusText = "استقر المصاب بنجاح ✓ - $effectDesc"
                                isCommandSuccess = true
                                activeCommandFeedback = "✅ $feedback"
                                logs = logs + "إجراء معتمد: $cmdText -> $effectDesc"
                                Toast.makeText(context, "إجراء بطل تكتيكي متميز! 🏆", Toast.LENGTH_SHORT).show()
                            } else {
                                scoreXP = if (scoreXP > 15) scoreXP - 15 else 5
                                liveHR = (liveHR * 1.08f).toInt().coerceAtMost(190)
                                liveSpO2 = (liveSpO2 * 0.9f).toInt().coerceAtLeast(50)
                                liveBP = "55/25"
                                liveStatusText = "تنبيه! تدهور للعلامات الحيوية: $effectDesc"
                                isCommandSuccess = false
                                activeCommandFeedback = "❌ $feedback"
                                logs = logs + "خطأ خطير: $cmdText -> $effectDesc"
                                Toast.makeText(context, "تحذير: إجراء طبي غير ممتثل! ⚠️", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReset = {
                            playBeep(ToneGenerator.TONE_CDMA_ABBR_ALERT)
                            liveHR = currentScenario.initialHR
                            liveSpO2 = currentScenario.initialSpO2
                            liveBP = currentScenario.initialBP
                            liveRR = currentScenario.initialRR
                            liveStatusText = currentScenario.initialStatus
                            activeCommandFeedback = ""
                            isCommandSuccess = null
                            logs = logs + "إعادة معايرة السيناريو."
                        },
                        onShowExplanation = { showExplanationDialog = true }
                    )
                    1 -> ProfessionalDashboardTab(playBeep = { playBeep(it) })
                    2 -> MilitaryCaseStudiesTab(playBeep = { playBeep(it) })
                    3 -> VRHeadsetsAnd3DDevicesTab(
                        vrHeadsets = vrHeadsets,
                        isVRConnected = isVRConnected,
                        isVRConnecting = isVRConnecting,
                        connectedHeadsetName = connectedHeadsetName,
                        vrStatusDetails = vrStatusDetails,
                        rotationX = rotation3dX,
                        rotationY = rotation3dY,
                        flowRate = flowRate,
                        pressureThreshold = pressureThreshold,
                        o2MixPct = o2MixPct,
                        onUpdateRotation = { dx, dy ->
                            rotation3dX = (rotation3dX + dy) % 360f
                            rotation3dY = (rotation3dY + dx) % 360f
                        },
                        onUpdateFlow = { flowRate = it },
                        onUpdatePressure = { pressureThreshold = it },
                        onUpdateO2Pct = { o2MixPct = it },
                        onConnectHeadset = { name ->
                            if (isVRConnecting) return@VRHeadsetsAnd3DDevicesTab
                            scope.launch {
                                isVRConnecting = true
                                playBeep(ToneGenerator.TONE_CDMA_CONFIRM)
                                vrStatusDetails = "جاري مسح شبكة الاتصال التكتيكي المشفر والبحث عن البلوتوث..."
                                delay(1800)
                                isVRConnecting = false
                                isVRConnected = true
                                connectedHeadsetName = name
                                vrStatusDetails = "تم الربط والمزامنة بنجاح! دقة الإرسال 100% ومعدل 90 إطاراً بالثانية."
                                logs = logs + "اتصال خارجي: دمج نظارة $name مع القاذفة الجراحية."
                                playBeep(ToneGenerator.TONE_PROP_BEEP)
                            }
                        },
                        onDisconnectHeadset = {
                            playBeep(ToneGenerator.TONE_SUP_ERROR)
                            isVRConnected = false
                            connectedHeadsetName = ""
                            vrStatusDetails = "النظارات الافتراضية مطفأة أو غير مزامنة"
                            logs = logs + "فصل اتصال: إنهاء مزامنة جهاز العرض التكتيكي."
                        }
                    )
                    4 -> TacticalToolsTab(playBeep = { playBeep(it) })
                }
            }
        }
    }

    // Modal dialog for lesson explanation
    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            containerColor = Color(0xFF030814),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.2.dp, TextGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📡 اتصال تكتيكي المرجع",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "الدرس العلمي المصاحب للحالة 📘",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        textAlign = TextAlign.Right
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    TerminalText(
                        text = currentScenario.educationalNotes,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = TextPrimary,
                        speedMs = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Text(
                        text = "يساعد التدريب من خلال معمل المحاكاة العسكري على تنمية المعاينة الطبية السريعة والقرارات المصيرية تحت الضغط في الميدان لتخفيض وفيات الخطوط الأمامية.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TextGold)
                ) {
                    Text("فهمت واستوعبت المرجعية علمياً 🫡", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ====================================================
// Tab 0: Patient Simulator Composable
// ====================================================
@Composable
fun PatientSimulatorTab(
    scenarios: List<SimulationScenario>,
    selectedScenarioIndex: Int,
    onSelectScenario: (Int) -> Unit,
    currentScenario: SimulationScenario,
    liveHR: Int,
    liveSpO2: Int,
    liveBP: String,
    liveRR: Int,
    liveStatusText: String,
    scoreXP: Int,
    ecgProgress: Float,
    activeCommandFeedback: String,
    isCommandSuccess: Boolean?,
    logs: List<String>,
    onExecuteCommand: (Boolean, String, String, String) -> Unit,
    onReset: () -> Unit,
    onShowExplanation: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Scenario Selection Row
        item {
            Column {
                Text(
                    text = "🩺 اختر أحد السيناريوهات القتالية والسريرية لبدء المحاكاة المباشرة وعرض مؤشرات المريض الحيوية:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(scenarios.size) { index ->
                        val scenario = scenarios[index]
                        val isSelected = (index == selectedScenarioIndex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) TextGold else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.dp,
                                    if (isSelected) TextGold else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .laserScanRipple(isSelected)
                                .clickable { onSelectScenario(index) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (scenario.id == "SC_01") "💥" else if (scenario.id == "SC_02") "🧪" else "⚡",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = scenario.title.substringBefore("(").trim(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cinematic Vital Monitor Panel (Draws smooth wave)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF030710).copy(alpha = 0.85f))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (liveHR > 120 || liveHR < 50) TextOrange else Color(0xFF2ECC71))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "وحدة مراقبة الوظائف الحيوية للمريض (Vitals Monitor)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                        }
                        Text(
                            text = "المجال الميداني النشط 📟",
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid of patient vital values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Heart Rate Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pulse (HR)", fontSize = 8.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "❤️",
                                        fontSize = 10.sp,
                                        modifier = Modifier.offset(y = if (ecgProgress > 0.45f && ecgProgress < 0.65f) (-2).dp else 0.dp)
                                    )
                                    Text(
                                        text = "$liveHR",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (liveHR > 120 || liveHR < 50) TextOrange else Color(0xFF2ECC71),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("bpm", fontSize = 7.sp, color = TextSecondary)
                                }
                            }
                        }

                        // Oxygen level SpO2 Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Oxygen (SpO2)", fontSize = 8.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("💧", fontSize = 10.sp)
                                    Text(
                                        text = "$liveSpO2%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (liveSpO2 < 90) TextOrange else Color(0xFF3498DB),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Blood Pressure Card
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Blood Press (mmHg)", fontSize = 8.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("♨️", fontSize = 10.sp)
                                    Text(
                                        text = liveBP,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Resp Rate Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Resp (RR)", fontSize = 8.sp, color = TextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("🌬️", fontSize = 10.sp)
                                    Text(
                                        text = "$liveRR",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (liveRR > 20 || liveRR < 10) TextOrange else Color(0xFF1ABC9C),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated real-time ECG Green Line Draw
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .drawBehind {
                                val gridWidth = size.width
                                val gridHeight = size.height

                                val gridSpacing = 20.dp.toPx()
                                var xCoord = 0f
                                while (xCoord < gridWidth) {
                                    drawLine(
                                        color = Color.Green.copy(alpha = 0.04f),
                                        start = Offset(xCoord, 0f),
                                        end = Offset(xCoord, gridHeight),
                                        strokeWidth = 1f
                                    )
                                    xCoord += gridSpacing
                                }

                                val count = 150
                                val path = Path()
                                val sampleStep = gridWidth / count
                                path.moveTo(0f, gridHeight / 2)

                                for (i in 0..count) {
                                    val x = i * sampleStep
                                    val normalizedX = (x / gridWidth + ecgProgress) % 1.0f
                                    val yVal = when {
                                        normalizedX in 0.2f..0.25f -> {
                                            val t = (normalizedX - 0.2f) / 0.05f
                                            Math.sin(t * Math.PI).toFloat() * -3.dp.toPx()
                                        }
                                        normalizedX in 0.28f..0.3f -> {
                                            val t = (normalizedX - 0.28f) / 0.02f
                                            t * 1.5.dp.toPx()
                                        }
                                        normalizedX in 0.3f..0.34f -> {
                                            val t = (normalizedX - 0.3f) / 0.04f
                                            if (t < 0.5f) (t * 2f) * -18.dp.toPx() else ((1.0f - t) * 2f) * -18.dp.toPx()
                                        }
                                        normalizedX in 0.34f..0.36f -> {
                                            val t = (normalizedX - 0.34f) / 0.02f
                                            (1.0f - t) * 5.dp.toPx()
                                        }
                                        normalizedX in 0.44f..0.52f -> {
                                            val t = (normalizedX - 0.44f) / 0.08f
                                            Math.sin(t * Math.PI).toFloat() * -5.dp.toPx()
                                        }
                                        else -> 0f
                                    }

                                    val multiplier = if (liveHR > 120) 1.25f else if (liveHR < 50) 0.60f else 1.0f
                                    val finalY = gridHeight / 2 + (yVal * multiplier)
                                    path.lineTo(x, finalY)
                                }

                                drawPath(
                                    path = path,
                                    color = if (liveHR > 120 || liveHR < 50) TextOrange else Color(0xFF2ECC71),
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Diagnosis & XP status row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("التشخيص ووصف الجرح الحالي:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                Text("نقاط الكفاءة التكتيكية: $scoreXP XP", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                            }
                            Text(
                                text = liveStatusText,
                                fontSize = 10.5.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 2.dp),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Diagnostic information details card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x0F112239))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "تفاصيل سريرية", tint = TextGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("العلامات العيادية والتشخيص الأولي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentScenario.diagnosticDetails,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentScenario.description,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp)
                    )
                }
            }
        }

        // Action Buttons list
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🛠️ حدد الإجراء الطبي التكتيكي الفوري الصحيح لإنقاذ حياة المقاتل في معمل المحاكاة:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGold
                )

                currentScenario.steps.forEach { step ->
                    val isSelectedStep = activeCommandFeedback.contains(step.text)
                    Card(
                        onClick = { onExecuteCommand(step.isCorrect, step.feedback, step.effectDescription, step.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isSelectedStep) {
                                    if (step.isCorrect) Color(0xFF2ECC71).copy(alpha = 0.7f) else TextOrange.copy(alpha = 0.7f)
                                } else {
                                    Color.White.copy(alpha = 0.08f)
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedStep) {
                                if (step.isCorrect) Color(0x1F2ECC71) else Color(0x1FEE4F57)
                            } else {
                                Color(0x0AFFFFFF)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelectedStep && step.isCorrect) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = "تنفيذ الأمر",
                                tint = if (isSelectedStep) {
                                    if (step.isCorrect) Color(0xFF2ECC71) else TextOrange
                                } else TextGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }

        // Feedback & Educational popup
        if (activeCommandFeedback.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCommandSuccess == true) Color(0x222ECC71) else Color(0x22E74C3C))
                        .border(
                            1.dp,
                            if (isCommandSuccess == true) Color(0xFF2ECC71).copy(alpha = 0.4f) else Color(0xFFE74C3C).copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (isCommandSuccess == true) "🛡️ نتيجة وتقرير التقييم الميداني الناجح:" else "⚠️ تقرير الخطأ الطبي العسكري:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCommandSuccess == true) Color(0xFF2ECC71) else TextOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeCommandFeedback,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onShowExplanation) {
                                Text("قراءة الدرس العلمي المصاحب لهذه الحالة 📖", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGold)
                            }

                            if (isCommandSuccess == true) {
                                Button(
                                    onClick = onReset,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("إعادة تصفير المحاكاة 🔄", fontSize = 9.sp, color = TextGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Action Logs block
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📜 سجل تتبع الإجراءات التكتيكة والقياس (Live Logs):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize()) {
                        items(logs.asReversed()) { log ->
                            Text(
                                text = "► $log",
                                fontSize = 9.5.sp,
                                color = if (log.contains("خطأ")) TextOrange else if (log.contains("إجراء") || log.contains("اتصال")) Color(0xFF2ECC71) else TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ====================================================
// Tab 1: Physician Progress & Dashboard (Using Canvas)
// ====================================================
@Composable
fun ProfessionalDashboardTab(playBeep: (Int) -> Unit) {
    // Selected point on chart to display tooltip
    var selectedChartIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Military Rank Title Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF030710).copy(alpha = 0.4f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(TextGold.copy(alpha = 0.12f))
                            .border(1.2.dp, TextGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🦅", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مقدم طبيب تكتيكي • مستوى 4", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextGold)
                        Text("مستشفى الكواليس العسكرية الميدانية • التقييم القتالي: ممتاز", fontSize = 9.5.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar to level 5
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.77f)
                                        .background(Brush.horizontalGradient(listOf(TextGold, Color(0xFF2ECC71))))
                                )
                            }
                            Text("77%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                        }
                    }
                }
            }
        }

        // Spline Area Graph (Native Rich Chart with interactive nodes representing physician drill metrics)
        item {
            val chartValues = listOf(68f, 81f, 89f, 86f, 95f)
            val chartLabels = listOf("الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4", "الأسبوع 5")
            val drillNames = listOf("إنعاش فوري", "تفجير صدري", "صدمة قتالية", "تطهير كيميائي", "علاج الصدمة الكلّي")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📈 منحنى دقة القرارات الطبية تحت الضغط الميداني:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                        Text("تفاعلي ⬤", fontSize = 8.5.sp, color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
                    }
                    Text("انقر على نقاط المنحنى لعرض تفاصيل الاختبارات الأسبوعية ونقاط السلوك المكتسبة", fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val stepWidth = size.width / (chartValues.size - 1)
                                            val clickedSegment = (offset.x / stepWidth).roundToInt()
                                            if (clickedSegment in chartValues.indices) {
                                                selectedChartIndex = clickedSegment
                                                playBeep(ToneGenerator.TONE_CDMA_PIP)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val stepWidth = size.width / (chartValues.size - 1)
                                            val clickedSegment = (change.position.x / stepWidth).roundToInt()
                                            if (clickedSegment in chartValues.indices && clickedSegment != selectedChartIndex) {
                                                selectedChartIndex = clickedSegment
                                                playBeep(ToneGenerator.TONE_CDMA_PIP)
                                            }
                                        }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw horizontal guidelines
                            val linesCount = 4
                            for (i in 0..linesCount) {
                                val gridY = h * (i / linesCount.toFloat())
                                drawLine(
                                    color = Color.White.copy(alpha = 0.04f),
                                    start = Offset(0f, gridY),
                                    end = Offset(w, gridY),
                                    strokeWidth = 1f
                                )
                            }

                            // Calculate points spacing
                            val stepX = w / (chartValues.size - 1)
                            val points = chartValues.mapIndexed { idx, value ->
                                val x = idx * stepX
                                // map 50%-100% to canvas height ranges representing accuracy scale
                                val pct = (value - 50f) / 50f
                                val y = h - (pct * h * 0.8f) - (h * 0.1f)
                                Offset(x, y)
                            }

                            // Draw gradient area underneath spline
                            val fillPath = Path()
                            fillPath.moveTo(0f, h)
                            fillPath.lineTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPointX = p1.x + (p2.x - p1.x) / 2
                                fillPath.cubicTo(
                                    controlPointX, p1.y,
                                    controlPointX, p2.y,
                                    p2.x, p2.y
                                )
                            }
                            fillPath.lineTo(w, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(TextGold.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )

                            // Draw spline stroke curve line
                            val strokePath = Path()
                            strokePath.moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPointX = p1.x + (p2.x - p1.x) / 2
                                strokePath.cubicTo(
                                    controlPointX, p1.y,
                                    controlPointX, p2.y,
                                    p2.x, p2.y
                                )
                            }

                            drawPath(
                                path = strokePath,
                                color = TextGold,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Draw nodes and active hover rings
                            points.forEachIndexed { index, point ->
                                val isHovered = selectedChartIndex == index
                                drawCircle(
                                    color = if (isHovered) Color(0xFF2ECC71) else TextGold,
                                    radius = if (isHovered) 5.dp.toPx() else 3.5.dp.toPx(),
                                    center = point
                                )
                                if (isHovered) {
                                    drawCircle(
                                        color = Color(0xFF2ECC71).copy(alpha = 0.25f),
                                        radius = 12.dp.toPx(),
                                        center = point,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // Labels below the chart
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chartLabels.forEachIndexed { index, label ->
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                color = if (selectedChartIndex == index) TextGold else TextSecondary,
                                fontWeight = if (selectedChartIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Interactive Tooltip Info Box
                    Spacer(modifier = Modifier.height(10.dp))
                    val focusIndex = if (selectedChartIndex == -1) 4 else selectedChartIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📌 تفاصيل الاختبار (${chartLabels[focusIndex]}):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold
                                )
                                Text(
                                    text = "دقة المزامنة: ${chartValues[focusIndex]}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chartValues[focusIndex] >= 85) Color(0xFF2ECC71) else TextOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "تدريب العمليات التكتيكية تحت الشدة: ${drillNames[focusIndex]} - تقييم فوري للكفاءة الكبيدية وتفريغ الضغط. السلوك الإسعافي سليم.",
                                fontSize = 9.5.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Radar Chart representation (Concentric hexagon representing medical skills axes)
        item {
            val skillLabels = listOf("سارين CBRN", "إنعاش CPR", "نزيف صدري", "صدمة قتالية", "مسالك قصبية")
            val skillValues = listOf(0.85f, 0.92f, 0.78f, 0.90f, 0.81f) // maximum 1.0f

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🕸️ مخطط رادار الكفاءة التكتيكية (Physician Competency Node):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "يعرض أوزان مهارة الطبيب التكتيكي في الحالات الميدانية والجراحية السريرية",
                        fontSize = 8.8.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2 * 0.82f
                            val sides = 5

                            // Draw concentric pentagons as grids
                            val levels = 4
                            for (l in 1..levels) {
                                val currentRad = radius * (l / levels.toFloat())
                                val pathGrid = Path()
                                for (i in 0 until sides) {
                                    val angle = (i * 2 * Math.PI / sides) - Math.PI / 2
                                    val x = center.x + cos(angle).toFloat() * currentRad
                                    val y = center.y + sin(angle).toFloat() * currentRad
                                    if (i == 0) pathGrid.moveTo(x, y) else pathGrid.lineTo(x, y)
                                }
                                pathGrid.close()
                                drawPath(
                                    path = pathGrid,
                                    color = Color.White.copy(alpha = 0.05f),
                                    style = Stroke(width = 1f)
                                )
                            }

                            // Draw spoke lines from center to outer pentagon
                            for (i in 0 until sides) {
                                val angle = (i * 2 * Math.PI / sides) - Math.PI / 2
                                val endPoint = Offset(
                                    center.x + cos(angle).toFloat() * radius,
                                    center.y + sin(angle).toFloat() * radius
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = center,
                                    end = endPoint,
                                    strokeWidth = 1f
                                )
                            }

                            // Plot and draw the score outline pentagon
                            val skillPath = Path()
                            val skillPoints = mutableListOf<Offset>()
                            for (i in 0 until sides) {
                                val currentVal = skillValues[i]
                                val angle = (i * 2 * Math.PI / sides) - Math.PI / 2
                                val currentRad = radius * currentVal
                                val x = center.x + cos(angle).toFloat() * currentRad
                                val y = center.y + sin(angle).toFloat() * currentRad
                                skillPoints.add(Offset(x, y))
                                if (i == 0) skillPath.moveTo(x, y) else skillPath.lineTo(x, y)
                            }
                            skillPath.close()

                            // Filled score polygon
                            drawPath(
                                path = skillPath,
                                color = TextGold.copy(alpha = 0.22f),
                                style = Fill
                            )
                            drawPath(
                                path = skillPath,
                                color = TextGold,
                                style = Stroke(width = 1.6.dp.toPx())
                            )

                            // Nodes
                            skillPoints.forEach { pt ->
                                drawCircle(color = TextGold, radius = 3.dp.toPx(), center = pt)
                            }
                        }
                    }

                    // Display labels and scores of competency Radar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        skillLabels.forEachIndexed { index, skill ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(skill, fontSize = 8.sp, color = TextSecondary)
                                Text("${(skillValues[index] * 100).toInt()}%", fontSize = 9.5.sp, color = TextGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================
// Tab 2: Military Case Studies (Tactical interactive)
// ====================================================

@Composable
fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 120L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        content()
    }
}

@Composable
fun CyberVitalDashboard(
    stability: Int,
    isFatal: Boolean,
    isVictory: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg_loop")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val bpm = remember(stability, isFatal) {
        if (isFatal) 0 
        else if (stability < 30) 140 
        else if (stability < 60) 105 
        else 75
    }
    
    val color = when {
        isFatal || stability < 30 -> Color(0xFFE74C3C)
        stability < 60 -> Color(0xFFE67E22)
        else -> Color(0xFF2ECC71)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        enablePersistentLaser = true
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📟 الاستقرار الحيوي للمصاب (BIO-TELEMETRY)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = if (bpm == 0) "⚠️ FLATLINE - غياب النبض" else "⚡ النبض: $bpm BPM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val path = Path()
                
                path.moveTo(0f, midY)
                val pointsCount = 120
                for (i in 0..pointsCount) {
                    val x = (width / pointsCount) * i
                    val normalizedX = (i.toFloat() / pointsCount) * 6f * Math.PI.toFloat() + animatedPhase
                    
                    val y = if (bpm == 0) {
                        midY
                    } else {
                        // Dynamic ECG complex simulation
                        val segment = (x / width * 4.5f + animatedPhase) % (Math.PI.toFloat() * 1.5f)
                        val heartImpulse = if (segment > 1.0f && segment < 1.3f) {
                            val pulsePhase = (segment - 1.0f) / 0.3f
                            if (pulsePhase < 0.2f) {
                                -midY * 0.5f * (pulsePhase / 0.2f) 
                            } else if (pulsePhase < 0.5f) {
                                midY * 0.85f * ((pulsePhase - 0.2f)/0.3f) - midY * 0.5f 
                            } else {
                                -midY * 0.25f * (1.0f - (pulsePhase - 0.5f)/0.5f) 
                            }
                        } else if (segment > 1.6f && segment < 2.0f) {
                            val pulsePhase = (segment - 1.6f) / 0.4f
                            Math.sin(pulsePhase * Math.PI).toFloat() * 12f
                        } else {
                            0f
                        }
                        
                        val drift = Math.sin(normalizedX.toDouble() * 2.0).toFloat() * 2f
                        (midY - heartImpulse + drift).coerceIn(4f, height - 4f)
                    }
                    
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الأكسجين SpO2: ${if(isFatal) "0" else "${(stability * 0.4f + 60).roundToInt()}%"}", fontSize = 9.sp, color = TextSecondary)
                Text("الضغط BP: ${if(isFatal) "0/0" else "${(stability * 0.5f + 70).roundToInt()}/${(stability * 0.3f + 45).roundToInt()}"}", fontSize = 9.sp, color = TextSecondary)
                Text("التنفس RR: ${if(isFatal) "0" else "${(stability * 0.12f + 12).roundToInt()}/min"}", fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun CombatRiskIndicator(
    riskLevel: Float,
    modifier: Modifier = Modifier
) {
    val animatedRisk by animateFloatAsState(
        targetValue = riskLevel,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "risk"
    )

    val color = when {
        animatedRisk > 70f -> Color(0xFFE74C3C)
        animatedRisk > 40f -> Color(0xFFE67E22)
        else -> Color(0xFF2ECC71)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        enablePersistentLaser = false
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🛡️ قياس الخطر التكتيكي المعاير (TACTICAL RISK HUD)",
                fontSize = 10.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sizePx = size.width
                    val arcRadius = sizePx / 2f
                    val strokeWidthPx = 10.dp.toPx()
                    
                    // Background Arc
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(strokeWidthPx/2f, strokeWidthPx/2f),
                        size = androidx.compose.ui.geometry.Size(sizePx - strokeWidthPx, sizePx - strokeWidthPx),
                        style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Danger Value Arc
                    val sweepAngle = (animatedRisk / 100f) * 270f
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidthPx/2f, strokeWidthPx/2f),
                        size = androidx.compose.ui.geometry.Size(sizePx - strokeWidthPx, sizePx - strokeWidthPx),
                        style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Dial needle
                    val needleAngleRad = Math.toRadians((135f + sweepAngle).toDouble())
                    val needleLength = arcRadius - strokeWidthPx - 10.dp.toPx()
                    val centerPt = Offset(arcRadius, arcRadius)
                    val endPt = Offset(
                        (centerPt.x + needleLength * Math.cos(needleAngleRad)).toFloat(),
                        (centerPt.y + needleLength * Math.sin(needleAngleRad)).toFloat()
                    )
                    
                    drawLine(
                        color = color,
                        start = centerPt,
                        end = endPt,
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    drawCircle(
                        color = Color.Black,
                        radius = 6.dp.toPx(),
                        center = centerPt
                    )
                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = centerPt
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 45.dp)
                ) {
                    Text(
                        text = "${animatedRisk.roundToInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("THREAT LEVEL", fontSize = 7.sp, color = TextSecondary)
                }
            }
            
            val threatDesc = when {
                animatedRisk > 70f -> "خطر حرج - ساحة معركة مهددة ونشطة 🚨"
                animatedRisk > 40f -> "خطر متوسط - اشتباك أو تلويث قائم ⚠️"
                else -> "طبيعي - تراجع في التهديدات والنشاط المعادي"
            }
            Text(
                text = threatDesc,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MilitaryCaseStudiesTab(playBeep: (Int) -> Unit) {
    val activeCombatCases = remember {
        listOf(
            CombatCaseStudy(
                id = "CS_01",
                title = "محاكاة إسعاف تحت النيران (Care Under Fire)",
                category = "إسعاف تكتيكي ناري (TCCC)",
                background = "يتعرض فصيل مقاتل لقصف ناري مباشر. تلوح لك في الأفق إصابة مقاتل بنزيف فخذي شرياني حاد (نزيف فخذي شرياني حاد) والعدو على بعد 150 متراً تحت النيران الكثيفة.",
                startNodeId = "CS1_START",
                nodes = mapOf(
                    "CS1_START" to CaseNode(
                        id = "CS1_START",
                        prompt = "المصنع الناري نشط والعدو يطلق النار مباشرة. ما هي خطوتك الإسعافية الطبية الأولى الأنسب فوراً؟",
                        dangerDelta = 15,
                        stabilityDelta = -5,
                        options = listOf(
                            CaseOption(
                                label = "طالب الجندي المصاب بالتغطية النارية الذاتية وتطبيق عاصبة الفخذ سريعاً (Self-Tourniquet).",
                                feedback = "ممتاز ومطابق لبروتوكول TCCC تماماً! العدو مستمر في إطلاق النار، وإيقاظ المصاب للإنقاذ الذاتي هو الأفضل.",
                                targetNodeId = "CS1_PHASE2",
                                msg = "تطبيق عاصبة الشريان بنجاح."
                            ),
                            CaseOption(
                                label = "الاندفاع التلقائي والركض نحوه لتضميد الجرح بلفافات شاش معقمة بوسط الميدان المفتوح.",
                                feedback = "فشل ذريع بمبادئ أمن العمليات! الاندفاع دون غطاء ناري سيعرّض المسعف للإصابة المباشرة ونزيف إضافي.",
                                targetNodeId = null,
                                isFatal = true,
                                msg = "مقتل المسعف والمصاب تحت النيران الكثيفة."
                            ),
                            CaseOption(
                                label = "البدء الفوري بفتح وريد ورش سوائل ملحية في يده بالمنطقة الخطرة.",
                                feedback = "عقد جراحي غير مناسب كلياً! إضاعة الوقت الثمين بفتح وريد تحت زخات الرصاص يؤدي لنفاذ دم المصاب بشكل كامل.",
                                targetNodeId = null,
                                isFatal = true,
                                msg = "تصفية المصاب بالكامل وموته بنزيف حاد."
                            )
                        )
                    ),
                    "CS1_PHASE2" to CaseNode(
                        id = "CS1_PHASE2",
                        prompt = "نجح المصاب في الاختباء خلف مدرعة تالفة ومفرز عاصبته بقوة. النزيف متوقف لكن الرئة مصابة بضيق صدر وارتفاع نبضه لـ 140نبضة/د. ما الإجراء التالي؟",
                        dangerDelta = -10,
                        stabilityDelta = 25,
                        options = listOf(
                            CaseOption(
                                label = "تطبيق إبرة تفريغ الضغط الصدري في الفراغ الضلعي الثاني فوراً (Needle Decompression).",
                                feedback = "رائع ومحترف! إنقاذ بطل من إعاقة اختناق الرئة وتجمع الغاز الرئوي المكبوت.",
                                targetNodeId = "CS1_SUCCESS",
                                isVictory = true,
                                msg = "تخفيف تجمع غاز الصدر واستقرار المريض كلياً."
                            ),
                            CaseOption(
                                label = "إعطاء المريض حائل تسكين مركزي حاد (جرعة فنتانيل 800 ميكرو فموياً).",
                                feedback = "تحذير خطير! السكنات القوية تعيق من الوعي العصبي والقياس وتسرع من فرط الفشل التنفسي.",
                                targetNodeId = null,
                                isFatal = true,
                                msg = "توقف تنفسي كامل للمصاب وموته."
                            )
                        )
                    )
                )
            ),
            CombatCaseStudy(
                id = "CS_02",
                title = "محاكاة الكيماوي لغاز الإنبار (CBRN Gas Threat)",
                category = "التطهير والوقاية الكيميائية",
                background = "تم العثور على امرأة فاقدة للوعي تماماً داخل خندق تكتيكي ملوث بغاز الإنبار / السارين الكيميائي السام. يظهر تضيق حدقة شديد وسيلان مخاطي وضيق شعبي حرج.",
                startNodeId = "CS2_START",
                nodes = mapOf(
                    "CS2_START" to CaseNode(
                        id = "CS2_START",
                        prompt = "الخندق ما زال ملوثاً بالغاز المبخر الكيماوي. كيف تبدأ استجابتك العلاجية وحماية الكادر الطبي؟",
                        dangerDelta = 20,
                        stabilityDelta = -10,
                        options = listOf(
                            CaseOption(
                                label = "ارتداء التجهيز الفردي الوقائي الكامل من الدرجة الرابعة (PPE Class 4 + Gas Mask) قبل الاقتراب.",
                                feedback = "إجراء وقائي سليم تماماً! الحفاظ على الذات قبل كل شيء لضمان ديمومة الرعاية التكتيكية للمصابين.",
                                targetNodeId = "CS2_PHASE2",
                                msg = "ارتداء البدلة الكيماوية والاقتراب الآمن للإنقاذ."
                            ),
                            CaseOption(
                                label = "الاندفاع وسحب المصابة فوراً لوضع قناع تنفس عادي على وجهها دون ارتداء بدلتك الخاصة.",
                                feedback = "كارثة! تنفس المسعف الفوري لأبخرة السارين العالقة بالخندق يؤدي لشلل عضلاته التنفسية فوراً.",
                                targetNodeId = null,
                                isFatal = true,
                                msg = "شلل كامل للمسعف ووفاته كيميائياً ملوثاً."
                            )
                        )
                    ),
                    "CS2_PHASE2" to CaseNode(
                        id = "CS2_PHASE2",
                        prompt = "تم إخلاء المصابة لخيمة التطهير الميدانية الجافة. ما هي خطوتك الإسعافية الطبية الطارئة التالية لإلغاء السمية؟",
                        dangerDelta = -15,
                        stabilityDelta = 30,
                        options = listOf(
                            CaseOption(
                                label = "قص وإزالة كافة الملابس والتطهير بمسحوق جاف ثم حقن الأتروبين الثنائي والتوكسوجونين.",
                                feedback = "علاج نوعي بطل! التخلص السريع من الملابس يزيل 90% من السموم الملتصقة ومضادات المجهد (الأتروبين) تعالج السارين.",
                                targetNodeId = "CS2_SUCCESS",
                                isVictory = true,
                                msg = "مضادة السمية بالأتروبين والإنعاش بنجاح بامتياز."
                            ),
                            CaseOption(
                                label = "تغسيل الجسد مباشرة بالماء الغزير دون خلع أو قص الملابس الملوثة أولاً.",
                                feedback = "خطأ دفاعي تكتيكي! غسيل الملابس المليئة بالغاز الساخن يوزع وينفذ السموم بعمق أكبر لخلايا الجلد.",
                                targetNodeId = null,
                                isFatal = true,
                                msg = "انتشار ممتد للملوث للطبقات الداخلية ووفاة دماغية."
                            )
                        )
                    )
                )
            )
        )
    }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tactical_decision_log", Context.MODE_PRIVATE) }
    
    var selectedCaseIdx by remember { mutableStateOf(0) }
    val currentCase = activeCombatCases[selectedCaseIdx]

    // States managing user's progression inside selected active case study
    var currentNodeId by remember { mutableStateOf(currentCase.startNodeId) }
    val currentNode = currentCase.nodes[currentNodeId]

    var healthStability by remember { mutableStateOf(50) } 
    var tacticalDangerMeter by remember { mutableStateOf(45) } 
    var caseReportFeedback by remember { mutableStateOf("") }
    
    // Persistent decisions log loaded from SharedPreferences
    var stepLogHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val saved = sharedPrefs.getString("decisions_list", "") ?: ""
        if (saved.isNotEmpty()) {
            stepLogHistory = saved.split("||")
        } else {
            stepLogHistory = listOf("تأمين مصفوفة السجل: نظام التدابير التكتيكي الدفاعي نشط.")
        }
    }

    val saveLogHistory = { newList: List<String> ->
        stepLogHistory = newList
        sharedPrefs.edit().putString("decisions_list", newList.joinToString("||")).apply()
    }

    // Interactive Slider State variables to calculate scenario dynamic factors
    var fireIntensity by remember { mutableStateOf(50f) }
    var windSpeed by remember { mutableStateOf(40f) }
    var armorProtection by remember { mutableStateOf(60f) }

    // Dynamically calculate risk based on sliders and current case state
    val computedRiskLevel = remember(fireIntensity, windSpeed, armorProtection, tacticalDangerMeter) {
        val baseFactor = (fireIntensity * 0.4f) + (windSpeed * 0.25f) - (armorProtection * 0.35f) + (tacticalDangerMeter * 0.7f)
        baseFactor.coerceIn(5f, 100f)
    }

    // Reset status when changing case studies
    LaunchedEffect(selectedCaseIdx) {
        currentNodeId = currentCase.startNodeId
        healthStability = 50
        tacticalDangerMeter = 45
        caseReportFeedback = ""
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Selection of dynamic combat studies (Staggered Item #0)
        item {
            StaggeredItem(index = 0) {
                Column {
                    Text(
                        text = "🛡️ اختر دراسة حالة عسكرية تكتيكية للبدء:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeCombatCases.forEachIndexed { idx, caseStudy ->
                            val isThisActive = (selectedCaseIdx == idx)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isThisActive) TextGold else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isThisActive) TextGold else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable { selectedCaseIdx = idx }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = caseStudy.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isThisActive) Color.Black else TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live HUD Metrics (Staggered Item #1) - Cyber Vital Dashboard & Combat Risk Indicator
        item {
            StaggeredItem(index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Twin Gauges: Risk Indicator and ECG Cyber Dashboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CombatRiskIndicator(riskLevel = computedRiskLevel)
                        }
                        Box(modifier = Modifier.weight(1.1f)) {
                            CyberVitalDashboard(
                                stability = healthStability,
                                isFatal = (currentNodeId.isEmpty() && caseReportFeedback.contains("🚫")),
                                isVictory = (currentNodeId.isEmpty() && caseReportFeedback.contains("🏆"))
                            )
                        }
                    }
                }
            }
        }

        // Interactive Threat Modulators Panel (Staggered Item #2)
        item {
            StaggeredItem(index = 2) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    enablePersistentLaser = false
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⚙️ لوحة ضبط وتعديل عوامل الميدان (THREAT FACTORS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Fire Intensity Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("كثافة نيران العدو: ${fireIntensity.roundToInt()}%", fontSize = 8.5.sp, color = TextPrimary, modifier = Modifier.width(130.dp))
                            Slider(
                                value = fireIntensity,
                                onValueChange = { fireIntensity = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = TextGold, activeTrackColor = TextGold)
                            )
                        }

                        // Wind Speed Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("سرعة الرياح وتشتت الغاز: ${windSpeed.roundToInt()}%", fontSize = 8.5.sp, color = TextPrimary, modifier = Modifier.width(130.dp))
                            Slider(
                                value = windSpeed,
                                onValueChange = { windSpeed = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = TextGold, activeTrackColor = TextGold)
                            )
                        }

                        // PPE Armor Level Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("جاهزية دروع Medic PPE: ${armorProtection.roundToInt()}%", fontSize = 8.5.sp, color = TextPrimary, modifier = Modifier.width(130.dp))
                            Slider(
                                value = armorProtection,
                                onValueChange = { armorProtection = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = TextGold, activeTrackColor = TextGold)
                            )
                        }
                    }
                }
            }
        }

        // Selected case study context (Staggered Item #3)
        item {
            StaggeredItem(index = 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030814).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TextGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(currentCase.category, fontSize = 9.5.sp, color = TextGold, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentCase.background,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Active prompt / Question step dialog (Staggered Item #4)
        if (currentNode != null) {
            item {
                StaggeredItem(index = 4) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            TerminalText(
                                text = "❓ القرار المطلوب: ${currentNode.prompt}",
                                style = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
                                color = TextGold,
                                speedMs = 15
                            )
                        }

                        // Options list
                        currentNode.options.forEachIndexed { optIndex, option ->
                            Button(
                                onClick = {
                                    playBeep(if (option.isFatal) ToneGenerator.TONE_SUP_ERROR else ToneGenerator.TONE_PROP_BEEP)
                                    tacticalDangerMeter = (tacticalDangerMeter + currentNode.dangerDelta).coerceIn(0, 100)
                                    healthStability = (healthStability + currentNode.stabilityDelta).coerceIn(10, 100)

                                    val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                    val actionText = "[${stamp}] ${currentCase.title.substringBefore("(").trim()} -> تم اختيار: ${option.label.substringBefore(".")}"
                                    val logMsg = if (option.isFatal) "$actionText (وفاة ❌)" else if (option.isVictory) "$actionText (نجاح 🏆)" else "$actionText (استقرار 🔸)"
                                    
                                    val history = stepLogHistory + logMsg
                                    saveLogHistory(history)

                                    if (option.isFatal) {
                                        caseReportFeedback = "🚫 خيار خاطئ تكتيكياً! ${option.feedback} • النتيجة: ${option.msg}"
                                        currentNodeId = ""
                                    } else if (option.isVictory) {
                                        caseReportFeedback = "🏆 نصر وتطهير تكتيكي رائع! ${option.feedback} • النتيجة: ${option.msg}"
                                        currentNodeId = ""
                                    } else {
                                        if (option.targetNodeId != null) {
                                            currentNodeId = option.targetNodeId
                                        } else {
                                            caseReportFeedback = "تم إنهاء التقدم بنتيجة عادية: ${option.feedback}"
                                            currentNodeId = ""
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(InvoluntaryWebConstants.ButtonHeight)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(TextGold.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${optIndex + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = option.label,
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Result presentation & Play-again controller (Staggered Item #5)
        if (caseReportFeedback.isNotEmpty()) {
            item {
                StaggeredItem(index = 5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (caseReportFeedback.contains("🏆")) Color(0x222ECC71) else Color(0x22E74C3C))
                            .border(1.dp, if (caseReportFeedback.contains("🏆")) Color(0xFF2ECC71).copy(alpha = 0.4f) else Color(0xFFE74C3C).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = if (caseReportFeedback.contains("🏆")) "🏆 تم استقرار حالة المريض الكيماوية/الرئوية وإنقاذه:" else "⚠️ فشل تكتيكي طبي بالميدان:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (caseReportFeedback.contains("🏆")) Color(0xFF2ECC71) else TextOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = caseReportFeedback,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    currentNodeId = currentCase.startNodeId
                                    healthStability = 50
                                    tacticalDangerMeter = 45
                                    caseReportFeedback = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TextGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إعادة تصفير وبدء دراسة الحالة مجدداً 🔄", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Persistent After Action Decision Log (Staggered Item #6)
        item {
            StaggeredItem(index = 6) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 سجل التدابير والقرارات التاريخية التراكمية (AAR LOG)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(
                            text = "مسح وقائع السجل 🗑️",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextOrange,
                            modifier = Modifier.clickable {
                                playBeep(ToneGenerator.TONE_SUP_ERROR)
                                saveLogHistory(listOf("تم تصفير السجل التكتيكي العام وقاعدة البيانات بنجاح."))
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(stepLogHistory.asReversed()) { h ->
                                Text("▪️ $h", fontSize = 8.5.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================
// Tab 3: VR Glasses Linking & 3D Interactive Medical Simulator
// ====================================================
@Composable
fun VRHeadsetsAnd3DDevicesTab(
    vrHeadsets: List<VRHeadset>,
    isVRConnected: Boolean,
    isVRConnecting: Boolean,
    connectedHeadsetName: String,
    vrStatusDetails: String,
    rotationX: Float,
    rotationY: Float,
    flowRate: Float,
    pressureThreshold: Float,
    o2MixPct: Float,
    onUpdateRotation: (Float, Float) -> Unit,
    onUpdateFlow: (Float) -> Unit,
    onUpdatePressure: (Float) -> Unit,
    onUpdateO2Pct: (Float) -> Unit,
    onConnectHeadset: (String) -> Unit,
    onDisconnectHeadset: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Pairing VR/AR Controller
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🥽", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("صندوق ربط وتوافق نظارات الواقع الافتراضي VR:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                            Text("مزامنة واجهة التطبيق وغرف العمليات ثلاثية الأبعاد", fontSize = 9.5.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    vrHeadsets.forEach { headset ->
                        val isThisActive = (isVRConnected && connectedHeadsetName == headset.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isThisActive) Color(0x22D4AF37) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (isThisActive) TextGold else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(headset.signalIcon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(headset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isThisActive) TextGold else TextPrimary)
                                Text("${headset.type} • ${headset.details}", fontSize = 9.sp, color = TextSecondary)
                            }

                            if (isThisActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2ECC71))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("متصلة ⬤", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onConnectHeadset(headset.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, Secondary),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (isVRConnecting) "جاري الربط..." else "ربط 🔄", fontSize = 9.sp, color = TextGold)
                                }
                            }
                        }
                    }

                    // Stat panel below headsets list
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("📊 تليمتري تتبع الإضاءة وحزم البلوتوث (Pair Telemetry):", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                            Text(vrStatusDetails, fontSize = 9.sp, color = TextPrimary, modifier = Modifier.padding(top = 2.dp))
                            if (isVRConnected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("قناة الإرسال: BLE عسكري مشفرة", fontSize = 8.sp, color = TextSecondary)
                                    Text("الاتصال: ممتاز (100%)", fontSize = 8.sp, color = Color(0xFF2ECC71))
                                    Text("إلغاء الربط ❌", fontSize = 8.5.sp, color = TextOrange, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDisconnectHeadset() })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive 3D Medical device Rendering Canvas (Orthographic projection vector calculation)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("📐 محاكاة ثلاثية الأبعاد لأجهزة التنفس الصناعي والمراقب الميداني (3D Device Projection):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGold)
                    Text("اسحب بإصبعك على منطقة المعاينة لتدوير الجهاز الطبي 3D المعروض في الفضاء الافتراضي", fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))

                    // Canvas Viewport with touch gestures to rotate 3D representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Translate mouse/slide drag to 3D rotation angles
                                    onUpdateRotation(dragAmount.x * 0.45f, dragAmount.y * 0.45f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val center = Offset(w / 2, h / 2)

                            // Define 8 corners of a 3D rectangular box representing medical ventilator cabinet
                            val sizeBox = 45.dp.toPx()
                            val rawVertices = listOf(
                                // Front face
                                floatArrayOf(-sizeBox, -sizeBox * 1.3f, -sizeBox), // 0
                                floatArrayOf(sizeBox, -sizeBox * 1.3f, -sizeBox),  // 1
                                floatArrayOf(sizeBox, sizeBox * 1.3f, -sizeBox),   // 2
                                floatArrayOf(-sizeBox, sizeBox * 1.3f, -sizeBox),  // 3
                                // Back face
                                floatArrayOf(-sizeBox, -sizeBox * 1.3f, sizeBox),  // 4
                                floatArrayOf(sizeBox, -sizeBox * 1.3f, sizeBox),   // 5
                                floatArrayOf(sizeBox, sizeBox * 1.3f, sizeBox),    // 6
                                floatArrayOf(-sizeBox, sizeBox * 1.3f, sizeBox)    // 7
                            )

                            // Convert rotation degrees to Radians
                            val radX = Math.toRadians(rotationX.toDouble())
                            val radY = Math.toRadians(rotationY.toDouble())

                            // Perform 3D coordinate rotation transformation
                            val projected2D = rawVertices.map { vertex ->
                                val xRaw = vertex[0]
                                val yRaw = vertex[1]
                                val zRaw = vertex[2]

                                // Rotate Y
                                val xRot1 = xRaw * cos(radY) - zRaw * sin(radY)
                                val zRot1 = xRaw * sin(radY) + zRaw * cos(radY)

                                // Rotate X
                                val yRot2 = yRaw * cos(radX) - zRot1 * sin(radX)
                                val zRot2 = yRaw * sin(radX) + zRot1 * cos(radX)

                                // simple orthographic perspective multiplier depending on Z-depth
                                val scale = (zRot2 + 250f) / 250f
                                Offset(
                                    (center.x + xRot1 * scale).toFloat(),
                                    (center.y + yRot2 * scale).toFloat()
                                )
                            }

                            // Render 3D back-graticule concentric circular HUD
                            drawCircle(
                                color = TextGold.copy(alpha = 0.04f),
                                radius = 60.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1f)
                            )
                            drawCircle(
                                color = TextGold.copy(alpha = 0.07f),
                                radius = 45.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1f)
                            )

                            // Draw 12 edges of the 3D cube chassis representing respirator
                            val edges = listOf(
                                Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0), // Front face
                                Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4), // Back face
                                Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7)  // Connections
                            )

                            edges.forEach { edge ->
                                drawLine(
                                    color = TextGold.copy(alpha = 0.35f),
                                    start = projected2D[edge.first],
                                    end = projected2D[edge.second],
                                    strokeWidth = 1.3.dp.toPx()
                                )
                            }

                            // Draw LCD monitor inside front face (Between vertices 0, 1, 2, 3)
                            // Draw an inner smaller neon green wave inside the projected face representing ventilation pressure
                            val wavePath = Path()
                            val screenPoints = listOf(0, 1)
                            val startP = projected2D[0]
                            val endP = projected2D[1]

                            wavePath.moveTo(startP.x, startP.y)
                            val segments = 20
                            for (i in 0..segments) {
                                val t = i / segments.toFloat()
                                val xCoord = startP.x + t * (endP.x - startP.x)
                                val yCoord = startP.y + t * (endP.y - startP.y)

                                // add sine wave ripple to simulate live EEG/Ventilative respiratory graphs adjusted by flow rate variables
                                val freq = 2.5 + (flowRate / 10f)
                                val amplitude = 8.dp.toPx() * (o2MixPct / 100f)
                                val waveOffset = Math.sin(t * freq * Math.PI * 2) * amplitude
                                wavePath.lineTo(xCoord, (yCoord + waveOffset).toFloat())
                            }

                            drawPath(
                                path = wavePath,
                                color = Color(0xFF2ECC71),
                                style = Stroke(width = 1.6.dp.toPx())
                            )

                            // Draw virtual connection ports / knobs
                            drawCircle(color = TextOrange, radius = 3.dp.toPx(), center = projected2D[2])
                            drawCircle(color = Color(0xFF3498DB), radius = 3.dp.toPx(), center = projected2D[3])
                        }

                        // Floating coordinate badge inside HUD
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Rotation X: ${rotationY.toInt()}° Y: ${rotationX.toInt()}°",
                                fontSize = 8.sp,
                                color = TextGold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Knobs and Controls that interactively update projection wave
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("⚙️ لوحة ضبط وتخصيص المؤشرات الافتراضية للجهاز التنفسي:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Flow rate slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("معدل دفق الغاز (Flow Rate):", fontSize = 9.5.sp, color = TextPrimary)
                            Text("${flowRate.toInt()} لتر/دقيقة", fontSize = 10.sp, color = TextGold, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = flowRate,
                            onValueChange = onUpdateFlow,
                            valueRange = 5f..25f,
                            colors = SliderDefaults.colors(thumbColor = TextGold, activeTrackColor = TextGold)
                        )
                    }

                    // Oxygen mix slider
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نسبة تركيز الأكسجين (O2 percentage):", fontSize = 9.5.sp, color = TextPrimary)
                            Text("${o2MixPct.toInt()}%", fontSize = 10.sp, color = TextGold, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = o2MixPct,
                            onValueChange = onUpdateO2Pct,
                            valueRange = 21f..100f,
                            colors = SliderDefaults.colors(thumbColor = TextGold, activeTrackColor = TextGold)
                        )
                    }

                    // VR head transmission trigger button
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!isVRConnected) {
                                Toast.makeText(context, "الرجاء توصيل ومزامنة أحد نظارات الـ VR بالأعلى أولاً! 🥽", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "جاري إرسال حزم المحاكاة ثلاثية الأبعاد لنظارة الـ BR بنجاح! 🦾", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isVRConnected) Color(0xFF2ECC71) else Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isVRConnected) "بث المحاكاة كبث ثلاثي الأبعاد فعال ⚡" else "يرجى ربط نظارة VR لتفعيل البث ثلاثي الأبعاد 🥽",
                            color = if (isVRConnected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3D Clinical Intubation Simulator (ألعاب محاكاة التنبيب الرغامي الافتراضي) - Drag, Lift Epiglottis and secure tube physics
        item {
            IntubationSimulator(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ====================================================
// Tab 4: Tactical Tools & Generative Suite (🧪 معمل الأدوات التكتيكية)
// ====================================================
@Composable
fun TacticalToolsTab(playBeep: (Int) -> Unit) {
    val context = LocalContext.current
    var selectedAgentKey by remember { mutableStateOf("sarin") }
    var userSelectedDoseIdx by remember { mutableStateOf(-1) }
    var doseAnswerFeedback by remember { mutableStateOf("") }

    // Anatomy Hotspot State
    var selectedHotspotId by remember { mutableStateOf<String?>(null) }
    var appliedInterventionResponse by remember { mutableStateOf("") }

    // Briefing Terminal Mode State
    var selectedDocIdx by remember { mutableStateOf(0) }
    var isBriefingGenerating by remember { mutableStateOf(false) }
    var visibleBriefingLines by remember { mutableStateOf<List<String>>(emptyList()) }

    // Anatomy 3D Rotation State
    var anatomyRotationX by remember { mutableStateOf(0f) }
    var anatomyRotationY by remember { mutableStateOf(0f) }
    var activeAnatomySystem by remember { mutableStateOf("all") } // "all", "skeleton", "muscles", "organs", "nervous"

    val agents = remember {
        listOf(
            HazardAgent(
                key = "sarin",
                name = "🧪 غاز السارين للأعصاب (Sarin GB)",
                latinName = "Isopropoxymethylphosphoryl fluoride",
                levelCode = "CBRN-Level A (Critical)",
                description = "سائل غازي لا طعم له ولا رائحة، يعطل إنزيم الأسيتيل كولينستريز مسبباً تشنجات عضلية حادة وفشل تنفسي سريع للغاية.",
                pulseSpeed = 600,
                antidoteName = "حقن الأتروبين الفوري (Atropine) + البراليدوكسيم (Pralidoxime)",
                idealDosePrompt = "ما هي الجرعة الإسعافية التكتيكية للأتروبين الموصى بها كجلسة أولى لإنقاذ المصاب المنقبض حدقته؟",
                correctDoseIdx = 2,
                doseOptions = listOf("0.5 ملغ عضل", "1 ملغ وريدي بطيء", "2 ملغ حقناً بالجرعة الفولاذية السريعة", "10 ملغ قطرات"),
                ppeRequired = "بدلة الوقاية الكاملة من المستوى A مع قناع منفاخ تنفسي مغلق صلب SCBA.",
                tacticalSteps = listOf(
                    "إخلاء المصاب ووضعه في مسار معاكس لاتجاه الرياح الملوثة فوراً.",
                    "تطهير جاف جاد للبشرة والوجه باستخدام مستحضرات البودرة الممتصة التكتيكية.",
                    "حقن الحقنة التلقائية المزدوجة للأعصاب (Atropine-Oxime Auto-injector).",
                    "صيانة ودعم مجرى الهواء الصدري تحت التهوية الميكانيكية بمخرجات ضغط منخفض."
                )
            ),
            HazardAgent(
                key = "vx",
                name = "☠️ غاز الأعصاب الأشد فتكاً (VX Agent)",
                latinName = "S-[2-(diisopropylamino)ethyl] O-ethyl methylphosphonothioate",
                levelCode = "CBRN-Level A+ (Fatal Skin Kontakt)",
                description = "مركب زيتي شديد اللزوجة والسمية عبر الجلد. تبخره بطيء ولكنه يدوم في الموقع لأيام، ويسبب الموت الفوري خلال دقائق.",
                pulseSpeed = 400,
                antidoteName = "مزيج الأتروبين والبراليدوكسيم وجرعات البنزوديازيبين (Diazepam)",
                idealDosePrompt = "ما هي عتبة الوقاية المصاحبة لمركب VX العضلي لمنع تشنجات الفك والبلع؟",
                correctDoseIdx = 1,
                doseOptions = listOf("المورفين 5 ملغ", "الأتروبين 4-6 ملغ مع الديازيبام 10 ملغ عضل", "الأكسجين الجاف فقط", "غسيل جاف بالماء المقطر"),
                ppeRequired = "درع غشاء وقائي مضاد للسوائل العضوية الكثيفة مع طبقات كربون مفعلة جافة.",
                tacticalSteps = listOf(
                    "التطهير المباشر الموضعي باستخدام مستحضر M291 التكتيكي الجاف لإزالة النفط الكيميائي.",
                    "الحقن الفوري للديازيبام عضل كعامل حماية للخلايا الدماغية والقلبية من التشنج.",
                    "دعم الأكسجين المستمر بنسبة 100% لتجنب انقطاع التهوية الانقباضية.",
                    "نقل المصاب في غطاء بولي-كربوني معزول عن باقي الطاقم لتجنب انتقال التبخر الثانوي."
                )
            ),
            HazardAgent(
                key = "chlorine",
                name = "🌬️ غاز الكلور الخانق والتدميري (Chlorine Gas)",
                latinName = "Diatomic Chlorine Cl2",
                levelCode = "CBRN-Level B (Respiratory)",
                description = "غاز ذو لون مخضر ورائحة نفاذة يدمر الأنسجة الرئوية الرطبة مكوناً حمض الهيدروكلوريك وحمض الهيبوكلوروز الحارقين.",
                pulseSpeed = 800,
                antidoteName = "الأكسجين المرطب الرطب وبخاخات بيكربونات الصوديوم المعايرة لدرجة الحموضة",
                idealDosePrompt = "ما هو الإجراء الصدري الطبي المعاكس للأحماض الكلورية في الأنسجة التنفسية العلوية؟",
                correctDoseIdx = 0,
                doseOptions = listOf("ترطيب الأكسجين بنبيب 4% بيكربونات صوديوم بخاخ", "الحقن الوقائي الفوري بالفنتانيل المهدئ", "صدمة كهربائية وقائية", "حقن الأتروبين عضل"),
                ppeRequired = "قناع واقي للوجه بالكامل من فئة CBRN مع مرشح جسيمات غازية مسال.",
                tacticalSteps = listOf(
                    "سحب جندي الاصابة للأماكن المرتفعة الطبيعية حيث أن غاز الكلور أثقل من الهواء ويترسب في الخنادق والمنخفضات.",
                    "تقديم بخاخات بيكربونات الصوديوم 4% لتخفيف حموضة الرئتين.",
                    "تجنب الإنعاش بالفم كلياً والاعتماد على قناع التنفس بالصمام اليدوي الأحادي.",
                    "العلاج السريع للتشنج القصبي باستخدام محفزات بيتا 2 مثل السالبيوتامول."
                )
            ),
            HazardAgent(
                key = "cyanide",
                name = "🍒 غاز السيانيد القاتل الخلوي (Cyanide)",
                latinName = "Hydrogen Cyanide HCN / Cyanogen Chloride",
                levelCode = "CBRN-Level A (Mitochondrial Arrest)",
                description = "يستهدف الميتوكوندريا ويعطل إنزيم الأكسيداز السيتوكرومي، مانعاً الخلايا من استغلال الأكسجين ومسبباً اختناقاً خلوياً مميتاً.",
                pulseSpeed = 450,
                antidoteName = "الهيدروكسوكوبالامين (Cyanokit) عالي السعة IV أو ثيوسلفات الصوديوم",
                idealDosePrompt = "ما هو الترياق التكتيكي الأسرع المعطل لروابط السيانيد الخلوية بالدم؟",
                correctDoseIdx = 3,
                doseOptions = listOf("المصل المالح العادي 500 مل", "الأتروبين المزدوج 2 ملغ", "بخاخ الفنتولين الرطب", "الهيدروكسوكوبالامين (Cyanokit) 5 غرام وريدي بطيء"),
                ppeRequired = "قناع وجه تكتيكي مع مرشح حابس لأبخرة حمض هيدروسيانيك المزدوجة.",
                tacticalSteps = listOf(
                    "حقن ترياق السيانوكيت (Hydroxocobalamin) وريدياً بشكل بطيء على مدى 15 دقيقة.",
                    "يقوم الترياق بربط جزيئات السيانيد في الدم لتشكيل فيتامين B12a غير السام الذي يخرج مع البول.",
                    "إذا لم يستجب المصاب، تكرر جرعة 5 غرام إضافية تدريجياً وبتحفظ شديد.",
                    "تقديم تهوية مستمرة بأكسجين عالي الضغط والتركيز."
                )
            )
        )
    }

    val selectedAgent = remember(selectedAgentKey) {
        agents.find { it.key == selectedAgentKey } ?: agents[0]
    }

    // Agent Changing Trigger resets
    LaunchedEffect(selectedAgentKey) {
        userSelectedDoseIdx = -1
        doseAnswerFeedback = ""
    }

    // Pulse animation of hazard alert glow
    val infiniteTransition = rememberInfiniteTransition(label = "hazard_pulse")
    val hazardPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(selectedAgent.pulseSpeed, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazard_pulse_alpha"
    )

    // Anatomy Hotspot nodes configuration
    val hotspots = remember {
        listOf(
            AnatomyHotspot(
                id = "brain",
                arName = "🧠 الرأس والوعي العصبي",
                engName = "Traumatic Brain & Gas Exposure",
                xPct = 0.5f,
                yPct = 0.12f,
                x3d = 0f,
                y3d = 72f,
                z3d = 0f,
                symptoms = "تشنجات بؤرية حادة، حدقة عين رأس دبوسية منقبضة (Miosis)، رتة كلامية وغيبوبة ارتدادية حادة.",
                safetyStatus = "غير مستقر - تلف خلوي بالدماغ ⚠️",
                urgentIntervention = "إدارة تأمين المجرى التنفسي وتثبيت الرأس، ومنع حدوث انخفاض حاد في إشباع الأكسجين بنسبة تحت %90.",
                fieldKit = "طقم مجرى الهواء التكتيكي المتقدم (Tactical Airways Trauma Kit v2)",
                surgicalSteps = listOf(
                    "تطهير تجويف البلعوم الفوري باستخدام الشفاط اليدوي التكتيكي لإزالة المفرزات اللعابية الكثيفة.",
                    "إدخال الأنبوب البلعومي الأنفي (NPA - Nasopharyngeal Airway) المشحم بحذر لتوفير مجرى بديل.",
                    "في حال تشنج الفك الكامل، إجراء بضع الغشاء الحلقي والدرقي الجراحي الطارئ (Surgical Cricothyroidotomy)."
                ),
                procedure = "تطبيق حماية مجرى الهواء المستقر، التحكم بالتشنجات العضلية العنيفة باستخدام الديازيبام المانع، وتجنب غيبوبة نقص الأكسجين.",
                tools = listOf("حقنة مخدر ديازيبام تكتيكية", "نبيطة فتح الفم التكتيكية (Bite Block)", "مصدر أكسجين متنقل"),
                triageClass = "أحمر (حرج - إسعاف فوري)",
                triageColor = TextOrange
            ),
            AnatomyHotspot(
                id = "lungs",
                arName = "🫁 الصدر والرئتين والمنافس",
                engName = "Respiratory & Tension Pneumothorax",
                xPct = 0.5f,
                yPct = 0.32f,
                x3d = 0f,
                y3d = 34f,
                z3d = 4f,
                symptoms = "زراق شديد على الصدر والجلد باللون الأزرق، اختناق وصعوبة بالتنفس، انحراف الرغامي وانعدام الهواء بالرئة اليسرى.",
                safetyStatus = "حرج للغاية - انخماص رئوي كامل 🚨",
                urgentIntervention = "تخفيف الضغط الصدري فورا باستخدام الإبرة الميدانية لمنع توقف القلب المفاجئ نتيجة الضغط الهوائي.",
                fieldKit = "طقم جراحة الجوف الصدري المعقم (Combat Thoracostomy System CSS)",
                surgicalSteps = listOf(
                    "فحص وتحديد الفراغ الضلعي الثاني في خط منتصف الترقوة للجهة المصابة بالـ Pneumothorax.",
                    "بتر معتدل وتطهير الجلد، ثم إدخال إبرة فك الضغط (TCD Needle 14G) بزاوية 90 درجة مع سماع صفير تسرب الهواء الحبيس.",
                    "تطبيق صمام الصدر أحادي الاتجاه اللاصق (Vented Chest Seal) لمنع عودة دخول الهواء الملوث."
                ),
                procedure = "إجراء التنفيس الصدري السريع بإبرة فك ضغط الصدر (Needle Decompression) في الضلع الثاني فوراً لمنع انهيار القلب.",
                tools = listOf("إبرة فك ضغط الصدر عيار 14G", "صمام صدر لاصق ذو اتجاه واحد (Vented Chest Seal)", "قناع التنفس ذو الصمام المغلق"),
                triageClass = "أحمر (حرج - إسعاف فوري)",
                triageColor = TextOrange
            ),
            AnatomyHotspot(
                id = "heart",
                arName = "🫀 القلب ومؤشرات النبض التكتيكية",
                engName = "Cardiac & Hemodynamic Support",
                xPct = 0.44f,
                yPct = 0.42f,
                x3d = -10f,
                y3d = 26f,
                z3d = 8f,
                symptoms = "تباطؤ حاد وشديد في ضربات القلب (Bradycardia) تحت 40 نبضة/د مع انخفاض حرج بضغط الدم وسقوط الدوران الدموي.",
                safetyStatus = "حرج - هبوط حاد بالتروية المحيطية 📉",
                urgentIntervention = "استعادة النبض والتروية الدموية بحقن موسعات الأوعية والمحفزات، والتعامل السريع مع علامات الصدمة القاتلة.",
                fieldKit = "طقم الصدمة الدورانية وإنعاش سوائل الجسم (Fluid Resuscitation Combat Kit - TXA)",
                surgicalSteps = listOf(
                    "تحضير وتجهيز خط وريدي محيطي ثنائي المدخل عريض القطر (18G IO/IV Access).",
                    "تسريب سريع مستمر حمض الترانيكساميك (TXA 1g) في غضون 3 ساعات من حدوث النزف لتثبيت تميع الدم العسكري.",
                    "تطبيق السوائل المساندة الدافئة ومعاوقة تباطؤ عضلة القلب بحقن الأتروبين الفورية."
                ),
                procedure = "مزامنة التحفيز القلبي بحقن الأتروبين لزيادة النبض، وتدفق السوائل والبدء بالضغط اليدوي عند انعدام الترددات الحيوية.",
                tools = listOf("أمبولات الأتروبين 1 ملغ", "قسطرة محيطية سريعة المداخل IV", "سوائل تكتيكية مضغوطة Hextend"),
                triageClass = "أحمر (حرج - إنقاذ حياة)",
                triageColor = TextOrange
            ),
            AnatomyHotspot(
                id = "abdomen",
                arName = "🩸 أعضاء البطن والنزيف الغائر",
                engName = "Abdominal Shrapnel & Bleed",
                xPct = 0.5f,
                yPct = 0.56f,
                x3d = 0f,
                y3d = 6f,
                z3d = 10f,
                symptoms = "صلابة شديدة بجدار الصدر والبطن (Board-like rigidity)، ألم جارف مبرح مصحوب بضياع النبض الشعاعي وصدمة صامتة.",
                safetyStatus = "مستقر مؤقتاً - مهددة بالنزيف الانفجاري ⚠️",
                urgentIntervention = "تثبيت الأجسام الصلبة والشظايا المغروسة بطرق دائرية، دون نزعها لتلافي الانضغاط الوعائي الداخلي الكبدي.",
                fieldKit = "حقيبة رتق نزيف تجويف البطن والإصابات الجراحية (Abdominal Trauma Packet)",
                surgicalSteps = listOf(
                    "تثبيت وتغطية الشظية المستقرة في جدار البطن باستخدام وسادات ضغط دائرية وتطويق الأطراف.",
                    "حشو أي شقوق بروتوبلازمية مفتوحة بشاش طيني مرقئ متطور (Kaoline QuikClot) والضغط المستمر لثلاث دقائق.",
                    "تجهيز المصاب مباشرة لإجراء جراحة استكشاف البطن الإسعافية الطارئة (Exploratory Laparotomy) بالخلف."
                ),
                procedure = "تثبيت الجسم الغريب المنغرز دون نزعه، تطبيق حزام ضغط البطن وصدمة وريدية ثنائية فورية مع النقل السريع للعمليات الجراحية.",
                tools = listOf("حشوات الشاش المرقئ الحاد (Kaoline QuikClot)", "أربطة ضغط بطنية مرنة", "أحزمة تثبيت الساق والحوض"),
                triageClass = "أصفر (عاجل - جراحة لازمة)",
                triageColor = TextGold
            ),
            AnatomyHotspot(
                id = "extremities",
                arName = "🦾 الشرايين الرئيسية والأطراف",
                engName = "Arterial Bleed & Extremities",
                xPct = 0.35f,
                yPct = 0.76f,
                x3d = -16f,
                y3d = -34f,
                z3d = 5f,
                symptoms = "تدفق نفاث قوي للدم الأحمر القاني النابض من جرح الشريان الفخذي أو العضدي مع تدهور سريع بضغط الدم.",
                safetyStatus = "تهديد حتمي بالنزيف وسقوط الحياة 🚨",
                urgentIntervention = "التطبيق الفوري والحازم لعصابة العواصم CAT في أعلى نقطة بالطرف المصاب لوقف ضياع الحياة بالكامل.",
                fieldKit = "طقم تطويق النزيف الشرياني الفوري للجنود (Tactical Extremity Hemostasis Kit)",
                surgicalSteps = listOf(
                    "استخراج عاصبة CAT ووضعها مباشرة فوق موقع الإصابة بـ 2-3 بوصات بعيداً عن الركبة أو الكوع.",
                    "شد الشريط الطوقي بكل قوة، وتدوير قضيب العاصبة الخشبي الضاغط حتى يتوقف تدفق النبض بالكامل.",
                    "غلق قفل القضيب، وكتابة تكتيكي للزمن المكتوب بالدقيقة (TIME) على شريط المعاينة للجبهة لإخطار المستلم."
                ),
                procedure = "تطبيق حزام العاصبة القتالية CAT (Tourniquet) فوراً بأعلى الطرف بمقدار 2-3 بوصات فوق النزيف وإحكام دوران القضيب وتوثيق الوقت.",
                tools = listOf("عاصبة الإنقاذ الميدانية التكتيكية CAT", "مقص ملابس الميدان الحديدي", "قلم توثيق زمن تطبيق العاصبة"),
                triageClass = "أحمر (حرج - حماية الأطراف والدم)",
                triageColor = TextOrange
            )
        )
    }

    val selectedHotspot = remember(selectedHotspotId) {
        hotspots.find { it.id == selectedHotspotId }
    }

    // Documents/Manuals list for generative terminal
    val docManuals = remember {
        listOf(
            "كتاب بروتوكولات الرعاية التكتيكية للجرحى في الميدان TCCC v4 (250 صفحة)",
            "الدليل القتالي الطبي للتعامل مع الحروب والغازات الكيماوية والإشعاعية (180 صفحة)",
            "دليل العمليات الجراحية الطارئة وإصابات الانفجار والشظايا في مستشفيات الخط المتقدم (320 صفحة)"
        )
    }

    val manualBriefs = remember {
        mapOf(
            0 to listOf(
                "🛡️ الرعاية تحت النار (Care Under Fire): الإجراء الطبي الوحيد المسموح به هو تطبيق العاصبة (Tourniquet) لوقف النزيف الشرياني الفوري.",
                "🛡️ الرعاية في الميدان (Tactical Field Care): تصفية الرئتين وفحص الصدر الصامت، تطبيق إبرة التنفيس الصدري للـ Pneumothorax.",
                "🛡️ الرعاية أثناء الإخلاء (TACEVAC Care): دعم السوائل الوريدية الدافئة بحذر شديد وقياس رصيد وعي المصاب التراكمي وتوثيق كافة مخرجات العلاج.",
                "🛡️ الأولوية الكبرى: إسكات نيران العدو وتحقيق الأمان الذاتي للمسعف قبل الشروع في أي إسعاف حرج جراحي ميداني."
            ),
            1 to listOf(
                "☣️ مكافحة تسمم غاز الأعصاب (Nerve Agent Defense): حقن الأتروبين ثنائي التفاعل فور ظهور العلامات الخفيفة لزيادة الحماية الرئوية والقلبية.",
                "☣️ إخلاء الملابس الصارم: نزع ملابس جندي الإصابة يقضي على 90% من الملوثات السطحية للمركبات السامة السائلة الجافة.",
                "☣️ تطهير العيون المتضررة: الغسيل الغزير بالماء المقطر أو المصل الفسيولوجي المعتدل لمدة 15 دقيقة بدون حك جفن العين.",
                "☣️ منع محفزات التنفس اليدوي المباشر (أمبو باج بالصمام المفرد المحمي فقط) لتلافي استنشاق الغازات المتأثرة من المسعف."
            ),
            2 to listOf(
                "🩸 التثبيت الفوري للأجسام المنغرزة: يحظر تماماً نزع الشظايا الكبيرة المستقرة في جدار الصدر والبطن بالميدان لتفادي النزيف الانفجاري المباشر.",
                "🩸 الفتائل الممتصة للحروق: تغطية الحروق الجافة الواسعة بالضمادات الجافة المعقمة وتجنب وضع المراهم الدهنية لتلافي جذب الشظايا والأتربة.",
                "🩸 مراقبة الفشل العضلاتي الطاحر (Crush Injury): الشحنات الصادمة للأطراف المبتورة تفرز بروتينات تسد الكلى، الإكثار من السوائل المعتدلة أساسي.",
                "🩸 تجميع وبتر الأطراف القتالية المهروسة تماماً لا يتم إلا بالمستشفيات المتقدمة الخلفية وتحت تخدير متكامل ومعاير."
            )
        )
    }

    // Effect for simulated terminal writing animation
    LaunchedEffect(selectedDocIdx, isBriefingGenerating) {
        if (isBriefingGenerating) {
            visibleBriefingLines = emptyList()
            val fullList = manualBriefs[selectedDocIdx] ?: emptyList()
            playBeep(ToneGenerator.TONE_CDMA_PIP)
            for (line in fullList) {
                delay(800)
                visibleBriefingLines = visibleBriefingLines + line
                playBeep(ToneGenerator.TONE_PROP_BEEP)
            }
            isBriefingGenerating = false
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP HEADER INDICATOR
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF030710).copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TextGold)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "الأدوات الطبية التكتيكية والتوليدية المتقدمة (Generative Suite) 🧪",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "مجموعة برمجية مساعدة لدعم قرارات الأطباء في إدارة حوادث الـ CBRN، تحليل الأعضاء تفاعلياً، وتوليد الموجز القتالي للمستندات الميدانية الضخمة.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // FEATURE 1: CBRN ANTIDOTE SELECTOR WITH PULSING RED LIGHT
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with pulsing warning indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = hazardPulseAlpha))
                                    .border(1.dp, Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دليل الطوارئ والـ CBRN التفاعلي السريع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "إنذار نشط 🚨",
                                fontSize = 8.5.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اختر طبيعة العامل الكيميائي السام المنبعث في ساحة القتال لتنزيل خطة الترياق العلاجية والجرعات وجدول حماية الفريق المسعف بالتفصيل:",
                        fontSize = 10.5.sp,
                        color = TextPrimary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agent Selector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        agents.forEach { agent ->
                            val isSelected = selectedAgentKey == agent.key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Red else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedAgentKey = agent.key
                                        playBeep(ToneGenerator.TONE_CDMA_CONFIRM)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = agent.name.substringBefore(" ").trim(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Red else TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Agent Live Technical Data Profile
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = selectedAgent.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                Text(text = selectedAgent.levelCode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                            Text(text = "الاسم العلمي الكامن: ${selectedAgent.latinName}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(text = selectedAgent.description, fontSize = 10.5.sp, color = TextPrimary, lineHeight = 14.sp)

                            Divider(color = Color.White.copy(alpha = 0.06f))

                            // Antidote Profile banner
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🧪 الترياق المعتمد (Emergency Antidote): ", fontSize = 9.5.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = selectedAgent.antidoteName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛡️ درع الحماية الشخصية (PPE Required): ", fontSize = 9.5.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = selectedAgent.ppeRequired, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3498DB))
                            }

                            Divider(color = Color.White.copy(alpha = 0.06f))

                            // Actionable Dosage verification widget
                            Text(text = "🎯 " + selectedAgent.idealDosePrompt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGold)
                            
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(selectedAgent.doseOptions.size) { index ->
                                    val doseText = selectedAgent.doseOptions[index]
                                    val isPicked = userSelectedDoseIdx == index
                                    val isCorrect = index == selectedAgent.correctDoseIdx
                                    val chipBg = when {
                                        isPicked && isCorrect -> Color(0xFF2ECC71).copy(alpha = 0.25f)
                                        isPicked && !isCorrect -> Color.Red.copy(alpha = 0.25f)
                                        else -> Color.White.copy(alpha = 0.06f)
                                    }
                                    val chipBorder = when {
                                        isPicked && isCorrect -> Color(0xFF2ECC71)
                                        isPicked && !isCorrect -> Color.Red
                                        else -> Color.White.copy(alpha = 0.15f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                userSelectedDoseIdx = index
                                                if (isCorrect) {
                                                    playBeep(ToneGenerator.TONE_PROP_BEEP)
                                                    doseAnswerFeedback = "جرعة تكتيكية دقيقة وممتازة! تم تلافي العوامل السامة وتثبيت المفرزات الصدرية بنجاح 🏆"
                                                } else {
                                                    playBeep(ToneGenerator.TONE_SUP_ERROR)
                                                    doseAnswerFeedback = "تنبيه! جرعة غير دقيقة قد تسبب استجابة قلبية حادة وفشل إنعاش. يرجى مراجعة تفاصيل الترياق ⚠️"
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = doseText, fontSize = 10.sp, color = TextPrimary)
                                    }
                                }
                            }

                            if (doseAnswerFeedback.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (userSelectedDoseIdx == selectedAgent.correctDoseIdx) Color(0xFF2ECC71).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = doseAnswerFeedback,
                                        fontSize = 10.2.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userSelectedDoseIdx == selectedAgent.correctDoseIdx) Color(0xFF2ECC71) else Color.Red,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.06f))

                            // Steps to protect tactical team (جدول حماية الأبطال الطبيين)
                            Text(text = "🛡️ تسلسل خطوات حماية الطاقم وإنقاذ المصاب:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                            selectedAgent.tacticalSteps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "${index + 1}.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                    Text(text = step, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // FEATURE 2: INTERACTIVE ANATOMY HOTSPOT CANVAS
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧬", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ماسح الأعراض الجزئية للأعضاء (قماش التشريح التفاعلي الميداني)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                        }
                        Text(
                            text = "تفاعلي ◯",
                            fontSize = 8.5.sp,
                            color = Color(0xFF2ECC71),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "انقر فوق الدوائر النابضة في الهيكل العظمي وجسم الجندي لعرض الأعراض الميدانية الدقيقة فوراً، ووصف الإسعاف التكتيكي الصارم والأدوات اللازمة لتجنب الفقد:",
                        fontSize = 10.5.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        lineHeight = 14.sp
                    )

                    // Anatomy System Control Buttons Bar (resembles professional multi-system toggling)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val systems = listOf(
                            Triple("all", "🧬 دمج الأنظمة", Color(0xFF2ECC71)),
                            Triple("skeleton", "🦴 هيكل عظمي", Color(0xFF3498DB)),
                            Triple("muscles", "💪 عضلات الميدان", Color(0xFFE74C3C)),
                            Triple("organs", "🫀 أعضاء وظيفية", Color(0xFFE67E22)),
                            Triple("nervous", "⚡ شبكة الأعصاب", Color(0xFFF1C40F))
                        )

                        systems.forEach { (sysKey, label, accentColor) ->
                            val isChosen = activeAnatomySystem == sysKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChosen) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.03f))
                                    .border(
                                        1.dp,
                                        if (isChosen) accentColor else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .laserScanRipple(isChosen)
                                    .clickable {
                                        activeAnatomySystem = sysKey
                                        playBeep(android.media.ToneGenerator.TONE_PROP_BEEP)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) accentColor else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // The Interactive Pseudo-3D Drawing Canvas and hotspots with drag rotation
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF02060F))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Rotate around Y axis with horizontal drag, constrain X pitch angle
                                    anatomyRotationY = (anatomyRotationY + dragAmount.x * 0.4f) % 360f
                                    anatomyRotationX = (anatomyRotationX - dragAmount.y * 0.3f).coerceIn(-30f, 30f)
                                }
                            }
                    ) {
                        val widthPx = constraints.maxWidth.toFloat()
                        val heightPx = constraints.maxHeight.toFloat()
                        val localDensity = androidx.compose.ui.platform.LocalDensity.current

                        // Local helper to project 3D points in orthographic coordinate system
                        val projectPoint3D = { xRaw: Float, yRaw: Float, zRaw: Float ->
                            val centerX = widthPx / 2f
                            val centerY = heightPx / 2f + 14f
                            val scaleFactor = 1.35f

                            val radX = Math.toRadians(anatomyRotationX.toDouble()).toFloat()
                            val radY = Math.toRadians(anatomyRotationY.toDouble()).toFloat()

                            // Yaw rotation (Y axis)
                            val cosY = cos(radY)
                            val sinY = sin(radY)
                            val xRotY = xRaw * cosY - zRaw * sinY
                            val zRotY = xRaw * sinY + zRaw * cosY

                            // Pitch tilt (X axis)
                            val cosX = cos(radX)
                            val sinX = sin(radX)
                            val yRotX = yRaw * cosX - zRotY * sinX

                            val xProj = centerX + xRotY * scaleFactor
                            val yProj = centerY - yRotX * scaleFactor

                            Offset(xProj, yProj)
                        }

                        // Project all skeletal joints
                        val headV = projectPoint3D(0f, 74f, 0f)
                        val neckV = projectPoint3D(0f, 52f, 0f)
                        val chestV = projectPoint3D(0f, 32f, 3f)
                        val stomachV = projectPoint3D(0f, 4f, 5f)
                        val pelvisV = projectPoint3D(0f, -22f, 0f)

                        val leftShoulderV = projectPoint3D(-24f, 50f, -2f)
                        val rightShoulderV = projectPoint3D(24f, 50f, -2f)

                        val leftElbowV = projectPoint3D(-38f, 24f, -4f)
                        val rightElbowV = projectPoint3D(38f, 24f, -4f)

                        val leftHandV = projectPoint3D(-44f, 0f, -6f)
                        val rightHandV = projectPoint3D(44f, 0f, -6f)

                        val leftHipV = projectPoint3D(-14f, -24f, 0f)
                        val rightHipV = projectPoint3D(14f, -24f, 0f)

                        val leftKneeV = projectPoint3D(-18f, -54f, 2f)
                        val rightKneeV = projectPoint3D(18f, -54f, 2f)

                        val leftFootV = projectPoint3D(-22f, -86f, 4f)
                        val rightFootV = projectPoint3D(22f, -86f, 4f)

                        // Draw rotating bones and selected systems
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Radar rings background
                            drawCircle(
                                color = TextGold.copy(alpha = 0.02f),
                                radius = 70.dp.toPx(),
                                center = Offset(widthPx / 2f, heightPx / 2f)
                            )
                            drawCircle(
                                color = TextGold.copy(alpha = 0.04f),
                                radius = 110.dp.toPx(),
                                center = Offset(widthPx / 2f, heightPx / 2f),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )
                            )

                            // SYSTEM 1: SKELETON LAYER (Bone system)
                            if (activeAnatomySystem == "skeleton" || activeAnatomySystem == "all") {
                                // 1. Draw central vertebral spine
                                drawLine(
                                    color = Color(0x3F2ECC71),
                                    start = neckV,
                                    end = pelvisV,
                                    strokeWidth = 2.dp.toPx()
                                )

                                // 2. Draw skull circle
                                drawCircle(
                                    color = Color(0x3F3498DB),
                                    radius = 17.dp.toPx(),
                                    center = headV,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                // 3. Draw shoulder bar
                                drawLine(
                                    color = Color(0x2F3498DB),
                                    start = leftShoulderV,
                                    end = rightShoulderV,
                                    strokeWidth = 2.5.dp.toPx()
                                )

                                // 4. Clavicles
                                drawLine(Color(0x1F3498DB), leftShoulderV, neckV, strokeWidth = 1.dp.toPx())
                                drawLine(Color(0x1F3498DB), rightShoulderV, neckV, strokeWidth = 1.dp.toPx())

                                // 5. Rib cage elements
                                for (i in 1..4) {
                                    val leftRibStart = projectPoint3D(-10f - i, 44f - i * 5f, 1f)
                                    val leftRibEnd = projectPoint3D(0f, 42f - i * 5f, 3f)
                                    val rightRibStart = projectPoint3D(10f + i, 44f - i * 5f, 1f)
                                    val rightRibEnd = projectPoint3D(0f, 42f - i * 5f, 3f)

                                    drawLine(Color(0x1F3498DB), leftRibStart, leftRibEnd, strokeWidth = 1.2.dp.toPx())
                                    drawLine(Color(0x1F3498DB), rightRibStart, rightRibEnd, strokeWidth = 1.2.dp.toPx())
                                }

                                // 6. Arms wireframe
                                drawLine(Color(0x283498DB), leftShoulderV, leftElbowV, strokeWidth = 1.5.dp.toPx())
                                drawLine(Color(0x203498DB), leftElbowV, leftHandV, strokeWidth = 1.2.dp.toPx())
                                drawLine(Color(0x283498DB), rightShoulderV, rightElbowV, strokeWidth = 1.5.dp.toPx())
                                drawLine(Color(0x203498DB), rightElbowV, rightHandV, strokeWidth = 1.2.dp.toPx())

                                // 7. Pelvis and leg links
                                drawLine(Color(0x2F3498DB), leftHipV, rightHipV, strokeWidth = 2.dp.toPx())
                                drawLine(Color(0x1F3498DB), pelvisV, leftHipV, strokeWidth = 1.5.dp.toPx())
                                drawLine(Color(0x1F3498DB), pelvisV, rightHipV, strokeWidth = 1.5.dp.toPx())

                                drawLine(Color(0x203498DB), leftHipV, leftKneeV, strokeWidth = 1.8.dp.toPx())
                                drawLine(Color(0x1A3498DB), leftKneeV, leftFootV, strokeWidth = 1.5.dp.toPx())
                                drawLine(Color(0x203498DB), rightHipV, rightKneeV, strokeWidth = 1.8.dp.toPx())
                                drawLine(Color(0x1A3498DB), rightKneeV, rightFootV, strokeWidth = 1.5.dp.toPx())
                            }

                            // SYSTEM 2: MUSCLES LAYER (Red translucent capsules/blocks)
                            if (activeAnatomySystem == "muscles" || activeAnatomySystem == "all") {
                                // Deltoid caps (Shoulder muscles)
                                drawCircle(color = Color(0x3FE74C3C), radius = 8.dp.toPx(), center = leftShoulderV)
                                drawCircle(color = Color(0x3FE74C3C), radius = 8.dp.toPx(), center = rightShoulderV)

                                // Pectoralis major (chest)
                                val leftPecPath = Path().apply {
                                    moveTo(neckV.x, neckV.y)
                                    lineTo(leftShoulderV.x, leftShoulderV.y)
                                    lineTo(chestV.x, chestV.y)
                                    close()
                                }
                                val rightPecPath = Path().apply {
                                    moveTo(neckV.x, neckV.y)
                                    lineTo(rightShoulderV.x, rightShoulderV.y)
                                    lineTo(chestV.x, chestV.y)
                                    close()
                                }
                                drawPath(leftPecPath, color = Color(0x40E74C3C), style = Fill)
                                drawPath(rightPecPath, color = Color(0x40E74C3C), style = Fill)

                                // Arm biceps and triceps lines
                                drawLine(Color(0x4FE74C3C), leftShoulderV, leftElbowV, strokeWidth = 10.dp.toPx())
                                drawLine(Color(0x4FE74C3C), rightShoulderV, rightElbowV, strokeWidth = 10.dp.toPx())
                                drawLine(Color(0x38E74C3C), leftElbowV, leftHandV, strokeWidth = 7.dp.toPx())
                                drawLine(Color(0x38E74C3C), rightElbowV, rightHandV, strokeWidth = 7.dp.toPx())

                                // Abdominals blocks
                                val midAb1 = Offset((chestV.x + stomachV.x)/2f, (chestV.y + stomachV.y)/2f)
                                val midAb2 = Offset((stomachV.x + pelvisV.x)/2f, (stomachV.y + pelvisV.y)/2f)
                                drawCircle(color = Color(0x4FE74C3C), radius = 4.5.dp.toPx(), center = midAb1 + Offset(-7f, 0f))
                                drawCircle(color = Color(0x4FE74C3C), radius = 4.5.dp.toPx(), center = midAb1 + Offset(7f, 0f))
                                drawCircle(color = Color(0x4FE74C3C), radius = 5.dp.toPx(), center = stomachV + Offset(-6f, 0f))
                                drawCircle(color = Color(0x4FE74C3C), radius = 5.dp.toPx(), center = stomachV + Offset(6f, 0f))
                                drawCircle(color = Color(0x4FE74C3C), radius = 4.5.dp.toPx(), center = midAb2 + Offset(-6f, 0f))
                                drawCircle(color = Color(0x4FE74C3C), radius = 4.5.dp.toPx(), center = midAb2 + Offset(6f, 0f))

                                // Upper Leg (Quadriceps)
                                drawLine(Color(0x4FE74C3C), leftHipV, leftKneeV, strokeWidth = 13.dp.toPx())
                                drawLine(Color(0x4FE74C3C), rightHipV, rightKneeV, strokeWidth = 13.dp.toPx())

                                // Lower Leg (Gastrocnemius)
                                drawLine(Color(0x3FE74C3C), leftKneeV, leftFootV, strokeWidth = 9.dp.toPx())
                                drawLine(Color(0x3FE74C3C), rightKneeV, rightFootV, strokeWidth = 9.dp.toPx())
                            }

                            // SYSTEM 3: ORGANS LAYER (Translucent teal/magenta organic models)
                            if (activeAnatomySystem == "organs" || activeAnatomySystem == "all") {
                                // Lungs (Teal overlapping outlines)
                                drawCircle(color = Color(0x251ABC9C), radius = 12.dp.toPx(), center = chestV + Offset(-14f, 8f))
                                drawCircle(color = Color(0x251ABC9C), radius = 12.dp.toPx(), center = chestV + Offset(14f, 8f))
                                drawCircle(color = Color(0x601ABC9C), radius = 12.dp.toPx(), center = chestV + Offset(-14f, 8f), style = Stroke(width = 1.dp.toPx()))
                                drawCircle(color = Color(0x601ABC9C), radius = 12.dp.toPx(), center = chestV + Offset(14f, 8f), style = Stroke(width = 1.dp.toPx()))

                                // Beating Heart (Pulsing Red)
                                val heartR = (8.dp + (1.5.dp * hazardPulseAlpha)).toPx()
                                drawCircle(color = Color(0xCFE74C3C), radius = heartR, center = chestV)
                                drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 2.dp.toPx(), center = chestV + Offset(-1.5f, -1.5f))

                                // Stomach (Yellow/green organic crescent shape)
                                drawCircle(color = Color(0x8FEDF2F7), radius = 7.5.dp.toPx(), center = stomachV + Offset(7f, -3f))
                                drawCircle(color = Color(0x3F02060F), radius = 7.5.dp.toPx(), center = stomachV + Offset(12f, -5f))

                                // Liver (Warm brown wedges)
                                drawCircle(color = Color(0x7FD35400), radius = 9.dp.toPx(), center = stomachV + Offset(-9f, -3f))
                            }

                            // SYSTEM 4: NERVOUS SYSTEM LAYER (Yellow highly glowing fiber networks)
                            if (activeAnatomySystem == "nervous" || activeAnatomySystem == "all") {
                                // Brain network points inside skull
                                val networkPoints = listOf(
                                    headV + Offset(-3f, -3f),
                                    headV + Offset(3f, -3f),
                                    headV + Offset(-5f, 2f),
                                    headV + Offset(5f, 2f),
                                    headV + Offset(0f, -6f)
                                )
                                networkPoints.forEach { pt ->
                                    drawCircle(color = Color(0xFFF1C40F), radius = 1.8.dp.toPx(), center = pt)
                                }
                                for (i in 0 until networkPoints.size - 1) {
                                    drawLine(color = Color(0x60F1C40F), start = networkPoints[i], end = networkPoints[i+1], strokeWidth = 1.dp.toPx())
                                }

                                // Neural spinal main cord
                                drawLine(color = Color(0xFFF1C40F), start = headV, end = pelvisV, strokeWidth = 1.8.dp.toPx())

                                // Outer peripheral branches (arm and leg nerve threads)
                                drawLine(color = Color(0x8FF1C40F), start = leftShoulderV, end = leftHandV, strokeWidth = 1.dp.toPx())
                                drawLine(color = Color(0x8FF1C40F), start = rightShoulderV, end = rightHandV, strokeWidth = 1.dp.toPx())
                                drawLine(color = Color(0x8FF1C40F), start = leftHipV, end = leftFootV, strokeWidth = 1.dp.toPx())
                                drawLine(color = Color(0x8FF1C40F), start = rightHipV, end = rightFootV, strokeWidth = 1.dp.toPx())

                                // Moving electrical signal impulses along pathways
                                val pulseProgress = (System.currentTimeMillis() % 1200) / 1200f
                                val lArmPulse = Offset(
                                    leftShoulderV.x + (leftHandV.x - leftShoulderV.x) * pulseProgress,
                                    leftShoulderV.y + (leftHandV.y - leftShoulderV.y) * pulseProgress
                                )
                                val rArmPulse = Offset(
                                    rightShoulderV.x + (rightHandV.x - rightShoulderV.x) * pulseProgress,
                                    rightShoulderV.y + (rightHandV.y - rightShoulderV.y) * pulseProgress
                                )
                                val lLegPulse = Offset(
                                    leftHipV.x + (leftFootV.x - leftHipV.x) * pulseProgress,
                                    leftHipV.y + (leftFootV.y - leftHipV.y) * pulseProgress
                                )
                                val rLegPulse = Offset(
                                    rightHipV.x + (rightFootV.x - rightHipV.x) * pulseProgress,
                                    rightHipV.y + (rightFootV.y - rightHipV.y) * pulseProgress
                                )

                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = lArmPulse)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = rArmPulse)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = lLegPulse)
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = rLegPulse)
                            }
                        }

                        // Hotspots overlay positioning based on 3D rotation projection
                        hotspots.forEach { spot ->
                            val projectedOffset = projectPoint3D(spot.x3d, spot.y3d, spot.z3d)
                            val isSelected = selectedHotspotId == spot.id

                            val xDp = with(localDensity) { projectedOffset.x.toDp() }
                            val yDp = with(localDensity) { projectedOffset.y.toDp() }

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = xDp - 14.dp,
                                        y = yDp - 14.dp
                                    )
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .laserScanRipple(isSelected)
                                    .clickable {
                                        selectedHotspotId = spot.id
                                        appliedInterventionResponse = ""
                                        playBeep(ToneGenerator.TONE_CDMA_PIP)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing warning center core
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 14.dp else 10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF2ECC71) else spot.triageColor)
                                )
                                // Expanding neon ring halo
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            1.5.dp,
                                            (if (isSelected) Color(0xFF2ECC71) else spot.triageColor).copy(alpha = hazardPulseAlpha),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // Little technical guide labels top-right
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("📍 مراكز الإصابات والنزيف الميداني:", fontSize = 8.sp, color = TextGold, fontWeight = FontWeight.Bold)
                            hotspots.forEach { spot ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(spot.triageColor))
                                    Text(text = spot.arName.substringBefore(" ").trim(), fontSize = 7.5.sp, color = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("🤾 اسحب لتفتير وفحص الجسم 3D", fontSize = 7.sp, color = TextGold, fontWeight = FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detail sheet of hovered/tapped Hotspot
                    AnimatedVisibility(
                        visible = selectedHotspot != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (selectedHotspot != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.2.dp, TextGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = selectedHotspot.arName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                            Text(text = "Target Area: ${selectedHotspot.engName}", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(selectedHotspot.triageColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(1.dp, selectedHotspot.triageColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = selectedHotspot.triageClass, fontSize = 9.5.sp, color = selectedHotspot.triageColor, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text("🧬 حالة الأمان وعصمة الدم:", fontSize = 9.5.sp, color = TextSecondary)
                                        Text(selectedHotspot.safetyStatus, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (selectedHotspot.safetyStatus.contains("🚨") || selectedHotspot.safetyStatus.contains("⚠️")) TextOrange else Color(0xFF2ECC71))
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.06f))

                                    // 1. Symptoms
                                    Column {
                                        Text(text = "❌ الأعراض والعلامات القاتلة في ساحة الوغى:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = selectedHotspot.symptoms, fontSize = 10.5.sp, color = TextPrimary, lineHeight = 15.sp)
                                    }

                                    // 2. Urgent Medical Interventions (الاستجابة الإسعافية الطارئة)
                                    Column {
                                        Text(text = "🩺 الاستجابة وتدابير الرعاية الإسعافية الطارئة:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = selectedHotspot.urgentIntervention, fontSize = 10.5.sp, color = TextPrimary, lineHeight = 15.sp)
                                    }

                                    // 3. Required Field Kits (العدة الميدانية اللازمة)
                                    Column {
                                        Text(text = "🧰 المعدات الطبية والعدة الميدانية المخصصة للعملية:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = selectedHotspot.fieldKit, fontSize = 10.5.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }

                                    // 4. Tools chips list
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        selectedHotspot.tools.forEach { tool ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(text = "⚙️ $tool", fontSize = 9.sp, color = TextSecondary)
                                            }
                                        }
                                    }

                                    // 5. Step-by-step Surgical Procedures (الخطوات الجراحية التفصيلية خطوة بخطوة) - WITH TYPEWRITER ENGINE effect
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(text = "⚔️ تسلسل التدخلات والخطوات الجراحية الصارمة (SURGICAL PROTOCOLS):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        selectedHotspot.surgicalSteps.forEachIndexed { sIdx, step ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(text = "الخطوة ${sIdx + 1}:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                TerminalText(
                                                    text = step,
                                                    style = LocalTextStyle.current.copy(fontSize = 10.sp, lineHeight = 14.sp),
                                                    color = TextPrimary,
                                                    speedMs = 12
                                                )
                                            }
                                        }
                                    }

                                    if (appliedInterventionResponse.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF2ECC71).copy(alpha = 0.1f))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = appliedInterventionResponse,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2ECC71),
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Apply immediate tactical intervention button
                                        Button(
                                            onClick = {
                                                playBeep(ToneGenerator.TONE_PROP_BEEP)
                                                appliedInterventionResponse = "✓ تم تنفيذ الإجراء التكتيكي العسكري وتأمين حياة الجندي بنجاح باستخدام الأدوات المخصصة! استقرار فوري للضغط الشرياني وهبوط وتيرة الصدمة القلبية."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Text("تطبيق الإسعاف التكتيكي الفوري 🫡", color = Primary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Hide / Dismiss Button
                                        Button(
                                            onClick = {
                                                selectedHotspotId = null
                                                appliedInterventionResponse = ""
                                                playBeep(ToneGenerator.TONE_SUP_ERROR)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("إخفاء المعلومات ✕", color = TextSecondary, fontSize = 10.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FEATURE 3: BRIEFING TERMINAL MODE GENERATOR
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📟", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مولد الاقتراحات والموجز التكتيكي الذكي (Briefing Terminal)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(TextGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("عالي الأمان 💻", fontSize = 8.sp, color = TextGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "اختر أحد كتيبات التدريب العسكري الكبيرة والموسوعات الطبية لضغطها وتلخيص أهم نقاط العمل الطبية في موجز تكتيكي مقتضب وسريع للتركيز والأداء:",
                        fontSize = 10.5.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        lineHeight = 14.sp
                    )

                    // Manual selectors
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        docManuals.forEachIndexed { idx, docName ->
                            val isChosen = selectedDocIdx == idx
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) TextGold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                                    .border(
                                        1.dp,
                                        if (isChosen) TextGold else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedDocIdx = idx
                                        visibleBriefingLines = emptyList()
                                        playBeep(ToneGenerator.TONE_CDMA_PIP)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isChosen) TextGold else Color.White.copy(alpha = 0.2f))
                                    )
                                    Text(
                                        text = docName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) TextGold else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Generation action button
                    Button(
                        onClick = {
                            isBriefingGenerating = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBriefingGenerating
                    ) {
                        Text(
                            text = if (isBriefingGenerating) "جاري مسح ومعاينة الكتيبات الطبية العسكرية وتلخيصها بالأحجام..." else "تحليل وتوليد موجز اليوم الطبي التكتيكي الموجه ⚡",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Holographic Terminal Shell Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.2.dp, Color(0x3F2ECC71), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Upper shell title bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Yellow))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Green))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MILITARY-MEDICAL-AI-TERMINAL v2.8", fontSize = 8.sp, color = Color(0x7F2ECC71), fontFamily = FontFamily.Monospace)
                                }
                                Text("SECURE LINE", fontSize = 8.sp, color = Color.Red, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color(0x1F2ECC71))

                            if (isBriefingGenerating) {
                                LinearProgressIndicator(
                                    color = Color(0xFF2ECC71),
                                    trackColor = Color(0x1F2ECC71),
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                                Text(
                                    text = "جاري تجميع حزم المعلومات السريرية، وقص المحشوات الدلالية، وضياع النثر الصغير...",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2ECC71),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (visibleBriefingLines.isEmpty()) {
                                Text(
                                    text = "بانتظار تلقي الأمر لبدء عملية المسح والتلخيص التوليدي الجراحي...",
                                    fontSize = 10.sp,
                                    color = Color(0x7F2ECC71),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            } else {
                                Text(
                                    text = "✓ تم توليد الموجز الإرشادي بنجاح تحت التوجيه التكتيكي الميداني:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2ECC71),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                visibleBriefingLines.forEach { line ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x0C2ECC71), RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = line,
                                            fontSize = 11.sp,
                                            color = Color(0xFF2ECC71),
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 15.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "خط الحماية الطبي موثق بالكامل. 🫡",
                                        fontSize = 9.sp,
                                        color = Color(0x7F2ECC71),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = {
                                            playBeep(ToneGenerator.TONE_CDMA_CONFIRM)
                                            Toast.makeText(context, "تم حفظ ومزامنة الخطة مع باقي الفصائل الطبية بنجاح! 📋", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71).copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("نسخ وتثبيت السجل 📋", color = Color(0xFF2ECC71), fontSize = 9.sp)
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

// Global Custom inline Constants inside Applet to keep variables unified
object InvoluntaryWebConstants {
    val ButtonHeight = 44.dp
}
