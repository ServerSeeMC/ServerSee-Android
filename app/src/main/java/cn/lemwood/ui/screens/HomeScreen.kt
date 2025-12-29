package cn.lemwood.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import cn.lemwood.ui.theme.*
import cn.lemwood.ui.components.AddServerDialog
import cn.lemwood.ui.components.ServerCard
import cn.lemwood.ui.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ServerViewModel,
    onServerClick: (Int) -> Unit
) {
    val servers by viewModel.servers.collectAsState(initial = emptyList())
    val serverStates by viewModel.serverStates.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Statistics calculations
    val totalServers = servers.size
    val offlineServers = servers.size - serverStates.values.count { it.status != null && it.lastError == null }
    val totalPlayers = serverStates.values.sumOf { it.status?.players ?: 0 }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ServerSee 客户端", 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "监控面板", 
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "手动刷新"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加服务器")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Statistics Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItem(label = "总数", value = "$totalServers", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                StatItem(label = "离线", value = "$offlineServers", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                StatItem(label = "玩家", value = "$totalPlayers", color = McGold, modifier = Modifier.weight(1f))
            }

            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "暂无服务器",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "点击右下角按钮添加第一个服务器",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(servers) { server ->
                        val state = serverStates[server.id]
                        ServerCard(
                            name = server.name,
                            status = state?.status,
                            metrics = state?.metrics,
                            lastError = state?.lastError,
                            serverAddress = server.serverAddress,
                            useAddressForIcon = server.useAddressForIcon,
                            mode = server.mode,
                            onClick = { onServerClick(server.id) },
                            onDelete = { viewModel.deleteServer(server) }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            val dialogState by viewModel.addServerDialogState.collectAsState()
            AddServerDialog(
                onDismiss = { 
                    showDialog = false
                    viewModel.resetAddDialogState()
                },
                onConfirm = { name, endpoint, token, serverAddress, useAddressForIcon, mode ->
                    viewModel.addServer(name, endpoint, token, serverAddress, useAddressForIcon, mode)
                    showDialog = false
                    viewModel.resetAddDialogState()
                },
                onTest = { endpoint, token, mode ->
                    viewModel.testConnection(endpoint, token, mode)
                },
                testResult = dialogState.testResult,
                isTesting = dialogState.isTesting,
                testedStatus = dialogState.testedStatus
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold, 
            color = color
        )
    }
}
