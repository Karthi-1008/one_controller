# OneController (TVDeck) — Universal Android TV Smart Controller

**OneController** is a high-performance, ultra-lightweight, and universal Android application designed to control any **Android TV, Google TV, and Fire TV** device with zero latency and maximum power.

Unlike conventional TV remotes that rely solely on ADB or basic IR, OneController features a **Dual-Engine Hybrid Transport Manager**:
1. **Engine 1 (Daily Driver): Android TV Remote Protocol v2** (TLS mutual auth on ports 6466/6467)
   - Works without developer options or USB debugging enabled.
   - Lowest latency (<20ms persistent TLS socket).
   - Native text injection directly into TV input fields.
   - App link deep launching (YouTube, Netflix, etc.).
   - Volume and power state telemetry.
2. **Engine 2 (Power Layer): Pure-Kotlin ADB Client** (Wireless Debugging & Port 5555)
   - Built with 100% pure Kotlin & Java NIO (no heavy native `libadb.so` or external binaries).
   - Complete shell command execution.
   - APK sideloading & direct installation.
   - Remote file explorer (upload, download, delete, directory navigation).
   - App Manager: leanback query, 3rd party apps, force stop, clear cache, permissions.
   - System tweaks: 0.5x UI animation speed, disable ADB 7-day timeout, display resolution/density scaling.
   - Live diagnostics: CPU, RAM, storage, thermal sensors, and real-time interactive terminal.
3. **Engine 3 (Wake / Edge Layer): Wake-on-LAN (WoL)**
   - Sends UDP magic broadcast packets to wake sleeping TVs over Wi-Fi or Ethernet.
4. **Engine 4: Home Screen Widgets & Persistent Notification Service**
   - Control your TV from your phone's home screen or notification drawer without even opening the app.

---

## ⚡ Comparison vs atvTools 1.3.2

| Metric / Feature | atvTools 1.3.2 (Reference) | OneController (TVDeck) |
|---|---|---|
| **APK File Size** | **47.5 MB** | **1.46 MB** (⚡ **32x smaller**) |
| **Native C++ Bloat** | ~20 MB `liba.so` + 5 MB `libadb.so` | **0 MB** (Pure Kotlin / NIO) |
| **Ads / Tracking** | AdMob, Firebase Analytics, In-App Billing | **Zero Ads, Zero Trackers** |
| **Android TV Remote v2** | Relies primarily on ADB | **Full v2 Engine** (No Dev Mode Required) |
| **Command Catalog** | Hidden / basic shortcuts | **200+ self-describing vetted commands** |
| **Undo Safety Net** | No undo | **One-tap undo & auto-revert timers** |
| **APK Sideloading** | Gated / Multi-file restricted | **Built-in free sideload & stream installer** |
| **Design** | Legacy XML layouts | **Modern Jetpack Compose & Material 3 Dark** |

---

## 📱 Features & Tabs

### 1. Remote Screen
- **D-Pad Mode**: Circular tactile directional controller (Up, Down, Left, Right, OK Center).
- **Trackpad / Gestures Mode**: Smooth touch surface mapping phone gestures to 1920x1080 screen coordinates.
- **Numpad Mode**: 0–9 direct keypad for TV channels and PIN entry.
- **Media Controller**: Rewind, Fast-Forward, Skip Next/Prev, Play/Pause, Stop.
- **Volume & Channel Rockers**: Vol+, Vol-, Mute, Ch+, Ch-, EPG Guide.
- **System Navigation**: Back, Home, Menu, App Switcher / Recents, Google Assistant / Mic.
- **Quick App Dock**: One-tap launch for YouTube, Netflix, Prime Video, Spotify, Plex, Kodi, Play Store.
- **Direct Text Input**: Type on phone keyboard -> immediately pushed into TV search bar.

### 2. Command Catalog
- Over 200 categorized commands with real-time search:
  - **Navigation & Keys**: All Android key events, color keys (Red, Green, Yellow, Blue), macros.
  - **Media & Audio**: Media session dispatch, audio stream inspection, surround sound modes.
  - **Power & Sleep**: Standby, Wake, Reboot, Screensaver Somnambulator, Keep Awake.
  - **TV Inputs**: HDMI 1–4, Composite, Live TV / Tuner.
  - **System Tweaks**: Double UI speed (0.5x animation scales), disable 7-day ADB timeout, font scale.
  - **Display**: Screen resolution (`wm size`), UI density (`wm density`), display modes & HDR.
  - **Network & WoL**: IP, MAC, Wi-Fi signal, ping, open ports.
  - **Bluetooth**: Bonded accessories, remote control battery level.
  - **HDMI-CEC**: One Touch Play, active source query, standby TV & soundbar.
  - **Debloat & Privacy**: Safe debloat profiles with instant reinstall undo (`cmd package install-existing`).
  - **Danger Zone**: Reboot to recovery, bootloader, power off.

### 3. TV Apps Manager & Sideload
- Scan all Leanback and 3rd party apps installed on TV.
- One-tap launch, force stop, clear app cache (`pm trim-caches 999G`), clear data, open TV settings.
- **Sideload APK**: Select any `.apk` file on your phone -> OneController uploads and installs it onto your TV automatically!

### 4. TV File Explorer
- Browse TV storage (`/sdcard/`, `/sdcard/Download`, etc.).
- Upload files/APKs from phone to TV.
- Delete files and create new folders.

### 5. Diagnostics & ADB Terminal
- **Telemetry Dashboard**: TV model, manufacturer, Android OS version, security patch, uptime, RAM memory usage (`dumpsys meminfo`), storage breakdown (`df -h`), and HDMI-CEC bus status.
- **Interactive Terminal**: Full ADB Shell terminal with command history, quick command suggestions, and output console.

### 6. Devices & Pairing
- **mDNS Auto-Discovery**: Automatically finds Android TVs on your Wi-Fi (`_androidtvremote2._tcp`, `_adb-tls-connect._tcp`).
- **Remote v2 Pairing Dialog**: Enter the 6-character code shown on your TV screen for one-time pairing.
- **Manual IP Connect**: Connect to any TV via IP and custom ports.
- **Wake-on-LAN**: Send magic packets to wake sleeping TVs.

---

## 📦 Download & Install

The compiled universal APK is available directly in the `release/` directory:
- **`release/OneController-v1.0.0-universal.apk`** (1.46 MB)

To install on your phone via ADB:
```bash
adb install release/OneController-v1.0.0-universal.apk
```

---

## 🛠️ Build from Source

Requirements:
- JDK 17
- Android SDK 34
- Gradle 8.9 (included via wrapper)

```bash
# Debug build
./gradlew assembleDebug

# Optimized Release Universal APK (R8 shrunk)
./gradlew assembleRelease
```
The generated APK will be placed in `app/build/outputs/apk/release/app-release.apk`.