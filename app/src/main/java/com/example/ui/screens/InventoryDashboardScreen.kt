package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

// Data model representing a tracked equipment item with category and maintenance schedule
data class MedicalEquipment(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val totalCount: Int,
    val activeCount: Int, // functional
    val unit: String,     // military hospital unit
    val status: String,   // Active, Under Maintenance, Critical calibration
    val serialNumber: String,
    val category: String = "دعم الحياة",
    val maintenanceSchedule: String = "معايرة أسبوعية وفحص شحن البطارية والموتور",
    val nextMaintenance: String = "2026-06-15"
) {
    // Percentage level computed for the progress bar
    val operationalPercentage: Float
        get() = if (totalCount > 0) activeCount.toFloat() / totalCount.toFloat() else 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Sourced initial database state of military hospitals equipment with Category & Schedules
    var equipmentList by remember {
        mutableStateOf(
            listOf(
                MedicalEquipment(
                    id = "EQ-101",
                    nameAr = "أجهزة الصدمات الكهربائية للقلب",
                    nameEn = "Cardiac Defibrillators",
                    totalCount = 12,
                    activeCount = 10,
                    unit = "وحدة الطوارئ ER",
                    status = "يعمل بالكامل",
                    serialNumber = "MIL-DF-886",
                    category = "دعم الحياة",
                    maintenanceSchedule = "تأكيد تفريغ الشحنة التجريبي ومعايرة المكثفات والجل الكهربائي أسبوعياً",
                    nextMaintenance = "2026-06-12"
                ),
                MedicalEquipment(
                    id = "EQ-102",
                    nameAr = "أجهزة التنفس الاصطناعي الميدانية",
                    nameEn = "Tactical Ventilators",
                    totalCount = 8,
                    activeCount = 5,
                    unit = "مستشفى ميداني (ألفا)",
                    status = "بحاجة لمعايرة ونقص",
                    serialNumber = "MIL-VT-241",
                    category = "أكسجين وتنفس",
                    maintenanceSchedule = "تنظيف واستبدال فلاتر الهيبا الهوائية وفحص ضغط الأكسجين والغازات المختلطة",
                    nextMaintenance = "2026-06-08"
                ),
                MedicalEquipment(
                    id = "EQ-103",
                    nameAr = "مراقبة العلامات الحيوية المتنقلة",
                    nameEn = "Multiparameter Patient Monitors",
                    totalCount = 15,
                    activeCount = 14,
                    unit = "قاعدة التدريب (جاما)",
                    status = "يعمل بالكامل",
                    serialNumber = "MIL-PM-952",
                    category = "تشخيصي",
                    maintenanceSchedule = "معايرة دورية لمستشعرات قياس النبض وجريان الدم وضغط الأساور شهرياً",
                    nextMaintenance = "2026-06-25"
                ),
                MedicalEquipment(
                    id = "EQ-104",
                    nameAr = "جهاز تخدير رئوي متنقل وعسكري",
                    nameEn = "Portable Field Anesthesia Machine",
                    totalCount = 4,
                    activeCount = 2,
                    unit = "وحدة الصدمات المتنقلة",
                    status = "تحت الصيانة الميدانية",
                    serialNumber = "MIL-AN-310",
                    category = "أكسجين وتخدير",
                    maintenanceSchedule = "دورة تفريغ صمامات الأمان الضامنة وفحص تسرّب الهالوثين/الغازات أسبوعياً",
                    nextMaintenance = "2026-06-10"
                ),
                MedicalEquipment(
                    id = "EQ-105",
                    nameAr = "أجهزة الأشعة فوق الصوتية اليدوية",
                    nameEn = "Handheld Ultrasound Devices",
                    totalCount = 6,
                    activeCount = 6,
                    unit = "وحدة الطوارئ ER",
                    status = "يعمل بالكامل",
                    serialNumber = "MIL-US-014",
                    category = "تشخيصي",
                    maintenanceSchedule = "فحص ومعايرة الرأس ومستوى شحن البطارية الاحتياطية وتحديث السوفتوير ربع ربع سنوي",
                    nextMaintenance = "2026-07-01"
                ),
                MedicalEquipment(
                    id = "EQ-106",
                    nameAr = "حقائب الجراحة السريرية المتقدمة",
                    nameEn = "Advanced Surgical Instruments Packs",
                    totalCount = 20,
                    activeCount = 17,
                    unit = "مستشفى ميداني (ألفا)",
                    status = "نقص قطع مفقودة",
                    serialNumber = "MIL-SP-750",
                    category = "جراحي",
                    maintenanceSchedule = "تنظيف بالموجات فوق الصوتية وتعقيم حراري رطوبي Autoclave فوري بعد كل استعمال",
                    nextMaintenance = "بشكل فوري بعد الاستعمال"
                )
            )
        )
    }

    // Interactive filter states of units
    val unitsList = listOf("الكل", "وحدة الطوارئ ER", "مستشفى ميداني (ألفا)", "قاعدة التدريب (جاما)", "وحدة الصدمات المتنقلة")
    var selectedUnit by remember { mutableStateOf("الكل") }

    // Category guide filters
    val categoriesList = listOf("الكل", "دعم الحياة", "أكسجين وتنفس", "تشخيصي", "أكسجين وتخدير", "جراحي")
    var selectedCategory by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    // Interacting dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var inputNameAr by remember { mutableStateOf("") }
    var inputNameEn by remember { mutableStateOf("") }
    var inputUnit by remember { mutableStateOf("وحدة الطوارئ ER") }
    var inputTotalQuantity by remember { mutableStateOf("") }
    var inputWorkingQuantity by remember { mutableStateOf("") }
    var inputStatus by remember { mutableStateOf("يعمل بالكامل") }

    // Edit/Update status Dialog states
    var showUpdateDialog by remember { mutableStateOf<MedicalEquipment?>(null) }
    var updateActiveCount by remember { mutableStateOf("") }
    var updateStatus by remember { mutableStateOf("") }

    // Filter list according to selection, category, and search query
    val filteredEquipment = remember(equipmentList, selectedUnit, selectedCategory, searchQuery) {
        equipmentList.filter { item ->
            val matchesUnit = (selectedUnit == "الكل" || item.unit == selectedUnit)
            val matchesCategory = (selectedCategory == "الكل" || item.category == selectedCategory)
            val matchesSearch = (searchQuery.isBlank() ||
                item.nameAr.contains(searchQuery, ignoreCase = true) ||
                item.nameEn.contains(searchQuery, ignoreCase = true) ||
                item.serialNumber.contains(searchQuery, ignoreCase = true))
            matchesUnit && matchesCategory && matchesSearch
        }
    }

    // Computed Stats for Summary Progress bar card
    val totalDevices = remember(filteredEquipment) { filteredEquipment.sumOf { it.totalCount } }
    val activeDevices = remember(filteredEquipment) { filteredEquipment.sumOf { it.activeCount } }
    val overallEfficiency = remember(totalDevices, activeDevices) {
        if (totalDevices > 0) activeDevices.toFloat() / totalDevices.toFloat() else 0f
    }
    val itemsNeedAttention = remember(filteredEquipment) {
        filteredEquipment.count { it.activeCount < it.totalCount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مخزون وحالة التجهيزات الطبية",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Text(
                            text = "لوحة متابعة ومراقبة كفاءة الأجهزة العسكرية",
                            fontSize = 11.sp,
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
                    containerColor = Primary,
                    titleContentColor = TextGold
                )
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
                        listOf(Primary, Color(0xFF071424), Color(0xFF0F1F33))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
            ) {
                // Main Overall Military Standard Status Card
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("efficiency_summary_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "مستوى الجاهزية التشغيلية للمستشفيات 🛡️",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Overall progress circle or visually attractive horizontal bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "جهاز يعمل: $activeDevices / $totalDevices",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = String.format("%.1f%%", overallEfficiency * 100),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallEfficiency > 0.85f) Color(0xFF2ECC71) else TextOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Beautiful heavy progress bar representing overall inventory efficiency state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(7.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(overallEfficiency)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    if (overallEfficiency < 0.6f) Color(0xFFC0392B) else Color(0xFFE67E22),
                                                    if (overallEfficiency >= 0.8f) Color(0xFF2ECC71) else Secondary
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$itemsNeedAttention",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (itemsNeedAttention > 0) TextOrange else Color(0xFF2ECC71)
                                    )
                                    Text(
                                        text = "تجهيزات نقص/أعطال",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(1.dp, 30.dp)
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = selectedUnit,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                    Text(
                                        text = "الموقع النشط حالياً",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter tabs item
                item {
                    Text(
                        text = "🔎 اختيار الوحدة العسكرية / المستشفى:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Custom chips logic using lazy row concept but dynamic to scroll or fit
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // We use a scrollable list of unit chips
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(unitsList) { unit ->
                                    val isSelected = (unit == selectedUnit)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Secondary else Color(0x1F162540))
                                            .border(
                                                1.dp,
                                                if (isSelected) SecondaryLight else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedUnit = unit }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = unit,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Primary else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search filter and Category guide section
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "🔍 ابحث باسم ومواصفات الجهاز أو الرقم التسلسلي:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("مثال: Defibrillator, Ventilator, MIL-...", fontSize = 11.sp, color = TextSecondary) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary),
                            leadingIcon = { Icon(Icons.Default.Search, "بحث عتاد", tint = TextGold) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("equipment_search_input"),
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

                        Text(
                            text = "🏷️ فئة الأجهزة الطبية العسكرية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(categoriesList) { cat ->
                                val isSelected = (cat == selectedCategory)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) TextGold else Color(0x1F162540))
                                        .border(
                                            1.dp,
                                            if (isSelected) TextGold else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Inventory Listing Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 عتاد الأجهزة الميدانية المتابع (${filteredEquipment.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_equipment_button")
                        ) {
                            Text("إضافة جهاز ➕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                }

                // Equipment Items List
                if (filteredEquipment.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📡", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد أجهزة مضافة لهذه الوحدة حالياً.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredEquipment) { equip ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11132237)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .testTag("equipment_card_${equip.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                // Title row and Status Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = equip.nameAr,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = equip.nameEn,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TextGold.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = equip.category,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextGold
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Status Badge Box
                                    val isFullyOperational = equip.activeCount == equip.totalCount
                                    val badgeColor = if (isFullyOperational) Color(0xFF2ECC71) else TextOrange
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = equip.status,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Visual Progress Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "معدل المتوفر والجاهز:",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${equip.activeCount} / ${equip.totalCount} يعمل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Level Progress Bar component
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(equip.operationalPercentage)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        if (equip.operationalPercentage < 0.5f) Color(0xFFC0392B) else SecondaryLite(equip.operationalPercentage),
                                                        if (equip.operationalPercentage >= 0.85f) Color(0xFF2ECC71) else Secondary
                                                    )
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Detailed Maintenance Schedule Block
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "⏱️ جدول الصيانة الدورية المانعة للجراية:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = equip.maintenanceSchedule,
                                            fontSize = 10.5.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "موعد المعايرة والصيانة القادم:",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = equip.nextMaintenance,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextOrange
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bottom Meta row & Update Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(Secondary, shape = RoundedCornerShape(2.5.dp))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "الوحدة: " + equip.unit,
                                                fontSize = 9.5.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "الرقم التسلسلي: " + equip.serialNumber,
                                            fontSize = 8.5.sp,
                                            color = TextSecondary.copy(alpha = 0.6f)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            showUpdateDialog = equip
                                            updateActiveCount = equip.activeCount.toString()
                                            updateStatus = equip.status
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F162540)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Secondary.copy(alpha = 0.3f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("تعديل الحالة ✏️", fontSize = 9.5.sp, color = TextGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Dialog for Adding Equipment ---
            if (showAddDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showAddDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight()
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Secondary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "تسجيل جهاز طبي عسكري جديد 🛡️",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = Secondary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Form fields
                            OutlinedTextField(
                                value = inputNameAr,
                                onValueChange = { inputNameAr = it },
                                label = { Text("الاسم بالعربية", fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputNameEn,
                                onValueChange = { inputNameEn = it },
                                label = { Text("الاسم بالإنجليزية (En)", fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Unit placement selector
                            Text(
                                text = "مستشفى التوزيع:",
                                fontSize = 10.sp,
                                color = TextGold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("وحدة الطوارئ ER", "مستشفى ميداني (ألفا)", "وحدة الصدمات المتنقلة").forEach { localUnit ->
                                    val isSel = (inputUnit == localUnit)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Secondary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                            .border(
                                                1.dp,
                                                if (isSel) Secondary else Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { inputUnit = localUnit }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = localUnit,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) TextGold else TextSecondary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputTotalQuantity,
                                    onValueChange = { inputTotalQuantity = it },
                                    label = { Text("العدد الكلي", fontSize = 11.sp, color = TextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Secondary,
                                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                    )
                                )
                                OutlinedTextField(
                                    value = inputWorkingQuantity,
                                    onValueChange = { inputWorkingQuantity = it },
                                    label = { Text("العدد الفعال", fontSize = 11.sp, color = TextSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Secondary,
                                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputStatus,
                                onValueChange = { inputStatus = it },
                                label = { Text("الوضع العام للحالة (مثال: مستقر / يعاني من نقص)", fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showAddDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("إلغاء", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val total = inputTotalQuantity.toIntOrNull() ?: 0
                                        val active = inputWorkingQuantity.toIntOrNull() ?: 0
                                        if (inputNameAr.trim().isEmpty() || inputNameEn.trim().isEmpty() || total <= 0) {
                                            Toast.makeText(context, "الرجاء اكمال البيانات والعدد بشكل صحيح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val validActive = if (active > total) total else active
                                            val newEquip = MedicalEquipment(
                                                id = "EQ-" + (100 + equipmentList.size + 1),
                                                nameAr = inputNameAr.trim(),
                                                nameEn = inputNameEn.trim(),
                                                totalCount = total,
                                                activeCount = validActive,
                                                unit = inputUnit,
                                                status = if (inputStatus.trim().isEmpty()) "يعمل بالكامل" else inputStatus.trim(),
                                                serialNumber = "MIL-MT-" + (100 + (Math.random() * 899).toInt())
                                            )
                                            equipmentList = equipmentList + newEquip
                                            Toast.makeText(context, "تم تسجيل وإضافة التجهيز بنجاح ✓", Toast.LENGTH_SHORT).show()
                                            
                                            // Reset inputs
                                            inputNameAr = ""
                                            inputNameEn = ""
                                            inputTotalQuantity = ""
                                            inputWorkingQuantity = ""
                                            inputStatus = "يعمل بالكامل"
                                            showAddDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("حفظ التجهيز 💾", color = Primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- Dialog for Editing/Updating general Status ---
            showUpdateDialog?.let { currentEquip ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showUpdateDialog = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight()
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Secondary.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "تعديل القياس التشغيلي للجهاز",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentEquip.nameAr,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Secondary.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = updateActiveCount,
                                onValueChange = { updateActiveCount = it },
                                label = { Text("العدد الفعال الحالي (الحد الأقصى ${currentEquip.totalCount})", fontSize = 11.sp, color = TextSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = updateStatus,
                                onValueChange = { updateStatus = it },
                                label = { Text("ملاحظة الوضع الفني والتشغيلي", fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showUpdateDialog = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("إلغاء", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val active = updateActiveCount.toIntOrNull() ?: 0
                                        if (active < 0 || active > currentEquip.totalCount) {
                                            Toast.makeText(context, "الرجاء إدخال عدد صالح بين 0 و ${currentEquip.totalCount}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            equipmentList = equipmentList.map { equip ->
                                                if (equip.id == currentEquip.id) {
                                                    equip.copy(
                                                        activeCount = active,
                                                        status = if (updateStatus.trim().isEmpty()) currentEquip.status else updateStatus.trim()
                                                    )
                                                } else {
                                                    equip
                                                }
                                            }
                                            Toast.makeText(context, "تم تحديث البيانات والمخزون بنجاح ✓", Toast.LENGTH_SHORT).show()
                                            showUpdateDialog = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تحديث الحالة 💾", color = Primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility to pick color progression from red to gold
private fun SecondaryLite(percentage: Float): Color {
    return if (percentage < 0.3f) {
        Color(0xFFC0392B) // High alert Red
    } else if (percentage < 0.75f) {
        Color(0xFFE67E22) // orange warning
    } else {
        Color(0xFFF1C40F) // Gold/yellow progress indicator
    }
}
