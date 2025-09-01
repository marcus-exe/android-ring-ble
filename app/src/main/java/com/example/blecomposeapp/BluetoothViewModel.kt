package com.example.blecomposeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
        scanner.connectToDevice(device) { status ->
            _connectionStatus.value = status
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDeviceByMac(macAddress: String) {
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress)
        scanner.connectToDevice(device) { status ->
            _connectionStatus.value = status
        }
    }

}
