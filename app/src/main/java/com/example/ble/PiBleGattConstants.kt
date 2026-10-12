package com.example.ble

import java.util.UUID

object PiBleGattConstants {
    // Primary Service for Raspberry Pi Wi-Fi Provisioning
    val SERVICE_PI_PROVISION: UUID = UUID.fromString("0000FFFF-0000-1000-8000-00805F9B34FB")

    // Characteristic: Write Wi-Fi Credentials (SSID + Password)
    val CHAR_CREDENTIALS: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")

    // Characteristic: Read/Notify Pi Network Status (Type, IP, SSID, MAC)
    val CHAR_NETWORK_STATUS: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")

    // Characteristic: Read/Notify Available Scanned Wi-Fi Networks from Pi
    val CHAR_SCAN_RESULTS: UUID = UUID.fromString("0000FFF3-0000-1000-8000-00805F9B34FB")

    // Characteristic: Read/Notify Saved Wi-Fi Networks on Pi
    val CHAR_SAVED_NETWORKS: UUID = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB")

    // Characteristic: Write Control Commands (e.g. TRIGGER_SCAN, DISCONNECT, FORGET:<ssid>, SWITCH:<ssid>)
    val CHAR_CONTROL: UUID = UUID.fromString("0000FFF5-0000-1000-8000-00805F9B34FB")

    // Standard Client Characteristic Configuration Descriptor (for Enable Notifications)
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Control command keywords
    const val CMD_SCAN_WIFI = "SCAN"
    const val CMD_DISCONNECT_CURRENT = "DISCONNECT"
    const val CMD_FORGET_PREFIX = "FORGET:"
    const val CMD_SWITCH_PREFIX = "SWITCH:"
    const val CMD_STATUS_REFRESH = "STATUS"
}
