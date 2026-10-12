package com.example.protocol

import java.util.UUID

object BleConstants {
    // Exact 128-bit UUIDs specified in specification
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    val CHAR_WRITE_COMMAND_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    val CHAR_STATUS_NOTIFY_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")

    // Client Characteristic Configuration Descriptor (CCCD) for enabling notifications
    val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Initial default advertised name filter for factory Raspberry Pi
    const val DEFAULT_ADVERTISED_NAME_PREFIX = "FactoryDevice-"
    const val DEFAULT_TARGET_NAME = "FactoryDevice-A123"

    // Scan settings
    const val SCAN_TIMEOUT_MILLIS = 15000L
    const val REQUEST_MTU_SIZE = 512
    const val DEFAULT_ATT_MTU = 23
    const val ATT_HEADER_OVERHEAD = 3
}
