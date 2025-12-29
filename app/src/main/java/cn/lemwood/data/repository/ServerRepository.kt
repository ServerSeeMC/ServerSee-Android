package cn.lemwood.data.repository

import cn.lemwood.data.api.ServerSeeApi
import cn.lemwood.data.local.ServerDao
import cn.lemwood.data.local.ServerEntity
import cn.lemwood.data.model.ServerMetrics
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.data.model.WhitelistResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServerRepository(private val serverDao: ServerDao) {

    val allServers: Flow<List<ServerEntity>> = serverDao.getAllServers()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            var request = chain.request()
            val requestBuilder = request.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 ServerSee/1.0")
                .header("Accept", "application/json, text/plain, */*")
                .header("Connection", "keep-alive")
            
            request = requestBuilder.build()
            
            var response = chain.proceed(request)
            var tryCount = 0
            val maxLimit = 3
            
            while (!response.isSuccessful && tryCount < maxLimit && (response.code == 503 || response.code == 504 || response.code == 408)) {
                tryCount++
                response.close()
                response = chain.proceed(request)
            }
            response
        }
        .build()

    suspend fun addServer(name: String, endpoint: String, token: String?, serverAddress: String?, useAddressForIcon: Boolean) {
        serverDao.insertServer(ServerEntity(name = name, endpoint = endpoint, token = token, serverAddress = serverAddress, useAddressForIcon = useAddressForIcon))
    }

    suspend fun deleteServer(server: ServerEntity) {
        serverDao.deleteServer(server)
    }

    private fun getApi(endpoint: String): ServerSeeApi {
        val baseUrl = if (endpoint.startsWith("http")) endpoint else "http://$endpoint"
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerSeeApi::class.java)
    }

    suspend fun getServerStatus(endpoint: String, token: String? = null): ServerStatus {
        val authHeader = token?.let { "Bearer $it" }
        return getApi(endpoint).getStatus(authHeader)
    }

    suspend fun getServerStatusRaw(endpoint: String, token: String? = null): String {
        val authHeader = token?.let { "Bearer $it" }
        val response = getApi(endpoint).getStatusRaw(authHeader)
        return if (response.isSuccessful) {
            response.body()?.string() ?: "Empty body"
        } else {
            "Error ${response.code()}: ${response.errorBody()?.string() ?: "Unknown error"}"
        }
    }

    suspend fun getServerMetrics(endpoint: String, token: String? = null): ServerMetrics {
        val authHeader = token?.let { "Bearer $it" }
        return getApi(endpoint).getMetrics(authHeader)
    }

    suspend fun getServerMetricsRaw(endpoint: String, token: String? = null): String {
        val authHeader = token?.let { "Bearer $it" }
        val response = getApi(endpoint).getMetricsRaw(authHeader)
        return if (response.isSuccessful) {
            response.body()?.string() ?: "Empty body"
        } else {
            "Error ${response.code()}: ${response.errorBody()?.string() ?: "Unknown error"}"
        }
    }

    suspend fun executeCommand(endpoint: String, token: String, command: String) {
        getApi(endpoint).executeCommand("Bearer $token", command)
    }
    
    suspend fun getWhitelist(endpoint: String, token: String): WhitelistResponse {
        return getApi(endpoint).getWhitelist("Bearer $token")
    }
    
    suspend fun removeWhitelist(endpoint: String, token: String, name: String) {
        getApi(endpoint).removeWhitelist("Bearer $token", name)
    }

    suspend fun addWhitelist(endpoint: String, token: String, name: String) {
        getApi(endpoint).addWhitelist("Bearer $token", name)
    }
}
