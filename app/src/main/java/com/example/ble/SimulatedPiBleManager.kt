package com.example.ble

import com.example.protocol.BleConstants
import com.example.protocol.ProvisioningProtocol
import com.example.data.model.PiNetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * High-fidelity Simulated Raspberry Pi BLE Peripheral.
 *
 * Implements the exact same binary framing protocol (Version 0x01) and UTF-8 JSON commands
 * specified in the project requirements.
 * Used for local testing, JVM unit/UI testing, and cloud emulator evaluation.
 */
class SimulatedPiBleManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : IBleManager {

    private val protocol = ProvisioningProtocol()

    private val simulatedDevices = listOf(
        BleDeviceItem(
            name = BleConstants.DEFAULT_TARGET_NAME,
            address = "B8:27:EB:4A:91:0C",
            rssi = -54,
            isFactoryPi = true
        ),
        BleDeviceItem(
            name = "FactoryDevice-B890",
            address = "DC:A6:32:8B:10:44",
            rssi = -71,
            isFactoryPi = true
        ),
        BleDeviceItem(
            name = "Generic-BLE-Beacon",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -88,
            isFactoryPi = false
        )
    )

    private val _discoveredDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDeviceItem>> = _discoveredDevices.asStateFlow()

    private val _connectionInfo = MutableStateFlow(BleConnectionInfo())
    override val connectionInfo: StateFlow<BleConnectionInfo> = _connectionInfo.asStateFlow()

    private val _incomingFrames = MutableStateFlow<ByteArray?>(null)
    override val incomingFrames: StateFlow<ByteArray?> = _incomingFrames.asStateFlow()

    private val _networkStatus = MutableStateFlow(PiNetworkStatus())
    override val networkStatus: StateFlow<PiNetworkStatus> = _networkStatus.asStateFlow()

    private var scanJob: Job? = null
    private var targetedDevice: BleDeviceItem? = null

    // Pi simulated internal Wi-Fi state
    private var currentConnectedSsid: String? = null
    private var currentAssignedIp: String? = null
    private val savedProfiles = mutableListOf(
        JSONObject().apply {
            put("ssid", "Factory_Line_IoT")
            put("active", false)
            put("lastSeen", "Factory Burn-in")
        }
    )

    override fun startScan(timeoutMs: Long) {
        scanJob?.cancel()
        _discoveredDevices.value = emptyList()
        _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.SCANNING)

        scanJob = scope.launch {
            delay(400)
            _discoveredDevices.value = listOf(simulatedDevices[0])
            delay(500)
            _discoveredDevices.value = listOf(simulatedDevices[0], simulatedDevices[1])
            delay(600)
            _discoveredDevices.value = simulatedDevices
            delay(timeoutMs)
            _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.DISCONNECTED)
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        if (_connectionInfo.value.state == BleConnectionState.SCANNING) {
            _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.DISCONNECTED)
        }
    }

    override fun connect(device: BleDeviceItem) {
        stopScan()
        targetedDevice = device

        _connectionInfo.value = BleConnectionInfo(
            state = BleConnectionState.CONNECTING,
            connectedDevice = device
        )

        scope.launch {
            delay(600) // Simulating BLE handshake, MTU negotiation & GATT discovery
            _connectionInfo.value = BleConnectionInfo(
                state = BleConnectionState.CONNECTED,
                connectedDevice = device,
                negotiatedMtu = 512,
                isServiceDiscovered = true,
                isNotificationsSubscribed = true
            )

            // Send initial "ready" status notification from Pi
            delay(200)
            val readyJson = JSONObject().apply {
                put("status", if (currentConnectedSsid != null) "connected" else "ready")
                put("ssid", currentConnectedSsid ?: "")
                put("ip", currentAssignedIp ?: "")
                put("message", "Factory Pi BLE Provisioning Service Ready")
            }.toString()
            emitFramedResponse(readyJson)
        }
    }

    override fun disconnect() {
        _connectionInfo.value = BleConnectionInfo(
            state = BleConnectionState.DISCONNECTED,
            connectedDevice = null
        )
    }

    override fun sendFrame(frame: ByteArray, onResult: (Boolean, String?) -> Unit) {
        if (_connectionInfo.value.state != BleConnectionState.CONNECTED) {
            onResult(false, "Device not connected")
            return
        }

        onResult(true, null)

        // Decode frame in Pi engine
        when (val result = protocol.decodeFrame(frame)) {
            is ProvisioningProtocol.ReassemblyResult.Complete -> {
                handleReceivedJsonCommand(result.json)
            }
            is ProvisioningProtocol.ReassemblyResult.Partial -> {
                // Waiting for next chunk
            }
            is ProvisioningProtocol.ReassemblyResult.Error -> {
                val errorJson = JSONObject().apply {
                    put("status", "error")
                    put("error", "Malformed frame: ${result.reason}")
                }.toString()
                emitFramedResponse(errorJson)
            }
        }
    }

    private fun handleReceivedJsonCommand(jsonStr: String) {
        scope.launch {
            try {
                val req = JSONObject(jsonStr)
                val cmd = req.getString("cmd")
                val reqId = req.optString("reqId", "req_sim")

                when (cmd) {
                    "wifi_connect" -> {
                        val ssid = req.getString("ssid")
                        val password = req.optString("password", "")

                        // First notify "connecting"
                        val connectingJson = JSONObject().apply {
                            put("status", "connecting")
                            put("reqId", reqId)
                            put("ssid", ssid)
                            put("message", "Associating with $ssid...")
                        }.toString()
                        emitFramedResponse(connectingJson)

                        delay(1500) // Simulating WPA supplicant authentication and DHCP lease

                        if (password.length < 4 && !password.isEmpty()) {
                            // Simulate invalid password error
                            val errJson = JSONObject().apply {
                                put("status", "error")
                                put("reqId", reqId)
                                put("error", "WPA authentication failed: Key handshake rejected")
                            }.toString()
                            emitFramedResponse(errJson)
                        } else {
                            currentConnectedSsid = ssid
                            currentAssignedIp = "192.168.1.${Random.nextInt(101, 240)}"

                            // Add to saved profiles
                            val existingIndex = (0 until savedProfiles.size).firstOrNull {
                                savedProfiles[it].getString("ssid") == ssid
                            }
                            if (existingIndex != null) {
                                savedProfiles[existingIndex].put("active", true)
                            } else {
                                savedProfiles.add(
                                    0,
                                    JSONObject().apply {
                                        put("ssid", ssid)
                                        put("active", true)
                                        put("lastSeen", "Just now")
                                    }
                                )
                            }

                            val successJson = JSONObject().apply {
                                put("status", "connected")
                                put("reqId", reqId)
                                put("ssid", currentConnectedSsid)
                                put("ip", currentAssignedIp)
                                put("message", "Wi-Fi connected successfully")
                            }.toString()
                            emitFramedResponse(successJson)
                        }
                    }

                    "wifi_scan" -> {
                        delay(1200) // Simulating iwlist / wpa_cli scan
                        val networksArr = JSONArray().apply {
                            put(JSONObject().apply {
                                put("ssid", "Factory_Line_IoT")
                                put("rssi", -52)
                                put("security", "WPA2-PSK")
                                put("channel", 6)
                                put("connected", currentConnectedSsid == "Factory_Line_IoT")
                            })
                            put(JSONObject().apply {
                                put("ssid", "TechHQ_Secure_5G")
                                put("rssi", -63)
                                put("security", "WPA2/WPA3")
                                put("channel", 36)
                                put("connected", currentConnectedSsid == "TechHQ_Secure_5G")
                            })
                            put(JSONObject().apply {
                                put("ssid", "Warehouse_Mesh_04")
                                put("rssi", -74)
                                put("security", "WPA2-PSK")
                                put("channel", 11)
                                put("connected", currentConnectedSsid == "Warehouse_Mesh_04")
                            })
                            put(JSONObject().apply {
                                put("ssid", "Guest_Open_Wifi")
                                put("rssi", -82)
                                put("security", "Open")
                                put("channel", 1)
                                put("connected", currentConnectedSsid == "Guest_Open_Wifi")
                            })
                        }

                        val scanResponse = JSONObject().apply {
                            put("status", "scan_results")
                            put("reqId", reqId)
                            put("networks", networksArr)
                        }.toString()
                        emitFramedResponse(scanResponse)
                    }

                    "wifi_status" -> {
                        delay(200)
                        val statusJson = JSONObject().apply {
                            put("status", if (currentConnectedSsid != null) "connected" else "disconnected")
                            put("reqId", reqId)
                            put("ssid", currentConnectedSsid ?: "")
                            put("ip", currentAssignedIp ?: "")
                            put("message", if (currentConnectedSsid != null) "Active link established" else "Unconfigured")
                        }.toString()
                        emitFramedResponse(statusJson)
                    }

                    "wifi_saved_networks" -> {
                        delay(300)
                        val savedArr = JSONArray()
                        savedProfiles.forEach { savedArr.put(it) }

                        val savedResp = JSONObject().apply {
                            put("status", "saved_networks")
                            put("reqId", reqId)
                            put("saved", savedArr)
                        }.toString()
                        emitFramedResponse(savedResp)
                    }

                    "wifi_disconnect" -> {
                        delay(400)
                        currentConnectedSsid = null
                        currentAssignedIp = null
                        savedProfiles.forEach { it.put("active", false) }

                        val discResp = JSONObject().apply {
                            put("status", "disconnected")
                            put("reqId", reqId)
                            put("message", "Interface wlan0 disconnected")
                        }.toString()
                        emitFramedResponse(discResp)
                    }

                    "wifi_forget" -> {
                        delay(300)
                        val ssidToForget = req.getString("ssid")
                        val iterator = savedProfiles.iterator()
                        while (iterator.hasNext()) {
                            if (iterator.next().getString("ssid") == ssidToForget) {
                                iterator.remove()
                            }
                        }
                        if (currentConnectedSsid == ssidToForget) {
                            currentConnectedSsid = null
                            currentAssignedIp = null
                        }

                        val savedArr = JSONArray()
                        savedProfiles.forEach { savedArr.put(it) }

                        val resp = JSONObject().apply {
                            put("status", "saved_networks")
                            put("reqId", reqId)
                            put("message", "Profile $ssidToForget forgotten")
                            put("saved", savedArr)
                        }.toString()
                        emitFramedResponse(resp)
                    }

                    "wifi_switch" -> {
                        delay(1000)
                        val targetSsid = req.getString("ssid")
                        currentConnectedSsid = targetSsid
                        currentAssignedIp = "192.168.1.${Random.nextInt(101, 240)}"
                        savedProfiles.forEach {
                            it.put("active", it.getString("ssid") == targetSsid)
                        }

                        val resp = JSONObject().apply {
                            put("status", "connected")
                            put("reqId", reqId)
                            put("ssid", currentConnectedSsid)
                            put("ip", currentAssignedIp)
                            put("message", "Switched to $targetSsid")
                        }.toString()
                        emitFramedResponse(resp)
                    }

                    else -> {
                        val unk = JSONObject().apply {
                            put("status", "error")
                            put("reqId", reqId)
                            put("error", "Unknown command: $cmd")
                        }.toString()
                        emitFramedResponse(unk)
                    }
                }
            } catch (e: Exception) {
                val err = JSONObject().apply {
                    put("status", "error")
                    put("error", "Invalid JSON request: ${e.message}")
                }.toString()
                emitFramedResponse(err)
            }
        }
    }

    private fun emitFramedResponse(jsonStr: String) {
        scope.launch {
            // Encode using the exact framing protocol (simulating chunks across MTU)
            val frames = protocol.encodeMessage(jsonStr, maxFrameSize = 128)
            frames.forEach { frame ->
                delay(40) // Small BLE characteristic indication interval
                _incomingFrames.value = frame
            }
        }
    }

    override fun simulateConnectionDrop() {
        val currentDropCount = _connectionInfo.value.let { 0 } + 1 // Simplified for demo
        _connectionInfo.value = _connectionInfo.value.copy(
            state = BleConnectionState.FAILED,
            errorMessage = "BLE Link lost (Supervision timeout). Auto-reconnecting..."
        )
        scope.launch {
            delay(2500)
            _connectionInfo.value = _connectionInfo.value.copy(
                state = BleConnectionState.CONNECTED,
                errorMessage = null
            )
        }
    }

    override fun setEthernetMode() {
        _networkStatus.value = _networkStatus.value.copy(
            connectionType = com.example.data.model.NetworkType.ETHERNET,
            ipAddress = "192.168.1.189",
            ssid = "eth0 (Gigabit)"
        )
    }
}
