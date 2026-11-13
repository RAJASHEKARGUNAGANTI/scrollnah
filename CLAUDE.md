# ScrollNah (formerly "Unscroll") — Android digital wellbeing app

**ScrollNah** (final name, chosen 2026-10-11; "Unscroll" was too crowded on the stores) helps people cut down excessive social media use. It should feel **funky, playful and easy to use**, never strict or preachy. A mascot reacts to the user's usage. There are two mascot themes: **Blobby** (default, an orange blob) and **Bloomy** (a plant in a pot that grows flowers).

Always write the name as **ScrollNah** (capital N) in the UI and docs. The lowercase "scrollnah" wordmark on Home is the only exception. The app ID (Play package name) is **`app.scrollnah`** (changed from `com.unscroll.app` on 2026-11-02, before the first Play upload; it can never change again). The code's package/class names stay `com.unscroll.app`, `UnscrollApplication` etc. (`namespace`; users never see them). In adb, use the app ID with full class names: `adb shell am start -n app.scrollnah/com.unscroll.app.MainActivity`, `run-as app.scrollnah`.

## Tech stack

- Android only for v1, native **Kotlin + Jetpack Compose**, Material 3 as a base but heavily restyled.
- minSdk 26, targetSdk latest stable.
- Usage data: `UsageStatsManager` / `UsageEvents` (needs `PACKAGE_USAGE_STATS`, granted via Settings > Usage access).
- Detecting launches and session length: one **foreground service** (`BlockerService`) that polls usage events about every 1 s and knows which app is in front.
- **Floating windows (picture-in-picture, e.g. YouTube's small video window):** Android doesn't count them as "in front", so a blocked app could keep playing there. `BlockerService.checkFloatingPlayers` spots a blocked app that is paused-but-not-stopped (`data/FloatingWindows.kt`, Android 10+) while sound is playing, and `service/MediaPauser` pauses it by briefly taking audio focus (no permission). YouTube / YouTube Music are also paused when blocked, for background play. Floating-window time is not counted toward the daily limit (only time in front is).
- Pause screen: a full-screen Activity launched over the app (needs `SYSTEM_ALERT_WINDOW` to open from the background).
- Mascot bubble: an overlay window (`SYSTEM_ALERT_WINDOW`).
- Notifications: `NotificationCompat` with action buttons. Ask for `POST_NOTIFICATIONS` on Android 13+.
- Home-screen widgets: plain **RemoteViews** layouts (`widget/MascotWidgets.kt`). Jetpack Glance was used until 2026-10-27 and removed to keep the app small (it added ~600 KB); don't add it back.
- Keep the APK small (developer request, 2026-10-27: "we need to make it compact"; Play release APK 1.54 MB at 2.3.0). Check the size before adding a library; fonts are trimmed copies (`tools/subset_fonts.py`), app text is English only (`localeFilters`).
- **Do NOT use AccessibilityService** for blocking. Google Play restricts it for non-accessibility apps.
  - **Exception (decided 2026-10-11):** an opt-in, off-by-default `FeatureWatchService` is used ONLY to detect Instagram Reels / YouTube Shorts and, at the developer's request, the websites of limited apps in browsers (reads only the browser address bar). Both are impossible with UsageStats. It must stay opt-in, show a prominent disclosure before sending the user to Settings, only inspect those apps and the address bar, and never store screen content or addresses (minutes only, plus a count of Reels/Shorts videos). Its only action: at the user's own Reels/Shorts limit (Firm/Beast) it presses Back once (`FeatureWatchService.act`); this is in the disclosure and the declaration, so any new action needs both updated (and the Play declaration resubmitted). **Disclosure:** every way to Android's Accessibility settings while the tracker is off must show `ui/components/TrackerDisclosure.kt` first (Limits card with "Agree & open Settings" / "Not now"; the same text as a dialog from Settings > Permissions; the setup guide's "Count Reels & Shorts" step (`SetupStep.TRACKER`, asked when a reels number or Reels/Shorts limit is on) shows the same words with "Not now", which stops asking (`trackerSetupSkipped`)). Break reminders and moods must NOT depend on it.
  - **Reel counting (2026-11-06, developer idea):** while the Reels/Shorts viewer is open, the tracker counts about how many videos were scrolled: each time a different page (child view) fills the video pager (`Feature.pagerId`), that's a new video (`data/ReelCounter.kt`, tested; saved as `feature_usage.views`, only the number). Scroll events can't be used: YouTube's Shorts pager hides its scrolling from Accessibility (checked on a real phone, 2026-11-06). The count shows with the chosen mascot in today's mood at the top of the screen (`service/ReelBadge.kt`, an Accessibility overlay that touches pass through; switch in Settings > Reminders), on Home ("85 reels" on the mascot) and on the "Reel count" widget. A new page counts only after it has stayed 0.8 s (`SETTLE_MS`), so a peek or a swipe-and-back doesn't count twice. On YouTube, going back to an earlier Short (and staying) counts it again (only one page is visible to the tracker). Reels/Shorts in a small window (YouTube shrinks Shorts into picture-in-picture, with a Next button, when you go Home; also split screen or pop-up windows) are found through `windows` (`flagRetrieveInteractiveWindows`): their time and videos still count, the badge hides, and at the limit the video is paused with the "is paused" note instead of a Pause screen. Not on websites. BrainPal (Play, 1M+) does the same with Accessibility. **In the Play version too since 2026-11-06** (developer's choice, after first deciding to wait for production access): the first Play upload with it needs the Accessibility API declaration and a video (`store/play-console-answers.md`).
  - UPI/bank apps (BHIM, PhonePe, GPay, Paytm, bank apps; list in `data/PaymentApps.kt`) often refuse to open while any app's Accessibility service is on and ask the user to close/uninstall it. Never try to hide from or get around that check. Instead: say so in the tracker's disclosure and in Settings, show a once-a-day notification when a payment app opens while the tracker is on (buttons: "Turn off tracker", which calls `disableSelf()`, and "Accessibility settings"; only the user can turn it back on), and never draw the bubble over payment apps.
- Local storage: Room (usage logs, limits, streak days, break reminder log, saved pots) + DataStore (settings, chosen theme, break reminder settings, current pot progress).
- Mascot art: drawn with Compose Canvas shapes (one drawing function per theme, also rendered to bitmaps for the widget, bubble and notifications). Motion is Compose animation (per-mood movement in `AnimatedMascot`); no Lottie, to keep the app small.
- App icons in lists come from the phone (`AppTile`), with the letter tile as fallback.

## Design system

- Background: `#F2EEFF` (light lilac)
- Ink (text, borders, shadows): `#16131F`
- Accent 1 (lime): `#C6F432`
- Accent 2 (orange): `#FF6B35`
- Extras: tile lilac `#B9A8FF`, pink `#FF9EB5` (cheeks, flower petals), sky `#7DE2FF` (sweat drops, tears), white `#FFFFFF`
- Bloomy extras: pale leaf `#E4F7A8`, pale petal `#F7D3DC`, dry petal `#D8B4A0`, brown `#A7783C`, soil `#8A5A2B`
- Fonts: **Bricolage Grotesque** (headings, ExtraBold 800) and **DM Sans** (body 400/500/700), both from Google Fonts. The .ttf files are in `res/font`.
- Style: neo-brutalist. 2dp solid ink borders on cards and buttons, hard offset shadows (3–5dp right and down, no blur), rounded corners 14–28dp, pill-shaped chips.
- Icons: simple stroke icons (2dp). No emoji in the UI.
- Touch targets at least 44dp. Text on orange or lime is ink, never white.
- Borders and shadows are reusable components (`NeoCard`, `NeoShadowSurface`, ...). Light/dark mode: use `Neo.colors.*`, never hard-coded White/Ink, except Ink for text on bright brand colours.
- If `design/` contains screen images, a PDF or HTML, match them closely (colors, spacing, borders, shadows).

## Mascot themes

The user picks a theme in Settings (Blobby is the default). Both themes use **the same mood logic and data**; only the artwork and mood names change. A theme is just a drawing function plus names (`ui/mascot/`), so adding a third theme later means only new artwork.

### Mood (both themes)

Based on the share of today's total limit used (summed across enabled limited apps). Resets at midnight.

| Limit used | Level | Blobby | Bloomy |
|---|---|---|---|
| 0–25% | 1 | Thriving: arms up, big grin, sparkles | Thriving: stems tall, flowers wide open, sparkles |
| 25–50% | 2 | Happy: smiling, rosy cheeks | Happy: upright, flowers open |
| 50–80% | 3 | Okay: a bit flat, straight mouth | Okay: slight lean, flowers tilting |
| 80–100% | 4 | Worried: raised brows, sweat drops | Thirsty: stems bending, pale drooping flowers and leaves, sweat drop |
| Over limit | 5 | Melting: slumped into a puddle, tear | Wilting: dry flowers hanging down, fallen petals, tear |

- "Over limit" = total used ≥ total limit. If any single app has reached its own limit, the mood is at least level 4 (decided 2026-10-26).
- **Reels mood (2026-11-12, user feedback):** the places that are about Reels & Shorts (the floating count badge, the reel count widget) react to reels only, everywhere else to time only. Same steps as the table, on today's count of the daily reels number (`MoodLevel.forReels`); without a daily number the mascot stays calm (happy, "I'm counting!"); Instagram Reels or YouTube Shorts at its own minute limit = at least level 4. The badge redraws as soon as that mood changes. Home's mascot stays time-only; its "46 reels" pill turns orange from 80% of the daily number. Streaks and Bloomy's flowers stay time-only.
- Bloomy's face is on the pot. Bloomy's mood art always shows the plant at its current growth stage (number of flowers), reacting with that mood.
- The Pause screen always shows level 4 (worried / thirsty).

### Growth (Bloomy)

- A **day counts** if the user stays under their limits that day (same rule as streaks, from `daily_summary`).
- Every **full week (7 counted days in a row)** adds **one flower**, up to **4 flowers**.
- Stages: Week 0 = sprout with two leaves, Week 1 = 1 flower, Week 2 = 2 flowers, Week 3 = 3 flowers, Week 4 = full pot (4 flowers).
- **During the week the plant visibly grows** (developer, 2026-10-30): each counted day toward the next flower (`daysTowardNext`, 1-6) adds a new stem with a bud that gets taller and bigger, and on day 7 it opens into the flower. Shown everywhere Bloomy is drawn (app, widgets, reminders) via `MascotLook.budDays`. Home's Bloomy card shows "Next flower" with 7 dots ("5 of 7 days"). A full pot has no bud.
- When a pot reaches 4 flowers, it is **saved to the user's garden** (a collection of completed pots, stored in Room) and a **new seed** pot starts at Week 0.
- **Missing a day** (a recorded day over the limit): the newest flower falls off (drop one stage) and the 7-day count restarts. A day with no data only restarts the count. The pot is never lost entirely, and saved pots in the garden are permanent ("Clear history" keeps them).
- Growth is tracked for every user, whichever theme is chosen, so switching to Bloomy shows a plant that matches their history.
- **Morning flower note** (developer, 2026-11-13): besides the Home pop-up, Bloomy sends "Bloomy: I grew a new flower!" (or "my pot is full!") the morning after, at 9:00-9:10 am; with Parent lock on only between 8:00 and 8:30 am (before school; missed = next morning). Not after 9 pm, and not at all if the pop-up was already seen. An inexact alarm, no exact-alarm permission (`service/BloomNotice.kt`, timing in `data/BloomNote.kt`, tested).
- Blobby has no growth stages in v1 (it only uses moods). The streak is tracked the same way for both themes.

## Screens

### 1. Home
- Header: "Hey [name]" + "scrollnah" wordmark, light/dark button (Settings is a tab, not a header button).
- **Order (UX pass, 2026-11-06):** mascot card, the setup card (if any), a running Focus, **Today's limits**, one slim suggestion, streak, fun stat, the one-row Focus. What people open Home for (what's left) comes first; suggestions never outrank it.
- Lime mascot card (turns orange when over the limit): mascot in today's mood, signed mood pill ("Blobby · thriving") with today's reel count beside it (a white "46 reels" pill, when the tracker counts), headline like "Only 38 min of scrolling today!", short encouraging line, and a bar with "49 min left today" / "12 min over today" (all limited apps together).
- "Today's limits" list: each app with a letter tile, name, "used / limit min", and a progress bar (orange when close to the limit). "Edit" opens Limits.
- Slim suggestion strips (`SuggestionStrip`: small picture, title, one line, "Not now" + action), one at a time: "Count your Reels & Shorts" (lilac, a mini count badge; after setup while the tracker is off and Instagram or YouTube is installed; "Show me" opens Limits on the Reels & Shorts tab, where the disclosure is; "Not now" or turning the tracker on hides it for good), then "Put [mascot] on your home screen". Home's buttons open Limits like the tab bar does (`openTab` in `UnscrollApp`).
- Fun stat card: "46 min of scrolling today" + today's minutes turned into something silly (songs, sitcom episodes; scroll distance can't be measured).
- Focus: one row ("Focus" + 30 min / 1 hour / 2 hours) at the end; while focus runs, a full lime card near the top with the countdown.
- Bottom nav: Home, Limits, Stats, Settings (decided 2026-10-30: the Pause tab was only a preview, so Settings took its place).

### 2. Pause screen (shown when a limited app is over its limit and the strictness mode is Firm or Beast)
- Full orange background. Pill: "3rd time today".
- Mascot looks worried (level 4). Title "Woah. [App] again?" and "Take one slow breath with me before you decide." (the mascot is speaking; name tag under the mascot).
- Breathing countdown circle (3, 2, 1).
- "Why are you opening it?" chips: Bored, Replying to someone, Pure habit, Work thing (log the choice).
- Buttons: "Nah, I'm good" (lime, primary: leaves the app and goes to the phone's home screen) and "Open for 5 minutes" (outline). Beast: locked, only "Take me home".

### 3. Limits ("Your time-sinks")
- Back button, title, short description.
- List of apps: letter tile, name, minus/plus stepper (5 min steps, 5–180), on/off switch.
- "How strict should I be?" (3 cards): Chill = gentle nudge notification, Firm = pause screen, Beast = hard lock until midnight.
- "See the Pause screen" link under the strictness cards (Firm and Beast): a full-screen preview of the Pause screen. **The preview never saves anything** (no Pause answer, so Stats don't change) and doesn't open the app; its buttons just show "Just a preview. Nothing was saved." and go back.
- "Break reminders" row ("After 20 min on one app") that opens screen 7.
- **Two tabs** (2026-11-06, developer's idea) when Instagram or YouTube is installed (and the version has the tracker): **Apps** (everything above, with bedtime, work hours, strictness and break reminders) and **Reels & Shorts** (the tracker's disclosure card until it's on, then "So far today: N Reels & Shorts"; "Reels & Shorts a day"; the Reels/Shorts minute limits; a line with the current strictness and "Change on the Apps tab"). The tab bar sticks to the top while scrolling. No tabs without those apps.
- "Lock it in": an orange bar under the list that slides in only while something is unsaved (one save for both tabs). Coming back to the app with unsaved changes asks "Lock it in first?".

### 4. Weekly report ("Your week")
- Total social time this week + badge comparing with last week ("1h 10m less").
- 7-day bar chart with a dashed daily-limit line; bars over the limit are orange, today is ink. Tapping a bar shows that day's apps.
- "Where the time went": each limited app (real icon), minutes, visits, minutes per visit, change vs last week.
- "Reels & Shorts" (once the tracker has counted a video; a week with 0 shows only while the tracker is on): videos in the last 7 days, change vs last week (only if the tracker was counting when last week began), a bar per day (orange over the daily reels limit), about N a day (over the days since counting started), about N s each, per feature, time watching. `WeekStats.reelWeek` (tested), from `feature_usage.views`.
- The mascot's verdict: up to three first-person lines picked on the phone from the week's numbers, reels included (`MascotLines.weekVerdict`, no AI).
- "Your week in review": swipeable cards (total, top app, "My reels" when there are reel counts, best/toughest day, scroll o'clock, verdict), each shareable as a picture through Android's share menu (FileProvider, cache only).
- Two cards: streak ("5 days under your limit in a row") and time won back ("1 movie").
- "Your scroll o'clock": minutes per hour, busiest hour, first scroll of the day (after 4 AM).
- "Did the nudges work?": Pause screen "Nah, I'm good" share (ring) and break reminders taken vs shown.
- "Why you reached for it" (Pause screen reasons), a month calendar (lime under / orange over, Bloomy's new-flower days when Bloomy is the mascot), and a 4-week trend of the daily average.
- Friend challenge card ("You vs [friend]") — a "coming soon" placeholder in v1.
- Data: Android keeps detailed usage events only ~7-10 days, so ScrollNah saves its own per-day summaries (`day_app_stats`: minutes, opens, first use per app; `day_hours`: minutes per hour) for finished days, next to `daily_summary`. Logic in `data/WeekStats.kt` (tested), loading in `data/WeekReportLoader.kt`.

### 5. Theme picker (in Settings)
- Choose Blobby or Bloomy, with a preview of the mascot. For Bloomy, also show the current pot and a "Garden" entry that lists saved pots.

### 6. Home-screen widgets (RemoteViews)
- Follows the chosen theme and shows the current mood art.
- **Medium (4×2):** mascot, mood pill (e.g. "Thirsty"), title ("Bloomy needs a break"), the most-used limited app with a "26 / 30 min" bar, and streak text ("10-day streak, 1 flower" for Bloomy, "10-day streak" for Blobby).
- **Small (2×2):** mascot + "4 min left" + "on Instagram today".
- **Reel count (2×2, needs the tracker, added 2026-11-06):** big "12" + "REELS TODAY", a line from the mascot ("I'm counting!", "Nearly there. I'm dizzy.", "That's all for today!") and the mascot in the **reels mood** (not the time mood). The tracker redraws it while Reels/Shorts are open (at most every 3 s, `MascotWidgets.updateReelCount`).
- **Mascot (animated, 2×2):** no background and no text: just the mascot in today's mood, standing on the wallpaper, bobbing (Blobby) or swaying (Bloomy) and blinking. Widgets can't run real animations: 4 pre-drawn frames in a `ViewFlipper` (`widget_mascot_frames.xml`) that the launcher flips every 260 ms. With "Remove animations" on, it shows a still frame. (Added 2026-10-26 at the developer's request.)
- Tapping opens ScrollNah.
- **At night** (the bedtime schedule if set, else 10 pm to 6 am; `WidgetData.isNight`) the mascot sleeps on every widget: closed eyes, calm smile, "z z" (`MascotPose.asleep`, `sleepyZs`), with night lines ("Sleep well. See you tomorrow!", "Zzz. Sleep well!").
- Widgets don't refresh constantly on their own: the foreground service triggers a widget update whenever the mood level or numbers change (and at midnight).

### 7. Break reminder settings ("Break reminders")
- Main switch: "Remind me to take breaks".
- "Remind me after": 10, 15, 20, 30 min or Custom (default 20). Applies to all limited apps.
- "Different time for some apps": per-app overrides (e.g. Instagram 10 min, YouTube 30 min) + "Add an app".
- "How should I remind you?": Notification, Bubble (mascot overlay), or Both (default Both).
- "Keep reminding me": repeat every 5 min after the first reminder, with a sadder mascot each time (default on).

### 8. Reminder while scrolling
- **Notification:** small ScrollNah icon, the mascot in a worried/thirsty mood as the large icon, title "Bloomy: 20 minutes of Instagram straight", text "I'm getting thirsty. Take a break?" (signed title + first person, see Working notes). Action buttons: "Take a break" (sends the user to the phone's home screen) and "5 more min" (snoozes 5 min).
- **Bubble:** the mascot in a round lime badge in the bottom-right corner with a speech bubble ("20 min straight! My eyes hurt."). Shows for a few seconds, tap to dismiss or open ScrollNah.

## Continuous-session rules (break reminders)

- A session is time spent continuously on the **same app** (enabled limited apps only).
- If the user leaves the app for **less than 2 minutes** and comes back, the session continues. Leaving for 2 minutes or more resets it.
- When the session reaches the reminder time (per-app override, else the global time), show the reminder in the chosen style.
- If "Keep reminding me" is on, repeat every 5 minutes while the session continues, each time with the next sadder mascot mood (1st reminder = level 4, then level 5).
- "5 more min" snoozes the next reminder by 5 minutes.
- Log reminders and the user's choice (break vs snooze) for the weekly report later.

## Also built (beyond this spec — keep them)

Onboarding (starts with "Pick your buddy"; its last step and Home's "Almost ready" card ask for one switch at a time, only the ones this phone and the user's choices need, and return to ScrollNah by themselves: `data/SetupPlan.kt`, `service/SetupReturn.kt`), Settings (name, light/dark, permissions, feedback via GitHub Discussions, About), streaks + milestone celebrations + evening streak nudge, Bloomy's "New flower!" / "Full pot!" celebration (these and the streak milestone pop-ups burst confetti from the mascot, pink petals for Bloomy's flowers and a bigger burst for a full pot: `ui/components/Celebration.kt`, drawn on a Canvas, no library, once for about 3 s, none with "Remove animations"; 2026-11-13), focus mode (also a Quick Settings tile) and bedtime schedule, weekday/weekend limits, Reels/Shorts and website limits (opt-in Accessibility exception above), "Why you reached for it" stats card, backup & restore to a JSON file the user picks (`data/Backup.kt`, nothing uploaded), a "X min left" mascot bubble when a limited app opens, a Beast-mode cool-down before loosening anything (`data/BeastGuard.kt`; restoring a backup counts too), **Parent lock** (optional, off by default: a PIN or pattern protects Limits, Settings, extra minutes, ending focus early and turning the lock off; a report of tampering; uninstall protection as a device admin app with no policies; see `data/ParentLock.kt`, `data/TamperLog.kt`, `service/TamperWatch.kt`, `ui/components/ParentLockUi.kt`), daily open limits per app, shared budgets (`app_groups`: a few apps with one limit together; each app keeps its own), work hours (like bedtime, on chosen weekdays), "Do this instead" ideas on the Pause screen (editable in Settings), a Sunday-evening "week in review is ready" note, streak rewards (hats for Blobby, pot colours for Bloomy at 3/7/14/30-day best streaks; `data/Rewards.kt`, `ui/mascot/RewardArt.kt`), an "Add widget" button (Settings > Mascot, and a one-time Home card after setup until a widget is placed; Android's `requestPinAppWidget`, manual steps where the home screen can't), a **daily reels limit** ("Reels & Shorts a day" in Limits, needs the tracker; a FeatureLimit row with id `ReelLimit.ID`, the count in limitMinutes; Pause screen reason `PauseReason.REELS`; badge/Home/widget show "32/50"), a **slow-down plan** (Limits card and a setup switch: every 7 days each app limit drops 5 or 10 min until the goal; `data/SlowDownPlan.kt` tested, applied in `BlockerService.planWork`, mascot note), a **setup reality check** (onboarding step 5: "How long do you scroll on a normal day?" -> "About 46 days a year", offers the plan), a **setup reels number** (onboarding step 6, only with Instagram or YouTube installed: "How many Reels & Shorts a day is enough?" 30/50/100/200/300 or +/-, with "About 25 min of videos a day", "36,500 a year", "stacked up ... 15 m tall, about a 5-storey building" (`ReelLimit.compare`, tested); it turns on "Reels & Shorts a day", and the last step asks for the tracker with its disclosure; setup's pages are a list, `SetupPage`, and its answers are `rememberSaveable`, so rotating keeps them), **Settings hints** (a tiny picture-in-picture window, `HintActivity`, over Android's Settings pages with one plain line; overlay windows are hidden on sensitive Settings pages since Android 12, PiP isn't; needs "Display over other apps"), the time-left/break **bubble steps aside when a swipe starts on it**, **mascot sounds** (`service/SoundFx.kt`; the developer's AI-made creature sounds in `design/sounds/` (dry, from ElevenLabs-style prompts like "a tiny forest sprite lets out a soft, breathy, droopy sigh-chirp going down, sad but sweet"), turned into `res/raw/mascot_*.ogg` by `tools/prepare_sounds.py` (trim, even volume, the same soft echo, mono Ogg); both mascots share one set for now; tap, happy, "uh-oh" on the Pause screen, nudge; never over other apps, quiet on silent/vibrate; notifications use one channel per mascot, e.g. "Limit nudges (Bloomy)", since a channel's sound is fixed (new notification sound = raise `SoundFx.SOUND_SET`, which makes new channels); Settings > Mascot > Sounds, off = Android's own notification sound), debug-only demo mode for screenshots. Details in `CLAUDE_UPDATES.md`.

## Build order

1. Project setup, theme (colors, fonts, border/shadow modifiers), navigation between screens with fake data.
2. Usage access permission flow + real daily usage per app on Home.
3. Limits screen saving to Room.
4. Foreground service that detects a limited app opening and shows the Pause screen.
5. Mood levels from today's usage; Blobby mood art on Home and Pause.
6. Break reminders: session tracking, settings screen, notification with actions, then the mascot bubble.
7. Weekly report from real data + streak tracking.
8. Bloomy theme: mood art, weekly flower growth, garden of saved pots, theme picker.
9. Home-screen widgets (both themes; Glance at first, now plain RemoteViews).
10. Later: friend challenges, AI weekly insights via an LLM API. **The first build is fully on-device:** anything that needs a server (AI insights, friend challenges) waits until after v1. Never ship an API key in the APK.

## Working notes

- **ScrollNah vs the mascot:** ScrollNah is the app (name, settings, permissions, About). The mascot (Blobby or Bloomy, whichever theme is chosen) is the companion character who watches the user's scrolling and alerts them. Wherever the mascot talks (Home mood card, Pause screen, notifications, widget, bubble, celebrations), write in its first person ("I'm watching", "breathe with me") and sign it ("Blobby · thriving" pill, name tag, notification titles starting "Blobby:" / "Bloomy:"). Elsewhere, refer to the mascot in the third person. Never hard-code "Blobby" where the chosen mascot speaks; use the current theme's name.

- **Permission/setup copy must be calm and plain** (developer feedback): short, factual, no hype like "superpowers", "peek", "Blobby needs switches". Say what it is for in a few words ("For reminders", "To show the pause screen") and that data stays on the phone. The mascot can be playful elsewhere, not when asking for access.

- Progress, decisions and test notes live in `CLAUDE_UPDATES.md`. Read it first when resuming.

- **Every screen must work on small phones** (developer feedback: buttons and text were hidden, tabs sat under the navigation buttons). Check at 360x640dp with 3-button navigation and font scale 1.3 (`adb shell wm size 720x1280; wm density 320; settings put system font_scale 1.3`, then reset). Rules: the bottom nav pads for the navigation bar; content that can be long scrolls above fixed buttons; no fixed heights/widths on things with text (use `heightIn`/`widthIn`); chip rows use `FlowRow`; text fields need `imePadding`.

- **Rotation:** never lock the orientation; the app follows the phone's Auto-rotate setting (developer, 2026-10-27). Screens must work sideways: tab screens are wrapped in `ReadableWidth` (max 640dp, centred, in `UnscrollApp`), onboarding is width-limited, and the Pause screen has a two-column landscape layout.

- **Battery:** no work while the screen is off or there's nothing to watch. Background loops wait for screen on (see `BlockerService.awaitWork`), and polling only runs while it's needed. Measure CPU time before/after (`/proc/<pid>/stat`) when adding background work.

- Explain changes briefly; the developer is learning, so prefer clear, simple code with short comments.
- Build with `./gradlew assembleGithubDebug` (or `assemblePlayDebug`) and fix errors before moving on. Unit tests: `./gradlew testGithubDebugUnitTest`.
- **Two app versions (product flavors, decided 2026-10-26):** `github` = everything, including the opt-in Accessibility tracker (GitHub releases: `assembleGithubRelease`); `play` = Google Play (`bundlePlayRelease`); until 2026-11-06 it had no tracker, now it has the same features (`TRACKER_AVAILABLE` true in both; to take the tracker out of Play again, set it false for `play` and put the removals back in `src/play/AndroidManifest.xml`, as its comment says). Same applicationId, but Play re-signs its installs with a Google-generated app signing key (SHA-256 `40:0F:DE:87...`, checked 2026-11-03), so GitHub APKs (our key `AC:58:24...`) and Play installs can't update each other: switching means uninstalling first. Any new tracker/Reels/Shorts UI must check `FeatureWatchService.available`.
- Targets Android 16 (API 36), AGP 8.13.2, Gradle 8.13 (Play requires API 36 since 31 Aug 2026).
- Play Store kit (listing text, form answers, icon, feature graphic, screenshots, FGS demo video): `store/` (see `store/README.md`). Privacy policy: `docs/privacy-policy.md`.
