package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.model.BookEntry
import com.example.data.OfflineCacheManager
import com.example.data.CachedDoc
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldButton
import com.example.ui.theme.*
import com.example.helper.ExportHelper
import kotlinx.coroutines.launch

data class DrawerFolder(
    val name: String,
    val description: String,
    val icon: String,
    val books: List<BookEntry>
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cacheManager = remember { OfflineCacheManager(context) }
    var cachedDocs by remember { mutableStateOf<List<CachedDoc>>(emptyList()) }

    val sharedPrefs = remember { context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE) }
    var biometricLockEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_lock_enabled", true)) }
    var securityPin by remember { mutableStateOf(sharedPrefs.getString("custom_security_pin", "1447") ?: "1447") }
    
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            cachedDocs = cacheManager.getCachedDocuments()
        }
    }

    val folders = remember {
        listOf(
            DrawerFolder(
                name = "الطب الميداني (Field Medicine)",
                description = "مقررات وأدلة الإسعاف الميداني في الألوية والمستشفيات العسكرية",
                icon = "🛡️",
                books = listOf(
                    BookEntry(chapter = 1, title = "المصطلحات الطبية العسكرية", type = "book", file = "military_medical_terms.pdf", cover_path = "military_terms.png"),
                    BookEntry(chapter = 5, title = "أسس الصيانة الطبية العامة", type = "general", file = "maintenance_general.pdf", cover_path = "m_general.png"),
                    BookEntry(chapter = 5, title = "السلامة الكهربائية في المستشفيات", type = "general", file = "electrical_safety.pdf", cover_path = "safety.png")
                )
            ),
            DrawerFolder(
                name = "الجراحة السريرية (Clinical Surgery)",
                description = "مقررات الجراحة العامة، تشريح الجسم البشري وفسيولوجيا الأعضاء",
                icon = "🔪",
                books = listOf(
                    BookEntry(chapter = 3, title = "المقرر الطبي للجراحة العامة", type = "book", file = "general_surgery.pdf", cover_path = "surgery.png"),
                    BookEntry(chapter = 1, title = "مبادئ علم التشريح البشري", type = "book", file = "anatomy_basics.pdf", cover_path = "anatomy.png"),
                    BookEntry(chapter = 1, title = "علم وظائف الأعضاء الأساسي", type = "book", file = "physiology_basics.pdf", cover_path = "physiology.png")
                )
            ),
            DrawerFolder(
                name = "بروتوكولات الطوارئ (Emergency Protocols)",
                description = "بروتوكولات الحالات الحرجة وإنعاش أجهزة الجسم التنفسية والقلبية",
                icon = "🚨",
                books = listOf(
                    BookEntry(chapter = 5, title = "التقييم الحركي والسريري (العملي) - الجهاز الهيكل العضلي", type = "subject", file = "defib_practical.pdf", cover_path = "defib.png"),
                    BookEntry(chapter = 5, title = "التقييم الرئوي والإنعاش التنفسي (العملي) - الجهاز التنفسي", type = "subject", file = "vent_practical.pdf", cover_path = "ventilator.png"),
                    BookEntry(chapter = 4, title = "طب الأطفال وحديثي الولادة", type = "book", file = "pediatrics.pdf", cover_path = "pediatrics.png")
                )
            )
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Primary,
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Primary, Color(0xFF0F1F33))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF030810), PrimaryLight)))
                            .padding(vertical = 24.dp, horizontal = 20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📂", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "إدارة وثائق المناهج",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مصنف الوثائق والمقررات الطبية المعتمدة",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Divider(color = Secondary.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 8.dp))

                    var expandedFolderIndex by remember { mutableStateOf<Int?>(null) }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🌟 البوابة والخدمات التكتيكية الميدانية:",
                                    fontSize = 12.5.sp,
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                Card(
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        onNavigate("simulation")
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33D4AF37)),
                                    border = BorderStroke(1.dp, TextGold),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🥽", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("معمل ومركز المحاكاة والـ VR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextGold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(TextOrange)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("نشط 🎚️", fontSize = 7.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text("محاكاة الحالات السريرية والتدريب التفاعلي الثلاثي", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }

                                SidebarServiceRow(
                                    icon = "📅",
                                    title = "الخطة الدراسية",
                                    description = "تصفح فصول ومقررات برنامج الطب والجراحة",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("study_plan")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "📚",
                                    title = "المناهج والمقررات الطبية",
                                    description = "المناهج الطبية المعتمدة للرتب والجاهزية",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("diploma")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "🩺",
                                    title = "الدليل الطبي المنظم ومناهج السنوات",
                                    description = "المسارات الأكاديمية وكتيب المنهج",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("directory")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "🏥",
                                    title = "جرد ومعايرة أجهزة الوحدات",
                                    description = "متابعة أجهزة الصدمات والتنفس والجاهزية",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("inventory")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "🔍",
                                    title = "محرك البحث الإسعافي السريري",
                                    description = "البحث الفوري بأسماء وعناوين الكتيبات والمقررات",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("search")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "📋",
                                    title = "حقيبة المهارات السريرية الميدانية",
                                    description = "مستويات جهوزية المعارك وتقييم الميدان",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("skills")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "📸",
                                    title = "فحص وتتبع المعدات بالـ QR",
                                    description = "مسح الأكواد التعريفية وتنزيل الكتيبات",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigate("qr_scanner")
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "💊",
                                    title = "حاسبة جرعات العقاقير الطارئة",
                                    description = "جرعات هامة ومحددة بدقة تكتيكية لمسعفي الميدان",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            try {
                                                val intent = android.content.Intent(context, Class.forName("com.example.DosageActivity"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لم يتم العثور على حاسبة الجرعات", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "🧮",
                                    title = "المقاييس الطبية والعصبية",
                                    description = "حساب قيم غازات الدم، وظائف الكلى و هبوط الوعي",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            try {
                                                val intent = android.content.Intent(context, Class.forName("com.example.CalculatorsActivity"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لم يتم العثور على المقاييس", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )

                                SidebarServiceRow(
                                    icon = "📝",
                                    title = "صياغة تقارير SOAP ونقل الحالات",
                                    description = "توثيق الحالات لضمان جودة الإخلاء الطبي الميداني",
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            try {
                                                val intent = android.content.Intent(context, Class.forName("com.example.ReportsActivity"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لم يتم العثور على تقارير الصياغة", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Secondary.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 8.dp))
                            Text(
                                text = "📁 مجلدات المناهج المصنفة والأجندة:",
                                fontSize = 12.5.sp,
                                color = TextGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }

                        items(folders.size) { index ->
                            val folder = folders[index]
                            val isExpanded = expandedFolderIndex == index

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) Color(0x1F162540) else Color(0x0F112239)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isExpanded) Secondary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedFolderIndex = if (isExpanded) null else index
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(folder.icon, fontSize = 20.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpanded) TextGold else TextPrimary
                                            )
                                            Text(
                                                text = folder.description,
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = if (isExpanded) "▲" else "▼",
                                            color = if (isExpanded) TextGold else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, start = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 6.dp))
                                            folder.books.forEach { book ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0x0DFFFFFF))
                                                        .clickable {
                                                            scope.launch { drawerState.close() }
                                                            onNavigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                                                        }
                                                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("📄", fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = book.title,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.9f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Secondary.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💾 المخزن المؤقت الميداني (أوفلاين):",
                                    fontSize = 12.5.sp,
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold
                                )
                                if (cachedDocs.isNotEmpty()) {
                                    Text(
                                        text = "مسح الكل 🗑️",
                                        fontSize = 10.sp,
                                        color = TextOrange,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            cacheManager.clearCache()
                                            cachedDocs = emptyList()
                                        }
                                    )
                                }
                            }
                        }

                        if (cachedDocs.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x05FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📡", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "لا توجد مستندات مؤقتة حالياً.\nتصفح أي مستند لحفظه تلقائياً للميدان.",
                                            fontSize = 9.5.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(cachedDocs.size) { index ->
                                val doc = cachedDocs[index]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x1F162540)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onNavigate("pdf_viewer/${Uri.encode(doc.title)}/${Uri.encode(doc.filePath)}")
                                        },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📄", fontSize = 16.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.title,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = doc.fileSizeFormatted,
                                                    fontSize = 8.5.sp,
                                                    color = TextSecondary
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(TextSecondary, shape = RoundedCornerShape(2.dp))
                                                )
                                                Text(
                                                    text = if (doc.isVerifiedOffline) "جاهز أوفلاين ✓" else "تحت المزامنة",
                                                    fontSize = 8.5.sp,
                                                    color = if (doc.isVerifiedOffline) Color(0xFF2ECC71) else Color(0xFFF1C40F),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text("⚡", fontSize = 11.sp, color = TextGold)
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030810))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "شعبة طب الكوارث والتدريب العسكري",
                                fontSize = 9.5.sp,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نظام إدارة المناهج الإصدار 2.4",
                                fontSize = 8.5.sp,
                                color = TextSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF030810), Primary, Color(0xFF0F1F33))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x1F162540), RoundedCornerShape(12.dp))
                            .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Text("📁", fontSize = 20.sp)
                    }

                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x1F162540), RoundedCornerShape(12.dp))
                            .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "القائمة الجانبية",
                            tint = TextGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "المَكْتَبَة الطِّبِّيَّة العَسْكَرِيَّة",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "دليلك لصناعة أطباء المستقبل",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تطبيق دليل برنامج الطب البشري — ليس دليلاً تقليدياً، بل مُحفِّزٌ للتميُّز الطبي العسكري!",
                            fontSize = 15.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        GoldButton(
                            text = "ابدأ التصفح",
                            onClick = { onNavigate("diploma") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Triple("📚", "المقررات الدراسية", "diploma"),
                            Triple("🩺", "الدليل المنظم", "directory")
                        ).forEach { (icon, title, route) ->
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(105.dp)
                                    .clickable { onNavigate(route) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = icon, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Triple("🔍", "البحث الطبي", "search"),
                            Triple("📋", "المهارات السريرية", "skills")
                        ).forEach { (icon, title, route) ->
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(105.dp)
                                    .clickable { onNavigate(route) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = icon, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp)
                                .clickable { onNavigate("study_plan") }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📅", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الخطة الدراسية",
                                    fontSize = 13.sp,
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp)
                                .clickable {
                                    val intent = android.content.Intent(context, com.example.DosageActivity::class.java)
                                    context.startActivity(intent)
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "💊", fontSize = 26.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "جرعات الأدوية 💉",
                                    fontSize = 12.sp,
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "حاسبة الجرعات لـ 47 عقار طارئ",
                                    fontSize = 8.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(105.dp)
                                .clickable {
                                    val intent = android.content.Intent(context, com.example.ReportsActivity::class.java)
                                    context.startActivity(intent)
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📋", fontSize = 26.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التقارير الطبية 📝",
                                    fontSize = 12.sp,
                                    color = TextGold,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "صياغة SOAP ونقل السجلات",
                                    fontSize = 8.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable {
                                    val intent = android.content.Intent(context, com.example.CalculatorsActivity::class.java)
                                    context.startActivity(intent)
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🧮", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "المقاييس الطبية والعصبية 🧠",
                                        fontSize = 14.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مقياس غيبوبة غلاسكو GCS، وظائف الكلى eGFR، غازات الدم ABG",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable { onNavigate("inventory") }
                                .testTag("inventory_dashboard_nav_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = "🛡️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "جرد الأجهزة والجاهزية الميدانية 🏥",
                                        fontSize = 14.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "رصد وتتبع الأجهزة والمستلزمات الحيوية طبياً",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable { onNavigate("qr_scanner") }
                                .testTag("qr_scanner_nav_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = "📸", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "فاحص الملصقات والمعدات الطبية (QR) 🔍",
                                        fontSize = 14.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مسح الملصقات التكتيكية بالرمز التعريفي واستدعاء الكتيبات فوراً",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Secondary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🧬", fontSize = 22.sp)
                                        Column {
                                            Text(
                                                text = "التأمين الحيوي والأمان العسكري 🛡️",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextGold
                                            )
                                            Text(
                                                text = "التحقق التلقائي عند تشغيل التطبيق لضمان سرية المستندات والتقارير",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                Divider(color = Secondary.copy(alpha = 0.15f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "حالة قفل التطبيق الذاتي:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (biometricLockEnabled) "مفعل ومؤمن بالبصمة لتفادي تسريب البيانات" else "غير مؤمن - دخول مباشر دون تحقق حامي",
                                            fontSize = 10.sp,
                                            color = if (biometricLockEnabled) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                                        )
                                    }

                                    Switch(
                                        checked = biometricLockEnabled,
                                        onCheckedChange = { isChecked ->
                                            biometricLockEnabled = isChecked
                                            sharedPrefs.edit().putBoolean("biometric_lock_enabled", isChecked).apply()
                                            Toast.makeText(
                                                context,
                                                if (isChecked) "🛡️ تم تفعيل جدار الحماية التكتيكي الحيوي بنجاح!" 
                                                else "⚠️ تم إلغاء تفعيل قفل البصمة. البيانات الآن غير محمية ذاتياً.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Secondary,
                                            checkedTrackColor = Secondary.copy(alpha = 0.4f),
                                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "رمز المرور العسكري الاحتياطي (PIN):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "الرمز الحالي المعتمد: $securityPin",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { 
                                            newPinInput = ""
                                            showPinChangeDialog = true 
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.12f)),
                                        border = BorderStroke(1.dp, Secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("تغيير الرمز 🔐", color = TextGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Secondary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📤", fontSize = 22.sp)
                                        Column {
                                            Text(
                                                text = "تصدير الملفات والتقارير الاحتياطية 💾",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextGold
                                            )
                                            Text(
                                                text = "حفظ ومشاركة الهوامش المدونة والتقارير الطبية المسجلة كملفات خارجية",
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                Divider(color = Secondary.copy(alpha = 0.15f))

                                Text(
                                    text = "قم بتصدير وإرسال كافة الهوامش العلمية والتقييمات المنجزة على هذا الجهاز اللوحي لتدعيم التنسيق والتوثيق الخارجي:",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { 
                                            ExportHelper.triggerExportShare(context, asJson = false)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Secondary.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, Secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text("تصدير نصي (TXT) 📄", color = TextGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { 
                                            ExportHelper.triggerExportShare(context, asJson = true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text("تصدير بيانات (JSON) ⌨️", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showPinChangeDialog) {
                    AlertDialog(
                        onDismissRequest = { showPinChangeDialog = false },
                        containerColor = PrimaryLight,
                        title = {
                            Text(
                                "تغيير الرمز السري الاحتياطي 🔐",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "أدخل رمز مرور عسكري جديد مكون من 4 أرقام للاستخدام عند عدم توفر ميزات الحماية الحيوية بالجهاز:",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                TextField(
                                    value = newPinInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 4) {
                                            newPinInput = input
                                        }
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Secondary,
                                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newPinInput.length == 4) {
                                        securityPin = newPinInput
                                        sharedPrefs.edit().putString("custom_security_pin", newPinInput).apply()
                                        showPinChangeDialog = false
                                        Toast.makeText(context, "تم تغيير رمز الدخول بنجاح! 🟢", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "خطأ: يجب أن يتكون الرمز من 4 أرقام تماماً!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                            ) {
                                Text("حفظ وتحديث 💾", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPinChangeDialog = false }) {
                                Text("إلغاء", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarServiceRow(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(description, fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}
