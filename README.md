# Cache Cleaner - Mobile Android App

A modern, high-performance Android mobile application built with **Kotlin** that scans installed applications, measures their cache and data storage footprints, and automates clearing temporary app caches using Android's Accessibility Automation architecture.

---

## ⚡ How It Works on Modern Android

On modern Android (Android 8.0 through Android 15+), Google deprecated the silent `CLEAR_APP_CACHE` permission for third-party apps to safeguard user privacy. 

This app solves the problem using the **official, industry-standard approach** (the same method used by top tools like SD Maid, 1Tap Cleaner, and CCleaner):

1. **StorageStats Engine**:
   - Queries `StorageStatsManager` & `UsageStatsManager` (via `PACKAGE_USAGE_STATS`) to measure exact cache sizes for each installed package.
2. **Automated Accessibility Service Engine (`CacheCleanerAccessibilityService`)**:
   - When the user selects apps and taps **Auto Clean**, the app queues the packages.
   - It navigates to each app's System App Info screen (`Settings > Apps > [App] > Storage & cache`).
   - The Accessibility Service identifies the **"Clear cache"** button, simulates the user tap, and proceeds automatically to the next app in sequence.
3. **1-Tap Assisted Deep-Links**:
   - For users who do not want to enable Accessibility, each app item includes a direct settings shortcut button that takes them straight to that specific app's storage management screen.
4. **Shared Storage Junk Sweeper**:
   - Scans shared device storage for orphan `.tmp`, `.log`, and thumbnail caches.

---

## 📱 Project Architecture

```
Cache Cleaner/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              # Permissions & Service declarations
│   │   ├── java/com/cachecleaner/app/
│   │   │   ├── model/
│   │   │   │   ├── AppInfo.kt               # App metadata, storage, and cache sizes
│   │   │   │   └── JunkItem.kt              # Temp files & junk categorization
│   │   │   ├── scanner/
│   │   │   │   ├── AppStorageScanner.kt     # StorageStatsManager & PackageManager scanner
│   │   │   │   └── JunkScanner.kt           # Shared storage temp/cache file scanner
│   │   │   ├── service/
│   │   │   │   ├── CacheCleanerAccessibilityService.kt # UI automation macro
│   │   │   │   └── CleaningManager.kt       # Batch queue orchestrator
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt          # Dashboard, storage gauge, permissions flow
│   │   │       └── AppAdapter.kt            # Dynamic RecyclerView adapter with multi-select
│   │   └── res/
│   │       ├── layout/                      # UI layouts (activity_main, item_app_cache, dialog)
│   │       ├── values/                      # Themes, strings, and modern dark-mode colors
│   │       └── xml/                         # accessibility_service_config.xml
│   └── build.gradle.kts                     # App module dependencies & SDK configs (Target SDK 34)
├── preview/                                 # Interactive Web Mobile Simulator
│   ├── index.html
│   ├── style.css
│   └── app.js
├── build.gradle.kts                         # Root Gradle config
├── settings.gradle.kts                      # Module settings
└── README.md
```

---

## 🛠️ How to Build and Run on Android

### Option A: Open with Android Studio
1. Open **Android Studio**.
2. Click **File > Open...** and select this directory (`Cache Cleaner`).
3. Allow Gradle to sync dependencies.
4. Connect an Android device (via USB with USB Debugging enabled) or start an Android Emulator.
5. Click **Run** (green play button or `Shift + F10`).

### Option B: Build via Command Line (Gradle)
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Permissions & Setup on Device

1. **Usage Access Permission**:
   - The app will prompt you on first launch to allow **"Usage Access"** in System Settings so it can read `StorageStatsManager` cache byte sizes.
2. **Accessibility Service**:
   - When you click **Auto Clean**, the app will direct you to `Settings > Accessibility > Installed apps > Cache Cleaner Automation` to enable the automated macro.

---

## 🌐 Instant Interactive Simulator Preview

Want to see and interact with the UI, test cache scanning, selection, sorting, and automated cleaning right now on your computer?
Open `preview/index.html` in any browser or launch a local preview server:
```bash
npx serve preview
```
