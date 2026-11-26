# Publishing ScrollNah for browsers

The accounts are yours to create; nothing here is uploaded automatically.

## 1. Build the packages
```bash
cd extension
npm test           # the rules (node --test, no packages needed)
node build.mjs     # dist/scrollnah-chrome-<version>.zip and dist/scrollnah-firefox-<version>.zip
```
Raise `version` in `extension/package.json` for every new upload (stores refuse the same version twice).

## 2. Chrome Web Store (Chrome, and installs in Brave, Opera, Vivaldi too)
1. https://chrome.google.com/webstore/devconsole: register as a developer (one-time US$5 fee).
2. **New item** > upload `dist/scrollnah-chrome-<version>.zip`.
3. **Store listing**: name, summary and description from `listing.md`, category, language English. Upload the icon
   (`src/icons/icon-128.png`), the five `screenshot-*.png` and `promo-440x280.png`.
4. **Privacy**: single purpose, permission justifications, "no data collected", privacy policy URL (all in `listing.md`).
5. **Distribution**: Public, all regions. Submit for review (usually a few days; host permissions can take longer).

## 3. Microsoft Edge Add-ons (free)
1. https://partner.microsoft.com/dashboard/microsoftedge: register (free).
2. Upload the **same Chrome zip**. Same listing text and pictures (Edge wants 1280x800 or 640x400 screenshots: these fit).

## 4. Firefox Add-ons (free)
1. https://addons.mozilla.org/developers: sign in with a Firefox account.
2. **Submit a New Add-on** > "On this site" > upload `dist/scrollnah-firefox-<version>.zip`.
3. Mozilla asks for the **source code** because the .js files are generated from TypeScript: upload a zip of the
   `extension/` folder (without `dist/`) and say: "Node 24+, run `node build.mjs`; no npm packages are needed. The build
   only strips TypeScript types (node:module stripTypeScriptTypes) and copies files."
4. Listing text and screenshots from `listing.md`. Data collection: none (already declared in the manifest).
5. The add-on id `{944b83b9-1b7c-4e38-80a8-0660716da1d1}` is fixed in `build.mjs`: never change it after the first upload.

## 5. After publishing
- Put the store links in `website/lib/site.ts` (`chromeStore`, `firefoxStore`, `edgeStore`): the website's
  "ScrollNah for your browser" section then shows "Add to Chrome" / "Add to Firefox" buttons instead of "coming soon".
- The website's idea vote "ScrollNah for your browser" can then be marked as done.

## Updating the pictures
```bash
cd extension
node build.mjs --test
PLAYWRIGHT_CORE=/path/to/node_modules/playwright-core node tools/store-shots.mjs
```
