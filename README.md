# TermUp - Offline Linux Runtime + Desktop for Android

> **Package:** `com.qali.termup` | **App Name:** `termup` | **Target:** ARM64 Android

TermUp is a fully offline Linux runtime environment for Android with integrated VNC desktop, built on top of the [Termux](https://termux.dev) application. It bundles everything needed for offline operation:

- **Termux Bootstrap** - Core shell environment
- **proot-distro** - Distribution management
- **Ubuntu 24.04 ARM64 Rootfs** - Complete Ubuntu environment (pre-provisioned with systemd, dbus, ca-certificates, locales)
- **Box64** - Transparent x86_64 execution on ARM64 (runs inside Ubuntu proot)
- **XFCE Desktop** - Full desktop environment accessible via integrated VNC viewer

## Key Features

### 🖥️ Dual-Tab Interface
- **Terminals Tab** - Traditional Termux terminal with multi-session support
- **Desktop Tab** - Auto-connecting VNC viewer showing XFCE desktop (runs on localhost:5901)

### 📦 Offline-First
All runtime assets are prebundled in the APK:
- Ubuntu 24.04 ARM64 rootfs (~41MB pre-provisioned)
- Box64 binary + x86_64 glibc libraries
- XFCE desktop environment with TigerVNC server
- proot-distro scripts + proot binary + runtime libraries

### ⚡ One-Click Desktop
The Desktop tab automatically:
1. Starts VNC server inside Ubuntu proot on first access
2. Connects VNC viewer to `localhost:5901`
3. Shows XFCE desktop with full keyboard/mouse support

## Offline Build

```bash
# First build (downloads assets)
./gradlew assembleRelease

# Subsequent offline builds (no network required)
./gradlew assembleRelease -PtermupOffline=true

# Prepare assets only
./gradlew prepareTermUpAssets

# Clean downloaded assets
./gradlew cleanTermUpAssets

# Print build summary
./gradlew termupBuildSummary
```

---

## Installation

### GitHub Releases
TermUp APKs are available on [GitHub Releases](https://github.com/qaliblog/termup/releases).

**Note**: Only install `apt-android-7` variants for Android 7+.

### Building from Source
```bash
git clone git@github.com:qaliblog/termup.git
cd termup
./gradlew assembleRelease
```

## Usage

### Terminal Tab
- Standard Termux terminal experience
- Multi-session support via drawer
- Extra keys toolbar
- Full package management with `apt`/`pkg`

### Desktop Tab
1. Tap "Desktop" tab at top
2. Wait for "Starting desktop environment..." (first launch takes ~10-15s)
3. XFCE desktop appears with:
   - Application menu (top-left)
   - File manager (Thunar)
   - Terminal emulator (Xfce Terminal)
   - Text editor (Mousepad)
   - Image viewer (Ristretto)
   - Panel with workspace switcher

### Running GUI Apps
GUI applications installed in Ubuntu (via `apt install firefox`, etc.) will appear on the XFCE desktop and can be launched from the Applications menu.

### VNC Details
- **Host**: `127.0.0.1`
- **Port**: `5901` (display `:1`)
- **Security**: None (local-only)
- **Resolution**: `1920x1080` (24-bit color)

## Architecture

```
┌─────────────────────────────────────┐
│           TermUp APK                │
├─────────────────────────────────────┤
│  Termux Bootstrap (shell, apt)      │
│  proot-distro + proot               │
│  Ubuntu 24.04 ARM64 Rootfs          │
│    ├─ systemd + dbus                │
│    ├─ TigerVNC server               │
│    ├─ XFCE 4.18 desktop             │
│    └─ Box64 (x86_64 emulator)       │
│  Box64 x86_64 glibc libs            │
│  libnative-vnc.so (JNI)             │
└─────────────────────────────────────┘
```

## For Developers

### Project Structure
```
termup/
├── app/                    # Main Android app
│   ├── src/main/
│   │   ├── java/com/termux/app/
│   │   │   ├── fragments/  # TerminalsFragment, DesktopFragment
│   │   │   ├── vnc/        # VNC client (JNI wrapper)
│   │   │   └── adapters/   # MainPagerAdapter
│   │   ├── cpp/            # Native VNC library (libvncclient)
│   │   └── res/layout/     # TabLayout + ViewPager2 UI
├── termbox-assets/         # Offline asset preparation
│   ├── build.gradle        # prepareTermUpAssets task
│   ├── manifest.json       # Asset definitions
│   ├── prebuilt/           # Box64 binaries, provision script
│   └── config/             # VNC startup scripts
└── .github/workflows/      # CI/CD (builds VNC + APK)
```

### Customization
- **VNC Resolution**: Edit `termbox-assets/prebuilt/provision-rootfs.sh` (geometry setting)
- **VNC Password**: Modify `termbox-vnc-start` script in provision-rootfs.sh
- **Desktop Packages**: Add/remove packages in provision-rootfs.sh apt install section

## Important Links
- [Termux Wiki](https://wiki.termux.com/wiki/)
- [Termux Packages](https://github.com/termux/termux-packages)
- [Box64](https://github.com/ptitSeb/box64)
- [proot-distro](https://github.com/termux/proot-distro)
- [XFCE Desktop](https://xfce.org/)

## License
TermUp is licensed under the same licenses as its components:
- Termux: GPL-3.0
- Ubuntu base: CC-BY-SA 4.0
- Box64: MIT
- XFCE: GPL-2.0+

## Sponsors
[<img alt="GitHub" width="25%" src="site/assets/sponsors/github.png" />](https://github.com)

---
*Based on [Termux](https://github.com/termux/termux-app) - Extended with offline desktop capabilities*