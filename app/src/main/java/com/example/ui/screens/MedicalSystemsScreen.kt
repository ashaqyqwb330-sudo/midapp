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
import com.example.model.MedSystem
import com.example.ui.components.GlassCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalSystemsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dataProvider = remember { DataProvider(context) }
    val repository = remember { StudyPlanRepository(dataProvider) }
    val systems = remember { repository.getMedicalSystems() }
    var selectedSystem by remember { mutableStateOf<MedSystem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedSystem != null) selectedSystem!!.name else "الأجهزة الطبية",
                        color = TextGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSystem != null) selectedSystem = null
                        else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        if (selectedSystem != null) {
            val system = selectedSystem!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = system.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${system.subjects.size} تخصصاً",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                items(system.subjects) { subject ->
                    StaggeredEntrance {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🔬", fontSize = 22.sp)
                                Text(
                                    text = subject,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(systems) { system ->
                    StaggeredEntrance {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSystem = system }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = system.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${system.subjects.size} تخصصاً",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
