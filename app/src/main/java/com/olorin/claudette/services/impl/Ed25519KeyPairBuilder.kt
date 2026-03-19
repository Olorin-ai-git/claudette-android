package com.olorin.claudette.services.impl

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec

object Ed25519KeyPairBuilder {

    fun build(rawPrivateKey: ByteArray): KeyPair {
        val privateKeyParams = Ed25519PrivateKeyParameters(rawPrivateKey, 0)
        val publicKeyParams = privateKeyParams.generatePublicKey()

        // Build PKCS#8 encoded private key for Java KeyPair
        val pkcs8Prefix = byteArrayOf(
            0x30, 0x2E, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06,
            0x03, 0x2B, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20
        )
        val pkcs8Bytes = pkcs8Prefix + rawPrivateKey

        val keyFactory = KeyFactory.getInstance("Ed25519")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes))

        // Build X.509 encoded public key
        val x509Prefix = byteArrayOf(
            0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65,
            0x70, 0x03, 0x21, 0x00
        )
        val x509Bytes = x509Prefix + publicKeyParams.encoded
        val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(x509Bytes))

        return KeyPair(publicKey, privateKey)
    }
}
