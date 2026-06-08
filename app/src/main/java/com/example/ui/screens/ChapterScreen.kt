package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun ChapterScreen(
    chapterName: String,
    chapterId: String,
    onBack: () -> Unit,
    onNavigate: (String, Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DataProvider(context) }
    val generals = remember { repository.getGeneralSubjects(chapterId) }
    val (_, _, devicesMap) = remember { repository.getBooksInChapter(chapterId) }
    val deviceNames = remember { devicesMap.keys.toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF030810), Primary, Color(0xFF0F1F33))))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔙",
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = chapterName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextGold,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (generals.isNotEmpty()) {
            Text(
                text = "📚 المواد العامة والمشتركة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextOrange,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                itemsIndexed(generals) { index, subject ->
                    GlassCard(
                        modifier = Modifier
                            .width(200.dp)
                            .height(110.dp)
                            .clickable {
                                onNavigate(
                                    "subject_content",
                                    mapOf(
                                        "chapterId" to chapterId,
                                        "deviceName" to "general_subject",
                                        "subjectTitle" to subject,
                                        "subjectIndex" to index.toString(),
                                        "isGeneral" to "true"
                                    )
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📋", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subject,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Secondary.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (deviceNames.isNotEmpty()) {
            Text(
                text = "🫁 بلكات أجهزة وأعضاء الجسم (الأجهزة المذكورة بالبلك)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(deviceNames) { _, device ->
                    GlassCard(
                        modifier = Modifier
                            .width(180.dp)
                            .height(120.dp)
                            .clickable {
                                onNavigate(
                                    "device_subjects",
                                    mapOf(
                                        "chapterId" to chapterId,
                                        "deviceName" to device
                                    )
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val emoji = when {
                                device.contains("الهيكل العضلي") -> "🦴"
                                device.contains("القلبي") || device.contains("القلب") -> "🫀"
                                device.contains("التنفسي") || device.contains("التنفس") -> "🫁"
                                device.contains("الهضمي") -> "🍕"
                                device.contains("البولي") || device.contains("التناسلي") -> "🧼"
                                device.contains("الدموي") || device.contains("اللمفاوي") -> "🩸"
                                device.contains("الصمائي") -> "🧬"
                                device.contains("العصبي") -> "🧠"
                                device.contains("عسكرية") -> "🛡️"
                                else -> "🫁"
                            }
                            Text(emoji, fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = device,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        
        // If empty
        if (generals.isEmpty() && deviceNames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مواد أو أجهزة مسجلة في هذا الفصل.",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        }
    }
}
