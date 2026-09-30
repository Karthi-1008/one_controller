package dev.tvdeck.onecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DiagTab {
    DASHBOARD,
    TERMINAL
}

@Composable
fun DiagnosticsScreen(transportManager: TransportManager) {
    var activeTab by remember { mutableStateOf(DiagTab.DASHBOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = activeTab == DiagTab.DASHBOARD,
                onClick = { activeTab = DiagTab.DASHBOARD },
                label = { Text("System Dashboard") },
                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSurface,
                    labelColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilterChip(
                selected = activeTab == DiagTab.TERMINAL,
                onClick = { activeTab = DiagTab.TERMINAL },
                label = { Text("ADB Terminal") },
                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan,
                    selectedLabelColor = Color.Black,
                    containerColor = CardSurface,
                    labelColor = TextSecondary
                )
            )
        }

        when (activeTab) {
            DiagTab.DASHBOARD -> SystemDashboardView(transportManager)
            DiagTab.TERMINAL -> AdbTerminalView(transportManager)
        }
    }
}

@Composable
private fun SystemDashboardView(transportManager: TransportManager) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var modelInfo by remember { mutableStateOf("Android TV") }
    var androidVer by remember { mutableStateOf("Unknown") }
    var uptimeInfo by remember { mutableStateOf("") }
    var storageInfo by remember { mutableStateOf("") }
    var ramInfo by remember { mutableStateOf("") }
    var cecInfo by remember { mutableStateOf("") }

    fun refreshDashboard() {
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val m = transportManager.adbClient.executeShell("getprop ro.product.manufacturer && getprop ro.product.model")
                if (m.success) modelInfo = m.output.trim().replace("\n", " ")

                val v = transportManager.adbClient.executeShell("getprop ro.build.version.release && getprop ro.build.version.security_patch")
                if (v.success) androidVer = v.output.trim().replace("\n", " • Patch: ")

                val u = transportManager.adbClient.executeShell("uptime")
                if (u.success) uptimeInfo = u.output.trim()

                val s = transportManager.adbClient.executeShell("df -h /data /sdcard")
                if (s.success) storageInfo = s.output.trim()

                val r = transportManager.adbClient.executeShell("dumpsys meminfo | head -n 12")
                if (r.success) ramInfo = r.output.trim()

                val c = transportManager.adbClient.executeShell("dumpsys hdmi_control | head -n 10")
                if (c.success) cecInfo = c.output.trim()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVICE TELEMETRY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                IconButton(onClick = { refreshDashboard() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Hardware Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hardware & OS", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Model: $modelInfo", fontSize = 13.sp, color = TextPrimary)
                    Text(text = "Android Version: $androidVer", fontSize = 13.sp, color = TextSecondary)
                    if (uptimeInfo.isNotBlank()) {
                        Text(text = "Uptime: $uptimeInfo", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Storage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = BrightGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storage (Data & Internal)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = storageInfo.ifBlank { "Storage metrics unavailable" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // RAM Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RAM & Processes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ramInfo.ifBlank { "RAM metrics unavailable" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // HDMI-CEC Card
        if (cecInfo.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsInputHdmi, contentDescription = null, tint = BrightYellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HDMI-CEC Bus Info", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cecInfo,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdbTerminalView(transportManager: TransportManager) {
    var commandInput by remember { mutableStateOf("") }
    var consoleOutput by remember { mutableStateOf("Connected to ADB Shell. Type command below.\n") }
    var isExecuting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val quickCommands = listOf(
        "dumpsys power",
        "logcat -d -v time -t 50 *:W",
        "pm list features",
        "ip -4 addr show",
        "cmd hdmi_control help",
        "wm size"
    )

    fun runCmd(cmd: String) {
        if (cmd.isBlank()) return
        scope.launch {
            isExecuting = true
            consoleOutput += "\n$ $cmd\n"
            val res = transportManager.adbClient.executeShell(cmd)
            consoleOutput += res.output + "\n"
            isExecuting = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick suggestions
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickCommands) { q ->
                SuggestionChip(
                    onClick = { runCmd(q) },
                    label = { Text(q, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        // Console screen
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                item {
                    Text(
                        text = consoleOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Enter adb shell command...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = ElectricCyan
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val c = commandInput.trim()
                    commandInput = ""
                    runCmd(c)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                enabled = !isExecuting
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = Color.Black)
            }
            IconButton(onClick = { consoleOutput = "$ " }) {
                Icon(Icons.Default.ClearAll, contentDescription = "Clear", tint = TextSecondary)
            }
        }
    }
}
