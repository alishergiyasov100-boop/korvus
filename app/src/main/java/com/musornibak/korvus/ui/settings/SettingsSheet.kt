package com.musornibak.korvus.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.KorvusApp
import com.musornibak.korvus.data.model.CustomModel
import com.musornibak.korvus.data.prefs.UserPrefs
import com.musornibak.korvus.ui.components.ProviderIcon
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusSurfaceHi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    val prefs = remember { UserPrefs(KorvusApp.instance) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var customs by remember { mutableStateOf<List<CustomModel>>(emptyList()) }
    var addOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiKey = prefs.proxyApiKey.first()
        baseUrl = prefs.proxyBaseUrl.first()
        name = prefs.userName.first() ?: ""
        customs = prefs.customModels.first()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (addOpen) {
        AddModelSheet(
            onDismiss = { addOpen = false },
            onSave = { m ->
                scope.launch {
                    prefs.addCustomModel(m)
                    customs = prefs.customModels.first()
                    addOpen = false
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            FieldLabel("Backend URL")
            Spacer(Modifier.height(6.dp))
            Text(
                "Адрес ds-free-api. По умолчанию локальный прокси в Termux.",
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft
            )
            Spacer(Modifier.height(10.dp))
            TokenField(
                value = baseUrl,
                onChange = { baseUrl = it },
                placeholder = UserPrefs.DEFAULT_BASE_URL,
                onPaste = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) baseUrl = clip.trim()
                }
            )

            Spacer(Modifier.height(24.dp))
            FieldLabel("API ключ")
            Spacer(Modifier.height(6.dp))
            Text(
                "sk-… из админки ds-free-api (http://127.0.0.1:22217/admin).",
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft
            )
            Spacer(Modifier.height(10.dp))
            TokenField(
                value = apiKey,
                onChange = { apiKey = it },
                placeholder = "sk-…",
                onPaste = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) apiKey = clip.trim()
                }
            )

            Spacer(Modifier.height(24.dp))
            FieldLabel("Имя / прозвище")
            Spacer(Modifier.height(10.dp))
            TokenField(
                value = name,
                onChange = { name = it },
                placeholder = "Алишер",
                onPaste = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) name = clip.trim()
                }
            )

            Spacer(Modifier.height(28.dp))
            FieldLabel("Сторонние модели")
            Spacer(Modifier.height(6.dp))
            Text(
                "Подключи любую OpenAI-совместимую модель: укажи baseUrl, ключ, model id и URL логотипа.",
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft
            )
            Spacer(Modifier.height(10.dp))
            customs.forEach { m ->
                CustomModelRow(
                    model = m,
                    onDelete = {
                        scope.launch {
                            prefs.removeCustomModel(m.id)
                            customs = prefs.customModels.first()
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(KorvusSurfaceHi)
                    .clickable { addOpen = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Text("Добавить модель", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    scope.launch {
                        prefs.saveAll(
                            userName = name.takeIf { it.isNotBlank() },
                            proxyApiKey = apiKey,
                            proxyBaseUrl = baseUrl
                        )
                        saving = false
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    if (saving) "Сохраняю…" else "Сохранить",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun CustomModelRow(model: CustomModel, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ProviderIcon(model = model.toInfo(), size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(model.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(model.providerModelId, style = MaterialTheme.typography.labelMedium, color = KorvusInkFaint)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = KorvusInkFaint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddModelSheet(onDismiss: () -> Unit, onSave: (CustomModel) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://") }
    var apiKey by remember { mutableStateOf("") }
    var modelId by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    val valid = id.isNotBlank() && name.isNotBlank() && baseUrl.isNotBlank() && modelId.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text("Новая модель", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            FieldLabel("Короткий id")
            Spacer(Modifier.height(6.dp))
            TokenField(value = id, onChange = { id = it.replace(' ', '-') }, placeholder = "groq-llama", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { id = it.trim() }
            })

            Spacer(Modifier.height(14.dp))
            FieldLabel("Название")
            Spacer(Modifier.height(6.dp))
            TokenField(value = name, onChange = { name = it }, placeholder = "Groq Llama 3.3 70B", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { name = it.trim() }
            })

            Spacer(Modifier.height(14.dp))
            FieldLabel("Base URL")
            Spacer(Modifier.height(6.dp))
            TokenField(value = baseUrl, onChange = { baseUrl = it }, placeholder = "https://api.groq.com/openai/v1", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { baseUrl = it.trim() }
            })

            Spacer(Modifier.height(14.dp))
            FieldLabel("API ключ")
            Spacer(Modifier.height(6.dp))
            TokenField(value = apiKey, onChange = { apiKey = it }, placeholder = "sk-…", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { apiKey = it.trim() }
            })

            Spacer(Modifier.height(14.dp))
            FieldLabel("Model id (provider)")
            Spacer(Modifier.height(6.dp))
            TokenField(value = modelId, onChange = { modelId = it }, placeholder = "llama-3.3-70b-versatile", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { modelId = it.trim() }
            })

            Spacer(Modifier.height(14.dp))
            FieldLabel("URL логотипа (опционально)")
            Spacer(Modifier.height(6.dp))
            TokenField(value = logoUrl, onChange = { logoUrl = it }, placeholder = "https://cdn.simpleicons.org/groq", onPaste = {
                clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { logoUrl = it.trim() }
            })

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (!valid) return@Button
                    onSave(
                        CustomModel(
                            id = id.trim(),
                            displayName = name.trim(),
                            baseUrl = baseUrl.trim().trimEnd('/'),
                            apiKey = apiKey.trim(),
                            providerModelId = modelId.trim(),
                            logoUrl = logoUrl.trim().takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = valid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("Сохранить модель", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TokenField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    onPaste: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(58.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onPaste, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.ContentPaste,
                contentDescription = "Вставить",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
