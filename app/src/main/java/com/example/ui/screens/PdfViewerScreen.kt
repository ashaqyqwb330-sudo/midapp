package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.data.OfflineCacheManager
import com.example.data.DocumentNotesManager
import com.example.model.BookEntry
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class DrawTool {
    NONE,
    PEN,
    HIGHLIGHTER
}

data class DrawPoint(val x: Float, val y: Float)

data class DrawPath(
    val tool: DrawTool,
    val color: Color,
    val strokeWidth: Float,
    val points: List<DrawPoint>
)

class DocumentDrawingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("document_drawings_storage", Context.MODE_PRIVATE)

    fun saveDrawings(filePath: String, drawings: Map<Int, List<DrawPath>>) {
        val sb = java.lang.StringBuilder()
        drawings.forEach { (pageIndex, paths) ->
            if (paths.isNotEmpty()) {
                val pageString = paths.joinToString("\n") { path ->
                    val pointsStr = path.points.joinToString(";") { "${it.x},${it.y}" }
                    val colorStr = "${path.color.red},${path.color.green},${path.color.blue},${path.color.alpha}"
                    "${path.tool.name}|$colorStr|${path.strokeWidth}|$pointsStr"
                }
                sb.append("$pageIndex#$pageString##")
            }
        }
        prefs.edit().putString(filePath, sb.toString()).apply()
    }

    fun getDrawings(filePath: String): Map<Int, List<DrawPath>> {
        val result = mutableMapOf<Int, List<DrawPath>>()
        val saved = prefs.getString(filePath, "") ?: ""
        if (saved.isEmpty()) return result
        
        try {
            val pages = saved.split("##")
            for (p in pages) {
                if (p.trim().isEmpty()) continue
                val parts = p.split("#", limit = 2)
                if (parts.size < 2) continue
                val pageIndex = parts[0].toIntOrNull() ?: continue
                val lines = parts[1].split("\n")
                val pathsList = mutableListOf<DrawPath>()
                for (line in lines) {
                    if (line.trim().isEmpty()) continue
                    val pathParts = line.split("|")
                    if (pathParts.size < 4) continue
                    val tool = try { DrawTool.valueOf(pathParts[0]) } catch(e: Exception) { DrawTool.NONE }
                    val colorParts = pathParts[1].split(",")
                    val color = if (colorParts.size == 4) {
                        Color(
                            colorParts[0].toFloatOrNull() ?: 1f,
                            colorParts[1].toFloatOrNull() ?: 0f,
                            colorParts[2].toFloatOrNull() ?: 0f,
                            colorParts[3].toFloatOrNull() ?: 1f
                        )
                    } else Color.Red
                    
                    val strokeWidth = pathParts[2].toFloatOrNull() ?: 5f
                    val pointsStr = pathParts[3]
                    val points = pointsStr.split(";").mapNotNull {
                        val coords = it.split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloatOrNull() ?: return@mapNotNull null
                            val y = coords[1].toFloatOrNull() ?: return@mapNotNull null
                            DrawPoint(x, y)
                        } else null
                    }
                    if (points.isNotEmpty()) {
                        pathsList.add(DrawPath(tool, color, strokeWidth, points))
                    }
                }
                result[pageIndex] = pathsList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    bookTitle: String,
    bookFilePath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DataProvider(context) }
    val scope = rememberCoroutineScope()
    val cacheManager = remember { OfflineCacheManager(context) }
    val notesManager = remember { DocumentNotesManager(context) }
    val drawingsManager = remember { DocumentDrawingsManager(context) }
    
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var parcelFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCachedOffline by remember { mutableStateOf(false) }
    var showNotesPanel by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    
    // Drawings and highlight states
    var pageDrawings by remember { mutableStateOf<Map<Int, List<DrawPath>>>(emptyMap()) }
    var activeDrawTool by remember { mutableStateOf(DrawTool.NONE) }
    var activeDrawColor by remember { mutableStateOf(Color(0xFFE74C3C)) } // Default: Red
    var activeStrokeWidth by remember { mutableStateOf(5f) }             // Default: 5dp
    
    // Zoom state
    var globalScale by remember { mutableStateOf(1f) }
    
    // Resolved File
    LaunchedEffect(bookFilePath) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                // Find file on device or asset cache using the mock BookEntry
                val dummyBook = BookEntry(chapter = 1, title = bookTitle, type = "", file = bookFilePath, cover_path = "")
                val file = repository.getBookFile(dummyBook)
                
                if (file != null && file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    
                    // Register the book viewed in our cache/local storage
                    cacheManager.registerDocumentViewed(bookTitle, bookFilePath)
                    val existingNote = notesManager.getNote(bookFilePath)
                    val existingDrawings = drawingsManager.getDrawings(bookFilePath)
                    
                    withContext(Dispatchers.Main) {
                        parcelFileDescriptor = pfd
                        pdfRenderer = renderer
                        pageCount = renderer.pageCount
                        noteText = existingNote
                        pageDrawings = existingDrawings
                        isCachedOffline = true
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = "لم يتم العثور على الملف الطبي المطلبو. يرجى التأكد من مسار نقل البيانات."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "فشل في تهيئة مستند القراءة: ${e.localizedMessage}"
                }
            }
        }
    }
    
    // Clean up descriptors on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = bookTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCachedOffline) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF2ECC71), shape = RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "متاح ومحفوظ بالكامل دون اتصال (ميدانياً) ✓",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF2ECC71),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Zoom actions
                    IconButton(onClick = { if (globalScale > 0.8f) globalScale -= 0.2f }) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.5.dp)
                                    .background(Color.White, shape = RoundedCornerShape(1.dp))
                            )
                        }
                    }
                    Text(
                        text = "${(globalScale * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { if (globalScale < 3.0f) globalScale += 0.2f }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "تكبير",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryLight
                )
            )
        },
        floatingActionButton = {
            if (!isLoading && errorMessage == null) {
                FloatingActionButton(
                    onClick = {
                        showNotesPanel = true
                    },
                    containerColor = Secondary,
                    contentColor = Primary,
                    modifier = Modifier.padding(bottom = 60.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📝", fontSize = 18.sp)
                        Text(
                            text = if (noteText.isNotEmpty()) "ملاحظاتي 📝" else "إضافة ملاحظة 📝",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جاري تهيئة مستعرض الكتب والمقررات الآمن...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = TextOrange,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text("العودة للخلف", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val renderer = pdfRenderer
                if (renderer != null) {
                    val listState = rememberLazyListState()
                    val activeCenterIndex by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Modern Drawing Control Bar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .border(1.dp, Secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "أدوات التخطيط والتأشير الميداني 🩺",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGold
                                    )
                                    Text(
                                        text = when(activeDrawTool) {
                                            DrawTool.NONE -> "وضع المراجعة وتصفح المستند"
                                            DrawTool.PEN -> "وضع رسم القلم مفعل ✏️"
                                            DrawTool.HIGHLIGHTER -> "وضع التظليل الشفاف مفعل 🖍️"
                                        },
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tool Button: VIEW
                                    val isView = activeDrawTool == DrawTool.NONE
                                    Button(
                                        onClick = { activeDrawTool = DrawTool.NONE },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isView) Secondary else Color.White.copy(alpha = 0.05f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("معاينة 👁️", color = if (isView) Primary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Tool Button: PEN
                                    val isPen = activeDrawTool == DrawTool.PEN
                                    Button(
                                        onClick = { 
                                            activeDrawTool = DrawTool.PEN 
                                            activeStrokeWidth = 5f
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPen) Secondary else Color.White.copy(alpha = 0.05f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("قلم ✏️", color = if (isPen) Primary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Tool Button: HIGHLIGHTER
                                    val isHigh = activeDrawTool == DrawTool.HIGHLIGHTER
                                    Button(
                                        onClick = { 
                                            activeDrawTool = DrawTool.HIGHLIGHTER 
                                            activeStrokeWidth = 20f
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isHigh) Secondary else Color.White.copy(alpha = 0.05f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("تظليل 🖍️", color = if (isHigh) Primary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (activeDrawTool != DrawTool.NONE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Color row selection
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("اللون:", fontSize = 10.sp, color = Color.White)
                                            listOf(
                                                Color(0xFFE74C3C), // Red
                                                Color(0xFF3498DB), // Blue
                                                Color(0xFF2ECC71), // Green
                                                Color(0xFFF1C40F), // Yellow/Gold
                                                Color(0xFFE67E22)  // Orange
                                            ).forEach { color ->
                                                val isSelectedColor = activeDrawColor == color
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(color, shape = RoundedCornerShape(12.dp))
                                                        .border(
                                                            width = if (isSelectedColor) 2.dp else 0.dp,
                                                            color = if (isSelectedColor) Color.White else Color.Transparent,
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { activeDrawColor = color }
                                                )
                                            }
                                        }

                                        // Size row selection
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("الحجم:", fontSize = 10.sp, color = Color.White)
                                            listOf(
                                                Pair("خفيف", if (activeDrawTool == DrawTool.PEN) 4f else 15f),
                                                Pair("متوسط", if (activeDrawTool == DrawTool.PEN) 8f else 28f),
                                                Pair("عريض", if (activeDrawTool == DrawTool.PEN) 14f else 45f)
                                            ).forEach { (label, value) ->
                                                val isSelectedSize = activeStrokeWidth == value
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isSelectedSize) Secondary else Color.White.copy(alpha = 0.08f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .clickable { activeStrokeWidth = value }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelectedSize) Primary else Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Undo, Clear and Save feedback Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Clear page annotations button
                                        Button(
                                            onClick = {
                                                val updated = pageDrawings.toMutableMap()
                                                updated.remove(activeCenterIndex)
                                                pageDrawings = updated
                                                drawingsManager.saveDrawings(bookFilePath, updated)
                                                Toast.makeText(context, "تم مسح تخطيطات الصفحة ${activeCenterIndex + 1} 🗑️", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B).copy(alpha = 0.2f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC0392B)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("مسح الصفحة الحالية 🗑️", color = Color(0xFFC0392B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Undo button
                                        Button(
                                            onClick = {
                                                val existing = pageDrawings[activeCenterIndex] ?: emptyList()
                                                if (existing.isNotEmpty()) {
                                                    val updatedList = existing.dropLast(1)
                                                    val updatedMap = pageDrawings.toMutableMap()
                                                    if (updatedList.isEmpty()) {
                                                        updatedMap.remove(activeCenterIndex)
                                                    } else {
                                                        updatedMap[activeCenterIndex] = updatedList
                                                    }
                                                    pageDrawings = updatedMap
                                                    drawingsManager.saveDrawings(bookFilePath, updatedMap)
                                                    Toast.makeText(context, "تراجع عن خطوة ↩️", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp),
                                            enabled = (pageDrawings[activeCenterIndex]?.size ?: 0) > 0
                                        ) {
                                            Text("تراجع ↩️", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "تُحفظ تلقائياً للميدان 🛡️",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF2ECC71),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Main LazyColumn of pages
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .pointerInput(activeDrawTool) {
                                    if (activeDrawTool == DrawTool.NONE) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            globalScale = (globalScale * zoom).coerceIn(0.8f, 3.0f)
                                        }
                                    }
                                },
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(pageCount) { index ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(
                                            scaleX = globalScale,
                                            scaleY = globalScale
                                        )
                                        .clip(RoundedCornerShape(4.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    PdfPageRenderer(
                                        renderer = renderer,
                                        pageIndex = index,
                                        drawTool = activeDrawTool,
                                        activeColor = activeDrawColor,
                                        activeStrokeWidth = activeStrokeWidth,
                                        existingPaths = pageDrawings[index] ?: emptyList(),
                                        onAddPath = { newPath ->
                                            val updated = pageDrawings.toMutableMap()
                                            updated[index] = (pageDrawings[index] ?: emptyList()) + newPath
                                            pageDrawings = updated
                                            drawingsManager.saveDrawings(bookFilePath, updated)
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom Page Controller bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryLight,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 16.dp)
                                        .navigationBarsPadding(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (activeCenterIndex > 0) {
                                            scope.launch {
                                                listState.animateScrollToItem(activeCenterIndex - 1)
                                            }
                                        }
                                    },
                                    enabled = activeCenterIndex > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "الصفحة السابقة",
                                        tint = if (activeCenterIndex > 0) Color.White else Color.Gray
                                    )
                                }

                                Text(
                                    text = "الصفحة ${activeCenterIndex + 1} من $pageCount",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        if (activeCenterIndex < pageCount - 1) {
                                            scope.launch {
                                                listState.animateScrollToItem(activeCenterIndex + 1)
                                            }
                                        }
                                    },
                                    enabled = activeCenterIndex < pageCount - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "الصفحة التالية",
                                        modifier = Modifier.rotate(180f),
                                        tint = if (activeCenterIndex < pageCount - 1) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showNotesPanel) {
                // Dim screen overlay background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showNotesPanel = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight()
                            .clickable(enabled = false) {}, // Prevent clicks through to overlay
                        colors = CardDefaults.cardColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Secondary.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showNotesPanel = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("❌", fontSize = 16.sp)
                                }
                                Text(
                                    text = "تدوين ملاحظات سريعة 📝",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Secondary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "ملاحظات مرتبطة بمستند:\n$bookTitle",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                placeholder = {
                                    Text(
                                        "اكتب ملاحظاتك العملية أو التدريبية هنا...",
                                        fontSize = 12.sp,
                                        color = TextSecondary.copy(alpha = 0.7f)
                                    )
                                },
                                maxLines = 6,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Secondary,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                    focusedLabelColor = Secondary,
                                    cursorColor = Secondary
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Save Button
                                Button(
                                    onClick = {
                                        notesManager.saveNote(bookFilePath, noteText)
                                        Toast.makeText(context, "تم حفظ الملاحظة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        showNotesPanel = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("حفظ 💾", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                
                                // Delete/Clear button
                                if (notesManager.getNote(bookFilePath).isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            notesManager.deleteNote(bookFilePath)
                                            noteText = ""
                                            Toast.makeText(context, "تم حذف الملاحظة 🗑️", Toast.LENGTH_SHORT).show()
                                            showNotesPanel = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("حذف 🗑️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageRenderer(
    renderer: PdfRenderer,
    pageIndex: Int,
    drawTool: DrawTool,
    activeColor: Color,
    activeStrokeWidth: Float,
    existingPaths: List<DrawPath>,
    onAddPath: (DrawPath) -> Unit
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderError by remember { mutableStateOf(false) }
    
    // Active stroke drawing points
    var currentPathPoints by remember { mutableStateOf<List<DrawPoint>>(emptyList()) }

    LaunchedEffect(renderer, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(pageIndex)
                // Render at higher density for clean reading
                val scaleFactor = 1.8f
                val width = (page.width * scaleFactor).toInt()
                val height = (page.height * scaleFactor).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                
                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                page.close()
                
                withContext(Dispatchers.Main) {
                    pageBitmap = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    renderError = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = pageBitmap
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "محتوى الصفحة ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth
                )
                
                // Canvas overlay for drawing annotations
                val isDrawingEnabled = drawTool != DrawTool.NONE
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(drawTool) {
                            if (isDrawingEnabled) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = offset.x / size.width.toFloat()
                                        val normY = offset.y / size.height.toFloat()
                                        currentPathPoints = listOf(DrawPoint(normX, normY))
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val normX = change.position.x / size.width.toFloat()
                                        val normY = change.position.y / size.height.toFloat()
                                        currentPathPoints = currentPathPoints + DrawPoint(normX, normY)
                                    },
                                    onDragEnd = {
                                        if (currentPathPoints.isNotEmpty()) {
                                            val newPath = DrawPath(
                                                tool = drawTool,
                                                color = activeColor,
                                                strokeWidth = activeStrokeWidth,
                                                points = currentPathPoints
                                            )
                                            onAddPath(newPath)
                                            currentPathPoints = emptyList()
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Draw existing saved paths
                    existingPaths.forEach { path ->
                        if (path.points.size > 1) {
                            val composePath = Path().apply {
                                val start = path.points.first()
                                moveTo(start.x * size.width, start.y * size.height)
                                for (i in 1 until path.points.size) {
                                    val pt = path.points[i]
                                    lineTo(pt.x * size.width, pt.y * size.height)
                                }
                            }
                            
                            val isHighlighter = path.tool == DrawTool.HIGHLIGHTER
                            drawPath(
                                path = composePath,
                                color = path.color,
                                style = Stroke(
                                    width = path.strokeWidth, 
                                    cap = StrokeCap.Round, 
                                    join = StrokeJoin.Round
                                ),
                                alpha = if (isHighlighter) 0.4f else 1.0f
                            )
                        }
                    }
                    
                    // Draw current active stroke path
                    if (currentPathPoints.size > 1) {
                        val composePath = Path().apply {
                            val start = currentPathPoints.first()
                            moveTo(start.x * size.width, start.y * size.height)
                            for (i in 1 until currentPathPoints.size) {
                                val pt = currentPathPoints[i]
                                lineTo(pt.x * size.width, pt.y * size.height)
                            }
                        }
                        val isHighlighter = drawTool == DrawTool.HIGHLIGHTER
                        drawPath(
                            path = composePath,
                            color = activeColor,
                            style = Stroke(
                                width = activeStrokeWidth, 
                                cap = StrokeCap.Round, 
                                join = StrokeJoin.Round
                            ),
                            alpha = if (isHighlighter) 0.4f else 1.0f
                        )
                    }
                }
            }
        } else if (renderError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "فشل في عرض الصفحة المحددة",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0A1128),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
