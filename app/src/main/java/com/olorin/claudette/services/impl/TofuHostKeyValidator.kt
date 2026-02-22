package com.olorin.claudette.services.impl

import com.olorin.claudette.models.HostKeyVerificationResult
import com.olorin.claudette.models.KnownHost
import com.olorin.claudette.services.interfaces.HostKeyStoreInterface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import timber.log.Timber
import java.net.SocketAddress
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64

interface HostKeyVerificationDelegate {
    suspend fun verifyHostKey(
        result: HostKeyVerificationResult,
        fingerprint: String,
        hostIdentifier: String
    ): Boolean
}

class TofuHostKeyValidator(
    private val hostKeyStore: HostKeyStoreInterface
) : ServerKeyVerifier {

    var host: String = ""
    var port: Int = 22
    var delegate: HostKeyVerificationDelegate? = null

    override fun verifyServerKey(
        clientSession: ClientSession,
        remoteAddress: SocketAddress,
        serverKey: PublicKey
    ): Boolean {
        val hostIdentifier = KnownHost.identifier(host, port)
        val keyData = serverKey.encoded
        val keyType = serverKey.algorithm

        val digest = MessageDigest.getInstance("SHA-256").digest(keyData)
        val fingerprint = "SHA256:${Base64.getEncoder().encodeToString(digest)}"

        Timber.i("Validating host key for %s: %s", hostIdentifier, fingerprint)

        val existingHost = hostKeyStore.knownHost(hostIdentifier)

        if (existingHost != null) {
            if (existingHost.publicKeyData.contentEquals(keyData)) {
                // Key matches — trusted
                Timber.i("Host key matches known key for %s", hostIdentifier)
                existingHost.lastSeenAt = System.currentTimeMillis()
                hostKeyStore.storeHost(existingHost)
                return true
            } else {
                // Key changed — dangerous
                val previousFingerprint = existingHost.fingerprintSHA256
                Timber.w("HOST KEY CHANGED for %s", hostIdentifier)

                val currentDelegate = delegate ?: run {
                    Timber.e("No delegate to handle key change verification")
                    return false
                }

                val result = HostKeyVerificationResult.KeyChanged(
                    previousFingerprint = previousFingerprint,
                    newFingerprint = fingerprint
                )

                val accepted = runBlockingVerification(currentDelegate, result, fingerprint, hostIdentifier)

                if (accepted) {
                    val newHost = KnownHost(
                        hostIdentifier = hostIdentifier,
                        publicKeyData = keyData,
                        keyType = keyType
                    )
                    hostKeyStore.storeHost(newHost)
                    Timber.i("User accepted new key for %s", hostIdentifier)
                }

                return accepted
            }
        } else {
            // New host — ask user
            Timber.i("New host encountered: %s", hostIdentifier)

            val currentDelegate = delegate ?: run {
                Timber.e("No delegate to handle new host verification")
                return false
            }

            val accepted = runBlockingVerification(
                currentDelegate,
                HostKeyVerificationResult.NewHost,
                fingerprint,
                hostIdentifier
            )

            if (accepted) {
                val newHost = KnownHost(
                    hostIdentifier = hostIdentifier,
                    publicKeyData = keyData,
                    keyType = keyType
                )
                hostKeyStore.storeHost(newHost)
                Timber.i("User trusted new host: %s", hostIdentifier)
            }

            return accepted
        }
    }

    fun configure(host: String, port: Int): TofuHostKeyValidator {
        this.host = host
        this.port = port
        return this
    }

    /**
     * Bridge from the synchronous ServerKeyVerifier callback to the
     * async HostKeyVerificationDelegate. Uses a CompletableDeferred
     * that blocks the SSH thread until the UI responds.
     */
    private fun runBlockingVerification(
        delegate: HostKeyVerificationDelegate,
        result: HostKeyVerificationResult,
        fingerprint: String,
        hostIdentifier: String
    ): Boolean {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            delegate.verifyHostKey(result, fingerprint, hostIdentifier)
        }
    }
}
