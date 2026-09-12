# Shuffle

**Don't block the distraction. Break the habit.**

Shuffle is a digital-wellbeing Android launcher engineered to eliminate mindless phone habits by breaking both **spatial muscle memory** and **visual icon muscle memory** — all while delivering an ultra-smooth, 120Hz native experience.

---

## 💡 The Philosophy: Friction Over Restriction

Most screen-time limiters rely on hard blocks or timers that users quickly disable out of frustration. **Shuffle takes a cognitive approach:**

1. **Spatial Habit Breaking (Position Scrambling)**: Distracting apps shift positions across the grid based on usage intensity.
2. **Visual Habit Breaking (Dynamic Icon Disguise)**: High-distraction apps temporarily borrow the visual appearance of low-usage utility apps.
3. **Anti-Routine Detection**: A lightweight Markov transition engine catches habitual app-hopping loops (e.g. *App A → App B → App C*).
4. **Mindful Friction**: Optional micro-interventions provide a moment of intentional pause without locking you out of your device.

```
Normal Launcher (Muscle Memory Active):
┌───────────┬───────────┬───────────┬───────────┐
│   Camera  │ Instagram │  YouTube  │   Music   │
├───────────┼───────────┼───────────┼───────────┤
│  Messages │   Phone   │  Settings │   Maps    │
└───────────┴───────────┴───────────┴───────────┘

Shuffle (Spatial & Visual Muscle Memory Broken):
┌───────────┬───────────┬───────────┬───────────┐
│   Camera  │   Music   │   Maps    │  YouTube  │
├───────────┼───────────┼───────────┼───────────┤
│  Messages │  Settings │ [IG-Calc] │   Phone   │
└───────────┴───────────┴───────────┴───────────┘
Instagram moved AND visually disguised as a utility app.
Muscle memory fails. Intentionality restored.
```

---

## ⚡ Key Features

### 🏠 High-Performance Launcher (120Hz Fluidity)
- Fully functional Android home launcher with default launcher intent handling.
- Configurable grid density (3 to 6 columns) with toggleable app labels.
- Fast, instant-filter search bar (automatically reveals real icons during search).
- Optimized with an asynchronous, thread-safe LRU **`IconCache`** and pre-warming on IO dispatchers to guarantee zero dropped frames on 90Hz/120Hz displays.

### 🔀 Smart Spatial Scrambling
- **Intensity Levels**:
  - *Gentle*: Subtle position shifts, infrequent adjustments.
  - *Moderate*: Balanced movement and cadence.
  - *Strong*: Maximum unpredictability for high-frequency habit loops.
- **Pattern-Aware (Markov Chain)**: Tracks launch sequences to detect predictable triggers and prioritizes scrambles for reflexive app sequences.
- **Protected Essentials**: Core apps (Phone, Dialer, SMS, Settings, Emergency) are strictly safeguarded and never scrambled.

### 🎭 Dynamic Icon Disguise (Visual Scrambling)
- Automatically disguises distracting app tiles with the icons of least-used, non-essential "donor" applications.
- Breaks instantaneous optical recognition that causes compulsive tapping.
- Includes a subtle, clean indicator dot/mask so you can identify the disguise upon closer inspection.
- Pure in-memory ephemeral mapping recomputed per scramble session; toggleable in Settings.

### 🧘 Focus Mode
- Built-in distraction-free timer with customizable durations (15m, 25m, 45m, 60m).
- Animated radial progress countdown powered by coroutines.
- Focus streak tracking and daily focused minutes stored in the local database.

### ⚡ Mindful Interventions
- Non-blocking friction prompt appearing before launching high-distraction apps.
- Displays current session usage and daily total.
- Choice between **"Take a Break"** (gentle exit back to home) and **"Open Anyway"** (never blocks intentional access).

### 📊 On-Device Analytics Dashboard
- Real-time daily screen-time summaries and usage breakdown bars.
- Live distraction score (0–100) per app.
- Impact metrics: total position scrambles, active disguised apps, and estimated mindful time saved.
- **100% Private**: All processing happens strictly on-device via Android's `UsageStatsManager` and Room DB. Zero telemetry, zero accounts, zero cloud dependencies.

### 🛠️ Developer & Demo Mode
- Integrated test suite accessible from Settings to simulate +30 min usage increments, force instant scrambles, trigger disguises, and reset grid layouts.

---

## 🏗️ Architecture & Project Structure

Shuffle is built following modern Android architecture standards using **Kotlin**, **Jetpack Compose**, and **MVI / Unidirectional Data Flow**.

```
com.appmaze
├── ShuffleApplication.kt       # Application entry point & lazy singletons
│
├── launcher/
│   ├── LauncherActivity.kt     # Single activity / android.intent.category.HOME
│   └── LauncherViewModel.kt    # Unified UI state, event dispatching, and orchestration
│
├── apps/
│   ├── AppInfo.kt              # App models and essential package definitions
│   ├── InstalledAppsManager.kt # PackageManager wrapper & package filtering
│   └── IconCache.kt            # Thread-safe LRU icon cache (120Hz smooth scrolling)
│
├── scrambling/
│   ├── ScramblingEngine.kt     # Position swap algorithms and intensity logic
│   ├── IconDisguiseEngine.kt   # Pure donor selection & visual disguise mapping
│   └── PatternDetector.kt      # Markov chain sequential transition analysis
│
├── focus/
│   └── FocusManager.kt         # Coroutine countdown, session persistence & streaks
│
├── usage/
│   ├── UsageStatsRepository.kt # UsageStatsManager bridge & interval calculations
│   └── UsageAnalyzer.kt        # Distraction classification & scoring algorithms
│
├── data/
│   ├── AppDatabase.kt          # Room entities, DAOs, and database migrations
│   └── SettingsRepository.kt   # DataStore-backed user preferences
│
└── ui/
    ├── theme/                  # Material 3 typography, color schemes, and theme
    ├── components/             # Reusable UI widgets (AppGridItem, Dialogs, etc.)
    └── screens/
        ├── LauncherScreen.kt   # Main home grid with smooth reorder animations
        ├── DashboardScreen.kt  # Usage metrics, streak counters, and impact stats
        ├── FocusScreen.kt      # Minimalist focus timer and session log
        ├── SettingsScreen.kt   # Configuration for scrambling, disguise, and grid
        ├── ManageAppsScreen.kt # Distraction overrides and developer demo tools
        └── OnboardingScreen.kt # 6-step first-run guided onboarding
```

---

## 🧰 Technology Stack

| Component | Library / Framework |
|---|---|
| **Language** | Kotlin 2.0+ |
| **UI Toolkit** | Jetpack Compose (BOM) & Material 3 |
| **Asynchronous Engine** | Kotlin Coroutines & `StateFlow` |
| **Local Persistence** | Room DB (entities, DAOs, streaks, events) |
| **Preferences** | Jetpack DataStore (Preferences) |
| **System Integrations** | Android `UsageStatsManager`, `PackageManager` |
| **Image & Icon Pipeline** | Android Graphic Drawables → Hardware Bitmaps with `LruCache` |
| **Architecture** | MVVM / Clean Architecture with single source of truth |

---

## 🔐 Permissions & Privacy

| Permission | Purpose | Type |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Discover installed launchable applications to populate the grid | Normal / Install-time |
| `PACKAGE_USAGE_STATS` | Read screen-time data to calculate distraction scores | Special access (User granted in Settings) |

> [!NOTE]
> **Privacy First**: Shuffle contains **no network permissions** in its `AndroidManifest.xml`. It cannot make network requests or transmit your usage data anywhere.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK Platform 35 (compileSdk 35, targetSdk 35, minSdk 26)

### Build Debug APK

```bash
# Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 First-Time Setup

1. Launch **Shuffle** or press the device **Home** button.
2. Follow the 6-step interactive onboarding:
   - Identify your distracting applications.
   - Grant **Usage Access** permission so Shuffle can monitor screen time.
   - Select your preferred scrambling intensity (Gentle, Moderate, Strong).
   - Set Shuffle as your default launcher.
3. Tap **Finish Setup** to land on your new mindful home screen.

---

## 🧪 Testing & Verification

1. **Smooth Scrolling**: Scroll rapidly through the launcher grid on a 90Hz/120Hz display to verify fluid rendering without frame drops.
2. **Dynamic Icon Disguise**: Mark an app as distracting (or use Settings → Developer → Demo Mode). Trigger a scramble; notice the app's position changes and its icon temporarily mimics a low-usage donor app.
3. **Search Transparency**: Open search and query the disguised app. Notice that the search bar displays the true app name and authentic icon for effortless intentional search.
4. **Intervention**: Launch a distracting app with high usage to confirm the friction dialog presents both *Take a Break* and *Open Anyway* options.
5. **Focus Mode**: Start a 15-minute focus session from the Focus tab to test the countdown timer, completion dialog, and streak counter.

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
