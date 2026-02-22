package com.olorin.claudette.services.impl

import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.SshKeyPairResult
import com.olorin.claudette.services.interfaces.SshKeyServiceInterface
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class SshKeyService(
    private val keychainService: KeychainServiceInterface
) : SshKeyServiceInterface {

    override fun generateEd25519KeyPair(): SshKeyPairResult {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()

        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters

        val keyTag = UUID.randomUUID().toString()
        val rawPrivateKey = privateKey.encoded

        keychainService.storePrivateKeyData(rawPrivateKey, keyTag)

        val publicKeyString = formatEd25519PublicKey(publicKey.encoded)
        Timber.i("Generated Ed25519 key pair with tag: %s", keyTag)

        return SshKeyPairResult(privateKeyTag = keyTag, publicKeyString = publicKeyString)
    }

    override fun importPrivateKey(pemString: String): SshKeyPairResult {
        val trimmed = pemString.trim()

        require(trimmed.contains("PRIVATE KEY")) { "Invalid PEM format" }

        val base64Lines = trimmed.lines()
            .filter { !it.startsWith("-----") && it.isNotBlank() }
        val base64String = base64Lines.joinToString("")
        val derData = Base64.getDecoder().decode(base64String)

        // Try PKCS#8 Ed25519
        return tryImportPkcs8Ed25519(derData)
            ?: tryImportRawEd25519(derData)
            ?: error("Unsupported key type — only Ed25519 is supported")
    }

    override fun publicKeyString(forKeyTag: String): String? {
        val keyData = keychainService.retrievePrivateKeyData(forKeyTag) ?: return null
        if (keyData.size != 32) return null

        return try {
            val privateKey = Ed25519PrivateKeyParameters(keyData, 0)
            val publicKey = privateKey.generatePublicKey()
            formatEd25519PublicKey(publicKey.encoded)
        } catch (e: Exception) {
            Timber.e(e, "Failed to derive public key for tag: %s", forKeyTag)
            null
        }
    }

    override fun deleteKey(keyTag: String) {
        keychainService.deletePrivateKeyData(keyTag)
        Timber.i("Deleted key with tag: %s", keyTag)
    }

    private fun tryImportPkcs8Ed25519(derData: ByteArray): SshKeyPairResult? {
        return try {
            val asn1 = ASN1Sequence.getInstance(derData)
            val privateKeyInfo = PrivateKeyInfo.getInstance(asn1)
            val algorithmOid = privateKeyInfo.privateKeyAlgorithm.algorithm.id

            // Ed25519 OID: 1.3.101.112
            if (algorithmOid != "1.3.101.112") return null

            val rawKey = privateKeyInfo.parsePrivateKey().toASN1Primitive().encoded
            // The raw key might be wrapped in an OCTET STRING; extract last 32 bytes
            val keyBytes = if (rawKey.size > 32) {
                rawKey.takeLast(32).toByteArray()
            } else {
                rawKey
            }

            importEd25519Raw(keyBytes)
        } catch (e: Exception) {
            Timber.d(e, "Not a valid PKCS#8 Ed25519 key")
            null
        }
    }

    private fun tryImportRawEd25519(data: ByteArray): SshKeyPairResult? {
        if (data.size != 32) return null
        return try {
            importEd25519Raw(data)
        } catch (e: Exception) {
            null
        }
    }

    private fun importEd25519Raw(keyData: ByteArray): SshKeyPairResult {
        val privateKey = Ed25519PrivateKeyParameters(keyData, 0)
        val publicKey = privateKey.generatePublicKey()
        val keyTag = UUID.randomUUID().toString()

        keychainService.storePrivateKeyData(keyData, keyTag)

        val publicKeyString = formatEd25519PublicKey(publicKey.encoded)
        Timber.i("Imported Ed25519 key with tag: %s", keyTag)

        return SshKeyPairResult(privateKeyTag = keyTag, publicKeyString = publicKeyString)
    }

    private fun formatEd25519PublicKey(rawPublicKey: ByteArray): String {
        val keyType = "ssh-ed25519"
        val keyTypeBytes = keyType.toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Write key type with 4-byte big-endian length prefix
        dos.writeInt(keyTypeBytes.size)
        dos.write(keyTypeBytes)

        // Write public key with 4-byte big-endian length prefix
        dos.writeInt(rawPublicKey.size)
        dos.write(rawPublicKey)

        dos.flush()

        val wireFormat = baos.toByteArray()
        return "$keyType ${Base64.getEncoder().encodeToString(wireFormat)}"
    }
}
