# ScrollNah (formerly Unscroll) — Progress Log

Tracks what's done vs. what's left against the build order in `CLAUDE.md`.
Read this first when picking the project back up in a new session / Android Studio.

## Status (2026-11-04): Play closed testing (Alpha) release 11 (2.2.3) sent for Google's first review on 2026-11-03. Version 2.3.0 (code 12) is built (`release/ScrollNah-2.3.0-play.aab`, Play APK 1.54 MB) with everything since 2.2.3: backup & restore, time-left bubble, Beast cool-down, open limits, shared budgets, work hours, "Do this instead", Sunday week note, streak rewards, Xiaomi pop-up check, guided setup. Upload it to the closed track once 11 is approved. GitHub release v2.3.0 published 2026-11-04 (same commit 114e6e0). Next: 2.4.2 (code 15) = 2.3.0 + Parent lock + reel counting that works on real YouTube (also in picture-in-picture, no double counts) + reel count widget (GitHub version) + "Add widget", built 2026-11-06 as `release/ScrollNah-2.4.2-play.aab` and released on GitHub as v2.4.2 (v2.4.0 didn't count real YouTube Shorts; v2.4.1 could count a swipe-and-back twice); the developer will upload the bundle to Play after closed testing ends. Add the device-admin reviewer note from `store/play-console-answers.md` then. **Newer: 2.5.0 (code 16)**, built 2026-11-06 as `release/ScrollNah-2.5.0-play.aab` (same upload key `AC:58:24...`, Play release APK 1.68 MB) = 2.4.2 + daily reels limit, slow-down plan, setup reality check, Settings hints, sleeping widgets at night, mascot sounds, reels in the weekly report, and **the tracker in the Play version** (it needs the Accessibility API declaration + video from `store/play-console-answers.md`). **Newest: 2.5.3 (code 19)**, released on GitHub as v2.5.3 on 2026-11-06 and built as `release/ScrollNah-2.5.3-play.aab` = 2.5.0 + no black YouTube window over the Pause screen + readable "Not now" (2.5.1) + Limits asks about unsaved changes with a "Lock it in" bar (2.5.2) + Limits tabs (Apps / Reels & Shorts), Home's reels suggestion, the Home UX pass and the Home-button navigation fix (2.5.3); upload this bundle to Play instead of 2.5.0 / 2.4.2. Rule for first build: NOTHING that needs a server (AI insights + friend challenges deferred).

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
  `adb exec-out run-as app.scrollnah cat files/blocker.log`.
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
- **Demo mode (debug builds only):** `adb shell run-as app.scrollnah touch files/demo_mode`, then restart
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
- **Debug gallery:** `adb shell am start -n app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity` shows every
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
  `adb shell run-as app.scrollnah rm files/demo_mode`, then clear app data).

## Xiaomi pop-up permission check (2026-11-03, found on a Redmi Note 9 Pro Max)
- On Xiaomi, Redmi and POCO phones (MIUI/HyperOS) the Pause screen silently fails ("MIUILOG- Permission
  Denied Activity" in logcat) until the user allows App info > Other permissions > "Display pop-up windows
  while running in the background", even with "Display over other apps" on. Home's setup card was hidden
  because the three Android checks passed, so the old hint was never seen.
- `SetupChecks.backgroundPopupsAllowed` reads Xiaomi's own app-op 10021 (hidden `checkOpNoThrow`, by
  reflection); null on other phones or if it can't tell (then nothing is shown). `openPopupSettings` opens
  Xiaomi's permission page for ScrollNah (`miui.intent.action.APP_PERM_EDITOR`), falling back to App info.
- Checked on the Redmi with the Play 2.2.3 build: after "Always allow", the Pause screen opened over YouTube
  within ~1 s, and a floating YouTube window was paused with the "YouTube is paused" note. A test copy
  (`app.scrollnah.test`, built with a temporary `applicationIdSuffix`, installed from Downloads because
  MIUI's "Install via USB" wouldn't stay on) showed the pop-up row, its button opened the right page, and
  it turned to done after "Always allow".

## Guided setup: one switch at a time, only what's needed (2026-11-03, developer request)
- Developer: a list of every permission on every phone is overwhelming. Android can't grant these at
  once, so the setup now asks for **one switch at a time** ("Last step · 1 of 2" in onboarding, Home's
  "Almost ready" card with "Step 1 of 3" and one button), and **skips what the phone or the user's choices
  don't need**. Rules in `data/SetupPlan.kt` (tested in `SetupPlanTest`):
  - Notifications: always. Display over other apps: Firm/Beast/focus mode (Pause screen) or any bubble
    ("X min left" bubble, break-reminder bubble). Pop-ups in the background: only Xiaomi/Redmi/POCO
    (read from app-op 10021) and vivo/iQOO (can't be read: the user taps "I've done it", saved as
    `popups_confirmed`, not included in backups), and only when the Pause screen is used.
    Run in background: every phone except Google Pixels (they don't stop apps).
- **Comes back by itself:** `service/SetupReturn.kt` watches the switch twice a second for up to 2 min after
  a Settings page opens (usage access too) and brings ScrollNah back once it's on; `MainActivity.onResume`
  stops it. Some phones may refuse the jump back; Back still works.
- Settings > Permissions lists only the switches this phone has. Break reminders' overlay hint also comes back.
- Tested on the Pixel emulator (API 35): onboarding with Firm asked 2 steps (notifications, overlay), usage
  access and overlay both returned to ScrollNah by themselves, "You're all set" at the end; Home's card
  showed the one missing step; Settings had no battery/pop-up rows; card fits at 360x640 dp, font 1.3.
- Tested on the Redmi Note 9 Pro Max (MIUI 14, Android 12, test copy with demo data): Home showed "Step 1 of
  3" (overlay, pop-ups, battery; notifications already on), all three switched on and the card went away.
  Pop-ups and battery came back by themselves within ~1 s; overlay did NOT ("MIUILOG- Permission Denied":
  MIUI blocks any background start until the pop-up switch is on), so on Xiaomi the first step needs Back.
  Asking pop-ups first might fix that but Android may then block the jump back instead (untested).
  Not tried on vivo/iQOO yet.
- **Feedback while setting up** (developer: "it's like nothing was happening while giving permissions"):
  step dots next to the pill (`SetupDots`: done = lime with a tick, current = wider orange pill, to do =
  empty) and a lime "Display over other apps is on" note for 4 s after each switch (`SetupDoneNote`; the
  seconds start only once ScrollNah is resumed). After the last switch Home's card says "All set" for those
  4 s instead of vanishing. Steps finished earlier keep their tick (the guide remembers every step it saw).
- Fixes found while testing: (1) Xiaomi's battery page saves "No restrictions" a moment after you return,
  so the guide re-checks 1, 2 and 3 s after each return (the Redmi got stuck on "3 of 3" before);
  (2) `SetupReturn.stop()` moved from `onResume` to `onRestart`: a denied notification popup resumes the
  activity right after the fallback Settings page opens, which cancelled the watch.
- Tested on the Pixel emulator only (developer's request): dots and ticks in onboarding and on Home, the
  "Notifications are on" note between steps (denied popup -> Settings -> switch -> came back by itself),
  "All set" + note after the last step. The emulator was very slow (permission popup froze once), so some
  screenshots lagged; a temporary log confirmed the 4-second note each time.
- **Known Xiaomi limit (left as is, developer's choice 2026-11-03):** with Xiaomi's "Background autostart"
  off (the default), MIUI blocks `MY_PACKAGE_REPLACED`/boot restarts ("not permitted to auto start"), so after
  an update or reboot the blocker stays off until ScrollNah is opened. The switch is only in Security >
  Background autostart (`com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity`),
  not in App info on MIUI 14. App-op 10008 stayed "ignore", but Autostart was never actually turned on, so
  whether 10008 is that switch is unverified.
- **Beast mode: restoring a backup now needs the cool-down too** (2026-11-04). It was a gap: an older backup
  could bring back looser limits or Chill at once. `RestoreBackupAction` asks `BeastGuard` first (in Beast,
  any restore counts as loosening) and checks again before restoring; `BeastGuard.used()` now commits
  synchronously because the restore restarts the app. `BeastGuardDialog` is shared (`why` text). Tested on
  the emulator: Beast -> Settings > Restore a backup shows "Beast mode is on", no file picker; after
  "Start cool-down" it shows the countdown.
## Home UX pass (2026-11-06, developer: "is the UI good for UX?"; not released yet)
- Before: Today's limits came after the reels card, Focus, streak and fun stat, so it was always below the fold;
  the suggestion card was the biggest thing on the page. Now (see CLAUDE.md, Home): mascot card -> (setup) ->
  (running Focus) -> Today's limits -> one `SuggestionStrip` -> streak -> fun stat -> one-row Focus.
- Mascot card: `TimeLeftBar` ("49 min left today", ink bar on white); the reel count is a white pill next to the
  mood pill (`FlowRow`, wraps on narrow phones) instead of covering the mascot's head. Streak card drops its
  "(49 min left)". Fun stat leads with "46 min of scrolling today".
- Emulator (demo mode): light, dark, 360x640 at font 1.3 (Focus's "2 hours" wraps to a 2nd line). Note for
  tests: `adb shell monkey -p <pkg> 1` launches AND injects one random event (it tapped "Not now" twice);
  launch with `am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n ...` instead.
  Screenshots: `design/review/home-new-light.png`, `home-new-dark.png` (before: `home-2026-11-06.png`).
## Reel counting easier to find: Limits tabs + Home card (2026-11-06, after 2.5.2; not released yet)
- Developer: "the count reels option is gone into some corner". It was only on Limits, after apps, the slow-down plan
  and budgets, under a long disclosure; setup and Home never mentioned it.
- **Limits tabs** (developer's idea): `LimitsTab.APPS` / `REELS`, a sticky `LimitsTabs` pill bar (`stickyHeader`),
  shown only when Instagram or YouTube is installed. Drafts are shared, so one "Lock it in" saves both tabs.
  Switching tabs while scrolled jumps to the tab's top; "Change on the Apps tab" jumps to the end (strictness).
- **Home card** `CountReelsCard` (settings `reel_card_done`): after setup, tracker off, Instagram/YouTube
  installed. "Show me" -> `UnscrollApp.limitsOpenReels` -> Limits opens on the Reels tab (disclosure there).
- **Old navigation bug found and fixed:** Limits opened by a Home button (plain `navigate`) came straight back when
  the Home tab was tapped (also in 2.5.2): `popUpTo(HOME) { saveState }` saved Limits under Home and
  `restoreState` put it back. Home's buttons now use `openTab`, the tab bar's own navigation.
- Android 17 emulator (release build): card -> Reels tab; Home/Stats/Limits tabs and Back all work after it;
  Reels + strictness changes saved by one Lock it in; Not now survives a restart; dark mode; tracker on ->
  "So far today: 0 Reels & Shorts"; 360x640 at font 1.3 ("Reels & Shorts" wraps to 2 lines, tab bar stays pinned).
## 2.5.0 checked on the Nothing phone + 2 fixes (2026-11-06, after the v2.5.0 release; not released yet)
- **Emulator (released files):** GitHub APK and Play release APK with the stand-in "Instagram" app: open = 1,
  5 swipes = 6, 2 back = 6, leave and return = 6. Reels limit 10 / 20: Pause screen at exactly the 10th / 20th.
- **Nothing Phone (2a), GitHub 2.5.0 from the release page, real YouTube 21.43:** tracker turned on through the
  Limits disclosure card ("Agree & open Settings" -> Accessibility page + the PiP Settings hint, which works here);
  the developer flipped the switch. Counting 12/20 -> 19/20, one per Short; Pause screen at exactly 20; after
  "Open for 5 minutes" the count goes on (25/20) and the Pause screen came back ~2 s after the 5 minutes. A 40 s
  screen recording: one swipe = +1 about 1.5 s later; scene changes inside a Short don't count. Instagram not
  installed there, so real Reels are still untested.
- **Fix: black small window over the Pause screen.** At the reels/Shorts limit the tracker pressed Back and opened
  the Pause screen at the same moment; YouTube was still on the Short when covered and shrank it into
  picture-in-picture (paused, black) that floated over the Pause screen and everything after. Now `act` presses
  Back and opens the Pause screen `BACK_SETTLE_MS` (500 ms) later; `onDestroy` clears all handler callbacks.
  Phone, release build installed over 2.5.0 (data kept): Shorts -> YouTube Home -> Pause screen 0.58 s later,
  YouTube stopped, nothing pinned (twice, once recorded).
- **Fix: "Not now" on the tracker card** was light text on the always-light lilac card in dark mode: now Ink.
- **Unsaved Limits after leaving the app (developer report after 2.5.1: "edit the reel count and exit without
  saving, it was not prompting").** Every way out inside the app already asked (tabs, the arrow, Back, also the
  predictive Back swipe on Android 17), but leaving the app (Home, another app) can't be stopped, and on return
  Limits still showed the new numbers while the old limits were in force. Now: (1) `LimitsScreen` watches the
  activity: stopped with unsaved changes -> on return the "Lock it in first?" dialog shows at once (`{}` = stay;
  Discard resets the drafts); (2) "Lock it in" is a bar under the list that slides in only while something is
  unsaved (the old button at the very end of the list is gone; Beast's cool-down line stays in the list), and the
  list scrolls up by the bar's height (`LOCK_BAR_HEIGHT`) so the edited card stays in view. Android 17 emulator:
  YouTube for 8 s -> back = dialog; Discard / Lock it in from it; the bar saves; 360x640 at font 1.3 fits. (A
  Home-and-back within ~5 s doesn't stop the activity there, so no dialog: nothing was left.)
- Noticed, not changed: with every limited app switched off, Home shows the "Which apps eat your day?" setup card
  and Stats "Add some apps", so someone using only the reels limit sees their count only on the badge.
## Next batch (2026-11-06, developer: "do all this"): fixes + 5 features, not yet released
- **Setup note no longer moves the button:** `SetupDoneNote` keeps its space while it fades (alpha, not
  shrinking), so "Open settings" stays under the finger. Checked on the emulator (same position before/after).
- **Bubble steps aside:** the time-left/break bubble hides as soon as a touch on it moves past the touch slop
  (`BubbleOverlay.SwipeAway`); taps still dismiss/open. Making it pass touches through isn't possible: Android
  12+ blocks touches under another app's opaque window. The swipe that starts on it is lost, the next works.
- **Daily reels limit:** `data/ReelLimit.kt` (FeatureLimit row `reel_count`, count in limitMinutes, so it
  saves with "Lock it in", backs up and gets the Beast cool-down). Limits > Reels & Shorts > "Reels & Shorts a
  day" (10-500, step 10, default 50). Tracker: `addTime` checks `totalViews >= limit` in Reels/Shorts (apps
  only) -> `PauseReason.REELS` (pill "50 reels today", "That's your 50 reels for today. Breathe with me, then
  choose.", Chill nudge "Blobby: that's 50 reels", small-window pause). "Open for 5 minutes" = pass key
  "reels". Badge, Home sticker and widget show "32/50"; widget line "That's all for today!" at the limit.
  Emulator: limit 10 -> Pause screen right after the 10th video.
- **Slow-down plan:** `data/SlowDownPlan.kt` (+ SlowDownPlanTest): weekly steps since `lastStep`, never under
  the goal, never raised; settings in DataStore (`slow_plan_*`). `BlockerService.planWork` (in streakWork, every
  30 min while the screen is on) lowers enabled apps' limits (and weekend limits), moves lastStep by whole
  weeks, and sends "Bloomy: a little less this week" / "we made it". Limits card (switch, 5/10 min, goal
  5-120, "Next step: Fri, 20 Nov"). Emulator: date moved +8 days -> "1 step, 2 limits lowered", 30 -> 25 min.
- **Display over other apps first (developer, 2026-11-06):** a new optional onboarding page (Step 2, before
  usage access; "Open settings" / "Skip for now") asks for it, so the Settings hints can already help with
  usage access and every switch after it. Skipped = the last step asks again (only if the choices need it).
  Onboarding now has 10 dots: name, overlay, usage, apps, reality check, strictness, Parent lock, switches.
- **Tracker card in Limits (developer):** compact by default: heading, a 2-line key disclosure and the
  "Agree & open Settings" button; the chevron shows the rest (where it looks, the UPI note, where the switch
  is). The key facts stay right above the button (Google's prominent disclosure + consent).
- **Mascot sounds (developer idea):** `tools/make_sounds.py` synthesises 8 sounds (4 per mascot, ~41 KB in
  all, Ogg Vorbis stereo because ffmpeg's built-in Vorbis encoder needs 2 channels). `SoundFx` (SoundPool,
  USAGE_ASSISTANCE_SONIFICATION) plays them on: mascot poke (Home), celebrations, the setup "...is on" note,
  "Lock it in", the Pause screen (sad when it opens, happy on "Nah, I'm good"). Skipped on silent/vibrate
  and with the switch off. Notifications: `SoundFx.channel` makes "nudges_blobby" / "nudges_bloomy" (and
  "breaks_*", "*_plain" when sounds are off) with the mascot's nudge sound, and deletes the old "nudges" /
  "breaks" channels. Not over other apps (badge, bubble).
- **Mascot sounds, take 2 (2026-11-06):** the developer found the synthesised tones too "computer generated",
  didn't like free CC0 picks, and made 4 creature sounds with AI (`design/sounds/`: tap, happy, Pause_screen,
  notification; 2.04 s each with up to 0.8 s of silence first). `tools/make_sounds.py` was replaced by
  `tools/prepare_sounds.py`: finds where the sound starts/ends (10 ms steps, -50 dB, 3 loud steps in a row so
  a stray click after the end is cut), high-pass 120 Hz, 8 ms fade in / short fade out, gentle compressor,
  volume to a target (tap softest, notification loudest), the same soft echo (aecho 90/190/300 ms), limiter,
  32 kHz mono libvorbis q3 (~51 KB for 4 files) + WAV previews in `design/sounds/preview/`. Both mascots share
  the set for now (`SoundFx.rawId`). Notification channels are now "nudges_blobby_2" etc. (`SOUND_SET`, raise
  it when the nudge sound changes; older ones are deleted) and the sound URI uses the resource name
  (`android.resource://pkg/raw/mascot_nudge`), since resource numbers can shift between app versions.
- **Reels in the weekly report (developer, 2026-11-06):** `WeekStats.reelWeek` (5 new tests) adds up
  `feature_usage.views` for the last 7 days (per day, per feature, watch time of the in-app viewers) and
  compares with the 7 days before only if the tracker's first counted day (`FeatureDao.firstCountedDate`) is
  on or before last week's start. The average divides by the days since counting started. Shown when the
  tracker is available and has counted something (0 this week only while it's on). Stats: "Reels & Shorts"
  card after "Where the time went" (`ReelsCard`, `DayBars`). Week in review: pink "My reels" card (3rd):
  number, change pill, 7 bars, "About 69 a day, around 10 s each", "Stacked up, that's a tower of phone
  screens about 72 m tall" (15 cm a screen, `WeekStats.screenTower`); short cards drop the bars and tower.
  Verdict: "120 fewer reels than last week. My thumbs say thank you." (+-20) or "412 reels this week. I
  counted every single one." (100+). Sunday note: "...: 7h 34m and 412 reels, 38 min less...". Demo mode
  seeds two weeks of reel counts. Checked on the emulator (demo) at normal size and 360x640dp, font 1.3.
- **Tracker in the Play version (developer, 2026-11-06):** after first deciding to wait for production access,
  the developer chose to add it now. `play` gets `TRACKER_AVAILABLE = true` and `src/play/AndroidManifest.xml`
  no longer removes `FeatureWatchService` / the reel count widget (Play debug APK checked: both in the manifest).
  Google's checklist (support.google.com/googleplay/android-developer/answer/10964491): in-app disclosure of
  what's read and how it's used, affirmative consent, a way to decline, and a video showing the disclosure,
  declining (and the disclosure coming back), agreeing incl. Android's permission, and the core feature. So:
  `ui/components/TrackerDisclosure.kt` holds one text (now says what's read: Instagram/YouTube screen, video
  scrolling, browser address bar; what for; the one Back press at the user's own Reels/Shorts limit; only
  minutes + count saved, nothing sent). The Limits card shows it in full above "Agree & open Settings", plus
  "Not now" (hides the card until Limits opens again). Settings > Permissions' tracker row used to open
  Accessibility settings straight away: now it shows `TrackerDisclosureDialog` first (only when the tracker is
  off). The service description string mentions the Back press too. Privacy policy (no longer "GitHub only"),
  store listing (an optional-tracker paragraph + Accessibility in the permissions list, 3257/4000 chars) and
  `store/play-console-answers.md` (Accessibility declaration answers, reviewer steps, data safety note, video
  steps) updated. Checked on the emulator (Play debug, demo): card, "Not now", card back after reopening Limits,
  Settings dialog ("Not now" closes; "Agree" opens Accessibility settings with the PiP hint), turning it on.
  Not recorded yet: the declaration video. The emulator's YouTube can't play Shorts without a Google sign-in,
  so the core-feature part needs a real phone.
- **Setup reality check:** new onboarding step (now Step 5): 1-5 h chips -> "About N days a year" (h*365/24) +
  "win back 30 min a day = about 8 days a year", and a switch that turns on the slow-down plan.
- **Settings hints:** first built as an overlay window, but Android 12+ hides non-system overlays on Settings
  pages (Accessibility, special app access, device admin), so it's a picture-in-picture `HintActivity`
  (own task, excluded from recents, `makeLaunchIntoPip` on Android 13+, enterPictureInPictureMode before).
  Shown for usage access, Xiaomi/vivo pop-ups, battery, uninstall protection and the tracker's Accessibility
  page; closed by `SetupReturn` / MainActivity.onRestart, by the user, or after 60 s. Starting it while
  Settings is in front relies on "Display over other apps" (background activity start). Emulator: shown on
  the Accessibility page, closed on return.
- **Sleeping mascot at night** on all widgets (see CLAUDE.md, widgets). Emulator: Blobby and Bloomy asleep
  with a whole-day bedtime; back to awake when bedtime was turned off.
- 74 unit tests. Small screen (360x640, font 1.3): the new Limits cards fit. Sizes: GitHub release APK
  1.62 MB, Play 1.60 MB (+17 KB). Not tested on a real phone yet (PiP hint on MIUI/vivo especially).

## 2.4.1 / 2.4.2: reel counting that works with real YouTube, reel count widget (2026-11-06)
- **Real-phone finding (Nothing Phone (2a), Android 16, YouTube 21.43):** YouTube's Shorts pager
  (`reel_recycler`, a RecyclerView) reports scrollable=false and sends almost no scroll events (one in
  ~30 s, fromIndex/toIndex -1), so 2.4.0's counting (scroll events) counted nothing on real YouTube.
- **Now:** `checkScreen` (runs on every screen event from the app, and every 2 s) looks at the pager's child
  views: the one covering >= 60% of the pager is "the page". A different view than before = a new video
  (`ReelCounter.onPage`, keyed by `AccessibilityNodeInfo.hashCode()`, i.e. which view; nothing on screen is
  read). Pagers reuse a few views, but two videos never share one at the same time, so each swipe changes it.
  Swiping back is skipped when two pages were seen during the swipe (`onBetween`, upper/lower); YouTube only
  exposes one child, so there going back counts again. Scroll events are no longer used for counting.
- Verified on the Nothing phone by the developer: the count matched the Shorts swiped (log: "page
  youtube_shorts: view ... -> +1" once per swipe; coming back within a minute continues the visit).
- **Badge mascot fix:** the badge used `container.mascot`, which starts as Blobby until the settings load;
  right after a restart it drew Blobby and kept it 5 min. Now it reads the theme from the settings
  (`WidgetData.load`) and redraws when the mascot changes.
- **Reel count widget** (`ReelCountWidgetReceiver`, `layout/widget_reels.xml`, `xml/reels_widget_info.xml`):
  "12 / REELS TODAY", `MascotLines.reelWidgetLine`, mascot in today's mood. First in the "Add widget" picker
  (`MascotWidgets.available` hides it in the Play version, whose manifest removes the receiver).
- **Double counts (developer report "double count on one swipe", after 2.4.1):** YouTube reports the next
  page as soon as it covers most of the screen, so a swipe forward and straight back (or a drag that
  springs back) counted +2 (A -> B -> A within a second, seen in the log). Now a new page only counts once
  it has stayed `ReelCounter.SETTLE_MS` (800 ms); `settleCheck` re-checks right then, so the badge still
  updates within ~1 s. Tested on the Nothing phone with real YouTube: 5 swipes = +5, a short slow drag that
  really moved on = +1 (YouTube advances after ~1/5 of the screen), a tiny drag that sprang back = +0,
  forward then straight back = +0 (same video, badge unchanged). Videos skipped in under 0.8 s don't count.
- **Picture-in-picture (developer report: "not tracking when YouTube was in a floating window"):** pressing
  Home during Shorts puts it in PiP (`mode=pinned`, aspect 9/16, actions Previous|Play/Pause|Next), and the
  tracker only looked at `rootInActiveWindow` (the launcher). Now `checkScreen` also searches `windows`
  (needs `flagRetrieveInteractiveWindows`) for Instagram/YouTube windows: active window in PiP, or a
  non-active app window (split screen, pop-up) -> `Seen(floating = true)`. Time and videos keep counting
  (pressing Next in the PiP window shows a new page view), the badge hides (`badgeAway`), and at the limit
  `act` pauses playback (`MediaPauser`) with `LimitActions.showFloatingPaused` instead of Back + Pause screen.
  Tested on the Nothing phone: Home -> PiP, no false count when it shrank, Next x3 -> +3, back to full screen.
- The Nothing phone had the Play 2.2.3 and an old GitHub 2.2.2 (`com.unscroll.app`) installed side by side;
  both were removed at the developer's request and the test copy `app.scrollnah.test` installed by adb
  (adb install works on that phone).

## Reel counting + "Add widget" button (2026-11-06, developer request after looking at BrainPal)
- (Superseded in 2.4.1, see above: counting by page view, not scroll events.)
- **Reel counting (tracker only, so GitHub version for now):** `FeatureWatchService` now also listens to
  `typeViewScrolled`. While the Reels/Shorts viewer is open (a "visit"), scroll events from the pager
  (`clips_viewer_view_pager`, `reel_recycler`) go to `ReelCounter`: the opening video counts 1, then each
  page that shows up (fromIndex..toIndex) for the first time in the visit adds 1 (swiping back doesn't).
  It does NOT wait for the settled event (from == to): Android batches scroll events for the service
  (notificationTimeout 300 ms), and in the emulator test only "between two videos" events arrived.
  Back within 60 s = same visit. Apps that give no page numbers: a swipe moving >= 40% of the screen counts.
  Saved in `feature_usage.views` (DB version 8, migration adds the column; backups carry it automatically).
  Debug builds log every scroll event to `files/blocker.log` ("scroll youtube_shorts: from=3 to=3 ... -> +1",
  or "scroll ignored: <id>") to tune the IDs on real phones.
- **Badge while scrolling:** `ReelBadge` = lime pill with the mascot (today's mood from `WidgetData.load`,
  redrawn at most every 5 min) and today's total (Reels + Shorts), top centre under the status bar, a small
  hop when it goes up. `TYPE_ACCESSIBILITY_OVERLAY`, so no overlay permission and FLAG_NOT_TOUCHABLE.
  Settings > Reminders > "Reel count while scrolling" (default on, only shown when the tracker exists).
- **Home:** "0 reels" / "85 reels" sticker on the mascot's head (tracker on, or a count today).
- **Add widget:** `MascotWidgets.Kind` + `canPin`/`pin` (`requestPinAppWidget`), `AddWidgetDialog` (3 choices
  with mini mascots; manual steps if the home screen can't pin). Settings > Mascot > "Home-screen widget: Add",
  and Home's "Put Blobby on your home screen" card (after setup, until a widget is placed or "Not now").
- Texts updated: Accessibility description, the Limits disclosure ("never reads messages, passwords or other
  apps"), privacy policy (minutes and the count; 6 Nov 2026).
- Tested 2026-11-06 on the emulator (API 35): unit tests (`ReelCounterTest`, 8 cases; 70 tests in all); Home sticker; widget
  card, picker and the Pixel launcher's "Add to home screen" prompt (card gone once placed); 360x640 at font
  1.3. Counting with a throwaway stand-in app (package com.instagram.android, a full-screen snapping ListView
  with id `clips_viewer_view_pager`, kept in the session scratchpad, never in the repo): opening video = 1,
  10 swipes forward counted, 3 back not counted, leave and return within a minute not counted, badge = Home
  count (17); the R8 release APK counted 3 swipes -> 4. The emulator's YouTube is too old for Shorts, so
  NOT yet tested with real Instagram/YouTube (IDs and event batching may differ) - do it on the Redmi/iQOO.
- Sizes: GitHub release APK 1.61 MB, Play release APK 1.58 MB (the tracker code is removed from Play by R8).

## Parent lock: levels 1, 2 and 3 (2026-11-04, developer request)
Developer: on a child's phone, kids got around limits; protect everything with a PIN or pattern, choose it during
setup, a lock button on Home, an on/off switch in Settings that also shows/hides the Home button, and make
uninstalling need the lock too. Name chosen: **Parent lock**.
- **Level 1, the lock** (`data/ParentLock.kt`, `ui/components/ParentLockUi.kt`): PIN (4-8 numbers) or 3x3 pattern
  (>= 4 dots, passing over a middle dot picks it). Stored as a salted PBKDF2-SHA256 hash in SharedPreferences
  `parent_lock` (not in ScrollNah backups). 5 free tries, then 30 s, 1 min, 5 min, 15 min waits. "Forgot PIN"
  starts a 24-hour wait (the right PIN cancels it), then a new PIN can be set without the old one; both noted in
  the report. Unlocking lasts while a locked page is in use + 5 min; the screen turning off (receiver in
  UnscrollApplication), the Home lock button, or ScrollNah restarting locks it again.
  Protected: Limits, Settings, Break reminders and Add app (`ParentLockGate` in UnscrollApp), the Pause screen's
  "Open for 5 minutes" (PIN popup; locks again right after), ending focus early on Home (PIN; the Quick Settings
  tile just says "end focus in ScrollNah"), Chill's "5 more min" notification button (hidden; NudgeReceiver
  ignores old ones). With the lock on, Beast's cool-down is skipped (the PIN already proves it's the parent).
- **Level 2, the report** (`data/TamperLog.kt`, `service/TamperWatch.kt`): only while the lock is on, max 100
  entries, newest first. Notes: a needed switch turned off (must stay off >= 20 s; Android briefly read the overlay
  as off during setup) and back on, ScrollNah force-stopped (Android 11+ `ApplicationExitInfo` REASON_USER_REQUESTED,
  checked at app start), the clock moved >= 2 min (manifest TIME_SET receiver, offset vs elapsedRealtime, same boot
  only), 5 wrong tries, "Forgot PIN" asked/cancelled/used, uninstall protection turned off. Settings > Parent lock >
  Report lists them; an orange dot on Home's lock button means unseen entries.
- **Level 3, uninstall protection** (`UninstallGuardReceiver`, `res/xml/uninstall_guard.xml` with empty
  uses-policies): optional switch in setup (default on when the lock is chosen) and Settings; the guided setup adds
  an "uninstall protection" step (`SetupStep.UNINSTALL_GUARD`). Android then refuses to uninstall
  (`DELETE_FAILED_DEVICE_POLICY_MANAGER`) until it's deactivated; deactivating shows ScrollNah's "ask a parent"
  message and is noted in the report, and Home asks to turn it back on. It can't truly stop a determined child
  (Android allows deactivating, force stop, safe mode, factory reset); Settings points to Google Family Link.
- Setup: new page "Is this phone for a child?" (step 5 of 8) between strictness and permissions.
- Also fixed: Settings > Focus & passes let Beast mode end focus early (Home and the tile already blocked it).
- Tested on the Pixel emulator (API 35): setup with PIN + uninstall protection (3 steps), Android refused
  `adb uninstall`, Limits locked, wrong PIN message, right PIN opens Limits, Home lock button locks, Pause screen
  "Open for 5 minutes" asked for the PIN and then opened Chrome, report noted overlay off (after ~25 s), force stop,
  clock +3 h ("forward by 2 h 59 min"), and deactivating uninstall protection (asked + off). Unit tests in
  `ParentLockTest`. Redmi (MIUI 14, 2026-11-04, test copy): setup with a pattern, the uninstall-protection step
  opened Android's "Activate device admin app?" page and activated it; Home shows the lock button (dark mode).
  Still to try on a real phone: locked pages with the pattern, the report, and vivo/iQOO; the lock pages weren't
  seen at 360x640.
- Play: no new permission, but reviewers check device-admin apps: reviewer note in `store/play-console-answers.md`.
  Privacy policy updated. Ships in the version after 2.3.0.

- **Play signing:** the Play-installed app is signed by Google's app signing key (CN=Android, O=Google Inc.,
  SHA-256 40:0F:DE:87:8D:F2:7E:9E...), not ours (AC:58:24:BA...). GitHub APKs and Play installs can't update
  each other. (`apksigner verify` needs `--max-sdk-version 36` on Play APKs: they carry a v3.2 post-quantum
  signer it can't read.)
- The developer's Redmi now runs the test build `app.scrollnah.test` (github debug, built with a temporary
  `applicationIdSuffix = ".test"`, installed from Downloads) instead of the Play version.

## App ID is now `app.scrollnah` (2026-11-02, developer request in the Play Console "Create app" form)
- `applicationId` changed from `com.unscroll.app` to `app.scrollnah` before the first Play upload (it can
  never change once the app is on Play). `namespace` and all code packages stay `com.unscroll.app`.
  First tried `com.scrollnah.app`, but Play said it was already in use (not on the public store, so
  another developer's draft or removed app); `app.scrollnah` was free on 2026-11-03.
- Pause/break `taskAffinity` now use `${applicationId}` (like the FileProvider), so the old and new app
  can sit side by side on one phone without sharing tasks.
- adb commands: `run-as app.scrollnah`, and full class names with `am start -n`
  (`app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity`; the `/.debug...` shorthand no longer works).
- GitHub releases up to v2.2.3 are `com.unscroll.app`: the new ID installs as a separate app (it can't
  update the old one), and 2.2.3 has no Backup yet, so those users start fresh and should uninstall the
  old app (two copies would both show pause screens).
- Play bundle rebuilt from the v2.2.3 code with only the new ID: `release/ScrollNah-2.2.3-play.aab`
  (the old-ID bundles moved to `release/old-id/`).
- Play Console: app created as `app.scrollnah`; internal testing release 11 (2.2.3) published 2026-11-03.
  Play says 1.45 MB download; the Play page on the phone says 2.9 MB because Android 12+ gets the code
  (2.7 MB) unpacked. Not a size regression.

## Open limits, shared budgets, work hours, ideas, Sunday note, rewards (2026-11-02, developer request, not released yet)
All tested on the emulator (Firm mode unless said); 48 unit tests; release APK 1,544,702 bytes (2.2.3: 1,495,550).
- **Sunday "week in review is ready"** (`BlockerService.weekReadyWork`, in the 30-min streak loop): Sunday from 6 PM, once,
  only if a finished day this week has data. "Blobby: your week in review is ready" / "I put your week together: 7h 34m,
  38 min less than last week. Come have a look!" (`MascotLines.weekReadyText`). Tapping opens Stats > week review
  (`MainActivity.EXTRA_OPEN`, CLEAR_TOP|SINGLE_TOP so an open app gets `onNewIntent`). Settings > Reminders switch
  (default on). Debug trigger: `am start -n app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity --ez weekready true`.
- **"Do this instead"** (`ui/screens/InsteadIdeas.kt`): white "Try this instead?" card on the Pause screen (also when locked)
  with one idea, random each time, "Another idea" cycles. Settings > "Do this instead": remove ideas, add your own
  (max 8, 60 chars). Starter list in `SettingsRepository.STARTER_IDEAS`; an empty list hides the card.
- **Open limits** (`TrackedApp.maxOpens`, DB v6): Limits row "+ Limit opens" -> "Opens a day" stepper (1-10 by 1, then by 5
  to 50). The service counts today's opens when a limited app is opened (`opensOverLimit`); past the limit it acts like the
  time limit with `PauseReason.OPENS` ("Woah. YouTube again?" / "You said 1 open a day was plenty."). Beast locks till
  midnight. Home shows "3 / 10 opens". More opens or removing it = loosening for the Beast cool-down.
  Note: Android counts every resume as a visit, so killing/reinstalling ScrollNah while it's in front briefly resumes the
  app underneath (seen on the emulator: 10 "opens" of YouTube from test reinstalls). Normal use doesn't do that.
- **Work hours** (`FocusRules.WorkHours`, prefs "focus": work_enabled/start/end/days): Limits card under Bedtime with
  times and M-S day chips (default off, Mon-Fri 9-5). A shift past midnight belongs to the day it started (tested).
  `PauseReason.WORK`, "Work time. YouTube can wait." Behaves like bedtime: Chill nudges, Firm allows +5, Beast locks.
  Shortening it is loosening (`BeastGuard.workHoursLoosened`). Included in backups.
- **Shared budgets** (`AppGroup` table `app_groups`, DB v7): Limits "Shared budgets" section (shown with 2+ apps):
  "Make a shared budget" asks a name, then tick apps, set minutes (5-300), switch, rename, remove. Each app keeps its own
  limit; the group counts the minutes of its apps whose own limit is on. The service pauses a member when the group is
  used up ("Social had 5 min together today, and it's all used."), the time-left bubble uses the group if it has less
  left ("Only 4 min of Social left today!"). Home shows a row per group with its apps; a used-up group counts as "an app
  at its limit" for the mood (Home and widgets). Removing an app also takes it out of groups.
- **Streak rewards** (`data/Rewards.kt`, `ui/mascot/RewardArt.kt`): Blobby: Cap, Party hat, Beanie, Crown; Bloomy: lilac,
  sky, pink, white pot; unlocked at a best streak of 3, 7, 14, 30 days (`SettingsRepository.bestStreakEver`, only goes
  up, so Clear history keeps them; recorded by Home and the service). Settings > Mascot shows the picker (locked ones
  show the days needed). Blobby is drawn at 86% when wearing a hat so it fits the same square; everything that draws the
  mascot gets it (app, Pause screen, bubble, notifications, widgets via `MascotLook.reward` / `MascotBitmap`). The
  30-day crown and pot are white so they show on the lime card. The milestone celebration mentions a newly earned one.
  Widgets weren't on the emulator's home screen, so the widget hat is not seen yet.

## Backup, time-left bubble, Beast cool-down (2026-11-02, developer request, not released yet)
- **Backup & restore** (`data/Backup.kt`, `ui/screens/BackupUi.kt`): Settings > Data > "Save a backup" writes one
  JSON file where the user picks (Storage Access Framework, no permission, nothing uploaded); "Restore a backup" is in
  Settings and on the first setup screen ("Moving from another phone? Restore a backup"). Every Room table is copied
  generically (sqlite_master + PRAGMA table_info, so new tables/columns need no change here), plus all DataStore
  settings (`SettingsRepository.exportJson/importJson`, typed values) and the bedtime (FocusRules prefs). Restore asks
  first (date, app version, apps / days / pots), replaces everything in one transaction, then restarts the app.
  A backup from a newer app (format or DB version) is refused. Tested on the emulator: backup, `pm clear`, restore
  from setup -> name, limits, streak (5 days), Stats history and garden back.
- Found while testing restore: the Stats chart read only Android's own usage events, so restored days (and days older
  than Android's ~7-10 day memory) showed empty bars while "Where the time went" had them. `loadWeekReport` now uses
  the saved `daily_summary` total for finished days and falls back to live events only for days never saved.
- **Time-left bubble**: when a limited app opens under its limit, the mascot badge says "27 min of YouTube left
  today." ("Only 4 min ..." at 5 or less; `MascotLines.timeLeftText`). Needs "Display over other apps", never over
  payment apps, at most once per app per 10 min. Settings > Reminders > "Show time left" (default on).
- **Beast cool-down** (`data/BeastGuard.kt`, tested): in Beast mode, any change that gives more time (higher limit,
  limit off, bigger/removed weekend limit, removing an app, lower strictness, shorter or no bedtime) asks for a
  10-minute cool-down; after it there are 15 minutes to save one loosening change, then it locks again. Stricter
  changes save straight away. Status line under "Lock it in". "Keep it strict" discards the unsaved changes.
  Tested on the emulator (raise blocked -> cool-down -> saved 35 min; lowering instant; removing an app asks).

## Version 2.2.3 (2026-11-01)
- `versionCode` 11, `versionName` "2.2.3": Bloomy's bud on the Settings mascot card, store prep (listing, reviewer
  notes, logo-free screenshots), `store/tester-invite.md`, `.gitignore` for `brag-output*/` and `release/`.
- Built for the first Play upload: `release/ScrollNah-2.2.3-play.aab` (local only). Same code as the GitHub APK.

## Play Store submission prep (2026-11-01)
- Developer paid for and applied to Play Console; the account is in Google's review.
- Readiness check of the Play bundle (versionCode 10, 2.2.2): targetSdk 36; the only native lib
  (`libandroidx.graphics.path.so`) has 16 KB LOAD alignment on all ABIs and the APK passes `zipalign -c -P 16`;
  no AccessibilityService and no INTERNET permission in the Play manifest; signed with the release key; debug log
  is debug-only.
- `store/listing.md`: full description rewritten for 2.1-2.2 (weekend limits, floating-video pause, growing bud,
  per-app weekly report, week in review, Focus tile, TalkBack, size) = 2788/4000 chars; first-release notes now launch
  wording (233/500); duplicated "Category/Contact" section removed.
- `store/play-console-answers.md`: reviewer steps mention "See the Pause screen" and the Focus tile, a note on pausing a
  limited app's picture-in-picture playback via audio focus, a Data safety line on user-initiated "Share this card",
  and the FGS description mentions the floating-window pause.
- `store/screenshots/04-week.jpg` and `08-limits.jpg` retaken without the YouTube logo (YouTube hidden on the emulator
  with `pm uninstall -k --user 0`, restored with `cmd package install-existing`), so no third-party logos in the store
  screenshots. 03-break-reminder still shows Chrome + a Wikipedia page.

## Version 2.2.2 (2026-10-30)
- `versionCode` 10, `versionName` "2.2.2": the floating-window fix below (+ "Blobby: YouTube is paused" message) and
  the Settings tab / Pause preview change.
- README screenshots retaken after the release (new bottom bar, Settings tab, Stats per-app list + verdict, Bloomy's
  bud + "Next flower"): home-blobby, stats, limits, break-reminders, home-bloomy, mascot-picker, garden, bloom.
  Same method as before (demo mode, System UI demo status bar 9:41, `sips -Z 1200`). Found while doing it: the
  Settings mascot card didn't show Bloomy's bud; fixed (`MascotChoice(budDays)`), not yet in a release.
- Play Store screenshots (`store/screenshots/`, 8 x 1080x1920 JPEG) retaken the same day with the Play debug build
  (`wm size 1080x1920`, demo mode, demo status bar): new bottom bar, per-app Stats list, Bloomy's bud + "Next flower",
  Limits with a weekend limit, Pause screen from the Limits preview, and a real break reminder (Chrome with a 5 min
  per-app break time, Wikipedia page, notification + bubble). `store/listing.md` text not updated for the 2.1-2.2
  features yet (weekend limits, Focus tile, per-app stats, week in review).

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
  app.scrollnah/com.unscroll.app.service.FeatureWatchService` (+ `accessibility_enabled 1`), notification shown
  with the debug trigger `adb shell am start -n app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity --es paymenthelp BHIM`,
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
  `adb shell cmd statusbar add-tile app.scrollnah/com.unscroll.app.service.FocusTileService`. The emulator still drew a ">"
  arrow on the tile after the toggle flag was added (not re-checked; likely cached by System UI).
- **Bloomy celebrates growth:** `Growth.Result.newFlowers`; `GrowthRepository.update()` stores a `BloomEvent`
  (FLOWER or POT, only when Bloomy is the mascot; a pending POT is never downgraded) in DataStore
  (`bloom_event`). Home shows `BloomDialog` once ("New flower!" / "Full pot!" + "See garden"), after any
  streak celebration. Debug trigger: `adb shell am start -n app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity --es bloom FLOWER`.
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
   Read the debug log with `adb exec-out run-as app.scrollnah cat files/blocker.log`.
   Second test phone: Nothing A142 (Android 16), serial `00069349C000377`. Fresh install, not set up yet.
   With 2+ devices connected, set `ANDROID_SERIAL` or use `adb -s <serial>`.
4. First build is fully on-device: no AI insights or friend challenges (both need a server). Pick the next on-device feature from the list the developer chose.
5. Run the unit tests with `./gradlew testDebugUnitTest`. Check mascot art with the debug gallery
   (`adb shell am start -n app.scrollnah/com.unscroll.app.debug.MascotGalleryActivity`).
