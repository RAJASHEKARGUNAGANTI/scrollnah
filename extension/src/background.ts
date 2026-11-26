// The background script: counts time on the user's sites, shows the Pause page or a nudge when a
// limit is reached, and keeps the toolbar mascot and badge up to date.
//
// Fair counting: time counts only while a site is the tab in front, in the browser window you're using,
// and you're at the computer (no keyboard or mouse for 60 s = away, unless the tab is playing sound).
// Background tabs don't count. Everything is saved in the browser; nothing is sent anywhere.
//
// Chrome may stop this script when it's quiet, so nothing important lives in variables: the current
// stretch of time ("live") is in storage.session and every event starts by reading it back.

import { mascotDataUrl, mascotImageData } from "./lib/art.ts";
import { ext } from "./lib/ext.ts";
import { hiddenPageFor } from "./lib/hide.ts";
import { nudgeText } from "./lib/lines.ts";
import { blockFor, minutes, moodPill, moodToday, msUntilChange, limitOn, type Block, type DayData } from "./lib/rules.ts";
import type { FeatureId, Settings } from "./lib/settings.ts";
import { featureOfUrl, patternsFor, siteOfUrl } from "./lib/sites.ts";
import { loadDay, loadSettings, pruneDays, saveDay } from "./lib/store.ts";
import { dayKey, nextMidnight } from "./lib/time.ts";
import { creditTime, type Live } from "./lib/track.ts";

const IDLE_SECONDS = 60;
const PASS_MS = 5 * 60_000;
const NUDGE_EVERY = 10; // Chill: another nudge every 10 minutes over
const PAUSE_PAGE = "pages/pause.html";

// ---- Run one thing at a time (events can arrive together; each reads and writes storage) ----

let queue: Promise<unknown> = Promise.resolve();
function serial<T>(job: () => Promise<T>): Promise<T> {
  const run = queue.then(job, job);
  queue = run.catch((e) => console.error("ScrollNah:", e));
  return run;
}

// ---- Small saved state for this browser session ----

async function loadLive(): Promise<Live> {
  const r = await ext.storage.session.get(["live"]);
  return (r.live as Live) ?? { site: null, feature: null, tabId: null, since: Date.now() };
}

/** The site each tab was last seen on (in front), to count visits: a tab arriving on a site = 1 visit. */
async function loadLastSeen(): Promise<Record<string, string | null>> {
  const r = await ext.storage.session.get(["lastSeen"]);
  return (r.lastSeen as Record<string, string | null>) ?? {};
}

// ---- What's in front ----

interface Front {
  tabId: number;
  url: string;
  site: string;
  feature: FeatureId | null;
  counting: boolean; // false while you're away from the computer
}

function enabledSites(settings: Settings) {
  return settings.sites.filter((s) => s.enabled);
}

async function whatIsInFront(settings: Settings): Promise<Front | null> {
  let win;
  try {
    win = await ext.windows.getLastFocused();
  } catch {
    return null;
  }
  if (!win?.focused || win.id === undefined) return null;
  const [tab] = await ext.tabs.query({ active: true, windowId: win.id });
  if (!tab || tab.id === undefined || !tab.url) return null; // no url = a site ScrollNah has no access to
  const site = siteOfUrl(tab.url, enabledSites(settings));
  if (!site) return null;
  const idle = await ext.idle.queryState(IDLE_SECONDS);
  return {
    tabId: tab.id,
    url: tab.url,
    site,
    feature: featureOfUrl(tab.url)?.id ?? null,
    counting: idle === "active" || (idle === "idle" && !!tab.audible),
  };
}

// ---- The main check: runs on every tab, window, idle and alarm event ----

async function refresh(): Promise<{ msLeft: number }> {
  const now = Date.now();
  const settings = await loadSettings();
  const live = await loadLive();
  const todayKey = dayKey(now);
  const day = await loadDay(todayKey);

  // 1. Count the time since the last check for whatever was in front then.
  for (const part of creditTime(live, now)) {
    const target = part.key === todayKey ? day : await loadDay(part.key);
    target.ms[live.site!] = (target.ms[live.site!] ?? 0) + part.ms;
    if (live.feature) target.featureMs[live.feature] = (target.featureMs[live.feature] ?? 0) + part.ms;
    if (target !== day) await saveDay(part.key, target);
  }

  const front = settings.setupDone ? await whatIsInFront(settings) : null;

  // 2. A tab arriving on a site counts as a visit (switching between tabs doesn't).
  if (front) {
    const lastSeen = await loadLastSeen();
    if (lastSeen[front.tabId] !== front.site) {
      day.opens[front.site] = (day.opens[front.site] ?? 0) + 1;
      lastSeen[front.tabId] = front.site;
      await ext.storage.session.set({ lastSeen });
    }
  }

  // 3. Over a limit? Firm and Beast show the Pause page, Chill sends a nudge.
  let msLeft = 0;
  let paused = false;
  if (front) {
    const block = blockFor(settings, day, front.site, front.feature, now);
    if (block) paused = await act(block, front, settings, day);
    else msLeft = msUntilChange(settings, day, front.site, front.feature, now);
  }
  await saveDay(todayKey, day);

  // 4. Start the next stretch of time.
  const counting = front && front.counting && !paused;
  const next: Live = counting
    ? { site: front.site, feature: front.feature, tabId: front.tabId, since: now }
    : { site: null, feature: null, tabId: null, since: now };
  await ext.storage.session.set({ live: next });

  // 5. Check again right when something changes; tick every minute only while a site is in front.
  if (front && !paused) {
    if (!(await ext.alarms.get("tick"))) await ext.alarms.create("tick", { periodInMinutes: 1 });
    if (msLeft > 0) await ext.alarms.create("check", { when: now + msLeft + 500 });
  } else {
    await ext.alarms.clear("tick");
    await ext.alarms.clear("check");
  }
  const midnight = nextMidnight(now) + 1000;
  if ((await ext.alarms.get("midnight"))?.scheduledTime !== midnight) await ext.alarms.create("midnight", { when: midnight });

  await updateToolbar(settings, day, paused ? null : front, now);
  return { msLeft: paused ? 0 : msLeft };
}

/** Shows the Pause page (or a Chill nudge). True if the tab was sent to the Pause page. */
async function act(block: Block, front: Front, settings: Settings, day: DayData): Promise<boolean> {
  const schedule = block.reason === "bedtime" || block.reason === "work";
  if (settings.strictness === "chill" && !schedule) {
    await nudge(block, settings, day);
    return false;
  }
  const params = new URLSearchParams({ site: block.site, reason: block.reason, back: front.url });
  if (block.budget) params.set("budget", block.budget.id);
  if (block.feature) params.set("feature", block.feature);
  await ext.tabs.update(front.tabId, { url: `${ext.runtime.getURL(PAUSE_PAGE)}?${params}` });
  return true;
}

/** Chill mode: a notification from the mascot when a limit is reached, then every 10 minutes over. */
async function nudge(block: Block, settings: Settings, day: DayData) {
  const site = settings.sites.find((s) => s.id === block.site);
  if (!site) return;
  const key = `${block.reason}:${block.budget?.id ?? block.feature ?? block.site}`;
  const used =
    block.reason === "opens" ? (day.opens[block.site] ?? 0)
    : block.reason === "reels" && block.feature ? minutes(day.featureMs[block.feature])
    : block.budget ? block.budget.sites.reduce((sum, id) => sum + minutes(day.ms[id]), 0)
    : minutes(day.ms[block.site]);
  const last = day.nudged[key];
  const due = last === undefined || (block.reason === "opens" ? used > last : used - last >= NUDGE_EVERY);
  if (!due) return;
  day.nudged[key] = used;
  const over = block.reason === "opens" ? 0 : Math.max(0, used - (block.limit ?? used));
  const text = nudgeText(settings.mascot, block.budget?.name ?? site.name, block.reason, block.limit, over);
  try {
    const iconUrl = await mascotDataUrl(128, { mascot: settings.mascot, mood: over > 0 ? 5 : 4, flowers: settings.flowers });
    await ext.notifications.create(`nudge-${key}`, { type: "basic", iconUrl, title: text.title, message: text.message });
  } catch (e) {
    console.warn("ScrollNah: nudge failed", e);
  }
}

// ---- Toolbar: the mascot in today's mood, and minutes left on the site in front ----

async function updateToolbar(settings: Settings, day: DayData, front: Front | null, now: number) {
  const mood = moodToday(settings, day, now);
  const look = `${settings.mascot}-${mood.level}-${settings.flowers}`;
  const r = await ext.storage.session.get(["iconLook"]);
  if (r.iconLook !== look) {
    try {
      const art = { mascot: settings.mascot, mood: mood.level, flowers: settings.flowers };
      await ext.action.setIcon({ imageData: { 16: mascotImageData(16, art), 32: mascotImageData(32, art) } });
      await ext.storage.session.set({ iconLook: look });
    } catch (e) {
      console.warn("ScrollNah: icon failed", e);
    }
  }

  let text = "";
  let worried = false;
  if (front) {
    const site = settings.sites.find((s) => s.id === front.site)!;
    const limit = limitOn(site, now);
    let left = limit - minutes(day.ms[site.id]);
    for (const b of settings.budgets) {
      if (b.enabled && b.sites.includes(site.id)) {
        const used = b.sites.reduce((sum, id) => sum + (settings.sites.find((s) => s.id === id)?.enabled ? minutes(day.ms[id]) : 0), 0);
        left = Math.min(left, b.minutes - used);
      }
    }
    text = left > 0 ? `${Math.min(left, 999)}m` : "0";
    worried = left <= Math.max(2, limit * 0.2);
  }
  await ext.action.setBadgeText({ text });
  if (text) {
    await ext.action.setBadgeBackgroundColor({ color: worried ? "#FF6B35" : "#C6F432" });
    await ext.action.setBadgeTextColor?.({ color: "#16131F" });
  }
  const left = mood.limit - mood.used;
  const summary = !settings.setupDone
    ? "Finish setting up ScrollNah"
    : mood.limit === 0 ? "No limits yet"
    : left >= 0 ? `${left} min left today` : `${-left} min over today`;
  await ext.action.setTitle({ title: `ScrollNah · ${moodPill(settings, mood.level)} · ${summary}` });
}

// ---- Content script: registered only for the user's sites that ScrollNah has access to ----

async function syncContentScripts() {
  const settings = await loadSettings();
  const patterns: string[] = [];
  for (const s of settings.sites) {
    const p = patternsFor(s.hosts);
    if (await ext.permissions.contains({ origins: p }).catch(() => false)) patterns.push(...p);
  }
  await ext.scripting.unregisterContentScripts({ ids: ["scrollnah"] }).catch(() => {});
  if (patterns.length === 0) return;
  await ext.scripting.registerContentScripts([
    { id: "scrollnah", matches: patterns, js: ["content.js"], runAt: "document_start", allFrames: false, persistAcrossSessions: false },
  ]);
  // Tabs that were already open get it now too (it ignores a second copy).
  for (const tab of await ext.tabs.query({})) {
    if (tab.id !== undefined && siteOfUrl(tab.url, settings.sites)) {
      await ext.scripting.executeScript({ target: { tabId: tab.id }, files: ["content.js"] }).catch(() => {});
    }
  }
}

// ---- Tabs ----

async function onTabUpdated(tabId: number, url: string | undefined, active: boolean) {
  const settings = await loadSettings();
  if (settings.setupDone && url) {
    const hidden = hiddenPageFor(url, settings.hide);
    if (hidden) {
      const params = new URLSearchParams({ reason: "hidden", site: hidden.site, what: hidden.what, home: hidden.home });
      if (hidden.watch) params.set("watch", hidden.watch);
      await ext.tabs.update(tabId, { url: `${ext.runtime.getURL(PAUSE_PAGE)}?${params}` });
      return;
    }
  }
  // Leaving a site resets the tab's visit, so coming back counts as a new visit. Our own Pause page
  // doesn't (after "5 more minutes" you're back on the same visit).
  if (!url?.startsWith(ext.runtime.getURL(""))) {
    const lastSeen = await loadLastSeen();
    const now = siteOfUrl(url, enabledSites(settings));
    if (lastSeen[tabId] && lastSeen[tabId] !== now) {
      lastSeen[tabId] = null;
      await ext.storage.session.set({ lastSeen });
    }
  }
  if (active) await refresh();
}

// ---- Messages from the pages and the content script ----

type Message =
  | { type: "beat" }
  | { type: "flush" }
  | { type: "pass"; site: string }
  | { type: "logPause"; site: string; why: string | null; choice: "left" | "more" };

async function handle(msg: Message): Promise<unknown> {
  switch (msg.type) {
    case "beat":
    case "flush":
      return refresh();
    case "pass": {
      const settings = await loadSettings();
      if (settings.strictness === "beast" || !settings.sites.some((s) => s.id === msg.site)) return { ok: false };
      const key = dayKey(Date.now());
      const day = await loadDay(key);
      day.passes[msg.site] = Date.now() + PASS_MS;
      await saveDay(key, day);
      return { ok: true };
    }
    case "logPause": {
      const key = dayKey(Date.now());
      const day = await loadDay(key);
      day.pauses.push({ at: Date.now(), site: String(msg.site).slice(0, 253), why: msg.why ? String(msg.why).slice(0, 40) : null, choice: msg.choice === "more" ? "more" : "left" });
      day.pauses = day.pauses.slice(-200);
      await saveDay(key, day);
      return { ok: true };
    }
  }
  return null;
}

// ---- Listeners (registered right away, as Manifest V3 requires) ----

ext.idle.setDetectionInterval(IDLE_SECONDS);

ext.runtime.onInstalled.addListener((details) => {
  serial(async () => {
    if (details.reason === "install") await ext.tabs.create({ url: ext.runtime.getURL("pages/welcome.html") });
    await syncContentScripts();
    await refresh();
  });
});
ext.runtime.onStartup.addListener(() => {
  serial(async () => {
    await syncContentScripts();
    await refresh();
  });
});

ext.tabs.onActivated.addListener(() => void serial(refresh));
ext.tabs.onUpdated.addListener((tabId, change, tab) => {
  if (change.url !== undefined || change.status === "loading" || change.audible !== undefined) {
    void serial(() => onTabUpdated(tabId, tab.url, tab.active));
  }
});
ext.tabs.onRemoved.addListener((tabId) => {
  void serial(async () => {
    const lastSeen = await loadLastSeen();
    if (tabId in lastSeen) {
      delete lastSeen[tabId];
      await ext.storage.session.set({ lastSeen });
    }
  });
});
ext.windows.onFocusChanged.addListener(() => void serial(refresh));
ext.idle.onStateChanged.addListener(() => void serial(refresh));

ext.alarms.onAlarm.addListener((alarm) => {
  void serial(async () => {
    if (alarm.name === "midnight") await pruneDays();
    await refresh();
  });
});

ext.storage.onChanged.addListener((changes, area) => {
  if (area === "local" && changes.settings) {
    void serial(async () => {
      await syncContentScripts();
      await refresh();
    });
  }
});
ext.permissions.onAdded.addListener(() => void serial(syncContentScripts));
ext.permissions.onRemoved.addListener(() => void serial(syncContentScripts));

ext.notifications.onClicked.addListener((id) => void ext.notifications.clear(id));

ext.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (!msg || typeof msg !== "object" || typeof msg.type !== "string") return;
  serial(() => handle(msg as Message)).then(sendResponse, () => sendResponse(null));
  return true; // the answer comes later
});
