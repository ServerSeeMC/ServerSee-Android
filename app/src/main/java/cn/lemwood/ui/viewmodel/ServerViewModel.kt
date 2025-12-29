package cn.lemwood.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.lemwood.data.local.ServerEntity
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

data class ServerUIState(
    val status: ServerStatus? = null,
    val metrics: ServerMetrics? = null,
    val history: List<ServerMetrics> = emptyList(),
    val logs: List<String> = emptyList(),
    val lastError: String? = null,
    val rawStatus: String? = null,
    val rawMetrics: String? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testedStatus: ServerStatus? = null
)

class ServerViewModel(private val repository: ServerRepository) : ViewModel() {

    val servers = repository.allServers

    private val _serverStates = MutableStateFlow<Map<Int, ServerUIState>>(emptyMap())
    val serverStates = _serverStates.asStateFlow()

    private val _addServerDialogState = MutableStateFlow(ServerUIState())
    val addServerDialogState = _addServerDialogState.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var logJob: kotlinx.coroutines.Job? = null
    private val _activeServerId = MutableStateFlow<Int?>(null)

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
    }

    private fun startLogSync(server: ServerEntity) {
        logJob = viewModelScope.launch {
            // 清空旧日志
            _serverStates.value = _serverStates.value.toMutableMap().apply {
                put(server.id, (get(server.id) ?: ServerUIState()).copy(logs = emptyList()))
            }
            
            repository.getLogStream(server.endpoint, server.token!!)
                .collect { logLine ->
                    _serverStates.value = _serverStates.value.toMutableMap().apply {
                        val currentState = get(server.id) ?: ServerUIState()
                        val newLogs = (currentState.logs + logLine).takeLast(200) // 最多保留 200 行
                        put(server.id, currentState.copy(logs = newLogs))
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

    fun refreshAll() {
        viewModelScope.launch {
            val currentServers = repository.allServers.first()
            currentServers.forEach { server ->
                refreshServer(server)
            }
        }
    }

    private suspend fun refreshServer(server: ServerEntity) {
        try {
            val status = repository.getServerStatus(server.endpoint, server.token, server.mode)
            val metrics = if (server.mode == "API") {
                repository.getServerMetrics(server.endpoint, server.token)
            } else {
                null
            }
            
            // 只有活跃服务器且是 API 模式才获取历史记录和原始数据
            val isActive = _activeServerId.value == server.id
            val history = if (isActive && server.mode == "API") {
                try { repository.getHistory(server.endpoint) } catch (e: Exception) { emptyList() }
            } else {
                _serverStates.value[server.id]?.history ?: emptyList()
            }
            
            val rawStatus = if (isActive) {
                try { repository.getServerStatusRaw(server.endpoint, server.token, server.mode) } catch (e: Exception) { e.message }
            } else null
            
            val rawMetrics = if (isActive && server.mode == "API") {
                try { repository.getServerMetricsRaw(server.endpoint, server.token) } catch (e: Exception) { e.message }
            } else null

            _serverStates.value = _serverStates.value.toMutableMap().apply {
                val currentState = get(server.id) ?: ServerUIState()
                put(server.id, currentState.copy(
                    status = status,
                    metrics = metrics,
                    history = history,
                    rawStatus = rawStatus,
                    rawMetrics = rawMetrics
                ))
            }
        } catch (e: Exception) {
            _serverStates.value = _serverStates.value.toMutableMap().apply {
                put(server.id, ServerUIState(
                    lastError = e.message ?: "Unknown error"
                ))
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
                    // 动态调整延迟：如果有活跃服务器，可以快一点；否则慢一点
                    val delayTime = if (_activeServerId.value != null) 3000L else 5000L
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
            val list = repository.allServers.first()
            val s = list.find { it.id == serverId }
            if (s?.token != null) {
                repository.addWhitelist(s.endpoint, s.token, name)
                fetchWhitelist(serverId)
            }
        }
    }

    fun removeWhitelist(serverId: Int, name: String) {
        viewModelScope.launch {
            val list = repository.allServers.first()
            val s = list.find { it.id == serverId }
            if (s?.token != null) {
                repository.removeWhitelist(s.endpoint, s.token, name)
                fetchWhitelist(serverId)
            }
        }
    }

    fun executeCommand(serverId: Int, command: String) {
        viewModelScope.launch {
            val list = repository.allServers.first()
            val s = list.find { it.id == serverId }
            if (s?.token != null) {
                repository.executeCommand(s.endpoint, s.token, command)
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
