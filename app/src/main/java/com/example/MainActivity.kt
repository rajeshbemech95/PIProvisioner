package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ProvisioningViewModel
import com.example.ui.components.AvailableNetworksSection
import com.example.ui.components.DeviceDiscoveryCard
import com.example.ui.components.ForgetConfirmationDialog
import com.example.ui.components.PermissionBanner
import com.example.ui.components.ProvisioningCard
import com.example.ui.components.SavedNetworksSection
import com.example.ui.components.TopHeaderSection
import com.example.ui.components.WifiStatusCard
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.PiProvisionerTheme
import com.example.ble.BleConnectionInfo
import com.example.ble.BleConnectionState
import com.example.ble.BleDeviceItem
import com.example.protocol.ProvisioningResponse
import com.example.protocol.SavedNetworkItem
import com.example.protocol.WifiScanItem

class MainActivity : ComponentActivity() {

    private val viewModel: ProvisioningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PiProvisionerTheme {
                ProvisioningAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ProvisioningAppScreen(
    viewModel: ProvisioningViewModel,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // States from ViewModel
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val isHardwareBleAvailable = viewModel.isHardwareBleAvailable
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectionInfo by viewModel.connectionInfo.collectAsStateWithLifecycle()
    val currentStatus by viewModel.currentStatus.collectAsStateWithLifecycle()
    val scannedNetworks by viewModel.scannedNetworks.collectAsStateWithLifecycle()
    val savedNetworks by viewModel.savedNetworks.collectAsStateWithLifecycle()
    val isProvisioning by viewModel.isProvisioningInProgress.collectAsStateWithLifecycle()
    val isScanningWifi by viewModel.isScanningWifiInProgress.collectAsStateWithLifecycle()
    val isFetchingSaved by viewModel.isFetchingSavedNetworksInProgress.collectAsStateWithLifecycle()
    val lastResponseLog by viewModel.lastResponseLog.collectAsStateWithLifecycle()

    val ssidInput by viewModel.ssidInput.collectAsStateWithLifecycle()
    val passwordInput by viewModel.passwordInput.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()
    val validationError by viewModel.validationError.collectAsStateWithLifecycle()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
    val networkToForget by viewModel.networkToForget.collectAsStateWithLifecycle()

    // Show feedback messages in snackbar
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("provisioning_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NavyDarkest
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NavyDarkest)
                .testTag("provisioning_content_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header & Mode Selector
            item {
                TopHeaderSection(
                    isDemoMode = isDemoMode,
                    isHardwareBleAvailable = isHardwareBleAvailable,
                    onToggleDemoMode = { viewModel.setDemoMode(it) }
                )
            }

            // 2. Android Bluetooth & Location Permission Banner
            item {
                PermissionBanner(
                    onPermissionsGranted = {
                        viewModel.startBleScan()
                    }
                )
            }

            // 3. Device Discovery & BLE Connection Controls
            item {
                DeviceDiscoveryCard(
                    discoveredDevices = discoveredDevices,
                    connectionInfo = connectionInfo,
                    onStartScan = { viewModel.startBleScan() },
                    onStopScan = { viewModel.stopBleScan() },
                    onConnectDevice = { viewModel.connectToDevice(it) },
                    onDisconnect = { viewModel.disconnectDevice() }
                )
            }

            // 4. Wi-Fi Information & Status Labeling (Active SSID, IPv4 address, Last Response)
            item {
                WifiStatusCard(
                    status = currentStatus,
                    lastResponseLog = lastResponseLog,
                    isConnectedBle = connectionInfo.isReadyForProvisioning,
                    onRefreshStatus = { viewModel.refreshWifiStatus() }
                )
            }

            // 5. Wi-Fi Provisioning Dashboard (SSID & Password Input, Validation, Send Button)
            item {
                ProvisioningCard(
                    ssid = ssidInput,
                    password = passwordInput,
                    isPasswordVisible = isPasswordVisible,
                    isProvisioning = isProvisioning,
                    isBleReady = connectionInfo.isReadyForProvisioning,
                    validationError = validationError,
                    feedbackMessage = userFeedbackMessage,
                    discoveredDevices = discoveredDevices,
                    selectedDevice = viewModel.selectedDevice.collectAsStateWithLifecycle().value,
                    onSsidChange = { viewModel.onSsidChanged(it) },
                    onPasswordChange = { viewModel.onPasswordChanged(it) },
                    onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
                    onSendCredentials = { viewModel.sendWifiCredentials() },
                    onSelectDevice = { viewModel.onSelectDevice(it) }
                )
            }

            // 6. Network Management: Wi-Fi Survey on Raspberry Pi
            item {
                AvailableNetworksSection(
                    networks = scannedNetworks,
                    isScanning = isScanningWifi,
                    isBleReady = connectionInfo.isReadyForProvisioning,
                    onTriggerScan = { viewModel.triggerPiWifiScan() },
                    onSelectNetwork = { viewModel.onSelectScannedNetwork(it) }
                )
            }

            // 7. Network Management: Saved Profiles, Switch & Disconnect
            item {
                SavedNetworksSection(
                    savedNetworks = savedNetworks,
                    isFetching = isFetchingSaved,
                    isBleReady = connectionInfo.isReadyForProvisioning,
                    onRefreshSaved = { viewModel.refreshSavedNetworks() },
                    onDisconnectCurrent = { viewModel.disconnectCurrentWifi() },
                    onSwitchNetwork = { viewModel.switchToSavedNetwork(it) },
                    onRequestForget = { viewModel.requestForgetNetwork(it) }
                )
            }
        }
    }

    // Confirmation dialog for forgetting saved Wi-Fi profile
    networkToForget?.let { ssid ->
        ForgetConfirmationDialog(
            ssid = ssid,
            onConfirm = { viewModel.confirmForgetNetwork() },
            onDismiss = { viewModel.dismissForgetDialog() }
        )
    }
}
