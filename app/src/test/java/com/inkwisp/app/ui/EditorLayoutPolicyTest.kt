package com.inkwisp.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorLayoutPolicyTest {
    @Test
    fun phonePortraitUsesOverlayWorkspaceAndFullChrome() {
        val policy = editorLayoutPolicy(widthDp = 411, heightDp = 891)

        assertFalse(policy.persistentWorkspace)
        assertFalse(policy.compactChrome)
        assertEquals(318, policy.workspaceWidthDp)
    }

    @Test
    fun tabletPortraitKeepsEditorWideAndUsesRoomierDrawer() {
        val policy = editorLayoutPolicy(widthDp = 700, heightDp = 1_000)

        assertTrue(policy.isTablet)
        assertFalse(policy.persistentWorkspace)
        assertEquals(360, policy.workspaceWidthDp)
    }

    @Test
    fun tabletLandscapeUsesPersistentWorkspace() {
        val policy = editorLayoutPolicy(widthDp = 1_024, heightDp = 600)

        assertTrue(policy.isTablet)
        assertTrue(policy.persistentWorkspace)
        assertTrue(policy.compactChrome)
        assertEquals(280, policy.workspaceWidthDp)
    }

    @Test
    fun shortExpandedWindowKeepsTabletNavigation() {
        val policy = editorLayoutPolicy(widthDp = 840, heightDp = 480)

        assertTrue(policy.isTablet)
        assertTrue(policy.persistentWorkspace)
        assertTrue(policy.compactChrome)
    }

    @Test
    fun navigationBreakpointsDependOnWidthNotKeyboardHeight() {
        assertFalse(editorLayoutPolicy(widthDp = 599, heightDp = 1_000).isTablet)
        assertTrue(editorLayoutPolicy(widthDp = 600, heightDp = 599).isTablet)
        assertFalse(editorLayoutPolicy(widthDp = 839, heightDp = 480).persistentWorkspace)
        assertTrue(editorLayoutPolicy(widthDp = 840, heightDp = 480).persistentWorkspace)
    }

    @Test
    fun largeLandscapeTabletAllowsWiderWorkspace() {
        val policy = editorLayoutPolicy(widthDp = 1_280, heightDp = 800)

        assertTrue(policy.persistentWorkspace)
        assertFalse(policy.compactChrome)
        assertEquals(320, policy.workspaceWidthDp)
    }
}
