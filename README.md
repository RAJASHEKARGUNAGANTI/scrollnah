# ScrollNah

A playful Android app that helps you scroll less, with a mascot called **Blobby** who reacts to how much you scroll.

- **Daily limits** for any app, plus separate limits for **Instagram Reels** and **YouTube Shorts**.
- **Websites count too**: youtube.com, instagram.com and others in your browser use the same limits.
- **Three strictness levels**: Chill (a gentle nudge), Firm (a pause screen with a breath, then +5 minutes if you really want), Beast (locked until midnight).
- **Focus mode** (lock your apps for 30 min to 2 hours) and a **bedtime** schedule.
- **Streaks**, a weekly report, a home-screen widget, and light and dark themes.
- **Private by design**: no account, and everything stays on your phone.

## Feedback

Ideas, bugs or questions? Start a thread in [Discussions](https://github.com/RAJASHEKARGUNAGANTI/scrollnah/discussions). The app's **Settings > Send feedback** opens the same page.

## Build

Requirements: Android Studio (or JDK 17 + the Android SDK). Min Android 8.0 (API 26).

```bash
./gradlew assembleDebug        # debug APK  -> app/build/outputs/apk/debug/
./gradlew bundleRelease        # Play Store bundle (needs keystore.properties, see below)
```

Release signing reads a private `keystore.properties` file in the project root (not in this repo):

```
storeFile=keystore/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## Tech

Kotlin + Jetpack Compose (Material 3, heavily restyled), Room, DataStore, a foreground service using `UsageStatsManager`, and an optional Accessibility service that only detects Reels/Shorts and browser address bars.

Project notes for contributors: [`CLAUDE.md`](CLAUDE.md) (design and rules) and [`CLAUDE_UPDATES.md`](CLAUDE_UPDATES.md) (progress log).
