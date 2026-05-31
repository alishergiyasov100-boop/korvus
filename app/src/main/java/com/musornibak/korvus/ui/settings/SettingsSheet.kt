package com.musornibak.korvus.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.KorvusApp
import com.musornibak.korvus.data.prefs.UserPrefs
import com.musornibak.korvus.ui.theme.KorvusInkSoft

@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    val prefs = remember { UserPrefs(KorvusApp.instance) }
    val tokenFlow by prefs.hfToken.collectAsState(initial = "")
    val nameFlow by prefs.userName.collectAsState(initial = "")
    val failoverFlow by prefs.autoFailover.collectAsState(initial = true)

    var token by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var failover by remember { mutableStateOf(true) }

    LaunchedEffect(tokenFlow) { token = tokenFlow }
    LaunchedEffect(nameFlow) { name = nameFlow ?: "" }
    LaunchedEffect(failoverFlow) { failover = failoverFlow }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            Text("HuggingFace токен", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "hf_… с правами Inference. Без него работают только Pollinations модели.",
                style = MaterialTheme.typography.bodyMedium,
                color = KorvusInkSoft
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                placeholder = { Text("hf_…") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.height(20.dp))

            Text("Имя / прозвище", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Алишер") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Авто-failover", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Если выбранная модель упадёт — Корвус молча переключится на запасную",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KorvusInkSoft
                    )
                }
                Switch(checked = failover, onCheckedChange = { failover = it })
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    prefs.setHfToken(token)
                    if (name.isNotBlank()) prefs.setUserName(name)
                    prefs.setAutoFailover(failover)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сохранить", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
