package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.data.StudyPlanRepository
import com.example.model.StudyStage
import com.example.ui.components.GlassCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanScreen(
    onBack: () -> Unit,
    onSemesterClick: (Int) -> Unit,
    onMedicalSystemsClick: () -> Unit
) {
    val context = LocalContext.current
    val dataProvider = remember { DataProvider(context) }
    val repository = remember { StudyPlanRepository(dataProvider) }
    val stages = remember { repository.getStages() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الخطة الدراسية", color = TextGold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(stages) { stage ->
                StaggeredEntrance {
                    StageCard(stage = stage, onSemesterClick = onSemesterClick)
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMedicalSystemsClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🏥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الأجهزة الطبية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StageCard(stage: StudyStage, onSemesterClick: (Int) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stage.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextGold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stage.description,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            stage.semesters.forEach { semester ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSemesterClick(semester.id) },
                    color = PrimaryLight.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = semester.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${semester.courses.size} مقرر",
                            fontSize = 12.sp,
                            color = TextGold
                        )
                    }
                }
            }
        }
    }
}
