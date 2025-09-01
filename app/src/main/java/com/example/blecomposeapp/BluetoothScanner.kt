package com.example.blecomposeapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothScanner(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    var onDataReceived: ((String) -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                if (!device.name.isNullOrEmpty()) {
                    onDeviceFound?.invoke(device)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        bluetoothLeScanner?.startScan(scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(
        device: BluetoothDevice,
        onStatusChange: (BluetoothGatt?, String) -> Unit = { _, _ -> }
    ) {
        device.connectGatt(context, false, object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onStatusChange(gatt, "✅ Connected to ${device.name ?: device.address}")
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        onStatusChange(null, "❌ Disconnected from ${device.name ?: device.address}")
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return

                // Log all services & characteristics
                gatt.services.forEach { service ->
                    println("🟢 Service discovered: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        val readable = char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                        val writable = char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                        val notifiable = char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

                        println("  ↳ Characteristic: ${char.uuid}")
                        println("     ├─ Readable: $readable")
                        println("     ├─ Writable: $writable")
                        println("     └─ Notifiable: $notifiable")
                    }
                }

                // Find first notifiable characteristic
                val notifiableChar = gatt.services
                    .flatMap { it.characteristics }
                    .firstOrNull { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }

                notifiableChar?.let { char ->
                    val service = gatt.services.find { it.characteristics.contains(char) }
                    println("🔔 Subscribing to characteristic: ${char.uuid} in service: ${service?.uuid}")

                    gatt.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor?.let { d ->
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(d)
                    }
                } ?: run {
                    println("⚠️ No notifiable characteristic found!")
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                val hexString = data.joinToString(" ") { "%02X".format(it) }
                val decoded = decodeData(data)
                val resultText = "Raw: $hexString\nDecoded: $decoded"

                // Print to log
                println("📥 $resultText")

                // Send back via callback
                onDataReceived?.invoke(resultText)
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("✅ Notification enabled for ${descriptor.characteristic.uuid}")
                } else {
                    println("❌ Failed to enable notifications for ${descriptor.characteristic.uuid}, status: $status")
                }
            }

            // Simple decoder (example: first byte value)
            private fun decodeData(data: ByteArray): String {
                if (data.isEmpty()) return "Empty"
                return "Value: ${data[0].toInt() and 0xFF}"
            }

        })
    }
}
