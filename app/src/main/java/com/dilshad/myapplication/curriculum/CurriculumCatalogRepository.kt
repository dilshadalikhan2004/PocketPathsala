package com.dilshad.myapplication.curriculum

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurriculumCatalogRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun load(): CurriculumCatalog = withContext(Dispatchers.IO) {
        val catalog = try {
            context.assets.open(CATALOG_ASSET_PATH).use { input ->
                gson.fromJson(input.reader(Charsets.UTF_8), CurriculumCatalog::class.java)
            }
        } catch (exception: Exception) {
            throw IllegalStateException("Unable to load curriculum catalog asset '$CATALOG_ASSET_PATH'", exception)
        }
        validateCatalog(catalog)
    }

    suspend fun find(bookId: String): BookCatalogEntry? = load().entries.firstOrNull { it.bookId == bookId }

    suspend fun filter(classLevel: Int?, subject: String?, language: String?): List<BookCatalogEntry> =
        filterEntries(load(), classLevel, subject, language)

    companion object {
        const val CATALOG_ASSET_PATH = "curriculum/ncert_catalog_v1.json"
        const val MIN_CLASS_LEVEL = 6
        const val MAX_CLASS_LEVEL = 10

        internal fun filterEntries(
            catalog: CurriculumCatalog,
            classLevel: Int?,
            subject: String?,
            language: String?
        ): List<BookCatalogEntry> = catalog.entries.filter { entry ->
            (classLevel == null || entry.classLevel == classLevel) &&
                (subject == null || entry.subject.equals(subject, ignoreCase = true)) &&
                (language == null || entry.language.equals(language, ignoreCase = true))
        }

        internal fun validateCatalog(catalog: CurriculumCatalog?): CurriculumCatalog {
            requireNotNull(catalog) { "Curriculum catalog must not be null" }
            check(catalog.schemaVersion > 0) { "Curriculum catalog schemaVersion must be positive" }
            check(!catalog.sourceName.isNullOrBlank()) { "Curriculum catalog sourceName must not be blank" }
            check(catalog.entries.isNotEmpty()) { "Curriculum catalog must contain at least one entry" }

            val bookIds = HashSet<String>()
            catalog.entries.forEach { entry ->
                check(entry.classLevel in MIN_CLASS_LEVEL..MAX_CLASS_LEVEL) {
                    "Unsupported class level ${entry.classLevel} for book '${entry.bookId}'"
                }
                check(!entry.bookId.isNullOrBlank()) { "Catalog entry bookId must not be blank" }
                check(bookIds.add(entry.bookId)) { "Duplicate catalog bookId '${entry.bookId}'" }
                check(!entry.subject.isNullOrBlank()) { "Catalog entry '${entry.bookId}' subject must not be blank" }
                check(!entry.language.isNullOrBlank()) { "Catalog entry '${entry.bookId}' language must not be blank" }
                check(!entry.title.isNullOrBlank()) { "Catalog entry '${entry.bookId}' title must not be blank" }
                check(!entry.officialUrl.isNullOrBlank() && entry.officialUrl.isHttpUrl()) { "Catalog entry '${entry.bookId}' officialUrl must be HTTP(S)" }
                check(!entry.licensingNote.isNullOrBlank()) { "Catalog entry '${entry.bookId}' licensingNote must not be blank" }
                check(!entry.chapters.isNullOrEmpty()) { "Catalog entry '${entry.bookId}' must contain at least one chapter" }

                val chapterIds = HashSet<String>()
                entry.chapters.orEmpty().forEach { chapter ->
                    check(!chapter.chapterId.isNullOrBlank()) { "Book '${entry.bookId}' has a blank chapterId" }
                    check(chapterIds.add(chapter.chapterId)) { "Book '${entry.bookId}' has duplicate chapterId '${chapter.chapterId}'" }
                    check(!chapter.number.isNullOrBlank()) { "Book '${entry.bookId}' chapter '${chapter.chapterId}' number must not be blank" }
                    check(!chapter.title.isNullOrBlank()) { "Book '${entry.bookId}' chapter '${chapter.chapterId}' title must not be blank" }
                    check(runCatching { chapter.sections.all { !it.isNullOrBlank() } }.getOrDefault(false)) { "Book '${entry.bookId}' chapter '${chapter.chapterId}' has a blank section" }
                }
            }

            val coveredClasses = catalog.entries.map { it.classLevel }.toSet()
            check(coveredClasses.containsAll((MIN_CLASS_LEVEL..MAX_CLASS_LEVEL).toSet())) {
                "Curriculum catalog must include entries for every class 6 through 10"
            }
            return catalog
        }

        private fun String.isHttpUrl(): Boolean = startsWith("https://") || startsWith("http://")
    }
}





