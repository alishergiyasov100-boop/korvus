package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusOrange
import com.musornibak.korvus.ui.theme.KorvusSurfaceHi
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
        "assistant" -> AssistantPlain(message)
        else -> {}
    }
}

@Composable
private fun UserBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
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
private fun AssistantPlain(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        Text(
            "\u2731",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                color = KorvusOrange
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ToolResultBubble(content: String) {
    val cleaned = content.removePrefix("[tool result:").substringAfter("]\n", content).trim()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KorvusSurfaceHi)
                .padding(10.dp)
        ) {
            Column {
                Text(
                    "\u2699 tool result",
                    style = MaterialTheme.typography.labelMedium,
                    color = KorvusInkFaint
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (cleaned.length > 600) cleaned.take(600) + "\n…" else cleaned,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KorvusInkSoft,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
