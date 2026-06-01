package com.musornibak.korvus.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.KorvusApp
import com.musornibak.korvus.data.prefs.UserPrefs
import com.musornibak.korvus.ui.theme.KorvusInkSoft
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

    LaunchedEffect(Unit) {
        apiKey = prefs.proxyApiKey.first()
        baseUrl = prefs.proxyBaseUrl.first()
        name = prefs.userName.first() ?: ""
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
