package com.example.data

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.ble.BleConnectionInfo
import com.example.ble.BleConnectionState
import com.example.ble.BleDeviceItem
import com.example.ble.IBleManager
import com.example.ble.NativeBleManager
import com.example.ble.SimulatedPiBleManager
import com.example.protocol.BleConstants
import com.example.protocol.ProvisioningCommand
import com.example.protocol.ProvisioningProtocol
import com.example.protocol.ProvisioningResponse
import com.example.protocol.SavedNetworkItem
import com.example.protocol.WifiScanItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProvisioningRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private const val TAG = "ProvisioningRepository"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val isHardwareBleAvailable = bluetoothManager?.adapter != null

    private val nativeBleManager = NativeBleManager(context, scope)
    private val simulatedBleManager = SimulatedPiBleManager(scope)

    // Demo Mode toggle: defaults to true if hardware BLE is absent
    private val _isDemoMode = MutableStateFlow(!isHardwareBleAvailable)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val activeManager: IBleManager
        get() = if (_isDemoMode.value) simulatedBleManager else nativeBleManager

    private val protocol = ProvisioningProtocol()

    // Public UI states
    val discoveredDevices: StateFlow<List<BleDeviceItem>>
        get() = activeManager.discoveredDevices

    val connectionInfo: StateFlow<BleConnectionInfo>
        get() = activeManager.connectionInfo

    private val _currentStatus = MutableStateFlow<ProvisioningResponse?>(null)
    val currentStatus: StateFlow<ProvisioningResponse?> = _currentStatus.asStateFlow()

    private val _scannedNetworks = MutableStateFlow<List<WifiScanItem>>(emptyList())
    val scannedNetworks: StateFlow<List<WifiScanItem>> = _scannedNetworks.asStateFlow()

    private val _savedNetworks = MutableStateFlow<List<SavedNetworkItem>>(emptyList())
    val savedNetworks: StateFlow<List<SavedNetworkItem>> = _savedNetworks.asStateFlow()

    private val _isProvisioningInProgress = MutableStateFlow(false)
    val isProvisioningInProgress: StateFlow<Boolean> = _isProvisioningInProgress.asStateFlow()

    private val _isScanningWifiInProgress = MutableStateFlow(false)
    val isScanningWifiInProgress: StateFlow<Boolean> = _isScanningWifiInProgress.asStateFlow()

    private val _isFetchingSavedNetworksInProgress = MutableStateFlow(false)
    val isFetchingSavedNetworksInProgress: StateFlow<Boolean> = _isFetchingSavedNetworksInProgress.asStateFlow()

    private val _lastResponseLog = MutableStateFlow<String>("Awaiting connection...")
    val lastResponseLog: StateFlow<String> = _lastResponseLog.asStateFlow()

    private var incomingFramesCollectorJob: Job? = null

    init {
        setupIncomingFramesCollector()
    }

    private fun setupIncomingFramesCollector() {
        incomingFramesCollectorJob?.cancel()
        incomingFramesCollectorJob = scope.launch {
            // Listen to active manager's incoming frames
            launch {
                nativeBleManager.incomingFrames.collectLatest { frame ->
                    if (!_isDemoMode.value && frame != null) {
                        processIncomingFrame(frame)
                    }
                }
            }
            launch {
                simulatedBleManager.incomingFrames.collectLatest { frame ->
                    if (_isDemoMode.value && frame != null) {
                        processIncomingFrame(frame)
                    }
                }
            }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        if (_isDemoMode.value != enabled) {
            activeManager.disconnect()
            activeManager.stopScan()
            protocol.resetReassembly()
            _isDemoMode.value = enabled
            _currentStatus.value = null
            _scannedNetworks.value = emptyList()
            _savedNetworks.value = emptyList()
            _lastResponseLog.value = if (enabled) "Switched to Simulated Pi Mode" else "Switched to Native BLE Mode"
        }
    }

    private fun processIncomingFrame(frame: ByteArray) {
        when (val result = protocol.decodeFrame(frame)) {
            is ProvisioningProtocol.ReassemblyResult.Complete -> {
                Log.d(TAG, "Reassembled JSON message (${result.json.length} chars)")
                _lastResponseLog.value = result.json
                try {
                    val response = ProvisioningResponse.fromJson(result.json)
                    handleParsedResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing JSON response", e)
                    _lastResponseLog.value = "JSON Parse Error: ${e.message}"
                }
            }
            is ProvisioningProtocol.ReassemblyResult.Partial -> {
                Log.d(TAG, "Received chunk ${result.receivedChunks} of ${result.totalChunks}")
            }
            is ProvisioningProtocol.ReassemblyResult.Error -> {
                Log.e(TAG, "Frame decode error: ${result.reason}")
                _lastResponseLog.value = "Framing Error: ${result.reason}"
            }
        }
    }

    private fun handleParsedResponse(response: ProvisioningResponse) {
        when (response.status) {
            "connecting" -> {
                _isProvisioningInProgress.value = true
                _currentStatus.value = response
            }
            "connected" -> {
                _isProvisioningInProgress.value = false
                _currentStatus.value = response
                // Refresh saved networks
                requestSavedNetworks()
            }
            "disconnected", "ready" -> {
                _isProvisioningInProgress.value = false
                _currentStatus.value = response
            }
            "error" -> {
                _isProvisioningInProgress.value = false
                _currentStatus.value = response
            }
            "scan_results" -> {
                _isScanningWifiInProgress.value = false
                _scannedNetworks.value = response.networks
            }
            "saved_networks" -> {
                _isFetchingSavedNetworksInProgress.value = false
                _savedNetworks.value = response.savedNetworks
            }
            else -> {
                _currentStatus.value = response
            }
        }
    }

    fun startScan() {
        activeManager.startScan(BleConstants.SCAN_TIMEOUT_MILLIS)
    }

    fun stopScan() {
        activeManager.stopScan()
    }

    fun connect(device: BleDeviceItem) {
        protocol.resetReassembly()
        activeManager.connect(device)
    }

    fun disconnect() {
        activeManager.disconnect()
        protocol.resetReassembly()
        _isProvisioningInProgress.value = false
        _isScanningWifiInProgress.value = false
        _isFetchingSavedNetworksInProgress.value = false
    }

    fun sendProvisioningCredentials(
        ssid: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val command = ProvisioningCommand.ConnectWifi(ssid = ssid, password = password)
        _isProvisioningInProgress.value = true
        sendCommand(command, onResult)
    }

    fun requestWifiScan() {
        _isScanningWifiInProgress.value = true
        val command = ProvisioningCommand.ScanWifi()
        sendCommand(command) { success, error ->
            if (!success) {
                _isScanningWifiInProgress.value = false
                _lastResponseLog.value = "Failed to send scan command: $error"
            }
        }
    }

    fun requestWifiStatus() {
        val command = ProvisioningCommand.GetStatus()
        sendCommand(command) { success, error ->
            if (!success) {
                _lastResponseLog.value = "Failed to send status request: $error"
            }
        }
    }

    fun requestSavedNetworks() {
        _isFetchingSavedNetworksInProgress.value = true
        val command = ProvisioningCommand.GetSavedNetworks()
        sendCommand(command) { success, error ->
            if (!success) {
                _isFetchingSavedNetworksInProgress.value = false
                _lastResponseLog.value = "Failed to fetch saved networks: $error"
            }
        }
    }

    fun disconnectCurrentWifi(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val command = ProvisioningCommand.DisconnectWifi()
        sendCommand(command, onResult)
    }

    fun forgetSavedNetwork(ssid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val command = ProvisioningCommand.ForgetWifi(ssid = ssid)
        sendCommand(command, onResult)
    }

    fun switchToSavedNetwork(ssid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val command = ProvisioningCommand.SwitchWifi(ssid = ssid)
        sendCommand(command, onResult)
    }

    private fun sendCommand(
        command: ProvisioningCommand,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        if (!activeManager.connectionInfo.value.isReadyForProvisioning) {
            onResult(false, "BLE device is not connected or GATT service is not ready")
            return
        }

        val json = command.toJson()
        val mtu = activeManager.connectionInfo.value.negotiatedMtu
        val maxFrameSize = (mtu - BleConstants.ATT_HEADER_OVERHEAD).coerceIn(20, 509)

        // Chunk and frame the JSON
        val frames = protocol.encodeMessage(json, maxFrameSize)
        Log.i(TAG, "Sending command ${command.cmd} encoded into ${frames.size} frame(s)")

        // Send frames sequentially
        sendFramesSequentially(frames, 0, onResult)
    }

    private fun sendFramesSequentially(
        frames: List<ByteArray>,
        index: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (index >= frames.size) {
            onComplete(true, null)
            return
        }

        activeManager.sendFrame(frames[index]) { success, error ->
            if (success) {
                sendFramesSequentially(frames, index + 1, onComplete)
            } else {
                onComplete(false, error ?: "Frame write failed at index $index")
            }
        }
    }
}
