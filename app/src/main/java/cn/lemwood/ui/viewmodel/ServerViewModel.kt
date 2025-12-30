package cn.lemwood.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.lemwood.data.local.ServerEntity
import cn.lemwood.data.model.HistoricalMetrics
import cn.lemwood.data.model.ServerMetrics
import cn.lemwood.data.model.ServerStatus
import cn.lemwood.data.repository.ServerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import android.content.SharedPreferences

data class ServerUIState(
    val status: ServerStatus? = null,
    val metrics: ServerMetrics? = null,
    val history: List<HistoricalMetrics> = emptyList(),
    val logs: List<String> = emptyList(),
    val lastError: String? = null,
    val rawStatus: String? = null,
    val rawMetrics: String? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testedStatus: ServerStatus? = null
)

class ServerViewModel(
    private val repository: ServerRepository,
    private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val servers = repository.allServers

    private val _serverStates = MutableStateFlow<Map<Int, ServerUIState>>(emptyMap())
    val serverStates = _serverStates.asStateFlow()

    private val _addServerDialogState = MutableStateFlow(ServerUIState())
    val addServerDialogState = _addServerDialogState.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var logJob: kotlinx.coroutines.Job? = null
    private val _activeServerId = MutableStateFlow<Int?>(null)
    private val refreshMutexes = java.util.concurrent.ConcurrentHashMap<Int, Mutex>()

    // Settings
    private val _pollingInterval = MutableStateFlow(prefs.getLong("polling_interval", 5000L))
    val pollingInterval = _pollingInterval.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode = _themeMode.asStateFlow()

    private val _colorScheme = MutableStateFlow(prefs.getString("color_scheme", "system") ?: "system")
    val colorScheme = _colorScheme.asStateFlow()

    private val _primaryColor = MutableStateFlow(prefs.getString("primary_color", "#6750A4") ?: "#6750A4")
    val primaryColor = _primaryColor.asStateFlow()

    private val _backgroundType = MutableStateFlow(prefs.getString("bg_type", "none") ?: "none")
    val backgroundType = _backgroundType.asStateFlow()

    private val _backgroundPath = MutableStateFlow(prefs.getString("bg_path", "") ?: "")
    val backgroundPath = _backgroundPath.asStateFlow()

    private val _backgroundScale = MutableStateFlow(prefs.getString("bg_scale", "fill") ?: "fill")
    val backgroundScale = _backgroundScale.asStateFlow()

    private val _backgroundVolume = MutableStateFlow(prefs.getFloat("bg_volume", 0.0f))
    val backgroundVolume = _backgroundVolume.asStateFlow()

    private val _backgroundAlpha = MutableStateFlow(prefs.getFloat("bg_alpha", 0.7f))
    val backgroundAlpha = _backgroundAlpha.asStateFlow()

    init {
        startPolling()
        
        // 监听活跃服务器变化，启动/停止日志同步
        viewModelScope.launch {
            _activeServerId.collectLatest { serverId ->
                logJob?.cancel()
                if (serverId != null) {
                    val server = repository.allServers.first().find { it.id == serverId }
                    if (server != null && server.mode == "API" && !server.token.isNullOrBlank()) {
                        startLogSync(server)
                    }
                }
            }
        }

        // 监听轮询间隔变化，重启轮询
        viewModelScope.launch {
            _pollingInterval.collectLatest { 
                startPolling()
            }
        }
    }

    fun setPollingInterval(interval: Long) {
        _pollingInterval.value = interval
        prefs.edit().putLong("polling_interval", interval).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setColorScheme(scheme: String) {
        _colorScheme.value = scheme
        prefs.edit().putString("color_scheme", scheme).apply()
    }

    fun setPrimaryColor(colorHex: String) {
        _primaryColor.value = colorHex
        prefs.edit().putString("primary_color", colorHex).apply()
    }

    fun setBackground(type: String, path: String) {
        _backgroundType.value = type
        _backgroundPath.value = path
        prefs.edit()
            .putString("bg_type", type)
            .putString("bg_path", path)
            .apply()
    }

    fun setBackgroundScale(scale: String) {
        _backgroundScale.value = scale
        prefs.edit().putString("bg_scale", scale).apply()
    }

    fun setBackgroundVolume(volume: Float) {
        _backgroundVolume.value = volume
        prefs.edit().putFloat("bg_volume", volume).apply()
    }

    fun setBackgroundAlpha(alpha: Float) {
        _backgroundAlpha.value = alpha
        prefs.edit().putFloat("bg_alpha", alpha).apply()
    }

    private fun startLogSync(server: ServerEntity) {
        logJob = viewModelScope.launch {
            try {
                // 清空旧日志，避免切换服务器时看到之前的日志
                _serverStates.value = _serverStates.value.toMutableMap().apply {
                    val currentState = get(server.id) ?: ServerUIState()
                    put(server.id, currentState.copy(logs = emptyList()))
                }
                
                repository.getLogStream(server.endpoint, server.token!!)
                    .collect { logLine ->
                        _serverStates.value = _serverStates.value.toMutableMap().apply {
                            val currentState = get(server.id) ?: ServerUIState()
                            // 改进的去重逻辑：检查最后 5 行是否包含当前行
                            if (!currentState.logs.takeLast(5).contains(logLine)) {
                                val newLogs = (currentState.logs + logLine).takeLast(500)
                                put(server.id, currentState.copy(logs = newLogs))
                            }
                        }
                    }
            } catch (e: Exception) {
                _serverStates.value = _serverStates.value.toMutableMap().apply {
                    val currentState = get(server.id) ?: ServerUIState()
                    put(server.id, currentState.copy(
                        lastError = "日志同步失败: ${e.message ?: "Unknown error"}"
                    ))
                }
            }
        }
    }

    fun setActiveServer(serverId: Int?) {
        _activeServerId.value = serverId
    }

    fun testConnection(endpoint: String, token: String?, mode: String) {
        viewModelScope.launch {
            _addServerDialogState.value = _addServerDialogState.value.copy(
                isTesting = true, 
                testResult = null,
                testedStatus = null
            )
            try {
                val status = repository.getServerStatus(endpoint, token, mode)
                var result = "连接成功！版本: ${status.version}\n玩家: ${status.players}/${status.maxPlayers}"
                
                if (mode == "API" && token != null) {
                    try {
                        val response = repository.getWhitelist(endpoint, token)
                        result += "\nToken 验证成功！白名单人数: ${response.players.size}"
                    } catch (e: retrofit2.HttpException) {
                        val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                        result += "\nToken 验证失败 (${e.code()}): $errorBody"
                    } catch (e: Exception) {
                        result += "\nToken 验证失败: ${e.localizedMessage ?: e.message}"
                    }
                }
                _addServerDialogState.value = _addServerDialogState.value.copy(
                    isTesting = false, 
                    testResult = result,
                    testedStatus = status
                )
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message()
                _addServerDialogState.value = _addServerDialogState.value.copy(
                    isTesting = false, 
                    testResult = "连接失败 (${e.code()}): $errorBody"
                )
            } catch (e: Exception) {
                _addServerDialogState.value = _addServerDialogState.value.copy(
                    isTesting = false, 
                    testResult = "连接失败: ${e.localizedMessage ?: e.message}"
                )
            }
        }
    }

    fun resetAddDialogState() {
        _addServerDialogState.value = ServerUIState()
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentServers = repository.allServers.first()
            currentServers.forEach { server ->
                refreshServer(server)
            }
            _isRefreshing.value = false
        }
    }

    private suspend fun refreshServer(server: ServerEntity) {
        val mutex = refreshMutexes.getOrPut(server.id) { Mutex() }
        if (mutex.isLocked) return // 如果正在刷新，则跳过本次
        
        mutex.withLock {
            try {
                val status = repository.getServerStatus(server.endpoint, server.token, server.mode)
            val hasToken = !server.token.isNullOrBlank()
            
            val metrics = if (server.mode == "API" && hasToken) {
                try {
                    repository.getServerMetrics(server.endpoint, server.token)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            // 只有活跃服务器且是 API 模式才获取历史记录和原始数据
            val isActive = _activeServerId.value == server.id
            val history = if (isActive && server.mode == "API" && hasToken) {
                try { 
                    repository.getHistory(server.endpoint, server.token) 
                } catch (e: Exception) { 
                    emptyList() 
                }
            } else {
                _serverStates.value[server.id]?.history ?: emptyList()
            }
            
            val rawStatus = if (isActive) {
                try { repository.getServerStatusRaw(server.endpoint, server.token, server.mode) } catch (e: Exception) { e.message }
            } else null
            
            val rawMetrics = if (isActive && server.mode == "API" && hasToken) {
                try { repository.getServerMetricsRaw(server.endpoint, server.token) } catch (e: Exception) { e.message }
            } else null

            _serverStates.value = _serverStates.value.toMutableMap().apply {
                val currentState = get(server.id) ?: ServerUIState()
                put(server.id, currentState.copy(
                    status = status,
                    metrics = metrics,
                    history = history,
                    rawStatus = rawStatus,
                    rawMetrics = rawMetrics,
                    lastError = null // 成功时清除之前的错误
                ))
            }
        } catch (e: Exception) {
            _serverStates.value = _serverStates.value.toMutableMap().apply {
                val currentState = get(server.id) ?: ServerUIState()
                put(server.id, currentState.copy(
                    lastError = e.message ?: "Unknown error"
                ))
            }
        }
    }
}

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repository.allServers.collectLatest { list ->
                while (true) {
                    list.forEach { server ->
                        launch {
                            // 如果是当前正在查看的服务器，或者间隔一段时间更新一次首页
                            refreshServer(server)
                        }
                    }
                    // 动态调整延迟：如果有活跃服务器，可以快一点；否则使用设置的间隔
                    val interval = _pollingInterval.value
                    val delayTime = if (_activeServerId.value != null) (interval / 2).coerceAtLeast(2000L) else interval
                    delay(delayTime)
                }
            }
        }
    }

    private val _whitelist = MutableStateFlow<List<String>>(emptyList())
    val whitelist = _whitelist.asStateFlow()

    fun fetchWhitelist(serverId: Int) {
        viewModelScope.launch {
            val list = repository.allServers.first()
            val s = list.find { it.id == serverId }
            if (s?.token != null) {
                try {
                    _whitelist.value = repository.getWhitelist(s.endpoint, s.token).players
                } catch (e: Exception) {
                    _whitelist.value = emptyList()
                }
            }
        }
    }

    fun addWhitelist(serverId: Int, name: String) {
        viewModelScope.launch {
            try {
                val list = repository.allServers.first()
                val s = list.find { it.id == serverId }
                if (s?.token != null) {
                    repository.addWhitelist(s.endpoint, s.token, name)
                    fetchWhitelist(serverId)
                }
            } catch (e: Exception) {
                _serverStates.value = _serverStates.value.toMutableMap().apply {
                    val currentState = get(serverId) ?: ServerUIState()
                    put(serverId, currentState.copy(lastError = "添加白名单失败: ${e.message}"))
                }
            }
        }
    }

    fun removeWhitelist(serverId: Int, name: String) {
        viewModelScope.launch {
            try {
                val list = repository.allServers.first()
                val s = list.find { it.id == serverId }
                if (s?.token != null) {
                    repository.removeWhitelist(s.endpoint, s.token, name)
                    fetchWhitelist(serverId)
                }
            } catch (e: Exception) {
                _serverStates.value = _serverStates.value.toMutableMap().apply {
                    val currentState = get(serverId) ?: ServerUIState()
                    put(serverId, currentState.copy(lastError = "移除白名单失败: ${e.message}"))
                }
            }
        }
    }

    fun executeCommand(serverId: Int, command: String) {
        viewModelScope.launch {
            try {
                val list = repository.allServers.first()
                val s = list.find { it.id == serverId }
                if (s?.token != null) {
                    repository.executeCommand(s.endpoint, s.token, command)
                }
            } catch (e: Exception) {
                _serverStates.value = _serverStates.value.toMutableMap().apply {
                    val currentState = get(serverId) ?: ServerUIState()
                    put(serverId, currentState.copy(lastError = "执行指令失败: ${e.message}"))
                }
            }
        }
    }

    fun clearLogs(serverId: Int) {
        _serverStates.value = _serverStates.value.toMutableMap().apply {
            val currentState = get(serverId) ?: ServerUIState()
            put(serverId, currentState.copy(logs = emptyList()))
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.deleteServer(server)
            // 删除服务器后移除其状态，防止统计错误和内存泄漏
            _serverStates.value = _serverStates.value.toMutableMap().apply {
                remove(server.id)
            }
            refreshMutexes.remove(server.id)
        }
    }

    fun addServer(name: String, endpoint: String, token: String?, serverAddress: String?, useAddressForIcon: Boolean, mode: String) {
        viewModelScope.launch {
            repository.addServer(ServerEntity(
                name = name, 
                endpoint = endpoint, 
                token = token,
                serverAddress = serverAddress,
                useAddressForIcon = useAddressForIcon,
                mode = mode
            ))
        }
    }
}
