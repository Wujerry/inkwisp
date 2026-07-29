package com.inkwisp.app.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentExporterTest {
    @Test
    fun plainTextRemovesCommonMarkdownMarkers() {
        val plain = DocumentExporter.markdownToPlainText("# Title\n\n- [ ] **Write** [docs](https://example.com)")
        assertTrue(plain.contains("Title"))
        assertTrue(plain.contains("Write docs"))
        assertFalse(plain.contains("**"))
    }

    @Test
    fun htmlEscapesSourceAndRendersSafeStructure() {
        val html = DocumentExporter.markdownToHtml("Note.md", "# <Title>\n\n**safe**")
        assertTrue(html.contains("<h1>&lt;Title&gt;</h1>"))
        assertTrue(html.contains("<strong>safe</strong>"))
        assertFalse(html.contains("<h1><Title>"))
    }
}
