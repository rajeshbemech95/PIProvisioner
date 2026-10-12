package com.example

import com.example.protocol.ProvisioningCommand
import com.example.protocol.ProvisioningResponse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMessageTest {

    @Test
    fun testConnectWifiCommandSerialization() {
        val cmd = ProvisioningCommand.ConnectWifi(
            ssid = "Factory_WiFi_HQ",
            password = "SecretPassword123",
            reqId = "req_test_01"
        )
        val jsonStr = cmd.toJson()
        val obj = JSONObject(jsonStr)

        assertEquals("wifi_connect", obj.getString("cmd"))
        assertEquals("Factory_WiFi_HQ", obj.getString("ssid"))
        assertEquals("SecretPassword123", obj.getString("password"))
        assertEquals("req_test_01", obj.getString("reqId"))
    }

    @Test
    fun testScanWifiCommandSerialization() {
        val cmd = ProvisioningCommand.ScanWifi(reqId = "req_scan_99")
        val jsonStr = cmd.toJson()
        val obj = JSONObject(jsonStr)

        assertEquals("wifi_scan", obj.getString("cmd"))
        assertEquals("req_scan_99", obj.getString("reqId"))
    }

    @Test
    fun testProvisioningResponseParsingConnected() {
        val responseJson = """
            {
              "status": "connected",
              "ssid": "Factory_Line_IoT",
              "ip": "192.168.1.50",
              "reqId": "req_101",
              "message": "Wi-Fi link up"
            }
        """.trimIndent()

        val resp = ProvisioningResponse.fromJson(responseJson)
        assertEquals("connected", resp.status)
        assertEquals("Factory_Line_IoT", resp.ssid)
        assertEquals("192.168.1.50", resp.ip)
        assertTrue(resp.isConnected)
        assertFalse(resp.isError)
    }

    @Test
    fun testProvisioningResponseParsingScanResults() {
        val scanJson = """
            {
              "status": "scan_results",
              "reqId": "req_scan_01",
              "networks": [
                {"ssid": "Net_5G", "rssi": -48, "security": "WPA2-PSK", "channel": 36},
                {"ssid": "Guest_Net", "rssi": -78, "security": "Open", "channel": 1}
              ]
            }
        """.trimIndent()

        val resp = ProvisioningResponse.fromJson(scanJson)
        assertEquals("scan_results", resp.status)
        assertEquals(2, resp.networks.size)
        assertEquals("Net_5G", resp.networks[0].ssid)
        assertEquals(-48, resp.networks[0].rssi)
        assertEquals("Open", resp.networks[1].security)
    }

    @Test
    fun testProvisioningResponseParsingSavedNetworks() {
        val savedJson = """
            {
              "status": "saved_networks",
              "saved": [
                {"ssid": "Factory_Line_IoT", "active": true, "lastSeen": "Just now"},
                {"ssid": "Old_Lab_Net", "active": false, "lastSeen": "3 days ago"}
              ]
            }
        """.trimIndent()

        val resp = ProvisioningResponse.fromJson(savedJson)
        assertEquals("saved_networks", resp.status)
        assertEquals(2, resp.savedNetworks.size)
        assertEquals("Factory_Line_IoT", resp.savedNetworks[0].ssid)
        assertTrue(resp.savedNetworks[0].isActive)
        assertFalse(resp.savedNetworks[1].isActive)
    }

    @Test
    fun testProvisioningResponseParsingError() {
        val errJson = """
            {
              "status": "error",
              "error": "Authentication failed: invalid passphrase",
              "reqId": "req_err"
            }
        """.trimIndent()

        val resp = ProvisioningResponse.fromJson(errJson)
        assertEquals("error", resp.status)
        assertTrue(resp.isError)
        assertEquals("Authentication failed: invalid passphrase", resp.error)
    }
}
