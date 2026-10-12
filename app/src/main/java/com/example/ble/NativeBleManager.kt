package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.protocol.BleConstants
import com.example.data.model.PiNetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NativeBleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : IBleManager {

    companion object {
        private const val TAG = "NativeBleManager"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _discoveredDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    override val discoveredDevices: StateFlow<List<BleDeviceItem>> = _discoveredDevices.asStateFlow()

    private val _connectionInfo = MutableStateFlow(BleConnectionInfo())
    override val connectionInfo: StateFlow<BleConnectionInfo> = _connectionInfo.asStateFlow()

    private val _networkStatus = MutableStateFlow(PiNetworkStatus())
    override val networkStatus: StateFlow<PiNetworkStatus> = _networkStatus.asStateFlow()

    private val _incomingFrames = MutableStateFlow<ByteArray?>(null)
    override val incomingFrames: StateFlow<ByteArray?> = _incomingFrames.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var pendingWriteCallback: ((Boolean, String?) -> Unit)? = null
    private val scanTimeoutHandler = Handler(Looper.getMainLooper())

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
            val address = device.address

            val isFactoryCandidate = name.startsWith(BleConstants.DEFAULT_ADVERTISED_NAME_PREFIX, ignoreCase = true) ||
                    result.scanRecord?.serviceUuids?.any { it.uuid == BleConstants.SERVICE_UUID } == true

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }

            val item = BleDeviceItem(
                name = name,
                address = address,
                rssi = result.rssi,
                bluetoothDevice = device,
                isFactoryPi = isFactoryCandidate,
                lastSeenMs = System.currentTimeMillis()
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }

            _discoveredDevices.value = currentList.sortedWith(
                compareByDescending<BleDeviceItem> { it.isFactoryPi }.thenByDescending { it.rssi }
            )
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed: errorCode=$errorCode")
            _connectionInfo.value = _connectionInfo.value.copy(
                state = BleConnectionState.FAILED,
                errorMessage = "Scan failed with error code $errorCode"
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.FAILED, errorMessage = "GATT error: $status")
                safeCloseGatt()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.CONNECTING)
                gatt?.requestMtu(BleConstants.REQUEST_MTU_SIZE)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionInfo.value = BleConnectionInfo(state = BleConnectionState.DISCONNECTED)
                safeCloseGatt()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            _connectionInfo.value = _connectionInfo.value.copy(negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23)
            gatt?.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(BleConstants.SERVICE_UUID)
                writeCharacteristic = service?.getCharacteristic(BleConstants.CHAR_WRITE_COMMAND_UUID)
                val notifyChar = service?.getCharacteristic(BleConstants.CHAR_STATUS_NOTIFY_UUID)

                if (writeCharacteristic != null && notifyChar != null) {
                    gatt?.setCharacteristicNotification(notifyChar, true)
                    val cccd = notifyChar.getDescriptor(BleConstants.CCCD_DESCRIPTOR_UUID)
                    if (cccd != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt?.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt?.writeDescriptor(cccd)
                        }
                    } else {
                        _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.CONNECTED, isServiceDiscovered = true, isNotificationsSubscribed = true)
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.CONNECTED, isServiceDiscovered = true, isNotificationsSubscribed = true)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic?.uuid == BleConstants.CHAR_STATUS_NOTIFY_UUID) {
                _incomingFrames.value = characteristic.value
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            val cb = pendingWriteCallback
            pendingWriteCallback = null
            cb?.invoke(status == BluetoothGatt.GATT_SUCCESS, if (status == BluetoothGatt.GATT_SUCCESS) null else "Write failed: $status")
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan(timeoutMs: Long) {
        _discoveredDevices.value = emptyList()
        _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.SCANNING)
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
        scanTimeoutHandler.postDelayed({ stopScan() }, timeoutMs)
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionInfo.value.state == BleConnectionState.SCANNING) {
            _connectionInfo.value = _connectionInfo.value.copy(state = BleConnectionState.DISCONNECTED)
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(device: BleDeviceItem) {
        activeGatt = device.bluetoothDevice?.connectGatt(context, false, gattCallback)
        _connectionInfo.value = BleConnectionInfo(state = BleConnectionState.CONNECTING, connectedDevice = device)
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        activeGatt?.disconnect()
        safeCloseGatt()
        _connectionInfo.value = BleConnectionInfo(state = BleConnectionState.DISCONNECTED)
    }

    @SuppressLint("MissingPermission")
    override fun sendFrame(frame: ByteArray, onResult: (Boolean, String?) -> Unit) {
        val char = writeCharacteristic
        if (char == null) { onResult(false, "No characteristic"); return }
        pendingWriteCallback = onResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activeGatt?.writeCharacteristic(char, frame, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            char.value = frame
            @Suppress("DEPRECATION")
            activeGatt?.writeCharacteristic(char)
        }
    }

    override fun simulateConnectionDrop() {}
    override fun setEthernetMode() {}

    @SuppressLint("MissingPermission")
    private fun safeCloseGatt() {
        activeGatt?.close()
        activeGatt = null
        writeCharacteristic = null
    }
}
