package cn.lemwood.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.lemwood.ui.components.AdvancedColorPicker
import cn.lemwood.ui.viewmodel.ServerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ServerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val version = packageInfo.versionName ?: "1.0.0"
    
    val themeMode by viewModel.themeMode.collectAsState()
    val colorScheme by viewModel.colorScheme.collectAsState()
    val primaryColor by viewModel.primaryColor.collectAsState()
    val pollingInterval by viewModel.pollingInterval.collectAsState()
    
    val bgType by viewModel.backgroundType.collectAsState()
    val bgScale by viewModel.backgroundScale.collectAsState()
    val bgVolume by viewModel.backgroundVolume.collectAsState()
    val bgAlpha by viewModel.backgroundAlpha.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    
    var tempColor by remember { mutableStateOf(Color(android.graphics.Color.parseColor(primaryColor))) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setBackground("image", it.toString())
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setBackground("video", it.toString())
        }
    }

    val presetColors = listOf(
        "#6750A4" to "默认蓝",
        "#4CAF50" to "森林绿",
        "#F44336" to "活力红",
        "#FF9800" to "阳光橙",
        "#9C27B0" to "梦幻紫",
        "#00BCD4" to "清澈青"
    )

    Scaffold(
        containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (bgType != "none") Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 外观与交互
            SettingsGroup(title = "外观与交互") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "主题模式",
                    subtitle = when (themeMode) {
                        "light" -> "浅色模式"
                        "dark" -> "深色模式"
                        else -> "跟随系统"
                    },
                    onClick = { showThemeDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = "主颜色方案",
                    subtitle = when (colorScheme) {
                        "system" -> "从系统获取 (动态色彩)"
                        "preset" -> "预设配色方案"
                        "custom" -> "自定义配色: $primaryColor"
                        else -> "未知"
                    },
                    onClick = { showColorDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Wallpaper,
                    title = "背景设置",
                    subtitle = when (bgType) {
                        "none" -> "无背景"
                        "image" -> "图片背景"
                        "video" -> "视频背景"
                        else -> "未知"
                    },
                    onClick = { showBackgroundDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "数据轮询间隔",
                    subtitle = "${pollingInterval}s (默认 5s)",
                    onClick = { showIntervalDialog = true }
                )
            }

            // 关于
            SettingsGroup(title = "关于软件") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本",
                    subtitle = "v$version",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "开源地址",
                    subtitle = "GitHub: ServerSee-Plugin",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ServerSeeMC/ServerSee-Plugin"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    // 主题对话框
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题模式") },
            text = {
                Column {
                    ThemeOption("跟随系统", themeMode == "system") { viewModel.setThemeMode("system"); showThemeDialog = false }
                    ThemeOption("浅色模式", themeMode == "light") { viewModel.setThemeMode("light"); showThemeDialog = false }
                    ThemeOption("深色模式", themeMode == "dark") { viewModel.setThemeMode("dark"); showThemeDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("取消") }
            }
        )
    }

    // 颜色对话框
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("主颜色方案") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("预设方案", style = MaterialTheme.typography.labelLarge)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setColorScheme("system")
                                showColorDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = colorScheme == "system", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("从系统获取 (Android 12+)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        presetColors.forEach { (hex, _) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        viewModel.setColorScheme("preset")
                                        viewModel.setPrimaryColor(hex)
                                        showColorDialog = false
                                    },
                                shape = CircleShape,
                                color = color,
                                border = if (primaryColor == hex && colorScheme == "preset") {
                                    androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                                } else null
                            ) {}
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("高级颜色选择器", style = MaterialTheme.typography.labelLarge)
                    
                    AdvancedColorPicker(
                        initialColor = Color(android.graphics.Color.parseColor(primaryColor)),
                        onColorChanged = { tempColor = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hex = String.format("#%06X", (0xFFFFFF and tempColor.toArgb()))
                    viewModel.setColorScheme("custom")
                    viewModel.setPrimaryColor(hex)
                    showColorDialog = false
                }) {
                    Text("应用选择")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 背景对话框
    if (showBackgroundDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = { Text("背景设置") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("背景类型", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = bgType == "none", onClick = { viewModel.setBackground("none", "") })
                        Text("无", modifier = Modifier.clickable { viewModel.setBackground("none", "") })
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = bgType == "image", onClick = { imageLauncher.launch(arrayOf("image/*")) })
                        Text("图片", modifier = Modifier.clickable { imageLauncher.launch(arrayOf("image/*")) })
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = bgType == "video", onClick = { videoLauncher.launch(arrayOf("video/*")) })
                        Text("视频", modifier = Modifier.clickable { videoLauncher.launch(arrayOf("video/*")) })
                    }

                    if (bgType != "none") {
                        Divider()
                        Text("缩放模式", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = bgScale == "fill",
                                onClick = { viewModel.setBackgroundScale("fill") },
                                label = { Text("撑满") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = bgScale == "fit",
                                onClick = { viewModel.setBackgroundScale("fit") },
                                label = { Text("自适应") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = bgScale == "stretch",
                                onClick = { viewModel.setBackgroundScale("stretch") },
                                label = { Text("拉伸") }
                            )
                        }

                        if (bgType == "video") {
                            Divider()
                            Text("视频音量: ${(bgVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = bgVolume,
                                onValueChange = { viewModel.setBackgroundVolume(it) },
                                valueRange = 0f..1f
                            )
                        }

                        Divider()
                        Text("背景透明度 (内容覆盖层): ${(bgAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = bgAlpha,
                            onValueChange = { viewModel.setBackgroundAlpha(it) },
                            valueRange = 0f..1f
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackgroundDialog = false }) { Text("完成") }
            }
        )
    }

    // 轮询间隔对话框
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("设置轮询间隔") },
            text = {
                Column {
                    IntervalOption("3秒", 3, pollingInterval) { viewModel.setPollingInterval(3); showIntervalDialog = false }
                    IntervalOption("5秒 (默认)", 5, pollingInterval) { viewModel.setPollingInterval(5); showIntervalDialog = false }
                    IntervalOption("10秒", 10, pollingInterval) { viewModel.setPollingInterval(10); showIntervalDialog = false }
                    IntervalOption("30秒", 30, pollingInterval) { viewModel.setPollingInterval(30); showIntervalDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
fun IntervalOption(
    text: String,
    value: Long,
    current: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = value == current, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}
