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
import com.inkwisp.app.R
import java.io.File

class WorkspaceRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val supportedExtensions = setOf("md", "markdown", "txt")
    private val managedRoot = File(context.filesDir, "managed-workspace")
    val managedWorkspaceUri: Uri = Uri.parse("inkwisp://managed-workspace")

    suspend fun listFiles(treeUri: Uri): Pair<String, List<WorkspaceFile>> = withContext(Dispatchers.IO) {
        if (treeUri == managedWorkspaceUri) return@withContext listManagedFiles()
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("The selected workspace is unavailable.")
        val result = mutableListOf<WorkspaceFile>()
        collectMarkdownFiles(root, path = "", depth = 0, destination = result)
        (root.name ?: "Workspace") to result.sortedBy { it.relativePath.lowercase() }
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") return@withContext File(requireNotNull(uri.path)).readText(Charsets.UTF_8)
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IOException("Unable to open document.")
    }

    suspend fun readLimited(uri: Uri, maxChars: Int): String = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            return@withContext File(requireNotNull(uri.path)).bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(maxChars)
                val count = reader.read(buffer, 0, maxChars)
                if (count <= 0) "" else String(buffer, 0, count)
            }
        }
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val buffer = CharArray(maxChars)
            val count = reader.read(buffer, 0, maxChars)
            if (count <= 0) "" else String(buffer, 0, count)
        } ?: throw IOException("Unable to index document.")
    }

    suspend fun write(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            File(requireNotNull(uri.path)).writeText(content, Charsets.UTF_8)
            return@withContext
        }
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to save document.")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }

    fun persistReadWritePermission(uri: Uri) {
        if (uri == managedWorkspaceUri || uri.scheme == "file") return
        resolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun hasPersistedReadPermission(uri: Uri): Boolean =
        uri == managedWorkspaceUri || (uri.scheme == "file" && uri.path?.let(::File)?.isFile == true) ||
            resolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }

    suspend fun createDocument(treeUri: Uri, requestedTitle: String): Uri = withContext(Dispatchers.IO) {
        val requestedName = markdownFileName(requestedTitle)
        if (treeUri == managedWorkspaceUri) {
            managedRoot.mkdirs()
            return@withContext Uri.fromFile(uniqueFile(managedRoot, requestedName).apply { writeText("") })
        }
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("The selected workspace is unavailable.")
        val name = uniqueName(root, requestedName)
        root.createFile("text/markdown", name)?.uri
            ?: throw IOException("Unable to create the document.")
    }

    suspend fun renameManagedDocument(uri: Uri, requestedTitle: String): Uri = withContext(Dispatchers.IO) {
        if (uri.scheme != "file") return@withContext uri
        val source = File(requireNotNull(uri.path)).canonicalFile
        val root = managedRoot.canonicalFile
        if (source.parentFile != root || !source.isFile) return@withContext uri
        val requested = markdownFileName(requestedTitle)
        if (source.name.equals(requested, ignoreCase = true)) return@withContext uri
        val target = uniqueFile(root, requested)
        if (!source.renameTo(target)) throw IOException("Unable to name the document.")
        Uri.fromFile(target)
    }

    suspend fun importAttachment(treeUri: Uri, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        if (treeUri == managedWorkspaceUri) {
            val assets = File(managedRoot, "assets").apply { mkdirs() }
            val originalName = displayName(sourceUri) ?: "image-${System.currentTimeMillis()}.png"
            val safeName = originalName.replace(Regex("[^\\p{L}\\p{N}._-]"), "-").take(120)
            val target = uniqueFile(assets, safeName)
            resolver.openInputStream(sourceUri)?.use { input -> target.outputStream().use(input::copyTo) }
                ?: throw IOException("Unable to read the selected attachment.")
            return@withContext "assets/${target.name}"
        }
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("The selected workspace is unavailable.")
        val assets = root.findFile("assets")?.takeIf(DocumentFile::isDirectory)
            ?: root.createDirectory("assets")
            ?: throw IOException("Unable to create the assets folder.")
        val originalName = displayName(sourceUri) ?: "image-${System.currentTimeMillis()}.png"
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

    private fun listManagedFiles(): Pair<String, List<WorkspaceFile>> {
        managedRoot.mkdirs()
        val files = managedRoot.walkTopDown()
            .maxDepth(MAX_INDEX_DEPTH)
            .filter { file -> file.isFile && file.extension.lowercase() in supportedExtensions }
            .map { file ->
                val relative = file.relativeTo(managedRoot).invariantSeparatorsPath
                WorkspaceFile(file.name, Uri.fromFile(file), relative, relative.count { it == '/' })
            }
            .sortedBy { it.relativePath.lowercase() }
            .toList()
        return context.getString(R.string.managed_workspace_name) to files
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun markdownFileName(title: String): String {
        val safe = title.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "-")
            .replace(Regex("\\s+"), " ").trim('.', ' ').take(80).ifBlank { "Untitled" }
        return if (safe.endsWith(".md", ignoreCase = true)) safe else "$safe.md"
    }

    private fun uniqueFile(directory: File, requested: String): File {
        val direct = File(directory, requested)
        if (!direct.exists()) return direct
        val base = requested.substringBeforeLast('.', requested)
        val extension = requested.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val candidateName = if (extension.isBlank()) "$base $index" else "$base $index.$extension"
            val candidate = File(directory, candidateName)
            if (!candidate.exists()) return candidate
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
