package dev.tvdeck.onecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.model.TvDevice
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(transportManager: TransportManager) {
    val discoveredDevices by transportManager.discoveryManager.discoveredDevices.collectAsState()
    val isScanning by transportManager.discoveryManager.isScanning.collectAsState()
    val savedDevices by transportManager.savedDevices.collectAsState()
    val activeDevice by transportManager.activeDevice.collectAsState()
    val statusMessage by transportManager.statusMessage.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }

    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var pendingPairingDevice by remember { mutableStateOf<TvDevice?>(null) }
    var pairingCodeDeferred by remember { mutableStateOf<CompletableDeferred<String>?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        transportManager.discoveryManager.startDiscovery()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Status Bar
        if (statusMessage.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANAGE TV DEVICES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            Row {
                IconButton(onClick = { transportManager.discoveryManager.startDiscovery() }) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = ElectricCyan)
                    }
                }
                Button(
                    onClick = { showManualAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual IP", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discovered Devices Section
            item {
                Text(
                    text = "DISCOVERED ON WI-FI (${discoveredDevices.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricCyan
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Text(
                        text = if (isScanning) "Searching for Android TVs on your Wi-Fi..." else "No TVs found via mDNS. Try entering IP manually.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(discoveredDevices, key = { it.host }) { dev ->
                    TvDeviceCard(
                        device = dev,
                        isActive = activeDevice?.host == dev.host,
                        onConnect = {
                            transportManager.connectToDevice(dev)
                        },
                        onPairRemoteV2 = {
                            pendingPairingDevice = dev
                            val deferred = CompletableDeferred<String>()
                            pairingCodeDeferred = deferred
                            showPairingDialog = true

                            scope.launch {
                                transportManager.pairRemoteV2(dev) {
                                    deferred.await()
                                }
                            }
                        }
                    )
                }
            }

            // Saved Devices Section
            if (savedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "SAVED DEVICES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                items(savedDevices, key = { it.id }) { dev ->
                    TvDeviceCard(
                        device = dev,
                        isActive = activeDevice?.host == dev.host,
                        onConnect = { transportManager.connectToDevice(dev) },
                        onPairRemoteV2 = {
                            pendingPairingDevice = dev
                            val deferred = CompletableDeferred<String>()
                            pairingCodeDeferred = deferred
                            showPairingDialog = true

                            scope.launch {
                                transportManager.pairRemoteV2(dev) {
                                    deferred.await()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Manual Add Dialog
    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Connect to TV via IP") },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("IP Address (e.g. 192.168.1.100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("TV Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            val name = if (manualName.isNotBlank()) manualName else "Android TV ($manualIp)"
                            val dev = TvDevice(
                                id = manualIp,
                                name = name,
                                host = manualIp.trim()
                            )
                            transportManager.saveDevice(dev)
                            transportManager.connectToDevice(dev)
                            showManualAddDialog = false
                        }
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pairing Code Dialog
    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = {
                pairingCodeDeferred?.complete("")
                showPairingDialog = false
            },
            title = { Text("Enter TV Pairing Code") },
            text = {
                Column {
                    Text(
                        text = "Check your TV screen for a 6-character code (hex or numbers).",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { pairingCodeInput = it },
                        label = { Text("6-Character Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = pairingCodeInput.trim()
                        pairingCodeDeferred?.complete(c)
                        showPairingDialog = false
                        pairingCodeInput = ""
                    }
                ) {
                    Text("Pair")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pairingCodeDeferred?.complete("")
                        showPairingDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TvDeviceCard(
    device: TvDevice,
    isActive: Boolean,
    onConnect: () -> Unit,
    onPairRemoteV2: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CardSurfaceVariant else CardSurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isActive) ElectricCyan else CardSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (isActive) Color.Black else ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${device.host} • Ports: ${device.remoteV2Port}/${device.adbPort}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (device.macAddress.isNotBlank()) {
                        Text(
                            text = "MAC: ${device.macAddress}",
                            fontSize = 10.sp,
                            color = TextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row {
                OutlinedButton(
                    onClick = onPairRemoteV2,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text("Pair", fontSize = 11.sp)
                }

                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) BrightGreen else ElectricCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) "Active" else "Connect",
                        fontSize = 11.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
