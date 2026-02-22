package com.olorin.claudette.services

import com.olorin.claudette.services.impl.WakeOnLanService
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class WakeOnLanServiceTest {

    private lateinit var service: WakeOnLanService

    private lateinit var parseMacAddress: Method
    private lateinit var buildMagicPacket: Method

    @Before
    fun setUp() {
        service = WakeOnLanService()

        // Access private methods via reflection for unit testing
        parseMacAddress = WakeOnLanService::class.java.getDeclaredMethod("parseMacAddress", String::class.java)
        parseMacAddress.isAccessible = true

        buildMagicPacket = WakeOnLanService::class.java.getDeclaredMethod("buildMagicPacket", ByteArray::class.java)
        buildMagicPacket.isAccessible = true
    }

    // --- parseMacAddress with colons ---

    @Test
    fun `valid MAC with colons parses correctly`() {
        val result = invokeParseMac("AA:BB:CC:DD:EE:FF")

        assertEquals(6, result.size)
        assertEquals(0xAA.toByte(), result[0])
        assertEquals(0xBB.toByte(), result[1])
        assertEquals(0xCC.toByte(), result[2])
        assertEquals(0xDD.toByte(), result[3])
        assertEquals(0xEE.toByte(), result[4])
        assertEquals(0xFF.toByte(), result[5])
    }

    @Test
    fun `valid MAC with colons lowercase parses correctly`() {
        val result = invokeParseMac("aa:bb:cc:dd:ee:ff")

        assertEquals(6, result.size)
        assertEquals(0xAA.toByte(), result[0])
        assertEquals(0xFF.toByte(), result[5])
    }

    // --- parseMacAddress with dashes ---

    @Test
    fun `valid MAC with dashes parses correctly`() {
        val result = invokeParseMac("11-22-33-44-55-66")

        assertEquals(6, result.size)
        assertEquals(0x11.toByte(), result[0])
        assertEquals(0x22.toByte(), result[1])
        assertEquals(0x33.toByte(), result[2])
        assertEquals(0x44.toByte(), result[3])
        assertEquals(0x55.toByte(), result[4])
        assertEquals(0x66.toByte(), result[5])
    }

    @Test
    fun `valid MAC with mixed case and dashes parses correctly`() {
        val result = invokeParseMac("aA-Bb-cC-Dd-eE-fF")

        assertEquals(6, result.size)
        assertEquals(0xAA.toByte(), result[0])
    }

    // --- Invalid MACs ---

    @Test(expected = IllegalArgumentException::class)
    fun `MAC too short throws exception`() {
        invokeParseMac("AA:BB:CC")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `MAC too long throws exception`() {
        invokeParseMac("AA:BB:CC:DD:EE:FF:00")
    }

    @Test
    fun `MAC with invalid hex characters throws exception`() {
        try {
            invokeParseMac("GG:HH:II:JJ:KK:LL")
            fail("Should have thrown IllegalArgumentException")
        } catch (e: Exception) {
            // Reflection wraps exceptions in InvocationTargetException
            val cause = e.cause ?: e
            assertTrue(
                "Expected IllegalArgumentException but got ${cause::class.simpleName}",
                cause is IllegalArgumentException
            )
        }
    }

    @Test(expected = Exception::class)
    fun `empty MAC throws exception`() {
        invokeParseMac("")
    }

    // --- buildMagicPacket ---

    @Test
    fun `magic packet is 102 bytes`() {
        val macBytes = invokeParseMac("AA:BB:CC:DD:EE:FF")
        val packet = invokeBuildMagicPacket(macBytes)

        assertEquals(
            "Magic packet must be 6 (header) + 16*6 (MAC repeated) = 102 bytes",
            102,
            packet.size
        )
    }

    @Test
    fun `magic packet header is six 0xFF bytes`() {
        val macBytes = invokeParseMac("01:02:03:04:05:06")
        val packet = invokeBuildMagicPacket(macBytes)

        for (i in 0 until 6) {
            assertEquals(
                "Header byte $i should be 0xFF",
                0xFF.toByte(),
                packet[i]
            )
        }
    }

    @Test
    fun `magic packet contains MAC address repeated 16 times`() {
        val macBytes = invokeParseMac("01:02:03:04:05:06")
        val packet = invokeBuildMagicPacket(macBytes)

        for (repetition in 0 until 16) {
            val offset = 6 + repetition * 6
            for (b in 0 until 6) {
                assertEquals(
                    "MAC byte $b at repetition $repetition should match",
                    macBytes[b],
                    packet[offset + b]
                )
            }
        }
    }

    @Test
    fun `magic packet with all-zeros MAC`() {
        val macBytes = invokeParseMac("00:00:00:00:00:00")
        val packet = invokeBuildMagicPacket(macBytes)

        assertEquals(102, packet.size)
        // First 6 bytes should be 0xFF
        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i])
        }
        // Remaining 96 bytes should be 0x00
        for (i in 6 until 102) {
            assertEquals(0x00.toByte(), packet[i])
        }
    }

    @Test
    fun `magic packet with broadcast MAC`() {
        val macBytes = invokeParseMac("FF:FF:FF:FF:FF:FF")
        val packet = invokeBuildMagicPacket(macBytes)

        assertEquals(102, packet.size)
        // All 102 bytes should be 0xFF
        for (i in 0 until 102) {
            assertEquals(0xFF.toByte(), packet[i])
        }
    }

    // --- Helpers ---

    private fun invokeParseMac(address: String): ByteArray {
        try {
            return parseMacAddress.invoke(service, address) as ByteArray
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    private fun invokeBuildMagicPacket(macBytes: ByteArray): ByteArray {
        try {
            return buildMagicPacket.invoke(service, macBytes) as ByteArray
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }
}
