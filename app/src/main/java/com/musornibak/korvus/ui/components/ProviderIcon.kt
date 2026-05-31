package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.musornibak.korvus.data.model.ModelInfo

private data class Mono(val bg: Color, val fg: Color, val letter: String)

private fun monoFor(id: String): Mono = when (id) {
    "qwen3-coder-480b"     -> Mono(Color(0xFF6B5BFF), Color.White, "Q")
    "deepseek-v3-2"        -> Mono(Color(0xFF4D6BFE), Color.White, "D")
    "llama-4-maverick"     -> Mono(Color(0xFF0467DF), Color.White, "L")
    "kimi-k2"              -> Mono(Color(0xFF111111), Color.White, "K")
    "cp-claude-opus-4-6"   -> Mono(Color(0xFFD97757), Color.White, "C")
    "cp-claude-sonnet-4-6" -> Mono(Color(0xFFE89B7F), Color.White, "C")
    "cp-gpt-5-2"           -> Mono(Color(0xFF111111), Color.White, "G")
    "cp-gemini-3-pro"      -> Mono(Color(0xFF4285F4), Color.White, "G")
    "poll-openai-fast"     -> Mono(Color(0xFF837A6F), Color.White, "P")
    else                   -> Mono(Color(0xFF837A6F), Color.White, "?")
}

@Composable
fun ProviderIcon(
    model: ModelInfo,
    size: Dp = 32.dp
) {
    val mono = monoFor(model.id)
    val ctx = LocalContext.current
    var failed by remember(model.id) { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (model.logoUrl == null || failed) mono.bg else Color.Transparent)
    ) {
        val url = model.logoUrl
        if (url != null && !failed) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(ctx).data(url).build(),
                contentDescription = model.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
                loading = { MonoLabel(mono, size) },
                error = {
                    failed = true
                    MonoLabel(mono, size)
                }
            )
        } else {
            MonoLabel(mono, size)
        }
    }
}

@Composable
private fun MonoLabel(mono: Mono, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(mono.bg)
    ) {
        Text(
            text = mono.letter,
            color = mono.fg,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.46f).sp
            )
        )
    }
}
