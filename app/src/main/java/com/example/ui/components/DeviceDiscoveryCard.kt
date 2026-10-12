package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleConnectionInfo
import com.example.ble.BleConnectionState
import com.example.ble.BleDeviceItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.RedAlert
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DeviceDiscoveryCard(
    discoveredDevices: List<BleDeviceItem>,
    connectionInfo: BleConnectionInfo,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (BleDeviceItem) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning = connectionInfo.state == BleConnectionState.SCANNING
    val isConnected = connectionInfo.state == BleConnectionState.CONNECTED
    val isConnecting = connectionInfo.state == BleConnectionState.CONNECTING
    val isFailed = connectionInfo.state == BleConnectionState.FAILED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_discovery_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Connection State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BLE Device Discovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target: FactoryDevice-A123",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealMuted
                        )
                    }
                }

                // Connection status chip
                ConnectionStatePill(state = connectionInfo.state)
            }

            // Error alert if failed
            AnimatedVisibility(visible = isFailed && connectionInfo.errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = RedAlert.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionInfo.errorMessage ?: "Connection failure",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Connected Device Banner or Scan Controls
            if (isConnected && connectionInfo.connectedDevice != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SuccessEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = connectionInfo.connectedDevice.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = connectionInfo.connectedDevice.address,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "MTU: ${connectionInfo.negotiatedMtu} • GATT Provisioning Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealMuted
                                )
                            }
                        }

                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("disconnect_ble_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Disconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Scan / Stop Scan Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isScanning) "Scanning 2.4GHz BLE channels (15s timeout)..." else "${discoveredDevices.size} BLE Peripherals nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = {
                            if (isScanning) onStopScan() else onStartScan()
                        },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("scan_ble_devices_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) RedAlert else TealAccent,
                            contentColor = if (isScanning) Color.White else NavyDarkest
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Stop Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Scan BLE Devices", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Discovered Devices List
                if (discoveredDevices.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NavySurfaceElevated.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isScanning) "Listening for FactoryDevice BLE advertisements..." else "No devices discovered yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Power on the Raspberry Pi 4 to broadcast provisioning advertisement.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        discoveredDevices.forEach { device ->
                            val isConnectingThis = isConnecting && connectionInfo.connectedDevice?.address == device.address

                            DiscoveredDeviceRow(
                                device = device,
                                isConnecting = isConnectingThis,
                                onConnect = { onConnectDevice(device) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredDeviceRow(
    device: BleDeviceItem,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_item_${device.address}")
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isConnecting) { onConnect() },
        shape = RoundedCornerShape(12.dp),
        color = if (device.isFactoryPi) NavySurfaceElevated else NavySurfaceElevated.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (device.isFactoryPi) TealAccent.copy(alpha = 0.4f) else NavyBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (device.isFactoryPi) TealAccent.copy(alpha = 0.15f) else NavyDarkest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (device.isFactoryPi) TealAccent else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (device.isFactoryPi) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FACTORY PI",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TealAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("connect_device_button_${device.address}"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent,
                    contentColor = NavyDarkest
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NavyDarkest
                    )
                } else {
                    Text(text = "Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatePill(state: BleConnectionState) {
    val (color, label) = when (state) {
        BleConnectionState.CONNECTED -> Pair(SuccessEmerald, "Connected")
        BleConnectionState.CONNECTING -> Pair(AmberWarning, "Connecting...")
        BleConnectionState.SCANNING -> Pair(ElectricBlue, "Scanning...")
        BleConnectionState.FAILED -> Pair(RedAlert, "Failed")
        BleConnectionState.DISCONNECTED -> Pair(TextMuted, "Disconnected")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
