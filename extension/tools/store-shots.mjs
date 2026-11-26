// Makes the store pictures in extension/store/ (Chrome Web Store: 1280x800 screenshots and a 440x280
// promo tile; Firefox uses the same screenshots). Loads the test build into Playwright's Chromium with
// demo numbers, so nothing real is shown.
//
//   node build.mjs --test
//   PLAYWRIGHT_CORE=/path/to/node_modules/playwright-core node tools/store-shots.mjs
//
// (Playwright isn't a dependency of the extension; point PLAYWRIGHT_CORE at any copy of playwright-core
// that has its Chromium installed.)

import { mkdtempSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const { chromium } = await import(process.env.PLAYWRIGHT_CORE ? join(process.env.PLAYWRIGHT_CORE, "index.mjs") : "playwright-core");
const ext = join(root, "dist", "chrome");
const out = join(root, "store");
mkdirSync(out, { recursive: true });

const ctx = await chromium.launchPersistentContext(mkdtempSync(join(tmpdir(), "sn-shots-")), {
  channel: "chromium",
  headless: true,
  viewport: { width: 1280, height: 800 },
  deviceScaleFactor: 1,
  args: [`--disable-extensions-except=${ext}`, `--load-extension=${ext}`],
});
let [sw] = ctx.serviceWorkers();
if (!sw) sw = await ctx.waitForEvent("serviceworker");
const id = sw.url().split("/")[2];
const page = (p) => `chrome-extension://${id}/pages/${p}`;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Demo numbers: 35 of 115 minutes used today (Blobby is happy).
await sw.evaluate(async () => {
  const d = new Date();
  const key = `day:${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const site = (id, name, hosts, minutes) => ({ id, name, hosts, minutes, weekendMinutes: null, maxOpens: null, enabled: true });
  await chrome.storage.local.set({
    settings: {
      setupDone: true, setupAt: 1, name: "Alex", mascot: "blobby", flowers: 3, strictness: "firm",
      sites: [site("youtube.com", "YouTube", ["youtube.com", "youtu.be"], 45), site("instagram.com", "Instagram", ["instagram.com"], 30), site("reddit.com", "Reddit", ["reddit.com"], 20), site("x.com", "X", ["x.com", "twitter.com"], 20)],
      features: [{ id: "youtube_shorts", minutes: 15, enabled: false }, { id: "instagram_reels", minutes: 15, enabled: false }],
      hide: { youtubeShorts: true, youtubeHome: true, instagramReels: false, instagramExplore: true, xForYou: false },
    },
    [key]: { ms: { "youtube.com": 22 * 60000, "instagram.com": 9 * 60000, "reddit.com": 4 * 60000 }, featureMs: { youtube_shorts: 6 * 60000 }, opens: { "youtube.com": 3 }, passes: {}, nudged: {}, pauses: [] },
  });
});
await sleep(800);
for (const p of ctx.pages()) if (p.url().includes("welcome")) await p.close();

const tab = await ctx.newPage();

/** A 1280x800 picture: a caption on the left, a picture of the extension on the right. */
async function framed(file, title, text, shot, { width, height, bg = "var(--lilac)" }) {
  await tab.setViewportSize({ width: 1280, height: 800 });
  await tab.goto(page("popup.html"));
  await sleep(300);
  await tab.evaluate(({ title, text, img, width, height, bg }) => {
    document.body.style.cssText = `width:1280px;height:800px;margin:0;background:${bg};overflow:hidden`;
    document.body.innerHTML = `
      <div style="display:flex;align-items:center;gap:64px;height:800px;padding:0 88px;box-sizing:border-box;color:#16131f">
        <div style="flex:1">
          <div class="wordmark" style="font-size:28px;margin-bottom:28px">scrollnah</div>
          <h1 style="font-size:58px;line-height:1.02;margin:0 0 20px">${title}</h1>
          <p style="font-size:22px;line-height:1.45;max-width:30ch;margin:0">${text}</p>
        </div>
        <img src="${img}" style="width:${width}px;height:${height}px;border:2px solid #16131f;border-radius:22px;box-shadow:8px 8px 0 #16131f;background:#fff">
      </div>`;
  }, { title, text, img: `data:image/png;base64,${shot.toString("base64")}`, width, height, bg });
  await sleep(300);
  await tab.screenshot({ path: join(out, file) });
}

// 1. The popup.
const popup = await ctx.newPage();
await popup.setViewportSize({ width: 360, height: 560 });
await popup.emulateMedia({ reducedMotion: "reduce" });
await popup.goto(page("popup.html"));
await sleep(900);
const popupShot = await popup.screenshot({ clip: { x: 0, y: 0, width: 360, height: 560 } });
await framed("screenshot-1-popup.png", "Your buddy in the toolbar", "Blobby or Bloomy reacts to how much you scroll today, with the minutes left on every site.", popupShot, { width: 412, height: 641 });

// 2. The Pause page, full size.
await tab.setViewportSize({ width: 1280, height: 800 });
await tab.emulateMedia({ reducedMotion: "reduce" });
await tab.goto(page("pause.html?site=youtube.com&reason=limit"));
await sleep(3600); // after the breath
await tab.getByRole("button", { name: "Pure habit" }).click();
await tab.screenshot({ path: join(out, "screenshot-2-pause.png") });

// 3. Settings: sites and strictness.
const settings = await ctx.newPage();
await settings.setViewportSize({ width: 760, height: 760 });
await settings.emulateMedia({ reducedMotion: "reduce" });
await settings.goto(page("options.html#sites"));
await sleep(900);
await settings.evaluate(() => document.getElementById("sites").scrollIntoView());
const settingsShot = await settings.screenshot();
await framed("screenshot-3-limits.png", "A daily limit for each site", "Weekend limits, visits a day, shared budgets, bedtime and work hours. Only the tab you're looking at counts.", settingsShot, { width: 600, height: 600 });

// 4. Hidden Shorts.
await tab.goto(page("pause.html?reason=hidden&site=youtube.com&what=YouTube%20Shorts&home=https%3A%2F%2Fwww.youtube.com%2F"));
await sleep(700);
await tab.screenshot({ path: join(out, "screenshot-4-hide.png") });

// 5. Import from phone.
await settings.goto(page("options.html#phone"));
await sleep(700);
const file = join(mkdtempSync(join(tmpdir(), "sn-file-")), "scrollnah-limits.json");
const { writeFileSync } = await import("node:fs");
writeFileSync(file, JSON.stringify({
  app: "ScrollNah limits", format: 1, exportedAt: Date.now(), appVersion: "2.7.0", name: "Alex", mascot: "BLOBBY", flowers: 3, strictness: "FIRM",
  bedtime: { enabled: true, start: 1380, end: 420 }, workHours: { enabled: false, start: 540, end: 1020, days: [1, 2, 3, 4, 5] },
  apps: [
    { packageName: "com.google.android.youtube", appName: "YouTube", limitMinutes: 45, weekendLimitMinutes: 60, maxOpens: null, enabled: true },
    { packageName: "com.instagram.android", appName: "Instagram", limitMinutes: 30, weekendLimitMinutes: null, maxOpens: 10, enabled: true },
    { packageName: "com.reddit.frontpage", appName: "Reddit", limitMinutes: 20, weekendLimitMinutes: null, maxOpens: null, enabled: true },
    { packageName: "com.king.candycrushsaga", appName: "Candy Crush", limitMinutes: 20, weekendLimitMinutes: null, maxOpens: null, enabled: true },
  ],
  groups: [], features: [{ id: "youtube_shorts", limitMinutes: 15, enabled: true }],
}));
await settings.setInputFiles('input[type="file"]', file);
await sleep(500);
await settings.evaluate(() => document.querySelector(".found").scrollIntoView({ block: "center" }));
await sleep(200);
const box = await settings.locator(".found").boundingBox();
const importShot = await settings.screenshot({ clip: { x: box.x - 16, y: box.y - 16, width: box.width + 32, height: box.height + 32 } });
await framed("screenshot-5-phone.png", "Bring your phone's limits", "Set them once in the ScrollNah app, copy one small file to your computer and drop it in. Nothing is uploaded.", importShot, { width: 600, height: Math.round(600 * (box.height + 32) / (box.width + 32)) });

// Promo tile, 440x280.
await tab.setViewportSize({ width: 440, height: 280 });
await tab.goto(page("popup.html"));
await sleep(300);
await tab.evaluate(async () => {
  const { drawMascot } = await import(chrome.runtime.getURL("lib/art.js"));
  document.body.style.cssText = "width:440px;height:280px;margin:0;background:#c6f432;overflow:hidden";
  document.body.innerHTML = `<div style="display:flex;align-items:center;gap:18px;height:280px;padding:0 26px;box-sizing:border-box;color:#16131f">
    <canvas width="300" height="300" style="width:150px;height:150px;flex:none"></canvas>
    <div><div class="wordmark" style="font-size:40px">scrollnah</div><p style="font-size:18px;font-weight:700;margin-top:10px;line-height:1.3">Scroll less, with a little buddy. For your browser.</p></div></div>`;
  const ctx = document.querySelector("canvas").getContext("2d");
  ctx.scale(2, 2);
  drawMascot(ctx, 150, { mascot: "blobby", mood: 1 });
});
await sleep(300);
await tab.screenshot({ path: join(out, "promo-440x280.png") });

await ctx.close();
console.log("Store pictures saved in", out);
