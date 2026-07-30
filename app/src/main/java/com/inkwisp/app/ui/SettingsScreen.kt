package com.inkwisp.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.inkwisp.app.model.ConnectionDraft
import com.inkwisp.app.model.EditorUiState
import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.ModelProtocol
import com.inkwisp.app.model.ProviderCatalog
import com.inkwisp.app.model.ProviderCategory
import com.inkwisp.app.model.PredictionProtocol
import com.inkwisp.app.model.PromptFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: EditorUiState,
    onClose: () -> Unit,
    onSave: (ConnectionDraft) -> Unit,
    onTest: (ConnectionDraft) -> Unit,
    onLoadModels: (ConnectionDraft) -> Unit,
    onClearModels: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(ConnectionDraft(baseUrl = defaultBaseUrl(ModelProtocol.OpenAiChat))) }
    var providerExpanded by remember { mutableStateOf(false) }
    var protocolExpanded by remember { mutableStateOf(false) }
    var predictionProtocolExpanded by remember { mutableStateOf(false) }
    var promptFormatExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val isChinese = LocalConfiguration.current.locales[0].language == "zh"
    val tr: (String, String) -> String = { english, chinese -> if (isChinese) chinese else english }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "INKWISP  /  ${tr("SETTINGS", "设置")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(tr("Model connections", "模型连接"), style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "返回"))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val contentWidth = minOf(maxWidth, 720.dp)
            LazyColumn(
                modifier = Modifier.width(contentWidth).fillMaxHeight().align(Alignment.Center),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            item {
                SettingsSectionHeader("01", tr("Interface language", "界面语言"))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    LanguageOption(
                        label = tr("System", "跟随系统"),
                        selected = currentLanguage == "system",
                        onClick = { onLanguageChange("system") },
                        modifier = Modifier.weight(1f),
                    )
                    LanguageOption(
                        label = "English",
                        selected = currentLanguage == "en",
                        onClick = { onLanguageChange("en") },
                        modifier = Modifier.weight(1f),
                    )
                    LanguageOption(
                        label = "简体中文",
                        selected = currentLanguage == "zh-CN",
                        onClick = { onLanguageChange("zh-CN") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    tr(
                        "System mode uses Simplified Chinese for every Chinese phone locale; all others use English.",
                        "跟随系统时，手机语言为中文则使用简体中文，其余语言使用英文。",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
            item {
                SettingsSectionHeader("02", tr("Saved connections", "已保存连接"))
                Text(
                    tr("Requests go directly from this device to the selected service.", "请求从本设备直接发送到所选模型服务。"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.modelConnections.isEmpty()) {
                item {
                    Text(
                        tr("No model connection yet. Editing works without AI.", "尚未配置模型连接；不使用 AI 也可正常编辑。"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.modelConnections, key = { it.id }) { connection ->
                    ConnectionRow(
                        connection = connection,
                        selected = connection.id == state.selectedConnectionId,
                        onSelect = {
                            onSelect(connection.id)
                            val selectedDraft = connection.toDraft()
                            draft = selectedDraft
                            onLoadModels(selectedDraft)
                        },
                        onEdit = {
                            val selectedDraft = connection.toDraft()
                            draft = selectedDraft
                            onLoadModels(selectedDraft)
                        },
                        onDelete = { onDelete(connection.id) },
                        isChinese = isChinese,
                    )
                }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsSectionHeader(
                        "03",
                        if (draft.id == null) tr("Add connection", "添加连接") else tr("Edit connection", "编辑连接"),
                    )
                    if (draft.id != null) {
                        TextButton(
                            onClick = {
                                draft = ConnectionDraft(baseUrl = defaultBaseUrl(ModelProtocol.OpenAiChat))
                                onClearModels()
                            },
                        ) { Text(tr("New", "新建")) }
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                ) {
                    OutlinedTextField(
                        value = draft.name.ifBlank { tr("Choose a provider", "选择服务商") },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Provider preset", "服务商预设")) },
                        supportingText = {
                            Text(tr("Presets only fill the editable fields below.", "预设只负责填写下方字段，所有内容仍可修改。"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        ProviderCatalog.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            providerCategoryLabel(preset.category, isChinese),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    val presetDraft = draft.copy(
                                        name = preset.name,
                                        protocol = preset.protocol,
                                        baseUrl = preset.baseUrl,
                                        modelId = preset.exampleModel,
                                        requiresApiKey = preset.requiresApiKey,
                                        predictionProtocol = preset.predictionProtocol,
                                        predictionBaseUrl = preset.predictionBaseUrl,
                                        predictionModelId = preset.predictionModelId,
                                        promptFormat = preset.promptFormat,
                                    )
                                    draft = presetDraft
                                    onClearModels()
                                    if (!preset.requiresApiKey) onLoadModels(presetDraft)
                                    if (preset.category == ProviderCategory.Custom) showAdvanced = true
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = {
                        draft = draft.copy(apiKey = it)
                        onClearModels()
                    },
                    label = { Text(if (draft.id == null) "API Key" else tr("API key (blank keeps current)", "API Key（留空则保留原值）")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = draft.requiresApiKey,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { if (state.availableModels.isNotEmpty()) modelExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = draft.modelId,
                            onValueChange = { draft = draft.copy(modelId = it) },
                            label = { Text("Model ID") },
                            placeholder = { Text(tr("Type or choose a model", "输入或选择模型")) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (state.availableModels.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded)
                                }
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                        ) {
                            state.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        draft = draft.copy(modelId = model)
                                        modelExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    state.modelListMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.availableModels.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                    TextButton(
                        onClick = { onLoadModels(draft) },
                        enabled = !state.modelListLoading,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        if (state.modelListLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.modelListLoading) tr("Fetching…", "正在获取…")
                            else tr("Fetch models", "获取模型列表"),
                        )
                    }
                }
            }
            item {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(
                        if (showAdvanced) tr("Hide advanced settings", "收起高级设置")
                        else tr("Advanced settings", "高级设置"),
                    )
                }
            }
            if (showAdvanced) {
            item {
                Text(tr("Connection protocol", "连接协议"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr(
                        "These settings control assisted edits and general model requests.",
                        "这些设置用于 AI 改写及通用模型请求。",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        draft = draft.copy(requiresApiKey = !draft.requiresApiKey)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = draft.requiresApiKey,
                        onCheckedChange = { draft = draft.copy(requiresApiKey = it) },
                    )
                    Text(tr("This service requires an API key", "此服务需要 API Key"))
                }
            }
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text(tr("Name", "名称")) },
                    placeholder = { Text(tr("My writing model", "我的写作模型")) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = protocolExpanded,
                    onExpandedChange = { protocolExpanded = it },
                ) {
                    OutlinedTextField(
                        value = protocolLabel(draft.protocol, isChinese),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Protocol", "协议")) },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(protocolExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = protocolExpanded,
                        onDismissRequest = { protocolExpanded = false },
                    ) {
                        ModelProtocol.entries.forEach { protocol ->
                            DropdownMenuItem(
                                text = { Text(protocolLabel(protocol, isChinese)) },
                                onClick = {
                                    val previousDefault = defaultBaseUrl(draft.protocol)
                                    draft = draft.copy(
                                        protocol = protocol,
                                        baseUrl = if (draft.baseUrl.isBlank() || draft.baseUrl == previousDefault) {
                                            defaultBaseUrl(protocol)
                                        } else draft.baseUrl,
                                    )
                                    onClearModels()
                                    protocolExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = draft.baseUrl,
                    onValueChange = {
                        draft = draft.copy(baseUrl = it)
                        onClearModels()
                    },
                    label = { Text("Base URL") },
                    supportingText = { Text(tr("HTTPS recommended. HTTP supports local services such as Ollama.", "推荐使用 HTTPS；Ollama 等本地服务可使用 HTTP。")) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text(tr("Inline prediction", "行内预测"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr(
                        "Choose how this connection predicts text at the cursor. Auto prefers a native FIM API when it can identify one, then falls back to chat continuation.",
                        "选择当前连接在光标处续写的方式。自动模式会优先使用可识别的原生 FIM API，否则回退到对话续写。",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = predictionProtocolExpanded,
                    onExpandedChange = { predictionProtocolExpanded = it },
                ) {
                    OutlinedTextField(
                        value = predictionProtocolLabel(draft.predictionProtocol, isChinese),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Prediction protocol", "预测协议")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(predictionProtocolExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = predictionProtocolExpanded,
                        onDismissRequest = { predictionProtocolExpanded = false },
                    ) {
                        PredictionProtocol.entries.forEach { protocol ->
                            DropdownMenuItem(
                                text = { Text(predictionProtocolLabel(protocol, isChinese)) },
                                onClick = {
                                    draft = draft.copy(predictionProtocol = protocol)
                                    predictionProtocolExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            if (draft.predictionProtocol != PredictionProtocol.ChatContinuation) {
            item {
                OutlinedTextField(
                    value = draft.predictionBaseUrl,
                    onValueChange = { draft = draft.copy(predictionBaseUrl = it) },
                    label = { Text(tr("Prediction Base URL (optional)", "预测 Base URL（可选）")) },
                    supportingText = { Text(tr("Leave blank to derive it from the main Base URL.", "留空则根据主 Base URL 自动推导。")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = draft.predictionModelId,
                    onValueChange = { draft = draft.copy(predictionModelId = it) },
                    label = { Text(tr("Prediction model ID (optional)", "预测模型 ID（可选）")) },
                    supportingText = { Text(tr("Leave blank to use the main model ID.", "留空则使用主模型 ID。")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = draft.predictionMaxOutputTokens.toString(),
                    onValueChange = { value ->
                        value.filter(Char::isDigit).toIntOrNull()?.let {
                            draft = draft.copy(predictionMaxOutputTokens = it.coerceIn(16, 4096))
                        }
                    },
                    label = { Text(tr("Prediction max tokens", "预测最大 Token 数")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            }
            if (draft.predictionProtocol == PredictionProtocol.OpenAiCompatibleFim ||
                draft.predictionProtocol == PredictionProtocol.Auto) {
            item {
                ExposedDropdownMenuBox(
                    expanded = promptFormatExpanded,
                    onExpandedChange = { promptFormatExpanded = it },
                ) {
                    OutlinedTextField(
                        value = promptFormatLabel(draft.promptFormat),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Prompt format", "Prompt 格式")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(promptFormatExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = promptFormatExpanded,
                        onDismissRequest = { promptFormatExpanded = false },
                    ) {
                        PromptFormat.entries.forEach { format ->
                            val zeta = format in setOf(PromptFormat.Zeta, PromptFormat.Zeta2, PromptFormat.Zeta2_1)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(promptFormatLabel(format))
                                        if (zeta) Text(
                                            tr("Requires next-edit context; not cursor FIM", "需要下一编辑上下文，不适用于光标 FIM"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                enabled = !zeta,
                                onClick = {
                                    draft = draft.copy(promptFormat = format)
                                    promptFormatExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            }
            }
            state.connectionTestMessage?.let { message ->
                item {
                    Text(
                        message,
                        color = if (
                            message.contains("success", ignoreCase = true) ||
                            message.contains("saved", ignoreCase = true) ||
                            message.contains("成功") || message.contains("保存")
                        ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onTest(draft) },
                        enabled = !state.connectionTestInProgress,
                    ) {
                        Text(if (state.connectionTestInProgress) tr("Testing…", "测试中…") else tr("Test connection", "测试连接"))
                    }
                    Button(onClick = { onSave(draft) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(tr("Save", "保存"))
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(tr("AI data transfer", "AI 数据传输"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr(
                        "When you run an AI edit, InkWisp sends the complete active document and up to four locally retrieved workspace passages directly to the configured service. Inline prediction sends a limited prefix and suffix around the cursor; chat fallback may also include retrieved passages. InkWisp does not receive this content. The provider's privacy and retention terms apply.",
                        "运行 AI 编辑时，续墨会把当前完整文档及最多四段本地检索内容直接发送到所配置服务；行内预测发送光标附近有限的前缀与后缀，回退到对话续写时还可能包含检索片段。续墨不会接收这些内容，数据处理受服务商政策约束。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        draft = draft.copy(dataTransferAccepted = !draft.dataTransferAccepted)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = draft.dataTransferAccepted,
                        onCheckedChange = { draft = draft.copy(dataTransferAccepted = it) },
                    )
                    Text(tr("I understand and want to enable this transfer", "我已了解并希望启用此数据传输"))
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(tr("Credential privacy", "凭据隐私"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr(
                        "API keys are encrypted with Android Keystore, never shown again, and never included in settings exports or diagnostics.",
                        "API Key 使用 Android Keystore 加密，保存后不再回显，也不会进入设置导出或诊断信息。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader("04", tr("Privacy policy", "隐私政策"))
                Text(
                    tr(
                        "InkWisp stores documents, indexes, revisions, settings, and encrypted credentials on this device. It has no account, synchronization, analytics, advertising, or crash-reporting service. AI is optional; after your confirmation, an AI edit sends the active document directly to your configured provider under that provider's terms. Uninstalling removes app-private data but never deletes your workspace files.",
                        "续墨把文档索引、版本、设置及加密凭据保存在本设备，不提供账号、同步、行为分析、广告或远程崩溃报告服务。AI 为可选功能；经你确认后，AI 编辑会把当前文档直接发送到你配置的模型服务，并受该服务商条款约束。卸载会删除应用私有数据，但不会删除工作区文件。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(index: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            index,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(34.dp),
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
        else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier.clip(RoundedCornerShape(7.dp)).clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun ConnectionRow(
    connection: ModelConnection,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isChinese: Boolean,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 13.dp, top = 9.dp, end = 4.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(
                    connection.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${protocolLabel(connection.protocol, isChinese)} · ${connection.modelId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = if (isChinese) "删除连接" else "Delete connection")
            }
        }
    }
}

private fun ModelConnection.toDraft() = ConnectionDraft(
    id = id,
    name = name,
    protocol = protocol,
    baseUrl = baseUrl,
    modelId = modelId,
    requiresApiKey = requiresApiKey,
    predictionProtocol = predictionProtocol,
    predictionBaseUrl = predictionBaseUrl,
    predictionModelId = predictionModelId,
    promptFormat = promptFormat,
    predictionMaxOutputTokens = predictionMaxOutputTokens,
    dataTransferAccepted = false,
)

private fun predictionProtocolLabel(protocol: PredictionProtocol, isChinese: Boolean): String = when (protocol) {
    PredictionProtocol.Auto -> if (isChinese) "自动（推荐）" else "Auto (recommended)"
    PredictionProtocol.ChatContinuation -> if (isChinese) "对话续写" else "Chat continuation"
    PredictionProtocol.OpenAiFim -> "OpenAI FIM · prompt + suffix"
    PredictionProtocol.DeepSeekFim -> "DeepSeek FIM · /beta/completions"
    PredictionProtocol.MistralFim -> "Mistral FIM · /fim/completions"
    PredictionProtocol.OpenAiCompatibleFim -> if (isChinese) "OpenAI 兼容 · 格式化 FIM" else "OpenAI-compatible · formatted FIM"
}

private fun promptFormatLabel(format: PromptFormat): String = when (format) {
    PromptFormat.Infer -> "Infer"
    PromptFormat.Plain -> "Plain"
    PromptFormat.Zeta -> "Zeta"
    PromptFormat.Zeta2 -> "Zeta 2"
    PromptFormat.Zeta2_1 -> "Zeta 2.1"
    PromptFormat.CodeLlama -> "Code Llama"
    PromptFormat.StarCoder -> "StarCoder"
    PromptFormat.DeepSeekCoder -> "DeepSeek Coder"
    PromptFormat.Qwen -> "Qwen"
    PromptFormat.CodeGemma -> "CodeGemma"
    PromptFormat.Codestral -> "Codestral"
    PromptFormat.Glm -> "GLM"
}

private fun protocolLabel(protocol: ModelProtocol, isChinese: Boolean): String = when (protocol) {
    ModelProtocol.OpenAiChat -> if (isChinese) "OpenAI 对话补全" else "OpenAI Chat Completions"
    ModelProtocol.OpenAiResponses -> "OpenAI Responses"
    ModelProtocol.AnthropicMessages -> "Anthropic Messages"
    ModelProtocol.GoogleGemini -> "Google Gemini"
}

private fun providerCategoryLabel(category: ProviderCategory, isChinese: Boolean): String = when (category) {
    ProviderCategory.Official -> if (isChinese) "官方服务" else "Official service"
    ProviderCategory.Aggregator -> if (isChinese) "聚合与托管平台" else "Gateway or hosted platform"
    ProviderCategory.China -> if (isChinese) "国内与亚洲服务" else "China and Asia"
    ProviderCategory.Local -> if (isChinese) "本地与自托管" else "Local or self-hosted"
    ProviderCategory.Custom -> if (isChinese) "自定义协议" else "Custom protocol"
}

private fun defaultBaseUrl(protocol: ModelProtocol): String = when (protocol) {
    ModelProtocol.OpenAiChat, ModelProtocol.OpenAiResponses -> "https://api.openai.com/v1"
    ModelProtocol.AnthropicMessages -> "https://api.anthropic.com/v1"
    ModelProtocol.GoogleGemini -> "https://generativelanguage.googleapis.com/v1beta"
}
