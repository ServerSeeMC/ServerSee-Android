package cn.lemwood.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val endpoint: String,
    val token: String? = null,
    val serverAddress: String? = null,
    val useAddressForIcon: Boolean = false,
    val mode: String = "API" // API, JAVA_ADDRESS, BEDROCK_ADDRESS
)
