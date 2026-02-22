package com.olorin.claudette.services.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLanService {

    suspend fun wake(
        macAddress: String,
        broadcastAddress: String = DEFAULT_BROADCAST_ADDRESS
    ) = withContext(Dispatchers.IO) {
        val macBytes = parseMacAddress(macAddress)
        val magicPacket = buildMagicPacket(macBytes)

        val address = InetAddress.getByName(broadcastAddress)
        val packet = DatagramPacket(magicPacket, magicPacket.size, address, WOL_PORT)

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(packet)
        }

        Timber.i("Sent Wake-on-LAN packet to %s via %s", macAddress, broadcastAddress)
    }

    private fun parseMacAddress(address: String): ByteArray {
        val cleaned = address
            .replace(":", "")
            .replace("-", "")

        require(cleaned.length == MAC_HEX_LENGTH) {
            "Invalid MAC address format. Use XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX"
        }

        return ByteArray(MAC_BYTE_LENGTH) { i ->
            val hexPair = cleaned.substring(i * 2, i * 2 + 2)
            hexPair.toIntOrNull(16)?.toByte()
                ?: throw IllegalArgumentException(
                    "Invalid MAC address format. Use XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX"
                )
        }
    }

    private fun buildMagicPacket(macBytes: ByteArray): ByteArray {
        // Magic packet: 6 bytes of 0xFF followed by MAC address repeated 16 times
        val packet = ByteArray(HEADER_LENGTH + MAC_BYTE_LENGTH * MAC_REPEAT_COUNT)

        // Fill header with 0xFF
        for (i in 0 until HEADER_LENGTH) {
            packet[i] = 0xFF.toByte()
        }

        // Repeat MAC address 16 times
        for (i in 0 until MAC_REPEAT_COUNT) {
            macBytes.copyInto(packet, HEADER_LENGTH + i * MAC_BYTE_LENGTH)
        }

        return packet
    }

    companion object {
        private const val WOL_PORT = 9
        private const val DEFAULT_BROADCAST_ADDRESS = "255.255.255.255"
        private const val MAC_HEX_LENGTH = 12
        private const val MAC_BYTE_LENGTH = 6
        private const val HEADER_LENGTH = 6
        private const val MAC_REPEAT_COUNT = 16
    }
}
