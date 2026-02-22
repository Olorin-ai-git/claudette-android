package com.olorin.claudette.ui.screens.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.config.LoggerFactory
import com.olorin.claudette.models.AuthMethod
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.ProfileStoreInterface
import com.olorin.claudette.services.interfaces.SshKeyServiceInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMethodSelection {
    PASSWORD,
    GENERATED_KEY,
    IMPORTED_KEY
}

sealed class SaveResult {
    data object Idle : SaveResult()
    data object Saving : SaveResult()
    data object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val profileStore: ProfileStoreInterface,
    private val keychainService: KeychainServiceInterface,
    private val sshKeyService: SshKeyServiceInterface,
    private val config: AppConfiguration
) : ViewModel() {

    var name by mutableStateOf("")
    var host by mutableStateOf("")
    var port by mutableIntStateOf(0)
    var username by mutableStateOf("")
    var projectPath by mutableStateOf("")
    var password by mutableStateOf("")
    var macAddress by mutableStateOf("")
    var authMethodSelection by mutableStateOf(AuthMethodSelection.PASSWORD)
    var publicKey by mutableStateOf<String?>(null)

    private var existingProfile: ServerProfile? = null
    private var currentKeyTag: String? = null

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    init {
        port = config.sshDefaultPort
    }

    fun loadProfile(profileId: String) {
        if (profileId == NEW_PROFILE_ID) return

        viewModelScope.launch {
            val profiles = profileStore.loadProfiles()
            val profile = profiles.find { it.id == profileId } ?: return@launch

            existingProfile = profile
            name = profile.name
            host = profile.host
            port = profile.port
            username = profile.username
            projectPath = profile.lastProjectPath ?: ""
            macAddress = profile.macAddress ?: ""

            when (val auth = profile.authMethod) {
                is AuthMethod.Password -> {
                    authMethodSelection = AuthMethodSelection.PASSWORD
                    password = keychainService.retrievePassword(profile.id) ?: ""
                }
                is AuthMethod.GeneratedKey -> {
                    authMethodSelection = AuthMethodSelection.GENERATED_KEY
                    currentKeyTag = auth.keyTag
                    publicKey = sshKeyService.publicKeyString(auth.keyTag)
                }
                is AuthMethod.ImportedKey -> {
                    authMethodSelection = AuthMethodSelection.IMPORTED_KEY
                    currentKeyTag = auth.keyTag
                    publicKey = sshKeyService.publicKeyString(auth.keyTag)
                }
            }

            LoggerFactory.d(LOG_TAG, "Loaded profile for editing: ${profile.name}")
        }
    }

    fun saveProfile() {
        val validationError = validateFields()
        if (validationError != null) {
            _saveResult.value = SaveResult.Error(validationError)
            return
        }

        _saveResult.value = SaveResult.Saving

        viewModelScope.launch {
            try {
                val authMethod = buildAuthMethod()
                val profile = existingProfile?.copy(
                    name = name.trim(),
                    host = host.trim(),
                    port = port,
                    username = username.trim(),
                    authMethod = authMethod,
                    lastProjectPath = projectPath.trim().ifEmpty { null },
                    macAddress = macAddress.trim().ifEmpty { null }
                ) ?: ServerProfile.create(
                    name = name.trim(),
                    host = host.trim(),
                    port = port,
                    username = username.trim(),
                    authMethod = authMethod,
                    lastProjectPath = projectPath.trim().ifEmpty { null },
                    macAddress = macAddress.trim().ifEmpty { null }
                )

                // Store password if using password auth
                if (authMethodSelection == AuthMethodSelection.PASSWORD && password.isNotEmpty()) {
                    keychainService.storePassword(password, profile.id)
                }

                if (existingProfile != null) {
                    profileStore.updateProfile(profile)
                } else {
                    profileStore.saveProfile(profile)
                }

                LoggerFactory.i(LOG_TAG, "Saved profile: ${profile.name}")
                _saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                LoggerFactory.e(LOG_TAG, "Failed to save profile", e)
                _saveResult.value = SaveResult.Error(e.message ?: "Failed to save profile")
            }
        }
    }

    fun generateSshKey() {
        viewModelScope.launch {
            try {
                // Clean up old key if one exists
                currentKeyTag?.let { sshKeyService.deleteKey(it) }

                val result = sshKeyService.generateEd25519KeyPair()
                currentKeyTag = result.privateKeyTag
                publicKey = result.publicKeyString
                authMethodSelection = AuthMethodSelection.GENERATED_KEY
                LoggerFactory.i(LOG_TAG, "Generated new SSH key pair")
            } catch (e: Exception) {
                LoggerFactory.e(LOG_TAG, "Failed to generate SSH key", e)
            }
        }
    }

    fun importSshKey(pemString: String) {
        viewModelScope.launch {
            try {
                // Clean up old key if one exists
                currentKeyTag?.let { sshKeyService.deleteKey(it) }

                val result = sshKeyService.importPrivateKey(pemString)
                currentKeyTag = result.privateKeyTag
                publicKey = result.publicKeyString
                authMethodSelection = AuthMethodSelection.IMPORTED_KEY
                LoggerFactory.i(LOG_TAG, "Imported SSH key")
            } catch (e: Exception) {
                LoggerFactory.e(LOG_TAG, "Failed to import SSH key", e)
            }
        }
    }

    private fun buildAuthMethod(): AuthMethod {
        return when (authMethodSelection) {
            AuthMethodSelection.PASSWORD -> AuthMethod.Password
            AuthMethodSelection.GENERATED_KEY -> {
                val tag = requireNotNull(currentKeyTag) { "No SSH key generated" }
                AuthMethod.GeneratedKey(keyTag = tag)
            }
            AuthMethodSelection.IMPORTED_KEY -> {
                val tag = requireNotNull(currentKeyTag) { "No SSH key imported" }
                AuthMethod.ImportedKey(keyTag = tag)
            }
        }
    }

    private fun validateFields(): String? {
        if (name.isBlank()) return "Name is required"
        if (host.isBlank()) return "Host is required"
        if (port !in VALID_PORT_RANGE) return "Port must be between ${VALID_PORT_RANGE.first} and ${VALID_PORT_RANGE.last}"
        if (username.isBlank()) return "Username is required"

        when (authMethodSelection) {
            AuthMethodSelection.PASSWORD -> {
                if (password.isBlank() && existingProfile == null) {
                    return "Password is required"
                }
            }
            AuthMethodSelection.GENERATED_KEY,
            AuthMethodSelection.IMPORTED_KEY -> {
                if (currentKeyTag == null) return "SSH key is required"
            }
        }

        return null
    }

    companion object {
        private const val LOG_TAG = "ProfileEditorVM"
        const val NEW_PROFILE_ID = "new"
        private val VALID_PORT_RANGE = 1..65535
    }
}
