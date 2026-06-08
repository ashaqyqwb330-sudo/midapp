package com.example.data

import android.content.Context

class DocumentNotesManager(context: Context) {
    private val prefs = context.getSharedPreferences("document_notes_storage", Context.MODE_PRIVATE)

    fun getNote(filePath: String): String {
        return prefs.getString(filePath, "") ?: ""
    }

    fun saveNote(filePath: String, note: String) {
        prefs.edit().putString(filePath, note.trim()).apply()
    }

    fun deleteNote(filePath: String) {
        prefs.edit().remove(filePath).apply()
    }
}
