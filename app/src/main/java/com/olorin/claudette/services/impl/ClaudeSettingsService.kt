package com.olorin.claudette.services.impl

import com.olorin.claudette.models.ClaudeSettings
import com.olorin.claudette.services.interfaces.ClaudeSettingsServiceInterface
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import timber.log.Timber

class ClaudeSettingsService(
    private val fileBrowserService: RemoteFileBrowserServiceInterface
) : ClaudeSettingsServiceInterface {

    private val _settings = MutableStateFlow<ClaudeSettings?>(null)
    override val settings: StateFlow<ClaudeSettings?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun loadSettings() {
        _isLoading.value = true
        _error.value = null

        try {
            val data = fileBrowserService.readFile(SETTINGS_PATH)
            val content = data.toString(Charsets.UTF_8)
            _settings.value = json.decodeFromString<ClaudeSettings>(content)
            Timber.i("Loaded Claude settings")
        } catch (e: Exception) {
            // File might not exist yet, which is expected
            _settings.value = ClaudeSettings()
            Timber.d("No existing Claude settings found, starting fresh: %s", e.message)
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun saveSettings() {
        val currentSettings = _settings.value ?: return

        try {
            val content = json.encodeToString(ClaudeSettings.serializer(), currentSettings)
            fileBrowserService.writeFile(content.toByteArray(Charsets.UTF_8), SETTINGS_PATH)
            Timber.i("Saved Claude settings")
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to save settings"
            Timber.e(e, "Failed to save Claude settings")
        }
    }

    override fun updateSettings(settings: ClaudeSettings) {
        _settings.value = settings
    }

    companion object {
        private const val SETTINGS_PATH = "~/.claude/settings.json"
    }
}
