package com.dilshad.myapplication.curriculum

data class CurriculumCatalog(
    val schemaVersion: Int,
    val sourceName: String,
    val entries: List<BookCatalogEntry>
)

data class BookCatalogEntry(
    val bookId: String,
    val classLevel: Int,
    val subject: String,
    val language: String,
    val title: String,
    val edition: String?,
    val officialUrl: String,
    val chapters: List<ChapterCatalogEntry>,
    val licensingNote: String
)

data class ChapterCatalogEntry(
    val chapterId: String,
    val number: String,
    val title: String,
    val sections: List<String>
)
