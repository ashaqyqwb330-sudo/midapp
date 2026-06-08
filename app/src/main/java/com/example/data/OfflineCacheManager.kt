package com.example.data

import android.content.Context
import com.example.model.BookEntry
import com.example.data.DataProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CachedDoc(
    val title: String,
    val filePath: String,
    val viewedTimestamp: Long,
    val fileSizeFormatted: String,
    val isVerifiedOffline: Boolean
)

class OfflineCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("offline_document_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dataProvider = DataProvider(context)

    fun registerDocumentViewed(title: String, filePath: String) {
        val currentList = getCachedDocuments().toMutableList()
        
        // Find existing to avoid duplicates
        currentList.removeAll { it.filePath == filePath }

        // Get file size
        val dummyBook = BookEntry(chapter = 1, title = title, type = "", file = filePath, cover_path = "")
        val bookFile = dataProvider.getBookFile(dummyBook)
        val sizeFormatted = if (bookFile != null && bookFile.exists()) {
            val bytes = bookFile.length()
            if (bytes > 1024 * 1024) {
                String.format(Locale.getDefault(), "%.2f MB", bytes.toFloat() / (1024 * 1024))
            } else {
                String.format(Locale.getDefault(), "%.1f KB", bytes.toFloat() / 1024)
            }
        } else {
            "تحت المزامنة"
        }

        val newDoc = CachedDoc(
            title = title,
            filePath = filePath,
            viewedTimestamp = System.currentTimeMillis(),
            fileSizeFormatted = sizeFormatted,
            isVerifiedOffline = bookFile != null && bookFile.exists()
        )

        currentList.add(0, newDoc) // Prepend for recency

        // Limit cache index to top 30 files
        val trimmedList = if (currentList.size > 30) currentList.take(30) else currentList

        prefs.edit().putString("cached_docs_json", gson.toJson(trimmedList)).apply()
    }

    fun getCachedDocuments(): List<CachedDoc> {
        val json = prefs.getString("cached_docs_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CachedDoc>>() {}.type
            val list: List<CachedDoc> = gson.fromJson(json, type)
            // Re-verify offline existence dynamically
            list.map { doc ->
                val dummyBook = BookEntry(chapter = 1, title = doc.title, type = "", file = doc.filePath, cover_path = "")
                val bookFile = dataProvider.getBookFile(dummyBook)
                val exists = bookFile != null && bookFile.exists()
                doc.copy(isVerifiedOffline = exists)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearCache() {
        prefs.edit().remove("cached_docs_json").apply()
        // Optional: clear generated cache folder files
        try {
            val files = context.cacheDir.listFiles()
            files?.forEach { file ->
                if (file.name.startsWith("gen_") || file.name.endsWith(".pdf")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
