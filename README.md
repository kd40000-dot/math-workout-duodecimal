# Duodecimal Workout

An independent, offline, base-twelve Android mental-arithmetic trainer. This is **new original source code**, not a reverse-engineered or repackaged version of the closed-source Math Tricks Workout APK.

All exercise operands, answers, and scores use base twelve: `0`–`9`, `↊` for ten, `↋` for eleven. `10₁₂` = twelve in decimal and `100₁₂` = one gross.

Features: 20 exercise types, five difficulty levels, custom dozenal keypad, local progress and Dracula theme.

### Install

Open **Actions → Build Duodecimal Workout APK** and download the APK artifact from the latest successful run. The app installs alongside the original, with package `com.duodecimal.workout`.

### Build

JDK 17, Android SDK 35, Gradle 8.10.2: `gradle :app:testDebugUnitTest :app:assembleDebug`.

### Scope

The original uploaded app's mini-games, lesson walkthroughs and saved progress have not been ported. No original app code or bundled credentials are redistributed. Per-gross percentages mean dividing by `100₁₂` (144 decimal), not the standard decimal percent.
