package com.olorin.claudette.services.impl

import com.olorin.claudette.models.RemoteFileEntry
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import timber.log.Timber
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

class RemoteFileBrowserService : RemoteFileBrowserServiceInterface {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var sshClient: SshClient? = null
    private var session: ClientSession? = null
    private var sftpClient: SftpClient? = null

    override suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String?,
        privateKeyData: ByteArray?
    ) = withContext(Dispatchers.IO) {
        val client = SshClient.setUpDefaultClient()
        client.start()
        sshClient = client

        val sess = client.connect(username, host, port)
            .verify(30, TimeUnit.SECONDS).session

        if (password != null) {
            sess.addPasswordIdentity(password)
        } else if (privateKeyData != null) {
            val keyPair = buildEd25519KeyPair(privateKeyData)
            sess.addPublicKeyIdentity(keyPair)
        }

        sess.auth().verify(30, TimeUnit.SECONDS)
        session = sess

        sftpClient = SftpClientFactory.instance().createSftpClient(sess)
        _isConnected.value = true
        Timber.i("SFTP connected to %s:%d", host, port)
    }

    override suspend fun listDirectory(atPath: String): List<RemoteFileEntry> =
        withContext(Dispatchers.IO) {
            val sftp = requireSftp()
            val entries = mutableListOf<RemoteFileEntry>()

            for (entry in sftp.readDir(atPath)) {
                val name = entry.filename
                if (name == "." || name == "..") continue

                val fullPath = if (atPath.endsWith("/")) {
                    "$atPath$name"
                } else {
                    "$atPath/$name"
                }

                val attrs = entry.attributes
                val isDirectory = attrs.isDirectory

                entries.add(
                    RemoteFileEntry(
                        name = name,
                        path = fullPath,
                        isDirectory = isDirectory,
                        size = attrs.size,
                        modifiedAt = attrs.modifyTime?.toMillis()
                    )
                )
            }

            // Sort: directories first, then alphabetical
            entries.sortWith(compareByDescending<RemoteFileEntry> { it.isDirectory }
                .thenBy { it.name.lowercase() })

            entries
        }

    override suspend fun getHomeDirectory(username: String): String =
        withContext(Dispatchers.IO) {
            val sftp = requireSftp()

            // Try /Users/username (macOS)
            try {
                val path = "/Users/$username"
                val attrs = sftp.stat(path)
                if (attrs.isDirectory) return@withContext path
            } catch (_: Exception) { }

            // Try /home/username (Linux)
            try {
                val path = "/home/$username"
                val attrs = sftp.stat(path)
                if (attrs.isDirectory) return@withContext path
            } catch (_: Exception) { }

            "/"
        }

    override suspend fun readFile(atPath: String): ByteArray =
        withContext(Dispatchers.IO) {
            val sftp = requireSftp()
            val baos = java.io.ByteArrayOutputStream()

            sftp.read(atPath).use { inputStream ->
                inputStream.copyTo(baos)
            }

            val data = baos.toByteArray()
            Timber.i("Read file: %s (%d bytes)", atPath, data.size)
            data
        }

    override suspend fun writeFile(data: ByteArray, atPath: String) =
        withContext(Dispatchers.IO) {
            val sftp = requireSftp()
            sftp.write(
                atPath,
                java.util.EnumSet.of(
                    SftpClient.OpenMode.Write,
                    SftpClient.OpenMode.Create,
                    SftpClient.OpenMode.Truncate
                )
            ).use { outputStream ->
                outputStream.write(data)
            }
            Timber.i("Wrote file: %s (%d bytes)", atPath, data.size)
        }

    override suspend fun fileExists(atPath: String): Boolean =
        withContext(Dispatchers.IO) {
            val sftp = sftpClient ?: return@withContext false
            try {
                sftp.stat(atPath)
                true
            } catch (_: Exception) {
                false
            }
        }

    override fun disconnect() {
        try { sftpClient?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { sshClient?.stop() } catch (_: Exception) {}
        sftpClient = null
        session = null
        sshClient = null
        _isConnected.value = false
        Timber.i("SFTP disconnected")
    }

    private fun requireSftp(): SftpClient {
        return sftpClient ?: error("Not connected to remote server")
    }

    private fun buildEd25519KeyPair(rawPrivateKey: ByteArray): KeyPair {
        val privateKeyParams = Ed25519PrivateKeyParameters(rawPrivateKey, 0)
        val publicKeyParams = privateKeyParams.generatePublicKey()

        val pkcs8Prefix = byteArrayOf(
            0x30, 0x2E, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06,
            0x03, 0x2B, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20
        )
        val pkcs8Bytes = pkcs8Prefix + rawPrivateKey

        val keyFactory = KeyFactory.getInstance("Ed25519")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes))

        val x509Prefix = byteArrayOf(
            0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65,
            0x70, 0x03, 0x21, 0x00
        )
        val x509Bytes = x509Prefix + publicKeyParams.encoded
        val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(x509Bytes))

        return KeyPair(publicKey, privateKey)
    }
}
