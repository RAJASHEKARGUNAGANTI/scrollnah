// Tests for the rules: sites, schedules, limits, mood, counting and Beast mode.
// Run with: npm test (node --test, no packages needed)

import assert from "node:assert/strict";
import { test } from "node:test";
import { allowsLoosening, guardState, loosenings, SETTLING_MS, WAIT_MS, WINDOW_MS } from "../src/lib/beast.ts";
import { blockFor, emptyDay, limitOn, moodFrom, moodToday, msUntilChange } from "../src/lib/rules.ts";
import { cleanSettings, defaultSettings, newSite, type Settings } from "../src/lib/settings.ts";
import { cleanSiteInput, featureOfUrl, siteForApp, siteOfUrl } from "../src/lib/sites.ts";
import { dayKey, duration, durationPair, ordinal, scheduleActive, scheduleEndsAt, scheduleNextStart } from "../src/lib/time.ts";
import { creditTime, MAX_GAP_MS } from "../src/lib/track.ts";
import { hiddenPageFor } from "../src/lib/hide.ts";

const at = (y: number, mo: number, d: number, h = 12, mi = 0) => new Date(y, mo - 1, d, h, mi).getTime();
const MIN = 60_000;

function settingsWith(patch: Partial<Settings> = {}): Settings {
  return { ...defaultSettings(), setupDone: true, sites: [newSite("youtube.com", 30), newSite("reddit.com", 20)], ...patch };
}

test("cleaning typed addresses", () => {
  assert.equal(cleanSiteInput("https://www.YouTube.com/feed?x=1"), "youtube.com");
  assert.equal(cleanSiteInput("m.facebook.com"), "facebook.com");
  assert.equal(cleanSiteInput("old.reddit.com/r/all"), "old.reddit.com");
  assert.equal(cleanSiteInput("news.ycombinator.com:443"), "news.ycombinator.com");
  assert.equal(cleanSiteInput("not a site"), null);
  assert.equal(cleanSiteInput("localhost"), null);
  assert.equal(cleanSiteInput("192.168.0.1"), null);
  assert.equal(cleanSiteInput("-bad-.com"), null);
});

test("which site a page belongs to", () => {
  const sites = [newSite("youtube.com"), newSite("reddit.com"), newSite("old.reddit.com"), newSite("x.com")];
  assert.equal(siteOfUrl("https://www.youtube.com/watch?v=1", sites), "youtube.com");
  assert.equal(siteOfUrl("https://m.youtube.com/", sites), "youtube.com");
  assert.equal(siteOfUrl("https://youtu.be/abc", sites), "youtube.com");
  assert.equal(siteOfUrl("https://music.youtube.com/", sites), null, "YouTube Music doesn't count");
  assert.equal(siteOfUrl("https://old.reddit.com/r/all", sites), "old.reddit.com", "the most specific address wins");
  assert.equal(siteOfUrl("https://www.reddit.com/", sites), "reddit.com");
  assert.equal(siteOfUrl("https://twitter.com/home", sites), "x.com");
  assert.equal(siteOfUrl("https://notyoutube.com/", sites), null);
  assert.equal(siteOfUrl("chrome://extensions", sites), null);
  assert.equal(siteOfUrl(undefined, sites), null);
});

test("Shorts and Reels pages", () => {
  assert.equal(featureOfUrl("https://www.youtube.com/shorts/abc123")?.id, "youtube_shorts");
  assert.equal(featureOfUrl("https://www.youtube.com/watch?v=abc"), null);
  assert.equal(featureOfUrl("https://www.instagram.com/reels/xyz/")?.id, "instagram_reels");
  assert.equal(featureOfUrl("https://www.instagram.com/reel/xyz/")?.id, "instagram_reels");
  assert.equal(featureOfUrl("https://www.instagram.com/someone/"), null);
  assert.equal(featureOfUrl("https://example.com/shorts/1"), null);
});

test("hidden pages", () => {
  const hide = { ...defaultSettings().hide, youtubeShorts: true, instagramReels: true };
  const shorts = hiddenPageFor("https://www.youtube.com/shorts/dQw4w9WgXcQ", hide);
  assert.equal(shorts?.what, "YouTube Shorts");
  assert.equal(shorts?.watch, "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
  assert.equal(hiddenPageFor("https://www.instagram.com/reels/", hide)?.what, "Instagram Reels");
  assert.equal(hiddenPageFor("https://www.instagram.com/reel/abc/", hide), null, "a single reel someone sent still opens");
  assert.equal(hiddenPageFor("https://www.instagram.com/explore/", hide), null, "Explore isn't hidden");
  assert.equal(hiddenPageFor("https://www.youtube.com/shorts/x", defaultSettings().hide), null);
});

test("phone apps map to websites", () => {
  assert.equal(siteForApp("com.google.android.youtube")?.id, "youtube.com");
  assert.equal(siteForApp("com.twitter.android")?.id, "x.com");
  assert.equal(siteForApp("com.king.candycrushsaga"), undefined);
});

test("schedules: bedtime across midnight and work days", () => {
  const bedtime = { enabled: true, start: 22 * 60, end: 7 * 60 };
  assert.equal(scheduleActive(bedtime, at(2026, 11, 26, 23, 30)), true);
  assert.equal(scheduleActive(bedtime, at(2026, 11, 26, 6, 59)), true);
  assert.equal(scheduleActive(bedtime, at(2026, 11, 26, 7, 0)), false);
  assert.equal(scheduleActive({ ...bedtime, enabled: false }, at(2026, 11, 26, 23)), false);
  assert.equal(new Date(scheduleEndsAt(bedtime, at(2026, 11, 26, 23))).getHours(), 7);
  assert.equal(new Date(scheduleEndsAt(bedtime, at(2026, 11, 26, 23))).getDate(), 27);

  const work = { enabled: true, start: 9 * 60, end: 17 * 60, days: [1, 2, 3, 4, 5] };
  assert.equal(scheduleActive(work, at(2026, 11, 26, 10)), true, "Thursday 10:00");
  assert.equal(scheduleActive(work, at(2026, 11, 28, 10)), false, "Saturday");
  // Night shift Friday 22:00 - 06:00 belongs to Friday, so Saturday 03:00 is still work.
  const night = { enabled: true, start: 22 * 60, end: 6 * 60, days: [5] };
  assert.equal(scheduleActive(night, at(2026, 11, 28, 3)), true);
  assert.equal(scheduleActive(night, at(2026, 11, 29, 3)), false);
  assert.equal(scheduleNextStart(work, at(2026, 11, 27, 18)), at(2026, 11, 30, 9), "Friday evening -> Monday 9:00");
});

test("how time is written", () => {
  assert.equal(duration(45), "45 min");
  assert.equal(duration(78), "1h 18m");
  assert.equal(duration(120), "2h");
  assert.equal(durationPair(3, 5), "3 of 5 min");
  assert.equal(durationPair(45, 90), "45 min of 1h 30m");
  assert.deepEqual([1, 2, 3, 4, 11, 12, 13, 21, 22, 23, 101].map(ordinal), ["1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "23rd", "101st"]);
});

test("weekend limits", () => {
  const site = { ...newSite("youtube.com", 30), weekendMinutes: 60 };
  assert.equal(limitOn(site, at(2026, 11, 26)), 30, "Thursday");
  assert.equal(limitOn(site, at(2026, 11, 28)), 60, "Saturday");
  assert.equal(limitOn({ ...site, weekendMinutes: null }, at(2026, 11, 28)), 30);
});

test("when a site is paused, and why", () => {
  const now = at(2026, 11, 26, 15);
  const s = settingsWith();
  const day = emptyDay();
  assert.equal(blockFor(s, day, "youtube.com", null, now), null);

  day.ms["youtube.com"] = 29 * MIN + 59_000;
  assert.equal(blockFor(s, day, "youtube.com", null, now), null, "29:59 is still under 30 min");
  day.ms["youtube.com"] = 30 * MIN;
  assert.equal(blockFor(s, day, "youtube.com", null, now)?.reason, "limit");

  day.passes["youtube.com"] = now + 5 * MIN;
  assert.equal(blockFor(s, day, "youtube.com", null, now), null, "5 more minutes");
  assert.equal(blockFor(s, day, "youtube.com", null, now + 5 * MIN)?.reason, "limit", "...then paused again");

  const off = settingsWith({ sites: [{ ...newSite("youtube.com", 30), enabled: false }] });
  assert.equal(blockFor(off, day, "youtube.com", null, now), null, "a switched-off limit never pauses");
});

test("visits, shared budgets, Reels and schedules", () => {
  const now = at(2026, 11, 26, 15);
  const day = emptyDay();

  const opens = settingsWith({ sites: [{ ...newSite("reddit.com", 60), maxOpens: 3 }] });
  day.opens["reddit.com"] = 3;
  assert.equal(blockFor(opens, day, "reddit.com", null, now), null, "the 3rd visit is fine");
  day.opens["reddit.com"] = 4;
  assert.equal(blockFor(opens, day, "reddit.com", null, now)?.reason, "opens");

  const budget = settingsWith({ budgets: [{ id: "b1", name: "Social", minutes: 40, enabled: true, sites: ["youtube.com", "reddit.com"] }] });
  const d2 = emptyDay();
  d2.ms["youtube.com"] = 25 * MIN;
  d2.ms["reddit.com"] = 15 * MIN;
  const b = blockFor(budget, d2, "youtube.com", null, now);
  assert.equal(b?.reason, "budget");
  assert.equal(b?.budget?.name, "Social");

  const reels = settingsWith({ features: [{ id: "youtube_shorts", minutes: 10, enabled: true }, { id: "instagram_reels", minutes: 10, enabled: false }] });
  const d3 = emptyDay();
  d3.featureMs.youtube_shorts = 10 * MIN;
  assert.equal(blockFor(reels, d3, "youtube.com", "youtube_shorts", now)?.reason, "reels");
  assert.equal(blockFor(reels, d3, "youtube.com", null, now), null, "normal videos still play");

  const bed = settingsWith({ bedtime: { enabled: true, start: 22 * 60, end: 7 * 60 } });
  assert.equal(blockFor(bed, emptyDay(), "youtube.com", null, at(2026, 11, 26, 23))?.reason, "bedtime");
  assert.equal(blockFor(bed, emptyDay(), "youtube.com", null, at(2026, 11, 26, 12)), null);
});

test("checking again right on time", () => {
  const now = at(2026, 11, 26, 15);
  const s = settingsWith();
  const day = emptyDay();
  day.ms["youtube.com"] = 28 * MIN;
  assert.equal(msUntilChange(s, day, "youtube.com", null, now), 2 * MIN);
  const bed = settingsWith({ bedtime: { enabled: true, start: 15 * 60 + 1, end: 7 * 60 } });
  assert.equal(msUntilChange(bed, day, "youtube.com", null, now), MIN, "bedtime starts in a minute");
});

test("mood follows the app's steps", () => {
  assert.equal(moodFrom(0, 100, false), 1);
  assert.equal(moodFrom(25, 100, false), 2);
  assert.equal(moodFrom(50, 100, false), 3);
  assert.equal(moodFrom(80, 100, false), 4);
  assert.equal(moodFrom(100, 100, false), 5);
  assert.equal(moodFrom(10, 100, true), 4, "one site at its limit = at least worried");
  assert.equal(moodFrom(0, 0, false), 1, "no limits = thriving");

  const s = settingsWith();
  const day = emptyDay();
  day.ms["reddit.com"] = 20 * MIN;
  const m = moodToday(s, day, at(2026, 11, 26));
  assert.deepEqual([m.level, m.used, m.limit, m.atLimit], [4, 20, 50, "Reddit"]);
});

test("counting time, split at midnight, and not across sleep", () => {
  const since = at(2026, 11, 26, 23, 59);
  const parts = creditTime({ site: "youtube.com", feature: null, tabId: 1, since }, since + 90_000);
  assert.deepEqual(parts, [
    { key: dayKey(since), ms: 60_000 },
    { key: "2026-11-27", ms: 30_000 },
  ]);
  assert.deepEqual(creditTime({ site: null, feature: null, tabId: null, since }, since + 10_000), []);
  assert.deepEqual(creditTime({ site: "x.com", feature: null, tabId: 1, since }, since + MAX_GAP_MS + 1), [], "the computer slept");
});

test("Beast cool-down", () => {
  const setup = at(2026, 11, 1);
  const now = at(2026, 11, 26, 12);
  assert.equal(guardState(0, now, setup).kind, "locked");
  assert.equal(guardState(0, setup + 10 * MIN, setup).kind, "settling", "first hour after setup");
  assert.equal(guardState(0, setup + SETTLING_MS, setup).kind, "locked");
  assert.deepEqual(guardState(now + WAIT_MS, now, setup), { kind: "waiting", msLeft: WAIT_MS });
  assert.equal(allowsLoosening(guardState(now, now + MIN, setup)), true);
  assert.equal(guardState(now, now + WINDOW_MS, setup).kind, "locked", "the window closed");
});

test("what counts as loosening", () => {
  const old = settingsWith({ strictness: "beast", hide: { ...defaultSettings().hide, youtubeShorts: true } });
  assert.deepEqual(loosenings(old, structuredClone(old)), []);
  const stricter = structuredClone(old);
  stricter.sites[0].minutes = 10;
  stricter.bedtime = { enabled: true, start: 22 * 60, end: 7 * 60 };
  assert.deepEqual(loosenings(old, stricter), []);

  const looser = structuredClone(old);
  looser.sites[0].minutes = 60;
  looser.sites[1].enabled = false;
  looser.strictness = "firm";
  looser.hide.youtubeShorts = false;
  assert.deepEqual(loosenings(old, looser).sort(), ["Reddit without a limit", "a gentler strictness", "more time on YouTube", "showing hidden feeds again"].sort());

  const bed = settingsWith({ bedtime: { enabled: true, start: 22 * 60, end: 7 * 60 } });
  assert.deepEqual(loosenings(bed, { ...bed, bedtime: { enabled: true, start: 23 * 60, end: 7 * 60 } }), ["a shorter bedtime"]);
  assert.deepEqual(loosenings(bed, { ...bed, bedtime: { enabled: true, start: 21 * 60, end: 8 * 60 } }), []);
});

test("damaged settings are cleaned", () => {
  const s = cleanSettings({ sites: [{ id: "YouTube.com", minutes: 9999 }, { id: "bad id" }, { id: "youtube.com" }], strictness: "nope", budgets: [{ name: "X", sites: ["youtube.com", "gone.com"], minutes: 3 }] });
  assert.equal(s.sites.length, 1);
  assert.equal(s.sites[0].id, "youtube.com");
  assert.equal(s.sites[0].minutes, 300);
  assert.deepEqual(s.sites[0].hosts, ["youtube.com", "youtu.be"]);
  assert.equal(s.strictness, "firm");
  assert.deepEqual(s.budgets[0].sites, ["youtube.com"]);
  assert.equal(s.budgets[0].minutes, 5);
  assert.deepEqual(cleanSettings(null), defaultSettings());
});
