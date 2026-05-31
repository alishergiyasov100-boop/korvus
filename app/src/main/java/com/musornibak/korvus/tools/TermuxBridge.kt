package com.musornibak.korvus.tools

import android.content.Context
import android.content.Intent
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TermuxBridge {

    private const val TAG = "TermuxBridge"
    private const val PORT_PRIMARY = 47823
    private const val PORT_FALLBACK = 47824

    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private var server: NanoHTTPD? = null
    private var actualPort: Int = PORT_PRIMARY

    @Synchronized
    fun ensureStarted() {
        if (server != null) return
        for (port in listOf(PORT_PRIMARY, PORT_FALLBACK)) {
            try {
                val s = object : NanoHTTPD("127.0.0.1", port) {
                    override fun serve(session: IHTTPSession): Response {
                        val uri = session.uri
                        if (!uri.startsWith("/r/")) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "")
                        val token = uri.substringAfter("/r/")
                        val files = HashMap<String, String>()
                        try { session.parseBody(files) } catch (_: Throwable) {}
                        val body = files["postData"] ?: session.queryParameterString ?: ""
                        val def = pending.remove(token)
                        def?.complete(body)
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
                    }
                }
                s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                server = s
                actualPort = port
                Log.i(TAG, "Server up on $port")
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Port $port busy: ${t.message}")
            }
        }
    }

    suspend fun runCmd(ctx: Context, cmd: String, timeoutMs: Long = 60_000): String {
        ensureStarted()
        if (server == null) throw RuntimeException("Localhost bridge не поднялся (порты заняты)")

        val token = UUID.randomUUID().toString().take(12)
        val deferred = CompletableDeferred<String>()
        pending[token] = deferred

        val wrapped = "( $cmd ) 2>&1 | curl -s -X POST --data-binary @- 'http://127.0.0.1:$actualPort/r/$token' >/dev/null 2>&1"

        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", wrapped))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        try {
            ctx.startService(intent)
        } catch (t: Throwable) {
            pending.remove(token)
            throw RuntimeException("Termux недоступен. Установи Termux + дай RUN_COMMAND permission.\n${t.message}")
        }

        return try {
            withTimeout(timeoutMs) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            pending.remove(token)
            "[timeout ${timeoutMs}ms] команда не вернула ответ"
        }
    }
}
