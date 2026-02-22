package com.olorin.claudette.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.models.AuthMethod
import com.olorin.claudette.models.ClaudeResource
import com.olorin.claudette.models.ConnectionSettings
import com.olorin.claudette.models.ConnectionState
import com.olorin.claudette.models.HostKeyVerificationResult
import com.olorin.claudette.models.NetworkStatus
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.models.TerminalTab
import android.content.Context
import com.olorin.claudette.services.impl.AgentActivityParser
import com.olorin.claudette.services.impl.AuthUrlInterceptor
import com.olorin.claudette.services.impl.NetworkProbeService
import com.olorin.claudette.services.impl.PermissionNotificationService
import com.olorin.claudette.services.impl.SshConnectionManager
import com.olorin.claudette.services.impl.TofuHostKeyValidator
import com.olorin.claudette.services.impl.HostKeyVerificationDelegate
import com.olorin.claudette.services.interfaces.HostKeyStoreInterface
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.OutputInterceptor
import com.olorin.claudette.services.interfaces.ProfileStoreInterface
import com.olorin.claudette.services.interfaces.TmuxSessionServiceInterface
import com.olorin.claudette.terminal.TerminalController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class HostKeyAlertState(
    val id: String = UUID.randomUUID().toString(),
    val result: HostKeyVerificationResult,
    val fingerprint: String,
    val hostIdentifier: String,
    val deferred: CompletableDeferred<Boolean>
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val profileStore: ProfileStoreInterface,
    private val keychainService: KeychainServiceInterface,
    private val hostKeyStore: HostKeyStoreInterface,
    private val tmuxService: TmuxSessionServiceInterface,
    private val config: AppConfiguration,
    savedStateHandle: SavedStateHandle
) : ViewModel(), HostKeyVerificationDelegate {

    private val _profile = MutableStateFlow<ServerProfile?>(null)
    val profile: StateFlow<ServerProfile?> = _profile.asStateFlow()

    private val _tabs = MutableStateFlow<List<TerminalTab>>(emptyList())
    val tabs: StateFlow<List<TerminalTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val activeConnectionState: StateFlow<ConnectionState> = _activeConnectionState.asStateFlow()

    private val _hostKeyAlert = MutableStateFlow<HostKeyAlertState?>(null)
    val hostKeyAlert: StateFlow<HostKeyAlertState?> = _hostKeyAlert.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _claudeResources = MutableStateFlow<List<ClaudeResource>>(emptyList())
    val claudeResources: StateFlow<List<ClaudeResource>> = _claudeResources.asStateFlow()

    private val _detectedAuthUrl = MutableStateFlow<String?>(null)
    val detectedAuthUrl: StateFlow<String?> = _detectedAuthUrl.asStateFlow()

    val agentParser = AgentActivityParser()
    val authInterceptor = AuthUrlInterceptor(appContext)
    val permissionService = PermissionNotificationService(appContext)

    // Connection managers and terminal controllers per tab
    private val connectionManagers = mutableMapOf<String, SshConnectionManager>()
    private val terminalControllers = mutableMapOf<String, TerminalController>()
    private var networkProbeService: NetworkProbeService? = null

    private val hostKeyValidator = TofuHostKeyValidator(hostKeyStore).also {
        it.delegate = this
    }

    private var settings: ConnectionSettings? = null
    private var tabCounter = 1
    private var hasDiscoveredResources = false

    fun initialize(profileId: String, projectPath: String) {
        viewModelScope.launch {
            val profiles = profileStore.loadProfiles()
            val foundProfile = profiles.firstOrNull { it.id == profileId } ?: return@launch
            _profile.value = foundProfile

            settings = foundProfile.toConnectionSettings(projectPath)

            // Update last connected
            val updated = foundProfile.copy(
                lastConnectedAt = System.currentTimeMillis(),
                lastProjectPath = projectPath
            )
            profileStore.updateProfile(updated)

            // Create first tab and connect
            val manager = createConnectionManager()
            val tabId = UUID.randomUUID().toString()
            connectionManagers[tabId] = manager

            val controller = TerminalController(manager, viewModelScope)
            terminalControllers[tabId] = controller
            controller.start()

            val tab = TerminalTab(id = tabId, connectionManagerId = tabId, label = "Terminal 1")
            _tabs.value = listOf(tab)
            _activeTabId.value = tabId

            // Observe connection state
            observeConnectionState(tabId)

            // Start network probe
            startNetworkProbe(foundProfile.host, foundProfile.port)

            // Connect
            connect(manager, foundProfile)
        }
    }

    fun addTab() {
        val currentSettings = settings ?: return
        val currentProfile = _profile.value ?: return

        tabCounter++
        val manager = createConnectionManager()
        val tabId = UUID.randomUUID().toString()
        connectionManagers[tabId] = manager

        val controller = TerminalController(manager, viewModelScope)
        terminalControllers[tabId] = controller
        controller.start()

        val tab = TerminalTab(id = tabId, connectionManagerId = tabId, label = "Terminal $tabCounter")
        _tabs.value = _tabs.value + tab
        _activeTabId.value = tabId

        observeConnectionState(tabId)
        connect(manager, currentProfile)
    }

    fun closeTab(tabId: String) {
        if (_tabs.value.size <= 1) return
        connectionManagers[tabId]?.disconnect()
        terminalControllers[tabId]?.stop()
        connectionManagers.remove(tabId)
        terminalControllers.remove(tabId)

        val newTabs = _tabs.value.filter { it.id != tabId }
        _tabs.value = newTabs

        if (_activeTabId.value == tabId) {
            _activeTabId.value = newTabs.first().id
            observeConnectionState(newTabs.first().id)
        }
    }

    fun selectTab(tabId: String) {
        if (tabId == _activeTabId.value) return
        _activeTabId.value = tabId
        observeConnectionState(tabId)
    }

    fun getTerminalController(tabId: String): TerminalController? {
        return terminalControllers[tabId]
    }

    fun getActiveTerminalController(): TerminalController? {
        return terminalControllers[_activeTabId.value]
    }

    fun getActiveConnectionManager(): SshConnectionManager? {
        return connectionManagers[_activeTabId.value]
    }

    fun sendSnippet(command: String) {
        val controller = getActiveTerminalController() ?: return
        val textBytes = ("$command ").toByteArray(Charsets.UTF_8)
        controller.sendInput(textBytes)
        viewModelScope.launch {
            kotlinx.coroutines.delay(50)
            controller.sendInput(byteArrayOf(0x0D))
        }
    }

    fun reconnect() {
        val currentProfile = _profile.value ?: return
        for ((tabId, manager) in connectionManagers) {
            val state = manager.connectionState.value
            if (state is ConnectionState.Disconnected || state is ConnectionState.Failed) {
                manager.reconnect()
            }
        }
    }

    fun disconnect() {
        networkProbeService?.stop()
        for ((_, manager) in connectionManagers) {
            manager.disconnect()
        }
        for ((_, controller) in terminalControllers) {
            controller.stop()
        }
    }

    fun copySessionToClipboard(): String? {
        return getActiveTerminalController()?.getTerminalContent()
    }

    fun clearDetectedAuthUrl() {
        authInterceptor.clearDetectedUrl()
        _detectedAuthUrl.value = null
    }

    // Host key verification
    fun acceptHostKey() {
        val alert = _hostKeyAlert.value ?: return
        _hostKeyAlert.value = null
        alert.deferred.complete(true)
    }

    fun rejectHostKey() {
        val alert = _hostKeyAlert.value ?: return
        _hostKeyAlert.value = null
        alert.deferred.complete(false)
    }

    override suspend fun verifyHostKey(
        result: HostKeyVerificationResult,
        fingerprint: String,
        hostIdentifier: String
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        _hostKeyAlert.value = HostKeyAlertState(
            result = result,
            fingerprint = fingerprint,
            hostIdentifier = hostIdentifier,
            deferred = deferred
        )
        return deferred.await()
    }

    private fun createConnectionManager(): SshConnectionManager {
        val manager = SshConnectionManager(
            config = config,
            keychainService = keychainService,
            tmuxService = tmuxService
        )

        // Wire output interceptor
        manager.outputInterceptor = OutputInterceptor { bytes ->
            agentParser.processOutput(bytes)
            permissionService.processOutput(bytes)
            authInterceptor.processOutput(bytes)
            _detectedAuthUrl.value = authInterceptor.detectedUrl
        }

        return manager
    }

    private fun connect(manager: SshConnectionManager, profile: ServerProfile) {
        val currentSettings = settings ?: return

        val credential: String = when (profile.authMethod) {
            is AuthMethod.Password -> keychainService.retrievePassword(profile.id) ?: ""
            is AuthMethod.GeneratedKey, is AuthMethod.ImportedKey -> ""
        }

        hostKeyValidator.configure(currentSettings.host, currentSettings.port)

        manager.connect(
            settings = currentSettings,
            credential = credential,
            profileId = profile.id
        )
    }

    private fun observeConnectionState(tabId: String) {
        val manager = connectionManagers[tabId] ?: return
        viewModelScope.launch {
            manager.connectionState.collect { state ->
                if (tabId == _activeTabId.value) {
                    _activeConnectionState.value = state
                }
            }
        }
    }

    private fun startNetworkProbe(host: String, port: Int) {
        val probe = NetworkProbeService(
            host = host,
            port = port,
            intervalSeconds = config.networkProbeIntervalSeconds,
            timeoutSeconds = config.networkProbeTimeoutSeconds,
            degradedThresholdMs = config.networkProbeDegradedThresholdMs
        )
        networkProbeService = probe
        viewModelScope.launch {
            probe.start()
            probe.status.collect { status ->
                _networkStatus.value = status
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
