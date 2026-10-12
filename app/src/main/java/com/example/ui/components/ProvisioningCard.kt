package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.example.ble.BleDeviceItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningCard(
    ssid: String,
    password: String,
    isPasswordVisible: Boolean,
    isProvisioning: Boolean,
    isBleReady: Boolean,
    validationError: String?,
    feedbackMessage: String?,
    discoveredDevices: List<BleDeviceItem>,
    selectedDevice: BleDeviceItem?,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSendCredentials: () -> Unit,
    onSelectDevice: (BleDeviceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("provisioning_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ... (existing code, unchanged) ...
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Factory Wi-Fi Provisioning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select Target → Provision",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Device Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDevice?.name ?: "Select Target Raspberry Pi",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("device_selector_dropdown"),
                    label = { Text("Target Device") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        focusedTextColor = TextPrimary
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    discoveredDevices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.name) },
                            onClick = {
                                expanded = false
                                onSelectDevice(device)
                            }
                        )
                    }
                }
            }

            // Disabled warning if not ready
            if (!isBleReady) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = NavySurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connect to Factory Raspberry Pi over BLE to enable credentials provisioning.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Validation Error Alert
            AnimatedVisibility(visible = validationError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            // SSID Input
            OutlinedTextField(
                value = ssid,
                onValueChange = onSsidChange,
                enabled = isBleReady && !isProvisioning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ssid_input_field"),
                label = { Text("Network SSID") },
                placeholder = { Text("e.g. Factory_WiFi_5G") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "SSID Icon",
                        tint = if (isBleReady) TealAccent else TextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = NavyBorder,
                    focusedLabelColor = TealAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextMuted,
                    disabledBorderColor = NavyBorder.copy(alpha = 0.4f),
                    focusedContainerColor = NavySurfaceElevated,
                    unfocusedContainerColor = NavySurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Input with Masking & Toggle
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                enabled = isBleReady && !isProvisioning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input_field"),
                label = { Text("WPA2/WPA3 Password") },
                placeholder = { Text("Enter Wi-Fi security key") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = if (isBleReady) TealAccent else TextMuted
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        enabled = isBleReady && !isProvisioning,
                        modifier = Modifier.testTag("toggle_password_visibility_button")
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = if (isBleReady) TealMuted else TextMuted
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSendCredentials()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = NavyBorder,
                    focusedLabelColor = TealAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextMuted,
                    disabledBorderColor = NavyBorder.copy(alpha = 0.4f),
                    focusedContainerColor = NavySurfaceElevated,
                    unfocusedContainerColor = NavySurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Send Credentials Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSendCredentials()
                },
                enabled = isBleReady && !isProvisioning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_credentials_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent,
                    contentColor = NavyDarkest,
                    disabledContainerColor = NavyBorder,
                    disabledContentColor = TextMuted
                )
            ) {
                if (isProvisioning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = NavyDarkest
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configuring Raspberry Pi...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Credentials Over BLE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Feedback Message
            AnimatedVisibility(visible = feedbackMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = TealAccent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TealMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Security note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SuccessEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Security: Password is wiped from memory upon send/disconnect.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
