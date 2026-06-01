package com.musornibak.korvus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusOrange
import com.musornibak.korvus.ui.theme.KorvusSurface
import com.musornibak.korvus.ui.theme.KorvusSurfaceHi
import com.musornibak.korvus.ui.theme.KorvusUserBubble
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val TOOL_FENCE = Regex(
    "```\\s*tool\\s*\\n([\\s\\S]*?)\\n```",
    RegexOption.IGNORE_CASE
)
private val parserJson = Json { ignoreUnknownKeys = true }

@Composable
fun MessageBubble(message: Message) {
    when (message.role) {
        "user" -> {
            if (message.content.startsWith("[tool result:")) {
                ToolResultBlock(message.content)
            } else {
                UserBubble(message.content)
            }
        }
        "assistant" -> AssistantSegments(message.content, streaming = false)
        else -> {}
    }
}

@Composable
fun StreamingAssistant(content: String) {
    AssistantSegments(content, streaming = true)
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        AnimatedDots()
    }
}

@Composable
private fun AnimatedDots() {
    val t = rememberInfiniteTransition(label = "thinking")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val count = phase.toInt().coerceIn(0, 3)
    Text(
        "Thinking" + ".".repeat(count),
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 16.sp,
            color = KorvusInkSoft
        )
    )
}

@Composable
private fun BlinkingCursor() {
    val t = rememberInfiniteTransition(label = "cursor")
    val alpha by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Text(
        "▎",
        style = MaterialTheme.typography.bodyLarge.copy(color = KorvusOrange.copy(alpha = alpha))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserBubble(content: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(KorvusUserBubble)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { clipboard.setText(AnnotatedString(content)) }
                )
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

private sealed interface Segment {
    data class PlainText(val text: String) : Segment
    data class ToolCall(val name: String, val args: String) : Segment
}

private fun splitSegments(content: String): List<Segment> {
    val out = mutableListOf<Segment>()
    var cursor = 0
    for (m in TOOL_FENCE.findAll(content)) {
        val before = content.substring(cursor, m.range.first).trim()
        if (before.isNotEmpty()) out.add(Segment.PlainText(before))
        val body = m.groupValues[1].trim()
        val parsed = try { parserJson.parseToJsonElement(body).jsonObject } catch (_: Throwable) { null }
        val name = parsed?.get("name")?.jsonPrimitive?.content ?: "tool"
        val argsObj = parsed?.get("args") as? JsonObject
        val argsLine = argsObj?.entries?.joinToString(", ") { (k, v) ->
            val s = v.toString().trim('"')
            "$k=${s.take(80)}"
        } ?: ""
        out.add(Segment.ToolCall(name, argsLine))
        cursor = m.range.last + 1
    }
    val tail = content.substring(cursor).trim()
    if (tail.isNotEmpty()) out.add(Segment.PlainText(tail))
    if (out.isEmpty()) out.add(Segment.PlainText(""))
    return out
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantSegments(content: String, streaming: Boolean) {
    val segments = splitSegments(content)
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        val plain = segments.filterIsInstance<Segment.PlainText>()
                            .joinToString("\n") { it.text }
                            .trim()
                        if (plain.isNotEmpty()) clipboard.setText(AnnotatedString(plain))
                    }
                )
        ) {
            segments.forEachIndexed { i, seg ->
                when (seg) {
                    is Segment.PlainText -> {
                        if (seg.text.isNotEmpty() || (streaming && i == segments.lastIndex)) {
                            Row {
                                Text(
                                    seg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (streaming && i == segments.lastIndex) BlinkingCursor()
                            }
                        }
                    }
                    is Segment.ToolCall -> ToolCallFrame(seg.name, seg.args)
                }
                if (i < segments.lastIndex) Spacer(Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ToolCallFrame(name: String, args: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, KorvusSurfaceHi, RoundedCornerShape(8.dp))
            .background(KorvusSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row {
            Text(
                "● ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = KorvusOrange
                )
            )
            Text(
                "$name($args)",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun ToolResultBlock(content: String) {
    val cleanedHeader = content.removePrefix("[tool result:").substringBefore("]").trim()
    val body = content.substringAfter("]\n", "").trim()
    val display = if (body.length > 600) body.take(600) + "\n…" else body
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 2.dp, bottom = 8.dp)
    ) {
        Row {
            Text(
                "\u2937 ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = KorvusInkFaint
                )
            )
            Column {
                Text(
                    cleanedHeader,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = KorvusInkFaint
                    )
                )
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    display,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = KorvusInkSoft
                    )
                )
            }
        }
    }
}
