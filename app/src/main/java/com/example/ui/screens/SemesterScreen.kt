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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.data.StudyPlanRepository
import com.example.model.StudyCourse
import com.example.ui.components.GlassCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterScreen(
    semesterId: Int,
    onBack: () -> Unit,
    onCourseClick: (String) -> Unit
) {
    val context = LocalContext.current
    val dataProvider = remember { DataProvider(context) }
    val repository = remember { StudyPlanRepository(dataProvider) }
    val stages = remember { repository.getStages() }
    val semester = stages.flatMap { it.semesters }.find { it.id == semesterId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(semester?.name ?: "الفصل $semesterId", color = TextGold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        if (semester != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(semester.courses) { course ->
                    StaggeredEntrance {
                        CourseCard(course = course, onClick = { onCourseClick(course.name) })
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("الفصل غير موجود", color = TextSecondary)
            }
        }
    }
}

@Composable
fun CourseCard(course: StudyCourse, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = course.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when (course.type) {
                    "book" -> "📘"
                    "subject" -> "🔬"
                    "general" -> "📋"
                    else -> "📄"
                },
                fontSize = 20.sp
            )
        }
    }
}
