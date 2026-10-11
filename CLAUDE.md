# ScrollNah (formerly "Unscroll") — Android digital wellbeing app

**ScrollNah** (final name, chosen 2026-10-11; "Unscroll" was too crowded on the stores) helps people cut down excessive social media use. It should feel **funky, playful and easy to use**, not strict or preachy. A mascot called **Blobby** reacts to the user's usage.

## Tech stack

- Android only for v1, native **Kotlin + Jetpack Compose**, Material 3 as a base but heavily restyled.
- minSdk 26, targetSdk latest stable.
- Usage data: `UsageStatsManager` / `UsageEvents` (needs `PACKAGE_USAGE_STATS`, granted via Settings > Usage access).
- Detecting launches: a **foreground service** that polls usage events every ~1 s for the blocked packages.
- Pause screen: a full-screen Activity launched over the app (or an overlay with `SYSTEM_ALERT_WINDOW`).
- **Do NOT use AccessibilityService** for whole-app blocking. Google Play restricts it for non-accessibility apps.
  - **Exception (decided 2026-10-11):** an opt-in, off-by-default `FeatureWatchService` is used ONLY to detect Instagram Reels / YouTube Shorts and, at the developer's request, the websites of limited apps in browsers (reads only the browser address bar). Both are impossible with UsageStats. It must stay opt-in, show a prominent disclosure before sending the user to Settings, only inspect those apps and the address bar, and never store screen content or addresses (minutes only). Expect extra Play Store review/declaration for this.
- Local storage: Room (usage logs, limits) + DataStore (settings).
- Animations: Lottie for Blobby later; simple Compose shapes are fine for v1.

## Design system

- Background: `#F2EEFF` (light lilac)
- Ink (text, borders, shadows): `#16131F`
- Accent 1 (lime): `#C6F432`
- Accent 2 (orange): `#FF6B35`
- Extras: tile lilac `#B9A8FF`, cheek pink `#FF9EB5`, white `#FFFFFF`
- Fonts: **Bricolage Grotesque** (headings, ExtraBold 800) and **DM Sans** (body 400/500/700), both from Google Fonts.
- Style: neo-brutalist. 2dp solid ink borders on cards and buttons, hard offset shadows (4–5dp right and down, no blur), rounded corners 14–28dp, pill-shaped chips.
- Icons: simple stroke icons (2dp). No emoji in the UI.
- Touch targets at least 44dp. Text on orange or lime is ink, never white.

## Screens (v1)

### 1. Home
- Header: "Hey [name]" + "unscroll" wordmark, settings button.
- Lime Blobby card: Blobby (orange blob, happy face), mood pill ("Mood: vibing"), headline like "Only 38 min of scrolling today!", short encouraging line.
- Fun stat card: scroll distance turned into something silly ("86 metres, nearly three blue whales").
- "Today's limits" list: each app with a letter tile, name, "used / limit min", and a progress bar (orange when close to limit).
- Bottom nav: Home, Limits, Stats, Pause.

### 2. Pause screen (shown when a limited app opens)
- Full orange background. Pill: "3rd time today".
- Blobby looks worried. Title "Woah. [App] again?" and "Take one slow breath with Blobby before you decide."
- Breathing countdown circle (3, 2, 1).
- "Why are you opening it?" chips: Bored, Replying to someone, Pure habit, Work thing (log the choice).
- Buttons: "Nah, I'm good" (lime, primary, closes the app and goes home) and "Open for 5 minutes" (outline).

### 3. Limits
- Back button, title "Your time-sinks".
- List of apps: letter tile, name, minus/plus stepper (5 min steps, 5–180), on/off switch.
- Strictness picker (3 cards): Chill = gentle nudge notification, Firm = pause screen, Beast = hard lock until tomorrow.
- "Lock it in" orange button saves.

### 4. Weekly report
- Total social time this week + "1h 10m less" badge vs last week.
- 7-day bar chart with a dashed 1-hour limit line; bars over the limit are orange.
- Two cards: streak ("5 days under your limit") and time won back ("1 movie").
- Friend challenge card ("You vs [friend]") — can be a placeholder in v1.

## Build order

1. Project setup, theme (colors, fonts, border/shadow styles), navigation between the 4 screens with fake data.
2. Usage access permission flow + real daily usage per app on Home.
3. Limits screen saving to Room.
4. Foreground service that detects a limited app opening and shows the Pause screen.
5. Blobby moods based on today's usage, weekly report from real data.
6. Later: streaks, friend challenges, AI weekly insights via an LLM API. **First build is fully on-device:** anything that needs a server (AI insights, friend challenges) waits until after v1. Never ship an API key in the APK.

## Working notes

- **ScrollNah vs Blobby:** ScrollNah is the app (name, settings, permissions, About). Blobby is the companion character who watches the user's scrolling and alerts them. Wherever Blobby talks (Home mood card, Pause screen, notifications, widget, celebrations), write in Blobby's first person ("I'm watching", "breathe with me") and sign it ("Blobby · vibing" pill, "Blobby" name tag, notification titles starting "Blobby:"). Elsewhere, refer to Blobby in the third person.

- **Permission/setup copy must be calm and plain** (developer feedback): short, factual, no hype like "superpowers", "peek", "Blobby needs switches". Say what it is for in a few words ("For reminders", "To show the pause screen") and that data stays on the phone. Blobby can be playful elsewhere, not when asking for access.

- Progress, decisions and test notes live in `CLAUDE_UPDATES.md`. Read it first when resuming.

- Explain changes briefly; the developer is learning, so prefer clear, simple code with short comments.
- Build with `./gradlew assembleDebug` and fix errors before moving on.