package com.inkwisp.app.editor

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class EditorBridge(
    private val onReady: () -> Unit,
    private val onChange: (content: String, revision: Long, cursor: Int) -> Unit,
    private val onCommand: (command: String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun ready() {
        mainHandler.post(onReady)
    }

    @JavascriptInterface
    fun changed(content: String, revision: Long, cursor: Int) {
        if (content.length > MAX_DOCUMENT_CHARS) return
        if (cursor !in 0..content.length) return
        mainHandler.post { onChange(content, revision, cursor) }
    }

    @JavascriptInterface
    fun command(command: String) {
        if (command.length > MAX_COMMAND_CHARS) return
        mainHandler.post { onCommand(command) }
    }

    private companion object {
        const val MAX_DOCUMENT_CHARS = 5_000_000
        const val MAX_COMMAND_CHARS = 100_000
    }
}
