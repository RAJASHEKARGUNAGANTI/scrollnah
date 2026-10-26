# Play Store kit

Everything needed for the Google Play listing of the **Play version** (no Reels/Shorts tracker).

| File | What it is |
|---|---|
| `listing.md` | App name, short and full description, release notes, category, contact links |
| `play-console-answers.md` | Draft answers for Play Console forms (data safety, content rating, foreground service, signing, closed testing) |
| `icon-512.png` | Store icon, 512x512 |
| `feature-graphic.png` | Feature graphic, 1024x500 |
| `screenshots/` | Phone screenshots, 1080x1920 (9:16), upload in file-name order |
| `graphics/` | HTML sources for the icon and feature graphic |
| `art/` | Mascot PNGs exported from the app (used by the feature graphic) |
| `fgs-demo.mp4` | Screen recording for the foreground service declaration (upload to YouTube as Unlisted) |

## Build the app bundle for Play

```bash
./gradlew bundlePlayRelease
```

Output: `app/build/outputs/bundle/playRelease/app-play-release.aab`. Bump `versionCode` in `app/build.gradle.kts` for every upload.

## Re-render the icon and feature graphic

They're HTML pages rendered with Chrome in headless mode (run from the project root):

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --hide-scrollbars --allow-file-access-from-files --window-size=1024,500 --virtual-time-budget=3000 --screenshot=$PWD/store/feature-graphic.png file://$PWD/store/graphics/feature.html
```

For the icon use `--window-size=512,512`, `icon.html` and `icon-512.png`. If the app icon changes, regenerate `icon.html` from `app/src/main/res/drawable/ic_launcher_foreground.xml` (same path data).

## Export mascot art from the app (debug builds only)

```bash
adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity --ez export true
adb shell run-as com.unscroll.app ls files/store
adb exec-out run-as com.unscroll.app cat files/store/blobby_thriving.png > store/art/blobby_thriving.png
```

## Retake the screenshots

On the emulator, with the Play debug build (`./gradlew assemblePlayDebug`):

1. `adb shell wm size 1080x1920` (Play allows at most 2:1; reset afterwards with `adb shell wm size reset`).
2. Clear the app data, turn on demo mode (`adb shell run-as com.unscroll.app touch files/demo_mode`, then start the app twice) and grant Usage access, Display over other apps and notifications.
3. Clean status bar: `adb shell settings put global sysui_demo_allowed 1`, then `adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941` (and battery/network/notifications, see `CLAUDE_UPDATES.md`).
4. `adb exec-out screencap -p > store/screenshots/NN-name.png` for each screen.
