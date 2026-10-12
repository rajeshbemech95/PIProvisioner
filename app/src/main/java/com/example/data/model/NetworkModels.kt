package com.example.data.model

enum class NetworkType {
    WIFI,
    ETHERNET,
    NONE
}

data class PiNetworkStatus(
    val connectionType: NetworkType = NetworkType.NONE,
    val ipAddress: String? = null,
    val ssid: String? = null,
    val macAddress: String? = null,
    val interfaceName: String? = null,
    val signalStrength: Int? = null,
    val isConnected: Boolean = false
) {
    val displayType: String
        get() = when (connectionType) {
            NetworkType.WIFI -> "Wi-Fi (wlan0)"
            NetworkType.ETHERNET -> "Ethernet (eth0)"
            NetworkType.NONE -> "Not Connected"
        }
}

data class AvailableWifiNetwork(
    val ssid: String,
    val rssi: Int,
    val security: String = "WPA2/WPA3",
    val channel: Int = 6,
    val isConnected: Boolean = false
)

data class SavedWifiProfile(
    val ssid: String,
    val priority: Int = 0,
    val isActive: Boolean = false,
    val lastConnected: String = "Previously saved"
)
