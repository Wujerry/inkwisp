package com.inkwisp.app.search

import java.util.Locale

data class IndexDocument(
    val name: String,
    val path: String,
    val text: String,
)

data class ContextPassage(
    val name: String,
    val path: String,
    val text: String,
    val score: Int,
)

class WorkspaceIndex {
    @Volatile
    private var documents: List<IndexDocument> = emptyList()

    fun replace(next: List<IndexDocument>) {
        documents = next
    }

    fun clear() {
        documents = emptyList()
    }

    fun backlinks(targetName: String): List<String> {
        val canonical = normalize(targetName.substringBeforeLast('.'))
        return documents.asSequence()
            .filter { document ->
                WIKI_LINK_REGEX.findAll(document.text).any { match ->
                    normalize(match.groupValues[1].substringBefore('#').trim().substringBeforeLast('.')) == canonical
                }
            }
            .map(IndexDocument::path)
            .sorted()
            .toList()
    }

    fun retrieve(
        query: String,
        explicitReferences: Set<String> = emptySet(),
        excludedPath: String? = null,
        limit: Int = 4,
    ): List<ContextPassage> {
        val terms = tokenize(query).takeLast(MAX_QUERY_TERMS).distinct()
        val references = explicitReferences.map { normalize(it) }.toSet()
        return documents.asSequence()
            .filterNot { it.path == excludedPath }
            .mapNotNull { document ->
                val normalizedName = normalize(document.name.substringBeforeLast('.'))
                val normalizedPath = normalize(document.path)
                val explicit = references.any { reference ->
                    normalizedName == reference || normalizedPath.endsWith(reference)
                }
                val content = normalize(document.text)
                val matched = terms.filter { term -> term.length > 1 && (content.contains(term) || normalizedPath.contains(term)) }
                val score = (if (explicit) EXPLICIT_REFERENCE_SCORE else 0) +
                    matched.sumOf { term ->
                        (if (normalizedName.contains(term)) 14 else 0) +
                            content.windowed(term.length, step = maxOf(1, term.length), partialWindows = false)
                                .count { it == term }
                                .coerceAtMost(6)
                    }
                if (score <= 0) null else ContextPassage(
                    name = document.name,
                    path = document.path,
                    text = snippet(document.text, matched.firstOrNull()),
                    score = score,
                )
            }
            .sortedWith(compareByDescending<ContextPassage> { it.score }.thenBy { it.path })
            .take(limit)
            .toList()
    }

    private fun snippet(text: String, preferredTerm: String?): String {
        if (text.length <= MAX_SNIPPET_CHARS) return text
        val normalized = normalize(text)
        val center = preferredTerm?.let(normalized::indexOf)?.takeIf { it >= 0 } ?: 0
        val start = maxOf(0, center - MAX_SNIPPET_CHARS / 3)
        val end = minOf(text.length, start + MAX_SNIPPET_CHARS)
        return buildString {
            if (start > 0) append("…\n")
            append(text.substring(start, end))
            if (end < text.length) append("\n…")
        }
    }

    private fun tokenize(value: String): List<String> = TOKEN_REGEX
        .findAll(normalize(value))
        .map { it.value }
        .filterNot(STOP_WORDS::contains)
        .toList()

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)

    private companion object {
        const val MAX_QUERY_TERMS = 40
        const val MAX_SNIPPET_CHARS = 1_600
        const val EXPLICIT_REFERENCE_SCORE = 10_000
        val TOKEN_REGEX = Regex("[\\p{L}\\p{N}_-]{2,}")
        val STOP_WORDS = setOf("the", "and", "for", "with", "this", "that", "from", "into", "you", "your")
        val WIKI_LINK_REGEX = Regex("!?\\[\\[([^]]+)]]")
    }
}

fun findExplicitReferences(text: String): Set<String> = Regex("@([\\p{L}\\p{N}_. -]{1,80})")
    .findAll(text)
    .map { it.groupValues[1].trim().substringBefore('\n') }
    .filter(String::isNotBlank)
    .toSet()
