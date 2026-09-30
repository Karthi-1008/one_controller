package dev.tvdeck.onecontroller.core.transport

import android.content.Context
import android.util.Log
import dev.tvdeck.onecontroller.core.adb.AdbClient
import dev.tvdeck.onecontroller.core.discovery.TvDiscoveryManager
import dev.tvdeck.onecontroller.core.model.*
import dev.tvdeck.onecontroller.core.remotev2.AndroidTvRemoteV2Client
import dev.tvdeck.onecontroller.core.remotev2.RemoteDirection
import dev.tvdeck.onecontroller.core.wol.WakeOnLan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

class TransportManager(val context: Context) {
    companion object {
        private const val TAG = "TransportManager"
        private const val PREFS_DEVICES = "saved_tv_devices"
        private const val KEY_SAVED_LIST = "devices_json"
        private const val KEY_LAST_DEVICE_ID = "last_connected_id"

        @Volatile
        private var instance: TransportManager? = null

        fun getInstance(context: Context): TransportManager {
            return instance ?: synchronized(this) {
                instance ?: TransportManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled coroutine error: ${throwable.message}", throwable)
    }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)

    val remoteV2Client = AndroidTvRemoteV2Client(context)
    val adbClient = AdbClient(context)
    val discoveryManager = TvDiscoveryManager(context)

    private val _savedDevices = MutableStateFlow<List<TvDevice>>(emptyList())
    val savedDevices: StateFlow<List<TvDevice>> = _savedDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<TvDevice?>(null)
    val activeDevice: StateFlow<TvDevice?> = _activeDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val tvState: StateFlow<TvState> = remoteV2Client.tvState

    init {
        loadSavedDevices()
        autoConnectLastDevice()
    }

    private fun loadSavedDevices() {
        val prefs = context.getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SAVED_LIST, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<TvDevice>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TvDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        host = obj.getString("host"),
                        remoteV2Port = obj.optInt("remoteV2Port", 6466),
                        adbPort = obj.optInt("adbPort", 5555),
                        macAddress = obj.optString("macAddress", ""),
                        model = obj.optString("model", "Android TV"),
                        manufacturer = obj.optString("manufacturer", "Google"),
                        isPairedRemoteV2 = obj.optBoolean("isPairedRemoteV2", false),
                        isPairedAdb = obj.optBoolean("isPairedAdb", false),
                        lastConnected = obj.optLong("lastConnected", 0L)
                    )
                )
            }
            _savedDevices.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved devices", e)
        }
    }

    fun saveDevice(device: TvDevice) {
        val current = _savedDevices.value.toMutableList()
        val index = current.indexOfFirst { it.id == device.id }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(0, device)
        }
        _savedDevices.value = current

        try {
            val arr = JSONArray()
            for (d in current) {
                val obj = JSONObject().apply {
                    put("id", d.id)
                    put("name", d.name)
                    put("host", d.host)
                    put("remoteV2Port", d.remoteV2Port)
                    put("adbPort", d.adbPort)
                    put("macAddress", d.macAddress)
                    put("model", d.model)
                    put("manufacturer", d.manufacturer)
                    put("isPairedRemoteV2", d.isPairedRemoteV2)
                    put("isPairedAdb", d.isPairedAdb)
                    put("lastConnected", d.lastConnected)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_LIST, arr.toString())
                .putString(KEY_LAST_DEVICE_ID, device.id)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving devices", e)
        }
    }

    private fun autoConnectLastDevice() {
        val prefs = context.getSharedPreferences(PREFS_DEVICES, Context.MODE_PRIVATE)
        val lastId = prefs.getString(KEY_LAST_DEVICE_ID, null) ?: return
        val dev = _savedDevices.value.firstOrNull { it.id == lastId }
        if (dev != null) {
            connectToDevice(dev)
        }
    }

    fun connectToDevice(device: TvDevice) {
        _activeDevice.value = device
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _statusMessage.value = "Connecting to ${device.name}..."

        scope.launch(Dispatchers.IO) {
            // Step 1: Connect to ADB (USB/Wireless debugging on port 5555)
            val adbSuccess = adbClient.connect(device.host, device.adbPort)
            if (adbSuccess) {
                Log.d(TAG, "ADB connected successfully")
                saveDevice(device.copy(isPairedAdb = true, lastConnected = System.currentTimeMillis()))

                // Fetch MAC address for WoL if missing
                if (device.macAddress.isEmpty()) {
                    val macRes = adbClient.executeShell("cat /sys/class/net/wlan0/address || cat /sys/class/net/eth0/address")
                    if (macRes.success && macRes.output.isNotBlank()) {
                        val cleanMac = macRes.output.trim().lines().firstOrNull() ?: ""
                        if (cleanMac.length == 17) {
                            saveDevice(device.copy(macAddress = cleanMac))
                        }
                    }
                }
            }

            // Step 2: Connect to Remote v2 (Lowest latency protocol on port 6466)
            var v2Success = false
            if (device.isPairedRemoteV2) {
                v2Success = remoteV2Client.connect(device.host, device.remoteV2Port)
                if (v2Success) {
                    Log.d(TAG, "Remote v2 connected successfully")
                    saveDevice(device.copy(lastConnected = System.currentTimeMillis()))
                }
            }

            if (adbSuccess || v2Success) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                val mode = when {
                    v2Success && adbSuccess -> "Hybrid (Remote v2 + ADB)"
                    adbSuccess -> "ADB Shell (Ready to control TV)"
                    else -> "Remote v2"
                }
                _statusMessage.value = "Connected via $mode"
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
                _statusMessage.value = "Connection failed. Ensure TV is turned on and ADB (port 5555) is enabled."
            }
        }
    }

    suspend fun pairRemoteV2(device: TvDevice, onCodeRequested: suspend () -> String): Boolean = withContext(Dispatchers.IO) {
        _statusMessage.value = "Probing TV pairing port 6467 on ${device.host}..."

        // Quick TCP probe to verify port 6467 is reachable
        val portOpen = try {
            java.net.Socket().use { s ->
                s.connect(java.net.InetSocketAddress(device.host, 6467), 3000)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Port 6467 unreachable: ${e.message}")
            false
        }

        if (!portOpen) {
            _statusMessage.value = "TV pairing port 6467 is not accessible. Device is controlled via ADB Shell."
            return@withContext false
        }

        _statusMessage.value = "Connecting to TV pairing service (port 6467)..."
        val paired = remoteV2Client.startPairing(device.host, 6467, onCodeRequested)
        if (paired) {
            val updated = device.copy(isPairedRemoteV2 = true, lastConnected = System.currentTimeMillis())
            saveDevice(updated)
            _statusMessage.value = "Pairing successful! Connecting to TV..."
            connectToDevice(updated)
        } else {
            _statusMessage.value = "Pairing failed. If USB debugging is on, you can control the TV directly via ADB."
        }
        paired
    }

    suspend fun pairAdbWireless(host: String, port: Int, code: String): Boolean = withContext(Dispatchers.IO) {
        _statusMessage.value = "Pairing ADB wireless on port $port..."
        val res = adbClient.connect(host, port)
        if (res) {
            _statusMessage.value = "ADB Wireless paired!"
        }
        res
    }

    // -------------------------------------------------------------
    // Intelligent Command Dispatcher
    // -------------------------------------------------------------

    fun sendNavigationKey(keyCode: Int, remoteKeyName: String? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                var sent = false
                // Try Remote v2 if paired and connected
                if (activeDevice.value?.isPairedRemoteV2 == true && remoteV2Client.isConnected.value) {
                    sent = remoteV2Client.sendKey(keyCode, RemoteDirection.SHORT)
                }

                // Fall back to ADB Shell (Instant key injection)
                if (!sent && adbClient.isConnected()) {
                    val res = adbClient.executeShell("input keyevent $keyCode")
                    if (!res.success) {
                        Log.w(TAG, "ADB keyevent failed: ${res.output}")
                    }
                } else if (!sent && !adbClient.isConnected()) {
                    Log.w(TAG, "Cannot send key $keyCode: Not connected to TV")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending key: ${e.message}")
            }
        }
    }

    fun sendText(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                var sent = false
                if (activeDevice.value?.isPairedRemoteV2 == true && remoteV2Client.isConnected.value) {
                    sent = remoteV2Client.sendText(text)
                }
                if (!sent && adbClient.isConnected()) {
                    val escaped = text.replace(" ", "%s").replace("'", "\\'")
                    adbClient.executeShell("input text '$escaped'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending text: ${e.message}")
            }
        }
    }

    fun sendTouchCoordinates(x: Int, y: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                if (adbClient.isConnected()) {
                    adbClient.executeShell("input tap $x $y")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending tap: ${e.message}")
            }
        }
    }

    fun sendSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 200) {
        scope.launch(Dispatchers.IO) {
            try {
                if (adbClient.isConnected()) {
                    adbClient.executeShell("input swipe $x1 $y1 $x2 $y2 $durationMs")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending swipe: ${e.message}")
            }
        }
    }

    suspend fun executeCommand(item: CommandItem, paramValues: Map<String, String> = emptyMap()): AdbResult {
        // Substitute parameters into template
        var shellCmd = item.shell
        for ((k, v) in paramValues) {
            shellCmd = shellCmd.replace("{$k}", v)
        }

        // If command supports Remote v2 app link launch
        if (item.via.contains("R") && item.remoteKey != null && item.remoteKey.startsWith("http") && remoteV2Client.isConnected.value) {
            remoteV2Client.launchAppLink(item.remoteKey)
            return AdbResult(true, "Launched via Remote v2")
        }

        // Execute via ADB Shell
        if (adbClient.isConnected()) {
            return adbClient.executeShell(shellCmd)
        } else if (remoteV2Client.isConnected.value && item.remoteKey != null) {
            // Fallback key injection via Remote v2
            val keycode = parseRemoteKeycode(item.remoteKey)
            if (keycode > 0) {
                remoteV2Client.sendKey(keycode)
                return AdbResult(true, "Sent key via Remote v2")
            }
        }

        return AdbResult(false, "Device not connected via required transport")
    }

    private fun parseRemoteKeycode(name: String): Int {
        return when (name) {
            "DPAD_UP" -> 19
            "DPAD_DOWN" -> 20
            "DPAD_LEFT" -> 21
            "DPAD_RIGHT" -> 22
            "DPAD_CENTER" -> 23
            "BACK" -> 4
            "HOME" -> 3
            "MENU" -> 82
            "SETTINGS" -> 176
            "VOLUME_UP" -> 24
            "VOLUME_DOWN" -> 25
            "VOLUME_MUTE" -> 164
            "POWER" -> 26
            "MEDIA_PLAY_PAUSE" -> 85
            "MEDIA_PLAY" -> 126
            "MEDIA_PAUSE" -> 127
            "MEDIA_STOP" -> 86
            "MEDIA_NEXT" -> 87
            "MEDIA_PREVIOUS" -> 88
            "MEDIA_REWIND" -> 89
            "MEDIA_FAST_FORWARD" -> 90
            "GUIDE" -> 172
            "INFO" -> 165
            "SEARCH" -> 84
            "VOICE_ASSIST" -> 231
            else -> 0
        }
    }

    fun wakeActiveDevice() {
        val dev = _activeDevice.value ?: return
        scope.launch(Dispatchers.IO) {
            if (dev.macAddress.isNotBlank()) {
                val sent = WakeOnLan.wake(dev.macAddress)
                _statusMessage.value = if (sent) "Wake-on-LAN magic packet sent!" else "Failed to send WoL"
            } else {
                _statusMessage.value = "No MAC address saved for this TV"
            }
        }
    }

    fun disconnect() {
        remoteV2Client.disconnect()
        adbClient.disconnect()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected"
    }
}
