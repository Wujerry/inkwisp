package com.inkwisp.app.model

import android.net.Uri

enum class EditorMode { Instant, Source }

enum class SaveState { Saved, Saving, Unsaved, Error }

data class WorkspaceFile(
    val name: String,
    val uri: Uri,
    val relativePath: String,
    val depth: Int,
)

data class ActiveDocument(
    val title: String,
    val uri: Uri?,
    val content: String,
    val isScratch: Boolean,
    val revision: Long = 0,
    val sourceFingerprint: String? = null,
)

data class EditConflict(
    val externalContent: String,
    val externalFingerprint: String,
)

data class AssistedEditProposal(
    val from: Int,
    val to: Int,
    val original: String,
    val replacement: String,
    val action: String,
)

data class EditorInsertion(
    val id: Long,
    val text: String,
)

data class EditorUiState(
    val workspaceName: String? = null,
    val workspaceUri: Uri? = null,
    val files: List<WorkspaceFile> = emptyList(),
    val activeDocument: ActiveDocument? = null,
    val editorMode: EditorMode = EditorMode.Instant,
    val saveState: SaveState = SaveState.Saved,
    val drawerOpen: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val modelConnections: List<ModelConnection> = emptyList(),
    val selectedConnectionId: String? = null,
    val predictionState: PredictionState = PredictionState.Disabled,
    val predictionText: String? = null,
    val showSettings: Boolean = false,
    val connectionTestInProgress: Boolean = false,
    val connectionTestMessage: String? = null,
    val workspaceIndexing: Boolean = false,
    val predictionContextFiles: List<String> = emptyList(),
    val editConflict: EditConflict? = null,
    val searchMatchedPaths: Set<String> = emptySet(),
    val backlinks: List<String> = emptyList(),
    val assistedEditLoading: Boolean = false,
    val assistedEditProposal: AssistedEditProposal? = null,
    val pendingInsertion: EditorInsertion? = null,
    val showOnboarding: Boolean = true,
) {
    val filteredFiles: List<WorkspaceFile>
        get() = if (searchQuery.isBlank()) files else files.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.relativePath.contains(searchQuery, ignoreCase = true) ||
                it.relativePath in searchMatchedPaths
        }

    val selectedConnection: ModelConnection?
        get() = modelConnections.firstOrNull { it.id == selectedConnectionId }
}
