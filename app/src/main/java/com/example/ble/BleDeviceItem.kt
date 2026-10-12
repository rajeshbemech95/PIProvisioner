package com.example.ble

import android.bluetooth.BluetoothDevice

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val bluetoothDevice: BluetoothDevice? = null,
    val isFactoryPi: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
)
