package com.olorin.claudette.ui.screens.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.LoggerFactory
import com.olorin.claudette.models.RemoteFileEntry
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteFileBrowserViewModel @Inject constructor(
    private val fileBrowserService: RemoteFileBrowserServiceInterface
) : ViewModel() {

    private val _entries = MutableStateFlow<List<RemoteFileEntry>>(emptyList())
    val entries: StateFlow<List<RemoteFileEntry>> = _entries.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun initialize(
        host: String,
        port: Int,
        username: String,
        password: String?,
        keyData: ByteArray?,
        startPath: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (!fileBrowserService.isConnected.value) {
                    fileBrowserService.connect(host, port, username, password, keyData)
                    LoggerFactory.i(LOG_TAG, "Connected to $host:$port")
                }
                navigateTo(startPath)
            } catch (e: Exception) {
                _error.value = e.message ?: "Connection failed"
                LoggerFactory.e(LOG_TAG, "Failed to initialize file browser", e)
                _isLoading.value = false
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val entries = fileBrowserService.listDirectory(path)
                val sorted = entries.sortedWith(
                    compareByDescending<RemoteFileEntry> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
                _entries.value = sorted
                _currentPath.value = path
                LoggerFactory.d(LOG_TAG, "Listed ${entries.size} entries in $path")
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to list directory"
                LoggerFactory.e(LOG_TAG, "Failed to list directory: $path", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current == "/" || current.isBlank()) return

        val parent = current.trimEnd('/').substringBeforeLast('/')
        val targetPath = parent.ifBlank { "/" }
        navigateTo(targetPath)
    }

    companion object {
        private const val LOG_TAG = "FileBrowserVM"
    }
}
