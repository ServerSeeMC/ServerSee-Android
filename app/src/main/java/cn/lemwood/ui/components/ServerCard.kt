package cn.lemwood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lemwood.data.model.ServerMetrics
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.ui.theme.*
import cn.lemwood.utils.MinecraftTextParser
import coil.compose.AsyncImage

@Composable
fun ServerCard(
    name: String,
    status: ServerStatus?,
    metrics: ServerMetrics?,
    lastError: String?,
    serverAddress: String? = null,
    useAddressForIcon: Boolean = false,
    mode: String = "API",
    onClick: () -> Unit,
    onDelete: () -> Unit,
    bgType: String = "none"
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val iconUrl = remember(status?.icon, serverAddress, useAddressForIcon) {
        if (!status?.icon.isNullOrBlank()) {
            status?.icon
        } else if (useAddressForIcon && !serverAddress.isNullOrBlank()) {
            "https://api.mcsrvstat.us/icon/$serverAddress"
        } else {
            null
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除服务器") },
            text = { Text("确定要删除服务器 $name 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showDeleteConfirm = true }
                )
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bgType != "none") 0.dp else 2.dp),
        border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon, Name, Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (iconUrl != null) {
                    AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (mode != "API") {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (mode == "JAVA_ADDRESS") "JAVA" else "BE",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = status?.version ?: "未知版本",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isOnline = status != null && lastError == null
                val statusText = if (isOnline) "在线" else if (lastError != null) "错误" else "离线"
                val statusColor = if (isOnline) McGreen else if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MOTD
            if (status != null) {
                Text(
                    text = MinecraftTextParser.parse(status.motd),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (lastError != null) {
                Text(
                    text = lastError,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    icon = Icons.Default.People,
                    label = "玩家",
                    value = status?.players?.toString() ?: "0",
                    total = "/${status?.maxPlayers ?: 0}",
                    tint = McBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    icon = if (mode == "BEDROCK_ADDRESS") Icons.Outlined.Games else Icons.Default.Speed,
                    label = if (mode == "API") "TPS" else "类型",
                    value = if (mode == "API") {
                        String.format("%.1f", metrics?.tps5s ?: 0.0)
                    } else if (mode == "BEDROCK_ADDRESS") {
                        status?.gamemode ?: "未知"
                    } else {
                        // 提取服务端类型
                        val version = status?.version ?: ""
                        if (version.contains(" ")) {
                            version.split(" ").first()
                        } else {
                            "JAVA"
                        }
                    },
                    tint = McGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (mode == "API") {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        icon = Icons.Default.Memory,
                        label = "内存",
                        value = String.format("%.0f", metrics?.memUsed ?: 0.0),
                        total = "MB",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                    MetricItem(
                        icon = Icons.Default.Computer,
                        label = "CPU",
                        value = String.format("%.0f", metrics?.cpuProcess ?: 0.0),
                        total = "%",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    total: String = "",
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (total.isNotEmpty()) {
                    Text(
                        text = total, 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
