# ScrollNah for browsers: store listing

The same text works for the Chrome Web Store, Microsoft Edge Add-ons and Firefox Add-ons (AMO).
Pictures: `screenshot-1..5.png` (1280x800) and `promo-440x280.png` (Chrome's small promo tile), made by
`tools/store-shots.mjs`. The icon is in the package (`src/icons/icon-128.png`).

## Name (22)
ScrollNah: Scroll Less

## Summary (Chrome: max 132, Firefox: max 250)
Daily limits for YouTube, Instagram and other sites, with a cute mascot that reacts. Everything stays in your browser.

## Category
Chrome: Productivity > Workflow & Planning (or "Well-being" if offered). Edge: Productivity. Firefox: Productivity, Social & Communication.

## Description
```
Spending more time on YouTube, Instagram or Reddit than you meant to? ScrollNah helps you cut back, without guilt trips. Give each site a daily budget, and a friendly mascot keeps you company in the toolbar.

MEET YOUR MASCOT
Choose Blobby, an orange blob, or Bloomy, a little plant in a pot. Both react to your day: thriving when you're doing great, worried as you get close to your limit, and melting (or wilting) when you go over. The toolbar badge shows the minutes left on the site you're on.

DAILY LIMITS THAT ACTUALLY STOP YOU
Set a budget for any site, from 5 minutes to 5 hours, with a different one for weekends if you like. Pick how strict ScrollNah should be:
• Chill: a gentle nudge when time's up
• Firm: the Pause page. One slow breath, "why are you opening it?", then "Nah, I'm good" or 5 more minutes
• Beast: the site is locked until midnight. No way around it

MORE WAYS TO SCROLL LESS
• Visits a day: say how many times you may open a site
• Shared budgets: Instagram + Reddit, one hour together
• Reels & Shorts limits: their own minutes, normal videos still play
• Bedtime and work hours: your sites pause, whatever time is left
• Hide the endless parts: switch off YouTube Shorts and the home feed, Instagram Reels and Explore, or X's "For you"
• Beast mode can't be loosened in a weak moment: giving yourself more time waits 10 minutes first

BRING YOUR PHONE'S LIMITS
Use the ScrollNah app on Android? Copy your limits to your computer in one small file (Settings > Backup > Copy limits to computer) and drop it in. No account needed.

FAIR AND PRIVATE
• Only the tab you're looking at counts. A tab in the background, or the computer while you're away, doesn't.
• ScrollNah asks for access to each site you add, and nothing else. No "read all your data" warning.
• No account, no server, no ads, no analytics. Everything stays in your browser, and uninstalling deletes it.

Free, made by one developer. Ideas and feedback: https://scrollnah.vercel.app
```

## Single purpose (Chrome asks for this)
ScrollNah helps people spend less time on distracting websites: it counts the time spent on the sites the user chose, shows a reminder or a pause page when the user's own daily limit is reached, and can hide endless feeds on those sites.

## Permission justifications (Chrome asks one line per permission)
- **storage**: Saves the user's limits and settings, and the minutes spent per site per day, in the browser. Nothing is sent anywhere.
- **alarms**: Checks again at the moment a limit is reached, at bedtime and at midnight (when the day's minutes reset).
- **idle**: Stops counting time when the user is away from the computer, so the minutes are fair.
- **notifications**: In "Chill" mode, the reminder when a site's time is up is a notification.
- **scripting**: Adds ScrollNah's small script to the sites the user limits, to count time while the page is visible and to hide endless feeds (YouTube Shorts, Instagram Reels) when the user switches that on.
- **Host permissions (optional, `*://*/*`)**: The user can limit any website, so access is requested at runtime for each site the user adds, one by one, and only for those sites. Nothing is requested at install.
- **Remote code**: No. All code is in the package.

## Privacy practices (Chrome "Privacy" tab, Firefox data collection)
- Data collected: **none**. (The extension stores settings and per-site minutes locally in the browser; it is never transmitted.)
- Not sold, not used for anything unrelated, not used for credit decisions.
- Firefox: the manifest declares `data_collection_permissions: { required: ["none"] }`.
- Privacy policy URL: https://scrollnah.vercel.app/privacy#extension

## Test notes for reviewers
1. After installing, the setup page opens. Pick a mascot, pick YouTube, keep "Firm", allow access to the site.
2. In the settings (right-click the toolbar icon > Options), set YouTube to 5 minutes.
3. Watch YouTube for 5 minutes: the Pause page appears. "Open for 5 minutes" waits for a 3-second breath.
4. Settings > Hide the endless parts > Hide YouTube Shorts: Shorts shelves disappear, and a Shorts page shows "I hid YouTube Shorts for you."
5. Import from phone: Settings > "Bring your phone's limits" > pick `extension/test/sample-limits.json` from the source.
