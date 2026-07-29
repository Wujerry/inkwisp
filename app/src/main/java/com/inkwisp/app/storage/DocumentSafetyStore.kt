package com.inkwisp.app.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class DocumentSafetyStore(context: Context) {
    private val recoveryRoot = File(context.filesDir, "recovery")
    private val revisionRoot = File(context.filesDir, "revisions")

    suspend fun stageRecovery(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        recoveryRoot.mkdirs()
        recoveryFile(uri).writeText(content, Charsets.UTF_8)
    }

    suspend fun recovery(uri: Uri): String? = withContext(Dispatchers.IO) {
        recoveryFile(uri).takeIf(File::isFile)?.readText(Charsets.UTF_8)
    }

    suspend fun clearRecovery(uri: Uri) = withContext(Dispatchers.IO) {
        recoveryFile(uri).delete()
    }

    suspend fun recordRevision(uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val directory = File(revisionRoot, stableId(uri)).apply { mkdirs() }
        val files = directory.listFiles()?.sortedByDescending(File::lastModified).orEmpty()
        val newest = files.firstOrNull()
        if (newest != null && System.currentTimeMillis() - newest.lastModified() < MIN_REVISION_INTERVAL_MS) {
            return@withContext
        }
        File(directory, "${System.currentTimeMillis()}.md").writeText(content, Charsets.UTF_8)
        directory.listFiles()?.sortedByDescending(File::lastModified)?.forEachIndexed { index, file ->
            if (index >= MAX_REVISIONS || file.lastModified() < System.currentTimeMillis() - MAX_REVISION_AGE_MS) {
                file.delete()
            }
        }
    }

    private fun recoveryFile(uri: Uri): File = File(recoveryRoot, "${stableId(uri)}.md")

    private fun stableId(uri: Uri): String = sha256(uri.toString()).take(24)

    companion object {
        const val MAX_REVISIONS = 30
        const val MIN_REVISION_INTERVAL_MS = 60_000L
        const val MAX_REVISION_AGE_MS = 30L * 24 * 60 * 60 * 1_000

        fun fingerprint(content: String): String = sha256(content)

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
