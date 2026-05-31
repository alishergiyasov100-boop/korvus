package com.musornibak.korvus.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.ui.components.LogoMark
import com.musornibak.korvus.ui.components.MessageBubble
import com.musornibak.korvus.ui.components.ProviderIcon
import com.musornibak.korvus.ui.drawer.ChatDrawerContent
import com.musornibak.korvus.ui.settings.SettingsSheet
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusOrangeBg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userName: String,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isSending by vm.isSending.collectAsStateWithLifecycle()
    val status by vm.statusLine.collectAsStateWithLifecycle()
    val selectedId by vm.selectedModelId.collectAsStateWithLifecycle()
    val threads by vm.threads.collectAsStateWithLifecycle()
    val activeThreadId by vm.activeThreadId.collectAsStateWithLifecycle()
    val selectedModel = ModelRegistry.byId(selectedId)

    var input by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (settingsOpen) {
        SettingsSheet(onDismiss = { settingsOpen = false })
    }

    LaunchedEffect(messages.size, activeThreadId) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground
            ) {
                ChatDrawerContent(
                    threads = threads,
                    activeId = activeThreadId,
                    onNewChat = {
                        vm.newThread()
                        scope.launch { drawerState.close() }
                    },
                    onSelect = { id ->
                        vm.selectThread(id)
                        scope.launch { drawerState.close() }
                    },
                    onDelete = { id -> vm.deleteThread(id) },
                    onSettings = {
                        settingsOpen = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            TopBar(
                model = selectedModel,
                showClear = messages.isNotEmpty(),
                onMenu = { scope.launch { drawerState.open() } },
                onPicker = { pickerOpen = true },
                onClear = { vm.clearChat() }
            )

            if (messages.isEmpty()) {
                EmptyHero(userName = userName, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.ts }) { msg ->
                        MessageBubble(msg)
                    }
                    if (status != null) {
                        item { StatusLine(text = status!!) }
                    }
                }
            }

            InputBar(
                value = input,
                onChange = { input = it },
                enabled = !isSending,
                onSend = {
                    if (input.isNotBlank()) {
                        vm.send(input, userName)
                        input = ""
                        keyboard?.hide()
                    }
                }
            )
        }
    }

    if (pickerOpen) {
        com.musornibak.korvus.ui.components.ModelPickerSheetExposed(
            currentId = selectedModel.id,
            onDismiss = { pickerOpen = false },
            onPick = {
                vm.selectModel(it.id)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun TopBar(
    model: ModelInfo,
    showClear: Boolean,
    onMenu: () -> Unit,
    onPicker: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Menu, contentDescription = "Чаты", tint = KorvusInkSoft)
        }
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(KorvusOrangeBg)
                .clickable { onPicker() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ProviderIcon(model = model, size = 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = KorvusInkSoft
            )
        }
        Spacer(Modifier.weight(1f))
        if (showClear) {
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Очистить чат", tint = KorvusInkSoft)
            }
        } else {
            Spacer(Modifier.size(48.dp))
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
        LogoMark(size = 180.dp, animated = true)
        Spacer(Modifier.height(32.dp))
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = KorvusInkFaint)
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(PaddingValues(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp))
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                placeholder = { Text("Спроси Корвуса…", style = MaterialTheme.typography.bodyLarge) },
                enabled = enabled,
                minLines = 1,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (enabled && value.isNotBlank()) MaterialTheme.colorScheme.primary
                            else KorvusOrangeBg
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (enabled && value.isNotBlank()) MaterialTheme.colorScheme.onPrimary else KorvusInkFaint
                    )
                }
            }
        }
    }
}
