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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.tools.AgenticTasks
import com.musornibak.korvus.ui.components.MessageBubble
import com.musornibak.korvus.ui.components.ProviderIcon
import com.musornibak.korvus.ui.components.StreamingAssistant
import com.musornibak.korvus.ui.components.TaskPanel
import com.musornibak.korvus.ui.components.ThinkingIndicator
import com.musornibak.korvus.ui.drawer.ChatDrawerContent
import com.musornibak.korvus.ui.settings.SettingsSheet
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusSurface
import com.musornibak.korvus.ui.theme.KorvusSurfaceHi
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userName: String,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isSending by vm.isSending.collectAsStateWithLifecycle()
    val streaming by vm.streamingContent.collectAsStateWithLifecycle()
    val selectedId by vm.selectedModelId.collectAsStateWithLifecycle()
    val threads by vm.threads.collectAsStateWithLifecycle()
    val activeThreadId by vm.activeThreadId.collectAsStateWithLifecycle()
    val available by vm.availableModels.collectAsStateWithLifecycle()
    val tasks by AgenticTasks.tasks.collectAsStateWithLifecycle()
    val selectedModel = ModelRegistry.byId(selectedId)

    var input by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (settingsOpen) {
        SettingsSheet(onDismiss = { settingsOpen = false })
    }

    LaunchedEffect(messages.size, activeThreadId, streaming?.length) {
        val extra = if (streaming != null) 1 else 0
        val total = messages.size + extra
        if (total > 0) listState.animateScrollToItem(total - 1)
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
                showClear = messages.isNotEmpty(),
                onMenu = { scope.launch { drawerState.open() } },
                onClear = { vm.clearChat() }
            )

            if (messages.isEmpty() && streaming == null) {
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
                    if (streaming != null) {
                        item {
                            if (streaming!!.isEmpty()) ThinkingIndicator()
                            else StreamingAssistant(streaming!!)
                        }
                    }
                }
            }

            TaskPanel(tasks)

            InputBar(
                value = input,
                onChange = { input = it },
                sending = isSending,
                selected = selectedModel,
                models = available,
                onModelChange = { vm.selectModel(it) },
                onSend = {
                    if (input.isNotBlank()) {
                        vm.send(input, userName)
                        input = ""
                        keyboard?.hide()
                    }
                }
            )
            Text(
                "MiaMuy · ${selectedModel.displayName} · ${selectedModel.tagline}",
                style = MaterialTheme.typography.labelMedium,
                color = KorvusInkFaint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopBar(
    showClear: Boolean,
    onMenu: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.weight(1f))
        if (showClear) {
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = KorvusInkSoft)
            }
        } else {
            IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = KorvusInkSoft)
            }
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
        Text(
            "${greeting()}, $userName",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..21 -> "Evening"
        else -> "Hello"
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    sending: Boolean,
    selected: ModelInfo,
    models: List<ModelInfo>,
    onModelChange: (String) -> Unit,
    onSend: () -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val cursorColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(KorvusSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            enabled = !sending,
            singleLine = false,
            maxLines = 6,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cursorColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp, max = 160.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "Chat with MiaMuy",
                        style = LocalTextStyle.current.copy(color = KorvusInkSoft, fontSize = 16.sp)
                    )
                }
                inner()
            }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(KorvusSurfaceHi)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(KorvusSurfaceHi)
                        .clickable { pickerOpen = true }
                        .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    ProviderIcon(model = selected, size = 24.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        selected.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = pickerOpen,
                    onDismissRequest = { pickerOpen = false },
                    modifier = Modifier.background(KorvusSurface)
                ) {
                    models.forEach { m ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProviderIcon(model = m, size = 22.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(m.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(m.tagline, style = MaterialTheme.typography.labelMedium, color = KorvusInkFaint)
                                    }
                                }
                            },
                            onClick = {
                                onModelChange(m.id)
                                pickerOpen = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (value.isBlank() && !sending) {
                IconButton(onClick = { }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(2.dp))
            }
            val sendEnabled = !sending && value.isNotBlank()
            val sendBg = when {
                sending -> MaterialTheme.colorScheme.onBackground
                sendEnabled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(sendBg)
                    .clickable(enabled = sendEnabled, onClick = onSend)
            ) {
                when {
                    sending -> Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.background
                    )
                    value.isNotBlank() -> Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}
