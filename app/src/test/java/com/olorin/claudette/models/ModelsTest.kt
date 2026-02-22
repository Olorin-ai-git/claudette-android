package com.olorin.claudette.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class ModelsTest {

    // --- AuthMethod sealed class ---

    @Test
    fun `AuthMethod Password displayName is Password`() {
        assertEquals("Password", AuthMethod.Password.displayName)
    }

    @Test
    fun `AuthMethod GeneratedKey displayName is Generated SSH Key`() {
        assertEquals("Generated SSH Key", AuthMethod.GeneratedKey("tag-1").displayName)
    }

    @Test
    fun `AuthMethod ImportedKey displayName is Imported SSH Key`() {
        assertEquals("Imported SSH Key", AuthMethod.ImportedKey("tag-2").displayName)
    }

    @Test
    fun `AuthMethod Password is not key based`() {
        assertFalse(AuthMethod.Password.isKeyBased)
    }

    @Test
    fun `AuthMethod GeneratedKey is key based`() {
        assertTrue(AuthMethod.GeneratedKey("tag-1").isKeyBased)
    }

    @Test
    fun `AuthMethod ImportedKey is key based`() {
        assertTrue(AuthMethod.ImportedKey("tag-2").isKeyBased)
    }

    @Test
    fun `AuthMethod Password keyTag is null`() {
        assertNull(AuthMethod.Password.keyTag)
    }

    @Test
    fun `AuthMethod GeneratedKey keyTag returns the tag`() {
        assertEquals("my-tag", AuthMethod.GeneratedKey("my-tag").keyTag)
    }

    @Test
    fun `AuthMethod ImportedKey keyTag returns the tag`() {
        assertEquals("imported-tag", AuthMethod.ImportedKey("imported-tag").keyTag)
    }

    // --- ServerProfile.toConnectionSettings ---

    @Test
    fun `toConnectionSettings creates correct settings`() {
        val profile = ServerProfile(
            id = "test-id",
            name = "My Server",
            host = "192.168.1.100",
            port = 22,
            username = "admin",
            authMethod = AuthMethod.Password,
            lastProjectPath = "/home/admin/projects"
        )

        val settings = profile.toConnectionSettings("/home/admin/work")

        assertEquals("192.168.1.100", settings.host)
        assertEquals(22, settings.port)
        assertEquals("admin", settings.username)
        assertEquals(AuthMethod.Password, settings.authMethod)
        assertEquals("/home/admin/work", settings.projectPath)
    }

    @Test
    fun `toConnectionSettings uses provided projectPath not lastProjectPath`() {
        val profile = ServerProfile(
            id = "id",
            name = "Server",
            host = "host",
            port = 2222,
            username = "user",
            authMethod = AuthMethod.GeneratedKey("key-1"),
            lastProjectPath = "/old/path"
        )

        val settings = profile.toConnectionSettings("/new/path")

        assertEquals("/new/path", settings.projectPath)
    }

    @Test
    fun `toConnectionSettings preserves auth method type`() {
        val keyAuth = AuthMethod.ImportedKey("imported-tag")
        val profile = ServerProfile(
            id = "id",
            name = "Server",
            host = "host",
            port = 22,
            username = "user",
            authMethod = keyAuth
        )

        val settings = profile.toConnectionSettings("/path")

        assertTrue(settings.authMethod is AuthMethod.ImportedKey)
        assertEquals("imported-tag", settings.authMethod.keyTag)
    }

    @Test
    fun `ServerProfile create generates UUID id`() {
        val profile = ServerProfile.create(
            name = "Test",
            host = "localhost",
            port = 22,
            username = "user",
            authMethod = AuthMethod.Password
        )

        assertNotNull(profile.id)
        assertTrue(
            "ID should be a valid UUID",
            profile.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        )
    }

    @Test
    fun `ServerProfile create generates unique IDs`() {
        val profile1 = ServerProfile.create(
            name = "Test1",
            host = "localhost",
            port = 22,
            username = "user",
            authMethod = AuthMethod.Password
        )
        val profile2 = ServerProfile.create(
            name = "Test2",
            host = "localhost",
            port = 22,
            username = "user",
            authMethod = AuthMethod.Password
        )

        assertNotEquals(profile1.id, profile2.id)
    }

    // --- KnownHost.identifier ---

    @Test
    fun `KnownHost identifier generates host colon port format`() {
        assertEquals("example.com:22", KnownHost.identifier("example.com", 22))
    }

    @Test
    fun `KnownHost identifier with non-standard port`() {
        assertEquals("10.0.0.1:2222", KnownHost.identifier("10.0.0.1", 2222))
    }

    @Test
    fun `KnownHost identifier with IPv6 address`() {
        assertEquals("::1:22", KnownHost.identifier("::1", 22))
    }

    // --- KnownHost.fingerprintSHA256 ---

    @Test
    fun `KnownHost fingerprintSHA256 generates SHA256 fingerprint`() {
        val testKeyData = "test-public-key-data".toByteArray(Charsets.UTF_8)
        val knownHost = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = testKeyData,
            keyType = "ssh-ed25519"
        )

        val fingerprint = knownHost.fingerprintSHA256

        assertTrue(
            "Fingerprint should start with SHA256:",
            fingerprint.startsWith("SHA256:")
        )

        // Verify the hash is correct
        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(testKeyData)
        val expectedBase64 = Base64.getEncoder().encodeToString(expectedDigest)
        assertEquals("SHA256:$expectedBase64", fingerprint)
    }

    @Test
    fun `KnownHost fingerprintSHA256 is deterministic`() {
        val keyData = ByteArray(32) { it.toByte() }
        val host = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = keyData,
            keyType = "ssh-rsa"
        )

        val fingerprint1 = host.fingerprintSHA256
        val fingerprint2 = host.fingerprintSHA256

        assertEquals(fingerprint1, fingerprint2)
    }

    @Test
    fun `KnownHost fingerprintSHA256 differs for different keys`() {
        val host1 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = ByteArray(32) { 0x01 },
            keyType = "ssh-ed25519"
        )
        val host2 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = ByteArray(32) { 0x02 },
            keyType = "ssh-ed25519"
        )

        assertNotEquals(host1.fingerprintSHA256, host2.fingerprintSHA256)
    }

    @Test
    fun `KnownHost id returns hostIdentifier`() {
        val host = KnownHost(
            hostIdentifier = "myhost:8022",
            publicKeyData = byteArrayOf(1, 2, 3),
            keyType = "ssh-rsa"
        )

        assertEquals("myhost:8022", host.id)
    }

    @Test
    fun `KnownHost equality is based on identifier keyData and keyType`() {
        val keyData = byteArrayOf(1, 2, 3, 4)
        val host1 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = keyData.copyOf(),
            keyType = "ssh-ed25519"
        )
        val host2 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = keyData.copyOf(),
            keyType = "ssh-ed25519"
        )

        assertEquals(host1, host2)
        assertEquals(host1.hashCode(), host2.hashCode())
    }

    @Test
    fun `KnownHost inequality when keyData differs`() {
        val host1 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = byteArrayOf(1, 2, 3),
            keyType = "ssh-ed25519"
        )
        val host2 = KnownHost(
            hostIdentifier = "host:22",
            publicKeyData = byteArrayOf(4, 5, 6),
            keyType = "ssh-ed25519"
        )

        assertNotEquals(host1, host2)
    }

    // --- SnippetCategory ---

    @Test
    fun `SnippetCategory CLAUDE_COMMANDS has correct displayName`() {
        assertEquals("Claude Commands", SnippetCategory.CLAUDE_COMMANDS.displayName)
    }

    @Test
    fun `SnippetCategory REFACTORING has correct displayName`() {
        assertEquals("Refactoring", SnippetCategory.REFACTORING.displayName)
    }

    @Test
    fun `SnippetCategory DEBUGGING has correct displayName`() {
        assertEquals("Debugging", SnippetCategory.DEBUGGING.displayName)
    }

    @Test
    fun `SnippetCategory GIT has correct displayName`() {
        assertEquals("Git", SnippetCategory.GIT.displayName)
    }

    @Test
    fun `SnippetCategory CUSTOM has correct displayName`() {
        assertEquals("Custom", SnippetCategory.CUSTOM.displayName)
    }

    @Test
    fun `SnippetCategory has five values`() {
        assertEquals(5, SnippetCategory.entries.size)
    }

    @Test
    fun `SnippetCategory each value has a non-empty iconName`() {
        for (category in SnippetCategory.entries) {
            assertTrue(
                "Category ${category.name} should have a non-empty iconName",
                category.iconName.isNotEmpty()
            )
        }
    }

    // --- ConnectionState ---

    @Test
    fun `ConnectionState Disconnected is distinct`() {
        val state: ConnectionState = ConnectionState.Disconnected
        assertTrue(state is ConnectionState.Disconnected)
        assertFalse(state is ConnectionState.Connected)
    }

    @Test
    fun `ConnectionState Connecting is distinct`() {
        val state: ConnectionState = ConnectionState.Connecting
        assertTrue(state is ConnectionState.Connecting)
        assertFalse(state is ConnectionState.Disconnected)
    }

    @Test
    fun `ConnectionState Connected is distinct`() {
        val state: ConnectionState = ConnectionState.Connected
        assertTrue(state is ConnectionState.Connected)
        assertFalse(state is ConnectionState.Connecting)
    }

    @Test
    fun `ConnectionState Reconnecting carries attempt info`() {
        val state = ConnectionState.Reconnecting(attempt = 3, maxAttempts = 5)
        assertTrue(state is ConnectionState.Reconnecting)
        assertEquals(3, state.attempt)
        assertEquals(5, state.maxAttempts)
    }

    @Test
    fun `ConnectionState Failed carries error description`() {
        val state = ConnectionState.Failed("Connection refused")
        assertTrue(state is ConnectionState.Failed)
        assertEquals("Connection refused", state.errorDescription)
    }

    @Test
    fun `ConnectionState variants are all distinct types`() {
        val states = listOf(
            ConnectionState.Disconnected,
            ConnectionState.Connecting,
            ConnectionState.Connected,
            ConnectionState.Reconnecting(1, 3),
            ConnectionState.Failed("error")
        )

        // Each pair should be different types
        for (i in states.indices) {
            for (j in states.indices) {
                if (i != j) {
                    assertNotEquals(
                        "States at $i and $j should be different",
                        states[i]::class,
                        states[j]::class
                    )
                }
            }
        }
    }

    // --- NetworkStatus ---

    @Test
    fun `NetworkStatus Unknown is not reachable`() {
        val status: NetworkStatus = NetworkStatus.Unknown
        assertFalse(status.isReachable)
    }

    @Test
    fun `NetworkStatus Reachable is reachable`() {
        val status: NetworkStatus = NetworkStatus.Reachable(latencyMs = 25.0)
        assertTrue(status.isReachable)
    }

    @Test
    fun `NetworkStatus Degraded is reachable`() {
        val status: NetworkStatus = NetworkStatus.Degraded(latencyMs = 500.0)
        assertTrue(status.isReachable)
    }

    @Test
    fun `NetworkStatus Unreachable is not reachable`() {
        val status: NetworkStatus = NetworkStatus.Unreachable
        assertFalse(status.isReachable)
    }

    @Test
    fun `NetworkStatus Reachable latencyMs returns value`() {
        val status = NetworkStatus.Reachable(latencyMs = 42.5)
        assertEquals(42.5, status.latencyMs!!, 0.001)
    }

    @Test
    fun `NetworkStatus Degraded latencyMs returns value`() {
        val status = NetworkStatus.Degraded(latencyMs = 350.0)
        assertEquals(350.0, status.latencyMs!!, 0.001)
    }

    @Test
    fun `NetworkStatus Unknown latencyMs is null`() {
        assertNull(NetworkStatus.Unknown.latencyMs)
    }

    @Test
    fun `NetworkStatus Unreachable latencyMs is null`() {
        assertNull(NetworkStatus.Unreachable.latencyMs)
    }
}
