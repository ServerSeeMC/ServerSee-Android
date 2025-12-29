package cn.lemwood.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.ui.theme.*
import cn.lemwood.utils.MinecraftTextParser
import coil.compose.AsyncImage

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, endpoint: String, token: String?, serverAddress: String?, useAddressForIcon: Boolean) -> Unit,
    onTest: (endpoint: String, token: String?) -> Unit,
    testResult: String?,
    isTesting: Boolean,
    testedStatus: ServerStatus? = null
) {
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var serverAddress by remember { mutableStateOf("") }
    var autoFillName by remember { mutableStateOf(true) }
    var useAddressForIcon by remember { mutableStateOf(false) }

    // When testedStatus changes and autoFillName is true, update name if empty
    LaunchedEffect(testedStatus) {
        if (testedStatus != null && autoFillName && name.isBlank()) {
            // Strip Minecraft color codes for the name field
            val cleanName = testedStatus.motd.replace(Regex("§[0-9a-fk-or]"), "").trim()
            if (cleanName.isNotBlank()) {
                name = cleanName
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加服务器") },
        text = {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("服务器名称") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如: 我的生存服务器") }
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("API 端点 (必填)") },
                    placeholder = { Text("例如: 192.168.1.100:8080") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = { Text("服务器连接地址 (可选)") },
                    placeholder = { Text("例如: play.example.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("管理功能需要 Token") }
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = autoFillName,
                            onCheckedChange = { autoFillName = it }
                        )
                        Text("自动使用 MOTD 作为名称", fontSize = 14.sp)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = useAddressForIcon,
                            onCheckedChange = { useAddressForIcon = it },
                            enabled = serverAddress.isNotBlank()
                        )
                        Text(
                            "根据服务器地址获取头像", 
                            fontSize = 14.sp,
                            color = if (serverAddress.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                        )
                    }
                }
                
                // Server Preview Section
                if (testedStatus != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (testedStatus.icon != null) {
                                AsyncImage(
                                    model = testedStatus.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = MinecraftTextParser.parse(testedStatus.motd),
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "版本: ${testedStatus.version}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onTest(endpoint.trim(), token.trim().ifBlank { null }) },
                    enabled = endpoint.isNotBlank() && !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("获取服务器信息并测试")
                }
                
                if (testResult != null) {
                    Text(
                        text = testResult,
                        color = if (testResult.contains("成功")) McGreen else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (endpoint.isNotBlank()) {
                        onConfirm(
                            name.trim().ifBlank { "Minecraft Server" }, 
                            endpoint.trim(), 
                            token.trim().ifBlank { null },
                            serverAddress.trim().ifBlank { null },
                            useAddressForIcon
                        )
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
