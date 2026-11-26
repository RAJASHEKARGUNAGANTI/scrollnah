// Tests for "Import from phone": the app's small limits file and its full backup.

import assert from "node:assert/strict";
import { test } from "node:test";
import { pauseText } from "../src/lib/lines.ts";
import { applyImport, ImportError, parsePhoneFile } from "../src/lib/phone.ts";
import { defaultSettings, newSite } from "../src/lib/settings.ts";

const limitsFile = {
  app: "ScrollNah limits",
  format: 1,
  exportedAt: 1_790_000_000_000,
  appVersion: "2.7.0",
  name: "Raj",
  mascot: "BLOOMY",
  flowers: 2,
  strictness: "BEAST",
  bedtime: { enabled: true, start: 1380, end: 420 },
  workHours: { enabled: false, start: 540, end: 1020, days: [1, 2, 3, 4, 5] },
  apps: [
    { packageName: "com.instagram.android", appName: "Instagram", limitMinutes: 30, weekendLimitMinutes: 45, maxOpens: null, enabled: true },
    { packageName: "com.google.android.youtube", appName: "YouTube", limitMinutes: 45, weekendLimitMinutes: null, maxOpens: 8, enabled: true },
    { packageName: "com.facebook.katana", appName: "Facebook", limitMinutes: 20, weekendLimitMinutes: null, maxOpens: null, enabled: false },
    { packageName: "com.facebook.lite", appName: "Facebook Lite", limitMinutes: 15, weekendLimitMinutes: null, maxOpens: null, enabled: true },
    { packageName: "com.king.candycrushsaga", appName: "Candy Crush", limitMinutes: 20, weekendLimitMinutes: null, maxOpens: null, enabled: true },
  ],
  groups: [
    { name: "Social", limitMinutes: 60, enabled: true, packages: ["com.instagram.android", "com.facebook.katana"] },
    { name: "Games", limitMinutes: 30, enabled: true, packages: ["com.king.candycrushsaga"] },
  ],
  features: [
    { id: "youtube_shorts", limitMinutes: 20, enabled: true },
    { id: "reel_count", limitMinutes: 50, enabled: true },
  ],
};

test("reads the app's limits file", () => {
  const imp = parsePhoneFile(JSON.stringify(limitsFile));
  assert.equal(imp.appVersion, "2.7.0");
  assert.equal(imp.strictness, "beast");
  assert.equal(imp.mascot, "bloomy");
  assert.equal(imp.flowers, 2);
  assert.equal(imp.name, "Raj");
  assert.deepEqual(imp.bedtime, { enabled: true, start: 1380, end: 420 });
  assert.deepEqual(imp.skipped, ["Candy Crush"]);
  const ids = imp.sites.map((s) => s.site.id);
  assert.deepEqual(ids, ["instagram.com", "youtube.com", "facebook.com"]);
  const insta = imp.sites[0].site;
  assert.deepEqual([insta.minutes, insta.weekendMinutes], [30, 45]);
  assert.equal(imp.sites[1].site.maxOpens, 8);
  // Facebook + Facebook Lite: one site, the stricter limit, on if either was on.
  const fb = imp.sites[2];
  assert.deepEqual([fb.site.minutes, fb.site.enabled, fb.appNames], [15, true, ["Facebook", "Facebook Lite"]]);
  // "Games" has no website, so only "Social" comes over.
  assert.deepEqual(imp.budgets, [{ name: "Social", minutes: 60, enabled: true, sites: ["instagram.com", "facebook.com"] }]);
  // The daily reels count can't be counted on websites.
  assert.deepEqual(imp.features, [{ id: "youtube_shorts", minutes: 20, enabled: true }]);
});

test("reads exactly what the app writes (app/src/test/.../LimitsExportTest.kt)", () => {
  const fromApp =
    '{"app":"ScrollNah limits","format":1,"exportedAt":1795000000000,"appVersion":"2.7.0",' +
    '"name":"Raj","mascot":"BLOOMY","flowers":2,"strictness":"BEAST",' +
    '"bedtime":{"enabled":true,"start":1380,"end":420},' +
    '"workHours":{"enabled":false,"start":540,"end":1020,"days":[1,5]},' +
    '"apps":[{"packageName":"com.instagram.android","appName":"Instagram","limitMinutes":30,"weekendLimitMinutes":45,"maxOpens":null,"enabled":true},' +
    '{"packageName":"com.google.android.youtube","appName":"YouTube \\"Main\\"","limitMinutes":45,"weekendLimitMinutes":null,"maxOpens":8,"enabled":false}],' +
    '"groups":[{"name":"Social","limitMinutes":60,"enabled":true,"packages":["com.google.android.youtube","com.instagram.android"]}],' +
    '"features":[{"id":"youtube_shorts","limitMinutes":20,"enabled":true}]}';
  const imp = parsePhoneFile(fromApp);
  assert.deepEqual(imp.sites.map((s) => [s.site.id, s.site.minutes, s.site.weekendMinutes, s.site.maxOpens, s.site.enabled]), [
    ["instagram.com", 30, 45, null, true],
    ["youtube.com", 45, null, 8, false],
  ]);
  assert.deepEqual(imp.work, { enabled: false, start: 540, end: 1020, days: [1, 5] });
  assert.deepEqual(imp.budgets[0].sites, ["youtube.com", "instagram.com"]);
  assert.equal(imp.sites[1].appNames[0], 'YouTube "Main"');
});

test("reads a full app backup, ignoring its history", () => {
  const backup = {
    app: "ScrollNah",
    format: 1,
    appVersion: "2.6.1",
    exportedAt: 1,
    dbVersion: 14,
    settings: { user_name: { t: "s", v: "Sam" }, strictness: { t: "s", v: "CHILL" }, mascot_theme: { t: "s", v: "BLOBBY" } },
    bedtime: { enabled: false, start: 1320, end: 420 },
    workHours: { enabled: true, start: 540, end: 1020, days: [1, 2, 3] },
    tables: {
      tracked_apps: [{ packageName: "com.reddit.frontpage", appName: "Reddit", limitMinutes: 25, enabled: 1, weekendLimitMinutes: null, maxOpens: null }],
      app_groups: [{ id: 1, name: "Fun", limitMinutes: 40, enabled: 0, packages: "com.reddit.frontpage,com.zhiliaoapp.musically" }],
      feature_limits: [{ featureId: "instagram_reels", limitMinutes: 10, enabled: 1 }],
      daily_summary: [{ date: "2026-11-01", usedMinutes: 30 }],
    },
  };
  const imp = parsePhoneFile(JSON.stringify(backup));
  assert.deepEqual(imp.sites.map((s) => [s.site.id, s.site.minutes, s.site.enabled]), [["reddit.com", 25, true]]);
  assert.equal(imp.strictness, "chill");
  assert.equal(imp.name, "Sam");
  assert.deepEqual(imp.work?.days, [1, 2, 3]);
  assert.deepEqual(imp.budgets, [{ name: "Fun", minutes: 40, enabled: false, sites: ["reddit.com"] }]);
  assert.deepEqual(imp.features, [{ id: "instagram_reels", minutes: 10, enabled: true }]);
});

test("refuses files that aren't from ScrollNah", () => {
  assert.throws(() => parsePhoneFile("not json"), ImportError);
  assert.throws(() => parsePhoneFile(JSON.stringify({ app: "Other" })), ImportError);
  assert.throws(() => parsePhoneFile(JSON.stringify({ ...limitsFile, format: 2 })), /newer/);
});

test("importing merges: phone sites replace, computer-only sites stay", () => {
  const current = { ...defaultSettings(), setupDone: true, sites: [newSite("youtube.com", 90), newSite("twitch.tv", 30)] };
  current.budgets = [{ id: "keep-me", name: "social", minutes: 90, enabled: true, sites: ["youtube.com"] }];
  const next = applyImport(current, parsePhoneFile(JSON.stringify(limitsFile)), 5);
  assert.deepEqual(next.sites.map((s) => [s.id, s.minutes]), [["youtube.com", 45], ["twitch.tv", 30], ["instagram.com", 30], ["facebook.com", 15]]);
  assert.equal(next.budgets.length, 1, "same name (any case) is replaced");
  assert.equal(next.budgets[0].id, "keep-me");
  assert.equal(next.budgets[0].minutes, 60);
  assert.equal(next.strictness, "beast");
  assert.equal(next.mascot, "bloomy");
  assert.deepEqual(next.imported, { at: 5, appVersion: "2.7.0" });
  assert.equal(next.features.find((f) => f.id === "youtube_shorts")?.minutes, 20);
});

test("Pause page texts", () => {
  const base = { mascot: "blobby" as const, site: "YouTube", locked: false, opens: 3, limit: 30 };
  assert.deepEqual(
    [pauseText({ ...base, reason: "limit" }).pill, pauseText({ ...base, reason: "limit" }).title],
    ["3rd time today", "Woah. YouTube again?"],
  );
  assert.equal(pauseText({ ...base, reason: "limit", locked: true }).title, "That's it for YouTube today.");
  assert.equal(pauseText({ ...base, reason: "bedtime", until: 420 }).pill, "Bedtime");
  assert.match(pauseText({ ...base, reason: "budget", budgetName: "Social", limit: 60 }).subtitle, /Social had 1h together/);
  assert.match(pauseText({ ...base, reason: "opens", limit: 5 }).subtitle, /5 visits a day/);
});
