package com.olorin.claudette.services.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import timber.log.Timber
import java.util.Base64

class KeychainService(
    private val serviceName: String,
    context: Context? = null
) : KeychainServiceInterface {

    private var encryptedPrefs: SharedPreferences? = null

    init {
        if (context != null) {
            initPrefs(context)
        }
    }

    fun initPrefs(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            serviceName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        Timber.i("KeychainService initialized with service: %s", serviceName)
    }

    private fun requirePrefs(): SharedPreferences {
        return encryptedPrefs ?: error("KeychainService not initialized with context")
    }

    override fun storePassword(password: String, profileId: String) {
        requirePrefs().edit()
            .putString("profile_$profileId", password)
            .apply()
        Timber.i("Stored password for profile: %s", profileId)
    }

    override fun retrievePassword(profileId: String): String? {
        return requirePrefs().getString("profile_$profileId", null)
    }

    override fun deletePassword(profileId: String) {
        requirePrefs().edit()
            .remove("profile_$profileId")
            .apply()
        Timber.i("Deleted password for profile: %s", profileId)
    }

    override fun storePrivateKeyData(data: ByteArray, keyTag: String) {
        val encoded = Base64.getEncoder().encodeToString(data)
        requirePrefs().edit()
            .putString("key_$keyTag", encoded)
            .apply()
        Timber.i("Stored private key with tag: %s", keyTag)
    }

    override fun retrievePrivateKeyData(keyTag: String): ByteArray? {
        val encoded = requirePrefs().getString("key_$keyTag", null) ?: return null
        return try {
            Base64.getDecoder().decode(encoded)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Failed to decode private key data for tag: %s", keyTag)
            null
        }
    }

    override fun deletePrivateKeyData(keyTag: String) {
        requirePrefs().edit()
            .remove("key_$keyTag")
            .apply()
        Timber.i("Deleted private key with tag: %s", keyTag)
    }
}
