package com.example.blecomposeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BleDeviceScreen(viewModel: BluetoothViewModel) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.startScan() }) {
                Text("Start Scan")
            }
            Button(onClick = { viewModel.stopScan() }) {
                Text("Stop Scan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Status: $status")

        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Text("No BLE devices found", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(devices) { device ->
                    DeviceItem(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        onClick = { viewModel.connectToDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItem(name: String, address: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = address, style = MaterialTheme.typography.bodySmall)
        }
    }
}

