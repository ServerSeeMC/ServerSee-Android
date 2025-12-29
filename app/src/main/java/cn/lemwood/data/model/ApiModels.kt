package cn.lemwood.data.model

import com.google.gson.annotations.SerializedName

data class ServerStatus(
    val online: Boolean,
    val motd: String,
    val version: String,
    @SerializedName("bukkit_version") val bukkitVersion: String,
    val players: Int,
    @SerializedName("max_players") val maxPlayers: Int,
    val gamemode: String,
    val plugins: List<String>,
    val icon: String? = null // Base64 encoded icon
)

data class ServerMetrics(
    @SerializedName("tps_5s") val tps5s: Double,
    @SerializedName("tps_1m") val tps1m: Double,
    val mspt: Double,
    @SerializedName("cpu_process") val cpuProcess: Double,
    @SerializedName("cpu_system") val cpuSystem: Double,
    @SerializedName("mem_used") val memUsed: Double,
    @SerializedName("mem_total") val memTotal: Double,
    @SerializedName("mem_max") val memMax: Double,
    @SerializedName("host_mem_used") val hostMemUsed: Double? = null,
    @SerializedName("host_mem_total") val hostMemTotal: Double? = null,
    @SerializedName("disk_used") val diskUsed: Double? = null,
    @SerializedName("disk_total") val diskTotal: Double? = null
)

data class WhitelistResponse(
    val enabled: Boolean,
    val players: List<String>
)

data class LogResponse(
    val lines: List<String>
)
