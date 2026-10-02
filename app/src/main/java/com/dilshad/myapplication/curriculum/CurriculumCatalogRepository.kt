package com.dilshad.myapplication.curriculum

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

class CurriculumCatalogRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun load(): CurriculumCatalog = withContext(Dispatchers.IO) {
        try {
            context.assets.open(CATALOG_ASSET_PATH).use { input ->
                val json = JsonParser.parseReader(input.reader(Charsets.UTF_8))
                validateJsonShape(json)
                validateCatalog(gson.fromJson(json, CurriculumCatalog::class.java))
            }
        } catch (exception: IllegalStateException) {
            throw exception
        } catch (exception: Exception) {
            throw IllegalStateException("Unable to load curriculum catalog asset '$CATALOG_ASSET_PATH'", exception)
        }
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

        internal fun parseAndValidate(json: String): CurriculumCatalog {
            return try {
                val element = JsonParser.parseString(json)
                validateJsonShape(element)
                validateCatalog(Gson().fromJson(element, CurriculumCatalog::class.java))
            } catch (exception: IllegalStateException) {
                throw exception
            } catch (exception: Exception) {
                throw IllegalStateException("Malformed curriculum catalog JSON", exception)
            }
        }

        internal fun validateCatalog(catalog: CurriculumCatalog?): CurriculumCatalog {
            val safeCatalog = catalog ?: throw IllegalStateException("Curriculum catalog must not be null")
            check(safeCatalog.schemaVersion > 0) { "Curriculum catalog schemaVersion must be positive" }
            check(!safeCatalog.sourceName.isNullOrBlank()) { "Curriculum catalog sourceName must not be blank" }
            val entries = (safeCatalog.entries as List<BookCatalogEntry?>?)
                ?: throw IllegalStateException("Curriculum catalog entries must not be null")
            check(entries.isNotEmpty()) { "Curriculum catalog must contain at least one entry" }

            val bookIds = HashSet<String>()
            entries.forEach { entry ->
                val safeEntry = entry ?: throw IllegalStateException("Curriculum catalog entries must not contain null elements")
                check(safeEntry.classLevel in MIN_CLASS_LEVEL..MAX_CLASS_LEVEL) {
                    "Unsupported class level ${safeEntry.classLevel} for book '${safeEntry.bookId}'"
                }
                check(!safeEntry.bookId.isNullOrBlank()) { "Catalog entry bookId must not be blank" }
                check(bookIds.add(safeEntry.bookId)) { "Duplicate catalog bookId '${safeEntry.bookId}'" }
                check(!safeEntry.subject.isNullOrBlank()) { "Catalog entry '${safeEntry.bookId}' subject must not be blank" }
                check(!safeEntry.language.isNullOrBlank()) { "Catalog entry '${safeEntry.bookId}' language must not be blank" }
                check(!safeEntry.title.isNullOrBlank()) { "Catalog entry '${safeEntry.bookId}' title must not be blank" }
                check(safeEntry.officialUrl.isValidHttpUrl()) { "Catalog entry '${safeEntry.bookId}' officialUrl must be a valid HTTP(S) URL" }
                check(!safeEntry.licensingNote.isNullOrBlank()) { "Catalog entry '${safeEntry.bookId}' licensingNote must not be blank" }
                val chapters = (safeEntry.chapters as List<ChapterCatalogEntry?>?)
                    ?: throw IllegalStateException("Catalog entry '${safeEntry.bookId}' chapters must not be null")
                check(chapters.isNotEmpty()) { "Catalog entry '${safeEntry.bookId}' must contain at least one chapter" }

                val chapterIds = HashSet<String>()
                chapters.forEach { chapter ->
                    val safeChapter = chapter ?: throw IllegalStateException("Book '${safeEntry.bookId}' chapters must not contain null elements")
                    check(!safeChapter.chapterId.isNullOrBlank()) { "Book '${safeEntry.bookId}' has a blank chapterId" }
                    check(chapterIds.add(safeChapter.chapterId)) { "Book '${safeEntry.bookId}' has duplicate chapterId '${safeChapter.chapterId}'" }
                    check(!safeChapter.number.isNullOrBlank()) { "Book '${safeEntry.bookId}' chapter '${safeChapter.chapterId}' number must not be blank" }
                    check(!safeChapter.title.isNullOrBlank()) { "Book '${safeEntry.bookId}' chapter '${safeChapter.chapterId}' title must not be blank" }
                    val sections = (safeChapter.sections as List<String?>?)
                        ?: throw IllegalStateException("Book '${safeEntry.bookId}' chapter '${safeChapter.chapterId}' sections must not be null")
                    check(sections.all { !it.isNullOrBlank() }) { "Book '${safeEntry.bookId}' chapter '${safeChapter.chapterId}' has a blank section" }
                }
            }

            val coveredClasses = entries.map { it!!.classLevel }.toSet()
            check(coveredClasses.containsAll((MIN_CLASS_LEVEL..MAX_CLASS_LEVEL).toSet())) {
                "Curriculum catalog must include entries for every class 6 through 10"
            }
            return safeCatalog
        }

        private fun validateJsonShape(json: JsonElement) {
            check(json.isJsonObject) { "Curriculum catalog root must be a JSON object" }
            val root = json.asJsonObject
            requireArray(root, "entries").forEach { entry ->
                check(entry.isJsonObject && !entry.isJsonNull) { "Curriculum catalog entries must be JSON objects" }
                val book = entry.asJsonObject
                requireArray(book, "chapters").forEach { chapter ->
                    check(chapter.isJsonObject && !chapter.isJsonNull) { "Catalog chapters must be JSON objects" }
                    requireArray(chapter.asJsonObject, "sections").forEach { section ->
                        check(section.isJsonPrimitive && section.asJsonPrimitive.isString) { "Chapter sections must be strings" }
                    }
                }
            }
        }

        private fun requireArray(objectValue: JsonObject, field: String): List<JsonElement> {
            val value = objectValue.get(field)
            check(value != null && value.isJsonArray && !value.isJsonNull) { "Catalog field '$field' must be an array" }
            return value!!.asJsonArray.toList()
        }

        private fun String?.isValidHttpUrl(): Boolean {
            if (this.isNullOrBlank() || any { it.isWhitespace() }) return false
            return runCatching {
                val uri = URI(this)
                (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
    }
}




