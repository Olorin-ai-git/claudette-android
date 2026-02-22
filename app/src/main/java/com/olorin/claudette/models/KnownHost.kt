package com.olorin.claudette.models

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.Base64

@Serializable
data class KnownHost(
    val hostIdentifier: String,
    val publicKeyData: ByteArray,
    val keyType: String,
    val firstSeenAt: Long = System.currentTimeMillis(),
    var lastSeenAt: Long = System.currentTimeMillis()
) {
    val id: String get() = hostIdentifier

    val fingerprintSHA256: String
        get() {
            val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyData)
            val base64 = Base64.getEncoder().encodeToString(digest)
            return "SHA256:$base64"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KnownHost) return false
        return hostIdentifier == other.hostIdentifier &&
            publicKeyData.contentEquals(other.publicKeyData) &&
            keyType == other.keyType
    }

    override fun hashCode(): Int {
        var result = hostIdentifier.hashCode()
        result = 31 * result + publicKeyData.contentHashCode()
        result = 31 * result + keyType.hashCode()
        return result
    }

    companion object {
        fun identifier(host: String, port: Int): String = "$host:$port"
    }
}
