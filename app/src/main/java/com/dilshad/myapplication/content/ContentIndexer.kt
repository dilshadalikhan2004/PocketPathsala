package com.dilshad.myapplication.content

import java.nio.charset.StandardCharsets

/** A page extracted from a source document. Page numbers are one-based. */
data class ExtractedPage(val pageNumber: Int, val text: String)

data class IndexedChunk(
    val chapter: String,
    val section: String,
    val pageNumber: Int,
    val text: String,
    val citation: String
)

class ContentIndexer(
    private val maxChunkBytes: Int = DEFAULT_MAX_CHUNK_BYTES,
    private val overlapBytes: Int = DEFAULT_OVERLAP_BYTES
) {
    init {
        require(maxChunkBytes >= MIN_CHUNK_BYTES) { "maxChunkBytes must be at least $MIN_CHUNK_BYTES bytes" }
        require(overlapBytes in 0 until maxChunkBytes) { "overlapBytes must be smaller than maxChunkBytes" }
    }

    fun index(pages: List<ExtractedPage>, bookTitle: String): List<IndexedChunk> {
        require(bookTitle.isNotBlank()) { "bookTitle must not be blank" }
        var chapter = "Unknown chapter"
        var section = "General"
        val result = mutableListOf<IndexedChunk>()
        pages.sortedBy { it.pageNumber }.forEach { page ->
            val lines = page.text.replace("\r\n", "\n").split('\n')
            val body = buildString {
                lines.forEach { line ->
                    val heading = heading(line)
                    if (heading != null) {
                        if (heading.first) chapter = heading.second else section = heading.second
                    } else if (line.isNotBlank()) {
                        if (isNotBlank()) append('\n')
                        append(line.trim())
                    }
                }
            }
            splitByUtf8Bytes(body, maxChunkBytes, overlapBytes).forEach { text ->
                result += IndexedChunk(chapter, section, page.pageNumber, text, citation(bookTitle, chapter, section, page.pageNumber))
            }
        }
        return result
    }

    private fun heading(line: String): Pair<Boolean, String>? {
        val value = line.trim()
        if (value.isBlank() || value.length > 160) return null
        val chapter = Regex("^(?:chapter|unit)\\s+(?:[0-9IVX]+)\\b[:.\\-]?\\s*(.+)?$", RegexOption.IGNORE_CASE).matchEntire(value)
        if (chapter != null) return true to (chapter.groupValues[1].ifBlank { value })
        val section = Regex("^(?:section|lesson|topic)\\s+[0-9]+(?:\\.[0-9]+)*\\b[:.\\-]?\\s*(.+)?$", RegexOption.IGNORE_CASE).matchEntire(value)
        if (section != null) return false to (section.groupValues[1].ifBlank { value })
        if (value.matches(Regex("^[0-9]+(?:\\.[0-9]+)*\\s+.+$")) || value == value.uppercase() && value.any(Char::isLetter)) return false to value
        return null
    }

    private fun citation(bookTitle: String, chapter: String, section: String, page: Int) =
        "$bookTitle, $chapter, $section, p. $page"

    companion object {
        const val MIN_CHUNK_BYTES = 4
        const val DEFAULT_MAX_CHUNK_BYTES = 1800
        const val DEFAULT_OVERLAP_BYTES = 240

        internal fun splitByUtf8Bytes(text: String, maxBytes: Int, overlapBytes: Int): List<String> {
            if (text.isBlank()) return emptyList()
            val chars = text.trim().toList()
            val result = mutableListOf<String>()
            var start = 0
            while (start < chars.size) {
                var end = start
                var bytes = 0
                while (end < chars.size) {
                    val next = chars[end].toString().toByteArray(StandardCharsets.UTF_8).size
                    if (end > start && bytes + next > maxBytes) break
                    bytes += next
                    end++
                    if (bytes >= maxBytes) break
                }
                result += chars.subList(start, end).joinToString("").trim()
                if (end == chars.size) break
                var overlap = 0
                var nextStart = end
                while (nextStart > start && overlap < overlapBytes) {
                    nextStart--
                    overlap += chars[nextStart].toString().toByteArray(StandardCharsets.UTF_8).size
                }
                start = if (nextStart <= start) end else nextStart
            }
            return result.filter(String::isNotBlank)
        }
    }
}