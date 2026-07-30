package com.inkwisp.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {
    @Test
    fun presetIdsAreUniqueAndCatalogCoversEveryProtocol() {
        assertEquals(ProviderCatalog.presets.size, ProviderCatalog.presets.map { it.id }.toSet().size)
        assertEquals(ModelProtocol.entries.toSet(), ProviderCatalog.presets.map { it.protocol }.toSet())
    }

    @Test
    fun deepSeekAnthropicPresetUsesDocumentedCompatibleBase() {
        val preset = ProviderCatalog.presets.single { it.id == "deepseek-anthropic" }
        assertEquals("https://api.deepseek.com/anthropic", preset.baseUrl)
        assertEquals(ModelProtocol.AnthropicMessages, preset.protocol)
        assertTrue(preset.exampleModel.startsWith("deepseek-"))
    }
}
