package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.KnownHost

interface HostKeyStoreInterface {
    fun knownHost(forIdentifier: String): KnownHost?
    fun storeHost(host: KnownHost)
    fun removeHost(forIdentifier: String)
    fun allKnownHosts(): List<KnownHost>
}
