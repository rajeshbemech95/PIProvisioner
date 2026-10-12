package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BleConnectionState
import com.example.data.model.BleConnectionStatus
import com.example.data.model.DiscoveredPiDevice
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RaspberryRed
import com.example.ui.theme.RoseError

@Composable
fun DeviceScannerSection(
    devices: List<DiscoveredPiDevice>,
    isScanning: Boolean,
    connectionState: BleConnectionState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (DiscoveredPiDevice) -> Unit,
    onRetryConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_scanner_section")
    ) {
        // Reconnecting Alert Banner (Connection Drop handling)
        AnimatedVisibility(
            visible = connectionState.status == BleConnectionStatus.RECONNECTING ||
                    (connectionState.status == BleConnectionStatus.ERROR && connectionState.errorMessage != null)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("reconnection_alert_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (connectionState.status == BleConnectionStatus.RECONNECTING) {
                        AmberWarning.copy(alpha = 0.15f)
                    } else {
                        RoseError.copy(alpha = 0.15f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (connectionState.status == BleConnectionStatus.RECONNECTING) AmberWarning else RoseError
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (connectionState.status == BleConnectionStatus.RECONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AmberWarning
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Connection Warning",
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (connectionState.status == BleConnectionStatus.RECONNECTING) {
                                stringResource(R.string.reconnecting)
                            } else {
                                stringResource(R.string.connection_lost)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = connectionState.errorMessage ?: "Attempting automatic recovery...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (connectionState.status == BleConnectionStatus.ERROR) {
                        OutlinedButton(
                            onClick = onRetryConnect,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("retry_connect_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = stringResource(R.string.action_reconnect), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Header with Scan controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.title_scanner),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isScanning) stringResource(R.string.scanner_searching) else "${devices.size} device(s) found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (isScanning) onStopScan() else onStartScan()
                },
                modifier = Modifier
                    .height(40.dp)
                    .testTag("scan_toggle_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.error else RaspberryRed
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.scan_button_stop), fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = "Scan",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.scan_button_start), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device List or Empty State
        if (devices.isEmpty()) {
            EmptyScannerCard(isScanning = isScanning, onStartScan = onStartScan)
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                devices.forEach { device ->
                    val isConnectingThis = connectionState.status == BleConnectionStatus.CONNECTING &&
                            connectionState.connectedDevice?.id == device.id

                    DeviceItemCard(
                        device = device,
                        isConnecting = isConnectingThis,
                        onConnect = { onConnectDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    device: DiscoveredPiDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_item_${device.id}")
            .clickable(enabled = !isConnecting) { onConnect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hardware Pi Icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RaspberryRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Raspberry Pi",
                    tint = RaspberryRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.model,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = device.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalIndicator(rssi = device.rssi)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Connect button
            Button(
                onClick = onConnect,
                enabled = !isConnecting,
                modifier = Modifier
                    .height(38.dp)
                    .testTag("connect_button_${device.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaspberryRed
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.action_connect),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScannerCard(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_scanner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RaspberryRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Sensors else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = RaspberryRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isScanning) "Searching for Advertising Pis..." else stringResource(R.string.scanner_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.scanner_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (!isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.testTag("start_scan_empty_state_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RaspberryRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Start Scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.scan_button_start))
                }
            }
        }
    }
}
