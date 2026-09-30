package dev.tvdeck.onecontroller.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.model.TvFileInfo
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun FilesScreen(transportManager: TransportManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf("/sdcard") }
    var fileList by remember { mutableStateOf<List<TvFileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    fun loadPath(path: String) {
        scope.launch {
            isLoading = true
            currentPath = path
            fileList = transportManager.adbClient.listFiles(path)
            isLoading = false
        }
    }

    LaunchedEffect(currentPath) {
        loadPath(currentPath)
    }

    // Upload file picker
    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                statusMessage = "Uploading file..."
                try {
                    val temp = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inS ->
                            FileOutputStream(temp).use { outS -> inS.copyTo(outS) }
                        }
                    }
                    val remoteDest = if (currentPath.endsWith("/")) "${currentPath}file_${System.currentTimeMillis()}" else "$currentPath/file_${System.currentTimeMillis()}"
                    val ok = transportManager.adbClient.pushFile(temp, remoteDest)
                    temp.delete()
                    statusMessage = if (ok) "Upload Complete!" else "Upload Failed"
                    loadPath(currentPath)
                } catch (e: Exception) {
                    statusMessage = "Error: ${e.message}"
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
        // Path navigation bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentPath != "/sdcard" && currentPath != "/") {
                        IconButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast("/")
                                loadPath(if (parent.isEmpty()) "/" else parent)
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Up", tint = ElectricCyan)
                        }
                    } else {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricCyan, modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentPath,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Row {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = TextSecondary)
                    }
                    IconButton(onClick = { fileUploadLauncher.launch("*/*") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload", tint = ElectricCyan)
                    }
                    IconButton(onClick = { loadPath(currentPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                fontSize = 12.sp,
                color = ElectricCyan,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(fileList, key = { it.path }) { file ->
                    FileRowItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                loadPath(file.path)
                            }
                        },
                        onDelete = {
                            scope.launch {
                                transportManager.adbClient.deleteFile(file.path)
                                loadPath(currentPath)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            val target = if (currentPath.endsWith("/")) "$currentPath$newFolderName" else "$currentPath/$newFolderName"
                            scope.launch {
                                transportManager.adbClient.makeDirectory(target)
                                loadPath(currentPath)
                            }
                            newFolderName = ""
                        }
                        showNewFolderDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FileRowItem(
    file: TvFileInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (file.isDirectory) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = file.name,
                        fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (file.isDirectory) "Directory • ${file.lastModified}" else "${formatFileSize(file.sizeBytes)} • ${file.lastModified}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
