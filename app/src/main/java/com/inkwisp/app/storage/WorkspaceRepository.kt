package com.inkwisp.app.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.inkwisp.app.model.WorkspaceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import android.provider.OpenableColumns

class WorkspaceRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val supportedExtensions = setOf("md", "markdown", "txt")

    suspend fun listFiles(treeUri: Uri): Pair<String, List<WorkspaceFile>> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("The selected workspace is unavailable.")
        val result = mutableListOf<WorkspaceFile>()
        collectMarkdownFiles(root, path = "", depth = 0, destination = result)
        (root.name ?: "Workspace") to result.sortedBy { it.relativePath.lowercase() }
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IOException("Unable to open document.")
    }

    suspend fun readLimited(uri: Uri, maxChars: Int): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val buffer = CharArray(maxChars)
            val count = reader.read(buffer, 0, maxChars)
            if (count <= 0) "" else String(buffer, 0, count)
        } ?: throw IOException("Unable to index document.")
    }

    suspend fun write(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to save document.")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }

    fun persistReadWritePermission(uri: Uri) {
        resolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun hasPersistedReadPermission(uri: Uri): Boolean =
        resolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }

    suspend fun importAttachment(treeUri: Uri, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("The selected workspace is unavailable.")
        val assets = root.findFile("assets")?.takeIf(DocumentFile::isDirectory)
            ?: root.createDirectory("assets")
            ?: throw IOException("Unable to create the assets folder.")
        val originalName = resolver.query(sourceUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "image-${System.currentTimeMillis()}.png"
        val safeName = originalName.replace(Regex("[^\\p{L}\\p{N}._-]"), "-").take(120)
        val name = uniqueName(assets, safeName)
        val mimeType = resolver.getType(sourceUri) ?: "application/octet-stream"
        val target = assets.createFile(mimeType, name)
            ?: throw IOException("Unable to create the attachment.")
        resolver.openInputStream(sourceUri)?.use { input ->
            resolver.openOutputStream(target.uri, "w")?.use { output -> input.copyTo(output) }
                ?: throw IOException("Unable to write the attachment.")
        } ?: throw IOException("Unable to read the selected attachment.")
        "assets/${target.name ?: name}"
    }

    private fun uniqueName(directory: DocumentFile, requested: String): String {
        if (directory.findFile(requested) == null) return requested
        val base = requested.substringBeforeLast('.', requested)
        val extension = requested.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val candidate = if (extension.isBlank()) "$base-$index" else "$base-$index.$extension"
            if (directory.findFile(candidate) == null) return candidate
            index += 1
        }
    }

    private fun collectMarkdownFiles(
        directory: DocumentFile,
        path: String,
        depth: Int,
        destination: MutableList<WorkspaceFile>,
    ) {
        directory.listFiles()
            .sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
            .forEach { file ->
                val name = file.name ?: return@forEach
                val relative = if (path.isBlank()) name else "$path/$name"
                when {
                    file.isDirectory && depth < MAX_INDEX_DEPTH ->
                        collectMarkdownFiles(file, relative, depth + 1, destination)
                    file.isFile && file.extension.lowercase() in supportedExtensions ->
                        destination += WorkspaceFile(name, file.uri, relative, depth)
                }
            }
    }

    private val DocumentFile.extension: String
        get() = name?.substringAfterLast('.', missingDelimiterValue = "") ?: ""

    private companion object {
        const val MAX_INDEX_DEPTH = 24
    }
}
