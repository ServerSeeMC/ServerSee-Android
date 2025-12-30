package cn.lemwood.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lemwood.data.model.*
import cn.lemwood.data.local.ServerEntity
import cn.lemwood.ui.theme.*
import cn.lemwood.ui.viewmodel.ServerViewModel
import cn.lemwood.utils.MinecraftTextParser
import coil.compose.AsyncImage
import android.widget.Toast
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    serverId: Int,
    viewModel: ServerViewModel,
    onBack: () -> Unit
) {
    var server by remember { mutableStateOf<ServerEntity?>(null) }
    val servers by viewModel.servers.collectAsState(initial = emptyList())
    
    LaunchedEffect(serverId, servers) {
        server = servers.find { it.id == serverId }
        viewModel.setActiveServer(serverId)
    }

    DisposableEffect(serverId) {
        onDispose {
            viewModel.setActiveServer(null)
        }
    }

    val hasToken = !server?.token.isNullOrBlank() && server?.mode == "API"
    var selectedTab by remember { mutableIntStateOf(0) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()

    Scaffold(
        containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(server?.name ?: "服务器详情", fontWeight = FontWeight.ExtraBold)
                        if (server != null) {
                            Text(
                                server!!.endpoint.replace("http://", "").replace("https://", ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        server?.let { viewModel.refreshAll() } 
                    }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            if (hasToken) {
                NavigationBar(
                    containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (bgType != "none") 0.dp else 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                        label = { Text("监控") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("白名单") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                        label = { Text("终端") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(
                if (bgType == "none") {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                }
            )
        ) {
            when (selectedTab) {
                0 -> InfoContent(server, viewModel)
                1 -> if (hasToken) WhitelistContent(server!!, viewModel)
                2 -> if (hasToken) CommandContent(server!!, viewModel)
            }
        }
    }
}

@Composable
fun InfoContent(server: ServerEntity?, viewModel: ServerViewModel) {
    val states by viewModel.serverStates.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()
    val state = server?.let { states[it.id] }
    val metrics = state?.metrics
    val isApiMode = server?.mode == "API"
    var showRaw by remember { mutableStateOf(false) }

    val iconUrl = remember(state?.status?.icon, server?.serverAddress, server?.useAddressForIcon) {
        if (!state?.status?.icon.isNullOrBlank()) {
            state?.status?.icon
        } else if (server?.useAddressForIcon == true && !server.serverAddress.isNullOrBlank()) {
            "https://api.mcsrvstat.us/icon/${server.serverAddress}"
        } else {
            null
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar/Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iconUrl != null) {
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer)
                            )
                        } else {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = server?.name ?: "Unknown",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state?.status != null) McGreen else Color.Red)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (state?.status != null) "在线" else "离线",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state?.status != null) McGreen else Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (state?.status != null) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = state.status.version,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                if (state?.lastError != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.lastError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                state?.status?.let { status ->
                    Spacer(Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "MOTD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = MinecraftTextParser.parse(status.motd),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        
        state?.status?.let { status ->
            // Players Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = McGold
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "在线玩家",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "${status.players} / ${status.maxPlayers}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Text(
            "性能监控",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        
        // TPS & MSPT Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernMetricCard(
            label = "TPS (5s)",
            value = if (isApiMode && metrics != null) "%.2f".format(metrics.tps5s) else "N/A",
            icon = Icons.Default.Speed,
            color = if (isApiMode && metrics != null) {
                when {
                    metrics.tps5s > 18 -> McGreen
                    metrics.tps5s > 15 -> McGold
                    else -> Color.Red
                }
            } else Color.Gray,
            bgType = bgType,
            modifier = Modifier.weight(1f)
        )
        ModernMetricCard(
            label = "MSPT",
            value = if (isApiMode && metrics != null) "%.1f ms".format(metrics.mspt) else "N/A",
            icon = Icons.Default.Timer,
            color = if (isApiMode && metrics != null) {
                when {
                    metrics.mspt < 40 -> McGreen
                    metrics.mspt < 50 -> McGold
                    else -> Color.Red
                }
            } else Color.Gray,
            bgType = bgType,
            modifier = Modifier.weight(1f)
        )
    }
    
    // Usage Progress Cards
    if (isApiMode && metrics != null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageCard(
                label = "CPU 使用率 (进程/系统)",
                value = "${ "%.0f".format(metrics.cpuProcess) }% / ${ "%.0f".format(metrics.cpuSystem) }%",
                progress = (metrics.cpuProcess / 100).toFloat().coerceIn(0f, 1f),
                icon = Icons.Default.Memory,
                color = MaterialTheme.colorScheme.primary,
                bgType = bgType
            )
            
            UsageCard(
                label = "JVM 内存 (已用/最大)",
                value = "${ "%.0f".format(metrics.memUsed) }MB / ${ "%.0f".format(metrics.memMax) }MB",
                progress = (metrics.memUsed / metrics.memMax).toFloat().coerceIn(0f, 1f),
                icon = Icons.Default.DataUsage,
                color = Color(0xFF9C27B0),
                bgType = bgType
            )

            if (metrics.hostMemUsed != null && metrics.hostMemTotal != null) {
                UsageCard(
                    label = "主机内存",
                    value = "${ "%.1f".format(metrics.hostMemUsed / 1024) }GB / ${ "%.1f".format(metrics.hostMemTotal / 1024) }GB",
                    progress = (metrics.hostMemUsed / metrics.hostMemTotal).toFloat().coerceIn(0f, 1f),
                    icon = Icons.Default.Storage,
                    color = Color(0xFF2196F3),
                    bgType = bgType
                )
            }

            if (metrics.diskUsed != null && metrics.diskTotal != null) {
                UsageCard(
                    label = "磁盘空间",
                    value = "${ "%.1f".format(metrics.diskUsed) }GB / ${ "%.1f".format(metrics.diskTotal) }GB",
                    progress = (metrics.diskUsed / metrics.diskTotal).toFloat().coerceIn(0f, 1f),
                    icon = Icons.Default.Save,
                    color = Color(0xFF607D8B),
                    bgType = bgType
                )
            }
        }
        } else if (!isApiMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前模式仅支持基础状态监控",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "如需查看 CPU、内存、TPS 趋势等详细指标，请在服务器安装 ServerSee 插件并使用 API 端点模式添加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // History Chart
        if (isApiMode && state != null && state.history.isNotEmpty()) {
            Text(
                "TPS 历史趋势 (近1小时)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
            MetricLineChart(
                history = state.history,
                bgType = bgType,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        if (isApiMode) {
            OutlinedButton(
                onClick = { showRaw = !showRaw },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (showRaw) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showRaw) "隐藏原始 JSON" else "查看原始数据")
            }
        }
        
        if (showRaw) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    RawDataSection("Status", state?.rawStatus)
                    Spacer(Modifier.height(12.dp))
                    RawDataSection("Metrics", state?.rawMetrics)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MetricLineChart(
    history: List<HistoricalMetrics>,
    bgType: String = "none",
    modifier: Modifier = Modifier
) {
    val sortedHistory = history.sortedBy { it.timestamp }
    val tpsList = sortedHistory.map { it.tps.toFloat() }
    if (tpsList.isEmpty()) return

    val maxTps = 20f
    val minTps = 0f
    
    val mcGreen = McGreen
    val mcGold = McGold
    val mcRed = Color.Red

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bgType != "none") 0.dp else 1.dp),
        border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column {
                // Latest value and label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TPS 实时波动",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "当前: %.2f".format(tpsList.last()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tpsList.last() > 18) mcGreen else if (tpsList.last() > 15) mcGold else mcRed
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val spacing = width / (tpsList.size.coerceAtLeast(2) - 1)

                        // Draw Grid Lines
                        listOf(20f, 15f, 10f, 5f).forEach { tps ->
                            val y = height - (tps / maxTps) * height
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val path = Path()
                        tpsList.forEachIndexed { index: Int, tps: Float ->
                            val x = index * spacing
                            val y = height - ((tps - minTps) / (maxTps - minTps)) * height
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        val chartColor = if (tpsList.last() > 18) mcGreen else if (tpsList.last() > 15) mcGold else mcRed

                        drawPath(
                            path = path,
                            color = chartColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Fill area under the line
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    chartColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgType: String = "none",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bgType != "none") 0.dp else 2.dp),
        border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UsageCard(
    label: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    bgType: String = "none"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bgType != "none") 0.dp else 2.dp),
        border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = color)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun RawDataSection(title: String, data: String?) {
    Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = data ?: "N/A",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

@Composable
fun WhitelistContent(server: ServerEntity, viewModel: ServerViewModel) {
    val whitelist by viewModel.whitelist.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()
    var newPlayer by remember { mutableStateOf("") }

    LaunchedEffect(server.id) {
        viewModel.fetchWhitelist(server.id)
    }

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("白名单管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.fetchWhitelist(server.id) }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
            tonalElevation = if (bgType != "none") 0.dp else 1.dp,
            border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
        ) {
            Row(
                modifier = Modifier.padding(12.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPlayer,
                    onValueChange = { newPlayer = it },
                    placeholder = { Text("输入玩家游戏名") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (newPlayer.isNotBlank()) {
                            viewModel.addWhitelist(server.id, newPlayer)
                            newPlayer = ""
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (whitelist.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "白名单为空", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "添加玩家到白名单以允许进入", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Text(
                "已授权玩家 (${whitelist.size})", 
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(whitelist) { player ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (bgType != "none") MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (bgType != "none") 0.dp else 1.dp),
                        border = if (bgType != "none") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) else null
                    ) {
                        ListItem(
                            headlineContent = { Text(player, fontWeight = FontWeight.Bold) },
                            leadingContent = { 
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            player.take(1).uppercase(), 
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold, 
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { viewModel.removeWhitelist(server.id, player) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "移除")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandContent(server: ServerEntity, viewModel: ServerViewModel) {
    var command by remember { mutableStateOf("") }
    val serverStates by viewModel.serverStates.collectAsState()
    val state = serverStates[server.id]
    val logs = state?.logs ?: emptyList()
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    // 自动滚动到底部
    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("控制台终端", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Row {
                IconButton(onClick = { 
                    val allLogs = logs.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(allLogs))
                    Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制全部", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { autoScroll = !autoScroll }) {
                    Icon(
                        if (autoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop,
                        contentDescription = "自动滚动",
                        tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = { viewModel.clearLogs(server.id) }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清除日志", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                if (logs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "等待日志推送...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            "Ready to send commands to ${server.name}",
                            color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            val parsedLog = MinecraftTextParser.parse(log)
                            Text(
                                text = parsedLog,
                                color = Color(0xFFE0E0E0),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(vertical = 1.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            clipboardManager.setText(AnnotatedString(log))
                                            Toast.makeText(context, "已复制该行日志", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = command,
                onValueChange = { 
                    command = it 
                },
                label = { Text("输入指令 (无需 /)") },
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.Enter -> {
                                    if (command.isNotBlank()) {
                                        viewModel.executeCommand(server.id, command)
                                        command = ""
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (command.isNotBlank()) {
                        viewModel.executeCommand(server.id, command)
                        command = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "发送")
            }
        }
    }
}

