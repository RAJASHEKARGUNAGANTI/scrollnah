// What the user set up: sites and their limits, shared budgets, Reels & Shorts limits, strictness,
// bedtime, work hours, the "hide" switches and the mascot. Saved in the browser (storage.local).

import { FEATURES, hostsForSite, nameForSite } from "./sites.ts";
import type { Schedule } from "./time.ts";

export type Strictness = "chill" | "firm" | "beast";
export type MascotId = "blobby" | "bloomy";
export type FeatureId = "youtube_shorts" | "instagram_reels";
export type HideId = "youtubeShorts" | "youtubeHome" | "instagramReels" | "instagramExplore" | "xForYou";

export interface SiteLimit {
  id: string; // main address, e.g. "youtube.com"
  name: string;
  hosts: string[];
  minutes: number; // daily limit
  weekendMinutes: number | null; // a different limit on Saturday and Sunday (null = same every day)
  maxOpens: number | null; // how many times a day it may be opened (null = no open limit)
  enabled: boolean;
}

/** A few sites with one limit together, e.g. "Social" = Instagram + Reddit, 60 min. Each keeps its own limit too. */
export interface Budget {
  id: string;
  name: string;
  minutes: number;
  enabled: boolean;
  sites: string[];
}

export interface FeatureLimit {
  id: FeatureId;
  minutes: number;
  enabled: boolean;
}

export interface Settings {
  setupDone: boolean;
  setupAt: number; // for Beast mode's "settling in" hour
  name: string;
  mascot: MascotId;
  flowers: number; // Bloomy's flowers (0-4), brought over from the phone
  strictness: Strictness;
  sites: SiteLimit[];
  budgets: Budget[];
  features: FeatureLimit[];
  bedtime: Schedule;
  work: Schedule;
  hide: Record<HideId, boolean>;
  imported: { at: number; appVersion: string } | null; // the last import from the phone app
}

export const MINUTES = { min: 5, max: 300, step: 5 };
export const OPENS = { min: 1, max: 50 };

export const STRICTNESS: { id: Strictness; title: string; text: string }[] = [
  { id: "chill", title: "Chill", text: "A gentle nudge when time's up" },
  { id: "firm", title: "Firm", text: "The Pause page, with 5 more minutes if you need them" },
  { id: "beast", title: "Beast", text: "Locked until midnight. No way around it" },
];

export const HIDE_OPTIONS: { id: HideId; site: string; title: string; text: string }[] = [
  { id: "youtubeShorts", site: "youtube.com", title: "Hide YouTube Shorts", text: "Shorts shelves, the Shorts tab and Shorts pages" },
  { id: "youtubeHome", site: "youtube.com", title: "Hide the YouTube home feed", text: "Search and your subscriptions still work" },
  { id: "instagramReels", site: "instagram.com", title: "Hide Instagram Reels", text: "The Reels tab and the Reels feed" },
  { id: "instagramExplore", site: "instagram.com", title: "Hide Instagram Explore", text: "The endless grid of suggestions" },
  { id: "xForYou", site: "x.com", title: "Hide X's \"For you\"", text: "Shows \"Following\" instead" },
];

export const DEFAULT_LIMIT = 30;

export function defaultSettings(): Settings {
  return {
    setupDone: false,
    setupAt: 0,
    name: "",
    mascot: "blobby",
    flowers: 3,
    strictness: "firm",
    sites: [],
    budgets: [],
    features: FEATURES.map((f) => ({ id: f.id, minutes: 15, enabled: false })),
    bedtime: { enabled: false, start: 22 * 60, end: 7 * 60 },
    work: { enabled: false, start: 9 * 60, end: 17 * 60, days: [1, 2, 3, 4, 5] },
    hide: { youtubeShorts: false, youtubeHome: false, instagramReels: false, instagramExplore: false, xForYou: false },
    imported: null,
  };
}

/** A new site entry with the default limit. */
export function newSite(id: string, minutes = DEFAULT_LIMIT): SiteLimit {
  return { id, name: nameForSite(id), hosts: hostsForSite(id), minutes, weekendMinutes: null, maxOpens: null, enabled: true };
}

// ---- Cleaning: settings come from storage or an imported file, so every value is checked ----

function num(v: unknown, min: number, max: number, fallback: number, step = 1): number {
  const n = typeof v === "number" ? v : typeof v === "string" ? Number(v) : NaN;
  if (!Number.isFinite(n)) return fallback;
  const rounded = Math.round(n / step) * step;
  return Math.min(max, Math.max(min, rounded));
}

function optNum(v: unknown, min: number, max: number, step = 1): number | null {
  return v === null || v === undefined || v === "" ? null : num(v, min, max, min, step);
}

function bool(v: unknown, fallback: boolean): boolean {
  if (typeof v === "boolean") return v;
  if (v === 1 || v === 0) return v === 1;
  return fallback;
}

function text(v: unknown, max: number, fallback = ""): string {
  return typeof v === "string" ? v.trim().slice(0, max) : fallback;
}

export function cleanMinutes(v: unknown, fallback = DEFAULT_LIMIT): number {
  return num(v, MINUTES.min, MINUTES.max, fallback, MINUTES.step);
}

function cleanSchedule(v: unknown, fallback: Schedule): Schedule {
  if (!v || typeof v !== "object") return { ...fallback };
  const o = v as Record<string, unknown>;
  const out: Schedule = {
    enabled: bool(o.enabled, fallback.enabled),
    start: num(o.start, 0, 1439, fallback.start),
    end: num(o.end, 0, 1439, fallback.end),
  };
  if (fallback.days) {
    const days = Array.isArray(o.days) ? o.days.map((d) => Number(d)).filter((d) => Number.isInteger(d) && d >= 1 && d <= 7) : fallback.days;
    out.days = [...new Set(days)].sort();
  }
  return out;
}

function cleanSite(v: unknown): SiteLimit | null {
  if (!v || typeof v !== "object") return null;
  const o = v as Record<string, unknown>;
  const id = text(o.id, 253).toLowerCase();
  if (!/^[a-z0-9.-]+\.[a-z0-9-]+$/.test(id)) return null;
  const hosts = Array.isArray(o.hosts)
    ? o.hosts.filter((h): h is string => typeof h === "string" && /^[a-z0-9.-]+$/.test(h)).slice(0, 10)
    : [];
  return {
    id,
    name: text(o.name, 40) || nameForSite(id),
    hosts: hosts.length ? hosts : hostsForSite(id),
    minutes: cleanMinutes(o.minutes),
    weekendMinutes: optNum(o.weekendMinutes, MINUTES.min, MINUTES.max, MINUTES.step),
    maxOpens: optNum(o.maxOpens, OPENS.min, OPENS.max),
    enabled: bool(o.enabled, true),
  };
}

/** Turns whatever is stored (maybe from an older version, maybe damaged) into valid settings. */
export function cleanSettings(raw: unknown): Settings {
  const d = defaultSettings();
  if (!raw || typeof raw !== "object") return d;
  const o = raw as Record<string, unknown>;

  const sites: SiteLimit[] = [];
  for (const s of Array.isArray(o.sites) ? o.sites : []) {
    const site = cleanSite(s);
    if (site && !sites.some((x) => x.id === site.id)) sites.push(site);
  }
  const siteIds = new Set(sites.map((s) => s.id));

  const budgets: Budget[] = [];
  for (const b of Array.isArray(o.budgets) ? o.budgets : []) {
    if (!b || typeof b !== "object") continue;
    const r = b as Record<string, unknown>;
    const members = Array.isArray(r.sites) ? r.sites.filter((x): x is string => typeof x === "string" && siteIds.has(x)) : [];
    budgets.push({
      id: text(r.id, 40) || `b${budgets.length + 1}`,
      name: text(r.name, 30) || "Shared budget",
      minutes: cleanMinutes(r.minutes, 60),
      enabled: bool(r.enabled, true),
      sites: [...new Set(members)],
    });
  }

  const storedFeatures = Array.isArray(o.features) ? (o.features as Record<string, unknown>[]) : [];
  const features = d.features.map((f) => {
    const r = storedFeatures.find((x) => x && x.id === f.id);
    return r ? { id: f.id, minutes: num(r.minutes, 5, 180, f.minutes, 5), enabled: bool(r.enabled, false) } : f;
  });

  const hideRaw = (o.hide && typeof o.hide === "object" ? o.hide : {}) as Record<string, unknown>;
  const hide = { ...d.hide };
  for (const k of Object.keys(hide) as HideId[]) hide[k] = bool(hideRaw[k], false);

  const imp = o.imported as Record<string, unknown> | null | undefined;
  return {
    setupDone: bool(o.setupDone, false),
    setupAt: num(o.setupAt, 0, Number.MAX_SAFE_INTEGER, 0),
    name: text(o.name, 30),
    mascot: o.mascot === "bloomy" ? "bloomy" : "blobby",
    flowers: num(o.flowers, 0, 4, d.flowers),
    strictness: o.strictness === "chill" || o.strictness === "beast" ? o.strictness : "firm",
    sites,
    budgets,
    features,
    bedtime: cleanSchedule(o.bedtime, d.bedtime),
    work: cleanSchedule(o.work, d.work),
    hide,
    imported: imp && typeof imp === "object" ? { at: num(imp.at, 0, Number.MAX_SAFE_INTEGER, 0), appVersion: text(imp.appVersion, 20) } : null,
  };
}

export const MASCOT_NAME: Record<MascotId, string> = { blobby: "Blobby", bloomy: "Bloomy" };
