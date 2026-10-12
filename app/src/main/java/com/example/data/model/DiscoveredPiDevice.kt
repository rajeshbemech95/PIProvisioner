package com.example.data.model

import android.bluetooth.BluetoothDevice

data class DiscoveredPiDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val model: String = "Raspberry Pi",
    val bluetoothDevice: BluetoothDevice? = null,
    val isSimulated: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
)
