package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.ClaudeSettings
import kotlinx.coroutines.flow.StateFlow

interface ClaudeSettingsServiceInterface {
    val settings: StateFlow<ClaudeSettings?>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    suspend fun loadSettings()
    suspend fun saveSettings()
    fun updateSettings(settings: ClaudeSettings)
}
