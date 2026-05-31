package com.musornibak.korvus.ui.chat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.ui.components.LogoMark
import com.musornibak.korvus.ui.components.MessageBubble
import com.musornibak.korvus.ui.components.ModelPickerChip
import com.musornibak.korvus.ui.settings.SettingsSheet
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft

@Composable
fun ChatScreen(
    userName: String,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isSending by vm.isSending.collectAsStateWithLifecycle()
    val status by vm.statusLine.collectAsStateWithLifecycle()
    val selectedId by vm.selectedModelId.collectAsStateWithLifecycle()
    val selectedModel = ModelRegistry.byId(selectedId)

    var input by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    if (settingsOpen) {
        SettingsSheet(onDismiss = { settingsOpen = false })
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopBar(
            empty = messages.isEmpty(),
            onClear = { vm.clearChat() },
            onSettings = { settingsOpen = true }
        )

        if (messages.isEmpty()) {
            EmptyHero(userName = userName, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.ts }) { msg ->
                    MessageBubble(msg)
                }
                if (status != null) {
                    item {
                        StatusLine(text = status!!)
                    }
                }
            }
        }

        InputBar(
            value = input,
            onChange = { input = it },
            enabled = !isSending,
            selectedEmoji = selectedModel.emoji,
            selectedLabel = selectedModel.displayName,
            onSend = {
                if (input.isNotBlank()) {
                    vm.send(input, userName)
                    input = ""
                    keyboard?.hide()
                }
            },
            modelPicker = {
                ModelPickerChip(selected = selectedModel) { vm.selectModel(it.id) }
            }
        )
    }
}

@Composable
private fun TopBar(empty: Boolean, onClear: () -> Unit, onSettings: () -> Unit) {
    val logoSize by animateDpAsState(
        targetValue = if (empty) 0.dp else 28.dp,
        animationSpec = tween(320),
        label = "logo-size"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logoSize > 0.dp) {
            LogoMark(size = logoSize, animated = false)
            Spacer(Modifier.width(8.dp))
            Text(
                "Корвус",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.weight(1f))
        if (!empty) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Очистить чат", tint = KorvusInkSoft)
            }
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = KorvusInkSoft)
        }
    }
}

@Composable
private fun EmptyHero(userName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoMark(size = 156.dp, animated = true)
        Spacer(Modifier.height(28.dp))
        Text(
            "Привет, $userName.",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Я Корвус. Чем помочь?",
            style = MaterialTheme.typography.bodyLarge,
            color = KorvusInkSoft
        )
    }
}

@Composable
private fun StatusLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = KorvusInkFaint
        )
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    selectedEmoji: String,
    selectedLabel: String,
    onSend: () -> Unit,
    modelPicker: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp))
        ) {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    placeholder = { Text("Спроси Корвуса…") },
                    enabled = enabled,
                    minLines = 1,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    modelPicker()
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onSend,
                        enabled = enabled && value.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (enabled && value.isNotBlank()) MaterialTheme.colorScheme.primary else KorvusInkFaint
                        )
                    }
                }
            }
        }
    }
}
