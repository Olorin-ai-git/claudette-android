package com.olorin.claudette.services.interfaces

data class SshKeyPairResult(
    val privateKeyTag: String,
    val publicKeyString: String
)

interface SshKeyServiceInterface {
    fun generateEd25519KeyPair(): SshKeyPairResult
    fun importPrivateKey(pemString: String): SshKeyPairResult
    fun publicKeyString(forKeyTag: String): String?
    fun deleteKey(keyTag: String)
}
