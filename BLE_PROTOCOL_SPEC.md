# Raspberry Pi BLE Wi-Fi Provisioning Protocol Specification

**Document Version:** 1.0  
**Framing Protocol Version:** `0x01`  
**Target Hardware:** Raspberry Pi 4 Model B (Linux / BlueZ) & Native Android App (API 26+)

---

## 1. BLE GATT Architecture

### 1.1 Service and Characteristics

The Raspberry Pi peripheral exposes a single primary GATT service with two characteristics:

| Entity | 128-Bit UUID | Properties | Description |
|---|---|---|---|
| **Provisioning Service** | `12345678-1234-5678-1234-56789abcdef0` | Primary Service | Main entry point for provisioning |
| **Command Write Characteristic** | `12345678-1234-5678-1234-56789abcdef1` | `WRITE`, `WRITE_NO_RESPONSE` | Client writes framed JSON command chunks |
| **Status Notify Characteristic** | `12345678-1234-5678-1234-56789abcdef2` | `READ`, `NOTIFY`, `INDICATE` | Server sends framed status notifications |
| **Client Characteristic Config (CCCD)** | `00002902-0000-1000-8000-00805f9b34fb` | `READ`, `WRITE` | Standard descriptor to enable notifications |

### 1.2 Advertising Configuration
- **Initial Device Name:** `FactoryDevice-A123` (Configurable prefix: `FactoryDevice-`)
- **Advertising Data:** Complete Local Name + Service UUID `12345678-1234-5678-1234-56789abcdef0`
- **Scan Response:** Manufacturer Data (Model identifier, firmware version)

---

## 2. Binary Framing Protocol (Version 0x01)

Due to BLE ATT MTU constraints (defaulting to 23 bytes, effective 20-byte payload), arbitrary JSON strings must be split into framed chunks and reassembled deterministically.

### 2.1 Frame Structure

Each frame transmitted over GATT consists of a 5-byte binary header followed by UTF-8 JSON payload:

```
 0                   1                   2                   3                   4
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Version 0x01 |     Flags     |  Msg Seq ID   |  Chunk Index  | Total Chunks  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| Bytes 5 .. N: UTF-8 JSON Payload Segment ...                                  |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 2.2 Field Definitions
1. **Byte 0 (Version):** Constant `0x01`. Frames with mismatching versions are discarded.
2. **Byte 1 (Flags):**
   - Bit 7 (`0x80`): `START_FLAG` (Set on the first chunk of a message).
   - Bit 6 (`0x40`): `END_FLAG` (Set on the last chunk of a message).
   - Bits 5..0: Message Type (`0x01` = JSON Payload, `0x02` = Heartbeat, `0x03` = ACK).
   - *Single chunk message:* Flags = `0x80 | 0x40 = 0xC0`.
3. **Byte 2 (Message Sequence ID):** Rolling `0..255` counter identifying the logical message.
4. **Byte 3 (Chunk Index):** 0-indexed position (`0, 1, ..., Total - 1`).
5. **Byte 4 (Total Chunks):** Total count of chunks in this message (`1..255`).
6. **Bytes 5..N:** Raw chunk payload.

### 2.3 Frame Validation & Reassembly Rules
- **Maximum Message Size:** 65,536 bytes (64 KB limit to prevent DoS attacks).
- **Sequential Continuity:** Chunks must arrive with matching Sequence ID and sequential indices (`index == previous_index + 1`).
- **Timeout:** If an in-progress multi-chunk message is not completed within 5.0 seconds, buffers are reset and a timeout error is raised.

---

## 3. JSON Command & Response Schema

### 3.1 Provision Wi-Fi Credentials (`wifi_connect`)
**Client Request:**
```json
{
  "cmd": "wifi_connect",
  "ssid": "Factory_Line_HQ",
  "password": "UltraSecretPassword123",
  "reqId": "req_1001"
}
```

**Pi Intermediate Notification (Connecting):**
```json
{
  "status": "connecting",
  "reqId": "req_1001",
  "ssid": "Factory_Line_HQ",
  "message": "Authenticating with AP and requesting DHCP lease..."
}
```

**Pi Final Notification (Success):**
```json
{
  "status": "connected",
  "reqId": "req_1001",
  "ssid": "Factory_Line_HQ",
  "ip": "192.168.1.150",
  "message": "Wi-Fi link up"
}
```

**Pi Final Notification (Error):**
```json
{
  "status": "error",
  "reqId": "req_1001",
  "error": "Authentication failed / incorrect password"
}
```

---

### 3.2 Wi-Fi Survey / Channel Scan (`wifi_scan`)
**Client Request:**
```json
{
  "cmd": "wifi_scan",
  "reqId": "req_1002"
}
```

**Pi Response Notification:**
```json
{
  "status": "scan_results",
  "reqId": "req_1002",
  "networks": [
    {
      "ssid": "Warehouse_5G",
      "rssi": -48,
      "security": "WPA2/WPA3",
      "channel": 36,
      "connected": false
    },
    {
      "ssid": "Factory_Line_HQ",
      "rssi": -62,
      "security": "WPA2-PSK",
      "channel": 6,
      "connected": true
    }
  ]
}
```

---

### 3.3 Query Status (`wifi_status`)
**Client Request:**
```json
{
  "cmd": "wifi_status",
  "reqId": "req_1003"
}
```

**Pi Response Notification:**
```json
{
  "status": "connected",
  "reqId": "req_1003",
  "ssid": "Factory_Line_HQ",
  "ip": "192.168.1.150"
}
```

---

### 3.4 Query Saved Network Profiles (`wifi_saved_networks`)
**Client Request:**
```json
{
  "cmd": "wifi_saved_networks",
  "reqId": "req_1004"
}
```

**Pi Response Notification:**
```json
{
  "status": "saved_networks",
  "reqId": "req_1004",
  "saved": [
    {
      "ssid": "Factory_Line_HQ",
      "active": true,
      "lastSeen": "Just now"
    },
    {
      "ssid": "Backup_LTE_WiFi",
      "active": false,
      "lastSeen": "Yesterday"
    }
  ]
}
```

---

### 3.5 Disconnect Active Wi-Fi (`wifi_disconnect`)
**Client Request:**
```json
{
  "cmd": "wifi_disconnect",
  "reqId": "req_1005"
}
```

---

### 3.6 Forget Saved Profile (`wifi_forget`)
**Client Request:**
```json
{
  "cmd": "wifi_forget",
  "ssid": "Backup_LTE_WiFi",
  "reqId": "req_1006"
}
```

---

### 3.7 Switch to Saved Profile (`wifi_switch`)
**Client Request:**
```json
{
  "cmd": "wifi_switch",
  "ssid": "Backup_LTE_WiFi",
  "reqId": "req_1007"
}
```

---

## 4. Security & Hardening Guidelines

1. **In-Memory Credential Scrubbing:** Passwords are never saved in SharedPreferences, Room, or logged. The password buffer in the UI is zeroed immediately upon transmission or BLE disconnect.
2. **GATT Encryption & Security:** For production builds, configure BlueZ `/etc/bluetooth/main.conf` with `LE_SECURITY_MODE_1_LEVEL_2` or `LEVEL_3` requiring Just-Works or Passkey numeric pairing before characteristic writes are permitted.
3. **Replay Protection:** Unique `reqId` and rolling message sequence counter `MsgSeqID` prevent duplicate frame injections.
4. **Transport Layer Isolation:** No internet permissions are used for provisioning; all interactions occur purely over local BLE GATT.
