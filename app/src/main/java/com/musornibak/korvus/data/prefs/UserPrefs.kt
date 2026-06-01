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
    private val keySiliconflowToken = stringPreferencesKey("siliconflow_token")
    private val keySelectedModel = stringPreferencesKey("selected_model")

    val userName: Flow<String?> = ctx.dataStore.data.map { it[keyUserName] ?: "" }
    val siliconflowToken: Flow<String> = ctx.dataStore.data.map { it[keySiliconflowToken] ?: "" }
    val selectedModelId: Flow<String> = ctx.dataStore.data.map { it[keySelectedModel] ?: ModelRegistry.DEFAULT_ID }

    suspend fun saveAll(
        userName: String?,
        siliconflowToken: String?
    ) {
        ctx.dataStore.edit { p ->
            userName?.trim()?.takeIf { it.isNotBlank() }?.let { p[keyUserName] = it }
            siliconflowToken?.let { p[keySiliconflowToken] = it.trim() }
        }
    }

    fun setUserName(name: String) {
        scope.launch { ctx.dataStore.edit { it[keyUserName] = name.trim() } }
    }

    fun setSelectedModel(id: String) {
        scope.launch { ctx.dataStore.edit { it[keySelectedModel] = id } }
    }
}
