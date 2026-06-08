package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.helper.GeminiHelper
import com.example.model.BookEntry
import com.example.ui.components.Book3DCard
import com.example.ui.components.GlassCard
import com.example.ui.components.Shelf
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Chat message structure for assistant
data class ChatMessage(
    val senderIsUser: Boolean,
    val text: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToPdf: (BookEntry) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DataProvider(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var activeTab by remember { mutableIntStateOf(0) } // 0: Search textbooks, 1: AI Assistant
    
    // TEXT SEARCH STATES
    var query by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<BookEntry?>(null) }
    
    val results = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            repository.allBooks.filter { 
                it.title.contains(query, ignoreCase = true) ||
                it.file.contains(query, ignoreCase = true)
            }
        }
    }

    // AI CURRICULUM SEARCH STATES (Tab 0)
    var searchMode by remember { mutableIntStateOf(0) } // 0: Local Title Match, 1: AI Curriculum Search
    var isAiSearching by remember { mutableStateOf(false) }
    var aiSearchResult by remember { mutableStateOf("") }

    fun runAiCurriculumSearch() {
        if (query.isBlank()) return
        isAiSearching = true
        aiSearchResult = ""
        coroutineScope.launch {
            try {
                // Synthesize active books listing for Gemini context
                val booksContext = repository.allBooks.mapIndexed { index, b ->
                    "${index + 1}. العنوان: \"${b.title}\" | الفصل رقم: ${b.chapter} | ملف القراءة: ${b.file}"
                }.joinToString("\n")

                val promptText = """
                    لقد طلب الطالب البحث واستخلاص المعلومات الطبيّة والفنيّة للموضوع التالي عبر مكتبة المناهج الطبية وصيانة الأجهزة:
                    "$query"

                    إليك الفهرس المرجعي الكامل للكتب والأدلة المتوفرة لدينا:
                    $booksContext

                    مهمتك هي إجراء فحص بحثي شامل لهذه العناوين كطبيب ومهندس إكلينيكي عسكري ومساعد دراسي ذكي.
                    من فضلك صغ الرد والمطابقة بدقة وأثرها معرفياً وعلمياً باللغة العربية، موزعاً الفكرة على الأقسام الثلاثة التالية:

                    1. 📑 **المطابقة المنهجية والأدلة الموصى بها:**
                       حدد بدقة أي الكتب من الفهرس المذكور أعلاه هي الأكثر أهمية وفائدة لدراسة هذا الاستفسار، وبأي الفصول توجد.

                    2. 🩺 **الخلاصة العلمية المركزة وسيناريو الإجراء الفني:**
                       اشرح المفاهيم الطبية والسريرية، أو طرق التشغيل والمعايرة للأجهزة الطبية المتعلقة بالموضوع بأسلوب أكاديمي مشوق وبسيط (القيم الطبيعية، المشاكل والحلول، خطوات الصيانة والوقاية). نسق النقاط لتكون واضحة باستخدام التعداد النقطي (•).

                    3. ⚠️ **التوصيات السريرية وتحذيرات العمل الميداني:**
                       أعط توجيهات عملية لحفظ الأرواح في الميدان العسكري أو صيانة الأجهزة لتفادي الأعطال الحرجة بالمستشفيات الميدانية.
                """.trimIndent()

                val response = GeminiHelper.askGemini(promptText)
                aiSearchResult = response
            } catch (e: Exception) {
                aiSearchResult = "عذراً، حدث خطأ أثناء الاتصال بمحرك البحث المنهجي الذكي: ${e.message}"
            } finally {
                isAiSearching = false
            }
        }
    }

    // AI ASSISTANT STATES
    var aiQuery by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    val chatHistory = remember { 
        mutableStateListOf<ChatMessage>().apply {
            add(ChatMessage(
                senderIsUser = false,
                text = "مرحباً بك! أنا مساعد الأستاذ الطبي الذكي (جيميني جين) 🩺.\nأنا مهيأ للإجابة عن أسئلتك السريرية ومعايرات الأجهزة الطبية من واقع المقررات والمرفقات بدقة وعناية.\n\n*يمكنك استخدام الأسئلة الجاهزة بالأسفل أو كتابة تساؤلك الفني مباشرة!*"
            ))
        }
    }

    // Voice Input Speech-To-Text Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val resultsList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!resultsList.isNullOrEmpty()) {
                val spokenText = resultsList[0]
                if (activeTab == 0) {
                    query = spokenText
                } else {
                    aiQuery = spokenText
                }
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن للبحث السريري والمنهجي 🎤")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "التعرف على الصوت غير مدعوم على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    val suggestionQuestions = listOf(
        "كيفية معايرة أجهزة الصدمات الكهربائية؟",
        "أساسيات فحص غازات الدم الشرياني ABG",
        "دليل تشغيل وصيانة أجهزة التنفس الاصطناعي",
        "أبرز أسباب إنذارات أجهزة التنفس الاصطناعي وكيفية حلها",
        "طريقة إجراء تخطيط القلب الكهربائي ECG",
        "معلومات تشريح عضلة القلب"
    )

    fun askAssistant(promptText: String) {
        if (promptText.isBlank()) return
        chatHistory.add(ChatMessage(senderIsUser = true, text = promptText))
        aiQuery = "" // Reset field
        isAiLoading = true
        
        coroutineScope.launch {
            try {
                val response = GeminiHelper.askGemini(promptText)
                chatHistory.add(ChatMessage(senderIsUser = false, text = response))
            } catch (e: Exception) {
                chatHistory.add(ChatMessage(senderIsUser = false, text = "عذراً، حدث خطأ أثناء الاتصال بالمحرك الذكي: ${e.message}"))
            } finally {
                isAiLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF02070F), Primary, Color(0xFF0C1929)))
            )
            .padding(16.dp)
    ) {
        
        // Appbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
                text = if (activeTab == 0) "البحث والمطابقة السريعة" else "مساعد الأستاذ الطبي الذكي 🩺",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Beautiful Navigation Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0x12FFFFFF),
            contentColor = Secondary,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("🔍 بحث المقررات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeTab == 0) TextGold else Color.White) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("🩺 مساعد الأستاذ (جيميني)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (activeTab == 1) TextGold else Color.White) }
            )
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == 0) {
                // TAB 0: CONSOLIDATED LOCAL AND AI SEARCH
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Input Row/TextField
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("ابحث عن كتاب، مقرر، أو جهاز طبي...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "ابحث", tint = Secondary) },
                        trailingIcon = {
                            IconButton(onClick = { startVoiceInput() }) {
                                Text("🎤", fontSize = 20.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Secondary,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0x10FFFFFF),
                            unfocusedContainerColor = Color(0x10FFFFFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Mode Choices Bar (Chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title Match Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (searchMode == 0) Secondary.copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                .border(1.dp, if (searchMode == 0) Secondary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { searchMode = 0 }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔍 مطابقة العناوين فورا",
                                color = if (searchMode == 0) TextGold else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // AI Search Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (searchMode == 1) Secondary.copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                .border(1.dp, if (searchMode == 1) Secondary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { searchMode = 1 }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🧠 البحث المنهجي بـ Gemini",
                                color = if (searchMode == 1) TextGold else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (searchMode == 0) {
                        // LOCAL TITLE MATCHING VIEW
                        if (query.isBlank()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "ابدأ بكتابة اسم المنهج أو الفصل للبحث فورا",
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "لم يتم العثور على نتائج لـ \"$query\"",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val chunkedResults = results.chunked(3)
                                items(chunkedResults.size) { shelfIndex ->
                                    Shelf {
                                        chunkedResults[shelfIndex].forEach { book ->
                                            Book3DCard(
                                                bookTitle = book.title,
                                                coverPath = book.cover_path,
                                                activeBaseDir = repository.activeBaseDir,
                                                onClick = { selectedBook = book }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // AI SEMANTIC CURRICULUM SEARCH VIEW
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Run Action Button
                            Button(
                                onClick = { runAiCurriculumSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = Secondary, contentColor = Primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                enabled = query.isNotBlank() && !isAiSearching
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("🔍 استفسر منهجياً بـ Gemini API", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            if (query.isBlank()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🧠", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "أدخل موضوع الطبي الفني أو السريري بالأعلى\nثم اضغط على زر البحث الذكي عبر Gemini API",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            } else if (isAiSearching) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33E67E22)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .border(1.dp, TextOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = TextOrange,
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "جاري تحليل ومطابقة المناهج... 🩺",
                                                fontSize = 12.sp,
                                                color = TextOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (aiSearchResult.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0x1F112239)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    ) {
                                                        Text("💡", fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "نتائج الفحص والتحليل المنهجي الذكي:",
                                                            color = TextGold,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    
                                                    // Plain markdown display
                                                    Text(
                                                        text = aiSearchResult,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        lineHeight = 20.sp,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Divider(color = Color.White.copy(alpha = 0.15f))
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    // APK key warned correctly in response item
                                                    Text(
                                                        text = "**Security Warning**: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                                                        color = TextPrimary.copy(alpha = 0.5f),
                                                        fontSize = 9.sp,
                                                        lineHeight = 12.sp,
                                                        fontWeight = FontWeight.Light,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Local Matches Shelves underneath Gemini results for immediate access
                                    if (results.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "📚 كتب ومناهج ذات صلة متصلة بالبحث (انقر لفتحها):",
                                                color = TextGold,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }

                                        val chunkedResults = results.chunked(3)
                                        items(chunkedResults.size) { shelfIndex ->
                                            Shelf {
                                                chunkedResults[shelfIndex].forEach { book ->
                                                    Book3DCard(
                                                        bookTitle = book.title,
                                                        coverPath = book.cover_path,
                                                        activeBaseDir = repository.activeBaseDir,
                                                        onClick = { selectedBook = book }
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
            } else {
                // TAB 1: AI ASSISTANT WORKSPACE
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // Chat Messages Scroll Area
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        reverseLayout = false,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatHistory) { msg ->
                            val alignment = if (msg.senderIsUser) Alignment.End else Alignment.Start
                            val bubbleColor = if (msg.senderIsUser) Color(0xFF1B314B) else Color(0x22FFFFFF)
                            val textColor = if (msg.senderIsUser) Color.White else Color.White
                            val borderColor = if (msg.senderIsUser) Secondary else Color.White.copy(alpha = 0.15f)

                            Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (msg.senderIsUser) 12.dp else 2.dp,
                                        bottomEnd = if (msg.senderIsUser) 2.dp else 12.dp
                                    ),
                                    modifier = Modifier
                                        .widthIn(max = 290.dp)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = textColor,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            textAlign = TextAlign.Right
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.timestamp,
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Waiting Loading state
                        if (isAiLoading) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33E67E22)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.widthIn(max = 240.dp).border(1.dp, TextOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextOrange, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("جاري الفحص الميداني والمطابقة... 🩺", fontSize = 11.sp, color = TextOrange, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Suggestions quick questions row
                    Text("💡 أسئلة فنية شائعة (انقر للسؤال الفوري):", fontSize = 11.sp, color = TextGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .height(75.dp)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val suggestionChunks = suggestionQuestions.chunked(2)
                        items(suggestionChunks) { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { question ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color(0xFF0F2034).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isAiLoading) { askAssistant(question) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = question,
                                            fontSize = 9.5.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom input chat Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aiQuery,
                            onValueChange = { aiQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("اكتب سؤالك السريري أو التقني هنا...", color = TextSecondary, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = { startVoiceInput() }, enabled = !isAiLoading) {
                                    Text("🎤", fontSize = 18.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Secondary,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF091422),
                                unfocusedContainerColor = Color(0xFF091422)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            enabled = !isAiLoading
                        )
                        FloatingActionButton(
                            onClick = {
                                if (aiQuery.isNotBlank() && !isAiLoading) {
                                    askAssistant(aiQuery)
                                }
                            },
                            containerColor = Secondary,
                            contentColor = Primary,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.size(45.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog under Tab 0
    selectedBook?.let { book ->
        AlertDialog(
            onDismissRequest = { selectedBook = null },
            title = {
                Text(
                    text = "معلومات المنهج الدراسي",
                    color = TextGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "العنوان العلمي: ${book.title}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ملف القراءة المرفق: ${book.file}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ملاحظة: يجري تحميل المستند الطبي من الخادم الآمن للقوات المسلحة لحفظ الأصول الفكرية وبقوة تشغيلية بدون إنترنت.",
                        color = TextOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val toOpen = selectedBook
                            if (toOpen != null) {
                                selectedBook = null
                                onNavigateToPdf(toOpen)
                            }
                        }
                    ) {
                        Text("قراءة في التطبيق 📖", color = Secondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val toOpen = selectedBook
                            if (toOpen != null) {
                                repository.openBook(toOpen)
                            }
                            selectedBook = null
                        }
                    ) {
                        Text("تطبيق خارجي 📁", color = TextGold, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBook = null }) {
                    Text("إغلاق", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = PrimaryLight
        )
    }
}
