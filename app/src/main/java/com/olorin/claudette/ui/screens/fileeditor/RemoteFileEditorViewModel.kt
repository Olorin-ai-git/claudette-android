package com.olorin.claudette.ui.screens.fileeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.config.LoggerFactory
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteFileEditorViewModel @Inject constructor(
    private val fileBrowserService: RemoteFileBrowserServiceInterface,
    private val config: AppConfiguration
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content: MutableStateFlow<String> = _content

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadFile(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val data = fileBrowserService.readFile(path)
                val maxSize = config.fileEditorMaxSizeBytes

                if (data.size > maxSize) {
                    _error.value = "File too large (${formatFileSize(data.size.toLong())}). " +
                        "Maximum supported size is ${formatFileSize(maxSize.toLong())}."
                    LoggerFactory.w(
                        LOG_TAG,
                        "File exceeds size limit: ${data.size} > $maxSize bytes"
                    )
                    return@launch
                }

                _content.value = data.toString(Charsets.UTF_8)
                LoggerFactory.i(LOG_TAG, "Loaded file: $path (${data.size} bytes)")
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to read file"
                LoggerFactory.e(LOG_TAG, "Failed to load file: $path", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFile(path: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            try {
                val data = _content.value.toByteArray(Charsets.UTF_8)
                fileBrowserService.writeFile(data, path)
                LoggerFactory.i(LOG_TAG, "Saved file: $path (${data.size} bytes)")
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save file"
                LoggerFactory.e(LOG_TAG, "Failed to save file: $path", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        private const val LOG_TAG = "FileEditorVM"
        private const val BYTES_PER_KB = 1024L
        private const val BYTES_PER_MB = 1024L * 1024L

        private fun formatFileSize(bytes: Long): String = when {
            bytes >= BYTES_PER_MB -> "${bytes / BYTES_PER_MB} MB"
            bytes >= BYTES_PER_KB -> "${bytes / BYTES_PER_KB} KB"
            else -> "$bytes bytes"
        }
    }
}
