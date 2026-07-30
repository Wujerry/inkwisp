package com.inkwisp.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentTitleTest {

    @Test
    fun markdownExtensionIsNotPartOfTheDocumentTitle() {
        assertEquals("A thoughtful title", titleWithoutMarkdownExtension("A thoughtful title.md"))
        assertEquals("Notes.v2", titleWithoutMarkdownExtension("Notes.v2.MARKDOWN"))
        assertEquals("README", titleWithoutMarkdownExtension("README"))
    }
    @Test
    fun firstMeaningfulHeadingBecomesTitle() {
        assertEquals("A quiet morning", titleFromMarkdown("\n# A quiet morning\n\nBody"))
        assertEquals("续墨", titleFromMarkdown("## **续墨**\n正文"))
    }

    @Test
    fun emptyDocumentHasNoTitle() {
        assertNull(titleFromMarkdown(" \n\n"))
    }
}
