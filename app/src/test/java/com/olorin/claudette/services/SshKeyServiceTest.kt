package com.olorin.claudette.services

import com.olorin.claudette.services.impl.SshKeyService
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SshKeyServiceTest {

    private lateinit var keychainService: KeychainServiceInterface
    private lateinit var sshKeyService: SshKeyService

    @Before
    fun setUp() {
        keychainService = mockk(relaxed = true)
        sshKeyService = SshKeyService(keychainService)
    }

    // --- generateEd25519KeyPair ---

    @Test
    fun `generateEd25519KeyPair returns valid key tag and public key string`() {
        every { keychainService.storePrivateKeyData(any(), any()) } just Runs

        val result = sshKeyService.generateEd25519KeyPair()

        assertNotNull(result.privateKeyTag)
        assertTrue(
            "Key tag should be a valid UUID format",
            result.privateKeyTag.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        )
        assertNotNull(result.publicKeyString)
    }

    @Test
    fun `generateEd25519KeyPair public key starts with ssh-ed25519`() {
        every { keychainService.storePrivateKeyData(any(), any()) } just Runs

        val result = sshKeyService.generateEd25519KeyPair()

        assertTrue(
            "Public key should start with ssh-ed25519",
            result.publicKeyString.startsWith("ssh-ed25519 ")
        )
    }

    @Test
    fun `generateEd25519KeyPair public key has two space-separated parts`() {
        every { keychainService.storePrivateKeyData(any(), any()) } just Runs

        val result = sshKeyService.generateEd25519KeyPair()

        val parts = result.publicKeyString.split(" ")
        assertEquals(
            "Public key should have exactly 2 parts (type and base64 data)",
            2,
            parts.size
        )
        assertEquals("ssh-ed25519", parts[0])
        assertTrue(
            "Base64 data should be non-empty",
            parts[1].isNotEmpty()
        )
    }

    @Test
    fun `generateEd25519KeyPair stores private key in keychain`() {
        val storedData = slot<ByteArray>()
        val storedTag = slot<String>()
        every { keychainService.storePrivateKeyData(capture(storedData), capture(storedTag)) } just Runs

        val result = sshKeyService.generateEd25519KeyPair()

        verify(exactly = 1) { keychainService.storePrivateKeyData(any(), any()) }
        assertEquals(result.privateKeyTag, storedTag.captured)
        assertEquals(
            "Ed25519 private key should be 32 bytes",
            32,
            storedData.captured.size
        )
    }

    @Test
    fun `generateEd25519KeyPair produces unique key pairs`() {
        every { keychainService.storePrivateKeyData(any(), any()) } just Runs

        val result1 = sshKeyService.generateEd25519KeyPair()
        val result2 = sshKeyService.generateEd25519KeyPair()

        assertTrue(
            "Each generation should produce a unique key tag",
            result1.privateKeyTag != result2.privateKeyTag
        )
        assertTrue(
            "Each generation should produce a unique public key",
            result1.publicKeyString != result2.publicKeyString
        )
    }

    // --- publicKeyString ---

    @Test
    fun `publicKeyString returns correct format for stored key`() {
        // First generate a key to capture the raw private key data
        val storedData = slot<ByteArray>()
        every { keychainService.storePrivateKeyData(capture(storedData), any()) } just Runs

        val generated = sshKeyService.generateEd25519KeyPair()

        // Now set up the mock to return the stored key data
        every { keychainService.retrievePrivateKeyData(generated.privateKeyTag) } returns storedData.captured

        val publicKey = sshKeyService.publicKeyString(generated.privateKeyTag)

        assertNotNull(publicKey)
        assertTrue(
            "Public key from stored data should start with ssh-ed25519",
            publicKey!!.startsWith("ssh-ed25519 ")
        )
        assertEquals(
            "Public key derived from stored private key should match original",
            generated.publicKeyString,
            publicKey
        )
    }

    @Test
    fun `publicKeyString returns null when key not found`() {
        every { keychainService.retrievePrivateKeyData("nonexistent-tag") } returns null

        val result = sshKeyService.publicKeyString("nonexistent-tag")

        assertNull(result)
    }

    @Test
    fun `publicKeyString returns null when stored data is wrong size`() {
        every { keychainService.retrievePrivateKeyData("bad-key") } returns ByteArray(16)

        val result = sshKeyService.publicKeyString("bad-key")

        assertNull(result)
    }

    // --- deleteKey ---

    @Test
    fun `deleteKey delegates to keychain`() {
        every { keychainService.deletePrivateKeyData("test-tag") } just Runs

        sshKeyService.deleteKey("test-tag")

        verify(exactly = 1) { keychainService.deletePrivateKeyData("test-tag") }
    }

    @Test
    fun `deleteKey passes correct tag to keychain`() {
        val tagSlot = slot<String>()
        every { keychainService.deletePrivateKeyData(capture(tagSlot)) } just Runs

        val keyTag = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        sshKeyService.deleteKey(keyTag)

        assertEquals(keyTag, tagSlot.captured)
    }
}
