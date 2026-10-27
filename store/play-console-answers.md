# Play Console: draft answers for ScrollNah (Play version)

The Play version is built with `./gradlew bundlePlayRelease`. It has **no Accessibility service**, so the Accessibility declaration does not apply. Check each answer against the current wording in Play Console before submitting.

## Policy > App content

### Privacy policy
`https://rajashekargunaganti.github.io/scrollnah/privacy-policy`
(Published from `docs/privacy-policy.md` with GitHub Pages.)

### App access
**All functionality is available without special access** (no login).
Optional instructions for reviewers:
> No account needed. To see ScrollNah in action: open ScrollNah, allow Usage access when asked, add an app (for example Chrome) in Limits with a 5-minute limit, choose "Firm", tap "Lock it in", then use that app for 5 minutes. The Pause screen appears over it. Break reminders (Limits > Break reminders) can be set to remind after a few minutes on one app.

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
- Do you provide a way for users to request that their data is deleted? Users can delete data in the app (**Settings > Data > Clear history**) or by uninstalling. If asked for a URL, use the privacy policy link.

### Advertising ID
**No**, the app does not use an advertising ID. (The manifest has no AD_ID permission.)

### Government apps / Financial features / Health
**No / None / No.** (ScrollNah is a digital wellbeing app, not a health or medical app.)

### Foreground service permissions
Type: **Special use** (`FOREGROUND_SERVICE_SPECIAL_USE`). The manifest already declares the subtype: "Digital wellbeing: checks which app is open so it can pause apps the user chose to limit and remind them to take breaks."

Description of the feature:
> ScrollNah is a digital wellbeing app. The user chooses apps to limit and a daily time budget for each. A foreground service checks about once a second which app is in front, only while the screen is on, so that when the user opens a limited app over its budget, ScrollNah can show its Pause screen or a reminder straight away. It also times continuous sessions on one app for optional break reminders. The ongoing notification ("Blobby is keeping watch") tells the user it is running. The service stops checking while the screen is off.

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
