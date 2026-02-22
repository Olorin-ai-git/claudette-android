package com.olorin.claudette.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AuthMethod {

    @Serializable
    @SerialName("password")
    data object Password : AuthMethod()

    @Serializable
    @SerialName("generatedKey")
    data class GeneratedKey(override val keyTag: String) : AuthMethod()

    @Serializable
    @SerialName("importedKey")
    data class ImportedKey(override val keyTag: String) : AuthMethod()

    val displayName: String
        get() = when (this) {
            is Password -> "Password"
            is GeneratedKey -> "Generated SSH Key"
            is ImportedKey -> "Imported SSH Key"
        }

    val isKeyBased: Boolean
        get() = when (this) {
            is Password -> false
            is GeneratedKey, is ImportedKey -> true
        }

    open val keyTag: String?
        get() = null
}
