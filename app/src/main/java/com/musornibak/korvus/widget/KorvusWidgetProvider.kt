package com.musornibak.korvus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.musornibak.korvus.MainActivity
import com.musornibak.korvus.R
import com.musornibak.korvus.data.model.Message
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class KorvusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    private fun updateOne(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.korvus_widget)

        val lines = lastLines(ctx, max = 3)
        views.setTextViewText(R.id.widget_line_1, lines.getOrNull(0) ?: ctx.getString(R.string.widget_hint))
        views.setTextViewText(R.id.widget_line_2, lines.getOrNull(1) ?: "")
        views.setTextViewText(R.id.widget_line_3, lines.getOrNull(2) ?: "")

        val launch = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)

        mgr.updateAppWidget(id, views)
    }

    private fun lastLines(ctx: Context, max: Int): List<String> {
        return try {
            val file = File(ctx.filesDir, "messages.json")
            if (!file.exists()) return emptyList()
            val ser = ListSerializer(Message.serializer())
            val msgs: List<Message> = Json { ignoreUnknownKeys = true }.decodeFromString(ser, file.readText())
            msgs
                .filter { it.role == "assistant" && !it.content.startsWith("[tool result:") }
                .takeLast(max)
                .reversed()
                .map { it.content.lineSequence().firstOrNull { l -> l.isNotBlank() }?.take(80) ?: "" }
                .filter { it.isNotBlank() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        fun requestUpdate(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val cn = ComponentName(ctx, KorvusWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isEmpty()) return
            val intent = Intent(ctx, KorvusWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            ctx.sendBroadcast(intent)
        }
    }
}
