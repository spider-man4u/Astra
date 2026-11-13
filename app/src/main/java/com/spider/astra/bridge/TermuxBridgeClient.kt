package com.spider.astra.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID

data class BridgeHealthResult(
    val connected: Boolean,
    val message: String
)

data class BridgeCommandResult(
    val configured: Boolean,
    val success: Boolean,
    val reply: String,
    val error: String? = null
)

/**
 * Fixed loopback-only Termux transport.
 *
 * The host is intentionally not user-editable: it can only contact the local
 * Termux process on the same Android device.
 */
class TermuxBridgeClient(
    private val tokenStore: BridgeTokenStore
) {
    suspend fun isConfigured(): Boolean = withContext(Dispatchers.IO) {
        tokenStore.hasToken()
    }

    suspend fun checkHealth(): BridgeHealthResult = withContext(Dispatchers.IO) {
        val token = tokenStore.readToken()
            ?: return@withContext BridgeHealthResult(false, "No access token saved.")
        val result = request(
            path = HEALTH_PATH,
            method = "GET",
            token = token,
            body = null
        )
        if (result.code == 200 && result.json?.optBoolean("ok", false) == true) {
            BridgeHealthResult(true, "Connected to ASTRA Termux Core.")
        } else {
            BridgeHealthResult(false, result.message)
        }
    }

    suspend fun sendCommand(command: String): BridgeCommandResult = withContext(Dispatchers.IO) {
        val token = tokenStore.readToken()
            ?: return@withContext BridgeCommandResult(
                configured = false,
                success = false,
                reply = "Termux Core is not configured. Set the access token in ASTRA Settings.",
                error = "not_configured"
            )

        val clean = command.trim()
        if (clean.isEmpty() || clean.length > MAX_COMMAND_CHARS) {
            return@withContext BridgeCommandResult(
                configured = true,
                success = false,
                reply = "The command is empty or too long.",
                error = "invalid_command"
            )
        }

        val body = JSONObject()
            .put("request_id", UUID.randomUUID().toString())
            .put("text", clean)

        val result = request(
            path = COMMAND_PATH,
            method = "POST",
            token = token,
            body = body.toString()
        )

        if (result.code == 200 && result.json?.optBoolean("ok", false) == true) {
            val reply = result.json.optString("reply").take(MAX_REPLY_CHARS)
            BridgeCommandResult(
                configured = true,
                success = true,
                reply = reply.ifBlank { "ASTRA Termux returned an empty reply." }
            )
        } else {
            BridgeCommandResult(
                configured = true,
                success = false,
                reply = result.message,
                error = "bridge_request_failed"
            )
        }
    }

    private fun request(
        path: String,
        method: String,
        token: String,
        body: String?
    ): HttpResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer " + token)
                setRequestProperty("Connection", "close")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }

            if (body != null) {
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()

            val message = when (code) {
                200 -> "Bridge responded."
                401 -> "Access token rejected. Copy the current token from Termux and save it again."
                403 -> "The bridge refused this connection. It accepts loopback requests only."
                404 -> "Termux bridge endpoint not found. Update bridge_server.py in ~/astra."
                413 -> "Command payload is too large."
                500 -> json?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: "The Termux backend returned an internal error. Check its Termux window."
                else -> "Termux bridge returned HTTP " + code + "."
            }
            HttpResult(code, json, message)
        } catch (_: SocketTimeoutException) {
            HttpResult(-1, null, "Termux Core timed out. Check that the bridge is running in Termux.")
        } catch (_: IOException) {
            HttpResult(-1, null, "Cannot reach Termux on 127.0.0.1:8765. Start bridge_server.py in Termux.")
        } catch (_: SecurityException) {
            HttpResult(-1, null, "Android blocked the local connection. Check ASTRA's network permission.")
        } catch (_: Exception) {
            HttpResult(-1, null, "Could not communicate with the Termux bridge.")
        } finally {
            connection?.disconnect()
        }
    }

    private data class HttpResult(
        val code: Int,
        val json: JSONObject?,
        val message: String
    )

    companion object {
        private const val BASE_URL = "http://127.0.0.1:8765"
        private const val HEALTH_PATH = "/v1/health"
        private const val COMMAND_PATH = "/v1/command"
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val MAX_COMMAND_CHARS = 4_000
        private const val MAX_REPLY_CHARS = 10_000
    }
}
