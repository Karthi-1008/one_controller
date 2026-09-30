package dev.tvdeck.onecontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.tvdeck.onecontroller.core.service.OneControllerService
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.screens.MainScreen
import dev.tvdeck.onecontroller.ui.theme.OneControllerTheme

class MainActivity : ComponentActivity() {

    private lateinit var transportManager: TransportManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transportManager = TransportManager.getInstance(this)

        setContent {
            OneControllerTheme {
                MainScreen(transportManager = transportManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
