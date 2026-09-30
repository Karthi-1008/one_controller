package dev.tvdeck.onecontroller.core.catalog

import dev.tvdeck.onecontroller.core.model.*

object CommandCatalog {

    val categories = listOf(
        CommandCategory("navigation", "Navigation & Keys", "navigation", "D-Pad, Home, Back, Menu, Color keys and macros"),
        CommandCategory("media", "Media & Playback", "play_arrow", "Play, pause, skip, active media session controls"),
        CommandCategory("volume", "Volume & Audio", "volume_up", "Volume rockers, precise volume, mute, surround mode"),
        CommandCategory("power", "Power & Screen", "power_settings_new", "Standby, wake, reboot, sleep timer, screensaver"),
        CommandCategory("inputs", "Inputs & Sources", "tv", "HDMI 1-4, AV inputs, Live TV, Tuner"),
        CommandCategory("text", "Text & Keyboard", "keyboard", "IME text typing, clipboard copy/paste, special keys"),
        CommandCategory("touch", "Pointer & Touch", "touch_app", "Tap, swipe, scroll coordinates, mouse cursor"),
        CommandCategory("apps", "App Management", "apps", "Leanback launcher, running apps, force stop, clear cache, permissions"),
        CommandCategory("links", "Deep Links & URLs", "link", "Launch YouTube, Netflix, Disney+, Prime, custom links"),
        CommandCategory("settings", "Settings Shortcuts", "tune", "Direct jumps into TV Wi-Fi, BT, Display, Developer settings"),
        CommandCategory("tweaks", "System Tweaks", "speed", "Animation speed 0.5x, disable ADB timeout, font scale"),
        CommandCategory("display", "Display & UI", "aspect_ratio", "Resolution & density adjustment with auto-revert timer"),
        CommandCategory("network", "Network & WoL", "wifi", "IP address, MAC address, Wi-Fi status, ping"),
        CommandCategory("bluetooth", "Bluetooth", "bluetooth", "Bonded accessories, remote control battery level, pairing"),
        CommandCategory("files", "Files & Storage", "folder", "Disk usage, storage trim, directory listing"),
        CommandCategory("capture", "Capture & Record", "screenshot", "Screenshots, screen recording"),
        CommandCategory("diagnostics", "Live Diagnostics", "analytics", "TV model, SoC, CPU, RAM, thermal sensors, uptime"),
        CommandCategory("cec", "HDMI-CEC", "settings_input_hdmi", "One Touch Play, active source, standby TV & soundbar"),
        CommandCategory("dev", "Developer Tools", "terminal", "Interactive logcat, stress tests, dumpsys inspect"),
        CommandCategory("debloat", "Debloat & Privacy", "security", "Disable telemetry, revoke permissions, safe restore"),
        CommandCategory("danger", "Danger Zone", "warning", "Reboot to recovery, power down, raw settings editor")
    )

    val commands: List<CommandItem> = listOf(
        // ---------------------------------------------------------
        // 6.1 Navigation & System Keys
        // ---------------------------------------------------------
        CommandItem("nav.up", "D-Pad Up", "Navigate upwards", "navigation", "arrow_upward", listOf("R", "S"), "input keyevent KEYCODE_DPAD_UP", "DPAD_UP"),
        CommandItem("nav.down", "D-Pad Down", "Navigate downwards", "navigation", "arrow_downward", listOf("R", "S"), "input keyevent KEYCODE_DPAD_DOWN", "DPAD_DOWN"),
        CommandItem("nav.left", "D-Pad Left", "Navigate left", "navigation", "arrow_back", listOf("R", "S"), "input keyevent KEYCODE_DPAD_LEFT", "DPAD_LEFT"),
        CommandItem("nav.right", "D-Pad Right", "Navigate right", "navigation", "arrow_forward", listOf("R", "S"), "input keyevent KEYCODE_DPAD_RIGHT", "DPAD_RIGHT"),
        CommandItem("nav.center", "Select / OK", "Confirm selection", "navigation", "check_circle", listOf("R", "S"), "input keyevent KEYCODE_DPAD_CENTER", "DPAD_CENTER"),
        CommandItem("nav.back", "Back", "Return to previous screen", "navigation", "arrow_back", listOf("R", "S"), "input keyevent KEYCODE_BACK", "BACK"),
        CommandItem("nav.home", "Home", "Return to Android TV leanback launcher", "navigation", "home", listOf("R", "S"), "input keyevent KEYCODE_HOME", "HOME"),
        CommandItem("nav.menu", "Menu / Options", "Open context menu or options", "navigation", "menu", listOf("R", "S"), "input keyevent KEYCODE_MENU", "MENU"),
        CommandItem("nav.settings", "Settings", "Open Android TV quick settings", "navigation", "settings", listOf("R", "S"), "input keyevent KEYCODE_SETTINGS", "SETTINGS"),
        CommandItem("nav.recents", "App Switcher / Recents", "View recently opened apps", "navigation", "view_carousel", listOf("S"), "input keyevent KEYCODE_APP_SWITCH"),
        CommandItem("nav.all_apps", "All Apps Drawer", "Open full apps list drawer", "navigation", "apps", listOf("S"), "input keyevent KEYCODE_ALL_APPS"),
        CommandItem("nav.search", "Search", "Open search bar", "navigation", "search", listOf("R", "S"), "input keyevent KEYCODE_SEARCH", "SEARCH"),
        CommandItem("nav.assistant", "Google Assistant / Voice", "Trigger voice assistant mic input", "navigation", "mic", listOf("R", "S"), "input keyevent KEYCODE_VOICE_ASSIST", "VOICE_ASSIST"),
        CommandItem("nav.guide", "Live TV Guide", "Open electronic program guide", "navigation", "list_alt", listOf("R", "S"), "input keyevent KEYCODE_GUIDE", "GUIDE"),
        CommandItem("nav.info", "Info", "Show media/channel info overlay", "navigation", "info", listOf("R", "S"), "input keyevent KEYCODE_INFO", "INFO"),
        CommandItem("nav.captions", "Subtitles / Captions", "Toggle closed captions", "navigation", "closed_caption", listOf("R", "S"), "input keyevent KEYCODE_CAPTIONS", "CAPTIONS"),
        CommandItem("nav.channel_up", "Channel Up", "Next TV channel", "navigation", "expand_less", listOf("R", "S"), "input keyevent KEYCODE_CHANNEL_UP", "CHANNEL_UP"),
        CommandItem("nav.channel_down", "Channel Down", "Previous TV channel", "navigation", "expand_more", listOf("R", "S"), "input keyevent KEYCODE_CHANNEL_DOWN", "CHANNEL_DOWN"),
        CommandItem("nav.last_channel", "Last Channel", "Jump to previously watched channel", "navigation", "history", listOf("R", "S"), "input keyevent KEYCODE_LAST_CHANNEL"),
        CommandItem("nav.color_red", "Red Key", "Interactive red button", "navigation", "circle", listOf("R", "S"), "input keyevent KEYCODE_PROG_RED"),
        CommandItem("nav.color_green", "Green Key", "Interactive green button", "navigation", "circle", listOf("R", "S"), "input keyevent KEYCODE_PROG_GREEN"),
        CommandItem("nav.color_yellow", "Yellow Key", "Interactive yellow button", "navigation", "circle", listOf("R", "S"), "input keyevent KEYCODE_PROG_YELLOW"),
        CommandItem("nav.color_blue", "Blue Key", "Interactive blue button", "navigation", "circle", listOf("R", "S"), "input keyevent KEYCODE_PROG_BLUE"),

        // ---------------------------------------------------------
        // 6.2 Media & Playback
        // ---------------------------------------------------------
        CommandItem("media.play_pause", "Play / Pause", "Toggle active media playback", "media", "play_circle", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_PLAY_PAUSE", "MEDIA_PLAY_PAUSE"),
        CommandItem("media.play", "Play", "Start playback", "media", "play_arrow", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_PLAY", "MEDIA_PLAY"),
        CommandItem("media.pause", "Pause", "Pause playback", "media", "pause", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_PAUSE", "MEDIA_PAUSE"),
        CommandItem("media.stop", "Stop", "Stop playback", "media", "stop", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_STOP", "MEDIA_STOP"),
        CommandItem("media.next", "Next Track", "Skip to next track/video", "media", "skip_next", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_NEXT", "MEDIA_NEXT"),
        CommandItem("media.prev", "Previous Track", "Return to previous track", "media", "skip_previous", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_PREVIOUS", "MEDIA_PREVIOUS"),
        CommandItem("media.rewind", "Rewind (10s)", "Seek backward", "media", "fast_rewind", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_REWIND", "MEDIA_REWIND"),
        CommandItem("media.fast_forward", "Fast Forward (10s)", "Seek forward", "media", "fast_forward", listOf("R", "S"), "input keyevent KEYCODE_MEDIA_FAST_FORWARD", "MEDIA_FAST_FORWARD"),
        CommandItem("media.session_dispatch", "Target Active Session", "Direct command to background media session", "media", "radio", listOf("S"), "cmd media_session dispatch play-pause"),
        CommandItem("media.list_sessions", "List Media Sessions", "List all apps currently holding audio focus", "media", "queue_music", listOf("S"), "cmd media_session list-sessions", output = CommandOutput.TEXT),
        CommandItem("media.dumpsys", "Now Playing Metadata", "Extract current playing track and artist info", "media", "info", listOf("S"), "dumpsys media_session | grep -E \"metadata:|state=PlaybackState\"", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.3 Volume & Audio
        // ---------------------------------------------------------
        CommandItem("vol.up", "Volume Up", "Increase volume level", "volume", "volume_up", listOf("R", "S"), "input keyevent KEYCODE_VOLUME_UP", "VOLUME_UP"),
        CommandItem("vol.down", "Volume Down", "Decrease volume level", "volume", "volume_down", listOf("R", "S"), "input keyevent KEYCODE_VOLUME_DOWN", "VOLUME_DOWN"),
        CommandItem("vol.mute", "Mute / Unmute", "Toggle audio mute", "volume", "volume_off", listOf("R", "S"), "input keyevent KEYCODE_VOLUME_MUTE", "VOLUME_MUTE"),
        CommandItem("vol.set_exact", "Set Volume (0-15)", "Set exact volume level for media stream", "volume", "tune", listOf("S"), "cmd media_session volume --show --stream 3 --set {level}",
            params = listOf(CommandParam("level", "Volume Level (0-15)", "8"))),
        CommandItem("vol.read_state", "Read Audio Streams", "Inspect current volume levels across all streams", "volume", "graphic_eq", listOf("S"), "dumpsys audio | grep -A 4 \"- STREAM_MUSIC:\"", output = CommandOutput.TEXT),
        CommandItem("vol.surround_mode", "Audio Surround Output", "Set surround sound passthrough (0=Auto, 1=Never, 2=Always)", "volume", "surround_sound", listOf("S"), "settings put global encoded_surround_output {mode}",
            params = listOf(CommandParam("mode", "Surround Mode (0/1/2)", "0")), risk = RiskLevel.YELLOW, undoCommand = "settings put global encoded_surround_output 0"),

        // ---------------------------------------------------------
        // 6.4 Power, Sleep & Screen
        // ---------------------------------------------------------
        CommandItem("power.sleep", "Sleep (Standby)", "Put Android TV to low power sleep mode", "power", "bedtime", listOf("R", "S"), "input keyevent KEYCODE_SLEEP", "POWER"),
        CommandItem("power.wake", "Wake Up", "Wake TV from standby", "power", "wb_sunny", listOf("S"), "input keyevent KEYCODE_WAKEUP"),
        CommandItem("power.toggle", "Power Toggle", "Toggle power state", "power", "power_settings_new", listOf("R", "S"), "input keyevent KEYCODE_POWER", "POWER"),
        CommandItem("power.tv_power", "TV Power (CEC)", "Broadcast power to HDMI-CEC display", "power", "tv", listOf("S"), "input keyevent KEYCODE_TV_POWER"),
        CommandItem("power.screen_state", "Check Wakefulness", "Check if display is currently Awake or Asleep", "power", "visibility", listOf("S"), "dumpsys power | grep -E \"mWakefulness=|Display Power\"", output = CommandOutput.TEXT),
        CommandItem("power.screensaver", "Start Screensaver Now", "Launch Android daydream ambient screensaver", "power", "wallpaper", listOf("S"), "am start -n com.android.systemui/.Somnambulator"),
        CommandItem("power.stayon", "Stay Awake While Plugged In", "Prevent TV from entering sleep while powered", "power", "lock_clock", listOf("S"), "svc power stayon true", risk = RiskLevel.YELLOW, undoCommand = "svc power stayon false"),
        CommandItem("power.reboot", "Reboot Device", "Restart Android TV system", "power", "restart_alt", listOf("S"), "reboot", risk = RiskLevel.YELLOW),

        // ---------------------------------------------------------
        // 6.5 Inputs & Live TV
        // ---------------------------------------------------------
        CommandItem("input.hdmi1", "Switch to HDMI 1", "Select HDMI input port 1", "inputs", "input", listOf("R", "S"), "input keyevent KEYCODE_TV_INPUT_HDMI_1"),
        CommandItem("input.hdmi2", "Switch to HDMI 2", "Select HDMI input port 2", "inputs", "input", listOf("R", "S"), "input keyevent KEYCODE_TV_INPUT_HDMI_2"),
        CommandItem("input.hdmi3", "Switch to HDMI 3", "Select HDMI input port 3", "inputs", "input", listOf("R", "S"), "input keyevent KEYCODE_TV_INPUT_HDMI_3"),
        CommandItem("input.hdmi4", "Switch to HDMI 4", "Select HDMI input port 4", "inputs", "input", listOf("R", "S"), "input keyevent KEYCODE_TV_INPUT_HDMI_4"),
        CommandItem("input.cycle", "Cycle TV Inputs", "Cycle through connected input ports", "inputs", "sync_alt", listOf("R", "S"), "input keyevent KEYCODE_TV_INPUT"),
        CommandItem("input.tv", "Live TV / Antenna", "Switch to antenna / cable input", "inputs", "live_tv", listOf("R", "S"), "input keyevent KEYCODE_TV"),
        CommandItem("input.list", "List TV Hardware Inputs", "Query registered TV input services and ports", "inputs", "format_list_bulleted", listOf("S"), "dumpsys tv_input", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.6 Text & Keyboard
        // ---------------------------------------------------------
        CommandItem("text.send_ascii", "Type Text (Shell)", "Type ASCII text on the active input field", "text", "keyboard", listOf("S"), "input text '{text}'",
            params = listOf(CommandParam("text", "Text to type", "Hello TV"))),
        CommandItem("text.paste", "Paste Clipboard", "Trigger paste key event", "text", "content_paste", listOf("S"), "input keyevent KEYCODE_PASTE"),
        CommandItem("text.copy", "Copy", "Trigger copy key event", "text", "content_copy", listOf("S"), "input keyevent KEYCODE_COPY"),
        CommandItem("text.delete", "Backspace", "Delete preceding character", "text", "backspace", listOf("R", "S"), "input keyevent KEYCODE_DEL", "DEL"),
        CommandItem("text.enter", "Enter / Submit", "Submit text input field", "text", "keyboard_return", listOf("R", "S"), "input keyevent KEYCODE_ENTER", "ENTER"),
        CommandItem("text.select_all", "Select All", "Select all text (Ctrl+A)", "text", "select_all", listOf("S"), "input keycombination KEYCODE_CTRL_LEFT KEYCODE_A"),
        CommandItem("text.list_ime", "List Installed Keyboards", "List all input methods registered on TV", "text", "list", listOf("S"), "ime list -s", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.7 Pointer & Touch
        // ---------------------------------------------------------
        CommandItem("touch.tap", "Tap Coordinates", "Simulate screen tap at X Y", "touch", "touch_app", listOf("S"), "input tap {x} {y}",
            params = listOf(CommandParam("x", "X Coordinate", "960"), CommandParam("y", "Y Coordinate", "540"))),
        CommandItem("touch.swipe", "Swipe Gesture", "Simulate swipe from X1,Y1 to X2,Y2 in MS", "touch", "swipe", listOf("S"), "input swipe {x1} {y1} {x2} {y2} {duration}",
            params = listOf(CommandParam("x1", "Start X", "960"), CommandParam("y1", "Start Y", "800"), CommandParam("x2", "End X", "960"), CommandParam("y2", "End Y", "300"), CommandParam("duration", "Duration ms", "300"))),
        CommandItem("touch.screensize", "Read Screen Resolution", "Display physical screen resolution in pixels", "touch", "aspect_ratio", listOf("S"), "wm size", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.8 App Management
        // ---------------------------------------------------------
        CommandItem("apps.list_3rd", "List User Installed Apps", "List all 3rd-party user installed packages", "apps", "view_list", listOf("S"), "pm list packages -3", output = CommandOutput.TEXT),
        CommandItem("apps.list_leanback", "List Leanback TV Apps", "Query launcher activities optimized for TV", "apps", "tv", listOf("S"), "cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER", output = CommandOutput.TEXT),
        CommandItem("apps.focused", "Current Focused App", "Show package name of currently active TV screen", "apps", "center_focus_strong", listOf("S"), "dumpsys window | grep -E \"mCurrentFocus|mFocusedApp\"", output = CommandOutput.TEXT),
        CommandItem("apps.force_stop", "Force Stop App", "Terminate app process immediately", "apps", "cancel", listOf("S"), "am force-stop {pkg}",
            params = listOf(CommandParam("pkg", "Package Name", "com.google.android.youtube.tv")), risk = RiskLevel.YELLOW),
        CommandItem("apps.clear_cache", "Trim All App Caches", "Frees storage space by clearing cached app data", "apps", "cleaning_services", listOf("S"), "pm trim-caches 999G", risk = RiskLevel.YELLOW, output = CommandOutput.TEXT),
        CommandItem("apps.clear_data", "Clear App Data", "Reset app to factory state (signs out user)", "apps", "delete_forever", listOf("S"), "pm clear {pkg}",
            params = listOf(CommandParam("pkg", "Package Name", "")), risk = RiskLevel.RED),
        CommandItem("apps.open_settings", "Open App Details", "Open system settings page for app", "apps", "settings_applications", listOf("S"), "am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:{pkg}",
            params = listOf(CommandParam("pkg", "Package Name", "com.google.android.youtube.tv"))),

        // ---------------------------------------------------------
        // 6.9 Deep Links & URLs
        // ---------------------------------------------------------
        CommandItem("link.youtube", "Launch YouTube", "Open YouTube Leanback TV app", "links", "smart_display", listOf("R", "S"), "monkey -p com.google.android.youtube.tv -c android.intent.category.LEANBACK_LAUNCHER 1", remoteKey = "https://www.youtube.com"),
        CommandItem("link.netflix", "Launch Netflix", "Open Netflix TV app", "links", "movie", listOf("R", "S"), "monkey -p com.netflix.ninja -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.prime", "Launch Prime Video", "Open Amazon Prime Video", "links", "ondemand_video", listOf("R", "S"), "monkey -p com.amazon.amazonvideo.livingroom -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.disney", "Launch Disney+", "Open Disney+ TV app", "links", "star", listOf("R", "S"), "monkey -p com.disney.disneyplus -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.spotify", "Launch Spotify", "Open Spotify Music app", "links", "music_note", listOf("R", "S"), "monkey -p com.spotify.tv.android -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.plex", "Launch Plex", "Open Plex Media Player", "links", "play_circle_filled", listOf("R", "S"), "monkey -p com.plexapp.android -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.kodi", "Launch Kodi", "Open Kodi Entertainment Center", "links", "folder_special", listOf("R", "S"), "monkey -p org.xbmc.kodi -c android.intent.category.LEANBACK_LAUNCHER 1"),
        CommandItem("link.playstore", "Open Google Play Store", "Browse Android TV apps & games", "links", "shopping_bag", listOf("R", "S"), "am start -a android.intent.action.VIEW -d \"market://details?id={pkg}\"",
            params = listOf(CommandParam("pkg", "App Package or empty", "com.google.android.youtube.tv"))),
        CommandItem("link.custom_url", "Open Custom URL / Stream", "Open web URL or video stream link on TV", "links", "open_in_browser", listOf("R", "S"), "am start -a android.intent.action.VIEW -d \"{url}\"",
            params = listOf(CommandParam("url", "Full URL", "https://www.youtube.com"))),

        // ---------------------------------------------------------
        // 6.10 Settings Shortcuts
        // ---------------------------------------------------------
        CommandItem("set.main", "TV Main Settings", "Open full TV settings panel", "settings", "settings", listOf("S"), "am start -a android.settings.SETTINGS"),
        CommandItem("set.wifi", "Wi-Fi Settings", "Direct jump to Wi-Fi networks configuration", "settings", "wifi", listOf("S"), "am start -a android.settings.WIFI_SETTINGS"),
        CommandItem("set.bluetooth", "Bluetooth Accessories", "Pair new remotes, gamepads, or audio devices", "settings", "bluetooth", listOf("S"), "am start -a android.settings.BLUETOOTH_SETTINGS"),
        CommandItem("set.display", "Display Settings", "Display resolution, HDR, and screen positioning", "settings", "tv", listOf("S"), "am start -a android.settings.DISPLAY_SETTINGS"),
        CommandItem("set.sound", "Sound & Audio Settings", "Surround formats, volume balance, audio output", "settings", "volume_up", listOf("S"), "am start -a android.settings.SOUND_SETTINGS"),
        CommandItem("set.dev", "Developer Options", "USB debugging and developer toggles", "settings", "developer_mode", listOf("S"), "am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
        CommandItem("set.apps", "All Applications List", "Manage installed apps, clear data, set permissions", "settings", "apps", listOf("S"), "am start -a android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS"),
        CommandItem("set.storage", "Internal Storage Info", "View storage breakdown and free space", "settings", "storage", listOf("S"), "am start -a android.settings.INTERNAL_STORAGE_SETTINGS"),

        // ---------------------------------------------------------
        // 6.11 System Tweaks
        // ---------------------------------------------------------
        CommandItem("tweak.speed_ui", "Speed Up UI Animations (0.5x)", "Makes TV navigation feel twice as fast", "tweaks", "speed", listOf("S"), "settings put global window_animation_scale 0.5 && settings put global transition_animation_scale 0.5 && settings put global animator_duration_scale 0.5",
            undoCommand = "settings put global window_animation_scale 1.0 && settings put global transition_animation_scale 1.0 && settings put global animator_duration_scale 1.0"),
        CommandItem("tweak.disable_adb_timeout", "Disable ADB 7-Day Revocation", "Prevents wireless debugging authorization from expiring after 7 days", "tweaks", "timer_off", listOf("S"), "settings put global adb_allowed_connection_time 0", risk = RiskLevel.YELLOW),
        CommandItem("tweak.font_scale", "Set Font Scale", "Adjust system UI text scale", "tweaks", "format_size", listOf("S"), "settings put system font_scale {scale}",
            params = listOf(CommandParam("scale", "Scale (1.0 default, 1.25 large)", "1.0"))),
        CommandItem("tweak.enable_dev_options", "Enable Developer Settings", "Activate Developer options toggle", "tweaks", "code", listOf("S"), "settings put global development_settings_enabled 1"),

        // ---------------------------------------------------------
        // 6.12 Display
        // ---------------------------------------------------------
        CommandItem("disp.get_size", "Get Display Size & Density", "Show resolution and DPI metrics", "display", "aspect_ratio", listOf("S"), "wm size && wm density", output = CommandOutput.TEXT),
        CommandItem("disp.reset", "Reset Resolution & Density", "Restore default TV display settings", "display", "restart_alt", listOf("S"), "wm size reset && wm density reset"),
        CommandItem("disp.dumpsys", "Display Capabilities & HDR", "Query supported refresh rates (60/120Hz) and HDR modes", "display", "hdr_on", listOf("S"), "dumpsys display | grep -E \"mSupportedModes|mDefaultModeId|colorMode\"", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.13 Network
        // ---------------------------------------------------------
        CommandItem("net.ip", "IP & Route Info", "Inspect IPv4 addresses on network interfaces", "network", "lan", listOf("S"), "ip -4 addr show", output = CommandOutput.TEXT),
        CommandItem("net.mac", "Get Wi-Fi MAC Address", "Read MAC address for Wake-on-LAN configuration", "network", "fingerprint", listOf("S"), "cat /sys/class/net/wlan0/address || cat /sys/class/net/eth0/address", output = CommandOutput.TEXT),
        CommandItem("net.wifi_status", "Wi-Fi Signal & Link Speed", "Read RSSI, frequency, and connection quality", "network", "wifi_find", listOf("S"), "dumpsys wifi | grep -E \"mWifiInfo|SSID|RSSI|Link speed|Frequency\"", output = CommandOutput.TEXT),
        CommandItem("net.ping", "Internet Latency Ping", "Ping Cloudflare DNS to check Internet connectivity", "network", "network_check", listOf("S"), "ping -c 3 -W 2 1.1.1.1", output = CommandOutput.TEXT),
        CommandItem("net.ports", "Listening Network Ports", "Inspect open listening TCP ports on TV", "network", "hub", listOf("S"), "ss -tln || netstat -tln", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.14 Bluetooth
        // ---------------------------------------------------------
        CommandItem("bt.bonded", "Bonded Bluetooth Accessories", "List paired remote controls, headphones, gamepads", "bluetooth", "bluetooth_connected", listOf("S"), "dumpsys bluetooth_manager", output = CommandOutput.TEXT),
        CommandItem("bt.battery", "Remote Control Battery Level", "Check battery level of TV remote accessory", "bluetooth", "battery_charging_full", listOf("S"), "dumpsys input | grep -i \"battery\" || dumpsys bluetooth_manager | grep -i \"battery\"", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.15 Files & Storage
        // ---------------------------------------------------------
        CommandItem("file.df", "Disk Space Breakdown", "Inspect free space on /data and /sdcard", "files", "pie_chart", listOf("S"), "df -h /data /sdcard", output = CommandOutput.TEXT),
        CommandItem("file.ls_downloads", "List Download Folder", "Show files in /sdcard/Download", "files", "download", listOf("S"), "ls -la /sdcard/Download", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.16 Capture & Screen Record
        // ---------------------------------------------------------
        CommandItem("cap.screenshot", "Take TV Screenshot", "Capture screen to /sdcard/screenshot.png", "capture", "screenshot", listOf("S"), "screencap -p /sdcard/tvdeck_shot.png", output = CommandOutput.TEXT),
        CommandItem("cap.record", "Screen Record (30s)", "Record TV screen to MP4 video", "capture", "videocam", listOf("S"), "screenrecord --time-limit 30 --bit-rate 4000000 /sdcard/tvdeck_rec.mp4", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.17 Live Diagnostics
        // ---------------------------------------------------------
        CommandItem("diag.device_info", "TV Hardware Identity", "Manufacturer, model, SoC, and build fingerprint", "diagnostics", "badge", listOf("S"), "getprop ro.product.manufacturer && getprop ro.product.model && getprop ro.build.version.release && getprop ro.build.version.security_patch", output = CommandOutput.TEXT),
        CommandItem("diag.uptime", "Uptime & System Load", "System uptime and load averages", "diagnostics", "timer", listOf("S"), "uptime", output = CommandOutput.TEXT),
        CommandItem("diag.meminfo", "RAM Memory Usage", "RAM usage breakdown across system and apps", "diagnostics", "memory", listOf("S"), "dumpsys meminfo | head -n 25", output = CommandOutput.TEXT),
        CommandItem("diag.thermal", "Thermal Sensors", "Query SoC temperature sensor readings", "diagnostics", "thermostat", listOf("S"), "dumpsys thermalservice || cat /sys/class/thermal/thermal_zone*/temp", output = CommandOutput.TEXT),
        CommandItem("diag.top", "Top CPU Processes", "List processes consuming highest CPU resources", "diagnostics", "leaderboard", listOf("S"), "top -b -n 1 | head -n 20", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.18 HDMI-CEC
        // ---------------------------------------------------------
        CommandItem("cec.onetouchplay", "CEC One Touch Play", "Wake display & switch TV input to this device", "cec", "power", listOf("S"), "cmd hdmi_control onetouchplay"),
        CommandItem("cec.bus_state", "CEC Bus Status", "List all connected HDMI devices (TV, AVR, Soundbar)", "cec", "account_tree", listOf("S"), "dumpsys hdmi_control", output = CommandOutput.TEXT),
        CommandItem("cec.help", "CEC Supported Commands", "List HDMI-CEC sub-commands supported on this device", "cec", "help_outline", listOf("S"), "cmd hdmi_control help", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.19 Developer Tools
        // ---------------------------------------------------------
        CommandItem("dev.logcat_w", "Logcat Warnings & Errors", "Read latest 100 log lines with warning/error severity", "dev", "bug_report", listOf("S"), "logcat -d -v time -t 100 *:W", output = CommandOutput.TEXT),
        CommandItem("dev.logcat_clear", "Clear Logcat Buffer", "Flush system log ring buffer", "dev", "clear_all", listOf("S"), "logcat -c"),
        CommandItem("dev.services", "List Registered Services", "List all active binder services on device", "dev", "miscellaneous_services", listOf("S"), "cmd -l", output = CommandOutput.TEXT),
        CommandItem("dev.features", "List System Features", "List hardware and software features reported by PM", "dev", "featured_play_list", listOf("S"), "pm list features", output = CommandOutput.TEXT),

        // ---------------------------------------------------------
        // 6.20 Debloat & Privacy
        // ---------------------------------------------------------
        CommandItem("debloat.reinstall", "Restore Uninstalled App", "Reinstall system app removed for current user", "debloat", "restore", listOf("S"), "cmd package install-existing {pkg}",
            params = listOf(CommandParam("pkg", "Package Name", ""))),
        CommandItem("debloat.disable_telemetry", "Disable Package", "Disable user access to background bloatware", "debloat", "block", listOf("S"), "pm disable-user --user 0 {pkg}",
            params = listOf(CommandParam("pkg", "Package Name", "")), risk = RiskLevel.YELLOW, undoCommand = "pm enable {pkg}"),

        // ---------------------------------------------------------
        // 6.21 Danger Zone
        // ---------------------------------------------------------
        CommandItem("danger.recovery", "Reboot to Recovery", "Restart into Android Recovery mode", "danger", "warning", listOf("S"), "reboot recovery", risk = RiskLevel.RED),
        CommandItem("danger.bootloader", "Reboot to Bootloader", "Restart into Fastboot / Bootloader", "danger", "warning", listOf("S"), "reboot bootloader", risk = RiskLevel.RED),
        CommandItem("danger.poweroff", "Power Off TV", "Shut down system completely (needs manual physical power on)", "danger", "power_off", listOf("S"), "reboot -p", risk = RiskLevel.RED)
    )

    fun getCommandsByCategory(categoryId: String): List<CommandItem> {
        return commands.filter { it.category == categoryId }
    }

    fun searchCommands(query: String): List<CommandItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return commands
        return commands.filter {
            it.label.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.shell.lowercase().contains(q)
        }
    }
}
