package com.inkwisp.app.storage

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "inkwisp_preferences")

class AppPreferences(private val context: Context) {
    suspend fun restore(): RestoredState {
        val values = context.dataStore.data.first()
        return RestoredState(
            workspaceUri = values[WORKSPACE_URI]?.let(Uri::parse),
            documentUri = values[DOCUMENT_URI]?.let(Uri::parse),
            scratch = values[SCRATCH_CONTENT],
            onboardingComplete = values[ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun setWorkspace(uri: Uri?) {
        context.dataStore.edit { values ->
            if (uri == null) values.remove(WORKSPACE_URI) else values[WORKSPACE_URI] = uri.toString()
        }
    }

    suspend fun setDocument(uri: Uri?) {
        context.dataStore.edit { values ->
            if (uri == null) values.remove(DOCUMENT_URI) else values[DOCUMENT_URI] = uri.toString()
        }
    }

    suspend fun setScratch(content: String) {
        context.dataStore.edit { it[SCRATCH_CONTENT] = content }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    data class RestoredState(
        val workspaceUri: Uri?,
        val documentUri: Uri?,
        val scratch: String?,
        val onboardingComplete: Boolean,
    )

    private companion object {
        val WORKSPACE_URI = stringPreferencesKey("workspace_uri")
        val DOCUMENT_URI = stringPreferencesKey("document_uri")
        val SCRATCH_CONTENT = stringPreferencesKey("scratch_content")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}
