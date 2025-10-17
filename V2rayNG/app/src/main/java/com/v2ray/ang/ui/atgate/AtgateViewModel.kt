package com.v2ray.ang.ui.atgate

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.R
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConnectionStatus {
    CONNECTED,
    READY,
    IDLE
}

@Immutable
data class AtgateUiState(
    val isRunning: Boolean = false,
    val connectionInProgress: Boolean = false,
    val importInProgress: Boolean = false,
    val hasProfile: Boolean = false,
    val profileName: String = "",
    val profileEndpoint: String = "",
    val latencyMs: Long? = null,
    val lastPingLabel: String = "",
    val status: ConnectionStatus = ConnectionStatus.IDLE,
    val showImportSheet: Boolean = false,
    val connectedSince: Long? = null
)

sealed interface AtgateEvent {
    data class Snackbar(val message: String, val isError: Boolean) : AtgateEvent
    data class ProfileImported(val guid: String) : AtgateEvent
}

class AtgateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AtgateUiState())
    val uiState: StateFlow<AtgateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AtgateEvent>()
    val events: SharedFlow<AtgateEvent> = _events.asSharedFlow()

    private var lastConnectedTimestamp: Long? = null

    init {
        syncSelectedProfile()
    }

    fun syncSelectedProfile() {
        viewModelScope.launch(Dispatchers.Default) {
            val guid = MmkvManager.getSelectServer()
            val profile = guid?.let { MmkvManager.decodeServerConfig(it) }
            val affiliation = guid?.let { MmkvManager.decodeServerAffiliationInfo(it) }
            val endpoint = buildString {
                if (!profile?.server.isNullOrBlank()) {
                    append(profile?.server)
                    profile?.serverPort?.takeIf { it.isNotBlank() }?.let {
                        append(":")
                        append(it)
                    }
                }
            }
            _uiState.update { current ->
                current.copy(
                    hasProfile = !guid.isNullOrBlank(),
                    profileName = profile?.remarks.orEmpty(),
                    profileEndpoint = endpoint,
                    latencyMs = affiliation?.testDelayMillis?.takeIf { it > 0 },
                    status = deriveStatus(current.isRunning, !guid.isNullOrBlank())
                )
            }
        }
    }

    fun onServiceStateChanged(running: Boolean) {
        if (running) {
            if (lastConnectedTimestamp == null) {
                lastConnectedTimestamp = System.currentTimeMillis()
            }
        } else {
            lastConnectedTimestamp = null
        }
        _uiState.update { current ->
            current.copy(
                isRunning = running,
                connectionInProgress = false,
                status = deriveStatus(running, current.hasProfile),
                connectedSince = lastConnectedTimestamp
            )
        }
    }

    fun onConnectionAttemptStarted() {
        _uiState.update { it.copy(connectionInProgress = true) }
    }

    fun onConnectionAttemptFailed(@StringRes messageRes: Int) {
        showSnackbar(messageRes, isError = true)
        _uiState.update { it.copy(connectionInProgress = false) }
    }

    fun onConnectionCancelled() {
        _uiState.update { it.copy(connectionInProgress = false) }
    }

    fun setShowImportSheet(show: Boolean) {
        _uiState.update { it.copy(showImportSheet = show) }
    }

    fun showSnackbar(@StringRes messageRes: Int, isError: Boolean = false) {
        val message = getApplication<Application>().getString(messageRes)
        showSnackbar(message, isError)
    }

    fun showSnackbar(message: String, isError: Boolean = false) {
        viewModelScope.launch {
            _events.emit(AtgateEvent.Snackbar(message = message, isError = isError))
        }
    }

    fun onPingUpdated(content: String?) {
        _uiState.update { it.copy(lastPingLabel = content.orEmpty()) }
        syncSelectedProfile()
    }

    fun importVless(link: String) {
        if (link.isBlank()) {
            showSnackbar(R.string.atgate_import_error_empty, isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(importInProgress = true) }
            val parsedConfig = withContext(Dispatchers.Default) {
                runCatching { VlessFmt.parse(link.trim()) }
            }
            val config = parsedConfig.getOrNull()
            if (parsedConfig.isFailure || config == null) {
                _uiState.update { it.copy(importInProgress = false) }
                showSnackbar(
                    if (parsedConfig.isFailure) {
                        R.string.atgate_import_error_unknown
                    } else {
                        R.string.atgate_import_error_invalid
                    },
                    isError = true
                )
                return@launch
            }

            if (config.remarks.isBlank() || config.remarks.equals("none", true)) {
                config.remarks = getApplication<Application>().getString(R.string.atgate_profile_default_name)
            }

            val guid = withContext(Dispatchers.Default) {
                val key = MmkvManager.encodeServerConfig("", config)
                MmkvManager.setSelectServer(key)
                key
            }

            _uiState.update {
                it.copy(
                    importInProgress = false,
                    showImportSheet = false,
                    hasProfile = true,
                    profileName = config.remarks,
                    profileEndpoint = buildString {
                        if (!config.server.isNullOrBlank()) {
                            append(config.server)
                            if (!config.serverPort.isNullOrBlank()) {
                                append(":")
                                append(config.serverPort)
                            }
                        }
                    },
                    status = deriveStatus(it.isRunning, true)
                )
            }

            _events.emit(AtgateEvent.ProfileImported(guid))
            _events.emit(AtgateEvent.Snackbar(getApplication<Application>().getString(R.string.atgate_import_success), isError = false))
        }
    }

    private fun deriveStatus(isRunning: Boolean, hasProfile: Boolean): ConnectionStatus {
        return when {
            isRunning -> ConnectionStatus.CONNECTED
            hasProfile -> ConnectionStatus.READY
            else -> ConnectionStatus.IDLE
        }
    }
}
