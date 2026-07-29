package com.inkwisp.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inkwisp.app.editor.InkWispEditor
import com.inkwisp.app.editor.EditorController
import com.inkwisp.app.model.EditorMode
import com.inkwisp.app.model.EditorUiState
import com.inkwisp.app.model.SaveState
import com.inkwisp.app.ui.theme.InkWispTheme
import com.inkwisp.app.ui.SettingsScreen
import com.inkwisp.app.export.ExportFormat
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModels()

    private val openWorkspace = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::openWorkspace)
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.openDocument(it) } }

    private val createConflictCopy = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> uri?.let(viewModel::saveConflictCopy) }

    private val exportMarkdown = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> uri?.let { viewModel.exportDocument(it, ExportFormat.Markdown) } }
    private val exportHtml = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri -> uri?.let { viewModel.exportDocument(it, ExportFormat.Html) } }
    private val exportPdf = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let { viewModel.exportDocument(it, ExportFormat.Pdf) } }
    private val exportText = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri -> uri?.let { viewModel.exportDocument(it, ExportFormat.PlainText) } }
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> uri?.let(viewModel::createDocument) }
    private val importImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importAttachment) }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPersistedLanguage()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleViewIntent(intent)
        setContent {
            InkWispTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                InkWispApp(
                    state = state,
                    viewModel = viewModel,
                    onOpenWorkspace = { openWorkspace.launch(null) },
                    onOpenDocument = { openDocument.launch(arrayOf("text/markdown", "text/plain")) },
                    onNewDocument = { createDocument.launch("untitled.md") },
                    onInsertImage = { importImage.launch(arrayOf("image/*")) },
                    onSaveConflictCopy = { title -> createConflictCopy.launch("${title.substringBeforeLast('.')}-copy.md") },
                    onExport = { format, title ->
                        val base = title.substringBeforeLast('.').ifBlank { "document" }
                        when (format) {
                            ExportFormat.Markdown -> exportMarkdown.launch("$base.md")
                            ExportFormat.Html -> exportHtml.launch("$base.html")
                            ExportFormat.Pdf -> exportPdf.launch("$base.pdf")
                            ExportFormat.PlainText -> exportText.launch("$base.txt")
                        }
                    },
                    currentLanguage = languagePreferenceMode(),
                    onLanguageChange = ::setLanguage,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { viewModel.openDocument(it) }
        }
    }

    private fun languagePreferences() = getSharedPreferences("interface_language", MODE_PRIVATE)

    private fun applyPersistedLanguage() {
        val tag = resolvedLanguageTag()
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun setLanguage(tag: String) {
        if (tag == SYSTEM_LANGUAGE) {
            languagePreferences().edit().remove(LANGUAGE_KEY).apply()
        } else {
            languagePreferences().edit().putString(LANGUAGE_KEY, tag).apply()
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(resolvedLanguageTag()),
        )
    }

    private fun resolvedLanguageTag(): String =
        languagePreferences().getString(LANGUAGE_KEY, null) ?: systemLanguageTag()

    private fun languagePreferenceMode(): String =
        languagePreferences().getString(LANGUAGE_KEY, null) ?: SYSTEM_LANGUAGE

    private fun systemLanguageTag(): String {
        val language = android.content.res.Resources.getSystem().configuration.locales[0].language
        return if (language.equals("zh", ignoreCase = true)) "zh-CN" else "en"
    }

    private companion object {
        const val LANGUAGE_KEY = "language_tag"
        const val SYSTEM_LANGUAGE = "system"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InkWispApp(
    state: EditorUiState,
    viewModel: EditorViewModel,
    onOpenWorkspace: () -> Unit,
    onOpenDocument: () -> Unit,
    onNewDocument: () -> Unit,
    onInsertImage: () -> Unit,
    onSaveConflictCopy: (String) -> Unit,
    onExport: (ExportFormat, String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val reducedMotion = rememberReducedMotion()
    val drawerState = rememberDrawerState(
        initialValue = if (state.drawerOpen) DrawerValue.Open else DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.drawerOpen) {
        if (state.drawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.currentValue) {
        val open = drawerState.currentValue == DrawerValue.Open
        if (open != state.drawerOpen) viewModel.toggleDrawer(open)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                WorkspacePanel(
                    state = state,
                    onOpenWorkspace = onOpenWorkspace,
                    onOpenDocument = onOpenDocument,
                    onNewDocument = onNewDocument,
                    onFileSelected = { viewModel.openDocument(it.uri, it.name) },
                    onSearch = viewModel::setSearchQuery,
                    onSettings = { viewModel.showSettings(true) },
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                )
                EditorSurface(
                    state = state,
                    darkTheme = darkTheme,
                    reducedMotion = reducedMotion,
                    onMenu = {},
                    onModeChange = viewModel::setMode,
                    onContentChanged = viewModel::onEditorChanged,
                    onEditorCommand = viewModel::handleEditorCommand,
                    onInsertionHandled = viewModel::insertionHandled,
                    onSave = viewModel::saveNow,
                    onInsertImage = onInsertImage,
                    onExport = { format ->
                        onExport(format, state.activeDocument?.title ?: "document.md")
                    },
                    showMenu = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.width(318.dp),
                    ) {
                        WorkspacePanel(
                            state = state,
                            onOpenWorkspace = onOpenWorkspace,
                            onOpenDocument = onOpenDocument,
                            onNewDocument = onNewDocument,
                            onFileSelected = {
                                viewModel.openDocument(it.uri, it.name)
                                scope.launch { drawerState.close() }
                            },
                            onSearch = viewModel::setSearchQuery,
                            onSettings = { viewModel.showSettings(true) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            ) {
                EditorSurface(
                    state = state,
                    darkTheme = darkTheme,
                    reducedMotion = reducedMotion,
                    onMenu = { scope.launch { drawerState.open() } },
                    onModeChange = viewModel::setMode,
                    onContentChanged = viewModel::onEditorChanged,
                    onEditorCommand = viewModel::handleEditorCommand,
                    onInsertionHandled = viewModel::insertionHandled,
                    onSave = viewModel::saveNow,
                    onInsertImage = onInsertImage,
                    onExport = { format ->
                        onExport(format, state.activeDocument?.title ?: "document.md")
                    },
                    showMenu = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        if (state.showSettings) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SettingsScreen(
                    state = state,
                    onClose = { viewModel.showSettings(false) },
                    onSave = viewModel::saveConnection,
                    onTest = viewModel::testConnection,
                    onSelect = viewModel::selectConnection,
                    onDelete = viewModel::deleteConnection,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange,
                )
            }
        }
        state.editConflict?.let {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("File changed outside InkWisp") },
                text = {
                    Text("Your edits are safe. Choose which version to keep; InkWisp will not overwrite the external change silently.")
                },
                confirmButton = {
                    TextButton(onClick = viewModel::overwriteExternalConflict) { Text("Keep mine") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = viewModel::reloadExternalConflict) { Text("Reload external") }
                        TextButton(
                            onClick = {
                                onSaveConflictCopy(state.activeDocument?.title ?: "document.md")
                            },
                        ) { Text("Save a copy") }
                    }
                },
            )
        }
        state.assistedEditProposal?.let { proposal ->
            AlertDialog(
                onDismissRequest = viewModel::dismissAssistedEdit,
                title = { Text("Review ${proposal.action}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Original", style = MaterialTheme.typography.labelLarge)
                        Text(proposal.original, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Text("Proposed", style = MaterialTheme.typography.labelLarge)
                        Text(proposal.replacement)
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::applyAssistedEdit) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissAssistedEdit) { Text("Cancel") }
                },
            )
        }
        if (state.showOnboarding) {
            OnboardingScreen(onStart = viewModel::completeOnboarding)
        }
    }
}

@Composable
private fun OnboardingScreen(onStart: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val contentWidth = if (maxWidth > 620.dp) 520.dp else maxWidth
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(420)) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
                    initialOffsetY = { it / 12 },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .width(contentWidth)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .padding(start = 30.dp, top = 38.dp, end = 30.dp, bottom = 30.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_inkwash),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(112.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "INKWISP  /  续墨",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(19.dp))
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.fillMaxWidth(0.94f),
                    )
                    Spacer(Modifier.height(34.dp))
                    listOf(
                        R.string.onboarding_local,
                        R.string.onboarding_editor,
                        R.string.onboarding_ai,
                    ).forEachIndexed { index, message ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "0${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp).padding(top = 2.dp),
                            )
                            Text(
                                text = stringResource(message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (index < 2) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 34.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
                    ) {
                        Text(stringResource(R.string.start_writing))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSurface(
    state: EditorUiState,
    darkTheme: Boolean,
    reducedMotion: Boolean,
    onMenu: () -> Unit,
    onModeChange: (EditorMode) -> Unit,
    onContentChanged: (String, Long, Int) -> Unit,
    onEditorCommand: (String) -> Unit,
    onInsertionHandled: (Long) -> Unit,
    onSave: () -> Unit,
    onInsertImage: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    showMenu: Boolean,
    modifier: Modifier = Modifier,
) {
    val editorController = remember { EditorController() }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DocumentHeader(
                state = state,
                showMenu = showMenu,
                onMenu = onMenu,
                onSave = onSave,
                onExport = onExport,
                onModeChange = onModeChange,
            )
        },
        bottomBar = {
            EditingToolbar(
                onCommand = editorController::runFormatCommand,
                onAssistedEdit = editorController::requestAssistedEdit,
                onInsertImage = onInsertImage,
                modifier = Modifier.imePadding().navigationBarsPadding(),
            )
        },
    ) { padding ->
        val document = state.activeDocument
        if (document == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_inkwash),
                    contentDescription = null,
                    modifier = Modifier.size(92.dp),
                )
                Text(
                    stringResource(R.string.no_workspace),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.empty_editor_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            InkWispEditor(
                content = document.content,
                revision = document.revision,
                mode = state.editorMode,
                darkTheme = darkTheme,
                reducedMotion = reducedMotion,
                predictionText = state.predictionText,
                pendingInsertion = state.pendingInsertion,
                onInsertionHandled = onInsertionHandled,
                onContentChanged = onContentChanged,
                onCommand = onEditorCommand,
                controller = editorController,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun DocumentHeader(
    state: EditorUiState,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onSave: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onModeChange: (EditorMode) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showMenu) {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.workspace))
                    }
                } else {
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    Modifier
                        .width(3.dp)
                        .height(31.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.activeDocument?.let { document ->
                            if (document.isScratch) stringResource(R.string.scratch_title) else document.title
                        } ?: stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SaveStatus(state)
                }
                HeaderModeToggle(mode = state.editorMode, onModeChange = onModeChange)
                IconButton(onClick = onSave, enabled = state.saveState != SaveState.Saving) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.saved),
                        tint = if (state.saveState == SaveState.Saved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                ExportMenu(onExport)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun ExportMenu(onExport: (ExportFormat) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Export")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExportFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text("Export ${format.name}") },
                    onClick = {
                        expanded = false
                        onExport(format)
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkspacePanel(
    state: EditorUiState,
    onOpenWorkspace: () -> Unit,
    onOpenDocument: () -> Unit,
    onNewDocument: () -> Unit,
    onFileSelected: (com.inkwisp.app.model.WorkspaceFile) -> Unit,
    onSearch: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 21.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_inkwash),
                contentDescription = null,
                modifier = Modifier.size(47.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.workspaceName ?: stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.workspace),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
        BasicTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            decorationBox = { innerField ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    stringResource(R.string.search),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            innerField()
                        }
                        AnimatedVisibility(
                            visible = state.searchQuery.isNotEmpty(),
                            enter = fadeIn(tween(160)),
                            exit = fadeOut(tween(120)),
                        ) {
                            IconButton(onClick = { onSearch("") }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            WorkspaceQuickAction(
                icon = Icons.Default.FolderOpen,
                label = stringResource(R.string.open_workspace_short),
                onClick = onOpenWorkspace,
                modifier = Modifier.weight(1f),
            )
            WorkspaceQuickAction(
                icon = Icons.Default.Description,
                label = stringResource(R.string.open_document_short),
                onClick = onOpenDocument,
                modifier = Modifier.weight(1f),
            )
            WorkspaceQuickAction(
                icon = Icons.Default.Add,
                label = stringResource(R.string.new_document_short),
                onClick = onNewDocument,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.filteredFiles, key = { it.uri.toString() }) { file ->
                WorkspaceFileRow(
                    name = file.name,
                    depth = file.depth,
                    selected = state.activeDocument?.uri == file.uri,
                    onClick = { onFileSelected(file) },
                )
            }
            if (state.backlinks.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    )
                    Row(
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.backlinks),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(state.backlinks, key = { "backlink:$it" }) { path ->
                    Text(
                        text = path,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.files.firstOrNull { it.relativePath == path }?.let(onFileSelected)
                            }
                            .padding(horizontal = 24.dp, vertical = 9.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(9.dp))
            Text(
                text = "InkWisp · 续墨",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkspaceQuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(7.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WorkspaceFileRow(
    name: String,
    depth: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier.fillMaxWidth().background(color).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(42.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = (16 + depth * 12).dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SaveStatus(state: EditorUiState) {
    val saveText = when (state.saveState) {
        SaveState.Saved -> stringResource(R.string.saved)
        SaveState.Saving -> stringResource(R.string.saving)
        SaveState.Unsaved, SaveState.Error -> stringResource(R.string.unsaved)
    }
    val wordCount = remember(state.activeDocument?.content) {
        WORD_REGEX.findAll(state.activeDocument?.content.orEmpty()).count()
    }
    val wordCountText = stringResource(R.string.word_count, wordCount)
    val contextCountText = stringResource(R.string.context_count, state.predictionContextFiles.size)
    val text = buildString {
        append(saveText)
        append(" · ")
        append(wordCountText)
        if (state.predictionState == com.inkwisp.app.model.PredictionState.Loading) append(" · AI…")
        if (state.predictionContextFiles.isNotEmpty()) {
            append(" · ")
            append(contextCountText)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (state.saveState == SaveState.Error) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EditingToolbar(
    onCommand: (String) -> Unit,
    onAssistedEdit: (String) -> Unit,
    onInsertImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        "H" to "heading",
                        "B" to "bold",
                        "I" to "italic",
                        "•" to "bullet",
                        "☐" to "task",
                        "</>" to "code",
                    ).forEach { (label, command) ->
                        InkTool(label = label, onClick = { onCommand(command) })
                    }
                    InkIconTool(
                        icon = Icons.Default.Image,
                        contentDescription = stringResource(R.string.insert_image),
                        onClick = onInsertImage,
                    )
                }
                Box(
                    Modifier.padding(horizontal = 6.dp).width(1.dp).height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                AssistedEditMenu(onAssistedEdit)
            }
        }
    }
}

@Composable
private fun InkTool(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(110),
        label = "toolbar-press",
    )
    Box(
        modifier = Modifier
            .size(39.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (label == "B") FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun InkIconTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(110),
        label = "toolbar-icon-press",
    )
    Box(
        modifier = Modifier
            .size(39.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(9.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun HeaderModeToggle(mode: EditorMode, onModeChange: (EditorMode) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = tween(110),
        label = "header-mode-press",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (mode == EditorMode.Source) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onModeChange(if (mode == EditorMode.Instant) EditorMode.Source else EditorMode.Instant)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (mode == EditorMode.Instant) Icons.Default.Visibility else Icons.Default.Code,
            contentDescription = stringResource(
                if (mode == EditorMode.Instant) R.string.instant_mode else R.string.source_mode,
            ),
            tint = if (mode == EditorMode.Source) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistedEditMenu(onAction: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isChinese = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "zh"
    val tr: (String, String) -> String = { en, zh -> if (isChinese) zh else en }
    Box {
        InkAiButton(onClick = { expanded = true })
        if (expanded) {
            ModalBottomSheet(
                onDismissRequest = { expanded = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = {
                    Box(
                        Modifier.padding(top = 12.dp, bottom = 8.dp).width(32.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline),
                    )
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
                        .navigationBarsPadding(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                tr("AI assistance", "AI 写作助手"),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                tr("Work with the text you selected", "处理你刚刚选中的文字"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val actions = listOf(
                        Triple(tr("Rewrite", "改写"), tr("Keep the meaning, change the expression", "保留原意，换一种更自然的表达"), "rewrite"),
                        Triple(tr("Shorten", "精简"), tr("Remove repetition and tighten the rhythm", "去掉重复，让表达更紧凑"), "shorten"),
                        Triple(tr("Expand", "扩写"), tr("Develop the idea with useful detail", "补充有用细节，展开当前观点"), "expand"),
                        Triple(tr("Translate", "翻译"), tr("Translate while preserving tone", "在保留语气的前提下翻译"), "translate"),
                        Triple(tr("Polish grammar", "润色语法"), tr("Fix grammar and awkward phrasing", "修正语法和不自然的措辞"), "grammar"),
                    )
                    actions.forEachIndexed { index, (title, description, action) ->
                        AiActionRow(
                            index = index + 1,
                            title = title,
                            description = description,
                            onClick = {
                                expanded = false
                                onAction(action)
                            },
                        )
                        if (index < actions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 42.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            tr(
                                "Only the selected text is sent to your configured model.",
                                "只会把选中文字发送给你配置的模型服务。",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InkAiButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(110),
        label = "ai-button-press",
    )
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("AI", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AiActionRow(
    index: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            index.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(42.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

private val WORD_REGEX = Regex("[\\p{L}\\p{N}_'-]+")
