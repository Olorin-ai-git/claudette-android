package com.olorin.claudette.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.LoggerFactory
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.ProfileStoreInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileStore: ProfileStoreInterface,
    private val keychainService: KeychainServiceInterface
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val profiles: StateFlow<List<ServerProfile>> = _profiles.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            val loaded = profileStore.loadProfiles()
                .sortedByDescending { it.lastConnectedAt ?: 0L }
            _profiles.value = loaded
            LoggerFactory.d(LOG_TAG, "Loaded ${loaded.size} profiles")
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val profile = _profiles.value.find { it.id == profileId } ?: return@launch
            profileStore.deleteProfile(profileId)

            // Clean up associated keychain entries
            keychainService.deletePassword(profileId)
            profile.authMethod.keyTag?.let { tag ->
                keychainService.deletePrivateKeyData(tag)
            }

            LoggerFactory.i(LOG_TAG, "Deleted profile: ${profile.name}")
            loadProfiles()
        }
    }

    fun sendWakeOnLan(macAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val macBytes = parseMacAddress(macAddress)
                val magicPacket = buildWolMagicPacket(macBytes)

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val broadcastAddress = InetAddress.getByName(WOL_BROADCAST_ADDRESS)
                    val packet = DatagramPacket(
                        magicPacket,
                        magicPacket.size,
                        broadcastAddress,
                        WOL_PORT
                    )
                    socket.send(packet)
                }
                LoggerFactory.i(LOG_TAG, "Wake-on-LAN sent to $macAddress")
            } catch (e: Exception) {
                LoggerFactory.e(LOG_TAG, "Wake-on-LAN failed for $macAddress", e)
            }
        }
    }

    private fun parseMacAddress(mac: String): ByteArray {
        val parts = mac.split(":", "-")
        require(parts.size == MAC_BYTE_COUNT) { "Invalid MAC address format: $mac" }
        return parts.map { it.toInt(HEX_RADIX).toByte() }.toByteArray()
    }

    private fun buildWolMagicPacket(macBytes: ByteArray): ByteArray {
        val packet = ByteArray(WOL_HEADER_SIZE + MAC_BYTE_COUNT * WOL_MAC_REPETITIONS)

        // 6 bytes of 0xFF header
        for (i in 0 until WOL_HEADER_SIZE) {
            packet[i] = 0xFF.toByte()
        }

        // MAC address repeated 16 times
        for (i in 0 until WOL_MAC_REPETITIONS) {
            System.arraycopy(macBytes, 0, packet, WOL_HEADER_SIZE + i * MAC_BYTE_COUNT, MAC_BYTE_COUNT)
        }

        return packet
    }

    companion object {
        private const val LOG_TAG = "ProfileListVM"
        private const val WOL_BROADCAST_ADDRESS = "255.255.255.255"
        private const val WOL_PORT = 9
        private const val WOL_HEADER_SIZE = 6
        private const val WOL_MAC_REPETITIONS = 16
        private const val MAC_BYTE_COUNT = 6
        private const val HEX_RADIX = 16
    }
}
