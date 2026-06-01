package com.musornibak.korvus.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musornibak.korvus.data.model.CustomModel
import com.musornibak.korvus.data.model.ModelRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "korvus_prefs")

class UserPrefs(private val ctx: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val customSerializer = ListSerializer(CustomModel.serializer())

    private val keyUserName = stringPreferencesKey("user_name")
    private val keyProxyApiKey = stringPreferencesKey("proxy_api_key")
    private val keyProxyBaseUrl = stringPreferencesKey("proxy_base_url")
    private val keySelectedModel = stringPreferencesKey("selected_model")
    private val keyCustomModels = stringPreferencesKey("custom_models_json")

    val userName: Flow<String?> = ctx.dataStore.data.map { it[keyUserName] ?: "" }
    val proxyApiKey: Flow<String> = ctx.dataStore.data.map { it[keyProxyApiKey] ?: "" }
    val proxyBaseUrl: Flow<String> = ctx.dataStore.data.map { it[keyProxyBaseUrl] ?: DEFAULT_BASE_URL }
    val selectedModelId: Flow<String> = ctx.dataStore.data.map { it[keySelectedModel] ?: ModelRegistry.DEFAULT_ID }

    val customModels: Flow<List<CustomModel>> = ctx.dataStore.data.map { p ->
        decodeCustom(p[keyCustomModels])
    }

    private fun decodeCustom(raw: String?): List<CustomModel> {
        if (raw.isNullOrBlank()) return emptyList()
        return try { json.decodeFromString(customSerializer, raw) } catch (_: Throwable) { emptyList() }
    }

    suspend fun saveAll(
        userName: String?,
        proxyApiKey: String?,
        proxyBaseUrl: String?
    ) {
        ctx.dataStore.edit { p ->
            userName?.trim()?.takeIf { it.isNotBlank() }?.let { p[keyUserName] = it }
            proxyApiKey?.let { p[keyProxyApiKey] = it.trim() }
            proxyBaseUrl?.trim()?.takeIf { it.isNotBlank() }?.let { p[keyProxyBaseUrl] = it.trimEnd('/') }
        }
    }

    fun setUserName(name: String) {
        scope.launch { ctx.dataStore.edit { it[keyUserName] = name.trim() } }
    }

    fun setSelectedModel(id: String) {
        scope.launch { ctx.dataStore.edit { it[keySelectedModel] = id } }
    }

    suspend fun saveCustomModels(list: List<CustomModel>) {
        val encoded = json.encodeToString(customSerializer, list)
        ctx.dataStore.edit { it[keyCustomModels] = encoded }
        ModelRegistry.setCustom(list)
    }

    suspend fun addCustomModel(m: CustomModel) {
        val cur = customModels.first()
        val dedup = cur.filter { it.id != m.id } + m
        saveCustomModels(dedup)
    }

    suspend fun removeCustomModel(id: String) {
        val cur = customModels.first()
        saveCustomModels(cur.filter { it.id != id })
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://127.0.0.1:22217/v1"
    }
}
