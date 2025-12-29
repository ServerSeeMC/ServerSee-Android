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

    init {
        startPolling()
    }

    fun testConnection(endpoint: String, token: String?) {
        viewModelScope.launch {
            _addServerDialogState.value = _addServerDialogState.value.copy(
                isTesting = true, 
                testResult = null,
                testedStatus = null
            )
            try {
                val status = repository.getServerStatus(endpoint, token)
                var result = "连接成功！版本: ${status.version}\n玩家: ${status.players}/${status.maxPlayers}"
                
                if (token != null) {
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
            val status = repository.getServerStatus(server.endpoint, server.token)
            val metrics = repository.getServerMetrics(server.endpoint, server.token)
            val rawStatus = try { repository.getServerStatusRaw(server.endpoint, server.token) } catch (e: Exception) { e.message }
            val rawMetrics = try { repository.getServerMetricsRaw(server.endpoint, server.token) } catch (e: Exception) { e.message }
            
            _serverStates.value = _serverStates.value.toMutableMap().apply {
                put(server.id, ServerUIState(
                    status = status,
                    metrics = metrics,
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
        viewModelScope.launch {
            repository.allServers.distinctUntilChanged().collectLatest { list ->
                while (true) {
                    list.forEach { server ->
                        launch {
                            refreshServer(server)
                        }
                    }
                    delay(5000)
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

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.deleteServer(server)
        }
    }

    fun addServer(name: String, endpoint: String, token: String?, serverAddress: String?, useAddressForIcon: Boolean) {
        viewModelScope.launch {
            repository.addServer(name, endpoint, token, serverAddress, useAddressForIcon)
        }
    }
}
