package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.example.model.RecentCalc
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DrugDatabaseHelper(context) }
    
    // Theme and Mode Trigger
    var isHighContrast by remember { ThemeSettings.isHighContrast }
    var recentCalcs by remember { mutableStateOf<List<RecentCalc>>(emptyList()) }

    // Fetch offline calculations on startup and whenever db changes
    LaunchedEffect(Unit) {
        recentCalcs = dbHelper.getRecentCalculations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المقاييس الطبية الميدانية 🩺",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "مقاييس سريرية تكتيكية منفصلة بالكامل",
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
                            contentDescription = "رجوع",
                            tint = TextGold
                        )
                    }
                },
                actions = {
                    // High Contrast Toggle Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { isHighContrast = !isHighContrast }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHighContrast) "عالي التباين 👁️" else "الوضع المظلم 🌌",
                            fontSize = 11.sp,
                            color = TextGold,
                            fontWeight = FontWeight.Bold
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
                    if (isHighContrast) {
                        Brush.verticalGradient(listOf(Color.Black, Color.Black))
                    } else {
                        Brush.verticalGradient(
                            listOf(Primary, Color(0xFF071424), Color(0xFF0F1F33))
                        )
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                // Section: Individual Calculators Selection
                item {
                    Text(
                        text = "اختر المقاييس السريرية المخصصة 🛡️",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
               }

                // Card GCS
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(context, com.example.GcsActivity::class.java))
                            }
                            .testTag("gcs_launch_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🧠", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مقياس غيبوبة غلاسكو (GCS)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "معاينة الاستجابة للعين، اللفظية والـحركية مع تفصيل النقاط.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(text = "⬅️", fontSize = 18.sp, color = TextGold)
                        }
                    }
                }

                // Card eGFR
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(context, com.example.EgfrActivity::class.java))
                            }
                            .testTag("egfr_launch_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🧪", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "معدل ترشيح الكلى (eGFR)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "حساب تصفية الكرياتينين بطريقة Cockcroft-Gault مع اعتبار الجنس والوزن.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(text = "⬅️", fontSize = 18.sp, color = TextGold)
                        }
                    }
                }

                // Card ABG
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(Intent(context, com.example.AbgActivity::class.java))
                            }
                            .testTag("abg_launch_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🫁", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "غازات الدم الشرياني (ABG)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "تفسير وتشخيص الحموضة والقلوية بالرئة والتمثيل الغذائي مع تصنيف التعويض.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(text = "⬅️", fontSize = 18.sp, color = TextGold)
                        }
                    }
                }

                // Visual Offline Caching / History section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل الحسابات والإنقاذ الميداني 📋 (أوفلاين)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        if (recentCalcs.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    dbHelper.clearCalculations()
                                    recentCalcs = emptyList()
                                    Toast.makeText(context, "تم تفريغ السجل بالكامل", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("تفريغ الميداني", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (recentCalcs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أي حسابات مخزنة محلياً في الجلسة الحالية.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(recentCalcs) { calc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighContrast) Color(0xFF141414) else Color(0x30112233)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isHighContrast) Color.Yellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = when (calc.type) {
                                            "GCS" -> "🧠"
                                            "eGFR" -> "🧪"
                                            "ABG" -> "🫁"
                                            else -> "💊"
                                        }
                                        Text(text = icon, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = calc.type,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = calc.timestamp,
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                dbHelper.deleteCalculation(calc.id)
                                                recentCalcs = dbHelper.getRecentCalculations()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Divider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Text(
                                    text = "المعطيات: ${calc.inputs}",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = calc.result,
                                        fontSize = 12.sp,
                                        color = if (isHighContrast) Color.Yellow else TextOrange,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth()
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
