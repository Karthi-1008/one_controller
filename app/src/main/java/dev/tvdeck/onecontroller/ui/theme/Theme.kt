package dev.tvdeck.onecontroller.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkSlateBg = Color(0xFF101216)
val CardSurface = Color(0xFF1B1E24)
val CardSurfaceVariant = Color(0xFF242831)
val ElectricCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF2979FF)
val BrightGreen = Color(0xFF00E676)
val BrightYellow = Color(0xFFFFD600)
val DangerRed = Color(0xFFFF5252)
val TextPrimary = Color(0xFFECEFF4)
val TextSecondary = Color(0xFF90A4AE)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = ElectricCyan,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    background = DarkSlateBg,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = DangerRed
)

@Composable
fun OneControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
