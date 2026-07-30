package com.inkwisp.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceLanguageTest {
    @Test
    fun systemPreferenceClearsTheApplicationLocaleOverride() {
        assertEquals("", applicationLocaleTags(null))
        assertEquals("", applicationLocaleTags("system"))
    }

    @Test
    fun explicitLanguageRemainsAnApplicationOverride() {
        assertEquals("en", applicationLocaleTags("en"))
        assertEquals("zh-CN", applicationLocaleTags("zh-CN"))
    }
}
