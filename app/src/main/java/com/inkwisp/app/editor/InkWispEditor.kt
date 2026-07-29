package com.inkwisp.app.editor

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.inkwisp.app.BuildConfig
import com.inkwisp.app.model.EditorMode
import com.inkwisp.app.model.EditorInsertion
import org.json.JSONObject

class EditorController {
    private var webView: WebView? = null

    internal fun attach(webView: WebView) {
        this.webView = webView
    }

    internal fun detach(webView: WebView) {
        if (this.webView === webView) this.webView = null
    }

    fun runFormatCommand(command: String) {
        if (command !in FORMAT_COMMANDS) return
        webView?.evaluateJavascript("window.InkWispEditor?.runCommand(${JSONObject.quote(command)})", null)
    }

    fun focus() {
        webView?.evaluateJavascript("window.InkWispEditor?.focus()", null)
    }

    fun requestAssistedEdit(action: String) {
        if (action !in ASSISTED_ACTIONS) return
        webView?.evaluateJavascript(
            "window.InkWispEditor?.requestAssistedEdit(${JSONObject.quote(action)})",
            null,
        )
    }

    private companion object {
        val FORMAT_COMMANDS = setOf("heading", "bold", "italic", "bullet", "task", "code")
        val ASSISTED_ACTIONS = setOf("rewrite", "shorten", "expand", "translate", "grammar")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InkWispEditor(
    content: String,
    revision: Long,
    mode: EditorMode,
    darkTheme: Boolean,
    reducedMotion: Boolean,
    predictionText: String?,
    pendingInsertion: EditorInsertion?,
    onInsertionHandled: (Long) -> Unit,
    onContentChanged: (String, Long, Int) -> Unit,
    onCommand: (String) -> Unit,
    controller: EditorController,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            WebView(context).apply {
                setBackgroundColor(if (darkTheme) Color.rgb(23, 22, 20) else Color.rgb(247, 243, 234))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowContentAccess = false
                settings.allowFileAccess = false
                settings.setSupportMultipleWindows(false)
                settings.javaScriptCanOpenWindowsAutomatically = false
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.url.host != WebViewAssetLoader.DEFAULT_DOMAIN
                }
                addJavascriptInterface(
                    EditorBridge(
                        onReady = { ready = true },
                        onChange = onContentChanged,
                        onCommand = onCommand,
                    ),
                    "InkWispNative",
                )
                loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/editor/index.html")
                webView = this
                controller.attach(this)
            }
        },
        update = { webView = it },
    )

    LaunchedEffect(ready, revision) {
        if (!ready) return@LaunchedEffect
        val quoted = JSONObject.quote(content)
        webView?.evaluateJavascript("window.InkWispEditor?.setDocument($quoted, $revision)", null)
        if (BuildConfig.DEBUG) {
            webView?.evaluateJavascript(
                """(function(){const e=document.querySelector('.cm-content');const l=document.querySelector('.cm-line');return JSON.stringify({text:e?.textContent,html:e?.innerHTML,color:getComputedStyle(e).color,fill:getComputedStyle(e).webkitTextFillColor,lineColor:l?getComputedStyle(l).color:null,lineFill:l?getComputedStyle(l).webkitTextFillColor:null,rect:e?.getBoundingClientRect().toJSON()})})()""",
            ) { result -> Log.d("InkWispEditor", result) }
        }
    }

    LaunchedEffect(ready, mode) {
        if (!ready) return@LaunchedEffect
        webView?.evaluateJavascript("window.InkWispEditor?.setMode('${mode.name.lowercase()}')", null)
    }

    LaunchedEffect(ready, darkTheme, reducedMotion) {
        if (!ready) return@LaunchedEffect
        webView?.setBackgroundColor(
            if (darkTheme) Color.rgb(23, 22, 20) else Color.rgb(247, 243, 234),
        )
        webView?.evaluateJavascript(
            "window.InkWispEditor?.setAppearance(${if (darkTheme) "'dark'" else "'light'"}, $reducedMotion)",
            null,
        )
    }

    LaunchedEffect(ready, predictionText) {
        if (!ready) return@LaunchedEffect
        webView?.evaluateJavascript(
            "window.InkWispEditor?.setPrediction(${JSONObject.quote(predictionText.orEmpty())})",
            null,
        )
    }

    LaunchedEffect(ready, pendingInsertion?.id) {
        val insertion = pendingInsertion ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        webView?.evaluateJavascript(
            "window.InkWispEditor?.insertText(${JSONObject.quote(insertion.text)})",
        ) { onInsertionHandled(insertion.id) }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let {
                controller.detach(it)
                it.removeJavascriptInterface("InkWispNative")
                it.destroy()
            }
            webView = null
        }
    }
}
