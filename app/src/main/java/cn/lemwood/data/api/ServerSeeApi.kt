package cn.lemwood.data.api

import cn.lemwood.data.model.ServerMetrics
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.data.model.WhitelistResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerSeeApi {
    @GET("/status")
    suspend fun getStatus(@Header("Authorization") token: String? = null): ServerStatus

    @GET("/status")
    suspend fun getStatusRaw(@Header("Authorization") token: String? = null): Response<ResponseBody>

    @GET("/metrics")
    suspend fun getMetrics(@Header("Authorization") token: String? = null): ServerMetrics

    @GET("/metrics")
    suspend fun getMetricsRaw(@Header("Authorization") token: String? = null): Response<ResponseBody>

    @GET("/history")
    suspend fun getHistory(@Query("limit") limit: Int = 60): List<ServerMetrics>

    @GET("/ping")
    suspend fun ping(): String

    // Admin APIs
    @FormUrlEncoded
    @POST("/admin/command")
    suspend fun executeCommand(
        @Header("Authorization") token: String,
        @Field("command") command: String
    ): Map<String, Any>

    @POST("/admin/restart")
    suspend fun restart(@Header("Authorization") token: String): Map<String, Any>

    @POST("/admin/shutdown")
    suspend fun shutdown(@Header("Authorization") token: String): Map<String, Any>

    @GET("/admin/whitelist")
    suspend fun getWhitelist(@Header("Authorization") token: String): WhitelistResponse

    @FormUrlEncoded
    @POST("/admin/whitelist/toggle")
    suspend fun toggleWhitelist(
        @Header("Authorization") token: String,
        @Field("enabled") enabled: Boolean
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("/admin/whitelist/add")
    suspend fun addWhitelist(
        @Header("Authorization") token: String,
        @Field("name") name: String
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("/admin/whitelist/remove")
    suspend fun removeWhitelist(
        @Header("Authorization") token: String,
        @Field("name") name: String
    ): Map<String, Any>
}

interface McStatApi {
    @GET("https://api.mcsrvstat.us/3/{address}")
    suspend fun getJavaStatus(@retrofit2.http.Path("address") address: String): Response<ResponseBody>

    @GET("https://api.mcsrvstat.us/bedrock/3/{address}")
    suspend fun getBedrockStatus(@retrofit2.http.Path("address") address: String): Response<ResponseBody>
}
