package com.musornibak.korvus.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musornibak.korvus.data.model.ModelRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "korvus_prefs")

class UserPrefs(private val ctx: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val keyUserName = stringPreferencesKey("user_name")
    private val keyHfToken = stringPreferencesKey("hf_token")
    private val keyCompletionsToken = stringPreferencesKey("completions_token")
    private val keySelectedModel = stringPreferencesKey("selected_model")
    private val keyAutoFailover = stringPreferencesKey("auto_failover")

    val userName: Flow<String?> = ctx.dataStore.data.map { it[keyUserName] ?: "" }
    val hfToken: Flow<String> = ctx.dataStore.data.map { it[keyHfToken] ?: "" }
    val completionsToken: Flow<String> = ctx.dataStore.data.map { it[keyCompletionsToken] ?: "" }
    val selectedModelId: Flow<String> = ctx.dataStore.data.map { it[keySelectedModel] ?: ModelRegistry.DEFAULT_ID }
    val autoFailover: Flow<Boolean> = ctx.dataStore.data.map { (it[keyAutoFailover] ?: "1") == "1" }

    suspend fun saveAll(
        userName: String?,
        hfToken: String?,
        completionsToken: String?,
        autoFailover: Boolean?
    ) {
        ctx.dataStore.edit { p ->
            userName?.trim()?.takeIf { it.isNotBlank() }?.let { p[keyUserName] = it }
            hfToken?.let { p[keyHfToken] = it.trim() }
            completionsToken?.let { p[keyCompletionsToken] = it.trim() }
            autoFailover?.let { p[keyAutoFailover] = if (it) "1" else "0" }
        }
    }

    fun setUserName(name: String) {
        scope.launch { ctx.dataStore.edit { it[keyUserName] = name.trim() } }
    }

    fun setSelectedModel(id: String) {
        scope.launch { ctx.dataStore.edit { it[keySelectedModel] = id } }
    }
}
