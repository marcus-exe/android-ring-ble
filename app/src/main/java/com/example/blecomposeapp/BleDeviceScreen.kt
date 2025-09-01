package com.example.blecomposeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BleDeviceScreen(viewModel: BluetoothViewModel) {
    val devices by viewModel.devices.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { viewModel.startScan() }) {
            Text("Start Scan")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.stopScan() }) {
            Text("Stop Scan")
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(devices) { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.connectToDevice(device) }
                        .padding(8.dp)
                ) {
                    Text(text = "${device.name} - ${device.address}")
                }
            }
        }
    }
}
