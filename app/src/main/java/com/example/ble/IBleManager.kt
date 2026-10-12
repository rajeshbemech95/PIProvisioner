package com.example.ble

import kotlinx.coroutines.flow.StateFlow
import com.example.data.model.PiNetworkStatus

interface IBleManager {
    val discoveredDevices: StateFlow<List<BleDeviceItem>>
    val connectionInfo: StateFlow<BleConnectionInfo>
    val incomingFrames: StateFlow<ByteArray?>
    val networkStatus: StateFlow<PiNetworkStatus>

    fun startScan(timeoutMs: Long = 15000L)
    fun stopScan()
    fun connect(device: BleDeviceItem)
    fun disconnect()
    fun sendFrame(frame: ByteArray, onResult: (Boolean, String?) -> Unit)
    fun simulateConnectionDrop() {}
    fun setEthernetMode() {}
}
