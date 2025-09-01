package com.example.blecomposeapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.UUID

val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothScanner(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null

    private val callback = object : ScanCallback() {
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
        bluetoothLeScanner?.startScan(callback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothLeScanner?.stopScan(callback)
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
                if (status == BluetoothGatt.GATT_SUCCESS) {

                    // ✅ Log all services and characteristics
                    gatt.services.forEach { service ->
                        println("🟢 Service discovered: ${service.uuid}")
                        service.characteristics.forEach { char ->
                            val props = char.properties
                            val readable = props and BluetoothGattCharacteristic.PROPERTY_READ != 0
                            val writable = props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                            val notifiable = props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

                            println("  ↳ Characteristic: ${char.uuid}")
                            println("     ├─ Readable: $readable")
                            println("     ├─ Writable: $writable")
                            println("     └─ Notifiable: $notifiable")
                        }
                    }

                    // 🔹 Dynamically find a notifiable characteristic
                    val notifiableChar = gatt.services
                        .flatMap { it.characteristics }
                        .firstOrNull { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }

                    notifiableChar?.let { char ->
                        // Find its parent service
                        val service = gatt.services.find { it.characteristics.contains(char) }

                        println("🔔 Subscribing to characteristic: ${char.uuid} in service: ${service?.uuid}")

                        // Enable notifications
                        gatt.setCharacteristicNotification(char, true)

                        // Write descriptor to enable notifications
                        val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor?.let { d ->
                            d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(d)
                        }
                    } ?: run {
                        println("⚠️ No notifiable characteristic found!")
                    }

                } else {
                    println("❌ Service discovery failed with status: $status")
                }
            }




            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val data = characteristic.value
                println("📥 Received data: ${data.joinToString(" ") { "%02X".format(it) }}")
            }


            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                val data = characteristic.value
                val text = data.joinToString(" ") { byte -> "%02X".format(byte) }
                println("Read data: $text")
            }
        })

    }


}
