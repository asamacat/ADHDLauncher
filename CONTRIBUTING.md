# Contributing to ADHD Launcher

Thank you for your interest in contributing to **ADHD Launcher**! Whether you are adding regional apps to the dictionary, translating strings, reporting bugs, or proposing new features, your help is welcome.

---

## 1. Adding Regional Apps & Language Keywords (Easiest Way!)

All app categorization rules and search aliases are stored in an external JSON file:
- [`app/src/main/assets/dictionaries/app_dictionary.json`](app/src/main/assets/dictionaries/app_dictionary.json)

### Categories
- `COMMUNICATION`: Messaging, email, dialer, social apps
- `FINANCE`: Mobile payments, banking, shopping, transit cards, loyalty/point apps
- `INTERNET`: Browsers, news readers, AI assistants, search tools
- `MEDIA`: Video, music, podcasts, gallery, camera
- `GAMES`: Mobile games
- `TOOLS`: Settings, clock, calculator, authenticators, file managers, maps

### Guidelines for Dictionary PRs
1. Keep `app_dictionary.json` valid UTF-8 JSON (no trailing commas).
2. Package names in `package_rules` must match the exact Android `applicationId`.
3. Keywords in `category_keywords` should be lowercase.
4. Submit a Pull Request using the **Dictionary / Localization Addition** template.

---

## 2. Code Contributions (Kotlin & Jetpack Compose)

1. **Architecture**: Keep the launcher ultra-lightweight, fast (110ms transitions), and battery-friendly. Avoid heavy background services or unnecessary third-party libraries.
2. **Formatting**: Follow standard Kotlin conventions (`.editorconfig` is included in the repository root).
3. **Verification**: Run `./gradlew assembleDebug` and `./gradlew lintDebug` locally before submitting a PR.
