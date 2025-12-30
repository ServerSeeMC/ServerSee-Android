package cn.lemwood.data.repository

import cn.lemwood.data.api.McStatApi
import cn.lemwood.data.api.WebSocketClient
import cn.lemwood.data.local.ServerDao
import cn.lemwood.data.local.ServerEntity
import cn.lemwood.data.model.ServerMetrics
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.data.model.WhitelistResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ServerRepository(private val serverDao: ServerDao) {

    val allServers: Flow<List<ServerEntity>> = serverDao.getAllServers()
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // WebSocket 客户端缓存 (endpoint + token -> WebSocketClient)
    private val wsClients = ConcurrentHashMap<String, WebSocketClient>()

    private fun getWsClient(endpoint: String, token: String? = null): WebSocketClient {
        val key = "$endpoint|$token"
        return wsClients.getOrPut(key) {
            WebSocketClient(endpoint, token)
        }
    }

    suspend fun addServer(server: ServerEntity) {
        serverDao.insertServer(server)
    }

    suspend fun deleteServer(server: ServerEntity) {
        val key = "${server.endpoint}|${server.token}"
        wsClients.remove(key)?.close()
        serverDao.deleteServer(server)
    }

    private fun getMcStatApi(): McStatApi {
        return Retrofit.Builder()
            .baseUrl("https://api.mcsrvstat.us/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(McStatApi::class.java)
    }

    suspend fun getLogStream(endpoint: String, token: String): Flow<String> {
        val wsClient = getWsClient(endpoint, token)
        // 发送订阅日志请求
        wsClient.sendRequest("admin/logs/subscribe")
        
        return wsClient.pushFlow
            .filter { it.get("action")?.asString == "log" }
            .map { it.get("data")?.asString ?: "" }
    }

    suspend fun getServerStatus(endpoint: String, token: String? = null, mode: String = "API"): ServerStatus {
        if (mode == "API") {
            val response = getWsClient(endpoint, token).sendRequest("status")
            return gson.fromJson(response.get("data"), ServerStatus::class.java)
        } else {
            val response = if (mode == "JAVA_ADDRESS") {
                getMcStatApi().getJavaStatus(endpoint)
            } else {
                getMcStatApi().getBedrockStatus(endpoint)
            }
            
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: throw Exception("Empty body")
                val json = JSONObject(body)
                val online = json.optBoolean("online", false)
                if (!online) throw Exception("服务器离线")
                
                val motdObj = json.optJSONObject("motd")
                val motdClean = motdObj?.optJSONArray("clean")?.optString(0) ?: "Minecraft Server"
                val playersObj = json.optJSONObject("players")
                val onlinePlayers = playersObj?.optInt("online", 0) ?: 0
                val maxPlayers = playersObj?.optInt("max", 0) ?: 0
                val version = json.optString("version", "Unknown")
                val icon = if (json.has("icon")) json.getString("icon") else null
                
                return ServerStatus(
                    online = true,
                    motd = motdClean,
                    players = onlinePlayers,
                    maxPlayers = maxPlayers,
                    version = version,
                    icon = icon,
                    bukkitVersion = "Unknown",
                    gamemode = "Unknown",
                    plugins = emptyList()
                )
            } else {
                throw Exception("API Error: ${response.code()}")
            }
        }
    }

    suspend fun getServerStatusRaw(endpoint: String, token: String? = null, mode: String = "API"): String {
        if (mode == "API") {
            val response = getWsClient(endpoint, token).sendRequest("status")
            return gson.toJson(response.get("data"))
        } else {
            val response = if (mode == "JAVA_ADDRESS") {
                getMcStatApi().getJavaStatus(endpoint)
            } else {
                getMcStatApi().getBedrockStatus(endpoint)
            }
            return if (response.isSuccessful) {
                response.body()?.string() ?: "Empty body"
            } else {
                "Error ${response.code()}: ${response.errorBody()?.string() ?: "Unknown error"}"
            }
        }
    }

    suspend fun getServerMetrics(endpoint: String, token: String? = null): ServerMetrics {
        val response = getWsClient(endpoint, token).sendRequest("metrics")
        return gson.fromJson(response.get("data"), ServerMetrics::class.java)
    }

    suspend fun getServerMetricsRaw(endpoint: String, token: String? = null): String {
        val response = getWsClient(endpoint, token).sendRequest("metrics")
        return gson.toJson(response.get("data"))
    }

    suspend fun getHistory(endpoint: String, token: String? = null, limit: Int = 60): List<ServerMetrics> {
        val response = getWsClient(endpoint, token).sendRequest("history", mapOf("limit" to limit))
        val type = object : TypeToken<List<ServerMetrics>>() {}.type
        return gson.fromJson(response.get("data"), type)
    }

    suspend fun executeCommand(endpoint: String, token: String, command: String) {
        getWsClient(endpoint, token).sendRequest("admin/command", mapOf("command" to command))
    }
    
    suspend fun getWhitelist(endpoint: String, token: String): WhitelistResponse {
        val response = getWsClient(endpoint, token).sendRequest("admin/whitelist")
        return gson.fromJson(response.get("data"), WhitelistResponse::class.java)
    }
    
    suspend fun removeWhitelist(endpoint: String, token: String, name: String) {
        getWsClient(endpoint, token).sendRequest("admin/whitelist/remove", mapOf("name" to name))
    }

    suspend fun addWhitelist(endpoint: String, token: String, name: String) {
        getWsClient(endpoint, token).sendRequest("admin/whitelist/add", mapOf("name" to name))
    }

    suspend fun toggleWhitelist(endpoint: String, token: String, enabled: Boolean) {
        getWsClient(endpoint, token).sendRequest("admin/whitelist/toggle", mapOf("enabled" to enabled))
    }
}
