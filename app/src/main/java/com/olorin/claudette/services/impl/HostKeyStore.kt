package com.olorin.claudette.services.impl

import android.content.Context
import com.olorin.claudette.models.KnownHost
import com.olorin.claudette.services.interfaces.HostKeyStoreInterface
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.Base64

/**
 * Serializable wrapper for KnownHost since ByteArray needs special handling
 * in kotlinx.serialization JSON.
 */
@Serializable
private data class KnownHostJson(
    val hostIdentifier: String,
    val publicKeyDataBase64: String,
    val keyType: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long
) {
    fun toKnownHost(): KnownHost = KnownHost(
        hostIdentifier = hostIdentifier,
        publicKeyData = Base64.getDecoder().decode(publicKeyDataBase64),
        keyType = keyType,
        firstSeenAt = firstSeenAt,
        lastSeenAt = lastSeenAt
    )

    companion object {
        fun from(host: KnownHost): KnownHostJson = KnownHostJson(
            hostIdentifier = host.hostIdentifier,
            publicKeyDataBase64 = Base64.getEncoder().encodeToString(host.publicKeyData),
            keyType = host.keyType,
            firstSeenAt = host.firstSeenAt,
            lastSeenAt = host.lastSeenAt
        )
    }
}

class HostKeyStore(context: Context) : HostKeyStoreInterface {

    private val file: File
    private val lock = Any()
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    init {
        val appDir = File(context.filesDir, "com.olorin.claudette")
        if (!appDir.exists()) appDir.mkdirs()
        file = File(appDir, "known_hosts.json")
        Timber.i("Known hosts store path: %s", file.absolutePath)
    }

    override fun knownHost(forIdentifier: String): KnownHost? = synchronized(lock) {
        return loadHostsUnsafe().firstOrNull { it.hostIdentifier == forIdentifier }
    }

    override fun storeHost(host: KnownHost) = synchronized(lock) {
        val hosts = loadHostsUnsafe().toMutableList()
        hosts.removeAll { it.hostIdentifier == host.hostIdentifier }
        hosts.add(host)
        writeHosts(hosts)
        Timber.i("Stored host key for: %s", host.hostIdentifier)
    }

    override fun removeHost(forIdentifier: String) = synchronized(lock) {
        val hosts = loadHostsUnsafe().toMutableList()
        hosts.removeAll { it.hostIdentifier == forIdentifier }
        writeHosts(hosts)
        Timber.i("Removed host key for: %s", forIdentifier)
    }

    override fun allKnownHosts(): List<KnownHost> = synchronized(lock) {
        return loadHostsUnsafe()
    }

    private fun loadHostsUnsafe(): List<KnownHost> {
        if (!file.exists()) return emptyList()
        return try {
            val data = file.readText()
            json.decodeFromString<List<KnownHostJson>>(data).map { it.toKnownHost() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load known hosts")
            emptyList()
        }
    }

    private fun writeHosts(hosts: List<KnownHost>) {
        val jsonHosts = hosts.map { KnownHostJson.from(it) }
        file.writeText(json.encodeToString(jsonHosts))
    }
}
