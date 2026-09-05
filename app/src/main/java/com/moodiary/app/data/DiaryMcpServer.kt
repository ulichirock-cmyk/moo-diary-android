package com.moodiary.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The diary as an MCP server, so Claude Code on a laptop on the same Wi-Fi can call
 * [DiaryTools] directly:
 *
 *     claude mcp add --transport http moodiary http://<phone-ip>:8765/mcp \
 *         --header "Authorization: Bearer <token>"
 *
 * It speaks the Streamable HTTP transport in its simplest legal form: every request
 * is one POST to `/mcp` answered with one JSON body; there is no push stream (GET is
 * 405, which the client accepts) and no session id. The HTTP itself is hand-rolled on
 * a [ServerSocket] — a dependency for one endpoint would be out of character for a
 * project that draws its own map tiles.
 *
 * Security is the bearer token and nothing else: the link is plain HTTP on the local
 * network, so the switch on 我的 is meant to be on while the laptop is in use and off
 * afterwards. A missing or wrong token is 403, not 401 — 401 tells the MCP client to
 * go looking for an OAuth server that does not exist.
 */
class DiaryMcpServer(
    private val port: Int,
    private val token: () -> String,
    private val entries: () -> List<DiaryEntry>,
) {
    private var socket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()

    @Throws(IOException::class)
    fun start() {
        if (!running.compareAndSet(false, true)) return
        val server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(port))
        }
        socket = server
        Thread({
            while (running.get()) {
                val client = try { server.accept() } catch (e: IOException) { break }
                // A client that connects and goes quiet times out in handle(); an
                // exception on a pool thread would take the whole process down.
                pool.execute { runCatching { client.use { handle(it) } } }
            }
        }, "moodiary-mcp").start()
    }

    fun stop() {
        running.set(false)
        runCatching { socket?.close() }
        socket = null
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private fun handle(client: Socket) {
        client.soTimeout = 10_000
        val input = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.ISO_8859_1))
        val requestLine = input.readLine() ?: return
        val parts = requestLine.split(' ')
        if (parts.size < 2) return respond(client, 400, text = "bad request")
        val method = parts[0]
        val path = parts[1].substringBefore('?')

        val headers = HashMap<String, String>()
        while (true) {
            val line = input.readLine() ?: break
            if (line.isEmpty()) break
            val i = line.indexOf(':')
            if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length > MAX_BODY) return respond(client, 413, text = "too large")
        val buffer = CharArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buffer, read, length - read)
            if (n < 0) break
            read += n
        }
        // Read as Latin-1 so byte count == char count; re-decode the body as UTF-8.
        val body = String(String(buffer, 0, read).toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)

        if (path != "/mcp") return respond(client, 404, text = "not found")
        val auth = headers["authorization"].orEmpty()
        if (auth != "Bearer ${token()}") return respond(client, 403, text = "forbidden")
        if (method != "POST") return respond(client, 405, text = "method not allowed")

        val message = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return respond(client, 200, json = error(JSONObject.NULL, -32700, "parse error"))
        }
        // Notifications carry no id and get no body — the client expects 202.
        if (!message.has("id")) return respond(client, 202)
        respond(client, 200, json = dispatch(message))
    }

    private fun respond(client: Socket, status: Int, json: JSONObject? = null, text: String? = null) {
        val payload = (json?.toString() ?: text.orEmpty()).toByteArray(Charsets.UTF_8)
        val type = if (json != null) "application/json" else "text/plain; charset=utf-8"
        val head = buildString {
            append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n")
            append("Content-Type: ").append(type).append("\r\n")
            append("Content-Length: ").append(payload.size).append("\r\n")
            append("Connection: close\r\n\r\n")
        }
        runCatching {
            client.getOutputStream().apply {
                write(head.toByteArray(Charsets.ISO_8859_1))
                write(payload)
                flush()
            }
        }
    }

    private fun reason(status: Int) = when (status) {
        200 -> "OK"
        202 -> "Accepted"
        400 -> "Bad Request"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        413 -> "Payload Too Large"
        else -> "Error"
    }

    // ── JSON-RPC ─────────────────────────────────────────────────────────────

    private fun dispatch(message: JSONObject): JSONObject {
        val id = message.opt("id") ?: JSONObject.NULL
        val params = message.optJSONObject("params") ?: JSONObject()
        return when (message.optString("method")) {
            "initialize" -> result(
                id,
                JSONObject()
                    .put("protocolVersion", params.optString("protocolVersion").ifEmpty { PROTOCOL })
                    .put("capabilities", JSONObject().put("tools", JSONObject()))
                    .put("serverInfo", JSONObject().put("name", "moodiary").put("version", "1"))
                    .put("instructions", INSTRUCTIONS),
            )
            "ping" -> result(id, JSONObject())
            "tools/list" -> result(
                id,
                JSONObject().put(
                    "tools",
                    JSONArray().also { array ->
                        DiaryTools.SPECS.forEach { spec ->
                            array.put(
                                JSONObject()
                                    .put("name", spec.name)
                                    .put("description", spec.description)
                                    .put("inputSchema", spec.schema),
                            )
                        }
                    },
                ),
            )
            "tools/call" -> {
                val name = params.optString("name")
                val args = params.optJSONObject("arguments") ?: JSONObject()
                val known = DiaryTools.SPECS.any { it.name == name }
                val output = DiaryTools.call(name, args, entries())
                result(
                    id,
                    JSONObject()
                        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", output.toString())))
                        .put("isError", !known),
                )
            }
            else -> error(id, -32601, "method not found")
        }
    }

    private fun result(id: Any, result: JSONObject) =
        JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)

    private fun error(id: Any, code: Int, message: String) =
        JSONObject().put("jsonrpc", "2.0").put("id", id)
            .put("error", JSONObject().put("code", code).put("message", message))

    companion object {
        const val DEFAULT_PORT = 8765
        private const val MAX_BODY = 1 shl 20
        private const val PROTOCOL = "2025-03-26"
        private val INSTRUCTIONS = """
            这是一本私人日记(Moodiary)。先用 diary_overview 看概况,再用 search_entries 找,需要全文用 get_entry。
            日记只读;回答时引用具体日期,不要编造日记里没有的事。
        """.trimIndent()

        /** The phone's address on the local network, or null when it is not on one. */
        fun localAddress(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it.isSiteLocalAddress && it.hostAddress?.contains(':') == false }
                ?.hostAddress
        }.getOrNull()
    }
}
