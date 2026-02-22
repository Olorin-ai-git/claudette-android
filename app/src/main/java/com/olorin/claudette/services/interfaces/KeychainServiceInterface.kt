package com.olorin.claudette.services.interfaces

interface KeychainServiceInterface {
    fun storePassword(password: String, profileId: String)
    fun retrievePassword(profileId: String): String?
    fun deletePassword(profileId: String)
    fun storePrivateKeyData(data: ByteArray, keyTag: String)
    fun retrievePrivateKeyData(keyTag: String): ByteArray?
    fun deletePrivateKeyData(keyTag: String)
}
