package com.inkwisp.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inkwisp.app.model.ActiveDocument
import com.inkwisp.app.model.EditorMode
import com.inkwisp.app.model.EditorUiState
import com.inkwisp.app.model.SaveState
import com.inkwisp.app.model.ConnectionDraft
import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.PredictionState
import com.inkwisp.app.ai.CompletionInput
import com.inkwisp.app.ai.ModelGateway
import com.inkwisp.app.storage.AppPreferences
import com.inkwisp.app.storage.ModelConnectionStore
import com.inkwisp.app.storage.WorkspaceRepository
import com.inkwisp.app.storage.DocumentSafetyStore
import com.inkwisp.app.model.EditConflict
import com.inkwisp.app.model.AssistedEditProposal
import com.inkwisp.app.model.EditorInsertion
import com.inkwisp.app.model.titleFromMarkdown
import com.inkwisp.app.model.titleWithoutMarkdownExtension
import com.inkwisp.app.search.IndexDocument
import com.inkwisp.app.search.WorkspaceIndex
import com.inkwisp.app.search.findExplicitReferences
import com.inkwisp.app.export.DocumentExporter
import com.inkwisp.app.export.ExportFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkspaceRepository(application)
    private val preferences = AppPreferences(application)
    private val connectionStore = ModelConnectionStore(application)
    private val modelGateway = ModelGateway()
    private val workspaceIndex = WorkspaceIndex()
    private val safetyStore = DocumentSafetyStore(application)
    private val exporter = DocumentExporter(application)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    private var saveJob: Job? = null
    private var predictionJob: Job? = null
    private var indexingJob: Job? = null
    private var lastEditorRevision = -1L

    init {
        loadConnections()
        restoreSession()
    }

    fun openWorkspace(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                repository.persistReadWritePermission(uri)
                repository.listFiles(uri)
            }.onSuccess { (name, files) ->
                preferences.setWorkspace(uri)
                _uiState.update {
                    it.copy(
                        workspaceName = name,
                        workspaceUri = uri,
                        files = files,
                        drawerOpen = true,
                        errorMessage = null,
                    )
                }
                rebuildWorkspaceIndex(files)
            }.onFailure(::showError)
        }
    }

    fun openManagedWorkspace() {
        viewModelScope.launch {
            val uri = repository.managedWorkspaceUri
            preferences.setWorkspace(uri)
            runCatching { repository.listFiles(uri) }
                .onSuccess { (name, files) ->
                    _uiState.update {
                        it.copy(
                            workspaceName = name,
                            workspaceUri = uri,
                            files = files,
                            drawerOpen = true,
                            errorMessage = null,
                        )
                    }
                    rebuildWorkspaceIndex(files)
                    if (files.isEmpty()) newDocument()
                    else openDocument(files.first().uri, files.first().name)
                }
                .onFailure(::showError)
        }
    }

    fun openDocument(uri: Uri, title: String? = null) {
        openDocument(uri, title, autoNameFromTitle = false)
    }

    private fun openDocument(uri: Uri, title: String? = null, autoNameFromTitle: Boolean) {
        saveJob?.cancel()
        viewModelScope.launch {
            runCatching {
                repository.persistReadWritePermission(uri)
                val stored = repository.read(uri)
                val recovery = safetyStore.recovery(uri)
                Triple(stored, recovery, DocumentSafetyStore.fingerprint(stored))
            }
                .onSuccess { (stored, recovery, fingerprint) ->
                    val content = recovery ?: stored
                    val fileName = title ?: uri.lastPathSegment?.substringAfterLast('/')
                        ?: getApplication<Application>().getString(R.string.open_document)
                    val document = ActiveDocument(
                        title = titleWithoutMarkdownExtension(fileName),
                        uri = uri,
                        content = content,
                        isScratch = false,
                        revision = System.nanoTime(),
                        sourceFingerprint = fingerprint,
                        autoNameFromTitle = autoNameFromTitle,
                    )
                    lastEditorRevision = -1L
                    preferences.setDocument(uri)
                    _uiState.update {
                        it.copy(
                            activeDocument = document,
                            saveState = if (recovery == null) SaveState.Saved else SaveState.Unsaved,
                            drawerOpen = false,
                            errorMessage = null,
                            backlinks = workspaceIndex.backlinks(document.title),
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun newScratch(defaultContent: String = "") {
        saveJob?.cancel()
        lastEditorRevision = -1L
        _uiState.update {
            it.copy(
                activeDocument = ActiveDocument(
                    title = getApplication<Application>().getString(R.string.scratch_title),
                    uri = null,
                    content = defaultContent,
                    isScratch = true,
                    revision = System.nanoTime(),
                ),
                saveState = SaveState.Saved,
                drawerOpen = false,
            )
        }
    }

    fun newDocument() {
        val workspaceUri = _uiState.value.workspaceUri ?: repository.managedWorkspaceUri
        val untitled = getApplication<Application>().getString(R.string.scratch_title)
        viewModelScope.launch {
            runCatching { repository.createDocument(workspaceUri, untitled) }
                .onSuccess { uri ->
                    refreshWorkspaceFiles(workspaceUri)
                    openDocument(uri, untitled, autoNameFromTitle = true)
                }
                .onFailure(::showError)
        }
    }

    fun onEditorChanged(content: String, editorRevision: Long, cursor: Int) {
        if (editorRevision <= lastEditorRevision) return
        lastEditorRevision = editorRevision
        val current = _uiState.value.activeDocument ?: return
        if (current.content == content) return
        val derivedTitle = if (current.autoNameFromTitle) titleFromMarkdown(content) else null
        _uiState.update {
            it.copy(
                activeDocument = current.copy(content = content, title = derivedTitle ?: current.title),
                saveState = SaveState.Unsaved,
                predictionText = null,
                predictionState = if (it.selectedConnection == null) PredictionState.Disabled else PredictionState.Idle,
            )
        }
        scheduleSave()
        schedulePrediction(content, cursor)
    }

    fun setMode(mode: EditorMode) {
        _uiState.update { it.copy(editorMode = mode) }
    }

    fun toggleDrawer(open: Boolean = !_uiState.value.drawerOpen) {
        _uiState.update { it.copy(drawerOpen = open) }
    }

    fun setSearchQuery(value: String) {
        val matched = if (value.isBlank()) emptySet() else {
            workspaceIndex.retrieve(value, limit = 100).map { it.path }.toSet()
        }
        _uiState.update { it.copy(searchQuery = value, searchMatchedPaths = matched) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show, connectionTestMessage = null) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.completeOnboarding()
            _uiState.update { it.copy(showOnboarding = false) }
        }
    }

    fun saveConnection(draft: ConnectionDraft) {
        if (draft.name.isBlank() || draft.baseUrl.isBlank() || draft.modelId.isBlank()) {
            _uiState.update { it.copy(connectionTestMessage = "Name, Base URL, and Model ID are required.") }
            return
        }
        if (!draft.dataTransferAccepted) {
            _uiState.update { it.copy(connectionTestMessage = "Confirm the AI data transfer disclosure before saving.") }
            return
        }
        runCatching { connectionStore.save(draft) }
            .onSuccess {
                loadConnections()
                _uiState.update { state ->
                    state.copy(connectionTestMessage = "Connection saved.", predictionState = PredictionState.Idle)
                }
            }
            .onFailure(::showError)
    }

    fun selectConnection(connectionId: String) {
        connectionStore.select(connectionId)
        loadConnections()
        _uiState.update { it.copy(predictionState = PredictionState.Idle, predictionText = null) }
    }

    fun deleteConnection(connectionId: String) {
        connectionStore.delete(connectionId)
        loadConnections()
    }

    fun testConnection(draft: ConnectionDraft) {
        val probeKey = draft.apiKey.ifBlank {
            draft.id?.let(connectionStore::credential).orEmpty()
        }
        if (draft.baseUrl.isBlank() || draft.modelId.isBlank() || (draft.requiresApiKey && probeKey.isBlank())) {
            _uiState.update { it.copy(connectionTestMessage = "Base URL, Model ID, and the required API key are needed to test.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(connectionTestInProgress = true, connectionTestMessage = null) }
            val temporary = ModelConnection(
                id = draft.id ?: "probe",
                name = draft.name.ifBlank { "Probe" },
                protocol = draft.protocol,
                baseUrl = draft.baseUrl.trim().trimEnd('/'),
                modelId = draft.modelId.trim(),
                maxOutputTokens = 64,
                temperature = 0.0,
                requiresApiKey = draft.requiresApiKey,
            )
            val result = runCatching {
                modelGateway.probe(
                    temporary,
                    probeKey,
                    CompletionInput(
                        system = "Reply with exactly OK.",
                        prompt = "Connection test. Do not use or request document content.",
                    ),
                )
            }
            _uiState.update {
                it.copy(
                    connectionTestInProgress = false,
                    connectionTestMessage = result.fold(
                        onSuccess = { "Connected successfully." },
                        onFailure = { failure -> failure.message ?: "Connection failed." },
                    ),
                )
            }
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { persistActiveDocument() }
    }

    fun reloadExternalConflict() {
        val conflict = _uiState.value.editConflict ?: return
        val current = _uiState.value.activeDocument ?: return
        _uiState.update {
            it.copy(
                activeDocument = current.copy(
                    content = conflict.externalContent,
                    sourceFingerprint = conflict.externalFingerprint,
                    revision = System.nanoTime(),
                ),
                editConflict = null,
                saveState = SaveState.Saved,
            )
        }
    }

    fun overwriteExternalConflict() {
        val current = _uiState.value.activeDocument ?: return
        _uiState.update {
            it.copy(
                activeDocument = current.copy(sourceFingerprint = it.editConflict?.externalFingerprint),
                editConflict = null,
                saveState = SaveState.Unsaved,
            )
        }
        saveNow()
    }

    fun saveConflictCopy(uri: Uri) {
        val current = _uiState.value.activeDocument ?: return
        viewModelScope.launch {
            runCatching {
                repository.write(uri, current.content)
                safetyStore.clearRecovery(current.uri ?: uri)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        activeDocument = current.copy(
                            uri = uri,
                            title = uri.lastPathSegment?.substringAfterLast('/')
                                ?.let(::titleWithoutMarkdownExtension) ?: current.title,
                            isScratch = false,
                            sourceFingerprint = DocumentSafetyStore.fingerprint(current.content),
                            revision = System.nanoTime(),
                        ),
                        editConflict = null,
                        saveState = SaveState.Saved,
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun createDocument(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.write(uri, "") }
                .onSuccess { openDocument(uri) }
                .onFailure(::showError)
        }
    }

    fun importAttachment(uri: Uri) {
        val workspaceUri = _uiState.value.workspaceUri
        if (workspaceUri == null) {
            _uiState.update { it.copy(errorMessage = "Open a workspace before importing attachments.") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.importAttachment(workspaceUri, uri) }
                .onSuccess { relativePath ->
                    val alt = relativePath.substringAfterLast('/').substringBeforeLast('.')
                    val markdownPath = relativePath.replace(" ", "%20")
                    _uiState.update {
                        it.copy(pendingInsertion = EditorInsertion(System.nanoTime(), "![$alt]($markdownPath)"))
                    }
                }
                .onFailure(::showError)
        }
    }

    fun insertionHandled(id: Long) {
        _uiState.update {
            if (it.pendingInsertion?.id == id) it.copy(pendingInsertion = null) else it
        }
    }

    fun exportDocument(uri: Uri, format: ExportFormat) {
        val document = _uiState.value.activeDocument ?: return
        viewModelScope.launch {
            runCatching { exporter.export(uri, document.title, document.content, format) }
                .onSuccess { _uiState.update { it.copy(errorMessage = "Export created.") } }
                .onFailure(::showError)
        }
    }

    fun handleEditorCommand(command: String) {
        val payload = runCatching { JSONObject(command) }.getOrNull() ?: return
        when (payload.optString("type")) {
            "error" -> _uiState.update { it.copy(errorMessage = payload.optString("message")) }
            "assistedEdit" -> requestAssistedEdit(
                action = payload.optString("action"),
                from = payload.optInt("from", -1),
                to = payload.optInt("to", -1),
                selectedText = payload.optString("text"),
            )
        }
    }

    fun applyAssistedEdit() {
        val proposal = _uiState.value.assistedEditProposal ?: return
        val document = _uiState.value.activeDocument ?: return
        if (proposal.from !in 0..document.content.length || proposal.to !in proposal.from..document.content.length) return
        if (document.content.substring(proposal.from, proposal.to) != proposal.original) {
            _uiState.update {
                it.copy(assistedEditProposal = null, errorMessage = "The selection changed. Run the action again.")
            }
            return
        }
        val content = document.content.replaceRange(proposal.from, proposal.to, proposal.replacement)
        _uiState.update {
            it.copy(
                activeDocument = document.copy(content = content, revision = System.nanoTime()),
                assistedEditProposal = null,
                saveState = SaveState.Unsaved,
            )
        }
        scheduleSave()
    }

    fun dismissAssistedEdit() {
        _uiState.update { it.copy(assistedEditProposal = null) }
    }

    private fun requestAssistedEdit(action: String, from: Int, to: Int, selectedText: String) {
        val document = _uiState.value.activeDocument ?: return
        val connection = _uiState.value.selectedConnection
        if (connection == null) {
            _uiState.update { it.copy(showSettings = true, errorMessage = "Add a model connection to use AI edits.") }
            return
        }
        val credential = connectionStore.credential(connection.id).orEmpty()
        if (connection.requiresApiKey && credential.isBlank()) {
            _uiState.update { it.copy(showSettings = true, errorMessage = "Enter an API key for the selected connection.") }
            return
        }
        if (
            from !in 0..document.content.length || to !in from..document.content.length ||
            document.content.substring(from, to) != selectedText
        ) return
        val instruction = when (action) {
            "rewrite" -> "Rewrite for clarity while preserving meaning and Markdown structure."
            "shorten" -> "Make this substantially shorter without losing essential meaning."
            "expand" -> "Expand this naturally with useful detail while preserving voice."
            "translate" -> "Translate into the other dominant language: Chinese if English, English if Chinese."
            "grammar" -> "Fix grammar, spelling, and punctuation without changing voice."
            else -> "Improve this text while preserving its intended meaning."
        }
        val beforeTarget = document.content.substring(0, from)
        val afterTarget = document.content.substring(to)
        viewModelScope.launch {
            _uiState.update { it.copy(assistedEditLoading = true) }
            val result = runCatching {
                modelGateway.complete(
                    connection,
                    credential,
                    CompletionInput(
                        system = "You edit a target inside a complete Markdown document. Use the entire document for context, but return only replacement Markdown for the target, with no commentary or fences.",
                        prompt = buildString {
                            appendLine(instruction)
                            appendLine()
                            appendLine("<document>")
                            append(beforeTarget)
                            appendLine("<target>")
                            append(selectedText)
                            appendLine("</target>")
                            append(afterTarget)
                            appendLine("</document>")
                        },
                    ),
                )
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { replacement ->
                        state.copy(
                            assistedEditLoading = false,
                            assistedEditProposal = AssistedEditProposal(from, to, selectedText, replacement, action),
                        )
                    },
                    onFailure = { failure ->
                        state.copy(
                            assistedEditLoading = false,
                            errorMessage = failure.message ?: "AI edit failed.",
                        )
                    },
                )
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            persistActiveDocument()
        }
    }

    private fun schedulePrediction(content: String, cursor: Int) {
        predictionJob?.cancel()
        val connection = _uiState.value.selectedConnection ?: return
        val credential = connectionStore.credential(connection.id).orEmpty()
        if (connection.requiresApiKey && credential.isBlank()) return
        if (cursor !in content.indices && cursor != content.length) return
        predictionJob = viewModelScope.launch {
            delay(PREDICTION_DELAY_MS)
            _uiState.update { it.copy(predictionState = PredictionState.Loading) }
            val before = content.substring(maxOf(0, cursor - CONTEXT_BEFORE_CHARS), cursor)
            val after = content.substring(cursor, minOf(content.length, cursor + CONTEXT_AFTER_CHARS))
            val retrievalQuery = before.takeLast(RETRIEVAL_QUERY_CHARS)
            val passages = workspaceIndex.retrieve(
                query = retrievalQuery,
                explicitReferences = findExplicitReferences(retrievalQuery),
                limit = MAX_CONTEXT_FILES,
            )
            _uiState.update { it.copy(predictionContextFiles = passages.map { passage -> passage.path }) }
            val workspaceContext = passages.joinToString("\n\n") { passage ->
                "<file path=\"${passage.path}\">\n${passage.text}\n</file>"
            }
            val result = runCatching {
                modelGateway.complete(
                    connection,
                    credential,
                    CompletionInput(
                        system = PREDICTION_SYSTEM_PROMPT,
                        prompt = buildString {
                            append("<before>\n$before\n</before>\n")
                            append("<after>\n$after\n</after>")
                            if (workspaceContext.isNotBlank()) {
                                append("\n<workspace_context>\n$workspaceContext\n</workspace_context>")
                            }
                        },
                    ),
                )
            }
            if (_uiState.value.activeDocument?.content != content) return@launch
            result.onSuccess { prediction ->
                _uiState.update {
                    it.copy(predictionText = prediction, predictionState = PredictionState.Ready)
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(predictionText = null, predictionState = PredictionState.Error)
                }
            }
        }
    }

    private suspend fun persistActiveDocument() {
        val document = _uiState.value.activeDocument ?: return
        _uiState.update { it.copy(saveState = SaveState.Saving) }
        val result = runCatching {
            if (document.isScratch || document.uri == null) {
                preferences.setScratch(document.content)
                PersistOutcome(uri = document.uri, autoNameFromTitle = document.autoNameFromTitle)
            } else {
                safetyStore.stageRecovery(document.uri, document.content)
                val external = repository.read(document.uri)
                val externalFingerprint = DocumentSafetyStore.fingerprint(external)
                if (document.sourceFingerprint != null && externalFingerprint != document.sourceFingerprint) {
                    return@runCatching PersistOutcome(
                        conflict = EditConflict(external, externalFingerprint),
                        uri = document.uri,
                        autoNameFromTitle = document.autoNameFromTitle,
                    )
                }
                repository.write(document.uri, document.content)
                val hasMeaningfulTitle = titleFromMarkdown(document.content) != null
                val finalUri = if (document.autoNameFromTitle && hasMeaningfulTitle) {
                    repository.renameManagedDocument(document.uri, document.title)
                } else document.uri
                if (finalUri != document.uri) preferences.setDocument(finalUri)
                safetyStore.recordRevision(finalUri, document.content)
                safetyStore.clearRecovery(document.uri)
                if (finalUri != document.uri) safetyStore.clearRecovery(finalUri)
                PersistOutcome(
                    uri = finalUri,
                    autoNameFromTitle = document.autoNameFromTitle && !hasMeaningfulTitle,
                )
            }
        }
        result.onFailure { failure ->
            _uiState.update { it.copy(saveState = SaveState.Error, errorMessage = failure.message) }
            return
        }
        val outcome = result.getOrThrow()
        if (outcome.conflict != null) {
            _uiState.update { state -> state.copy(saveState = SaveState.Error, editConflict = outcome.conflict) }
            return
        }
        _uiState.update { state ->
            state.copy(
                saveState = SaveState.Saved,
                activeDocument = state.activeDocument?.copy(
                    uri = outcome.uri,
                    autoNameFromTitle = outcome.autoNameFromTitle,
                    sourceFingerprint = DocumentSafetyStore.fingerprint(document.content),
                ),
            )
        }
        if (outcome.uri != document.uri) {
            _uiState.value.workspaceUri?.let { refreshWorkspaceFiles(it) }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val restored = runCatching { preferences.restore() }.getOrNull()
            _uiState.update { it.copy(showOnboarding = restored?.onboardingComplete != true) }
            val workspaceUri = restored?.workspaceUri?.takeIf(repository::hasPersistedReadPermission)
                ?: repository.managedWorkspaceUri
            preferences.setWorkspace(workspaceUri)
            val files = runCatching { repository.listFiles(workspaceUri) }
                .onSuccess { (name, files) ->
                    _uiState.update { it.copy(workspaceName = name, workspaceUri = workspaceUri, files = files) }
                    rebuildWorkspaceIndex(files)
                }
                .getOrNull()?.second.orEmpty()
            val restoredDocument = restored?.documentUri?.takeIf(repository::hasPersistedReadPermission)
            if (restoredDocument != null) {
                openDocument(restoredDocument)
            } else if (files.isNotEmpty()) {
                val first = files.first()
                openDocument(first.uri, first.name)
            } else {
                if (restored?.documentUri != null) preferences.setDocument(null)
                newDocument()
            }
        }
    }

    private suspend fun refreshWorkspaceFiles(workspaceUri: Uri) {
        runCatching { repository.listFiles(workspaceUri) }.onSuccess { (name, files) ->
            _uiState.update { it.copy(workspaceName = name, workspaceUri = workspaceUri, files = files) }
            rebuildWorkspaceIndex(files)
        }
    }

    private fun loadConnections() {
        val connections = connectionStore.loadAll()
        val selected = connectionStore.selectedId()?.takeIf { id -> connections.any { it.id == id } }
            ?: connections.firstOrNull()?.id
        if (selected != null && selected != connectionStore.selectedId()) connectionStore.select(selected)
        _uiState.update {
            it.copy(
                modelConnections = connections,
                selectedConnectionId = selected,
                predictionState = if (selected == null) PredictionState.Disabled else PredictionState.Idle,
            )
        }
    }

    private fun rebuildWorkspaceIndex(files: List<com.inkwisp.app.model.WorkspaceFile>) {
        indexingJob?.cancel()
        indexingJob = viewModelScope.launch {
            _uiState.update { it.copy(workspaceIndexing = true) }
            val indexed = files.mapNotNull { file ->
                runCatching {
                    IndexDocument(
                        name = file.name,
                        path = file.relativePath,
                        text = repository.readLimited(file.uri, MAX_INDEX_FILE_CHARS),
                    )
                }.getOrNull()
            }
            workspaceIndex.replace(indexed)
            _uiState.update {
                it.copy(
                    workspaceIndexing = false,
                    backlinks = it.activeDocument?.title?.let(workspaceIndex::backlinks).orEmpty(),
                )
            }
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = error.message ?: "Something went wrong.") }
    }

    private companion object {
        const val AUTO_SAVE_DELAY_MS = 650L
        const val PREDICTION_DELAY_MS = 700L
        const val CONTEXT_BEFORE_CHARS = 12_000
        const val CONTEXT_AFTER_CHARS = 2_000
        const val RETRIEVAL_QUERY_CHARS = 1_200
        const val MAX_CONTEXT_FILES = 4
        const val MAX_INDEX_FILE_CHARS = 500_000
        const val PREDICTION_SYSTEM_PROMPT = "You continue Markdown at the cursor. Return only the natural continuation, never commentary, quotation marks, or a complete rewrite. Preserve the nearby language and Markdown style. Keep the continuation concise."
    }
}

private data class PersistOutcome(
    val conflict: EditConflict? = null,
    val uri: Uri?,
    val autoNameFromTitle: Boolean,
)
