package com.example.data.model

enum class BleConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    RECONNECTING,
    ERROR
}

data class BleConnectionState(
    val status: BleConnectionStatus = BleConnectionStatus.DISCONNECTED,
    val connectedDevice: DiscoveredPiDevice? = null,
    val errorMessage: String? = null,
    val isAutoReconnecting: Boolean = false,
    val connectionDropCount: Int = 0
)
