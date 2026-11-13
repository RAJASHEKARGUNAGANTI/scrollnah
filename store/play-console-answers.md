# Play Console: draft answers for ScrollNah (Play version)

The Play version is built with `./gradlew bundlePlayRelease`. Since 2026-11-06 (version after 2.4.2) it includes the **optional Reels/Shorts & website tracker**, an Accessibility service, so the **Accessibility API declaration** below must be filled in with the first upload that has it. Steps and video plan updated for 2.5.4 (Limits tabs, Home's reels card, the setup reels step), still right for 2.5.5. Check each answer against the current wording in Play Console before submitting.

## Policy > App content

### Privacy policy
`https://rajashekargunaganti.github.io/scrollnah/privacy-policy`
(Published from `docs/privacy-policy.md` with GitHub Pages.)

### App access
**All functionality is available without special access** (no login).
Optional instructions for reviewers:
> No account needed. To see ScrollNah in action: open ScrollNah, allow Usage access when asked, add an app (for example Chrome) in Limits (the "Apps" tab) with a 5-minute limit, choose "Firm", tap "Lock it in" (the orange bar at the bottom), then use that app for 5 minutes. The Pause screen appears over it. To see it straight away, use Limits > Apps > "See the Pause screen" (a preview that saves nothing). Focus mode on Home (or the "Focus" Quick Settings tile) pauses limited apps at once. Break reminders (Limits > Break reminders) can be set to remind after a few minutes on one app.
>
> Optional Reels/Shorts & website tracker (Accessibility, off by default). Every way to it shows the same in-app disclosure first (what it reads and what it does), with "Agree & open Settings" and "Not now":
> - Home: the "Count your Reels & Shorts" card > "Show me" opens Limits on the "Reels & Shorts" tab, whose "Limit Reels, Shorts & websites" card is the disclosure.
> - Limits > "Reels & Shorts" tab: the same card.
> - First setup: if you pick a daily number on "How many Reels & Shorts a day is enough?", the last setup step ("Count Reels & Shorts") shows the disclosure.
> - Settings > Permissions > "Reels, Shorts & website tracker": the disclosure as a dialog.
>
> "Agree & open Settings" opens Android's Accessibility settings: under Downloaded apps, turn on "ScrollNah Reels & Shorts tracker". With YouTube or Instagram installed, set Limits > Reels & Shorts > "Reels & Shorts a day" to a low number (e.g. 10), tap "Lock it in", then open Shorts or Reels and swipe: a small mascot badge at the top counts the videos ("4/10") and the mascot looks more worried as the count gets close. At the number, the tracker presses Back once and the Pause screen appears. "Not now" declines; nothing is turned on. The badge can be turned off in Settings > Reminders > "Reel count while scrolling".
>
> Note on other apps' media: if an app the user chose to limit keeps playing in a picture-in-picture window after it is blocked (over the limit, or during Focus or Bedtime), ScrollNah pauses that playback by briefly requesting audio focus (the standard Android media API, no special permission) and shows a notification explaining why. It never affects apps the user hasn't limited, and nothing happens in Chill mode.

### Ads
**No, my app does not contain ads.**

### Content rating (IARC questionnaire)
- Category: **All other app types** (utility / productivity).
- Violence, sexuality, language, controlled substances, gambling, horror: **No** to all.
- Users can interact or exchange content: **No**.
- Shares the user's location: **No**.
- Digital purchases: **No**.
- Expected result: Everyone / PEGI 3.

### Target audience and content
- Age groups: **13–15, 16–17, 18 and over** (do not pick any group under 13).
- Appeals to children: **No**.

### News app
**No.**

### Data safety
- Does your app collect or share any of the required user data types? **No.**
  (ScrollNah reads app usage and the app list only on the device and never sends them anywhere. Play only counts data as "collected" when it leaves the device. The app has no INTERNET permission.)
- Is all user data encrypted in transit? Not applicable (nothing is transmitted).
- "Share this card" on the weekly report: only when the user taps it, ScrollNah saves a picture of that card and opens Android's share menu; the picture goes only to the app the user picks. Nothing goes to the developer, so it isn't "collected".
- "Save a backup" in Settings: only when the user taps it, ScrollNah writes a JSON file to a place the user picks with Android's file picker. ScrollNah never uploads it, so it isn't "collected" or "shared" either.
- The optional Reels/Shorts & website tracker (Accessibility, off by default) reads, on the phone only, which screen is open in Instagram and YouTube, when their videos scroll, and the browser's address bar. It keeps only minutes and a count of videos in the app's private storage and sends nothing anywhere, so this isn't "collected" either (no web browsing history is saved: the address is only matched against the limited apps' websites and dropped).
- Do you provide a way for users to request that their data is deleted? Users can delete data in the app (**Settings > Data > Clear history**) or by uninstalling. If asked for a URL, use the privacy policy link.

### Accessibility API (needed from the first Play upload with the tracker)
ScrollNah is not an accessibility tool (`isAccessibilityTool` isn't set), so Play asks for this declaration. Google's checklist: https://support.google.com/googleplay/android-developer/answer/10964491

- **Why does your app use the AccessibilityService API?** App functionality (only).
- **Does your app collect or share personal or sensitive user data through it?** No. Everything is processed on the phone; only minutes and a count of Reels/Shorts videos are saved, in the app's private storage, and nothing is sent anywhere (the app has no INTERNET permission).
- **If a description box is shown:**
  > ScrollNah is a digital wellbeing app. Its optional tracker (off by default) uses the AccessibilityService API only to notice when the user is in Instagram Reels or YouTube Shorts, to count about how many of those videos they scroll, and to read the browser's address bar so time on the websites of apps they chose to limit (like youtube.com) counts too. Android's usage statistics only say which app is open, not whether the user is in Reels/Shorts or which website is showing, so this can't be done another way. The service only looks at Instagram, YouTube and browsers, saves only minutes and a count on the device, never stores screen content or web addresses, never reads messages or passwords, and sends nothing off the device. Its only action is a fixed rule the user sets up: when their own Reels/Shorts limit is reached (Firm or Beast mode), it presses Back once to leave that screen before ScrollNah's Pause screen appears. It performs no gestures (canPerformGestures="false") and nothing else on its own. While Reels or Shorts are open it shows a small badge with the count at the top of the screen (an accessibility overlay that touches pass through; the user can turn it off in Settings). Before sending the user to Android's settings, ScrollNah always shows an in-app disclosure of what it reads and how it's used, with "Agree & open Settings" and "Not now" (in first setup, on Limits' Reels & Shorts tab, from Home's suggestion, and in Settings).
- **Video link** (YouTube, Unlisted). Record on a real phone with YouTube signed in (Shorts don't play on an emulator without it), tracker off at the start. Home's "Count your Reels & Shorts" card only shows on a phone where the tracker has never been on (and "Not now" wasn't tapped on it): use a fresh install (clear ScrollNah's data, or uninstall and reinstall), or start step 1 at Limits > Reels & Shorts instead. Show, in this order, slowly, with captions if anything isn't obvious:
  1. Open ScrollNah (Home). Show the **"Count your Reels & Shorts"** card and tap **Show me**: Limits opens on the **Reels & Shorts** tab with the **"Limit Reels, Shorts & websites"** card. Pause so the whole text can be read; tap the chevron to show the notes too.
  2. **Decline:** tap **Not now** (the card goes away and nothing is turned on). Tap **Home** in the bottom bar, then **Limits** again (it reopens on the Reels & Shorts tab): the disclosure is shown again. Optionally also show Settings > Permissions > "Reels, Shorts & website tracker" (the same disclosure as a dialog).
  3. **Agree:** tap **Agree & open Settings** > Android's Accessibility settings > Downloaded apps > **ScrollNah Reels & Shorts tracker** > turn on > Android's own "Allow" confirmation.
  4. Go back to ScrollNah: Limits > **Reels & Shorts** now says "So far today: 0 Reels & Shorts". Set **"Reels & Shorts a day"** to **10**, switch it on and tap **Lock it in**.
  5. **Core feature:** open YouTube Shorts (or Instagram Reels) and swipe slowly: the mascot badge at the top counts them ("1/10", "2/10"...) and the mascot gets more worried near 10. At 10, show the tracker pressing Back once and the Pause screen ("10 reels today"). Tap "Nah, I'm good".
  6. Back in ScrollNah's Home: the "10/10 reels" pill next to the mascot (Home shows the mascot once at least one app is limited, e.g. Chrome picked during setup). Caption at the end: "The tracker only spots Reels/Shorts and limited websites and counts them. Nothing leaves the phone."
  - Optional extra (fresh install): the setup page "How many Reels & Shorts a day is enough?" and the last setup step "Count Reels & Shorts", which shows the same disclosure with "Agree & open Settings" and "Not now".
- If you change how the tracker uses Accessibility later, Google says to submit this declaration again.

### Advertising ID
**No**, the app does not use an advertising ID. (The manifest has no AD_ID permission.)

### Government apps / Financial features / Health
**No / None / No.** (ScrollNah is a digital wellbeing app, not a health or medical app.)

### Foreground service permissions
Type: **Special use** (`FOREGROUND_SERVICE_SPECIAL_USE`). The manifest already declares the subtype: "Digital wellbeing: checks which app is open so it can pause apps the user chose to limit and remind them to take breaks."

Description of the feature:
> ScrollNah is a digital wellbeing app. The user chooses apps to limit and a daily time budget for each. A foreground service checks about once a second which app is in front, only while the screen is on, so that when the user opens a limited app over its budget, ScrollNah can show its Pause screen or a reminder straight away (and pause a limited app's video that keeps playing in a floating window). It also times continuous sessions on one app for optional break reminders. The ongoing notification ("Blobby is keeping watch") tells the user it is running. The service stops checking while the screen is off.

User impact if the task were deferred or interrupted:
> The limits the user set would not be enforced: they could keep using an app after reaching their own daily limit, and break reminders would arrive late or not at all. The feature only works if it reacts the moment a limited app is opened.

Video: Play may ask for a short video. Use `store/fgs-demo.mp4` (record a new one if missing: see `CLAUDE_UPDATES.md`), upload it to YouTube as **Unlisted**, and paste the link.

### Other permission notes (no forms, but reviewers may look)
- `PACKAGE_USAGE_STATS` (Usage access): granted by the user in Android Settings, used to count minutes on the chosen apps.
- `SYSTEM_ALERT_WINDOW` (Display over other apps): shows the Pause screen and the mascot bubble over a limited app.
- `RECEIVE_BOOT_COMPLETED`: restarts the limit checks after a reboot.
- `POST_NOTIFICATIONS`: reminders and nudges.

## Release > Setup > App signing (do this once, before the first upload)

Recommended: **use the existing key** so GitHub installs and Play installs are the same app and can update each other.
1. When Play Console asks how to sign, choose to use your own key and **"Export and upload a key from Java keystore"**.
2. Download `pepk.jar` and the encryption public key it offers, then run (fill in your alias; it asks for the passwords):
   ```bash
   java -jar pepk.jar --keystore=keystore/unscroll-release.jks --alias=YOUR_ALIAS --output=scrollnah-signing-key.zip --include-cert --rsa-aes-encryption --encryption-key-path=encryption_public_key.pem
   ```
3. Upload `scrollnah-signing-key.zip`. Keep `keystore/` and `keystore.properties` backed up somewhere safe outside the project.
4. Play will also want an **upload key**. You can create a separate one with `keytool -genkeypair -v -keystore keystore/upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`, register its certificate, and point `keystore.properties` at it for Play uploads.

## Release > Testing > Closed testing (personal accounts)

- Create a closed test track, upload the AAB from `./gradlew bundlePlayRelease` (`app/build/outputs/bundle/playRelease/app-play-release.aab`).
- Add at least **12 testers** (an email list or a Google Group), share the opt-in link, and make sure they install it and actually use it.
- Keep at least 12 opted in for **14 days in a row**, then apply for production access in the Dashboard.

## Parent lock (from 2.4.0, not in 2.3.0)

Parent lock is optional and off by default. It adds no new Android permissions. Its **uninstall protection** registers ScrollNah as a device admin app with an **empty `<uses-policies />`** (`res/xml/uninstall_guard.xml`): it can't lock, wipe or change anything; Android only asks to deactivate it before uninstalling. There is no separate Play declaration form for this, but reviewers check device-admin apps, so add a reviewer note (App content > App access, or the release's notes to reviewers):

> ScrollNah has an optional Parent lock for parents who set it up on a child's phone. With it, ScrollNah can register as a device admin app that requests no policies (uses-policies is empty), only so Android asks before ScrollNah is uninstalled. The user turns it on themselves on Android's standard "Activate device admin app?" screen and can turn it off in ScrollNah's Settings with the parent PIN, or in Android's Device admin apps settings. It is never required and never activated without the user's action. ScrollNah does not monitor messages, browsing, location or other apps' content; the lock's report only notes ScrollNah's own state (stopped, a needed setting turned off, the clock moved) and stays on the phone.

If Play considers this "parental control / monitoring", the Families or Stalkerware policy questions may come up: ScrollNah keeps its ongoing "keeping watch" notification, shows its icon, doesn't hide, and sends nothing off the phone.
