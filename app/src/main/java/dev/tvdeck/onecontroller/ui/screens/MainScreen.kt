package dev.tvdeck.onecontroller.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    REMOTE("Remote", Icons.Default.Tv),
    CATALOG("Catalog", Icons.Default.Terminal),
    APPS("Apps", Icons.Default.Apps),
    FILES("Files", Icons.Default.Folder),
    DIAGNOSTICS("Diagnostics", Icons.Default.Analytics),
    DEVICES("Devices", Icons.Default.Devices)
}

@Composable
fun MainScreen(transportManager: TransportManager) {
    var selectedTab by remember { mutableStateOf(AppTab.REMOTE) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricCyan,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = DarkSlateBg
        ) {
            when (selectedTab) {
                AppTab.REMOTE -> RemoteScreen(
                    transportManager = transportManager,
                    onNavigateToDevices = { selectedTab = AppTab.DEVICES }
                )
                AppTab.CATALOG -> CatalogScreen(transportManager = transportManager)
                AppTab.APPS -> AppsScreen(transportManager = transportManager)
                AppTab.FILES -> FilesScreen(transportManager = transportManager)
                AppTab.DIAGNOSTICS -> DiagnosticsScreen(transportManager = transportManager)
                AppTab.DEVICES -> DevicesScreen(transportManager = transportManager)
            }
        }
    }
}
