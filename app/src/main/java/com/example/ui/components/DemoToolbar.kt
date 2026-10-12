package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.RaspberryRed

@Composable
fun DemoToolbar(
    isDemoMode: Boolean,
    isConnected: Boolean,
    onToggleDemoMode: (Boolean) -> Unit,
    onTestConnectionDrop: () -> Unit,
    onToggleEthernetDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("demo_toolbar_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Test Mode",
                        tint = if (isDemoMode) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isDemoMode) "Simulated Pi Mode" else "Physical BLE Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDemoMode) "Virtual Pi 5 environment" else "Live Android Bluetooth LE hardware",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isDemoMode,
                    onCheckedChange = onToggleDemoMode,
                    modifier = Modifier.testTag("demo_mode_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyberCyan
                    )
                )
            }

            AnimatedVisibility(visible = isDemoMode && isConnected) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Hardware Simulation Lab:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestConnectionDrop,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("simulate_connection_drop_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AmberWarning
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Simulate Drop", fontSize = 11.sp, color = AmberWarning)
                        }

                        OutlinedButton(
                            onClick = onToggleEthernetDemo,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("simulate_ethernet_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lan,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Plug Ethernet", fontSize = 11.sp, color = CyberCyan)
                        }
                    }
                }
            }
        }
    }
}
