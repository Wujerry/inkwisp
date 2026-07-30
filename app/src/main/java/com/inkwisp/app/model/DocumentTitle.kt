package com.inkwisp.app.model

fun titleFromMarkdown(content: String): String? = content.lineSequence()
    .map(String::trim)
    .firstOrNull(String::isNotBlank)
    ?.replace(Regex("^#{1,6}\\s*"), "")
    ?.replace(Regex("^[>*+\\-]\\s+"), "")
    ?.replace(Regex("[*_`~\\[\\]]"), "")
    ?.trim()
    ?.take(80)
    ?.takeIf(String::isNotBlank)

fun titleWithoutMarkdownExtension(fileName: String): String =
    fileName.replace(Regex("(?i)\\.(md|markdown)$"), "")
