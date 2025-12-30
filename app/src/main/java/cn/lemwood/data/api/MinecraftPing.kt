package cn.lemwood.data.api

import cn.lemwood.data.model.ServerStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID

object MinecraftPing {
    private val gson = Gson()

    suspend fun getJavaStatus(address: String, port: Int = 25565, timeout: Int = 5000): ServerStatus = withContext(Dispatchers.IO) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(address, port), timeout)
            socket.soTimeout = timeout

            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // 1. Handshake Packet
            val handshakeStream = ByteArrayOutputStream()
            val handshakeData = DataOutputStream(handshakeStream)
            handshakeData.writeByte(0x00) // Packet ID
            writeVarInt(handshakeData, 765) // Protocol Version (1.20.4)
            writeString(handshakeData, address)
            handshakeData.writeShort(port)
            writeVarInt(handshakeData, 1) // Next state: Status

            writePacket(output, handshakeStream.toByteArray())

            // 2. Status Request Packet
            val statusRequestStream = ByteArrayOutputStream()
            val statusRequestData = DataOutputStream(statusRequestStream)
            statusRequestData.writeByte(0x00) // Packet ID
            writePacket(output, statusRequestStream.toByteArray())

            // 3. Status Response Packet
            val packetLength = readVarInt(input)
            val packetId = readVarInt(input)
            if (packetId != 0x00) throw Exception("Unexpected Packet ID: $packetId")

            val jsonResponse = readString(input)
            val json = gson.fromJson(jsonResponse, JsonObject::class.java)

            val online = true
            val motd = parseJavaMotd(json.get("description"))

            val playersObj = json.getAsJsonObject("players")
            val onlinePlayers = playersObj?.get("online")?.asInt ?: 0
            val maxPlayers = playersObj?.get("max")?.asInt ?: 0
            val versionObj = json.getAsJsonObject("version")
            val versionName = versionObj?.get("name")?.asString ?: "Unknown"
            val icon = if (json.has("favicon")) json.get("favicon").asString else null

            ServerStatus(
                online = online,
                motd = motd,
                version = versionName,
                bukkitVersion = "",
                players = onlinePlayers,
                maxPlayers = maxPlayers,
                gamemode = "Survival",
                plugins = emptyList(),
                icon = icon
            )
        }
    }

    suspend fun getBedrockStatus(address: String, port: Int = 19132, timeout: Int = 5000): ServerStatus = withContext(Dispatchers.IO) {
        val bedrockStatus = DatagramSocket().use { socket ->
            socket.soTimeout = timeout
            val inetAddress = java.net.InetAddress.getByName(address)

            // Unconnected Ping Packet
            val stream = ByteArrayOutputStream()
            val data = DataOutputStream(stream)
            data.writeByte(0x01) // Packet ID: Unconnected Ping
            data.writeLong(System.currentTimeMillis())
            // Offline Magic
            data.write(byteArrayOf(
                0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(),
                0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(),
                0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(),
                0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()
            ))
            data.writeLong(0) // Client GUID

            val sendData = stream.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, inetAddress, port)
            socket.send(sendPacket)

            // Receive Unconnected Pong
            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)

            val response = String(receiveData, 35, receivePacket.length - 35, StandardCharsets.UTF_8)
            val parts = response.split(";")

            // Part indices: 0: Edition, 1: MOTD1, 2: Protocol, 3: Version, 4: Online, 5: Max, 6: Server ID, 7: MOTD2, 8: Gamemode
            val motd = parts.getOrNull(1) ?: "Minecraft Server"
            val version = parts.getOrNull(3) ?: "Unknown"
            val onlinePlayers = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val maxPlayers = parts.getOrNull(5)?.toIntOrNull() ?: 0
            val gamemode = parts.getOrNull(8) ?: "Survival"

            ServerStatus(
                online = true,
                motd = motd,
                version = version,
                bukkitVersion = "",
                players = onlinePlayers,
                maxPlayers = maxPlayers,
                gamemode = gamemode,
                plugins = emptyList(),
                icon = null
            )
        }

        // 尝试通过 Java 协议获取图标 (如果是基岩服务器，通常也会开启 Java 端口或使用 Geyser)
        val icon = try {
            // 尝试标准 Java 端口 25565，如果基岩端口不是 19132，也尝试基岩端口
            val javaIcon = getJavaIcon(address, 25565, timeout / 2)
            javaIcon ?: if (port != 25565) getJavaIcon(address, port, timeout / 2) else null
        } catch (e: Exception) {
            null
        }

        bedrockStatus.copy(icon = icon)
    }

    private suspend fun getJavaIcon(address: String, port: Int, timeout: Int): String? = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeout)
                socket.soTimeout = timeout

                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                // Handshake
                val handshakeStream = ByteArrayOutputStream()
                val handshakeData = DataOutputStream(handshakeStream)
                handshakeData.writeByte(0x00)
                writeVarInt(handshakeData, 765)
                writeString(handshakeData, address)
                handshakeData.writeShort(port)
                writeVarInt(handshakeData, 1)
                writePacket(output, handshakeStream.toByteArray())

                // Status Request
                val statusRequestStream = ByteArrayOutputStream()
                val statusRequestData = DataOutputStream(statusRequestStream)
                statusRequestData.writeByte(0x00)
                writePacket(output, statusRequestStream.toByteArray())

                // Status Response
                readVarInt(input) // packet length
                val packetId = readVarInt(input)
                if (packetId != 0x00) return@withContext null

                val jsonResponse = readString(input)
                val json = gson.fromJson(jsonResponse, JsonObject::class.java)
                if (json.has("favicon")) json.get("favicon").asString else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJavaMotd(description: com.google.gson.JsonElement?): String {
        if (description == null) return ""
        if (description.isJsonPrimitive) return description.asString
        if (description.isJsonObject) {
            val obj = description.asJsonObject
            val sb = StringBuilder()
            if (obj.has("text")) sb.append(obj.get("text").asString)
            if (obj.has("extra")) {
                val extra = obj.getAsJsonArray("extra")
                for (element in extra) {
                    if (element.isJsonObject) {
                        if (element.asJsonObject.has("text")) {
                            sb.append(element.asJsonObject.get("text").asString)
                        }
                    } else if (element.isJsonPrimitive) {
                        sb.append(element.asString)
                    }
                }
            }
            return sb.toString()
        }
        return ""
    }

    private fun writePacket(out: DataOutputStream, data: ByteArray) {
        writeVarInt(out, data.size)
        out.write(data)
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and -0x80) == 0) {
                out.writeByte(v)
                return
            }
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun readVarInt(input: DataInputStream): Int {
        var value = 0
        var bitOffset = 0
        var b: Byte
        while (true) {
            b = input.readByte()
            value = value or ((b.toInt() and 0x7F) shl bitOffset)
            if ((b.toInt() and 0x80) == 0) break
            bitOffset += 7
            if (bitOffset >= 32) throw Exception("VarInt is too big")
        }
        return value
    }

    private fun writeString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = readVarInt(input)
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
