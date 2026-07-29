package com.inkwisp.app.storage

import android.content.Context
import com.inkwisp.app.model.ConnectionDraft
import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.security.SecureCredentialStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ModelConnectionStore(context: Context) {
    private val preferences = context.getSharedPreferences("model_connections", Context.MODE_PRIVATE)
    private val credentials = SecureCredentialStore(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadAll(): List<ModelConnection> = preferences.getString(CONNECTIONS, null)
        ?.let { runCatching { json.decodeFromString<List<ModelConnection>>(it) }.getOrNull() }
        .orEmpty()

    fun selectedId(): String? = preferences.getString(SELECTED_ID, null)

    fun select(connectionId: String?) {
        preferences.edit().apply {
            if (connectionId == null) remove(SELECTED_ID) else putString(SELECTED_ID, connectionId)
        }.apply()
    }

    fun save(draft: ConnectionDraft): ModelConnection {
        val all = loadAll().toMutableList()
        val id = draft.id ?: UUID.randomUUID().toString()
        val connection = ModelConnection(
            id = id,
            name = draft.name.trim(),
            protocol = draft.protocol,
            baseUrl = draft.baseUrl.trim().trimEnd('/'),
            modelId = draft.modelId.trim(),
            requiresApiKey = draft.requiresApiKey,
        )
        val index = all.indexOfFirst { it.id == id }
        if (index >= 0) all[index] = connection else all += connection
        preferences.edit()
            .putString(CONNECTIONS, json.encodeToString(all))
            .putString(SELECTED_ID, id)
            .apply()
        if (draft.apiKey.isNotBlank()) credentials.put(id, draft.apiKey)
        return connection
    }

    fun delete(connectionId: String) {
        val remaining = loadAll().filterNot { it.id == connectionId }
        preferences.edit().putString(CONNECTIONS, json.encodeToString(remaining)).apply()
        credentials.remove(connectionId)
        if (selectedId() == connectionId) select(remaining.firstOrNull()?.id)
    }

    fun credential(connectionId: String): String? = credentials.get(connectionId)

    private companion object {
        const val CONNECTIONS = "connections_json"
        const val SELECTED_ID = "selected_connection_id"
    }
}
