package dev.tvdeck.onecontroller.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import dev.tvdeck.onecontroller.core.model.TvDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class TvDiscoveryManager(private val context: Context) {
    companion object {
        private const val TAG = "TvDiscovery"
        const val SERVICE_TYPE_REMOTE_V2 = "_androidtvremote2._tcp."
        const val SERVICE_TYPE_ADB_CONNECT = "_adb-tls-connect._tcp."
        const val SERVICE_TYPE_GOOGLE_CAST = "_googlecast._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveredMap = mutableMapOf<String, TvDevice>()

    private val _discoveredDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanScope: CoroutineScope? = null
    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        discoveredMap.clear()
        _discoveredDevices.value = emptyList()

        val serviceTypes = listOf(
            SERVICE_TYPE_REMOTE_V2,
            SERVICE_TYPE_ADB_CONNECT,
            SERVICE_TYPE_GOOGLE_CAST
        )

        for (type in serviceTypes) {
            registerDiscoveryListener(type)
        }

        // Subnet port scanner as fallback
        scanScope = CoroutineScope(Dispatchers.IO + SupervisorJob()).apply {
            launch {
                scanLocalSubnet()
            }
        }
    }

    private fun registerDiscoveryListener(serviceType: String) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                try {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.w(TAG, "Resolve failed: $errorCode for ${serviceInfo.serviceName}")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress ?: return
                            val port = serviceInfo.port
                            val name = serviceInfo.serviceName

                            synchronized(discoveredMap) {
                                val existing = discoveredMap[host]
                                val updated = existing?.copy(
                                    name = if (existing.name.contains("TV") || existing.name.length > name.length) existing.name else name,
                                    remoteV2Port = if (serviceInfo.serviceType.contains("androidtvremote2")) port else existing.remoteV2Port,
                                    adbPort = if (serviceInfo.serviceType.contains("adb")) port else existing.adbPort
                                ) ?: TvDevice(
                                    id = host,
                                    name = cleanServiceName(name),
                                    host = host,
                                    remoteV2Port = if (serviceInfo.serviceType.contains("androidtvremote2")) port else 6466,
                                    adbPort = if (serviceInfo.serviceType.contains("adb")) port else 5555
                                )
                                discoveredMap[host] = updated
                                _discoveredDevices.value = discoveredMap.values.toList()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "Error resolving service", e)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            activeListeners.add(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery for $serviceType", e)
        }
    }

    private suspend fun scanLocalSubnet() = withContext(Dispatchers.IO) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return@withContext
            val ipInt = wm.connectionInfo.ipAddress
            if (ipInt == 0) return@withContext

            val myIp = Formatter.formatIpAddress(ipInt)
            val subnetPrefix = myIp.substringBeforeLast(".")

            // Scan common devices on subnet concurrently
            val jobs = (1..254).map { i ->
                async {
                    val host = "$subnetPrefix.$i"
                    if (host != myIp) {
                        checkHostForTvPorts(host)
                    }
                }
            }
            jobs.awaitAll()
        } catch (e: Exception) {
            Log.w(TAG, "Subnet scan error: ${e.message}")
        }
    }

    private fun checkHostForTvPorts(host: String) {
        val ports = listOf(6466, 5555)
        for (port in ports) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 200)
                    synchronized(discoveredMap) {
                        if (!discoveredMap.containsKey(host)) {
                            val dev = TvDevice(
                                id = host,
                                name = "Android TV ($host)",
                                host = host,
                                remoteV2Port = if (port == 6466) port else 6466,
                                adbPort = if (port == 5555) port else 5555
                            )
                            discoveredMap[host] = dev
                            _discoveredDevices.value = discoveredMap.values.toList()
                        }
                    }
                    return
                }
            } catch (_: Exception) {}
        }
    }

    private fun cleanServiceName(rawName: String): String {
        return rawName.replace(Regex("[_-]"), " ").trim()
    }

    fun stopDiscovery() {
        for (listener in activeListeners) {
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (_: Exception) {}
        }
        activeListeners.clear()
        scanScope?.cancel()
        scanScope = null
        _isScanning.value = false
    }
}
