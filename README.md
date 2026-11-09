# ADHD Launcher

**Anti-Dopamine Human-factor Drawer Launcher for Android**

[![Android CI](https://img.shields.io/badge/Android_CI-Gradle_9_%2F_JDK_17-3DDC84?logo=android)](.github/workflows/android-ci.yml)
[![Min SDK](https://img.shields.io/badge/API-26%2B-blue)](app/build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36-brightgreen)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Apache_2.0-orange.svg)](LICENSE)

> **日本語概要**: ADHD傾向やスマホ依存（「無意識にSNSやショート動画を開いてしまうドーパミンループ」）を防ぐために設計された、完全無料・超軽量・OLED省電力のミニマルAndroidホームアプリです。

---

## Core Philosophy

1. **Zero Home Icons (No Visual Triggers)**: The home screen displays only a clean clock and a 1-line **Anti-Dopamine Anchor** ("What did I unlock my phone for?"). No notification badges or colorful app icons on the home screen.
2. **Ergonomic Bottom-Right Priority Drawer (`Swipe Up`)**: Apps are automatically categorized and stacked from the **bottom-right corner** (or bottom-left for left-handed users) upward based on real usage frequency (`UsageStatsManager` + launch history), placing your #1 most-used app right under your thumb.
3. **Instant Pull-Down Search (`Swipe Down`)**: Swiping down on the home screen opens a lightning-fast search bar with your top 4 frequent apps and incremental phonetic/romanized search.
4. **OLED True Black Power Saving (`#000000`)**: Double-tap the home screen or use the automatic night schedule (`22:00 - 07:00`) to turn off OLED pixels completely.
5. **Community-Driven JSON Dictionary**: App categorization rules and localized phonetic search aliases live in [`app/src/main/assets/dictionaries/app_dictionary.json`](app/src/main/assets/dictionaries/app_dictionary.json) — zero hardcoding in Kotlin. Anyone can add regional apps or new languages via Pull Request!

---

## Gesture Map

| Gesture | Context | Action (Configurable in Settings) |
|---|---|---|
| **Swipe Up** | Home Screen | Open App Drawer (Stacked from Bottom-Right by usage frequency) |
| **Swipe Down** | Home Screen | Open Quick Search + Top 4 Frequent Apps |
| **Swipe Down** | App Drawer | Close App Drawer |
| **Double Tap** | Home Screen | Toggle Wallpaper / True Black (`#000000`) Mode |
| **Long Press** | App Icon in Drawer | Change App Category / Hide App / App Info |

---

## Contributing Localized App Dictionaries (No Kotlin Required!)

We welcome Pull Requests from users around the world! To add regional apps or localized search keywords:

1. Edit [`app/src/main/assets/dictionaries/app_dictionary.json`](app/src/main/assets/dictionaries/app_dictionary.json).
2. Add package names to `package_rules`, localized category substrings to `category_keywords`, or phonetic/romanized readings to `search_aliases`.
3. Open a Pull Request — GitHub Actions will automatically validate the JSON schema and build the APK.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`app/src/main/assets/dictionaries/README.md`](app/src/main/assets/dictionaries/README.md) for details.

---

## Building from Source

### Prerequisites
- **JDK 17** or newer
- **Android SDK Platform 36**

### Command Line Build
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
