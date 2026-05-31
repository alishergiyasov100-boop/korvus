package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.korvus.data.model.ModelInfo

data class ProviderMark(val bg: Color, val fg: Color, val letter: String)

private fun markFor(id: String): ProviderMark = when (id) {
    "qwen3-coder-480b"     -> ProviderMark(Color(0xFF6B5BFF), Color.White, "Q")
    "deepseek-v3-2"        -> ProviderMark(Color(0xFF2E6FE3), Color.White, "D")
    "llama-4-maverick"     -> ProviderMark(Color(0xFF1E8F4E), Color.White, "L")
    "kimi-k2"              -> ProviderMark(Color(0xFF0E8E89), Color.White, "K")
    "cp-claude-opus-4-6"   -> ProviderMark(Color(0xFFCB6A2E), Color.White, "C")
    "cp-claude-sonnet-4-6" -> ProviderMark(Color(0xFFE0883D), Color.White, "C")
    "cp-gpt-5-2"           -> ProviderMark(Color(0xFF12A37F), Color.White, "G")
    "cp-gemini-3-pro"      -> ProviderMark(Color(0xFF3A6FF5), Color.White, "G")
    "poll-openai-fast"     -> ProviderMark(Color(0xFF12A37F), Color.White, "P")
    else                   -> ProviderMark(Color(0xFF8B847B), Color.White, "?")
}

@Composable
fun ProviderIcon(
    model: ModelInfo,
    size: Dp = 32.dp
) {
    val mark = markFor(model.id)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(mark.bg)
    ) {
        Text(
            text = mark.letter,
            color = mark.fg,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.46f).sp
            )
        )
    }
}
