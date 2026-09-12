# AppMaze

**Don't block the distraction. Break the habit.**

## What is AppMaze?

AppMaze is a digital-wellbeing Android launcher that creates small amounts of friction to help you break the habit of automatically opening distracting apps.

Instead of completely blocking applications, AppMaze **changes the position** of distracting apps on its launcher grid. This breaks your muscle memory — you can still use the app if you intentionally want to, but you can't open it automatically without thinking.

### How It Works

```
Normal Layout:              After Scrambling:
┌────┬────┬────┬────┐      ┌────┬────┬────┬────┐
│ 📷 │ IG │ YT │ 🎵 │      │ 📷 │ 🎵 │ 🗺️ │ YT │
├────┼────┼────┼────┤      ├────┼────┼────┼────┤
│ 💬 │ 📞 │ ⚙️ │ 🗺️ │      │ 💬 │ ⚙️ │ IG │ 📞 │
└────┴────┴────┴────┘      └────┴────┴────┴────┘

Instagram moved — still accessible, just harder to tap automatically.
```

## Features

### 🏠 Real Android Launcher
- Functions as a legitimate Android home launcher
- Displays all installed apps in a configurable grid
- Supports app search, icons, labels, and smooth transitions
- Set as default launcher through Android system settings

### 🔀 Smart Scrambling
- **Gentle**: Small movement, infrequent scrambling
- **Moderate**: Noticeable movement, balanced frequency
- **Strong**: Large movement, maximum unpredictability
- Only scrambles apps marked as distracting
- Essential apps (Phone, Settings, Emergency) are protected

### 📊 Screen-Time Tracking
- Uses Android's UsageStatsManager for accurate tracking
- Daily and weekly usage per app
- Distraction scoring (0-100 based on daily usage)
- All data stays on-device — nothing is transmitted

### ⚡ Intervention Dialog
- Optional prompt before opening highly distracting apps
- Shows current session usage time
- "Open Anyway" always works — never blocks access
- "Take a Break" returns to launcher

### 📈 Dashboard
- Today's total screen time
- Top apps by usage with visual bars
- Distracting apps list
- AppMaze impact statistics (scrambles, estimated time saved)

### ⚙️ Settings
- Scrambling enable/disable, intensity, threshold, frequency
- Grid columns (3-6), show/hide labels
- Light / Dark / System theme
- Privacy controls (Usage Access status, clear statistics)
- Demo mode for testing

### 🎯 Onboarding
- 6-screen guided setup
- Choose distracting apps
- Enable Usage Access
- Select scrambling intensity
- Set as default launcher

### 🔒 Privacy
- 100% local operation — no server, no analytics, no login
- Screen-time data never leaves the device
- Minimal permissions (QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS)

## Architecture

```
com.appmaze
├── AppMazeApplication          # Application class
├── launcher/
│   ├── LauncherActivity        # Main HOME activity
│   └── LauncherViewModel       # Central state management
├── apps/
│   ├── AppInfo                 # App data model
│   └── InstalledAppsManager    # PackageManager wrapper
├── usage/
│   ├── UsageStatsRepository    # UsageStatsManager wrapper
│   └── UsageAnalyzer           # Distraction scoring
├── scrambling/
│   └── ScramblingEngine        # Position scrambling algorithm
├── data/
│   ├── AppDatabase             # Room database + entities + DAOs
│   └── SettingsRepository      # DataStore preferences
└── ui/
    ├── theme/Theme.kt          # Material 3 theming
    ├── components/AppGridItem  # Reusable app icon component
    └── screens/
        ├── LauncherScreen      # Home launcher grid
        ├── OnboardingScreen    # First-run experience
        ├── DashboardScreen     # Usage statistics
        ├── SettingsScreen      # All app settings
        └── ManageAppsScreen    # Distracting app management + Demo
```

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | UI framework |
| Material 3 | Design system |
| Room | Database (positions, usage, events) |
| DataStore | User preferences |
| Kotlin Coroutines | Async operations |
| StateFlow | Reactive state management |
| UsageStatsManager | Screen-time tracking |
| PackageManager | App discovery |
| Navigation Compose | Screen navigation |

## Android Permissions

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Discover all installed launchable apps |
| `PACKAGE_USAGE_STATS` | Read screen-time data (requires user to enable in Settings) |

### Usage Access

AppMaze requires **Usage Access** permission to track screen time. This is a special permission that must be enabled manually by the user through Android Settings.

Without this permission:
- The launcher still works fully
- Manual distraction marking still works
- Manual scrambling still works
- Screen-time statistics are unavailable
- Automatic distraction detection is disabled

## How Scrambling Works

1. **Detection**: Apps that exceed the usage threshold (default: 45 minutes) are flagged
2. **Classification**: Apps can be auto-detected or manually marked as distracting
3. **Scoring**: Each app gets a distraction score (0-100) based on daily usage
4. **Scrambling**: The ScramblingEngine swaps distracting apps to new grid positions
5. **Protection**: Essential apps (Phone, Settings, etc.) are never scrambled

### Scoring System

| Daily Usage | Score |
|---|---|
| 0–15 minutes | 0 |
| 15–30 minutes | 25 |
| 30–60 minutes | 50 |
| 60–90 minutes | 75 |
| 90+ minutes | 100 |

## Build Instructions

### Prerequisites
- JDK 17
- Android SDK (API 35)
- Build Tools 35.0.0+

### Build Debug APK

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install APK

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Launcher Setup

After installing:
1. Press the Home button
2. Select "AppMaze" from the launcher chooser
3. Optionally set as "Always" to make it the default

Or go to **Settings → Apps → Default Apps → Home app → AppMaze**

## Demo Mode

Enable Demo Mode in Settings → Developer to:
- Simulate usage data (+30 min increments)
- Mark/unmark apps as distracting
- Trigger immediate scrambling
- Reset layout to default positions

Demo mode data is isolated and does not affect real UsageStats.

## Known Limitations

1. **Usage Stats Accuracy**: UsageStatsManager may not report exact real-time data; there can be a short delay
2. **Scrambling Timing**: Automatic scrambling is checked when the launcher resumes, not via a background service, to minimize battery usage
3. **App Install/Uninstall**: New apps are detected on launcher resume; there is no broadcast receiver for real-time package changes
4. **Widget Support**: AppMaze does not currently support home screen widgets
5. **Wallpaper**: AppMaze uses the Material 3 theme background, not the system wallpaper

## Testing Instructions

1. Install the APK
2. Open AppMaze
3. Complete onboarding (select distracting apps, enable Usage Access, choose intensity)
4. Set AppMaze as default launcher
5. Verify installed apps appear in the grid
6. Search for apps using the search bar
7. Enable Demo Mode in Settings → Developer
8. Use demo controls to add usage and test scrambling
9. Verify scrambled apps change position
10. Verify essential apps remain stable
11. Test dark/light theme switching
12. Test intervention dialog on distracting apps
13. Check dashboard statistics

## License

This project is provided as-is for educational and personal use.
# Shuffle
