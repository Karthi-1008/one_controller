package dev.tvdeck.onecontroller.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.model.TvAppInfo
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppsScreen(transportManager: TransportManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appsList by remember { mutableStateOf<List<TvAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var installStatusMessage by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }

    fun refreshApps() {
        scope.launch {
            isLoading = true
            appsList = withContext(Dispatchers.IO) {
                val list = mutableListOf<TvAppInfo>()
                val res = transportManager.adbClient.executeShell("pm list packages -3")
                if (res.success) {
                    for (line in res.output.lines()) {
                        val pkg = line.removePrefix("package:").trim()
                        if (pkg.isNotBlank()) {
                            val friendlyName = pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                            list.add(
                                TvAppInfo(
                                    packageName = pkg,
                                    appName = friendlyName,
                                    isSystemApp = false
                                )
                            )
                        }
                    }
                }
                list.sortedBy { it.appName }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    // File picker for APK install
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isInstalling = true
                installStatusMessage = "Reading and uploading APK..."
                try {
                    val tempApk = File(context.cacheDir, "sideload_${System.currentTimeMillis()}.apk")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inStream ->
                            FileOutputStream(tempApk).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }
                    installStatusMessage = "Streaming install to TV..."
                    val res = transportManager.adbClient.installApk(tempApk)
                    tempApk.delete()
                    installStatusMessage = if (res.success) "Installation Successful!" else "Install Failed: ${res.output}"
                    refreshApps()
                } catch (e: Exception) {
                    installStatusMessage = "Error: ${e.message}"
                } finally {
                    isInstalling = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Sideload Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "APK Sideload & Manager",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isInstalling) installStatusMessage else "${appsList.size} apps installed on TV",
                        fontSize = 12.sp,
                        color = if (isInstalling) BrightYellow else TextSecondary
                    )
                }

                Row {
                    IconButton(onClick = { refreshApps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricCyan)
                    }

                    Button(
                        onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Install APK", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install APK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search apps
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search TV apps...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            val filtered = appsList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        onLaunch = {
                            scope.launch {
                                transportManager.adbClient.executeShell("monkey -p ${app.packageName} -c android.intent.category.LEANBACK_LAUNCHER 1")
                            }
                        },
                        onForceStop = {
                            scope.launch {
                                transportManager.adbClient.executeShell("am force-stop ${app.packageName}")
                            }
                        },
                        onOpenSettings = {
                            scope.launch {
                                transportManager.adbClient.executeShell("am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:${app.packageName}")
                            }
                        },
                        onUninstall = {
                            scope.launch {
                                transportManager.adbClient.executeShell("pm uninstall ${app.packageName}")
                                refreshApps()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemCard(
    app: TvAppInfo,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row {
                IconButton(onClick = onLaunch) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Launch", tint = BrightGreen)
                }
                IconButton(onClick = onForceStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Force Stop", tint = BrightYellow)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
                IconButton(onClick = onUninstall) {
                    Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = DangerRed)
                }
            }
        }
    }
}
