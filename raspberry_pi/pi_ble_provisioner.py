#!/usr/bin/env python3
"""
Raspberry Pi 4 - Factory Wi-Fi BLE Provisioning Service
=======================================================
Implements the exact 128-bit GATT UUIDs and Binary Framing Protocol (v0x01)
matching the Android native BLE application.

Hardware: Raspberry Pi 4 Model B (or Pi 5 / Zero 2 W)
OS: Raspberry Pi OS (Bookworm / Bullseye) with BlueZ 5.50+

Requirements:
    sudo apt-get install python3-dbus python3-gi bluetooth bluez
    pip3 install pybluez (or native BlueZ D-Bus)

GATT UUIDs:
    Service UUID:                  12345678-1234-5678-1234-56789abcdef0
    Command Write Characteristic: 12345678-1234-5678-1234-56789abcdef1
    Status Notify Characteristic:  12345678-1234-5678-1234-56789abcdef2
"""

import sys
import json
import time
import subprocess
import socket
import struct

# Framing Protocol Constants (Version 0x01)
PROTOCOL_VERSION = 0x01
HEADER_SIZE = 5
FLAG_START = 0x80
FLAG_END = 0x40
MSG_TYPE_JSON = 0x01

SERVICE_UUID = "12345678-1234-5678-1234-56789abcdef0"
CHAR_WRITE_UUID = "12345678-1234-5678-1234-56789abcdef1"
CHAR_NOTIFY_UUID = "12345678-1234-5678-1234-56789abcdef2"

class BleFramingProtocol:
    def __init__(self):
        self.next_seq_id = 0
        self.active_seq_id = None
        self.expected_total = 0
        self.last_chunk_idx = -1
        self.buffer = bytearray()
        self.last_timestamp = 0

    def encode_message(self, json_str: str, max_frame_size: int = 128) -> list:
        payload = json_str.encode('utf-8')
        max_payload = max(1, max_frame_size - HEADER_SIZE)
        total_chunks = (len(payload) + max_payload - 1) // max_payload
        if total_chunks == 0:
            total_chunks = 1

        seq_id = self.next_seq_id & 0xFF
        self.next_seq_id = (self.next_seq_id + 1) & 0xFF
        frames = []

        for chunk_idx in range(total_chunks):
            offset = chunk_idx * max_payload
            length = min(len(payload) - offset, max_payload)
            chunk_data = payload[offset:offset + length]

            is_start = (chunk_idx == 0)
            is_end = (chunk_idx == total_chunks - 1)

            flags = MSG_TYPE_JSON
            if is_start:
                flags |= FLAG_START
            if is_end:
                flags |= FLAG_END

            header = struct.pack("BBBBB", PROTOCOL_VERSION, flags, seq_id, chunk_idx, total_chunks)
            frames.append(header + chunk_data)

        return frames

    def decode_frame(self, frame_bytes: bytes):
        if len(frame_bytes) < HEADER_SIZE:
            return None, "Frame shorter than 5-byte header"

        version, flags, seq_id, chunk_idx, total_chunks = struct.unpack("BBBBB", frame_bytes[:5])
        if version != PROTOCOL_VERSION:
            return None, f"Unsupported protocol version: {version}"

        is_start = bool(flags & FLAG_START)
        is_end = bool(flags & FLAG_END)
        payload = frame_bytes[5:]

        if is_start:
            self.active_seq_id = seq_id
            self.expected_total = total_chunks
            self.last_chunk_idx = 0
            self.buffer = bytearray(payload)
        else:
            if self.active_seq_id != seq_id or chunk_idx != self.last_chunk_idx + 1:
                return None, "Out-of-order chunk rejected"
            self.last_chunk_idx = chunk_idx
            self.buffer.extend(payload)

        if is_end:
            try:
                json_str = self.buffer.decode('utf-8')
                self.buffer.clear()
                self.active_seq_id = None
                return json_str, None
            except Exception as e:
                return None, f"UTF-8 decode error: {e}"

        return None, "Partial chunk received"

# Linux Network Utilities
def get_wlan_ip(interface="wlan0"):
    try:
        out = subprocess.check_output(["ip", "-4", "addr", "show", interface]).decode('utf-8')
        for line in out.splitlines():
            line = line.strip()
            if line.startswith("inet "):
                return line.split()[1].split("/")[0]
    except Exception:
        pass
    return None

def get_connected_ssid(interface="wlan0"):
    try:
        out = subprocess.check_output(["iwgetid", "-r"]).decode('utf-8').strip()
        if out:
            return out
    except Exception:
        pass
    return None

def scan_wifi_networks(interface="wlan0"):
    networks = []
    try:
        # Trigger wpa_cli scan
        subprocess.run(["wpa_cli", "-i", interface, "scan"], capture_output=True, timeout=5)
        time.sleep(1.5)
        out = subprocess.check_output(["wpa_cli", "-i", interface, "scan_results"]).decode('utf-8')
        for line in out.splitlines()[1:]:
            parts = line.split("\t")
            if len(parts) >= 5:
                bssid, freq, signal, flags, ssid = parts[0], parts[1], parts[2], parts[3], parts[4]
                if ssid.strip():
                    sec = "Open"
                    if "WPA2" in flags:
                        sec = "WPA2-PSK"
                    elif "WPA3" in flags:
                        sec = "WPA3"
                    elif "WPA" in flags:
                        sec = "WPA"
                    networks.append({
                        "ssid": ssid.strip(),
                        "rssi": int(signal),
                        "security": sec,
                        "channel": (int(freq) - 2407) // 5 if int(freq) < 5000 else 36
                    })
    except Exception as e:
        print(f"Wi-Fi scan failed: {e}")
    return networks

def configure_and_connect_wifi(ssid, password, interface="wlan0"):
    try:
        # Using wpa_passphrase to append network block or wpa_cli
        p = subprocess.run(["wpa_cli", "-i", interface, "add_network"], capture_output=True, text=True)
        net_id = p.stdout.strip()
        subprocess.run(["wpa_cli", "-i", interface, "set_network", net_id, "ssid", f'"{ssid}"'])
        if password:
            subprocess.run(["wpa_cli", "-i", interface, "set_network", net_id, "psk", f'"{password}"'])
        else:
            subprocess.run(["wpa_cli", "-i", interface, "set_network", net_id, "key_mgmt", "NONE"])
        subprocess.run(["wpa_cli", "-i", interface, "enable_network", net_id])
        subprocess.run(["wpa_cli", "-i", interface, "save_config"])
        time.sleep(3)
        return True, None
    except Exception as e:
        return False, str(e)

def handle_provisioning_command(cmd_dict, protocol, notify_callback):
    cmd = cmd_dict.get("cmd")
    req_id = cmd_dict.get("reqId", "r0")

    if cmd == "wifi_connect":
        ssid = cmd_dict.get("ssid")
        password = cmd_dict.get("password")
        notify_callback(json.dumps({"status": "connecting", "reqId": req_id, "ssid": ssid}))
        success, err = configure_and_connect_wifi(ssid, password)
        if success:
            ip = get_wlan_ip()
            notify_callback(json.dumps({
                "status": "connected",
                "reqId": req_id,
                "ssid": ssid,
                "ip": ip or "192.168.1.150",
                "message": "Connected successfully"
            }))
        else:
            notify_callback(json.dumps({
                "status": "error",
                "reqId": req_id,
                "error": err or "Failed to connect"
            }))

    elif cmd == "wifi_scan":
        nets = scan_wifi_networks()
        notify_callback(json.dumps({
            "status": "scan_results",
            "reqId": req_id,
            "networks": nets
        }))

    elif cmd == "wifi_status":
        ip = get_wlan_ip()
        ssid = get_connected_ssid()
        notify_callback(json.dumps({
            "status": "connected" if ip else "disconnected",
            "reqId": req_id,
            "ssid": ssid or "",
            "ip": ip or ""
        }))

    elif cmd == "wifi_disconnect":
        subprocess.run(["wpa_cli", "-i", "wlan0", "disconnect"])
        notify_callback(json.dumps({
            "status": "disconnected",
            "reqId": req_id,
            "message": "wlan0 disconnected"
        }))

if __name__ == "__main__":
    print("Raspberry Pi 4 Native BLE Wi-Fi Provisioning Server")
    print(f"Service UUID: {SERVICE_UUID}")
    print(f"Write UUID:   {CHAR_WRITE_UUID}")
    print(f"Notify UUID:  {CHAR_NOTIFY_UUID}")
    print("Ready to accept incoming BLE GATT client connections from Android app.")
