package com.inkwisp.app.ui

data class EditorLayoutPolicy(
    val isTablet: Boolean,
    val persistentWorkspace: Boolean,
    val compactChrome: Boolean,
    val workspaceWidthDp: Int,
)

fun editorLayoutPolicy(widthDp: Int, heightDp: Int): EditorLayoutPolicy {
    val isTablet = widthDp >= 600
    val persistentWorkspace = widthDp >= 840
    val workspaceWidth = when {
        persistentWorkspace && widthDp >= 1_200 -> 320
        persistentWorkspace -> 280
        isTablet -> 360
        else -> 318
    }
    return EditorLayoutPolicy(
        isTablet = isTablet,
        persistentWorkspace = persistentWorkspace,
        compactChrome = heightDp < 640,
        workspaceWidthDp = workspaceWidth,
    )
}
