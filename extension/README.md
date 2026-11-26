# ScrollNah for browsers

ScrollNah as a browser extension for Chrome, Edge, Brave, Opera (one build) and Firefox (a second build from the
same code). Daily limits per site, the mascot in the toolbar, the Pause page, Chill/Firm/Beast, and "Import from
phone" to bring the app's limits over. Everything stays in the browser: no account, no server, no analytics.

## Build, test, try
Needs **Node 24 or newer** and nothing else (no npm packages).

```bash
cd extension
npm test              # unit tests for the rules (node --test)
node build.mjs        # dist/chrome, dist/firefox and a .zip of each
```

Try it in Chrome: `chrome://extensions` > Developer mode > **Load unpacked** > pick `extension/dist/chrome`.
In Firefox: `about:debugging#/runtime/this-firefox` > **Load Temporary Add-on** > pick `extension/dist/firefox/manifest.json`.
After changing code, run `node build.mjs` again and press the reload button on the extension.

Type-checking (optional, needs TypeScript 5.8+): `npx tsc -p tsconfig.json`, or `../website/node_modules/.bin/tsc -p tsconfig.json`.
The browser API types are written by hand in `src/types/ext.d.ts` (only what ScrollNah uses).

## How the build works
`build.mjs` uses Node's own `stripTypeScriptTypes`: each `.ts` file becomes a `.js` file with the types removed and
nothing else changed (imports of `./x.ts` are pointed at `./x.js`). So the code must use only "erasable" TypeScript:
no `enum`, no `namespace`, no constructor parameter properties. Pages, CSS, fonts and icons are copied as they are,
and a `manifest.json` is written for each browser (Chrome: a service worker; Firefox: a background script and the add-on id).
`node build.mjs --test` also grants the popular sites at install, for automated tests only.

## Where things are
| File | What it does |
|---|---|
| `src/background.ts` | Counts time, shows the Pause page or a Chill nudge, the toolbar mascot and badge |
| `src/content.ts` | Runs on the user's sites only: "still here" every 15 s while visible, and the hide switches. A plain script (no imports) |
| `src/lib/rules.ts` | Limits, budgets, visits, Reels/Shorts, bedtime: why a site is paused, and the mood |
| `src/lib/track.ts` | Time since the last check, split at midnight, not across sleep |
| `src/lib/beast.ts` | Beast mode's 10-minute cool-down, and what counts as loosening |
| `src/lib/phone.ts` | Reads the app's "Copy limits to computer" file (or a full app backup) |
| `src/lib/sites.ts` | Known sites, their addresses and the Android apps they belong to |
| `src/lib/art.ts` | Blobby and Bloomy, ported from the app's Compose drawing code |
| `src/lib/lines.ts` | What the mascot says (same lines as the app) |
| `src/pages/` | popup (toolbar), options (settings), welcome (setup), pause (Pause page) |
| `test/` | Unit tests (`npm test`) |
| `store/` | Store listing, permission answers, screenshots, publishing steps |

## How counting works (fair counting)
Time counts only while a limited site is **the tab in front**, in the **browser window you're using**, and you're at
the computer (no keyboard or mouse for 60 s = away, unless the tab is playing sound). The background script keeps the
current stretch ("live") in `storage.session` and adds it to the day on every event: tab switched, page changed, window
focus, idle, an alarm, or the page's 15-second "still here". A gap over 150 s means the computer slept, so it isn't counted.
A **visit** is a tab arriving on a site from somewhere else; switching between tabs doesn't count, and coming back from
the Pause page doesn't either.

## Privacy
- Permissions: storage, alarms, idle, notifications, scripting. Site access is optional and asked **per site** when the
  user adds it (`optional_host_permissions`), so there's no "read all your data" warning at install.
- No `tabs` permission: the extension only sees the address of tabs on the user's own sites.
- Saved in `storage.local`: settings, and per day the minutes, visits and Pause answers per site (last 60 days).
  Never page content, titles or addresses.

## Store
See `store/README.md`.
