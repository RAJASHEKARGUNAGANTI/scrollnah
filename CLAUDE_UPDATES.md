# ScrollNah (formerly Unscroll) — Progress Log

Tracks what's done vs. what's left against the build order in `CLAUDE.md`.
Read this first when picking the project back up in a new session / Android Studio.

## Status (2026-10-30): v2.2.2 released on GitHub (floating-window fix, Settings tab, Pause preview saves nothing) (enhancements batch + compact app, see below). APK 2.60 MB -> 1.43 MB. Rule for first build: NOTHING that needs a server (AI insights + friend challenges deferred).

---

## Step 1 — Project setup, theme, navigation with fake data ✅ DONE

### What exists
- **Gradle project**: native Kotlin + Jetpack Compose, `minSdk 26`, `targetSdk 35`, `compileSdk 35`.
  Wrapper is Gradle 8.9, AGP 8.7.3, Kotlin 2.1.0. Version catalog at `gradle/libs.versions.toml`.
- **Fonts**: real variable-font files downloaded from Google Fonts into `app/src/main/res/font/`
  (`bricolage_grotesque.ttf`, `dm_sans.ttf`), wired up in `ui/theme/Type.kt` using
  `FontVariation` to hit the exact weights (800 for display, 400/500/700 for body).
- **Theme** (`ui/theme/`): `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` — matches the lilac/ink/lime/orange
  neo-brutalist palette, 2dp ink borders, hard offset shadows, rounded corners from `CLAUDE.md`.
- **Blobby mascot** (`ui/components/Blobby.kt`): drawn with Compose `Canvas`/`Path`, no image assets.
  Two moods: `HAPPY` (orange, home screen) and `WORRIED` (lime, pause screen).
- **Icons** (`ui/components/Icons.kt`): hand-drawn 2dp stroke icons (settings, back, chevron, up-arrow,
  fire, movie, 4 nav icons) ported from the HTML mockup's SVG paths — not a Material icon library,
  so the brutalist line style is exact and there's no icon-name guessing risk.
- **Shared components** (`ui/components/CommonComponents.kt`): `NeoCard`, `NeoShadowSurface`,
  `NeoButtonPrimary`/`NeoButtonOutline`, `NeoChip`, `NeoPill`, `LetterTile`, `NeoProgressBar`.
- **Fake data** (`data/FakeData.kt`): placeholder apps/limits/week stats used by all 4 screens.
- **4 screens, all built and navigable** (`ui/screens/`):
  - `HomeScreen.kt` — Blobby mood card, fun stat card, today's limits list, "Edit" → navigates to Limits.
  - `LimitsScreen.kt` — working steppers (5–180 min, 5-min steps), on/off switches, 3-way strictness
    picker (Chill/Firm/Beast), all local state (not yet persisted).
  - `PauseScreen.kt` — live 3→2→1 breathing countdown, "why opening it" reason chips, decline/allow
    buttons. Currently reachable only via bottom nav tab (not yet triggered by real app detection).
  - `WeeklyReportScreen.kt` — bar chart with dashed 1h limit line, streak + time-won-back stat tiles,
    friend challenge card (placeholder).
- **Navigation** (`ui/navigation/`): `Destinations.kt`, `UnscrollBottomNav.kt` (custom-styled, not
  Material's `NavigationBar`), `UnscrollApp.kt` (NavHost + Scaffold, hides bottom nav on Pause screen).
- **App icon**: adaptive icon (`res/mipmap-anydpi-v26`, `res/drawable/ic_launcher_*.xml`) — simple
  Blobby-silhouette vector, lilac background.
- **Verified working**: ran `./gradlew assembleDebug` successfully — produced
  `app/build/outputs/apk/debug/app-debug.apk`. Android SDK is installed at
  `~/Library/Android/sdk` (`local.properties` already points to it).

### Bugs fixed along the way (good to know, don't re-break these)
- `NeoShadowSurface` originally had **both** inner boxes as `matchParentSize()`, which would've
  collapsed every card/button to zero height (nothing left to size the Box from). Fixed: only the
  shadow copy uses `matchParentSize()`; the foreground box sizes normally and drives the layout.
- `FlowRow` (pause screen reason chips) and `FontVariation` (variable font weights) are experimental
  Compose APIs — needed `@OptIn(ExperimentalLayoutApi::class)` / `@OptIn(ExperimentalTextApi::class)`
  file-level annotations in `PauseScreen.kt` and `Type.kt`.

---

## Step 2 — Usage access permission + real per-app usage data on Home ✅ DONE

### What exists
- `AndroidManifest.xml`: `PACKAGE_USAGE_STATS` declared (with `tools:ignore="ProtectedPermissions"`).
- `data/UsageRepository.kt`:
  - `hasUsageAccess()` checks the AppOps `GET_USAGE_STATS` mode.
  - `todayUsageMinutes(packages)` walks `UsageEvents` since midnight, pairing `ACTIVITY_RESUMED` /
    `ACTIVITY_PAUSED` per package (an app still open counts up to now). Deliberately NOT using the
    daily `queryUsageStats` buckets, which can spill over from yesterday.
  - `openUsageAccessSettings()` deep-links to ScrollNah's own toggle (`package:` URI), with a
    fallback to the general Usage access list.
- `HomeScreen.kt`: if access is missing, a lilac "Let Blobby peek at your screen time" card with a
  "Give usage access" button replaces the Blobby card. Permission + usage are re-checked on every
  `ON_RESUME` (`LifecycleResumeEffect`, from the new `lifecycle-runtime-compose` dependency), and
  usage is read on `Dispatchers.IO`.
- `AppLimit` now has a `packageName` (Instagram, YouTube, Snapchat, X). Limit values are still
  hard-coded in `FakeData.kt` until step 3; only "used minutes" and the headline total are real.
- Verified on emulator (Pixel 7 AVD `Unscroll_Pixel`, API 35): granted access, used YouTube ~1 min,
  Home showed `1 / 45 min`.

### Notes
- Blobby's mood pill, the encouraging line and the "86 metres" fun stat are still static (step 5).
- Settings opens inside ScrollNah's task, so the user returns with Back, which is standard behaviour.

## Step 3 — Limits saved to Room ✅ DONE (plus "real data everywhere" pass)

### What exists
- **No fake data anywhere.** `FakeData.kt` is deleted. Shared types live in `data/Models.kt`
  (`Strictness`, `pauseReasons`, limit constants, `Feature` enum, installed-app helpers).
- **Storage:** Room DB `data/db/` with `TrackedApp`, `FeatureLimit`, `FeatureUsage` (per day),
  `PauseLog` (reason + whether they opened anyway). DataStore `SettingsRepository` holds the user
  name and strictness. KSP added for the Room compiler.
- **`UnscrollApplication` / `AppContainer`** is a simple hand-made holder for the DB and repositories.
  Composables call `appContainer()`, and the service uses `application as UnscrollApplication`.
- **`UsageRepository`** turns raw events into sessions and gives: today's minutes (for chosen apps
  or for all apps), today's open counts (gaps under 5 s merged), and daily totals for N days with a
  `hasData` flag for days older than Android's kept history.
- **Limits:** real list from Room, plus an "Add an app" picker (`AddAppScreen`) that lists launcher
  apps sorted by today's usage, with search. Each row has a remove (X) button. Stepper/switch/strictness
  changes are drafts saved by "Lock it in"; drafts are also saved before opening the picker.
  `<queries>` was added to the manifest so other apps are visible on Android 11+.
- **Home:** "Hey [name]" (set from the gear button dialog, "Hey there" if blank). Empty states for
  no usage access and no apps. Blobby's mood comes from real usage vs limits (vibing / watchful /
  worried, and the card turns orange when over). The fun stat uses real minutes (songs or sitcom
  episodes), because scroll distance isn't measurable. Reels/Shorts rows appear when enabled.
- **Stats:** last 7 days from real events, dashed line = sum of enabled limits, "less/more vs last
  week" badge and "time won back" only when 14 days of history exist. The streak counts full days
  under the limit since install. Friend challenge is a "coming soon" card (no fake friend).
- **Pause tab:** a preview using the most-used limited app and its real open count today. The reason
  chip and choice are logged to `PauseLog`. "Open for 5 minutes" just launches the app; the timer
  comes in step 4.

## Reels & Shorts limits (opt-in Accessibility) ✅ BUILT, needs on-device verification
- `service/FeatureWatchService.kt` + `res/xml/feature_watch_service.xml`. It checks for view IDs from
  `Feature.viewIds` every 2 s and on screen events, adds time to `FeatureUsage`, and presses Back plus
  shows a toast once the limit is hit.
- View IDs (`reel_recycler` etc. for YouTube, `clips_viewer_view_pager` etc. for Instagram) are
  NOT verified yet and can change with app updates. To check them: open Shorts/Reels, run
  `adb shell uiautomator dump`, and look for the player's resource-id.
- Strictness doesn't affect Reels/Shorts yet (always "back out at limit").

## Step 4 — Foreground service + real Pause screen ✅ BUILT (testing on iQOO, Android 16)

### Rules (decided with the developer)
| Strictness | At the limit | +5 min? |
|---|---|---|
| Chill | Notification nudge with "5 more min" / "I'm done" | yes |
| Firm | `PauseActivity` over the app; "Open for 5 minutes" unlocks after the 3-2-1 breath | yes |
| Beast | Locked `PauseActivity`, only "Take me home", until midnight | **no** |
Reels/Shorts follow the same rules (Firm/Beast press Back first, then show the Pause screen).
Actions only fire when OVER the limit, not on every open.

### What exists
- `service/BlockerService.kt`: foreground service (`specialUse` type), polls `UsageRepository.latestForegroundApp`
  every 1 s, re-reads today's usage every 10 s (or right away when an app opens), and calls `LimitActions`.
  Cooldowns: 3 s for the Pause screen, 60 s / 5 min for Chill nudges. Started from `MainActivity.onResume`,
  after "Lock it in", and by `BootReceiver` (boot + app update).
- `service/LimitActions.kt`: strictness switch, notification channels, nudge notification, `PauseTarget`.
- `service/Allowances.kt`: in-memory "+5 min" passes (key = package or feature id).
- `service/Receivers.kt`: `NudgeReceiver` ("5 more min" button) and `BootReceiver`.
- `PauseActivity.kt`: own task (`taskAffinity`), hidden from Recents; Back = "go home"; logs to `PauseLog`.
- `PauseScreen` now has `locked` (Beast) and `limitMinutes`, and the +5 button waits for the breath.
- Home: "Make blocking work" checklist (`SetupChecks`): notifications, display over other apps, battery
  optimisation, plus a hint and link for vivo/Xiaomi-style background pop-up switches.
- Limits: a line under the strictness cards explains the +5 rule.

### Bugs found on the real phone (don't re-break)
- **Late usage events:** Android writes "app opened" events with a delay (seconds on vivo/iQOO). Reading
  only events newer than the last check skipped them, so blocking only worked sometimes. Fix:
  `BlockerService` re-reads the last 2 minutes every tick (30 min on the first tick).
- **Double Pause screen:** the blocker retried while the Pause screen was already up. Fix:
  `PauseActivity.isShowing`, checked in `LimitActions.onLimitReached`.
- **Invisible logs:** vivo hides app logcat lines (`log.tag=I` and more). `BlockerService.debug()` also
  writes to `files/blocker.log`, which you can read with
  `adb exec-out run-as com.unscroll.app cat files/blocker.log`.
- **Beast honoured old +5 passes:** a pass taken in Firm still let the app through after switching to
  Beast. Fix: Beast ignores `Allowances` (BlockerService + FeatureWatchService), and "Lock it in"
  with Beast calls `Allowances.clear()`.
- **Toasts blocked:** vivo blocks toasts when app notifications are off, so don't rely on toasts.

### Tested on iQOO (Android 16)
- Beast: locked screen, no +5, "Take me home" and Back both go home, 3/3 opens blocked.
- Firm: breath gate, "Open for 5 minutes" lets the app through and logs the reason.
- Firm +5, then switch to Beast: blocked straight away (after the fix).
- Firm re-block when the +5 runs out: Blobby returned in the same second.
- Chill: YouTube stays open, nudge notification with "5 more min" / "I'm done" posted.
- YouTube Shorts in the app (Firm): Back + Pause screen.
- Browser (Chrome): m.youtube.com/shorts -> Shorts Pause screen; YouTube search page -> whole-app
  Pause screen (app time + website time); instagram.com/reels counted as Reels + Instagram web time.
- Not tested: Instagram Reels inside the app (Android's own Digital Wellbeing timer had paused
  Instagram that day), the "5 more min" notification button, vivo Browser (address-bar ID unknown).

### Reliability (core gaps pass)
- `Allowances` now saves to SharedPreferences (`shared_prefs/allowances.xml`), initialised in
  `UnscrollApplication.onCreate`. Tested: took +5 in Firm, killed the process (`run-as ... kill -9`),
  the app restarted in ~2 s, BlockerService came back (START_STICKY), and YouTube stayed allowed.
- Watchdog: `FeatureWatchService` checks `BlockerService.isRunning` every 60 s and restarts it.
  Not directly tested (shell can't stop a non-exported service).
- Still to test by hand: the "5 more min" nudge button (vivo's shade is hard to automate), reboot
  survival, and Instagram Reels in the app.

### Known limits
- Background activity starts need "Display over other apps". vivo/iQOO may also need its own
  "background pop-ups" switch in App info.
- Play Store: the `specialUse` FGS and the Accessibility service both need declarations.

## Browser blocking ✅ BUILT + tested in Chrome
- `data/Websites.kt`: address-bar view IDs per browser, website -> app map (youtube.com, instagram.com,
  facebook.com, x.com, reddit.com, tiktok.com, snapchat.com), `match()` turns address text into
  app + Reels/Shorts feature. Website time is stored in `feature_usage` as `web:<package>`, so it
  needs no schema change.
- `FeatureWatchService` reads ONLY the address bar of known browsers. Shorts/Reels share one budget
  across app + web. Whole-app limits use app minutes (UsageStats) + website minutes, in the tracker,
  `BlockerService` and Home. In browsers it covers the page (no Back press).
- Needs the Accessibility tracker switched on. Disclosure text, the Accessibility description and
  CLAUDE.md were updated to mention the address bar.
- Gaps: vivo Browser and other browsers not in the list; Chrome may hand app links to the YouTube app
  (that case is still blocked by the app rules); web time isn't in the weekly report yet.

## Step 5 — Polish ✅ DONE (Compose animation instead of Lottie)
- **Animated Blobby** (`ui/components/Blobby.kt`): gentle bob + squish (faster when worried), random
  blinks, side-to-side glance when watchful, crossfade between moods. Turns off when the phone's
  "Remove animations" setting is on. New third mood **WATCHFUL** (raised brows, flat mouth) used on Home
  between 50% and 100% of the budget. Lottie can still replace this if a designer makes files.
- **Blobby lines** (`ui/components/BlobbyLines.kt`): 3 variants per mood and for the Pause/locked
  subtitles, picked once per day (stable, not jumpy).
- **Stats:** the weekly chart now adds website time (`web:<package>` rows) to app time. New
  "Why you reached for it" card shows Pause-screen count, how often the user chose "Nah, I'm good",
  and top reasons (from `PauseLogDao.reasonCounts`). The limit label moved left with a white backing.
- Testing note: the endless Blobby animation stops `uiautomator dump` from going idle, so tap by
  coordinates when automating Home/Pause.

## Light/dark mode + name editing ✅ DONE (2026-10-11, tested on the Nothing phone)
- The top-right Home button was a sun icon (`SettingsIcon`, renamed `SunIcon`) that opened the name
  dialog. It is now a light/dark switch: it shows `MoonIcon` in light mode and `SunIcon` in dark mode.
  The choice is saved in DataStore (`SettingsRepository.darkMode`, null = follow the phone).
- Theming: `ui/theme/Color.kt` has `NeoColors` (background/surface/content/outline/shadow) with
  `LightNeoColors` / `DarkNeoColors`, read via `Neo.colors`. `contentColorOn(bg)` keeps Ink on
  Lime/Orange/Lilac/CheekPink/White (design rule). `AppTheme` (used by MainActivity + PauseActivity)
  picks the mode and sets the status/nav bar icon colours. Icons follow `LocalContentColor`.
- The Pause screens always use the light design (full orange page) via `LocalNeoColors provides
  LightNeoColors`.
- Name: tap "Hey [name]" to reveal a lime pencil button beside it; the pencil opens the name dialog.
- When adding UI: use `Neo.colors.*` for page/card/text/border/shadow, never hard-coded `White`/`Ink`,
  except Ink for text on bright brand colours.

## First-build features (2026-10-11) ✅ BUILT, tested on the emulator (not yet on the phones)
- **Limits "Lock it in" UX:** the button is disabled ("Nothing to lock in yet") unless something differs
  from what's saved. Leaving with unsaved changes (back arrow, system back, or another tab) shows "Lock it
  in first?" (Lock it in / Discard / Keep editing). The tab-switch guard lives in `UnscrollApp`
  (`limitsUnsaved`, `pendingLeave`) and the dialog in `LimitsScreen`.
- **Streaks:** Room v2 adds `daily_summary` (MIGRATION_1_2). `StreakRepository` saves finished days
  (app + web minutes vs the summed enabled limits), computes current/best streak, and has
  `dailyTotalsWithWeb` (Stats uses it too). Home `StreakCard`, milestone `CelebrationDialog`
  (3/7/14/30/60/100, `celebrated_streak` in DataStore, reset when a streak breaks). Evening nudge after
  9pm once a day from `BlockerService.streakWork()` (every 30 min).
- **Focus / bedtime:** `service/FocusRules.kt` (SharedPreferences): focus session (`startFocus`, Home
  card with 30m/1h/2h, "End early" except in Beast) and a bedtime schedule (Limits `BedtimeCard`, saved
  via Lock it in). `PauseReason` LIMIT/BEDTIME/FOCUS flows through `PauseTarget`, `LimitActions`,
  `PauseActivity` and `PauseScreen`. Focus = always locked, no +5. Bedtime follows strictness. Applies to
  apps, websites and Reels/Shorts.
- **Home-screen widget:** `widget/BlobbyWidget.kt` (RemoteViews), `layout/widget_blobby.xml`, three
  Blobby face vectors `drawable/widget_blobby_*`, night variants. Refreshed by BlockerService every 5 min
  and on app resume. Not visually tested yet: add it from the launcher's widget picker.
- **Settings screen** (`SETTINGS_ROUTE`, gear button on Home): name, Light/Dark/Follow phone,
  permission statuses, end focus, clear +5 passes, clear history (confirm), About.
- **Onboarding** (`OnboardingScreen`, gate in `MainActivity.AppRoot`): welcome, name, usage access,
  pick apps, strictness, permissions. Existing installs with tracked apps are marked onboarded and skip it.
- **Fixes:** Blobby's modifier now goes on the outer Crossfade (centering works), and `PauseActivity.isShowing`
  also covers the first 5 s after launch (no double Pause screen on slow phones).

## Release build ✅ (v1.0.0, versionCode 1)
- Signing: `keystore/unscroll-release.jks` + `keystore.properties` (both gitignored). **Back these up
  outside the project**: losing them means no Play Store updates for this app.
- Release = R8 minify + resource shrinking. The APK is ~1.8 MB (debug is ~11 MB). Smoke-tested on the
  emulator (onboarding + every tab).
- `BuildConfig.DEBUG` guards `DebugLog` so release builds keep no `blocker.log`.
- Outputs: `./gradlew assembleRelease` (APK) and `./gradlew bundleRelease` (AAB, for Play).
  Copies in `~/Downloads/unscroll-1.0.0-release.apk|.aab`.
- A release APK can't be installed over a debug install (different signature): uninstall first, which
  wipes the app's data.

## Name, GitHub and feedback ✅ (2026-10-11)
- **App name: ScrollNah.** "Unscroll" had 5+ Android and 3+ iOS apps with the same name, and the
  other options checked were taken too. The mascot stays **Blobby**. The internal package / class names
  stay `com.unscroll.app`, `UnscrollApplication` etc. (users never see them; changing the package after
  publishing is impossible). scrollnah.com and scrollnah.in were free on 2026-10-11 (not registered yet).
- **Repo:** https://github.com/RAJASHEKARGUNAGANTI/scrollnah (public, Discussions on). Commits use the
  repo-local author `RAJASHEKARGUNAGANTI <85203365+RAJASHEKARGUNAGANTI@users.noreply.github.com>`.
  License: MIT (added 2026-10-11).
- **Feedback:** Settings > Feedback has a form (Idea / Bug / Question + message). "Continue on GitHub"
  opens `/discussions/new?category=..&title=..&body=..` pre-filled (`feedbackUrl()` in `data/Models.kt`):
  Idea goes to `ideas`, Question to `q-a`, Bug to `general` with a "Bug: " title. App version + Android
  version + phone model are appended. The user posts it on GitHub themselves (needs a GitHub account);
  nothing is sent from the app. About also has "Source code" (`REPO_URL`) and "All discussions" links.
- Never committed: `keystore/`, `keystore.properties`, `local.properties`, build outputs (gitignored).
- Builds in ~/Downloads: `scrollnah-debug.apk`, `scrollnah-1.0.0-release.apk|.aab`.

## Public download: GitHub Release v1.0.0 ✅ (2026-10-11)
- https://github.com/RAJASHEKARGUNAGANTI/scrollnah/releases/tag/v1.0.0 with asset `ScrollNah-1.0.0.apk`
  (signed release build, SHA-256 fd6cdf78...206e). README has a "Download the latest APK" link
  (`/releases/latest`) and install steps.
- **Next release:** bump `versionCode` + `versionName` in `app/build.gradle.kts`, then run
  `./gradlew clean assembleRelease`, then
  `gh release create vX.Y.Z app/build/outputs/apk/release/app-release.apk#ScrollNah-X.Y.Z.apk --notes-file ...`.
  Always sign with the same keystore so updates install over the old version.
- **Demo mode (debug builds only):** `adb shell run-as com.unscroll.app touch files/demo_mode`, then restart
  the app. It fills a sample week (Alex; Instagram/YouTube/Snapchat; 5-day streak; Pause reasons) for
  screenshots. `DemoMode.kt`; stripped from release builds (verified).

## Spec update 2026-10-26: mascot themes, 5 moods, break reminders, Glance widget ✅ BUILT
The developer pasted a new spec + `Wellbeing App Screens.pdf`. The spec was merged into `CLAUDE.md`
(kept: the name **ScrollNah**, the opt-in Accessibility exception, the mascot-voice and calm-copy notes).
**The PDF couldn't be read** (macOS blocks Claude from ~/Downloads and it never appeared in `design/`),
so the art follows the spec's text. Next session: put the PDF in `design/` and compare it with the
debug gallery (below).

### Moods + themes (`data/Mood.kt`, `ui/mascot/`)
- `MoodLevel` THRIVING/HAPPY/OKAY/WORRIED/OVER from today's total share (0–25/25–50/50–80/80–100/over).
  Decision: any single app at its limit = at least WORRIED.
- `MascotTheme` BLOBBY (default) / BLOOMY = name + mood names. Saved in DataStore (`mascot_theme`).
  `AppTheme` provides `LocalMascot` (theme + Bloomy's flowers) to every screen; `AppContainer.mascot` /
  `.flowers` are in-memory copies for the service, notifications and bubble.
- Art is Canvas code on a 140x140 grid: `BlobbyArt.kt`, `BloomyArt.kt`, shared faces/drops/sparkles in
  `MascotParts.kt`. `drawMascot()` is the only place that maps theme -> art (a 3rd theme = new art file +
  one `when` branch). `Mascot()` composable animates (bob/squish for Blobby, stem sway for Bloomy, blinks,
  sparkle twinkle); `animated = false` draws a still picture with no animation clock.
  `onOrange = true` swaps Blobby's body to lime and Bloomy's pot to lilac on orange pages.
- `MascotBitmap.render()` draws the same art into a Bitmap (widget, notification large icon, bubble badge).
- `MascotLines` replaces `BlobbyLines`: Home card text for all 5 levels in both voices, Pause subtitles,
  widget title, break reminder texts. Hard-coded "Blobby" strings now use the chosen mascot's name.
- Home card: lime for levels 1–4, orange for OVER (mascot drawn `onOrange`). Pause screen: level 4.
- **Debug gallery:** `adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity` shows every
  mood x growth stage + the bitmaps (`src/debug/`, not in release builds).

### Bloomy growth + garden (`data/Growth.kt`)
- `Growth.advance()` (pure, unit-tested) replays finished `daily_summary` days after `PotProgress.lastDate`:
  7 under-limit days in a row = +1 flower; an over-limit day = newest flower falls off + count restarts;
  a missing day = count restarts only; 4 flowers = pot saved to `garden_pots` (Room) and a new seed starts
  the next day. Progress lives in DataStore (`pot_flowers`, `pot_days`, `pot_last_date`).
- `GrowthRepository.update()` (Mutex, so the service and screens can't save a pot twice) runs at the end
  of `StreakRepository.recordFinishedDays()`. Growth is tracked for every user, whatever the theme.
- Settings > Mascot: two picker cards (lime when picked), "Your pot" text and a Garden row -> `GardenScreen`
  (2 pots per row, "Finished 16 Sep 2026"). "Clear history" keeps the garden.

### Break reminders
- Settings in DataStore (`BreakSettings`: enabled (default on), minutes (20), style (BOTH), repeat (on)).
  Per-app times = new nullable column `tracked_apps.breakMinutes`.
- Room v3 (`MIGRATION_2_3`): `breakMinutes` column, `break_log` (shown/break/snooze rows), `garden_pots`.
  Migration verified on the emulator (an old v2 install upgraded without a crash).
- `service/SessionTracker.kt` (pure, unit-tested): per-app sessions; < 2 min away keeps the session, 2+ min
  starts a new one; only time in front counts; first reminder at the app's time, then every 5 min if
  "Keep reminding me"; "5 more min" = next one in 5 min even with repeat off.
- `BlockerService.checkBreak()` feeds it every tick; `BreakReminders.show()` posts the notification
  (channel "breaks", mascot badge as large icon, "Take a break" / "5 more min") and/or the bubble.
  1st reminder = WORRIED art, later ones = OVER. If the chosen way isn't allowed, it falls back to the other.
- "Take a break" = `BreakActionActivity` (invisible; Android blocks starting activities from notification
  receivers): logs "break" and goes home. "5 more min" = `BreakSnoozeReceiver`.
- `service/BubbleOverlay.kt`: plain-View overlay window (`layout/overlay_bubble.xml`, `drawable/bubble_speech.xml`),
  bottom-right, 6 s. Tap the speech bubble = dismiss, tap the mascot = open ScrollNah.
- UI: Limits has a "Break reminders" row (asks "Lock it in first?" if limits are unsaved) ->
  `BreakRemindersScreen` (saves instantly): main switch, 10/15/20/30/Custom, per-app overrides + "Add an app",
  Notification/Bubble/Both cards, calm permission hints, "Keep reminding me".
- Notification titles are signed ("Bloomy: 20 min of Instagram straight") with first-person text
  ("I'm getting thirsty. Take a break?"), per the mascot-voice rule (the spec's example was third person).

### Glance widget (`widget/`)
- `MediumMascotWidget` (4x2) and `SmallMascotWidget` (2x2). The medium receiver keeps the old class name
  `widget.BlobbyWidget`, so widgets placed before still work; small = `SmallMascotWidgetReceiver`.
- `WidgetData.load()` = theme, flowers, mood, the limited app closest to its limit (highest used/limit share),
  streak. `MascotWidgets.refreshIfChanged()` runs every minute from BlockerService and only redraws when
  that data changed (covers mood/number changes and midnight). Theme change and app resume refresh at once.
- Card drawables now have a hard 4dp shadow (`widget_background.xml` + night version); bar frame and pill
  have night versions. Static preview layouts for the widget picker.
- Release APK is now ~2.6 MB (Glance adds ~0.8 MB). Gallery verified absent from the release manifest.
- **2026-10-27: Glance removed** (see "Enhancements + compact app"): same widgets, now plain RemoteViews.

### Tests
- First unit tests: `./gradlew testDebugUnitTest` (JUnit 4). `MoodLevelTest`, `GrowthTest`, `SessionTrackerTest`
  (19 tests, all passing).

### Verified on the emulator (Pixel AVD, demo mode)
- Gallery (all moods/stages), Home with Blobby and Bloomy, Settings picker + pot text, Garden, Limits row,
  Break reminders screen + per-app override.
- **Live break reminder:** YouTube with a 5-min override. Reminder #1 fired at exactly 5:00 ("Bloomy: 5 min
  of YouTube straight", thirsty badge, both buttons). "5 more min" -> #2 came 5 min later with the sadder
  text and the bubble ("11 min straight! I'm wilting.") bottom-right. "Take a break" -> home screen.
  `break_log` had shown/snooze/shown/break rows. DB upgraded 2 -> 3 without a crash.
- **Widgets on the Pixel launcher:** both appear in the picker with previews, show live data (mood pill,
  title, Instagram 22 / 30 min bar, "5-day streak, 2 flowers"; small: "8 min left on Instagram today"),
  tap opens ScrollNah, and switching Blobby <-> Bloomy updates them at once.
  Bug found + fixed: Glance keeps a widget's composition alive, so `updateAll()` only recomposed old data.
  The content now collects `MascotWidgets.latest` (a StateFlow) instead of capturing data once.
- **Device pass on the emulator (2026-10-26, iQOO couldn't be connected), real usage, no demo mode:**
  - Upgrade path: built v1.0.0 from the last commit, installed fresh, onboarded (name, Chrome + YouTube,
    Firm), then installed the new build over it. Name, apps and limits kept, DB 2 -> 3, no crash, Home
    showed "Blobby · melting" on the orange card.
  - Blocking with the new art: Firm Pause screen over Chrome (worried lime Blobby, "Bored" logged,
    "Nah, I'm good" -> home), Beast locked screen (only "Take me home", Back -> home), Chill nudge
    "Blobby: Chrome time's up" with Chrome left open. Bloomy on the Firm Pause screen (lilac pot).
  - Growth wiring: pushed 7 under-limit `daily_summary` days -> Settings showed "1 flower"; added one
    over-limit day -> back to "A little sprout". (Test rows are still in the emulator's DB.)
  - Widget in system dark mode: dark card, light text/pill, lilac shadow, live data.
  - Note: Chill waits 60 s after the previous action before nudging a just-opened app (older behaviour,
    noticed when switching strictness during the test).
- Not checked yet: the iQOO/Nothing phones (vivo background rules, late usage events), BHIM itself,
  a full on-screen keyboard (the emulator shows a floating toolbar).
- The emulator was left in demo mode with both widgets on its home screen (turn demo off:
  `adb shell run-as com.unscroll.app rm files/demo_mode`, then clear app data).

## Version 2.2.2 (2026-10-30)
- `versionCode` 10, `versionName` "2.2.2": the floating-window fix below (+ "Blobby: YouTube is paused" message) and
  the Settings tab / Pause preview change.
- README screenshots retaken after the release (new bottom bar, Settings tab, Stats per-app list + verdict, Bloomy's
  bud + "Next flower"): home-blobby, stats, limits, break-reminders, home-bloomy, mascot-picker, garden, bloom.
  Same method as before (demo mode, System UI demo status bar 9:41, `sips -Z 1200`). Found while doing it: the
  Settings mascot card didn't show Bloomy's bud; fixed (`MascotChoice(budDays)`), not yet in a release.
  **Play Store screenshots (`store/screenshots/`) still show the old Pause tab.**

## Blocked apps can't keep playing in a floating window (2026-10-30, developer report)
Released in 2.2.2. After pausing, `LimitActions.showFloatingPaused()` posts "<Mascot>: <App> is paused" (why: limit /
focus / bedtime, "Swipe it away when you're ready", disappears after 2 min), because the window itself can't be closed
by ScrollNah (only by the user or the app that owns it). Checked on the emulator (Focus + Chrome PiP). Report: YouTube got blocked but its small picture-in-picture window kept playing.
- Why: a floating (PiP) window is paused, not resumed, so `latestForegroundApp` never sees it.
- `data/FloatingWindows.kt`: an app whose activity is paused > 2 s with no "stopped" after it (and nothing of it in
  front) is on screen in a floating window or split screen. Needs ACTIVITY_STOPPED events (Android 10+).
  `UsageRepository.floatingApps()` reads 3 h of events, only when sound is playing and an app is blocked.
  6 unit tests (`FloatingWindowsTest`, 38 total).
- `service/MediaPauser.kt`: takes AUDIOFOCUS_GAIN (USAGE_MEDIA), lets go after 1.5 s; players pause on a full loss and
  don't resume by themselves. No permission. Used by `BlockerService.checkFloatingPlayers` (every 2 s; after a pause
  it waits until the sound stops before pausing again, because players keep a silent stream open ~10 s and other
  music shouldn't be paused over and over) and by `LimitActions.onLimitReached` for YouTube / YouTube Music
  (Premium background play). `isBlockedNow()` mirrors the blocking rules (focus, bedtime, over limit; Chill and
  "+5 min" passes don't block).
- Tested on the emulator with Chrome (the emulator's YouTube app needs an update + sign-in): Chrome limited, a
  video in Chrome's PiP window, Focus started -> PAUSED within ~1 s; pressed play in the PiP window -> paused again
  within ~1 s; Focus ended -> it keeps playing. Expanding the PiP window brings the app to front -> Pause screen.
  Not tested with the real YouTube app or on a phone.
- Known: PiP time isn't counted toward the daily limit (UsageStats only counts time in front); Android 8-9 have no
  floating-window detection (only the block-time pause for YouTube). Edge: if a blocked app's paused floating window
  stays on screen and other music starts, that music is paused once.

## Settings tab instead of the Pause tab (2026-10-30, developer request)
Released in 2.2.2. README/store screenshots still show the old Pause tab.
- Bottom nav: Home, Limits, Stats, **Settings** (gear icon). The gear button at the top of Home is gone (the
  light/dark button stays). `Destination.SETTINGS` replaced `Destination.PAUSE`; `PauseNavIcon` removed.
  Settings keeps its back arrow like Limits and Stats. Garden highlights the Settings tab.
- The Pause preview moved to Limits: "See the Pause screen" under "How strict should I be?" (Firm/Beast only), route
  `pause_preview`, full screen. Asks first if Limits has unsaved changes.
- **Bug fixed:** the old Pause tab's buttons saved real Pause answers (and "Open for 5 minutes" opened the app), so
  trying the preview changed "Why you reached for it" and "Did the nudges work?". Now the preview saves nothing and
  shows a toast. Tested on the emulator with demo data: tapped Bored + "Nah, I'm good", then Work thing + "Open for 5
  minutes"; Stats stayed at 10 / 7 and Bored 4, Pure habit 3, Replying 2, Work thing 1; the app wasn't opened.

## Version 2.2.1 (2026-10-30)
- `versionCode` 9, `versionName` "2.2.1": Bloomy's growing bud below. Release APK not run on a device or emulator
  (drawing-only change since 2.2.0, which was smoke-tested); version and signature checked.

## Bloomy's growing bud (2026-10-30, developer: the plant looked the same all week)
Released in 2.2.1.
- `drawBloomy(..., budDays)`: with 1-6 good days toward the next flower, the plant is drawn with room for one more
  stem (stems for count + 1); that stem (`budStem`: the middle/newest one) starts at 55% height and reaches full height
  by day 7, with a bud (two sepals + a petal-coloured tip, 4.5 -> 9 units) that hangs with the stem in sad moods.
  Day 0 = no bud (sprout stays a sprout). Full pot = no bud.
- `MascotLook.budDays` (from `pot.daysTowardNext` in `AppTheme`), `drawMascot`/`MascotBitmap.render/frame(budDays)`,
  `WidgetState.budDays`, `AppContainer.budDays` (break reminder notification + bubble).
- Home: Bloomy card gets "Next flower ●●●●●○○ 5 of 7 days" (FlowRow, wraps under the label on small phones;
  TalkBack: "Next flower: 5 of 7 days under your limits"). Hidden for Blobby and for a full pot.
- Debug gallery has a row per stage: days 0, 2, 4, 6 happy, then day 4 thirsty and wilting.
- Checked on the emulator: gallery, Home (normal and 360x640dp at font 1.3), all three widgets.

## Version 2.2.0 (2026-10-27)
- `versionCode` 8, `versionName` "2.2.0": the Stats screen upgrade below and the "Turn off tracker" button.
  Release APK smoke-tested on the emulator (Stats, review cards, Share). Not yet run on a real phone.

## Stats screen upgrade (2026-10-27, developer: "go ahead with all")
Released in 2.2.0. All nine ideas from the Stats list, built on the phone only (no server, no new library):
- **Own history (Room v5, `MIGRATION_4_5`):** `day_app_stats` (date, app, minutes incl. website time, opens,
  first use after 4 AM) and `day_hours` (24 numbers: minutes per hour). `StreakRepository.saveDayDetails()` saves
  finished days Android still remembers (reads 10 days back) every time finished days are saved, so these stats
  keep going after Android forgets (~7-10 days). Clear history deletes them too. Bloomy's growth is unchanged
  (still from `daily_summary`).
- **Logic:** `data/WeekStats.kt` (split sessions into days/hours/opens/first use, per-app totals vs last week,
  peak hour, average first use, 4-week trend, month grid) + `WeekStatsTest` (11 tests, 32 total now).
  `Growth.flowerDates()` replays saved days to mark new-flower days. `MascotLines.weekVerdict()` = up to 3
  first-person lines (vs last week, streak, weekends vs weekdays, late-night peak, best/toughest day, Pause wins).
- **Screen:** `WeeklyReportScreen.kt` (layout, tappable chart: tap a bar = that day's apps) + `StatsCards.kt`
  (Where the time went, verdict, review banner, scroll o'clock, Did the nudges work?, month calendar with
  previous/next month, 4-week trend). Loader: `data/WeekReportLoader.kt`.
- **Week in review:** `WeekReviewScreen.kt` (route `week_review`, full screen, no bottom bar): 5 swipeable cards
  sized to the screen (shorter screens show less, the wordmark and mascot always fit), always light colours.
  "Share this card" records the card with a GraphicsLayer, saves `cache/shared/scrollnah-week.png`, shares it
  through a FileProvider (`${applicationId}.files`, `xml/shared_files.xml`) with clip data so the share sheet
  can show a preview.
- **Bug fixed on the way:** `todayOpenCounts` started from `Long.MIN_VALUE`, so `start - lastEnd` overflowed and the
  first visit of the day wasn't counted (the Pause pill's "3rd time today" was one short).
- Letter tiles scale their letter with the tile size. Demo mode has per-app/per-hour samples for 4 weeks, ~5 weeks
  of days for the calendar, and break reminder answers.
- Tested on the emulator: v4 -> v5 upgrade (no crash), demo data (all cards, Bloomy flower mark on the calendar,
  month navigation, tap-a-day, review cards, share sheet with preview, the shared PNG), dark mode, 360x640dp at
  font scale 1.3 (fixed: trend labels were cut, review cards cut off the wordmark/mascot/third verdict line), and real
  usage with demo mode off (today's Chrome/YouTube from Android). Not checked: saving a real finished day into the new
  tables (the emulator had no earlier days of history); privacy policy updated for the new data and Share.

## "Turn off tracker" button for payment apps (2026-10-27, developer request)
- The "BHIM not opening?" notification (once a day, when a UPI/bank app opens while the tracker is on) now has
  **Turn off tracker** next to **Accessibility settings**. `TrackerOffReceiver` -> `FeatureWatchService.turnOff()`
  -> `disableSelf()` (the running service registers itself in `onServiceConnected`). Then the notification goes
  away and a toast says "Tracker off. You can pay now. Turn it back on in ScrollNah > Limits." Android only lets
  the user switch it back on (Limits shows the "Limit Reels, Shorts & websites" card again).
- Tested on the emulator: tracker switched on with `adb shell settings put secure enabled_accessibility_services
  com.unscroll.app/com.unscroll.app.service.FeatureWatchService` (+ `accessibility_enabled 1`), notification shown
  with the debug trigger `adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity --es paymenthelp BHIM`,
  tapped the button: no enabled/bound services afterwards, notification gone, no crash. Not tested with BHIM itself.

## Version 2.1.1 (2026-10-27)
- `versionCode` 7, `versionName` "2.1.1". Same app as 2.1.0 (only comments, the Gradle project name, README
  screenshots and the design mockup name changed). Released as a new version because moving the v2.1.0 tag
  needs a force push, which Claude's safety check blocks.

## Version 2.1.0 (2026-10-27)
- `versionCode` 6, `versionName` "2.1.0": everything in "Enhancements + compact app" below. GitHub release v2.1.0 (Latest).
- Not yet tested on a real phone (emulator only). Play listing text (`store/listing.md`) not updated for the new features.
- README: new screenshots `setup.png` (Pick your buddy), `bloom.png` (New flower!), `focus-tile.png` (Quick Settings,
  focus on), and a new `limits.png` (app icons + YouTube weekend limit). Same method as "README screenshots" below;
  the emulator's demo status bar wouldn't show the Wi-Fi icon this time (clock + battery only). The Focus tile showed
  no ">" arrow once it was removed and added again (System UI caches the toggle flag).

## Enhancements + compact app (2026-10-27, developer's list + "make it compact")
Committed as d5bf426 and released in 2.1.0. Built both flavors, 21 unit tests pass, tested on the emulator (`Unscroll_Pixel`).
- **Pick your mascot during setup:** new first step "Pick your buddy" (Blobby / Bloomy cards, reuses
  `MascotChoice` from Settings, saves at once), then "Hi, I'm <mascot>". `STEPS` = 7.
- **Faster Chill nudges:** `BlockerService.chillGap()`: reopening a limited app nudges straight away unless
  a nudge went out in the last 10 s (was up to 60 s); while it stays open, every 5 min (unchanged).
  Same for Chill at bedtime. Not timed on a device (needs 5+ min of real use).
- **Quick Settings "Focus" tile:** `service/FocusTileService.kt` (+ `ic_tile_focus.xml`, manifest entry with
  `TOGGLEABLE_TILE`). Tap = focus for 30 min, tap again = end early (not in Beast: toast). No Usage access =
  opens the app. Home's Focus card calls `FocusTileService.refresh()`. Tested on the emulator via
  `adb shell cmd statusbar add-tile com.unscroll.app/.service.FocusTileService`. The emulator still drew a ">"
  arrow on the tile after the toggle flag was added (not re-checked; likely cached by System UI).
- **Bloomy celebrates growth:** `Growth.Result.newFlowers`; `GrowthRepository.update()` stores a `BloomEvent`
  (FLOWER or POT, only when Bloomy is the mascot; a pending POT is never downgraded) in DataStore
  (`bloom_event`). Home shows `BloomDialog` once ("New flower!" / "Full pot!" + "See garden"), after any
  streak celebration. Debug trigger: `adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity --es bloom FLOWER`.
- **Weekday / weekend limits:** `TrackedApp.weekendLimitMinutes` (null = same every day), Room v4
  (`MIGRATION_3_4`), `limitOn(date)` / `totalLimitOn(date)` used by the blocker, website tracker, Pause screen,
  Home, widgets, streaks and the weekly chart (dashed limit line drawn per day). Limits screen: "+ Different on
  weekends" adds a Sat & Sun stepper, X goes back to one limit. Upgrade from v3 tested (no crash, data kept).
- **App icons:** `ui/components/AppIcon.kt` `AppTile` shows the app's real icon (read from the phone, so no
  size cost) in the usual bordered tile; letter tile while loading or if the app isn't installed. Reels/Shorts
  rows use their app's icon. Used on Home, Limits, Add app, setup, Break reminders.
- **Smoother mascot motion (no Lottie, no size cost):** per-mood body language in `AnimatedMascot`
  (thriving hop, worried shiver, melting sag, Bloomy sways more when happy), sine easing, a springy "boing"
  on mood change, occasional double blink, tap-to-poke on Home's mood card. "Remove animations" still = still.
- **Screen-reader (TalkBack) pass:** roles and states for switches (`toggleable`, Role.Switch), chips/option
  cards/mascot cards (selectable), nav tabs (Role.Tab), buttons; labels for icon buttons (Back, Settings,
  light/dark, Remove X, Clear search, Edit name), steppers ("Raise YouTube weekend limit", value read as
  "30 minutes"), bedtime times, Allow buttons; one sentence per limit row and for the weekly chart; headings;
  letter tiles/app icons hidden (name is next to them); pill text read without the "·"; widget mood labels.
  Checked with `uiautomator dump` (content-desc), not by listening with TalkBack on.
- **Small fixes:** touch targets of 44 dp for remove buttons, pills and time chips; Limits list doesn't flash
  "add your first app" while loading; Home "Edit" centred on its heading; tile + switch line up with the app name.

### App size: 2,604,965 -> 1,428,794 bytes (github release APK, -45%)
1. **Fonts trimmed** with `tools/subset_fonts.py` (fonttools): Bricolage = static ExtraBold 800 only,
   DM Sans = variable but only 400-700; Latin + Latin-1 + Latin Ext-A + punctuation, default OpenType
   features, no hinting. 649 KB -> 116 KB (~300 KB smaller inside the APK). Other scripts (a Hindi or Telugu
   name) fall back to the phone's system font. `Type.kt` uses the static Bricolage file directly.
2. **English-only library text** (`androidResources.localeFilters += "en"`) and packaging excludes (Kotlin
   builtins, DebugProbesKt.bin, DataStore's multi-process `.so`).
3. **Glance replaced by plain RemoteViews widgets** (`widget/MascotWidgets.kt`, `layout/widget_medium.xml`,
   `widget_small.xml`, `widget_bar_lime/orange.xml`, neutral `widget_loading_card/empty.xml` as initial layouts).
   Glance shipped ~990 layout files plus WorkManager: about 600 KB. Same look (light + dark checked), same
   receiver class names, so widgets already on home screens keep working (checked by upgrading the emulator).
   Side effect: the merged manifest no longer has WAKE_LOCK / ACCESS_NETWORK_STATE.
   Tried R8 `-repackageclasses`: the APK got bigger (1.46 MB), so it's not used.
- Left as is: classes.dex (Compose, Material 3, Room, DataStore) ~1.25 MB compressed. Replacing DataStore with
  SharedPreferences would save ~70 KB but would lose existing users' settings, so no.

### Still open from the developer's list
- **10. Match the design PDF:** still not in `design/` (only screenshots and `scrollnah-screens.html`).

## Version 2.0.2 (2026-10-27)
- `versionCode` 5, `versionName` "2.0.2": the landscape layouts below. GitHub release v2.0.2 (Latest), built
  with `./gradlew assembleGithubRelease` (the github flavor = what GitHub releases ship, tracker included).
- Release steps now: bump version -> commit -> `./gradlew clean assembleGithubRelease` -> copy the APK to a
  file named `ScrollNah-X.Y.Z.apk` (the `file#label` trick only sets the label, the download keeps the old file
  name) -> `gh release create vX.Y.Z ScrollNah-X.Y.Z.apk --target <FULL commit SHA> --latest` (a short SHA is rejected). Push without piping the output (piped pushes
  failed twice for no clear reason).

## Rotation / landscape (2026-10-27, developer: "it always tilts, should follow the phone's tilt setting")
- Checked: no activity sets `screenOrientation` and nothing calls `requestedOrientation`, so the app already
  follows Auto-rotate. Emulator: auto-rotate off + tilted = stays portrait (mRotation=0); auto-rotate on =
  rotates (mRotation=1). So the developer's phone most likely has Auto-rotate on.
- The real problem was the sideways layouts: cards stretched edge to edge, and the Pause screen cut the title
  and hid the reason chips. Fixed: `ReadableWidth` (max 640dp, centred) around every tab screen except Pause;
  onboarding limited to 640dp; Pause screen has a landscape layout (pill + mascot + breathing circle on the
  left, title/subtitle/chips scrolling + buttons on the right). Portrait layouts unchanged (checked).

## Play Store preparation (2026-10-26, developer: "start your part, without the tracker for play")
- **Android 16:** compileSdk/targetSdk 36, AGP 8.7.3 -> 8.13.2, Gradle 8.9 -> 8.13 (SDK platform 36 installed).
  Builds with no new warnings. Tested the Play debug build on the Nothing phone (Android 16): Limits has no
  Reels & Shorts section, Beast locked YouTube right away; YouTube limit restored to 60 min afterwards.
- **Flavors:** `github` (default, tracker included) and `play` (tracker removed via `tools:node="remove"` in
  `app/src/play/AndroidManifest.xml`, UI hidden by `FeatureWatchService.available`). Verified with aapt2: the
  play APK/AAB has no BIND_ACCESSIBILITY_SERVICE. Build outputs moved to `apk/github/...` and `apk/play/...`.
  Tests pass for both (`testGithubDebugUnitTest`, `testPlayDebugUnitTest`).
- **Pause screen:** a "compact" layout below 820dp tall (mascot and breathing circle side by side) so the
  reason chips fit on 1080x1920 phones without scrolling.
- **Store kit (`store/`):** `listing.md` (title "ScrollNah: Scroll Less", short + full description, release
  notes, all within Play limits), `play-console-answers.md` (data safety: no data collected; content rating;
  target audience 13+; foreground service description + video; signing via PEPK with the existing key;
  closed testing), `icon-512.png` and `feature-graphic.png` (rendered from `store/graphics/*.html` with headless
  Chrome), 8 screenshots 1080x1920 JPEG (demo mode, Play debug build), `fgs-demo.mp4` (Limits -> lock in ->
  YouTube -> Pause screen -> "Nah, I'm good"). `store/art/` = mascot PNGs from the debug gallery's new
  `--ez export true` mode.
- **Privacy policy:** `docs/privacy-policy.md` + `docs/index.md` for GitHub Pages (Settings > Pages > main /docs).
  Checked claims against the manifest: no INTERNET permission (ACCESS_NETWORK_STATE and WAKE_LOCK come from
  WorkManager via Glance).
- **Not done (needs the developer):** Play developer account ($25, ID check), 12 testers x 14 days (personal
  account), enabling GitHub Pages, a public support email, choosing PEPK/upload key in Play Console.

## Version 2.0.1 (2026-10-26)
- `versionCode` 4, `versionName` "2.0.1": 2.0.0 + the new app icon and opening screen below.
- Released as GitHub release v2.0.1 (marked Latest); `main` fast-forwarded to it.

## App icon + opening screen (2026-10-26, developer feedback: "face incomplete, no outline")
- New adaptive icon foreground (`drawable/ic_launcher_foreground.xml`): Blobby (happy) in the same shape as
  `BlobbyArt.kt`, with an ink outline, hard ink shadow, eyes with shine, cheeks and a smile, inside the 66dp
  safe zone. Lilac background unchanged. New `ic_launcher_monochrome.xml` (outline + eyes + smile) for
  Android 13+ themed icons: the Nothing launcher now shows that line version like its other icons (before,
  it fell back to the coloured icon in a ring).
- Opening (splash) screen: `androidx.core:core-splashscreen` 1.0.1. MainActivity uses
  `Theme.Unscroll.Starting` (Blobby on #F2EEFF, #16131F at night) and `installSplashScreen()`; it stays up
  until onboarding-vs-app is decided (`setKeepOnScreenCondition`), so the blank "checking" frame is gone.
  `Theme.Unscroll` windowBackground is now the same colour instead of white (no white flash in dark mode).
- Checked on the emulator (light + dark, app drawer) and the Nothing phone (dark splash, themed icon).

## Nothing Phone (A142, Android 16) test of 2.0.0 (2026-10-26)
- The developer's phone had 1.0.0 (YouTube 60 min, Shorts 15 min, Beast, name "Raj"). App data was backed
  up first (scratchpad tar). Installed the 2.0.0 debug build over it: DB 2 -> 3, no crash, name, limits,
  5-day streak and strictness all kept.
- Break reminder (YouTube override 5 min): #1 at exactly 5:00 with notification + bubble over YouTube
  ("I'm getting worried"), #2 at 10:00 sadder ("I'm melting here", melting badge). "Take a break" went to
  the Nothing launcher; `break_log` = shown, shown, break. Note: Nothing's notification shade can't be
  opened over adb (its swipe opens quick settings), so buttons were tested on the heads-up banner.
- Battery: "pausing checks (screen off)" logged within ~1 s of the system's SCREEN_OFF broadcast, "checks back
  on" on wake. After waking, Beast locked YouTube (49/45 min) within a second.
- Widgets on the Nothing launcher: the old 1.0.0 widget turned into the new medium widget by itself; the
  developer added the small and the animated Mascot widgets. All showed live data; the medium one updated
  "49 / 45" -> "49 min / 1h" within a minute of the limit change. The animated widget changes frames on this
  launcher (burst screenshots differ: Blobby bobs).
- Restored afterwards: YouTube 60 min, no per-app break time, Beast, screen timeout 30 s.
- Not tested here: BHIM (not installed; PhonePe, GPay and Paytm are), the iQOO.

## Version 2.0.0 (2026-10-26)
- `versionCode` 3, `versionName` "2.0.0" (developer's choice): 1.2.0 + the battery pass below.
- Released as GitHub release v2.0.0 right after 1.2.0; `main` fast-forwarded to it.

## Battery pass (2026-10-26, developer request)
- `BlockerService`: the 1-second check pauses (`awaitWork()`) while the screen is off (screen on/off
  broadcast receiver) or no limited app is switched on, and restarts the moment the screen turns on
  (with a 30-min look-back to learn what's open). The streak work and the widget check also wait for
  screen on. The widget check runs once a minute but only re-reads today's usage when a limited app or a
  browser was used, limits/theme changed, or the day changed (`widgetDirty`). Website minutes are read
  with the 10-second usage refresh instead of a DB query every second.
- `FeatureWatchService` (Reels/Shorts tracker): the 2-second heartbeat only runs while Reels/Shorts or a
  limited app's website is on screen; otherwise screen events wake it.
- Measured on the emulator (process CPU time, home screen, demo mode):
  before: ~0.36 s CPU per minute screen on, ~0.34 s screen off;
  after: ~0.16 s screen on (0.18 with the tracker on), 0 s screen off.
- Checked after the change: YouTube over its limit was blocked within a second after the screen slept for
  a minute and woke; Shorts in Chrome still detected. Log lines "pausing checks" / "checks back on".
- Still to do on the iQOO: Settings > Battery usage for ScrollNah over a normal day.

## Version 1.2.0 (2026-10-26)
- `versionCode` 2, `versionName` "1.2.0" (developer's choice; there was no 1.1). Includes everything from the
  2026-10-26 spec update: mascot themes, five moods, Bloomy's garden, break reminders, Glance widgets
  (incl. the animated mascot), search, payment-app help, small-screen fixes, new Home comparisons.
- Release APK builds and is signed with the same keystore as 1.0.0 (updates install over it).
- **Published 2026-10-26:** https://github.com/RAJASHEKARGUNAGANTI/scrollnah/releases/tag/v1.2.0
  (tag on commit 073ed2f, asset `ScrollNah-1.2.0.apk`, SHA-256 20fdd0ca...1e35). `main` fast-forwarded to it.
  Built in a separate worktree at 073ed2f so the then-uncommitted battery work wasn't included.

## Animated mascot widget (2026-10-26, developer request)
- New 2x2 widget "Mascot (animated)" (`MascotOnlyWidget`, receiver `MascotOnlyWidgetReceiver`): no background
  and no text (developer: "just plant or Blobby with moods"), the mascot in today's mood sized to the space
  (`SizeMode.Exact`). A minutes-left speech bubble was tried first and removed.
- Animation: `widget/MascotFrames.kt` draws 4 frames with `MascotBitmap.frame()` (bob up / middle / bob down /
  blink; Blobby bobs+squishes, Bloomy's stems sway) and puts them into 12 slots of a `ViewFlipper`
  (`layout/widget_mascot_frames.xml`, autoStart, 260 ms), embedded with Glance `AndroidRemoteViews`.
  The launcher does the flipping (no battery cost for ScrollNah). The same Bitmap objects are reused so only
  4 pictures are sent (~1 MB). "Remove animations" on = a still frame.
- The medium and small card widgets are unchanged (developer: keep them as they were). A "No background"
  style for them was tried and removed.
- Verified on the emulator's Pixel launcher: burst screenshots show the frames changing (Bloomy sways
  left/right, both blink), with Blobby and Bloomy. NOT verified: other launchers. Some (vivo/iQOO,
  Samsung, Xiaomi versions) may pause ViewFlippers or only show the first frame: check on the iQOO.

## Home "fun stat" wording (2026-10-26, developer feedback)
- The sitcom-episode / songs lines felt too specific, and then the plain version ("about a 3.8 km walk")
  felt flat. `ui/components/FunStat.kt` now has funny everyday comparisons, 4 per time range (3 for zero),
  picked once a day: "That's a cup of chai, brewed and sipped.", "You could've watered every plant you
  own.", "That's a whole pizza, from order to door.", "That's dinner cooked from scratch. Chef's kiss.",
  "That's a whole board game night. Someone would've flipped the table.", "That's enough time to bake
  bread. From flour." No exact distances or TV-show maths. `FunStatTest` checks every range.
- The streak and fun-stat cards on Home are now always full width (they shrank to fit short text).

## README screenshots (2026-10-26)
- `screenshots/` (540x1200 PNGs) is shown in a table in `README.md`: Home (Blobby + Bloomy), Pause,
  Your week, Limits, Break reminders, the reminder bubble, mascot picker, garden, widget.
- How they were taken: emulator, `pm clear`, then demo mode (`touch files/demo_mode`, restart twice),
  usage/overlay/notifications granted with `appops`/`pm grant`, and the System UI demo status bar
  (`settings put global sysui_demo_allowed 1`, `am broadcast -a com.android.systemui.demo -e command clock
  -e hhmm 0941`, battery 100, wifi 4, notifications hidden). Resize with `sips -Z 1200`.
- Found while taking them: `NeoShadowSurface` only stretched its shadow, not the card, when given
  `fillMaxWidth`/`weight` (visible on Home with Bloomy's shorter text). Fixed with
  `Box(propagateMinConstraints = true)`.

## Developer feedback 2026-10-26: search, payment apps, small screens
- **Search when adding apps:** shared `ui/components/AppSearchField` (search icon, clear X, keyboard
  "search" closes the keyboard). Used on the Add-an-app screen (now pinned at the top while the list
  scrolls, list has `imePadding`) and in onboarding's "pick apps" step (which had no search before).
  Ticked apps stay visible while searching so they can be unticked.
- **BHIM / UPI / bank apps asking to close or uninstall ScrollNah:** these apps refuse to run while a
  non-system app has an **Accessibility service** on (fraud protection). ScrollNah's only one is the opt-in
  Reels/Shorts & website tracker. ScrollNah can't (and must not try to) get around BHIM's check. What was done:
  - `data/PaymentApps.kt`: BHIM, PhonePe, Google Pay, Paytm, YONO, iMobile, HDFC, Axis, Kotak, CRED, MobiKwik.
  - When one opens while the tracker is on, `BlockerService` posts a calm notification once a day
    ("BHIM not opening?" + "Open Accessibility settings"), via `LimitActions.showPaymentAppHelp`.
  - The tracker disclosure (Limits) and Settings > Permissions now say UPI/bank apps may block it and to
    turn it off while paying. Limits, Pause screen, reminders and widget all work without the tracker.
  - The break bubble is never drawn over these apps.
  - NOT verified with BHIM itself (not on the emulator). If BHIM still complains with the tracker OFF, the
    trigger is something else (e.g. "Display over other apps"); ask the developer for BHIM's exact message.
- **Small screens / navigation buttons:** the bottom nav now has `navigationBarsPadding()` (it sat under
  3-button navigation before), `NavHost` uses `consumeWindowInsets(innerPadding)` (no double padding on the
  Pause tab), Pause screen and onboarding steps scroll above fixed buttons, option cards (strictness,
  reminder style) grow with large text, and `OptionCardRow` (LimitsScreen.kt) measures the words and stacks
  the cards when one wouldn't fit ("Notification" used to break mid-word), chip rows wrap (`FlowRow`),
  Settings/onboarding text fields stay above the keyboard (`imePadding`), weekly report rows share width
  instead of a fixed 150dp, the stepper's minutes text can widen, the Home mood pill moves above the
  mascot if it would wrap, Pause screen and onboarding use smaller pictures when the screen is under 700dp
  tall, and onboarding's app list scrolls its intro away with a sticky search box.
  Tested on the emulator at 360x640dp, 3-button nav, font scale 1.3: Home, Limits, Stats, Pause tab,
  Settings, Break reminders, Add an app (search), all onboarding steps. Then restored (1080x2400, 420dpi,
  gestures, font 1.0).

## Step 6 — Later features (streaks done; server features deferred until after v1)

### 6a. AI weekly insights — DEFERRED (needs a server; not in the first build)
**What:** once a week, a "Blobby's weekly take" card in Stats: a headline, one insight and one tip,
written by Claude in Blobby's voice from the week's numbers.

**Decisions still needed from the developer:**
1. Key setup: testing-only (API key in `local.properties` -> BuildConfig, own phone only, NEVER
   shipped) vs a proxy server from the start.
2. Model: Claude Opus 5.5 (`claude-opus-5-5`, default, about $0.01–0.02 per summary) vs Claude Haiku 5.5
   (`claude-haiku-5-5`, well under $0.001). Estimate assumes ~1.5K input + ~400 output tokens, not measured.
3. Whether they already have an Anthropic API key (console.anthropic.com) or need to create one.

**Planned design:**
- **Never ship the API key in the APK** (it can be extracted). For release: a tiny proxy, recommended
  a Cloudflare Worker using the official TypeScript SDK `@anthropic-ai/sdk`, holding the key, rate
  limiting about 1 call per device per week. The app POSTs the stats JSON to the proxy.
- **Data sent (opt-in screen first, off by default):** 7 daily totals, minutes per limited app,
  Pause-reason counts, pauses vs "Nah, I'm good", streak. Never URLs, other apps, or the user's name.
- **Request:** one Messages API call. Use structured outputs (`output_config.format`, JSON schema
  `{headline, insight, tip}`) so the card layout is stable. Low effort, short output. Check
  `stop_reason` (handle `refusal`); on Opus 5.5 enable the server-side `fallbacks: "default"` beta.
  Load the `claude-api` skill before writing the code (model IDs and API shapes change).
- **App side:** a `WeeklyInsight` Room table (week start, JSON, created at), generated once per week
  and cached. Card in `WeeklyReportScreen`, opt-in screen, friendly fallback when offline or on error.

### 6b. Streaks ✅ DONE (see "First-build features")
Save daily history in Room (so streaks don't depend on Android's ~7-day event history), milestones
(3/7/14/30), best streak, Blobby celebration, evening "almost kept your streak" nudge.

### 6c. Friend challenges (big)
Needs accounts (e.g. Google sign-in) + a backend (e.g. Firebase). Share weekly totals only. Weekly
leaderboard and invite links. Stats currently shows a "coming soon" card.

### Other leftovers
- vivo Browser support (address-bar view ID unknown; deferred by the developer).
- Hand tests: "5 more min" nudge button, reboot survival, Instagram Reels inside the app.
- Before Play Store: privacy policy, Accessibility + `specialUse` FGS declarations, onboarding flow,
  final name/icon, remove or limit `files/blocker.log` debug logging to debug builds, release signing.

---

## Quick resume checklist for next session
1. Open the project root (`/Users/rajashekargunnaganti/unscroll`) in Android Studio; `local.properties`
   already has the SDK path.
2. Run `./gradlew assembleGithubDebug` to confirm it still builds. A copy of the last debug APK is at
   `~/Downloads/unscroll-debug.apk`.
3. Test phone: iQOO I2221 (Android 16), USB debugging, serial `10BF5319GU002R3`. The developer is happy
   for Claude to drive it over adb but often uses it at the same time, so ask before long test runs.
   Read the debug log with `adb exec-out run-as com.unscroll.app cat files/blocker.log`.
   Second test phone: Nothing A142 (Android 16), serial `00069349C000377`. Fresh install, not set up yet.
   With 2+ devices connected, set `ANDROID_SERIAL` or use `adb -s <serial>`.
4. First build is fully on-device: no AI insights or friend challenges (both need a server). Pick the next on-device feature from the list the developer chose.
5. Run the unit tests with `./gradlew testDebugUnitTest`. Check mascot art with the debug gallery
   (`adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity`).
