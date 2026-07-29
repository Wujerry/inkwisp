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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: EditorUiState,
    onClose: () -> Unit,
    onSave: (ConnectionDraft) -> Unit,
    onTest: (ConnectionDraft) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(ConnectionDraft(baseUrl = defaultBaseUrl(ModelProtocol.OpenAiChat))) }
    var protocolExpanded by remember { mutableStateOf(false) }
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
                            draft = connection.toDraft()
                        },
                        onEdit = { draft = connection.toDraft() },
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
                            },
                        ) { Text(tr("New", "新建")) }
                    }
                }
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
                    onValueChange = { draft = draft.copy(baseUrl = it) },
                    label = { Text("Base URL") },
                    supportingText = { Text(tr("HTTPS recommended. HTTP supports local services such as Ollama.", "推荐使用 HTTPS；Ollama 等本地服务可使用 HTTP。")) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = draft.modelId,
                    onValueChange = { draft = draft.copy(modelId = it) },
                    label = { Text("Model ID") },
                    placeholder = { Text(tr("Provider model identifier", "服务商模型标识")) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { draft = draft.copy(apiKey = it) },
                    label = { Text(if (draft.id == null) "API Key" else tr("API key (blank keeps current)", "API Key（留空则保留原值）")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = draft.requiresApiKey,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.connectionTestMessage?.let { message ->
                item {
                    Text(
                        message,
                        color = if (
                            message.contains("success", ignoreCase = true) ||
                            message.contains("saved", ignoreCase = true)
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
                        "When AI is enabled, InkWisp sends the active document excerpt and up to four locally retrieved workspace passages directly to the model service you configured. InkWisp does not receive this content. The provider's privacy and retention terms apply.",
                        "启用 AI 后，续墨会把当前文档片段及最多四段本地检索到的工作区内容，直接发送到你配置的模型服务。续墨不会接收这些内容；数据处理受该服务商的隐私与保留政策约束。",
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
                        "InkWisp stores documents, indexes, revisions, settings, and encrypted credentials on this device. It has no account, synchronization, analytics, advertising, or crash-reporting service. AI is optional; when enabled after your confirmation, document excerpts are sent directly to your configured provider under that provider's terms. Uninstalling removes app-private data but never deletes your workspace files.",
                        "续墨把文档索引、版本、设置及加密凭据保存在本设备，不提供账号、同步、行为分析、广告或远程崩溃报告服务。AI 为可选功能；经你确认后，文档片段会直接发送到你配置的模型服务，并受该服务商条款约束。卸载会删除应用私有数据，但不会删除工作区文件。",
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
    dataTransferAccepted = false,
)

private fun protocolLabel(protocol: ModelProtocol, isChinese: Boolean): String = when (protocol) {
    ModelProtocol.OpenAiChat -> if (isChinese) "OpenAI 对话补全" else "OpenAI Chat Completions"
    ModelProtocol.OpenAiResponses -> "OpenAI Responses"
    ModelProtocol.AnthropicMessages -> "Anthropic Messages"
    ModelProtocol.GoogleGemini -> "Google Gemini"
}

private fun defaultBaseUrl(protocol: ModelProtocol): String = when (protocol) {
    ModelProtocol.OpenAiChat, ModelProtocol.OpenAiResponses -> "https://api.openai.com/v1"
    ModelProtocol.AnthropicMessages -> "https://api.anthropic.com/v1"
    ModelProtocol.GoogleGemini -> "https://generativelanguage.googleapis.com/v1beta"
}
