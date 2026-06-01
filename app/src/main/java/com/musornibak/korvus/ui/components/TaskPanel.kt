package com.musornibak.korvus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.musornibak.korvus.tools.AgenticTask
import com.musornibak.korvus.ui.theme.KorvusInkFaint
import com.musornibak.korvus.ui.theme.KorvusInkSoft
import com.musornibak.korvus.ui.theme.KorvusOrange
import com.musornibak.korvus.ui.theme.KorvusSurface
import com.musornibak.korvus.ui.theme.KorvusSurfaceHi

@Composable
fun TaskPanel(tasks: List<AgenticTask>) {
    if (tasks.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, KorvusSurfaceHi, RoundedCornerShape(10.dp))
            .background(KorvusSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            "Tasks · ${tasks.count { it.status != "done" }}/${tasks.size}",
            style = MaterialTheme.typography.labelMedium,
            color = KorvusInkFaint
        )
        Spacer(Modifier.padding(top = 4.dp))
        tasks.takeLast(6).forEach { t ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text(
                    glyph(t.status),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = colorFor(t.status)
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    t.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (t.status == "done") KorvusInkFaint else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    t.id,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = KorvusInkFaint
                    )
                )
            }
        }
    }
}

private fun glyph(status: String): String = when (status) {
    "done", "completed" -> "✓"
    "in_progress" -> "▶"
    else -> "○"
}

@androidx.compose.runtime.Composable
private fun colorFor(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "done", "completed" -> KorvusInkSoft
    "in_progress" -> KorvusOrange
    else -> KorvusInkFaint
}
