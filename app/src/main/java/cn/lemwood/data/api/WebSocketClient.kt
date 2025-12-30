package cn.lemwood.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebSocketClient(
    private val endpoint: String,
    private val token: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WS no timeout
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = com.google.gson.GsonBuilder()
        .disableHtmlEscaping()
        .create()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()
    private val mutex = Mutex()

    private val _pushFlow = MutableSharedFlow<JsonObject>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pushFlow: SharedFlow<JsonObject> = _pushFlow

    private var isConnected = false
    private val connectionMutex = Mutex()

    private suspend fun ensureConnected() {
        connectionMutex.withLock {
            if (isConnected && webSocket != null) return

            val baseUrl = if (endpoint.startsWith("http")) endpoint else "http://$endpoint"
            val wsUrl = baseUrl.replace("http://", "ws://")
                .replace("https://", "wss://")
                .let { if (it.endsWith("/")) it else "$it/" }

            val request = Request.Builder()
                .url(wsUrl)
                .header("User-Agent", "ServerSee-APK")
                .build()

            val deferred = CompletableDeferred<Unit>()
            
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    deferred.complete(Unit)
                    Log.d("WebSocketClient", "Connected to $wsUrl")
                    startHeartbeat()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch {
                        handleMessage(text)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    deferred.completeExceptionally(t)
                    Log.e("WebSocketClient", "Connection failed to $wsUrl", t)
                    cleanup()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    Log.d("WebSocketClient", "Closing connection: $reason")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                    cleanup()
                }
            })

            try {
                deferred.await()
            } catch (e: Exception) {
                webSocket?.cancel()
                webSocket = null
                throw e
            }
        }
    }

    private suspend fun handleMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString ?: "response"
            
            if (type == "push") {
                _pushFlow.emit(json)
            } else {
                val id = json.get("id")?.asString
                if (id != null) {
                    mutex.withLock {
                        pendingRequests.remove(id)?.complete(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Error handling message: $text", e)
        }
    }

    suspend fun sendRequest(action: String, data: Any? = null): JsonObject {
        ensureConnected()
        
        val requestId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis() / 1000
        val nonce = UUID.randomUUID().toString().substring(0, 8)
        val dataJson = if (data != null) gson.toJson(data) else ""

        val request = JsonObject().apply {
            addProperty("id", requestId)
            addProperty("type", "request")
            addProperty("action", action)
            
            if (token != null) {
                // 不再直接发送 token，而是发送签名
                val dataToSign = action + timestamp + nonce + dataJson
                val signature = calculateHMAC(dataToSign, token)
                
                addProperty("timestamp", timestamp)
                addProperty("nonce", nonce)
                addProperty("signature", signature)

                Log.d("WebSocketClient", "Signing: action=$action, ts=$timestamp, nonce=$nonce, data=$dataJson")
                Log.d("WebSocketClient", "Signature: $signature")
            }

            if (data != null) {
                add("data", gson.toJsonTree(data))
            }
        }

        val deferred = CompletableDeferred<JsonObject>()
        mutex.withLock {
            pendingRequests[requestId] = deferred
        }

        val success = webSocket?.send(gson.toJson(request)) ?: false
        if (!success) {
            mutex.withLock { pendingRequests.remove(requestId) }
            throw Exception("Failed to send request via WebSocket")
        }

        return deferred.await().also {
            if (it.get("success")?.asBoolean == false) {
                throw Exception(it.get("message")?.asString ?: "Unknown error")
            }
        }
    }

    private fun calculateHMAC(data: String, key: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(rawHmac)
        } else {
            android.util.Base64.encodeToString(rawHmac, android.util.Base64.NO_WRAP)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isConnected) {
                delay(30000) // 30 seconds
                try {
                    val pingRequest = JsonObject().apply {
                        addProperty("id", UUID.randomUUID().toString())
                        addProperty("type", "request")
                        addProperty("action", "ping")
                    }
                    webSocket?.send(gson.toJson(pingRequest))
                } catch (e: Exception) {
                    Log.e("WebSocketClient", "Heartbeat failed", e)
                }
            }
        }
    }

    private fun cleanup() {
        heartbeatJob?.cancel()
        scope.launch {
            mutex.withLock {
                pendingRequests.forEach { (_, deferred) ->
                    deferred.completeExceptionally(Exception("Connection closed"))
                }
                pendingRequests.clear()
            }
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "App closing")
        webSocket = null
        isConnected = false
    }
}
