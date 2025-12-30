package cn.lemwood.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
    onServerClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val servers by viewModel.servers.collectAsState(initial = emptyList())
    val serverStates by viewModel.serverStates.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Statistics calculations
    val totalServers = servers.size
    val onlineServers = servers.count { server ->
        val state = serverStates[server.id]
        state?.status != null && state.lastError == null
    }
    val offlineServers = (totalServers - onlineServers).coerceAtLeast(0)
    val totalPlayers = servers.sumOf { server -> 
        serverStates[server.id]?.status?.players ?: 0 
    }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()

    Scaffold(
        containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ServerSee 客户端", 
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "监控面板 • ${if (offlineServers == 0) "全部在线" else "${offlineServers}台离线"}", 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (offlineServers == 0) McGreen else MaterialTheme.colorScheme.error
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = "手动刷新"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
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
                StatItem(label = "总数", value = "$totalServers", color = MaterialTheme.colorScheme.primary, bgType = bgType, modifier = Modifier.weight(1f))
                StatItem(label = "离线", value = "$offlineServers", color = MaterialTheme.colorScheme.error, bgType = bgType, modifier = Modifier.weight(1f))
                StatItem(label = "玩家", value = "$totalPlayers", color = McGold, bgType = bgType, modifier = Modifier.weight(1f))
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
                            onDelete = { viewModel.deleteServer(server) },
                            bgType = bgType
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
fun StatItem(label: String, value: String, color: Color, bgType: String = "none", modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (bgType != "none") 0.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (bgType != "none") 0.2f else 0.3f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            Text(
                text = label, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value, 
                fontSize = 22.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = color
            )
        }
    }
}
