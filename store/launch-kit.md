# ScrollNah launch kit

Ready-to-paste posts for review and launch sites. Facts checked on 2026-11-25 against the release builds:
no INTERNET permission, no Google Play Services or Firebase, GitHub APK 1.73 MB ("under 2 MB"), Android 8.0+,
MIT licence.

Links used below:
- Google Play: https://play.google.com/store/apps/details?id=app.scrollnah
- Website (features, feedback form, newsletter): https://scrollnah.vercel.app
- Source code and APK: https://github.com/RAJASHEKARGUNAGANTI/scrollnah

## Ground rules (they decide how a post is received)

1. **Read each community's rules first** (Reddit sidebar, "About" or pinned posts). Many subreddits only allow
   self-promotion in a weekly thread, with a flair, or from accounts that also take part in the community.
   If a rule says no, use the weekly thread or skip that place.
2. **Be the maker, not an ad.** Say you built it, why, and ask a real question. No "best app ever".
3. **No donation link in posts.** The coffee button lives on the website; posts link to Play, the site or GitHub.
4. **Reply to every comment in the first few hours.** That is what keeps a post visible.
5. **Never ask for upvotes** (against Reddit's, Product Hunt's and Hacker News's rules). Sharing the link is fine.
6. Post on different days, not all at once, so you can answer everyone.
8. **On Reddit, link only Google Play** (and GitHub if the subreddit allows it). Many subreddits block links to
   free hosting addresses like `*.vercel.app` (r/droidappshowcase removed a post for it on 2026-11-25). Put the
   website in your Reddit profile instead, and say "website in my profile" if someone asks. A custom domain would
   avoid this filter later.
7. **Make the personal lines true for you.** Lines in [square brackets] are yours to fill in; reword anything that
   doesn't match your own story.

## Suggested order

| When | Where | Why |
|---|---|---|
| Day 1 | LinkedIn (post 1), r/SideProject, r/androidapps | Friendly to makers; first feedback |
| Day 2 | XDA thread, AlternativeTo listing, IzzyOnDroid request | Stay up for a long time; bring steady visitors |
| Day 3 | r/nosurf, r/digitalminimalism (only if their rules allow app posts) | Your exact users |
| Day 4 | r/developersIndia (check the showcase rules/day) | Indian dev community |
| A weekday later | **Show HN**, 8 to 10 am US Eastern (6:30 to 8:30 pm India) | HN is busiest then |
| A Tuesday to Thursday | **Product Hunt**, 12:01 am Pacific (1:31 pm India) | A launch day runs on Pacific time |
| About a week later | First newsletter issue (`website/newsletter/issues/`) | People who signed up from the posts |

---

## Reddit: r/SideProject

**Title**
```text
I built a tiny Android app that gets visibly sad when I doomscroll (free, no ads, no internet access)
```

**Post**
```text
Hi! I kept losing evenings to Reels and Shorts, so I turned a one-line note ("an app that stops my doomscrolling") into a real app: ScrollNah.

What it does:
- You set a daily budget for the apps that eat your day (5 minutes to 3 hours, different on weekends if you like).
- A mascot reacts to your day: Blobby the blob slowly melts as you use up your time, or Bloomy the plant grows a flower for every week you stay under your limits.
- You pick how strict it is: Chill (a nudge), Firm (a Pause screen with one slow breath before you continue), or Beast (locked until midnight).
- A weekly report shows where the time went and when you scroll most.

Things I cared about:
- No account, no ads, no analytics. The app doesn't even have the internet permission, so nothing can leave your phone.
- Under 2 MB. The mascots are drawn in code, not images.
- Open source (MIT), Kotlin + Jetpack Compose.

It's on Google Play and as an APK on GitHub. I'd love honest feedback: what would make you keep an app like this installed past the first week?

Play: https://play.google.com/store/apps/details?id=app.scrollnah
Source: https://github.com/RAJASHEKARGUNAGANTI/scrollnah
```

## Reddit: r/androidapps

(Check the required tag or flair for developer posts, e.g. "[DEV]".)

**Title**
```text
[DEV] ScrollNah: daily limits for the apps that eat your day, with a mascot that reacts (free, no ads, no internet permission, open source)
```

**Post**
```text
I'm the developer. ScrollNah is a small screen-time app with a bit of personality:

- Daily limits per app (5 to 180 min), weekend limits, a limit on how often you open an app, and shared budgets for a group of apps
- Three strictness levels: Chill (notification), Firm (a Pause screen: one breath, then "why are you opening it?"), Beast (locked until midnight)
- Break reminders after a long stretch on one app (notification, a mascot bubble, or both)
- Focus mode (also a Quick Settings tile), bedtime and work hours
- Weekly report: where the time went, your "scroll o'clock", whether the nudges worked
- Home-screen widgets, including an animated mascot on your wallpaper
- Light and dark, works with TalkBack

Permissions, in plain words: Usage access (to count minutes), notifications, display over other apps (for the Pause screen), running in the background. No internet permission at all, no account, no ads, no analytics. Under 2 MB, Android 8.0+. Open source (MIT).

Google Play: https://play.google.com/store/apps/details?id=app.scrollnah
APK and source: https://github.com/RAJASHEKARGUNAGANTI/scrollnah

Feedback and bug reports: Settings > Feedback in the app (no account needed), or reply here.
```

## Reddit: r/nosurf and r/digitalminimalism

(Only if the rules allow it; otherwise use their weekly or tools thread.)

**Title**
```text
What helped me stop opening Instagram on autopilot: one slow breath and the question "why?"
```

**Post**
```text
I noticed most of my scrolling wasn't a decision. My thumb opened the app before I'd even thought about it.

So I built myself a small Android app. When I open an app I've set a limit for and I'm over it, a little mascot asks "Woah. Instagram again?", breathes with me for three seconds, and asks why I'm opening it: bored, replying to someone, pure habit, or a work thing. Then I decide. [Say what happens for you, e.g. how often picking "pure habit" makes you close it.]

A few other things that helped:
- A daily budget per app instead of blocking everything
- A weekly report that shows my "scroll o'clock", the hour I scroll most [add yours if you like]
- A mascot that melts as I use up my time. Weirdly more motivating than a number.

It's free, with no ads and no account, and it doesn't have internet access, so nothing leaves the phone. It's called ScrollNah if you want to try it: https://play.google.com/store/apps/details?id=app.scrollnah

But I'm more curious about you: what actually worked for you to break the autopilot opening?
```

## Reddit: r/developersIndia

(Check the showcase rules: some Indian dev subreddits allow project posts only on a set day or with a flair.)

**Title**
```text
Built and shipped my first Android app on Play Store: ScrollNah, a playful screen-time app (Kotlin + Compose, open source)
```

**Post**
```text
Hi all! I've been turning ideas from my notes app into real things, and the first one is live: ScrollNah, a free Android app that helps you scroll less.

Some things I learned while building it, in case they help someone:
- Detecting which app is open without an Accessibility service: a foreground service polls UsageStatsManager about once a second (Play restricts Accessibility for non-accessibility apps).
- Floating video windows (picture-in-picture) don't count as "in front", so a blocked YouTube could keep playing there. I pause it by briefly taking audio focus.
- Keeping it small: dropped Jetpack Glance for plain RemoteViews widgets, trimmed the fonts, drew the mascots with Compose Canvas. The APK is under 2 MB.
- No internet permission at all, so the privacy story is simple.
- The website is Next.js on Vercel with MongoDB Atlas for the feedback form and newsletter, all on free tiers.

Play: https://play.google.com/store/apps/details?id=app.scrollnah
Source (MIT): https://github.com/RAJASHEKARGUNAGANTI/scrollnah

Happy to answer questions about the Play review process (closed testing, production access) or anything else. Feedback welcome!
```

---

## Show HN

**Title** (keep the format exactly: "Show HN: name – what it is")
```text
Show HN: ScrollNah – a tiny Android app to scroll less, with no internet access
```

**URL field:** https://github.com/RAJASHEKARGUNAGANTI/scrollnah

**Text** (post it as the first comment if you use the URL field)
```text
I built ScrollNah because I kept losing evenings to short videos. You give the apps you want to use less a daily budget, and a mascot reacts: it slowly melts as you use your time up. At the limit you get, depending on how strict you chose to be, a nudge, a Pause screen (one breath, then "why are you opening it?"), or a lock until midnight.

Some details that may interest HN:

- It has no INTERNET permission. Usage data, history and settings live only on the phone; backups are a file you pick.
- No Accessibility service for the core features (Play restricts it): a foreground service polls UsageStatsManager about once a second and only while the screen is on. There's an opt-in, off-by-default Accessibility tracker in the GitHub build for counting Reels/Shorts, with a disclosure screen first.
- Picture-in-picture windows aren't "in front" to UsageStats, so a blocked app could keep playing in a floating window; it pauses those by briefly taking audio focus.
- Under 2 MB: Compose Canvas mascots instead of images, trimmed fonts, plain RemoteViews widgets.
- Kotlin + Jetpack Compose, Room, DataStore. MIT licensed.

Google Play: https://play.google.com/store/apps/details?id=app.scrollnah

I'd love feedback on the approach, and on what would make a tool like this stick for you.
```

---

## Product Hunt

- **Name:** ScrollNah
- **Tagline** (60 max):
  ```text
  Scroll less, with a little buddy who reacts to every minute
  ```
- **Description** (260 max):
  ```text
  ScrollNah is a free Android app that helps you scroll less. Set a daily budget for the apps that eat your day; Blobby the blob melts and Bloomy the plant wilts when you overdo it. No account, no ads, no internet access, under 2 MB.
  ```
- **Links:** Google Play, website, GitHub
- **Topics:** Android, Productivity, Health & Fitness (or Digital Wellbeing), Open Source
- **Pricing:** Free
- **Gallery** (first image matters most): `channel-intro/youtube/linkedin/linkedin-scrollnah.png`, then the Play screenshots in `store/screenshots/` (Home, Pause screen, Your week, Bloomy), and the promo video `channel-intro/youtube/videos/2.3.0/out/ScrollNah-2.3.0-promo-wide-en.mp4` (upload it to YouTube and paste the link).
- **Thumbnail:** `store/icon-512.png`

**Maker comment** (post right after launching)
```text
Hi Product Hunt! I'm Raj, and ScrollNah is the first idea from my notes app that I turned into a real product.

I kept losing evenings to Reels and Shorts, and every screen-time app I tried felt like a strict teacher. So ScrollNah is a friend who notices instead: a mascot that's thriving in the morning, gets worried as you near your limit, and melts if you go over. If you pick Bloomy, a plant grows a flower for every week you stay under your limits, and full pots go to your garden.

You choose how strict it is: a gentle nudge, a Pause screen that asks why you're opening the app, or Beast mode (locked until midnight).

It's free, open source, has no account and no ads, and doesn't even have internet access, so nothing ever leaves your phone.

I'd love your honest feedback, especially: what would make you keep it installed past week one?
```

---

## XDA Forums (Android Apps and Games section)

**Title**
```text
[APP][8.0+][FREE][OPEN SOURCE] ScrollNah – daily app limits with a mascot that reacts
```

**Post**
```text
ScrollNah helps you cut down on endless scrolling, without guilt trips. Set a daily budget for the apps that eat your day, and a mascot (Blobby the blob or Bloomy the plant) reacts to every minute.

FEATURES
- Daily limits per app (5 to 180 min), weekend limits, open limits, shared budgets for a group of apps
- Strictness: Chill (notification), Firm (Pause screen with one breath and "why are you opening it?"), Beast (locked until midnight)
- Pauses blocked apps that keep playing in a floating (picture-in-picture) window
- Break reminders, Focus mode (also a Quick Settings tile), bedtime and work hours
- Weekly report with "scroll o'clock", Pause answers and a 4-week trend
- Bloomy grows a flower for every week under your limits; full pots go to your garden
- Widgets, including an animated mascot; light and dark; TalkBack support
- Backup and restore to a file you choose
- GitHub version only: optional, off-by-default Reels & Shorts counter (uses Accessibility, explained before you turn it on)

PRIVACY
No internet permission, no account, no ads, no analytics. Everything stays on your phone.

PERMISSIONS
Usage access (count minutes), notifications (reminders), display over other apps (Pause screen and bubble), run in the background (reminders on time).

DOWNLOAD
Google Play: https://play.google.com/store/apps/details?id=app.scrollnah
APK (GitHub releases): https://github.com/RAJASHEKARGUNAGANTI/scrollnah/releases/latest
Note: Play and GitHub installs can't update each other (different signing keys). Back up in Settings, uninstall, install the other, restore.

SOURCE: https://github.com/RAJASHEKARGUNAGANTI/scrollnah (MIT)
FEEDBACK / BUGS: https://scrollnah.vercel.app/feedback (or reply here)
```

---

## AlternativeTo (add as a new app)

- **Name:** ScrollNah
- **Short description:**
  ```text
  A playful, private Android app for daily app limits: a mascot reacts as you scroll, and a Pause screen asks why you're opening the app.
  ```
- **Description:**
  ```text
  ScrollNah helps you spend less time on the apps you choose. Give each one a daily budget and pick how strict it should be: a gentle nudge, a Pause screen with one slow breath, or a lock until midnight. Blobby the blob or Bloomy the plant reacts to your day, and a weekly report shows where the time went. No account, no ads, no analytics, and no internet permission: everything stays on the phone. Free and open source (MIT), under 2 MB.
  ```
- **Platforms:** Android
- **License:** Free, Open Source (MIT)
- **Website:** https://scrollnah.vercel.app
- **Tags:** digital-wellbeing, screen-time, app-blocker, focus, productivity, self-control, privacy-focused
- **Alternative to** (similar apps people search for): Digital Wellbeing (Google), ScreenZen, one sec, StayFree, ActionDash, Opal
- **Features to tick:** Ad-free, No registration required, Privacy focused, Offline, Dark mode, Widgets, Open source

---

## IzzyOnDroid (an F-Droid-style repository for open-source apps)

IzzyOnDroid includes apps whose APKs are attached to GitHub releases, so ScrollNah's existing release APKs fit.
Requests are made as an issue on their GitLab project ("IzzyOnDroid / repo"), using the app inclusion
template. Read their current inclusion criteria on that page before posting.

**Request text**
```text
App: ScrollNah (app.scrollnah)
Source: https://github.com/RAJASHEKARGUNAGANTI/scrollnah
License: MIT
APKs: attached to GitHub releases (latest: https://github.com/RAJASHEKARGUNAGANTI/scrollnah/releases/latest), signed with my own key
Description: Daily limits for the apps that eat your day, with a mascot that reacts. Strictness levels (nudge, Pause screen, lock until midnight), break reminders, focus mode, weekly report, widgets.
Network: no INTERNET permission; no trackers, no Google Play Services, no Firebase.
Notes: optional, off-by-default Accessibility service (only to count Reels/Shorts and time on limited websites; explained in an in-app disclosure first). Donations: https://www.buymeacoffee.com/rajashekargunaganti
I'm the developer and happy to help with anything needed.
```

Good to add later (helps IzzyOnDroid and F-Droid show your screenshots and text): Fastlane metadata in the repo
(`fastlane/metadata/android/en-US/` with `title.txt`, `short_description.txt`, `full_description.txt`,
`images/phoneScreenshots/`, `changelogs/<versionCode>.txt`).

---

## LinkedIn

Updated posts with the website and newsletter links: `channel-intro/youtube/linkedin-posts.md`
(images in `channel-intro/youtube/linkedin/`).

## Instagram and YouTube

Add the website to both bios: `scrollnah.vercel.app`. In captions, "Link in bio".
