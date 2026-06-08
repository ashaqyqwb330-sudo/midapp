package com.example.model

data class BookEntry(
    val chapter: Int,
    val title: String,
    val type: String,
    val file: String,
    val cover_path: String
)

data class Chapter(
    val id: String,
    val name: String,
    val bookCount: Int,
    val icon: String
)

data class ChapterContent(
    val books: List<BookEntry>,
    val generals: List<BookEntry>,
    val devices: Map<String, List<BookEntry>>
)
