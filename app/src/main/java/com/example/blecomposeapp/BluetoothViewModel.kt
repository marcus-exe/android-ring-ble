package com.example.blecomposeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothViewModel(context: Context) : ViewModel() {
    private val scanner = BluetoothScanner(context)

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _connectionStatus = MutableStateFlow("Not connected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private var connectedGatt: BluetoothGatt? = null

    init {
        scanner.onDeviceFound = { device ->
            if (!_devices.value.contains(device)) {
                _devices.value = _devices.value + device
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        scanner.startScan()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        scanner.stopScan()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice) {
        scanner.connectToDevice(device) { gatt, status ->
            if (gatt != null) connectedGatt = gatt
            _connectionStatus.value = status
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        connectedGatt?.disconnect()
        connectedGatt?.close()
        connectedGatt = null
        _connectionStatus.value = "Disconnected"
    }

}
