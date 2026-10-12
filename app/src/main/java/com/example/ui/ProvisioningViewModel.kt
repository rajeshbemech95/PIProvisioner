package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BleConnectionInfo
import com.example.ble.BleConnectionState
import com.example.ble.BleDeviceItem
import com.example.data.ProvisioningRepository
import com.example.protocol.ProvisioningResponse
import com.example.protocol.SavedNetworkItem
import com.example.protocol.WifiScanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProvisioningViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ProvisioningRepository(application, viewModelScope)

    val isHardwareBleAvailable = repository.isHardwareBleAvailable
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode

    val discoveredDevices: StateFlow<List<BleDeviceItem>> = repository.discoveredDevices
    val connectionInfo: StateFlow<BleConnectionInfo> = repository.connectionInfo
    val currentStatus: StateFlow<ProvisioningResponse?> = repository.currentStatus
    val scannedNetworks: StateFlow<List<WifiScanItem>> = repository.scannedNetworks
    val savedNetworks: StateFlow<List<SavedNetworkItem>> = repository.savedNetworks
    val isProvisioningInProgress: StateFlow<Boolean> = repository.isProvisioningInProgress
    val isScanningWifiInProgress: StateFlow<Boolean> = repository.isScanningWifiInProgress
    val isFetchingSavedNetworksInProgress: StateFlow<Boolean> = repository.isFetchingSavedNetworksInProgress
    val lastResponseLog: StateFlow<String> = repository.lastResponseLog

    // Form inputs & validation
    private val _ssidInput = MutableStateFlow("")
    val ssidInput: StateFlow<String> = _ssidInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    // Confirmation dialog state for forgetting network
    private val _networkToForget = MutableStateFlow<String?>(null)
    val networkToForget: StateFlow<String?> = _networkToForget.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BleDeviceItem?>(null)
    val selectedDevice: StateFlow<BleDeviceItem?> = _selectedDevice.asStateFlow()

    init {
        // Security requirement: Clear password when disconnected
        viewModelScope.launch {
            connectionInfo.collectLatest { info ->
                if (info.state == BleConnectionState.DISCONNECTED || info.state == BleConnectionState.FAILED) {
                    _passwordInput.value = ""
                    _selectedDevice.value = null
                }
            }
        }
    }

    fun onSelectDevice(device: BleDeviceItem) {
        _selectedDevice.value = device
        repository.connect(device)
    }

    fun onSsidChanged(ssid: String) {
        _ssidInput.value = ssid
        if (_validationError.value != null && ssid.isNotBlank()) {
            _validationError.value = null
        }
    }

    fun onPasswordChanged(password: String) {
        _passwordInput.value = password
        if (_validationError.value != null && password.isNotBlank()) {
            _validationError.value = null
        }
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun onSelectScannedNetwork(network: WifiScanItem) {
        _ssidInput.value = network.ssid
        _validationError.value = null
        _userFeedbackMessage.value = "Selected \"${network.ssid}\". Enter password if required."
    }

    fun startBleScan() {
        repository.startScan()
    }

    fun stopBleScan() {
        repository.stopScan()
    }

    fun connectToDevice(device: BleDeviceItem) {
        repository.connect(device)
    }

    fun disconnectDevice() {
        repository.disconnect()
        _passwordInput.value = "" // Security requirement: Clear password on disconnect
        _userFeedbackMessage.value = "Disconnected from BLE device."
    }

    fun sendWifiCredentials() {
        val ssid = _ssidInput.value.trim()
        val password = _passwordInput.value

        // Input validation
        if (ssid.isEmpty()) {
            _validationError.value = "SSID cannot be empty."
            return
        }

        if (password.isEmpty()) {
            _validationError.value = "Password cannot be empty (for Open network enter empty or select Open)."
            // Allow if user explicitly confirms or continues, but prompt
        }

        _validationError.value = null

        repository.sendProvisioningCredentials(ssid, password) { success, error ->
            if (success) {
                // Security requirement: Clear password after transmission
                _passwordInput.value = ""
                _userFeedbackMessage.value = "Credentials sent! Raspberry Pi is connecting to Wi-Fi..."
            } else {
                _userFeedbackMessage.value = "Failed to send credentials: ${error ?: "Unknown error"}"
            }
        }
    }

    fun refreshWifiStatus() {
        repository.requestWifiStatus()
    }

    fun triggerPiWifiScan() {
        repository.requestWifiScan()
    }

    fun refreshSavedNetworks() {
        repository.requestSavedNetworks()
    }

    fun disconnectCurrentWifi() {
        repository.disconnectCurrentWifi { success, error ->
            if (success) {
                _userFeedbackMessage.value = "Sent disconnect command to Raspberry Pi."
            } else {
                _userFeedbackMessage.value = "Disconnect failed: $error"
            }
        }
    }

    fun requestForgetNetwork(ssid: String) {
        _networkToForget.value = ssid
    }

    fun dismissForgetDialog() {
        _networkToForget.value = null
    }

    fun confirmForgetNetwork() {
        val ssid = _networkToForget.value ?: return
        _networkToForget.value = null
        repository.forgetSavedNetwork(ssid) { success, error ->
            if (success) {
                _userFeedbackMessage.value = "Profile \"$ssid\" deleted from Pi."
            } else {
                _userFeedbackMessage.value = "Failed to forget network: $error"
            }
        }
    }

    fun switchToSavedNetwork(ssid: String) {
        repository.switchToSavedNetwork(ssid) { success, error ->
            if (success) {
                _userFeedbackMessage.value = "Switching Pi Wi-Fi to \"$ssid\"..."
            } else {
                _userFeedbackMessage.value = "Failed to switch network: $error"
            }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }
}
