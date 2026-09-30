package dev.tvdeck.onecontroller.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvdeck.onecontroller.core.model.ConnectionStatus
import dev.tvdeck.onecontroller.core.transport.TransportManager
import dev.tvdeck.onecontroller.ui.theme.*
import kotlinx.coroutines.launch

enum class RemoteInputMode {
    DPAD,
    TRACKPAD,
    NUMPAD
}

@Composable
fun RemoteScreen(
    transportManager: TransportManager,
    onNavigateToDevices: () -> Unit
) {
    val connectionStatus by transportManager.connectionStatus.collectAsState()
    val activeDevice by transportManager.activeDevice.collectAsState()
    val tvState by transportManager.tvState.collectAsState()

    var inputMode by remember { mutableStateOf(RemoteInputMode.DPAD) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var textInputVal by remember { mutableStateOf("") }
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    fun haptic() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------------------------------------------------------
        // Header: Active TV & Connection Status
        // ---------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeDevice?.name ?: "No TV Connected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val (dotColor, statusText) = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Pair(BrightGreen, "Connected")
                            ConnectionStatus.CONNECTING -> Pair(BrightYellow, "Connecting...")
                            ConnectionStatus.PAIRING_REQUIRED -> Pair(ElectricCyan, "Pairing Needed")
                            ConnectionStatus.ERROR -> Pair(DangerRed, "Error")
                            ConnectionStatus.DISCONNECTED -> Pair(Color.Gray, "Disconnected")
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeDevice != null) "$statusText (${activeDevice!!.host})" else statusText,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.wakeActiveDevice()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Wake-on-LAN",
                            tint = BrightYellow
                        )
                    }

                    IconButton(onClick = onNavigateToDevices) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Manage TVs",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------------------------------------------------------
        // Top Remote Controls (Power, Inputs, Keyboard, Settings)
        // ---------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteRoundButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "Power",
                color = DangerRed
            ) {
                haptic()
                transportManager.sendNavigationKey(26, "POWER")
            }

            RemoteRoundButton(
                icon = Icons.Default.Keyboard,
                label = "Text",
                color = ElectricCyan
            ) {
                haptic()
                showTextInputDialog = true
            }

            RemoteRoundButton(
                icon = Icons.Default.Mic,
                label = "Voice",
                color = ElectricBlue
            ) {
                haptic()
                transportManager.sendNavigationKey(231, "VOICE_ASSIST")
            }

            RemoteRoundButton(
                icon = Icons.Default.Settings,
                label = "Settings",
                color = TextSecondary
            ) {
                haptic()
                transportManager.sendNavigationKey(176, "SETTINGS")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ---------------------------------------------------------
        // Mode Switcher (D-Pad, Trackpad, Numpad)
        // ---------------------------------------------------------
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ModeTabItem(
                title = "D-PAD",
                selected = inputMode == RemoteInputMode.DPAD,
                onClick = { inputMode = RemoteInputMode.DPAD; haptic() }
            )
            ModeTabItem(
                title = "TRACKPAD",
                selected = inputMode == RemoteInputMode.TRACKPAD,
                onClick = { inputMode = RemoteInputMode.TRACKPAD; haptic() }
            )
            ModeTabItem(
                title = "NUMPAD",
                selected = inputMode == RemoteInputMode.NUMPAD,
                onClick = { inputMode = RemoteInputMode.NUMPAD; haptic() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ---------------------------------------------------------
        // Interactive Center: D-Pad / Trackpad / Numpad
        // ---------------------------------------------------------
        AnimatedContent(targetState = inputMode, label = "input_mode") { mode ->
            when (mode) {
                RemoteInputMode.DPAD -> {
                    CircularDPad(
                        onUp = { haptic(); transportManager.sendNavigationKey(19, "DPAD_UP") },
                        onDown = { haptic(); transportManager.sendNavigationKey(20, "DPAD_DOWN") },
                        onLeft = { haptic(); transportManager.sendNavigationKey(21, "DPAD_LEFT") },
                        onRight = { haptic(); transportManager.sendNavigationKey(22, "DPAD_RIGHT") },
                        onCenter = { haptic(); transportManager.sendNavigationKey(23, "DPAD_CENTER") }
                    )
                }
                RemoteInputMode.TRACKPAD -> {
                    TrackpadSurface(
                        onTap = { x, y ->
                            haptic()
                            transportManager.sendTouchCoordinates(x, y)
                        },
                        onSwipe = { x1, y1, x2, y2 ->
                            transportManager.sendSwipe(x1, y1, x2, y2)
                        }
                    )
                }
                RemoteInputMode.NUMPAD -> {
                    NumpadGrid(
                        onNumberClick = { num ->
                            haptic()
                            val keycode = 7 + num // KEYCODE_0 is 7, KEYCODE_1 is 8...
                            transportManager.sendNavigationKey(keycode)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // Core Navigation Bar: Back, Home, Menu, Recents
        // ---------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemotePillButton(
                icon = Icons.Default.ArrowBack,
                label = "Back"
            ) {
                haptic()
                transportManager.sendNavigationKey(4, "BACK")
            }

            RemotePillButton(
                icon = Icons.Default.Home,
                label = "Home",
                tint = ElectricCyan
            ) {
                haptic()
                transportManager.sendNavigationKey(3, "HOME")
            }

            RemotePillButton(
                icon = Icons.Default.Menu,
                label = "Menu"
            ) {
                haptic()
                transportManager.sendNavigationKey(82, "MENU")
            }

            RemotePillButton(
                icon = Icons.Default.ViewCarousel,
                label = "Recents"
            ) {
                haptic()
                transportManager.sendNavigationKey(187) // KEYCODE_APP_SWITCH
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // Volume & Channel Controls
        // ---------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Volume Column
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(24, "VOLUME_UP")
                        }
                    ) {
                        Icon(Icons.Default.Add, "Vol+", tint = TextPrimary)
                    }

                    Text(
                        text = "VOL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(25, "VOLUME_DOWN")
                        }
                    ) {
                        Icon(Icons.Default.Remove, "Vol-", tint = TextPrimary)
                    }

                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(164, "VOLUME_MUTE")
                        }
                    ) {
                        Icon(
                            imageVector = if (tvState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                            contentDescription = "Mute",
                            tint = if (tvState.isMuted) DangerRed else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Channel Column
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(166, "CHANNEL_UP")
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Ch+", tint = TextPrimary)
                    }

                    Text(
                        text = "CH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(167, "CHANNEL_DOWN")
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Ch-", tint = TextPrimary)
                    }

                    IconButton(
                        onClick = {
                            haptic()
                            transportManager.sendNavigationKey(172, "GUIDE")
                        }
                    ) {
                        Icon(Icons.Default.ListAlt, "Guide", tint = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // Media Playback Bar
        // ---------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { haptic(); transportManager.sendNavigationKey(89, "MEDIA_REWIND") }) {
                    Icon(Icons.Default.FastRewind, "Rewind", tint = TextPrimary)
                }
                IconButton(onClick = { haptic(); transportManager.sendNavigationKey(88, "MEDIA_PREVIOUS") }) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = TextPrimary)
                }
                IconButton(
                    onClick = { haptic(); transportManager.sendNavigationKey(85, "MEDIA_PLAY_PAUSE") },
                    modifier = Modifier
                        .size(52.dp)
                        .background(ElectricCyan, CircleShape)
                ) {
                    Icon(Icons.Default.PlayArrow, "Play/Pause", tint = Color.Black)
                }
                IconButton(onClick = { haptic(); transportManager.sendNavigationKey(87, "MEDIA_NEXT") }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = TextPrimary)
                }
                IconButton(onClick = { haptic(); transportManager.sendNavigationKey(90, "MEDIA_FAST_FORWARD") }) {
                    Icon(Icons.Default.FastForward, "Fast Forward", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------
        // Quick App Launcher Dock
        // ---------------------------------------------------------
        Text(
            text = "QUICK APPS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppQuickLaunchPill("YouTube", Icons.Default.SmartDisplay, Color(0xFFFF0000)) {
                transportManager.sendNavigationKey(0, "https://www.youtube.com")
            }
            AppQuickLaunchPill("Netflix", Icons.Default.Movie, Color(0xFFE50914)) {
                scope.launch { transportManager.adbClient.executeShell("monkey -p com.netflix.ninja -c android.intent.category.LEANBACK_LAUNCHER 1") }
            }
            AppQuickLaunchPill("Prime", Icons.Default.OndemandVideo, Color(0xFF00A8E1)) {
                scope.launch { transportManager.adbClient.executeShell("monkey -p com.amazon.amazonvideo.livingroom -c android.intent.category.LEANBACK_LAUNCHER 1") }
            }
            AppQuickLaunchPill("Spotify", Icons.Default.MusicNote, Color(0xFF1DB954)) {
                scope.launch { transportManager.adbClient.executeShell("monkey -p com.spotify.tv.android -c android.intent.category.LEANBACK_LAUNCHER 1") }
            }
        }
    }

    // -------------------------------------------------------------
    // Direct Text Input Dialog
    // -------------------------------------------------------------
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Send Text to TV") },
            text = {
                OutlinedTextField(
                    value = textInputVal,
                    onValueChange = { textInputVal = it },
                    placeholder = { Text("Type search query, password, URL...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInputVal.isNotBlank()) {
                            transportManager.sendText(textInputVal)
                            textInputVal = ""
                        }
                        showTextInputDialog = false
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// Sub-components: Circular D-Pad
// -----------------------------------------------------------------

@Composable
private fun CircularDPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(CardSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // UP
        IconButton(
            onClick = onUp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(60.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Up", tint = TextPrimary, modifier = Modifier.size(36.dp))
        }

        // DOWN
        IconButton(
            onClick = onDown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(60.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Down", tint = TextPrimary, modifier = Modifier.size(36.dp))
        }

        // LEFT
        IconButton(
            onClick = onLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(60.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, "Left", tint = TextPrimary, modifier = Modifier.size(36.dp))
        }

        // RIGHT
        IconButton(
            onClick = onRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(60.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowRight, "Right", tint = TextPrimary, modifier = Modifier.size(36.dp))
        }

        // CENTER OK
        Button(
            onClick = onCenter,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
            modifier = Modifier.size(80.dp)
        ) {
            Text(
                text = "OK",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

// -----------------------------------------------------------------
// Sub-components: Trackpad Surface
// -----------------------------------------------------------------

@Composable
private fun TrackpadSurface(
    onTap: (x: Int, y: Int) -> Unit,
    onSwipe: (x1: Int, y1: Int, x2: Int, y2: Int) -> Unit
) {
    var accumulatedDx by remember { mutableStateOf(0f) }
    var accumulatedDy by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Map local tap to 1920x1080 standard TV screen coordinates
                    val tvX = ((offset.x / size.width) * 1920).toInt()
                    val tvY = ((offset.y / size.height) * 1080).toInt()
                    onTap(tvX, tvY)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDx += dragAmount.x
                        accumulatedDy += dragAmount.y
                    },
                    onDragEnd = {
                        if (Math.abs(accumulatedDx) > 30 || Math.abs(accumulatedDy) > 30) {
                            val startX = 960
                            val startY = 540
                            val endX = (960 + accumulatedDx * 2).toInt().coerceIn(100, 1820)
                            val endY = (540 + accumulatedDy * 2).toInt().coerceIn(100, 980)
                            onSwipe(startX, startY, endX, endY)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = ElectricCyan.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Touchpad Mode\nTap to click • Drag to scroll",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -----------------------------------------------------------------
// Sub-components: Numpad Grid
// -----------------------------------------------------------------

@Composable
private fun NumpadGrid(onNumberClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2)
        )

        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (num in row) {
                    if (num >= 0) {
                        Button(
                            onClick = { onNumberClick(num) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Text(
                                text = num.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(50.dp))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// Helper Remote Widgets
// -----------------------------------------------------------------

@Composable
private fun ModeTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ElectricCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.Black else TextSecondary
        )
    }
}

@Composable
private fun RemoteRoundButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CardSurface)
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun RemotePillButton(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = tint)
    }
}

@Composable
private fun AppQuickLaunchPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(78.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures { onClick() }
    }
)
