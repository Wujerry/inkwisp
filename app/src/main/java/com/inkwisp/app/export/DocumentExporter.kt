package com.inkwisp.app.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ExportFormat(val extension: String) {
    Markdown("md"),
    Html("html"),
    Pdf("pdf"),
    PlainText("txt"),
}

class DocumentExporter(private val context: Context) {
    suspend fun export(
        uri: Uri,
        title: String,
        markdown: String,
        format: ExportFormat,
    ) = withContext(Dispatchers.IO) {
        when (format) {
            ExportFormat.Markdown -> writeText(uri, markdown)
            ExportFormat.PlainText -> writeText(uri, markdownToPlainText(markdown))
            ExportFormat.Html -> writeText(uri, markdownToHtml(title, markdown))
            ExportFormat.Pdf -> writePdf(uri, title, markdownToPlainText(markdown))
        }
    }

    private fun writeText(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(content)
        } ?: error("Unable to create export file.")
    }

    private fun writePdf(uri: Uri, title: String, text: String) {
        val document = PdfDocument()
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 25)
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val titlePaint = Paint(bodyPaint).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val lines = wrapText(text, bodyPaint, PAGE_WIDTH - 2 * MARGIN)
        var cursor = 0
        var pageNumber = 1
        do {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            val canvas = page.canvas
            canvas.drawColor(Color.rgb(255, 252, 245))
            var y = MARGIN.toFloat()
            if (pageNumber == 1) {
                canvas.drawText(title.substringBeforeLast('.'), MARGIN.toFloat(), y + 20f, titlePaint)
                y += 52f
            }
            while (cursor < lines.size && y < PAGE_HEIGHT - MARGIN) {
                canvas.drawText(lines[cursor], MARGIN.toFloat(), y, bodyPaint)
                y += BODY_LINE_HEIGHT
                cursor += 1
            }
            document.finishPage(page)
            pageNumber += 1
        } while (cursor < lines.size || pageNumber == 2)
        context.contentResolver.openOutputStream(uri, "w")?.use(document::writeTo)
            ?: error("Unable to create PDF export.")
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> = buildList {
        text.lines().forEach { sourceLine ->
            if (sourceLine.isEmpty()) {
                add("")
                return@forEach
            }
            var remaining = sourceLine
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth.toFloat(), null).coerceAtLeast(1)
                var split = count
                if (count < remaining.length) {
                    remaining.lastIndexOf(' ', count).takeIf { it > 0 }?.let { split = it + 1 }
                }
                add(remaining.substring(0, split).trimEnd())
                remaining = remaining.substring(split).trimStart()
            }
        }
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48
        const val BODY_LINE_HEIGHT = 18f

        fun markdownToPlainText(markdown: String): String = markdown
            .replace(Regex("(?s)^---\\s*\n.*?\n---\\s*\n"), "")
            .replace(Regex("```[\\w-]*\\n?"), "")
            .replace("```", "")
            .replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("^>\\s?", RegexOption.MULTILINE), "")
            .replace(Regex("^[-*+]\\s+(?:\\[[ xX]]\\s+)?", RegexOption.MULTILINE), "")
            .replace(Regex("[*_~`]"), "")
            .trim()

        fun markdownToHtml(title: String, markdown: String): String {
            val output = StringBuilder()
            var inCode = false
            var inList = false
            markdown.lines().forEach { raw ->
                val line = raw.trimEnd()
                if (line.startsWith("```")) {
                    if (inList) { output.append("</ul>\n"); inList = false }
                    output.append(if (inCode) "</code></pre>\n" else "<pre><code>")
                    inCode = !inCode
                    return@forEach
                }
                if (inCode) {
                    output.append(escapeHtml(raw)).append('\n')
                    return@forEach
                }
                val listMatch = Regex("^[-*+]\\s+(.*)$").find(line)
                if (listMatch != null) {
                    if (!inList) { output.append("<ul>\n"); inList = true }
                    output.append("<li>").append(renderInline(listMatch.groupValues[1])).append("</li>\n")
                    return@forEach
                }
                if (inList) { output.append("</ul>\n"); inList = false }
                when {
                    line.isBlank() -> output.append('\n')
                    Regex("^#{1,6}\\s+").containsMatchIn(line) -> {
                        val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                        output.append("<h$level>")
                            .append(renderInline(line.drop(level).trimStart()))
                            .append("</h$level>\n")
                    }
                    line.startsWith(">") -> output.append("<blockquote>")
                        .append(renderInline(line.drop(1).trimStart())).append("</blockquote>\n")
                    line.matches(Regex("^([-*_])\\1{2,}$")) -> output.append("<hr>\n")
                    else -> output.append("<p>").append(renderInline(line)).append("</p>\n")
                }
            }
            if (inList) output.append("</ul>\n")
            if (inCode) output.append("</code></pre>\n")
            return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${escapeHtml(title.substringBeforeLast('.'))}</title><style>
:root{color-scheme:light dark}body{max-width:760px;margin:0 auto;padding:48px 24px;background:#f7f3ea;color:#1c1b19;font:18px/1.7 Georgia,serif}h1,h2,h3,h4,h5,h6{font-family:system-ui,sans-serif;line-height:1.25}a{color:#b74432}pre,code{background:#ebe6dc;border-radius:6px}code{padding:.12em .35em}pre{padding:16px;overflow:auto}blockquote{border-left:3px solid #b74432;margin-left:0;padding-left:18px;color:#706d66}@media(prefers-color-scheme:dark){body{background:#171614;color:#f1ece2}pre,code{background:#292622}}
</style></head><body>$output</body></html>"""
        }

        private fun renderInline(value: String): String = escapeHtml(value)
            .replace(Regex("`([^`]+)`"), "<code>$1</code>")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("__([^_]+)__"), "<strong>$1</strong>")
            .replace(Regex("~~([^~]+)~~"), "<del>$1</del>")
            .replace(Regex("\\*([^*]+)\\*"), "<em>$1</em>")
            .replace(Regex("\\[([^]]+)]\\((https?://[^)]+)\\)"), "<a href=\"$2\">$1</a>")

        private fun escapeHtml(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
