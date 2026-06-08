package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DataProvider
import com.example.model.BookEntry
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiplomaScreen(onNavigate: (String, Map<String, String>) -> Unit) {
    val context = LocalContext.current
    val repository = remember { DataProvider(context) }
    val chapters = remember { repository.getChapters() }

    // Names map representing curriculum semesters
    val names = remember {
        mapOf(
            1 to "التأسيس في العلوم الأساسية للميدان - الفصل الأول",
            2 to "التأسيس في العلوم الأساسية للميدان - الفصل الثاني",
            3 to "التأسيس في العلوم الأساسية للطب - الفصل الثالث",
            4 to "التأسيس في العلوم الأساسية للطب - الفصل الرابع",
            5 to "التأسيسية بنظام الأجهزة - الفصل الخامس",
            6 to "التأسيسية بنظام الأجهزة - الفصل السادس",
            7 to "التأسيسية بنظام الأجهزة - الفصل السابع",
            8 to "المرحلة السريرية - الفصل الثامن",
            9 to "المرحلة السريرية - الفصل التاسع",
            10 to "المرحلة السريرية - الفصل العاشر",
            11 to "المرحلة السريرية - الفصل الحادي عشر",
            12 to "المرحلة السريرية - الفصل الثاني عشر",
            13 to "المرحلة السريرية - الفصل الثالث عشر"
        )
    }

    // State for Selected Semester Index (0-based list matching chapters 1 to 13)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dynamic Search Query State
    var searchQuery by remember { mutableStateOf("") }

    // Toggle filter: Only show favorites / bookmarked courses
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // Persistent completions tracked in Shared Preferences
    val sharedPrefs = remember { context.getSharedPreferences("diploma_completion_prefs", Context.MODE_PRIVATE) }
    var completedCourses by remember {
        mutableStateOf(sharedPrefs.getStringSet("completed_courses_set", emptySet()) ?: emptySet())
    }

    // Persistent Bookmarked/Favorite courses tracked similarly
    var bookmarkedCourses by remember {
        mutableStateOf(sharedPrefs.getStringSet("bookmarked_courses_set", emptySet()) ?: emptySet())
    }

    // Selected chapter from list
    val currentChapter = remember(selectedTabIndex, chapters) {
        if (selectedTabIndex in chapters.indices) chapters[selectedTabIndex] else null
    }

    // Filter books associated with currently selected chapter
    val currentChapterBooks = remember(currentChapter, repository.allBooks) {
        currentChapter?.let { ch ->
            val num = ch.id.removePrefix("class").toIntOrNull() ?: 1
            repository.allBooks.filter { it.chapter == num }
        } ?: emptyList()
    }

    // Comprehensive Dynamic layout filtering (Synergy of global search + bookmarks + semester tabs)
    val booksToShow = remember(showFavoritesOnly, searchQuery, currentChapterBooks, repository.allBooks, bookmarkedCourses) {
        if (showFavoritesOnly) {
            val favs = repository.allBooks.filter { bookmarkedCourses.contains(it.title) }
            if (searchQuery.isNotBlank()) {
                favs.filter { it.title.contains(searchQuery, ignoreCase = true) }
            } else {
                favs
            }
        } else {
            if (searchQuery.isNotBlank()) {
                repository.allBooks.filter { it.title.contains(searchQuery, ignoreCase = true) }
            } else {
                currentChapterBooks
            }
        }
    }

    // Selected course details info modal
    var selectedCourseDetail by remember { mutableStateOf<BookEntry?>(null) }

    // Dynamic helper routines
    fun getCourseHours(title: String): String {
        return when (title.trim()) {
            "الثقافة الإسلامية – جزء عمَّ" -> "3"
            "علم الأدوية العام" -> "3.25"
            "أسس تمريض" -> "2.25"
            "أساسيات اللغة الإنجليزية" -> "1.25"
            "مبادئ الطب الوقائي" -> "2"
            "مقدمة الباطنية - الأمراض شائعة" -> "3.25"
            "علم التشريح العام" -> "2"
            "علم وظائف الأعضاء العام" -> "3.25"
            "طوارئ الحرب الجرثومية والكيميائية" -> "1"
            "إسعافات أولية" -> "1.25"
            
            "الثقافة القرآنية - السيرة النبوية" -> "2"
            "علم التشريح السريري" -> "3.25"
            "أساسيات الانعاش والطوارئ" -> "3.5"
            "مبادئ إصابات الحروب" -> "3.25"
            "علم الأدوية الطارئة" -> "1.25"
            "أساسيات نقل الدم" -> "1.25"
            "إدارة المراكز الميدانية" -> "1"
            
            "الثقافة القرآنية- الولاية – طبيعة الصراع" -> "3"
            "اللغة العربية" -> "2"
            "مصطلحات طبية (عام)" -> "3"
            "علم الأنسجة العام" -> "2.25"
            "الكيمياء الحيوية العامة" -> "4"
            "علم الأحياء الدقيقة العام" -> "5"
            "علم الطفيليات العامة" -> "5"
            "علم الأمراض العام" -> "6"
            "كيمياء عامة" -> "3"
            
            "الثقافة القرآنية – العقيدة" -> "3"
            "مهارات تشخيصية تقنية" -> "2.5"
            "مقدمة الفحص السريري والتقييم الصحي" -> "2.5"
            "مهارات الاتصال والتواصل" -> "2.5"
            "تقنيات عمليات جراحية (عام)" -> "2.5"
            "أساسيات رعاية الطوارئ الطبية" -> "2.5"
            "فيزيا طبية" -> "3"
            "علم الوبائيات" -> "1"
            "مقدمة رعاية الحالات الحرجة والعناية المركزة" -> "3"
            "علم الإحصاء الطبي" -> "2.5"
            "مقدمة في علوم الكمبيوتر" -> "3"
            
            "الثقافة القرآنية – يوم الفرقان – التربية الايمانية" -> "3"
            "الجهاز الهيكل العضلي" -> "6"
            "الجهاز القلبي والأوعية الدموية" -> "6"
            "الجهاز التنفسي" -> "6"
            "مكارم الأخلاق واخلاقيات المهنة" -> "2"
            
            "الثقافة القرآنية – الأحكام" -> "2"
            "الجهاز الهضمي" -> "6"
            "الجهاز البولي التناسلي" -> "6"
            "الجهاز الدموي واللمفاوي" -> "6"
            "مبادى التغذية العلاجية عام" -> "3"
            
            "الثقافة القرآنية – التأريخ الإسلامي" -> "2"
            "الجهاز الصمائي" -> "6"
            "الجهاز العصبي 1" -> "4"
            "الجهاز العصبي 2" -> "4"
            "علوم عسكرية" -> "4"
            
            "علم النفس السريري (علوم السلوك)" -> "3"
            "الجراحة العامة (1)" -> "9"
            
            "الطب العام 1" -> "9"
            "طب الأطفال 1" -> "9"
            "الطب الوقائي رقم 3" -> "2"
            "تكنولوجيا المعلومات" -> "2"
            
            "التوليد وأمراض النساء رقم 1" -> "9"
            "منهجية البحث والإحصاء الحيوي" -> "2.5"
            "الطب النفسي" -> "4.5"
            
            "الأنف والأذن والحنجرة" -> "2.25"
            "جراحة العظام" -> "2.25"
            "التخدير" -> "2.25"
            "علم الأعصاب" -> "2.25"
            "طب العيون" -> "2.25"
            "الأمراض الجلدية" -> "2.25"
            "الأشعة التشخيصية" -> "2.25"
            "الطب الشرعي وعلم السموم" -> "2.25"
            
            "الطب العام 2" -> "9"
            "الجراحة العامة 2" -> "9"
            "طب الأطفال 2" -> "9"
            
            "التوليد وأمراض النساء 2" -> "9"
            "الاختياري" -> "9"
            "مشروع تخرج" -> "3"
            else -> "3"
        }
    }

    fun getMilitaryReferences(title: String): List<String> {
        val refs = mutableListOf<String>()
        refs.add("منهجية التقييم والتسلسل السريري المعتمد بالخدمات الطبية العسكرية")
        when {
            title.contains("التشريح") || title.contains("الجراحي") || title.contains("الجراحة") || title.contains("تقنيات") -> {
                refs.add("دليل الأطباء العسكريين لإصابات وجراحة الحروب")
                refs.add("بروتوكول التدخل التكتيكي السريع في النقاط الأمامية")
            }
            title.contains("الحرب") || title.contains("الجرثومية") || title.contains("الوقائي") -> {
                refs.add("كتيب الطوارئ ضد الأسلحة الكيميائية والعوامل الحيوية (CBRN)")
                refs.add("دليل الخدمات الوقائية الميدانية للقوات المسلحة")
            }
            title.contains("الأدوية") || title.contains("السموم") -> {
                refs.add("معايير السلامة الدوائية وإدارة ترياق ساحة المعركة")
                refs.add("دليل الاستجابة السريعة لحالات التسمم العسكري والميداني")
            }
            title.contains("الإنعاش") || title.contains("الإسعاف") || title.contains("الحرجة") -> {
                refs.add("بروتوكول الدعم والإسعاف الطبي المتقدم على خط النار الأول (TCCC)")
                refs.add("دليل العناية المركزة وإنقاذ الحياة في المستشفيات الميدانية")
            }
            title.contains("البحث") || title.contains("المشاريع") -> {
                refs.add("لوائح البحث العلمي والتميز الطبي بمستشفيات القوات المسلحة")
            }
            else -> {
                refs.add("لوائح التدريب والتأهيل الطبي بالقوات المسلحة")
            }
        }
        return refs
    }

    // Dynamic statistical progress for the semester progress indicators
    val totalCoursesCount = currentChapterBooks.size
    val completedInChapterCount = remember(currentChapterBooks, completedCourses) {
        currentChapterBooks.count { completedCourses.contains(it.title) }
    }
    val progressPercentage = remember(totalCoursesCount, completedInChapterCount) {
        if (totalCoursesCount > 0) {
            (completedInChapterCount.toFloat() / totalCoursesCount.toFloat())
        } else {
            0f
        }
    }

    val overallProgressPercent = remember(repository.allBooks, completedCourses) {
        if (repository.allBooks.isNotEmpty()) {
            completedCourses.size.toFloat() / repository.allBooks.size.toFloat()
        } else {
            0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040914), Primary, Color(0xFF0F1F33))
                )
            )
            .padding(16.dp)
    ) {
        // App header with unified actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔙",
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onNavigate("home", emptyMap()) }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الأكاديمية الطبية العسكرية 🔬",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGold
                )
                Text(
                    text = "برنامج التدريب والتأهيل لدبلوم الطب البشري المعتمد",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // EXPORT PROGRESS REPORT AS PDF ACTION BUTTON
            Button(
                onClick = {
                    exportProgressAsPdf(
                        context = context,
                        totalCount = repository.allBooks.size,
                        completedCount = completedCourses.size,
                        progressPercent = overallProgressPercent,
                        books = repository.allBooks,
                        completions = completedCourses,
                        namesMap = names
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TextOrange.copy(alpha = 0.2f), contentColor = TextOrange),
                border = BorderStroke(1.dp, TextOrange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("تصدير التقرير PDF 📄", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // REQUESTED FEATURE 1: GLOBAL SEARCH BAR Styled elegantly matching modern glass looks
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "ابحث عن مقرر أو موضوع طبي عسكري...",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.5.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("❌", fontSize = 11.sp)
                    }
                } else {
                    Text("🔍", fontSize = 15.sp)
                }
            }
        )

        // REQUESTED FEATURE 2: FAVORITES & CURRICULUM OPTION FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showFavoritesOnly,
                onClick = { showFavoritesOnly = false },
                label = { Text("📖 كافة المقررات الدراسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Secondary.copy(alpha = 0.15f),
                    selectedLabelColor = Secondary,
                    disabledContainerColor = Color.Transparent,
                    labelColor = TextSecondary
                ),
                border = BorderStroke(1.dp, if (!showFavoritesOnly) Secondary else Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp)
            )

            FilterChip(
                selected = showFavoritesOnly,
                onClick = { showFavoritesOnly = true },
                label = { Text("⭐ المفضلة والمميزة (${bookmarkedCourses.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TextGold.copy(alpha = 0.15f),
                    selectedLabelColor = TextGold,
                    disabledContainerColor = Color.Transparent,
                    labelColor = TextSecondary
                ),
                border = BorderStroke(1.dp, if (showFavoritesOnly) TextGold else Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Conditionally render layout parameters based on Favorites Tab state
        if (!showFavoritesOnly) {
            // REQUESTED FEATURE 3: Tab navigation to individually toggle semesters
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Secondary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Secondary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                chapters.forEachIndexed { index, ch ->
                    val num = ch.id.removePrefix("class").toIntOrNull() ?: 1
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = "الفصل $num",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) TextGold else TextSecondary
                            )
                        }
                    )
                }
            }

            // Progress bar and details card for current chapter
            if (searchQuery.isBlank()) {
                currentChapter?.let { ch ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${ch.icon} ${ch.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Secondary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "المنهج التدريبي المصدق",
                                        fontSize = 8.5.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 رصد التقدم الأكاديمي للفصل:",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${(progressPercentage * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = TextOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            LinearProgressIndicator(
                                progress = { progressPercentage },
                                color = TextOrange,
                                trackColor = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "تم رصد $completedInChapterCount من أصل $totalCoursesCount مساق مكمل في هذا الفصل الدراسي.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // Favorites view header progress
            val totalFavoritesCount = bookmarkedCourses.size
            val completedFavoritesCount = remember(completedCourses, bookmarkedCourses) {
                bookmarkedCourses.count { completedCourses.contains(it) }
            }
            val favoritesProgress = remember(totalFavoritesCount, completedFavoritesCount) {
                if (totalFavoritesCount > 0) completedFavoritesCount.toFloat() / totalFavoritesCount.toFloat() else 0f
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⭐ إدارة المساقات الدراسية المفضلة ذات الأولوية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 رصد التقدم في المواد المفضلة:",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(favoritesProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = TextOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { favoritesProgress },
                        color = TextOrange,
                        trackColor = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // ORGANIZED CHRONOLOGICAL CURRICULUM DISPLAY LIST
        Text(
            text = if (showFavoritesOnly) "⭐ المساقات ذات الأولوية العالية المضافة:" else "📚 قائمة المقررات والمساقات الدراسية:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (booksToShow.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showFavoritesOnly) "لم تقم بإضافة أي مساقات دراسية للمفضلة حتى الآن.\nتصفح المقررات وانقر على النجمة لإضافتها." else "لا توجد نتائج مطابقة لفلترة البحث الجارية.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(booksToShow) { book ->
                    val isChecked = completedCourses.contains(book.title)
                    val isBookmarked = bookmarkedCourses.contains(book.title)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCourseDetail = book }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checked status toggle or standard icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) TextGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isChecked) "✅" else "📖",
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (book.type == "subject") Secondary.copy(alpha = 0.2f) else TextOrange.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (book.type == "subject") "مستوى تخصصي" else "مقرر أساسي",
                                            fontSize = 8.5.sp,
                                            color = if (book.type == "subject") Secondary else TextOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Semester Label for global search
                                    Text(
                                        text = "الفصل ${book.chapter}",
                                        fontSize = 10.5.sp,
                                        color = TextGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${getCourseHours(book.title)} ساعة",
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // REQUESTED FEATURE 2: FAVORITE MICRO-STAR ACTION ON EACH CARD
                            IconButton(
                                onClick = {
                                    val newBookmarks = if (isBookmarked) {
                                        bookmarkedCourses - book.title
                                    } else {
                                        bookmarkedCourses + book.title
                                    }
                                    bookmarkedCourses = newBookmarks
                                    sharedPrefs.edit().putStringSet("bookmarked_courses_set", newBookmarks).apply()

                                    Toast.makeText(
                                        context,
                                        if (!isBookmarked) "تمت إضافة \"${book.title}\" للمفضلة ⭐" else "تمت إزالة \"${book.title}\" من المفضلة.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text(
                                    text = if (isBookmarked) "⭐" else "☆",
                                    fontSize = 19.sp,
                                    color = if (isBookmarked) TextGold else Color.White.copy(alpha = 0.35f)
                                )
                            }

                            // Course completed checkbox
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newCompletions = if (checked) {
                                        completedCourses + book.title
                                    } else {
                                        completedCourses - book.title
                                    }
                                    completedCourses = newCompletions
                                    sharedPrefs.edit().putStringSet("completed_courses_set", newCompletions).apply()

                                    Toast.makeText(
                                        context,
                                        if (checked) "تم رصد مقرر \"${book.title}\" كمكتمل بنجاح!" else "تم إلغاء مقرر \"${book.title}\" كمكتمل.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TextGold,
                                    uncheckedColor = TextSecondary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // REQUESTED FEATURE 1: COMPONENT CORNER DISPLAYING DETAILS ON CLICK WITH MILITARY REFS & STATS
    selectedCourseDetail?.let { book ->
        val hours = getCourseHours(book.title)
        val refs = getMilitaryReferences(book.title)
        val isChecked = completedCourses.contains(book.title)
        val isBookmarked = bookmarkedCourses.contains(book.title)

        AlertDialog(
            onDismissRequest = { selectedCourseDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔎 تفاصيل وبيئة المقرر الدراسي",
                        color = TextGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اسم المقرر المعياري المعتمد:",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "الساعات المعتمدة:",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TextOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$hours ساعات تدريبية",
                                    color = TextOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "الفصل الدراسي:",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Secondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الفصل رقم ${book.chapter}",
                                    color = Secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Selected Semester Broad Title mapping display
                    names[book.chapter]?.let { semesterBrief ->
                        Text(
                            text = "المجال الأكاديمي الحاضن:",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = semesterBrief,
                            color = TextGold,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Text(
                        text = "المراجع الطبية والعسكرية التنظيمية المرتبطة:",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        refs.forEach { ref ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "🎖️",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ref,
                                    color = TextPrimary,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "المنفذ الرقمي الحركي: ${book.file}",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox toggling within the details modal
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val newCompletions = if (isChecked) {
                                    completedCourses - book.title
                                } else {
                                    completedCourses + book.title
                                }
                                completedCourses = newCompletions
                                sharedPrefs.edit().putStringSet("completed_courses_set", newCompletions).apply()
                            }
                            .padding(end = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val newCompletions = if (checked) {
                                    completedCourses + book.title
                                } else {
                                    completedCourses - book.title
                                }
                                completedCourses = newCompletions
                                sharedPrefs.edit().putStringSet("completed_courses_set", newCompletions).apply()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = TextGold)
                        )
                        Text(
                            text = if (isChecked) "مكتمل ✔️" else "غير منجز 🛑",
                            color = if (isChecked) TextGold else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        TextButton(
                            onClick = {
                                val target = selectedCourseDetail
                                if (target != null) {
                                    selectedCourseDetail = null
                                    onNavigate("pdf_viewer", mapOf("title" to target.title, "file" to target.file))
                                }
                            }
                        ) {
                            Text("قراءة فورية 📚", color = Secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = { selectedCourseDetail = null }) {
                            Text("إغلاق", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            },
            containerColor = PrimaryLight,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Native Multi-page PDF Report Generation Flow
 * Does not require external dependencies. Handles RTL text shaping gracefully using StaticLayout.
 */
fun exportProgressAsPdf(
    context: Context,
    totalCount: Int,
    completedCount: Int,
    progressPercent: Float,
    books: List<BookEntry>,
    completions: Set<String>,
    namesMap: Map<Int, String>
) {
    try {
        val pdfDocument = PdfDocument()
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.BLACK
        }

        var pageNumber = 1
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Setup Draw Page Header Function
        fun drawPageHeader(c: Canvas, pNum: Int) {
            val greenForest = GColor.parseColor("#0F1F33")
            val goldColor = GColor.parseColor("#C5A059")

            // Rect top stripe layout
            val paintBorder = Paint().apply {
                color = greenForest
                style = Paint.Style.FILL
            }
            c.drawRect(30f, 20f, 565f, 40f, paintBorder)

            val paintGold = Paint().apply {
                color = goldColor
                style = Paint.Style.FILL
            }
            c.drawRect(30f, 40f, 565f, 44f, paintGold)

            val hPaint = TextPaint().apply {
                isAntiAlias = true
                color = GColor.parseColor("#1B2A47")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Arabic Header (RTL ALIGNED)
            val rightLayout = StaticLayout(
                "الجمهورية اليمنية\nوزارة الدفاع - الخدمات الطبية العسكرية\nإدارة المناهج والتعليم الطبي الأكاديمي",
                hPaint,
                240,
                Layout.Alignment.ALIGN_OPPOSITE,
                1.15f,
                0.0f,
                false
            )
            c.save()
            c.translate(310f, 55f)
            rightLayout.draw(c)
            c.restore()

            // English Institutional Metadata (LTR ALIGNED)
            val leftLayout = StaticLayout(
                "Yemen Republic\nMinistry of Defense - Medical Services\nDepartment of Academic & Training",
                hPaint,
                240,
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                0.0f,
                false
            )
            c.save()
            c.translate(45f, 55f)
            leftLayout.draw(c)
            c.restore()

            val paintLine = Paint().apply {
                color = GColor.LTGRAY
                strokeWidth = 1f
            }
            c.drawLine(30f, 115f, 565f, 115f, paintLine)
        }

        // Draw standard footer with pagings
        fun drawPageFooter(c: Canvas, pNum: Int) {
            val fPaint = TextPaint().apply {
                isAntiAlias = true
                color = GColor.GRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val paintLine = Paint().apply {
                color = GColor.LTGRAY
                strokeWidth = 1f
            }
            c.drawLine(30f, 802f, 565f, 802f, paintLine)

            c.drawText("التعليم الطبي العسكري الأكاديمي الموحد - 2026", 45f, 816f, fPaint)
            c.drawText("صفحة $pNum", 520f, 816f, fPaint)
        }

        drawPageHeader(canvas, pageNumber)

        // Draw main title bar
        val titleRectPaint = Paint().apply {
            color = GColor.parseColor("#F5F7FA")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(45f, 130f, 550f, 170f, 6f, 6f, titleRectPaint)

        val titleBorderPaint = Paint().apply {
            color = GColor.parseColor("#C5A059")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(45f, 130f, 550f, 170f, 6f, 6f, titleBorderPaint)

        val docTitlePaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.parseColor("#0F1F33")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout(
            "بيان وإثـبات منجـز كشـف المنهـج الأكاديمي لدبلـوم الطب العسكري",
            docTitlePaint,
            490,
            Layout.Alignment.ALIGN_CENTER,
            1.1f,
            0.0f,
            false
        )
        canvas.save()
        canvas.translate(50f, 142f)
        titleLayout.draw(canvas)
        canvas.restore()

        var currentY = 185f

        // Render administrative student status details
        val metaPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.BLACK
            textSize = 9.5f
        }

        val metaLeft = "الحساب الدراسي الفعال: ansalshwby1447@gmail.com\nتاريخ الطباعة والرصد: 2026-06-07\nجهة المطابقة: الشؤون الأكاديمية العسكرية"
        val metaLeftLayout = StaticLayout(metaLeft, metaPaint, 240, Layout.Alignment.ALIGN_NORMAL, 1.25f, 0.0f, false)
        canvas.save()
        canvas.translate(45f, currentY)
        metaLeftLayout.draw(canvas)
        canvas.restore()

        val metaRight = "نمط البرنامج: دبلوم الطب البشري العسكري التخصصي\nطريقة الرصد والترشيح: تتبع ذاتي إلكتروني موثق\nالساعات الإجمالية للمنهج: 180 ساعة معتمدة"
        val metaRightLayout = StaticLayout(metaRight, metaPaint, 240, Layout.Alignment.ALIGN_OPPOSITE, 1.25f, 0.0f, false)
        canvas.save()
        canvas.translate(310f, currentY)
        metaRightLayout.draw(canvas)
        canvas.restore()

        currentY += 56f

        // Render progress board summary details inside a beautiful native card block
        val progressCardPaint = Paint().apply {
            color = GColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(45f, currentY, 550f, currentY + 70f, 8f, 8f, progressCardPaint)

        val progressCardBorder = Paint().apply {
            color = GColor.parseColor("#E4E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(45f, currentY, 550f, currentY + 70f, 8f, 8f, progressCardBorder)

        val metricTitlePaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.parseColor("#5A6B82")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metricValPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.parseColor("#102A43")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("معدل اجتياز المنهج العام", 60f, currentY + 20f, metricTitlePaint)
        canvas.drawText("${(progressPercent * 100).toInt()}%", 60f, currentY + 42f, metricValPaint)

        canvas.drawText("المساقات المنجزة والمصدقة", 230f, currentY + 20f, metricTitlePaint)
        canvas.drawText("$completedCount من $totalCount مساقات", 230f, currentY + 42f, metricValPaint)

        canvas.drawText("المساقات المتبقية للدراسة", 400f, currentY + 20f, metricTitlePaint)
        canvas.drawText("${totalCount - completedCount} مساق متبقٍ", 400f, currentY + 42f, metricValPaint)

        // Progress bar indicators inside the card
        val barPaint = Paint().apply {
            color = GColor.parseColor("#ECEFF1")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(60f, currentY + 52f, 530f, currentY + 60f, 3f, 3f, barPaint)

        barPaint.color = GColor.parseColor("#FF9800")
        val computedExtent = 60f + (470f * progressPercent)
        if (computedExtent > 60f) {
            canvas.drawRoundRect(60f, currentY + 52f, computedExtent, currentY + 60f, 3f, 3f, barPaint)
        }

        currentY += 86f

        // Table Header
        val listHeaderPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.parseColor("#0F1F33")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val listHeaderLayout = StaticLayout(
            "📋 كشف تفصيلي بالمساقات والمقررات التي تم إنجازها بنجاح:",
            listHeaderPaint,
            490,
            Layout.Alignment.ALIGN_OPPOSITE,
            1.0f,
            0.0f,
            false
        )
        canvas.save()
        canvas.translate(50f, currentY)
        listHeaderLayout.draw(canvas)
        canvas.restore()

        currentY += 21f

        // Render Table Headers
        val tblHeaderPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.parseColor("#0F1F33")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawRect(45f, currentY, 550f, currentY + 20f, titleRectPaint)
        canvas.drawRect(45f, currentY, 550f, currentY + 20f, progressCardBorder)

        canvas.drawText("حالة المساق الأكاديمي", 60f, currentY + 14f, tblHeaderPaint)
        canvas.drawText("ساعات معتمدة", 220f, currentY + 14f, tblHeaderPaint)
        canvas.drawText("الفصل", 320f, currentY + 14f, tblHeaderPaint)
        canvas.drawText("اسم المادة الطبية العسكرية", 400f, currentY + 14f, tblHeaderPaint)

        currentY += 20f

        val completedBooks = books.filter { completions.contains(it.title) }

        if (completedBooks.isEmpty()) {
            val emptyPaint = TextPaint().apply {
                isAntiAlias = true
                color = GColor.GRAY
                textSize = 10.5f
            }
            val emptyLayout = StaticLayout(
                "لم يتم تكملة أو تحديد أي مساقات أكاديمية كمنجزة حتى اللحظة.\nيرجى العودة لبرنامج دبلوم الطب وتحديد المواد الطبية المكتملة ليتم رصدها إجرائياً وعرضها في هذا الكشف المصدق.",
                emptyPaint,
                480,
                Layout.Alignment.ALIGN_OPPOSITE,
                1.3f,
                0.0f,
                false
            )
            canvas.save()
            canvas.translate(55f, currentY + 16f)
            emptyLayout.draw(canvas)
            canvas.restore()
            currentY += 75f
        } else {
            val rowPaint = TextPaint().apply {
                isAntiAlias = true
                color = GColor.parseColor("#334E68")
                textSize = 9f
            }

            fun getCourseHours(title: String): Int {
                return when {
                    title.contains("الثقافة") -> 2
                    title.contains("اللغة") -> 2
                    title.contains("التشريح") || title.contains("الجراح") || title.contains("الجراحة") || title.contains("الباطنية") -> 4
                    title.contains("البحث") || title.contains("الإحصاء") -> 2
                    else -> 3
                }
            }

            completedBooks.forEach { completedBook ->
                if (currentY + 24f > 720f) {
                    drawPageFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas = page.canvas
                    drawPageHeader(canvas, pageNumber)

                    currentY = 135f
                    canvas.drawRect(45f, currentY, 550f, currentY + 20f, titleRectPaint)
                    canvas.drawRect(45f, currentY, 550f, currentY + 20f, progressCardBorder)
                    canvas.drawText("حالة المساق الأكاديمي", 60f, currentY + 14f, tblHeaderPaint)
                    canvas.drawText("ساعات معتمدة", 220f, currentY + 14f, tblHeaderPaint)
                    canvas.drawText("الفصل", 320f, currentY + 14f, tblHeaderPaint)
                    canvas.drawText("اسم المادة الطبية العسكرية", 400f, currentY + 14f, tblHeaderPaint)
                    currentY += 20f
                }

                val rowDivider = Paint().apply {
                    color = GColor.parseColor("#E4E7EB")
                    strokeWidth = 0.5f
                }
                canvas.drawLine(45f, currentY + 20f, 550f, currentY + 20f, rowDivider)

                canvas.drawText("مكتمل ومصدق مرصود ✔️", 60f, currentY + 13f, rowPaint)
                canvas.drawText("${getCourseHours(completedBook.title)} ساعات معتمدة", 220f, currentY + 13f, rowPaint)
                canvas.drawText("الفصل الدراسي ${completedBook.chapter}", 310f, currentY + 13f, rowPaint)

                val nameLayout = StaticLayout(
                    completedBook.title,
                    rowPaint,
                    150,
                    Layout.Alignment.ALIGN_OPPOSITE,
                    1.0f,
                    0.0f,
                    false
                )
                canvas.save()
                canvas.translate(390f, currentY + 3f)
                nameLayout.draw(canvas)
                canvas.restore()

                currentY += 20f
            }
        }

        // Administrative Authorization & Signature panel spacing check
        if (currentY + 110f > 720f) {
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            pageNumber++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            drawPageHeader(canvas, pageNumber)
            currentY = 135f
        }

        currentY += 30f

        val sigPaint = TextPaint().apply {
            isAntiAlias = true
            color = GColor.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerTextLeft = "عميد الكلية الأكاديمية الطبية العسكرية\n\nتوقيع ومصادقة: ____________________"
        val footerLeftLayout = StaticLayout(footerTextLeft, sigPaint, 240, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false)
        canvas.save()
        canvas.translate(45f, currentY)
        footerLeftLayout.draw(canvas)
        canvas.restore()

        val footerTextRight = "مدير الهيئة الطبية والتعليم التخصصي العسكري\n\nخاتم الدائرة الرسمي: ____________________"
        val footerRightLayout = StaticLayout(footerTextRight, sigPaint, 240, Layout.Alignment.ALIGN_OPPOSITE, 1.2f, 0.0f, false)
        canvas.save()
        canvas.translate(310f, currentY)
        footerRightLayout.draw(canvas)
        canvas.restore()

        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Save generated bytes safely to documents directory
        val file = File(context.cacheDir, "Yemen_Military_Medical_Curriculum_Report_2026.pdf")
        val outStream = FileOutputStream(file)
        pdfDocument.writeTo(outStream)
        pdfDocument.close()
        outStream.close()

        // Distribute viewing Intent via System FileProvider cleanly
        val authority = "${context.packageName}.fileprovider"
        val fileUri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "افتح تقرير رصد تقدم المنهج الدراسي عسكرياً"))

        Toast.makeText(context, "تم تصدير تقرير تقدم المنهج الدراسي بنجاح بصيغة PDF! 📄", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "تعذر تصدير تقرير PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
