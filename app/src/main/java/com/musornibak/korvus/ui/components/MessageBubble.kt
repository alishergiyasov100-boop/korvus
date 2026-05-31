package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.ui.theme.KorvusBotBubble
import com.musornibak.korvus.ui.theme.KorvusDivider
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusUserBubble

@Composable
fun MessageBubble(message: Message) {
    when (message.role) {
        "user" -> {
            if (message.content.startsWith("[tool result:")) {
                ToolResultBubble(message.content)
            } else {
                UserBubble(message.content)
            }
        }
        "assistant" -> AssistantBubble(message)
        else -> {}
    }
}

@Composable
private fun UserBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(KorvusUserBubble)
                .padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp))
        ) {
            Text(
                content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun AssistantBubble(message: Message) {
    val modelLabel = message.modelId?.let { ModelRegistry.byId(it).displayName }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(KorvusBotBubble)
                .padding(PaddingValues(horizontal = 14.dp, vertical = 10.dp))
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (modelLabel != null) {
                Text(
                    "Корвус · $modelLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = KorvusInkFaint,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolResultBubble(content: String) {
    val cleaned = content.removePrefix("[tool result:").substringAfter("]\n", content).trim()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        ) {
            Column {
                Text(
                    "🔧 результат tool",
                    style = MaterialTheme.typography.labelMedium,
                    color = KorvusInkFaint
                )
                Text(
                    if (cleaned.length > 600) cleaned.take(600) + "\n…" else cleaned,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KorvusInkFaint,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(KorvusDivider.copy(alpha = 0.4f))
                        .padding(6.dp)
                )
            }
        }
    }
}
