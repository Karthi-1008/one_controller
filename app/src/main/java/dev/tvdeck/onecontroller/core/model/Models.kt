package dev.tvdeck.onecontroller.core.model

enum class TransportType {
    REMOTE_V2,
    ADB,
    HYBRID
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    PAIRING_REQUIRED,
    CONNECTED,
    ERROR
}

enum class RiskLevel {
    GREEN,   // Safe & reversible
    YELLOW,  // Caution / interrupts app
    RED      // Destructive / power / lockout risk
}

enum class CommandOutput {
    NONE,
    TEXT,
    JSON,
    IMAGE
}

data class TvDevice(
    val id: String,
    val name: String,
    val host: String,
    val remoteV2Port: Int = 6466,
    val adbPort: Int = 5555,
    val macAddress: String = "",
    val model: String = "Android TV",
    val manufacturer: String = "Google",
    val isPairedRemoteV2: Boolean = false,
    val isPairedAdb: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)

data class TvState(
    val isPoweredOn: Boolean = true,
    val volumeLevel: Int = 10,
    val maxVolume: Int = 25,
    val isMuted: Boolean = false,
    val currentApp: String = "com.google.android.tvlauncher",
    val currentTitle: String = ""
)

data class CommandParam(
    val name: String,
    val label: String,
    val defaultValue: String = "",
    val description: String = ""
)

data class CommandItem(
    val id: String,
    val label: String,
    val description: String,
    val category: String,
    val iconName: String = "code",
    val via: List<String> = listOf("R", "S"), // R = Remote v2, S = Shell/ADB, A = Agent, C = CEC, N = Phone
    val shell: String = "",
    val remoteKey: String? = null,
    val params: List<CommandParam> = emptyList(),
    val risk: RiskLevel = RiskLevel.GREEN,
    val minSdk: Int = 21,
    val undoCommand: String? = null,
    val output: CommandOutput = CommandOutput.NONE,
    val tags: List<String> = emptyList()
)

data class CommandCategory(
    val id: String,
    val title: String,
    val iconName: String,
    val description: String
)

data class TvAppInfo(
    val packageName: String,
    val appName: String,
    val activityName: String = "",
    val isSystemApp: Boolean = false,
    val isEnabled: Boolean = true,
    val versionName: String = ""
)

data class TvFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val permissions: String = "",
    val lastModified: String = ""
)

data class AdbResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int = 0
)
