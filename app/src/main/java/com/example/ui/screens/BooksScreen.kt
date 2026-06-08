package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataProvider
import com.example.model.BookEntry
import com.example.ui.components.Book3DCard
import com.example.ui.components.Shelf
import com.example.ui.theme.*

@Composable
fun BooksScreen(
    chapterName: String,
    onBack: () -> Unit,
    onNavigateToPdf: (BookEntry) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DataProvider(context) }
    
    // Safely extract chapterId number
    val chapterId = remember(chapterName) {
        val cleaned = chapterName.split(" - ").firstOrNull()?.replace("الفصل ", "")?.trim() ?: "1"
        "class$cleaned"
    }
    
    val books = remember(chapterId) { repository.getBooksInChapter(chapterId).first }
    var selectedBook by remember { mutableStateOf<BookEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF030810), Primary, Color(0xFF0F1F33)))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                color = TextGold
            )
        }
        
        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد كتب مباشرة لهذا الفصل، تصفح المواد المتخصصة.",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val shelves = books.chunked(3)
                items(shelves.size) { shelfIndex ->
                    Shelf {
                        shelves[shelfIndex].forEach { book ->
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

    // Detail Dialog
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
                        text = "ملاحظة: يجري تحميل المستند الطبي من الخادم الآمن للقوات المسلحة لحفظ الأصول الفكرية.",
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
