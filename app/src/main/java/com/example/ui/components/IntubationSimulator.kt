package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class IntubationStep(val arName: String, val desc: String) {
    SEDATION("🎚️ المخدر والتخدير", "قم بإعطاء المخدر وتوسيط الرأس لتأمين وضعية الشم البصرية مسبقاً"),
    LARYNGOSCOPE("🦴 نصل المنظار", "اسحب منظار الحنجرة للأمام وللأعلى لرفع لسان المزمار وكشف الأوتار"),
    TUBE_INSERTION("🫁 تمرير الأنبوب", "وجه الأنبوب الأصفر بحذر بين الأوتار الصوتية لتلافي وضعه بالمريء"),
    CUFF_INFLATION("🎈 ثني الكفة والتثبيت", "انفخ الكفة الهوائية لمنع التسرب، وتأكّد من حركية الصدر بسماعتك"),
    SUCCESS("🏆 تم التنبيب بنجاح", "وضعية ممتازة ومعدل تدفق الاكسجين 100% مستقر تكتيكياً")
}

@Composable
fun IntubationSimulator(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val scope = rememberCoroutineScope()

    // Key Game States
    var currentStep by remember { mutableStateOf(IntubationStep.SEDATION) }
    var scoreXP by remember { mutableStateOf(100) }
    var patientHR by remember { mutableStateOf(84) }
    var patientSpO2 by remember { mutableStateOf(92) }
    var patientRR by remember { mutableStateOf(14) }

    // Interactivity Offset state for tools
    var laryngoscopeOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var tubeOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    
    // Checkpoints & flags
    var isSedated by remember { mutableStateOf(false) }
    var epiglottisLiftedPercent by remember { mutableStateOf(0f) }
    var isTubeInTrachea by remember { mutableStateOf(false) }
    var isCuffInflated by remember { mutableStateOf(false) }
    var showTelemetryAlert by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("ابدأ بتخدير المريض وبسط العنق") }

    // Simulation Clock / Countdown to induce tension!
    LaunchedEffect(currentStep) {
        if (currentStep != IntubationStep.SUCCESS) {
            while (true) {
                delay(1200)
                // If taking too long, vils drop!
                if (currentStep == IntubationStep.TUBE_INSERTION || currentStep == IntubationStep.LARYNGOSCOPE) {
                    if (patientSpO2 > 75) {
                        patientSpO2 -= 1
                    }
                    if (patientHR < 140) {
                        patientHR += 1
                    }
                }
            }
        } else {
            // Success stabilizes patient
            patientSpO2 = 100
            patientHR = 72
            patientRR = 12
        }
    }

    // Interactive beep based on heart rate
    LaunchedEffect(patientHR, currentStep) {
        val delayTime = (60000 / patientHR).toLong()
        while (currentStep != IntubationStep.SUCCESS) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
            delay(delayTime)
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .staggeredEntrance(2)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with simulation title and game status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (currentStep == IntubationStep.SUCCESS) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "محاكاة التنبيب الرغامي الافتراضي التكتيكي (INTUBATION GAME)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "XP: $scoreXP/100",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                }
            }
            Text(
                text = "مرّن أصابعك على مناورات التخدير ورفع لسان المزمار وإيصال الهواء للرئتين مباشرة في ساحة العمليات",
                fontSize = 9.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            // Vital signs cyber-telemetry HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TelemetryItem(label = "HR (نبض)", value = "$patientHR bpm", color = if (patientHR > 100) Color(0xFFE74C3C) else Color(0xFF2ECC71))
                TelemetryItem(label = "SpO2 (أكسجين)", value = "$patientSpO2%", color = if (patientSpO2 < 85) Color(0xFFE74C3C) else Color(0xFF2ECC71))
                TelemetryItem(label = "RR (تنفس)", value = "$patientRR /min", color = TextGold)
                TelemetryItem(label = "المستوى الحالي", value = currentStep.arName, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Indicator timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IntubationStep.values().forEach { step ->
                    val isPast = step.ordinal < currentStep.ordinal
                    val isActive = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isPast -> Color(0xFF2ECC71)
                                    isActive -> TextGold
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Step guidance text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, TextGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "🎯 التوجيه التكتيكي للخطوة: ${currentStep.arName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                    Text(
                        text = currentStep.desc,
                        fontSize = 9.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "التوجيه الميداني: $feedbackMessage",
                        fontSize = 8.5.sp,
                        color = TextOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Interactive Physics/Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentStep) {
                            detectDragGestures(
                                onDragEnd = {
                                    // Snapping and checkpoint triggers based on drag offsets
                                    if (currentStep == IntubationStep.LARYNGOSCOPE) {
                                        // If dragged back/upwards (say Y offset is negative and X is centered)
                                        if (laryngoscopeOffset.y < -35f && Math.abs(laryngoscopeOffset.x) < 30f) {
                                            epiglottisLiftedPercent = 1f
                                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                                            currentStep = IntubationStep.TUBE_INSERTION
                                            feedbackMessage = "رائع! ارتفع لسان المزمار وانكشفت الأوتار. التقط الأنبوب للبدء بالتنبيب"
                                        } else {
                                            feedbackMessage = "اسحب للأعلى لرفع زاوية الفك وفتح البلعوم"
                                        }
                                    } else if (currentStep == IntubationStep.TUBE_INSERTION) {
                                        // If tube matches trachea offset (X: -10 to +10, Y: -50 to -30)
                                        if (tubeOffset.y < -40f && tubeOffset.x in -20f..20f) {
                                            isTubeInTrachea = true
                                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 250)
                                            currentStep = IntubationStep.CUFF_INFLATION
                                            feedbackMessage = "ممتاز! النفاد ناجح بالقصبة الهوائية. قم بنفخ الكفة الهوائية فوراً"
                                        } else if (tubeOffset.y < -40f && tubeOffset.x < -30f) {
                                            // Slid into esophagus!
                                            scoreXP = (scoreXP - 15).coerceIn(10, 100)
                                            patientSpO2 = (patientSpO2 - 12).coerceIn(40, 100)
                                            feedbackMessage = "🚨 خطأ! السلك زلق ودخل المريء! اسحب الأنبوب فوراً وحاول مجدداً"
                                            tubeOffset = Offset(0f, 0f)
                                            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 500)
                                        }
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                when (currentStep) {
                                    IntubationStep.LARYNGOSCOPE -> {
                                        laryngoscopeOffset = Offset(
                                            x = (laryngoscopeOffset.x + dragAmount.x).coerceIn(-100f, 100f),
                                            y = (laryngoscopeOffset.y + dragAmount.y).coerceIn(-120f, 80f)
                                        )
                                        // Update epiglottis lifting smoothly based on drag upward
                                        if (laryngoscopeOffset.y < 0) {
                                            epiglottisLiftedPercent = (-laryngoscopeOffset.y / 120f).coerceIn(0f, 1.0f)
                                        }
                                    }
                                    IntubationStep.TUBE_INSERTION -> {
                                        tubeOffset = Offset(
                                            x = (tubeOffset.x + dragAmount.x).coerceIn(-120f, 120f),
                                            y = (tubeOffset.y + dragAmount.y).coerceIn(-120f, 80f)
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // Draw concentric radar lines to anchor medical interface
                    drawCircle(color = TextGold.copy(alpha = 0.05f), radius = 80.dp.toPx(), center = center, style = Stroke(width = 1f))
                    drawCircle(color = TextGold.copy(alpha = 0.03f), radius = 120.dp.toPx(), center = center, style = Stroke(width = 0.8f))

                    // 1. Render Airway Anatomy (Mouth, Throat, Trachea, Esophagus in Cross Section)
                    // Trachea Channel (Right Airway) - Neon blue color
                    drawRect(
                        color = Color(0x3F3498DB),
                        topLeft = Offset(center.x + 10f, center.y - 70f),
                        size = Size(24.dp.toPx(), 130.dp.toPx())
                    )
                    // Esophagus Channel (Left Channel) - Translucent pink
                    drawRect(
                        color = Color(0x3FE74C3C),
                        topLeft = Offset(center.x - 50f, center.y - 70f),
                        size = Size(20.dp.toPx(), 130.dp.toPx())
                    )

                    // Draw Vocal cords (Overtone folds) inside the trachea
                    val vocalCordAlpha = 0.2f + (0.8f * epiglottisLiftedPercent)
                    drawLine(
                        color = Color.White.copy(alpha = vocalCordAlpha),
                        start = Offset(center.x + 15f, center.y - 40f),
                        end = Offset(center.x + 28f, center.y - 10f),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = vocalCordAlpha),
                        start = Offset(center.x + 45f, center.y - 40f),
                        end = Offset(center.x + 32f, center.y - 10f),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Anat labels
                    drawContextLabel("المريء (Esophagus)", Offset(center.x - 70f, center.y + 40f), Color(0xFFE74C3C))
                    drawContextLabel("القصبة (Trachea)", Offset(center.x + 55f, center.y + 40f), Color(0xFF3498DB))

                    // Mouth Opening shape (Oral cavity)
                    val mouthPath = Path().apply {
                        moveTo(center.x - 110f, center.y - 120f)
                        quadraticTo(center.x, center.y - 50f, center.x + 100f, center.y - 120f)
                    }
                    drawPath(mouthPath, color = Color.White.copy(alpha = 0.15f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // 2. The Interactive Epiglottis Flap (blocks trachea by default, lifts when laryngoscope pulls up)
                    val epiglottisAngle = -20f - (60f * epiglottisLiftedPercent)
                    val epiglottisPath = Path()
                    val epiglottisPivot = Offset(center.x, center.y - 75f)
                    epiglottisPath.moveTo(epiglottisPivot.x, epiglottisPivot.y)
                    
                    // Rotate the lid based on percentage
                    val length = 35.dp.toPx()
                    val rad = Math.toRadians(epiglottisAngle.toDouble())
                    val epiglottisEnd = Offset(
                        epiglottisPivot.x + (cos(rad) * length).toFloat(),
                        epiglottisPivot.y + (sin(rad) * length).toFloat()
                    )
                    epiglottisPath.lineTo(epiglottisEnd.x, epiglottisEnd.y)
                    drawPath(epiglottisPath, color = Color(0xFFFFCCAA), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                    drawContextLabel("لسان المزمار", epiglottisEnd + Offset(10f, -10f), Color(0xFFFFCCAA))

                    // 3. Render interactable tools depending on active step
                    if (currentStep == IntubationStep.LARYNGOSCOPE) {
                        // Interactive laryngoscope blade & handle
                        val blX = center.x + laryngoscopeOffset.x
                        val blY = center.y + 60f + laryngoscopeOffset.y

                        // Draw metal curves handle
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(blX, blY),
                            end = Offset(blX, blY + 90f),
                            strokeWidth = 14.dp.toPx()
                        )
                        // Glowing laser scan handle pointer
                        drawCircle(color = TextGold, radius = 4.dp.toPx(), center = Offset(blX, blY))

                        // Draw custom curved blade (Macintosh blade)
                        val bladePath = Path().apply {
                            moveTo(blX, blY)
                            quadraticTo(blX + 45f, blY - 20f, blX + 70f, blY - 60f)
                        }
                        drawPath(bladePath, color = Color(0xFFBDC3C7), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                    }

                    if (currentStep == IntubationStep.TUBE_INSERTION || currentStep == IntubationStep.CUFF_INFLATION || currentStep == IntubationStep.SUCCESS) {
                        // Endotracheal Tube drawing
                        val tubeX = if (isTubeInTrachea) (center.x + 22f) else (center.x + tubeOffset.x)
                        val tubeY = if (isTubeInTrachea) (center.y - 40f) else (center.y + 60f + tubeOffset.y)

                        // Draw major yellow tube cylinder
                        drawLine(
                            color = Color(0xFFF1C40F),
                            start = Offset(tubeX, tubeY - 80f),
                            end = Offset(tubeX, tubeY + 40f),
                            strokeWidth = 9.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Draw securing cuff balloon at proximal end
                        val balloonColor = if (isCuffInflated) Color(0xFF2ECC71).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
                        val balloonStrokeColor = if (isCuffInflated) Color(0xFF2ECC71) else Color.White.copy(alpha = 0.4f)
                        drawCircle(
                            color = balloonColor,
                            radius = if (isCuffInflated) 11.dp.toPx() else 7.dp.toPx(),
                            center = Offset(tubeX, tubeY + 14f)
                        )
                        drawCircle(
                            color = balloonStrokeColor,
                            radius = if (isCuffInflated) 11.dp.toPx() else 7.dp.toPx(),
                            center = Offset(tubeX, tubeY + 14f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }

                // Dragging hints overlay inside preview cardboard
                if (currentStep == IntubationStep.LARYNGOSCOPE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("💡 المس المقبض الفضي واسحب للأعلى ولليمين لرفع لسان المزمار حركياً", fontSize = 8.sp, color = TextGold)
                    }
                } else if (currentStep == IntubationStep.TUBE_INSERTION) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("💡 اسحب الأنبوب الرغامي ومكّنه للامتداد داخل القناة الزرقاء المستهدفة", fontSize = 8.sp, color = TextGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step controller actions / interactive game triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action 1: Anesthetize / Sedate patient
                if (currentStep == IntubationStep.SEDATION) {
                    Button(
                        onClick = {
                            isSedated = true
                            currentStep = IntubationStep.LARYNGOSCOPE
                            feedbackMessage = "تم تخدير المريض واسترخاء الأحبال العضلية. اسحب المنظار الآن"
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 150)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💉 تخدير وبسط رأس المريض (SEDATE)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 2: Inflate Securement Airway Cuff
                if (currentStep == IntubationStep.CUFF_INFLATION) {
                    Button(
                        onClick = {
                            isCuffInflated = true
                            currentStep = IntubationStep.SUCCESS
                            scoreXP = 100
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 500)
                            feedbackMessage = "أحسنت! المسارات الهوائية مؤمنة والتحقق من الانتفاخ تكلل بالكامل!"
                            Toast.makeText(context, "عمل تكتيكي متميز! أنقذت المصاب 🫁🏆", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎈 نفخ كفة الأنبوب وتثبيته (INFLATE CUFF)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 3: Reset Game
                Button(
                    onClick = {
                        currentStep = IntubationStep.SEDATION
                        isSedated = false
                        epiglottisLiftedPercent = 0f
                        isTubeInTrachea = false
                        isCuffInflated = false
                        laryngoscopeOffset = Offset(0f, 0f)
                        tubeOffset = Offset(0f, 0f)
                        patientSpO2 = 94
                        patientHR = 80
                        scoreXP = 100
                        feedbackMessage = "تمت إعادة تشكيل محاكاة التبيب العسكري"
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.width(110.dp)
                ) {
                    Text("🔄 تصفير اللعبة", color = TextPrimary, fontSize = 9.5.sp)
                }
            }
        }
    }
}

@Composable
fun TelemetryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = TextSecondary)
        Text(text = value, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 1.dp))
    }
}

// Simple Helper Extension to draw clinical text labels on custom game canvases
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContextLabel(text: String, position: Offset, color: Color) {
    // Custom drawn indicator dot to signal anatomical elements on Canvas boards
    drawCircle(color = color, radius = 2.dp.toPx(), center = position)
}

// Trig math helpers for circular positioning of epiglottis rotators
fun cos(rad: Double): Float = Math.cos(rad).toFloat()
fun sin(rad: Double): Float = Math.sin(rad).toFloat()
