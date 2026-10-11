# Unscroll — Progress Log

Tracks what's done vs. what's left against the build order in `CLAUDE.md`.
Read this first when picking the project back up in a new session / Android Studio.

## Status: Steps 1–5 DONE ✅ (+ browser blocking, reliability, light/dark mode). Rule for first build: NOTHING that needs a server (AI insights + friend challenges deferred).

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
  - `openUsageAccessSettings()` deep-links to Unscroll's own toggle (`package:` URI), with a
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
- Settings opens inside Unscroll's task, so the user returns with Back, which is standard behaviour.

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
2. Run `./gradlew assembleDebug` to confirm it still builds. A copy of the last debug APK is at
   `~/Downloads/unscroll-debug.apk`.
3. Test phone: iQOO I2221 (Android 16), USB debugging, serial `10BF5319GU002R3`. The developer is happy
   for Claude to drive it over adb but often uses it at the same time, so ask before long test runs.
   Read the debug log with `adb exec-out run-as com.unscroll.app cat files/blocker.log`.
   Second test phone: Nothing A142 (Android 16), serial `00069349C000377`. Fresh install, not set up yet.
   With 2+ devices connected, set `ANDROID_SERIAL` or use `adb -s <serial>`.
4. First build is fully on-device: no AI insights or friend challenges (both need a server). Pick the next on-device feature from the list the developer chose.
