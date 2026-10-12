package com.example.ble

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    FAILED
}

data class BleConnectionInfo(
    val state: BleConnectionState = BleConnectionState.DISCONNECTED,
    val connectedDevice: BleDeviceItem? = null,
    val negotiatedMtu: Int = 23,
    val isServiceDiscovered: Boolean = false,
    val isNotificationsSubscribed: Boolean = false,
    val errorMessage: String? = null
) {
    val isReadyForProvisioning: Boolean
        get() = state == BleConnectionState.CONNECTED && isServiceDiscovered && isNotificationsSubscribed
}
