package com.example.protocol

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Message models for JSON payloads exchanged between the Android app
 * and the Raspberry Pi provisioning service over BLE.
 */
sealed class ProvisioningCommand(
    val cmd: String,
    val reqId: String = UUID.randomUUID().toString().substring(0, 8)
) {
    class ConnectWifi(val ssid: String, val password: String, reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_connect", reqId)

    class ScanWifi(reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_scan", reqId)

    class GetStatus(reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_status", reqId)

    class GetSavedNetworks(reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_saved_networks", reqId)

    class DisconnectWifi(reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_disconnect", reqId)

    class ForgetWifi(val ssid: String, reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_forget", reqId)

    class SwitchWifi(val ssid: String, reqId: String = UUID.randomUUID().toString().substring(0, 8)) :
        ProvisioningCommand("wifi_switch", reqId)

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("cmd", cmd)
        obj.put("reqId", reqId)
        when (this) {
            is ConnectWifi -> {
                obj.put("ssid", ssid)
                obj.put("password", password)
            }
            is ForgetWifi -> {
                obj.put("ssid", ssid)
            }
            is SwitchWifi -> {
                obj.put("ssid", ssid)
            }
            else -> {}
        }
        return obj.toString()
    }
}

data class WifiScanItem(
    val ssid: String,
    val rssi: Int,
    val security: String = "WPA2-PSK",
    val channel: Int = 6,
    val isConnected: Boolean = false
)

data class SavedNetworkItem(
    val ssid: String,
    val isActive: Boolean = false,
    val lastSeen: String = "Configured"
)

data class ProvisioningResponse(
    val status: String,
    val reqId: String? = null,
    val ssid: String? = null,
    val ip: String? = null,
    val error: String? = null,
    val message: String? = null,
    val networks: List<WifiScanItem> = emptyList(),
    val savedNetworks: List<SavedNetworkItem> = emptyList(),
    val rawJson: String = ""
) {
    val isConnected: Boolean
        get() = status.equals("connected", ignoreCase = true) && !ip.isNullOrBlank()

    val isError: Boolean
        get() = status.equals("error", ignoreCase = true) || !error.isNullOrBlank()

    companion object {
        fun fromJson(jsonStr: String): ProvisioningResponse {
            val json = JSONObject(jsonStr)
            val status = json.optString("status", "unknown")
            val reqId = json.optString("reqId").ifBlank { null }
            val ssid = json.optString("ssid").ifBlank { null }
            val ip = json.optString("ip").ifBlank { null }
            val error = json.optString("error").ifBlank { null }
            val message = json.optString("message").ifBlank { null }

            val networks = mutableListOf<WifiScanItem>()
            val networksArr = json.optJSONArray("networks")
            if (networksArr != null) {
                for (i in 0 until networksArr.length()) {
                    val item = networksArr.getJSONObject(i)
                    networks.add(
                        WifiScanItem(
                            ssid = item.optString("ssid", "Unknown"),
                            rssi = item.optInt("rssi", -70),
                            security = item.optString("security", "WPA2-PSK"),
                            channel = item.optInt("channel", 6),
                            isConnected = item.optBoolean("connected", false) || (ssid != null && item.optString("ssid") == ssid)
                        )
                    )
                }
            }

            val saved = mutableListOf<SavedNetworkItem>()
            val savedArr = json.optJSONArray("saved")
            if (savedArr != null) {
                for (i in 0 until savedArr.length()) {
                    val item = savedArr.getJSONObject(i)
                    saved.add(
                        SavedNetworkItem(
                            ssid = item.optString("ssid", "Unknown"),
                            isActive = item.optBoolean("active", false) || (ssid != null && item.optString("ssid") == ssid),
                            lastSeen = item.optString("lastSeen", "Configured")
                        )
                    )
                }
            }

            return ProvisioningResponse(
                status = status,
                reqId = reqId,
                ssid = ssid,
                ip = ip,
                error = error,
                message = message,
                networks = networks,
                savedNetworks = saved,
                rawJson = jsonStr
            )
        }
    }
}
