package com.inkwisp.app.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceIndexTest {
    @Test
    fun explicitReferenceOutranksLooseTextMatch() {
        val index = WorkspaceIndex().apply {
            replace(
                listOf(
                    IndexDocument("Roadmap.md", "plans/Roadmap.md", "Release planning notes"),
                    IndexDocument("Notes.md", "Notes.md", "Roadmap roadmap roadmap repeated"),
                ),
            )
        }
        val result = index.retrieve("release roadmap", setOf("Roadmap"))
        assertEquals("Roadmap.md", result.first().name)
    }

    @Test
    fun excludedDocumentNeverEntersContext() {
        val index = WorkspaceIndex().apply {
            replace(listOf(IndexDocument("Private.md", "Private.md", "secret project")))
        }
        assertTrue(index.retrieve("secret", excludedPath = "Private.md").isEmpty())
    }

    @Test
    fun extractsAtReferencesFromNearbyWriting() {
        assertEquals(setOf("Roadmap.md"), findExplicitReferences("Continue from @Roadmap.md\nthen write"))
    }
}
