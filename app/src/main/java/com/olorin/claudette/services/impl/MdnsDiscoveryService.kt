package com.olorin.claudette.services.impl

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.olorin.claudette.models.BonjourHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class MdnsDiscoveryService(
    private val serviceType: String
) {

    private val _discoveredHosts = MutableStateFlow<List<BonjourHost>>(emptyList())
    val discoveredHosts: StateFlow<List<BonjourHost>> = _discoveredHosts.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery(context: Context) {
        stopDiscovery(context)

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifiManager?.createMulticastLock(MULTICAST_LOCK_TAG)
        lock?.setReferenceCounted(true)
        lock?.acquire()
        multicastLock = lock

        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager = manager

        val listener = createDiscoveryListener(manager)
        discoveryListener = listener

        manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        _isSearching.value = true
        Timber.i("Started mDNS discovery for %s", serviceType)
    }

    fun stopDiscovery(context: Context) {
        val manager = nsdManager
        val listener = discoveryListener

        if (manager != null && listener != null) {
            try {
                manager.stopServiceDiscovery(listener)
            } catch (e: IllegalArgumentException) {
                Timber.d("Discovery listener was not registered: %s", e.message)
            }
        }

        multicastLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }

        nsdManager = null
        discoveryListener = null
        multicastLock = null
        _isSearching.value = false
        Timber.i("Stopped mDNS discovery")
    }

    private fun createDiscoveryListener(manager: NsdManager): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(regType: String) {
                Timber.d("mDNS discovery started for: %s", regType)
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.i("Found mDNS service: %s", serviceInfo.serviceName)
                manager.resolveService(serviceInfo, createResolveListener())
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName
                Timber.i("Lost mDNS service: %s", name)
                _discoveredHosts.value = _discoveredHosts.value.filter { it.serviceName != name }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isSearching.value = false
                Timber.d("mDNS discovery stopped for: %s", serviceType)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isSearching.value = false
                Timber.e("mDNS discovery start failed for %s, error: %d", serviceType, errorCode)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("mDNS discovery stop failed for %s, error: %d", serviceType, errorCode)
            }
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.w(
                    "Failed to resolve mDNS service: %s, error: %d",
                    serviceInfo.serviceName,
                    errorCode
                )
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val hostname = serviceInfo.host?.hostName ?: serviceInfo.serviceName
                val port = serviceInfo.port
                val serviceName = serviceInfo.serviceName

                val host = BonjourHost(
                    serviceName = serviceName,
                    hostname = hostname,
                    port = port
                )

                val currentHosts = _discoveredHosts.value.toMutableList()
                currentHosts.removeAll { it.serviceName == serviceName }
                currentHosts.add(host)
                _discoveredHosts.value = currentHosts

                Timber.i("Resolved mDNS host: %s:%d", hostname, port)
            }
        }
    }

    companion object {
        private const val MULTICAST_LOCK_TAG = "claudette:mdns"
    }
}
