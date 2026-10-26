# ScrollNah (formerly "Unscroll") — Android digital wellbeing app

**ScrollNah** (final name, chosen 2026-10-11; "Unscroll" was too crowded on the stores) helps people cut down excessive social media use. It should feel **funky, playful and easy to use**, never strict or preachy. A mascot reacts to the user's usage. There are two mascot themes: **Blobby** (default, an orange blob) and **Bloomy** (a plant in a pot that grows flowers).

Always write the name as **ScrollNah** (capital N) in the UI and docs. The lowercase "scrollnah" wordmark on Home is the only exception. Internal package/class names stay `com.unscroll.app`, `UnscrollApplication` etc. (users never see them, and the package can't change after publishing).

## Tech stack

- Android only for v1, native **Kotlin + Jetpack Compose**, Material 3 as a base but heavily restyled.
- minSdk 26, targetSdk latest stable.
- Usage data: `UsageStatsManager` / `UsageEvents` (needs `PACKAGE_USAGE_STATS`, granted via Settings > Usage access).
- Detecting launches and session length: one **foreground service** (`BlockerService`) that polls usage events about every 1 s and knows which app is in front.
- Pause screen: a full-screen Activity launched over the app (needs `SYSTEM_ALERT_WINDOW` to open from the background).
- Mascot bubble: an overlay window (`SYSTEM_ALERT_WINDOW`).
- Notifications: `NotificationCompat` with action buttons. Ask for `POST_NOTIFICATIONS` on Android 13+.
- Home-screen widget: **Jetpack Glance**.
- **Do NOT use AccessibilityService** for blocking. Google Play restricts it for non-accessibility apps.
  - **Exception (decided 2026-10-11):** an opt-in, off-by-default `FeatureWatchService` is used ONLY to detect Instagram Reels / YouTube Shorts and, at the developer's request, the websites of limited apps in browsers (reads only the browser address bar). Both are impossible with UsageStats. It must stay opt-in, show a prominent disclosure before sending the user to Settings, only inspect those apps and the address bar, and never store screen content or addresses (minutes only). Expect extra Play Store review/declaration for this. Break reminders and moods must NOT depend on it.
  - UPI/bank apps (BHIM, PhonePe, GPay, Paytm, bank apps; list in `data/PaymentApps.kt`) often refuse to open while any app's Accessibility service is on and ask the user to close/uninstall it. Never try to hide from or get around that check. Instead: say so in the tracker's disclosure and in Settings, show a once-a-day notification with a link to Accessibility settings when a payment app opens while the tracker is on, and never draw the bubble over payment apps.
- Local storage: Room (usage logs, limits, streak days, break reminder log, saved pots) + DataStore (settings, chosen theme, break reminder settings, current pot progress).
- Mascot art: drawn with Compose Canvas shapes (one drawing function per theme, also rendered to bitmaps for the widget, bubble and notifications). Lottie animations later.

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
- Bloomy's face is on the pot. Bloomy's mood art always shows the plant at its current growth stage (number of flowers), reacting with that mood.
- The Pause screen always shows level 4 (worried / thirsty).

### Growth (Bloomy)

- A **day counts** if the user stays under their limits that day (same rule as streaks, from `daily_summary`).
- Every **full week (7 counted days in a row)** adds **one flower**, up to **4 flowers**.
- Stages: Week 0 = sprout with two leaves, Week 1 = 1 flower, Week 2 = 2 flowers, Week 3 = 3 flowers, Week 4 = full pot (4 flowers).
- When a pot reaches 4 flowers, it is **saved to the user's garden** (a collection of completed pots, stored in Room) and a **new seed** pot starts at Week 0.
- **Missing a day** (a recorded day over the limit): the newest flower falls off (drop one stage) and the 7-day count restarts. A day with no data only restarts the count. The pot is never lost entirely, and saved pots in the garden are permanent ("Clear history" keeps them).
- Growth is tracked for every user, whichever theme is chosen, so switching to Bloomy shows a plant that matches their history.
- Blobby has no growth stages in v1 (it only uses moods). The streak is tracked the same way for both themes.

## Screens

### 1. Home
- Header: "Hey [name]" + "scrollnah" wordmark, light/dark button, settings button.
- Lime mascot card (turns orange when over the limit): mascot in today's mood, signed mood pill ("Blobby · thriving"), headline like "Only 38 min of scrolling today!", short encouraging line.
- Fun stat card: today's minutes turned into something silly (songs, sitcom episodes; scroll distance can't be measured).
- "Today's limits" list: each app with a letter tile, name, "used / limit min", and a progress bar (orange when close to the limit). "Edit" opens Limits.
- Bottom nav: Home, Limits, Stats, Pause.

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
- "Break reminders" row ("After 20 min on one app") that opens screen 7.
- "Lock it in" orange button saves.

### 4. Weekly report ("Your week")
- Total social time this week + badge comparing with last week ("1h 10m less").
- 7-day bar chart with a dashed daily-limit line; bars over the limit are orange, today is ink.
- Two cards: streak ("5 days under your limit in a row") and time won back ("1 movie").
- Friend challenge card ("You vs [friend]") — a "coming soon" placeholder in v1.

### 5. Theme picker (in Settings)
- Choose Blobby or Bloomy, with a preview of the mascot. For Bloomy, also show the current pot and a "Garden" entry that lists saved pots.

### 6. Home-screen widget (Jetpack Glance)
- Follows the chosen theme and shows the current mood art.
- **Medium (4×2):** mascot, mood pill (e.g. "Thirsty"), title ("Bloomy needs a break"), the most-used limited app with a "26 / 30 min" bar, and streak text ("10-day streak, 1 flower" for Bloomy, "10-day streak" for Blobby).
- **Small (2×2):** mascot + "4 min left" + "on Instagram today".
- **Mascot (animated, 2×2):** no background and no text: just the mascot in today's mood, standing on the wallpaper, bobbing (Blobby) or swaying (Bloomy) and blinking. Widgets can't run real animations: 4 pre-drawn frames in a `ViewFlipper` (`widget_mascot_frames.xml`) that the launcher flips every 260 ms. With "Remove animations" on, it shows a still frame. (Added 2026-10-26 at the developer's request.)
- Tapping opens ScrollNah.
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

Onboarding, Settings (name, light/dark, permissions, feedback via GitHub Discussions, About), streaks + milestone celebrations + evening streak nudge, focus mode and bedtime schedule, Reels/Shorts and website limits (opt-in Accessibility exception above), "Why you reached for it" stats card, debug-only demo mode for screenshots. Details in `CLAUDE_UPDATES.md`.

## Build order

1. Project setup, theme (colors, fonts, border/shadow modifiers), navigation between screens with fake data.
2. Usage access permission flow + real daily usage per app on Home.
3. Limits screen saving to Room.
4. Foreground service that detects a limited app opening and shows the Pause screen.
5. Mood levels from today's usage; Blobby mood art on Home and Pause.
6. Break reminders: session tracking, settings screen, notification with actions, then the mascot bubble.
7. Weekly report from real data + streak tracking.
8. Bloomy theme: mood art, weekly flower growth, garden of saved pots, theme picker.
9. Home-screen widget with Glance (both themes).
10. Later: friend challenges, AI weekly insights via an LLM API, Lottie animations. **The first build is fully on-device:** anything that needs a server (AI insights, friend challenges) waits until after v1. Never ship an API key in the APK.

## Working notes

- **ScrollNah vs the mascot:** ScrollNah is the app (name, settings, permissions, About). The mascot (Blobby or Bloomy, whichever theme is chosen) is the companion character who watches the user's scrolling and alerts them. Wherever the mascot talks (Home mood card, Pause screen, notifications, widget, bubble, celebrations), write in its first person ("I'm watching", "breathe with me") and sign it ("Blobby · thriving" pill, name tag, notification titles starting "Blobby:" / "Bloomy:"). Elsewhere, refer to the mascot in the third person. Never hard-code "Blobby" where the chosen mascot speaks; use the current theme's name.

- **Permission/setup copy must be calm and plain** (developer feedback): short, factual, no hype like "superpowers", "peek", "Blobby needs switches". Say what it is for in a few words ("For reminders", "To show the pause screen") and that data stays on the phone. The mascot can be playful elsewhere, not when asking for access.

- Progress, decisions and test notes live in `CLAUDE_UPDATES.md`. Read it first when resuming.

- **Every screen must work on small phones** (developer feedback: buttons and text were hidden, tabs sat under the navigation buttons). Check at 360x640dp with 3-button navigation and font scale 1.3 (`adb shell wm size 720x1280; wm density 320; settings put system font_scale 1.3`, then reset). Rules: the bottom nav pads for the navigation bar; content that can be long scrolls above fixed buttons; no fixed heights/widths on things with text (use `heightIn`/`widthIn`); chip rows use `FlowRow`; text fields need `imePadding`.

- Explain changes briefly; the developer is learning, so prefer clear, simple code with short comments.
- Build with `./gradlew assembleDebug` and fix errors before moving on. Unit tests: `./gradlew testDebugUnitTest`.
