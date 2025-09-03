package com.example.blecomposeapp

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothViewModel(context: Context) : ViewModel() {
    private val scanner = BluetoothScanner(context)

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _connectionStatus = MutableStateFlow("Not connected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private var connectedGatt: BluetoothGatt? = null

    private val _receivedData = MutableStateFlow<List<String>>(emptyList())
    val receivedData: StateFlow<List<String>> = _receivedData

    init {
        // Add device found callback
        scanner.onDeviceFound = { device ->
            if (!_devices.value.contains(device)) {
                _devices.value = _devices.value + device
            }
        }

        // Add data received callback
        scanner.onDataReceived = { text ->
            _receivedData.value = _receivedData.value + text
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendCommand(command: ByteArray) {
        connectedGatt?.let { gatt ->
            // Find the characteristic to write to
            val writableChar = gatt.services
                .flatMap { it.characteristics }
                .firstOrNull { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 }

            writableChar?.let { char ->
                char.value = command
                gatt.writeCharacteristic(char)
            } ?: run {
                println("❌ No writable characteristic found!")
            }
        }
    }

    fun clearReceivedData() {
        _receivedData.value = emptyList()
    }

}
