package dev.tvdeck.onecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.catalog.CommandCatalog
import dev.tvdeck.onecontroller.core.model.AdbResult
import dev.tvdeck.onecontroller.core.model.CommandItem
import dev.tvdeck.onecontroller.core.model.RiskLevel
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(transportManager: TransportManager) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var activeCommandForParams by remember { mutableStateOf<CommandItem?>(null) }
    var paramValues by remember { mutableStateOf(mutableMapOf<String, String>()) }

    var lastResult by remember { mutableStateOf<AdbResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val filteredCommands = remember(searchQuery, selectedCategory) {
        val base = if (searchQuery.isNotBlank()) {
            CommandCatalog.searchCommands(searchQuery)
        } else if (selectedCategory != "all") {
            CommandCatalog.getCommandsByCategory(selectedCategory)
        } else {
            CommandCatalog.commands
        }
        base
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 200+ ADB & TV commands...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "all",
                    onClick = { selectedCategory = "all"; searchQuery = "" },
                    label = { Text("All (${CommandCatalog.commands.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = CardSurface,
                        labelColor = TextSecondary
                    )
                )
            }
            items(CommandCatalog.categories) { cat ->
                val count = CommandCatalog.getCommandsByCategory(cat.id).size
                FilterChip(
                    selected = selectedCategory == cat.id,
                    onClick = { selectedCategory = cat.id; searchQuery = "" },
                    label = { Text("${cat.title} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = CardSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Commands List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCommands, key = { it.id }) { item ->
                CommandCard(
                    command = item,
                    onExecute = {
                        if (item.params.isNotEmpty()) {
                            paramValues = item.params.associate { it.name to it.defaultValue }.toMutableMap()
                            activeCommandForParams = item
                        } else {
                            scope.launch {
                                isExecuting = true
                                val res = transportManager.executeCommand(item)
                                lastResult = res
                                if (res.output.isNotBlank()) {
                                    showResultDialog = true
                                }
                                isExecuting = false
                            }
                        }
                    },
                    onUndo = {
                        if (item.undoCommand != null) {
                            scope.launch {
                                isExecuting = true
                                val undoItem = item.copy(shell = item.undoCommand)
                                val res = transportManager.executeCommand(undoItem)
                                lastResult = res
                                showResultDialog = true
                                isExecuting = false
                            }
                        }
                    }
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Parameter Input Dialog
    // -------------------------------------------------------------
    activeCommandForParams?.let { cmd ->
        AlertDialog(
            onDismissRequest = { activeCommandForParams = null },
            title = { Text(cmd.label) },
            text = {
                Column {
                    Text(cmd.description, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    cmd.params.forEach { p ->
                        OutlinedTextField(
                            value = paramValues[p.name] ?: "",
                            onValueChange = { paramValues[p.name] = it },
                            label = { Text(p.label) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentCmd = activeCommandForParams
                        val currentParams = paramValues.toMap()
                        activeCommandForParams = null
                        if (currentCmd != null) {
                            scope.launch {
                                isExecuting = true
                                val res = transportManager.executeCommand(currentCmd, currentParams)
                                lastResult = res
                                showResultDialog = true
                                isExecuting = false
                            }
                        }
                    }
                ) {
                    Text("Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeCommandForParams = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Output Result Dialog
    // -------------------------------------------------------------
    if (showResultDialog && lastResult != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (lastResult!!.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (lastResult!!.success) BrightGreen else DangerRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lastResult!!.success) "Command Output" else "Execution Failed")
                }
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastResult!!.output.ifBlank { "Executed with exit code 0" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CommandCard(
    command: CommandItem,
    onExecute: () -> Unit,
    onUndo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = command.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RiskBadge(command.risk)
                }

                // Transport tags: R, S
                Row {
                    command.via.forEach { v ->
                        val (lbl, bg) = when (v) {
                            "R" -> Pair("Remote v2", ElectricCyan.copy(alpha = 0.2f))
                            "S" -> Pair("ADB Shell", ElectricBlue.copy(alpha = 0.2f))
                            else -> Pair(v, Color.Gray.copy(alpha = 0.2f))
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = lbl, fontSize = 10.sp, color = TextPrimary)
                        }
                    }
                }
            }

            Text(
                text = command.description,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )

            // Shell command preview
            if (command.shell.isNotBlank()) {
                Text(
                    text = "$ ${command.shell}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ElectricCyan.copy(alpha = 0.8f),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (command.undoCommand != null) {
                    OutlinedButton(
                        onClick = onUndo,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onExecute,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (command.risk) {
                            RiskLevel.GREEN -> ElectricCyan
                            RiskLevel.YELLOW -> BrightYellow
                            RiskLevel.RED -> DangerRed
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    val textColor = if (command.risk == RiskLevel.RED) Color.White else Color.Black
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(risk: RiskLevel) {
    val (color, text) = when (risk) {
        RiskLevel.GREEN -> Pair(BrightGreen, "Safe")
        RiskLevel.YELLOW -> Pair(BrightYellow, "Caution")
        RiskLevel.RED -> Pair(DangerRed, "Danger")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
