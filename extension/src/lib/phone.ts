// "Import from phone": reads the file the ScrollNah app makes (Settings > Backup > "Copy limits to
// computer"), or a full app backup, and turns the phone's limits into website limits.
// Only limits and settings are read; a full backup's history is ignored and never stored here.

import { cleanMinutes, cleanSettings, newSite, OPENS, type Budget, type FeatureId, type FeatureLimit, type MascotId, type Settings, type SiteLimit, type Strictness } from "./settings.ts";
import { siteForApp } from "./sites.ts";
import type { Schedule } from "./time.ts";

export class ImportError extends Error {}

export interface ImportedSite {
  site: SiteLimit;
  appNames: string[]; // the phone app(s) it came from
}

export interface PhoneImport {
  appVersion: string;
  exportedAt: number;
  name: string | null;
  mascot: MascotId | null;
  flowers: number | null;
  strictness: Strictness | null;
  bedtime: Schedule | null;
  work: Schedule | null;
  sites: ImportedSite[];
  skipped: string[]; // phone apps with no website, e.g. a game
  budgets: Omit<Budget, "id">[];
  features: FeatureLimit[];
}

const MAX_FILE = 30 * 1024 * 1024;

const isObj = (v: unknown): v is Record<string, unknown> => !!v && typeof v === "object" && !Array.isArray(v);
const asBool = (v: unknown, fallback = false) => (typeof v === "boolean" ? v : v === 1 ? true : v === 0 ? false : fallback);
const asNum = (v: unknown): number | null => (typeof v === "number" && Number.isFinite(v) ? v : typeof v === "string" && v.trim() !== "" && Number.isFinite(Number(v)) ? Number(v) : null);
const asStr = (v: unknown) => (typeof v === "string" ? v : "");

function strictnessOf(v: unknown): Strictness | null {
  const s = asStr(v).toLowerCase();
  return s === "chill" || s === "firm" || s === "beast" ? s : null;
}

function scheduleOf(v: unknown, withDays: boolean): Schedule | null {
  if (!isObj(v)) return null;
  const start = asNum(v.start);
  const end = asNum(v.end);
  if (start === null || end === null) return null;
  const s: Schedule = { enabled: asBool(v.enabled), start: Math.max(0, Math.min(1439, start)), end: Math.max(0, Math.min(1439, end)) };
  if (withDays) {
    s.days = Array.isArray(v.days) ? v.days.map(Number).filter((d) => Number.isInteger(d) && d >= 1 && d <= 7) : [1, 2, 3, 4, 5];
  }
  return s;
}

interface RawApp {
  packageName: string;
  appName: string;
  limitMinutes: number | null;
  weekendLimitMinutes: number | null;
  maxOpens: number | null;
  enabled: boolean;
}

/** Reads the app's file. Throws ImportError with a message for people when it isn't one. */
export function parsePhoneFile(text: string): PhoneImport {
  if (text.length > MAX_FILE) throw new ImportError("That file is too big to be from ScrollNah.");
  let json: unknown;
  try {
    json = JSON.parse(text);
  } catch {
    throw new ImportError("That isn't a ScrollNah file. In the app: Settings > Backup > Copy limits to computer.");
  }
  if (!isObj(json)) throw new ImportError("That isn't a ScrollNah file.");

  let apps: RawApp[];
  let groups: { name: string; minutes: number | null; enabled: boolean; packages: string[] }[];
  let features: { id: string; minutes: number | null; enabled: boolean }[];
  let name: string | null = null;
  let mascot: MascotId | null = null;
  let flowers: number | null = null;
  let strictness: Strictness | null = null;

  if (json.app === "ScrollNah limits") {
    // The small file made for computers.
    if ((asNum(json.format) ?? 1) > 1) throw new ImportError("This file is from a newer ScrollNah app. Update the extension first.");
    apps = (Array.isArray(json.apps) ? json.apps : []).filter(isObj).map((a) => ({
      packageName: asStr(a.packageName),
      appName: asStr(a.appName),
      limitMinutes: asNum(a.limitMinutes),
      weekendLimitMinutes: asNum(a.weekendLimitMinutes),
      maxOpens: asNum(a.maxOpens),
      enabled: asBool(a.enabled, true),
    }));
    groups = (Array.isArray(json.groups) ? json.groups : []).filter(isObj).map((g) => ({
      name: asStr(g.name),
      minutes: asNum(g.limitMinutes),
      enabled: asBool(g.enabled, true),
      packages: Array.isArray(g.packages) ? g.packages.map(asStr).filter(Boolean) : [],
    }));
    features = (Array.isArray(json.features) ? json.features : []).filter(isObj).map((f) => ({ id: asStr(f.id), minutes: asNum(f.limitMinutes), enabled: asBool(f.enabled) }));
    name = asStr(json.name) || null;
    mascot = asStr(json.mascot).toUpperCase() === "BLOOMY" ? "bloomy" : asStr(json.mascot) ? "blobby" : null;
    flowers = asNum(json.flowers);
    strictness = strictnessOf(json.strictness);
  } else if (json.app === "ScrollNah" && isObj(json.tables)) {
    // A full backup from the app: only its limits and a few settings are read.
    const tables = json.tables;
    const rows = (t: string) => (Array.isArray(tables[t]) ? (tables[t] as unknown[]).filter(isObj) : []);
    apps = rows("tracked_apps").map((a) => ({
      packageName: asStr(a.packageName),
      appName: asStr(a.appName),
      limitMinutes: asNum(a.limitMinutes),
      weekendLimitMinutes: asNum(a.weekendLimitMinutes),
      maxOpens: asNum(a.maxOpens),
      enabled: asBool(a.enabled, true),
    }));
    groups = rows("app_groups").map((g) => ({
      name: asStr(g.name),
      minutes: asNum(g.limitMinutes),
      enabled: asBool(g.enabled, true),
      packages: asStr(g.packages).split(",").map((p) => p.trim()).filter(Boolean),
    }));
    features = rows("feature_limits").map((f) => ({ id: asStr(f.featureId), minutes: asNum(f.limitMinutes), enabled: asBool(f.enabled) }));
    const settings = isObj(json.settings) ? json.settings : {};
    const setting = (key: string) => (isObj(settings[key]) ? (settings[key] as Record<string, unknown>).v : undefined);
    name = asStr(setting("user_name")) || null;
    const theme = asStr(setting("mascot_theme"));
    mascot = theme === "BLOOMY" ? "bloomy" : theme === "BLOBBY" ? "blobby" : null;
    flowers = asNum(setting("pot_flowers"));
    strictness = strictnessOf(setting("strictness"));
  } else {
    throw new ImportError("That isn't a ScrollNah file. In the app: Settings > Backup > Copy limits to computer.");
  }

  // Phone apps -> websites. Two apps for one site (Facebook and Facebook Lite) keep the stricter limit.
  const sites: ImportedSite[] = [];
  const skipped: string[] = [];
  for (const a of apps) {
    const known = siteForApp(a.packageName);
    if (!known) {
      if (a.appName) skipped.push(a.appName);
      continue;
    }
    const site = newSite(known.id, cleanMinutes(a.limitMinutes));
    site.enabled = a.enabled;
    site.weekendMinutes = a.weekendLimitMinutes === null ? null : cleanMinutes(a.weekendLimitMinutes);
    site.maxOpens = a.maxOpens === null ? null : Math.min(OPENS.max, Math.max(OPENS.min, Math.round(a.maxOpens)));
    const existing = sites.find((x) => x.site.id === site.id);
    if (!existing) {
      sites.push({ site, appNames: [a.appName || known.name] });
      continue;
    }
    const e = existing.site;
    existing.appNames.push(a.appName || known.name);
    e.enabled = e.enabled || site.enabled;
    e.minutes = Math.min(e.minutes, site.minutes);
    e.weekendMinutes = e.weekendMinutes === null ? site.weekendMinutes : site.weekendMinutes === null ? e.weekendMinutes : Math.min(e.weekendMinutes, site.weekendMinutes);
    e.maxOpens = e.maxOpens === null ? site.maxOpens : site.maxOpens === null ? e.maxOpens : Math.min(e.maxOpens, site.maxOpens);
  }

  const budgets: Omit<Budget, "id">[] = [];
  for (const g of groups) {
    const ids = [...new Set(g.packages.map((p) => siteForApp(p)?.id).filter((id): id is string => !!id && sites.some((s) => s.site.id === id)))];
    if (ids.length === 0) continue;
    budgets.push({ name: g.name.slice(0, 30) || "Shared budget", minutes: cleanMinutes(g.minutes, 60), enabled: g.enabled, sites: ids });
  }

  const featureLimits: FeatureLimit[] = [];
  for (const f of features) {
    if (f.id !== "youtube_shorts" && f.id !== "instagram_reels") continue; // e.g. the daily reels count: not on websites
    featureLimits.push({ id: f.id as FeatureId, minutes: Math.min(180, cleanMinutes(f.minutes, 15)), enabled: f.enabled });
  }

  return {
    appVersion: asStr(json.appVersion).slice(0, 20),
    exportedAt: asNum(json.exportedAt) ?? 0,
    name: name ? name.slice(0, 30) : null,
    mascot,
    flowers: flowers === null ? null : Math.min(4, Math.max(0, Math.round(flowers))),
    strictness,
    bedtime: scheduleOf(json.bedtime, false),
    work: scheduleOf(json.workHours, true),
    sites,
    skipped: [...new Set(skipped)],
    budgets,
    features: featureLimits,
  };
}

/**
 * The settings after the import: imported sites replace the same sites, sites only set up on this
 * computer stay. Shared budgets with the same name are replaced; the rest is the phone's.
 */
export function applyImport(current: Settings, imp: PhoneImport, now: number): Settings {
  const next: Settings = structuredClone(current);
  for (const { site } of imp.sites) {
    const i = next.sites.findIndex((s) => s.id === site.id);
    if (i >= 0) next.sites[i] = { ...site, hosts: next.sites[i].hosts, name: next.sites[i].name };
    else next.sites.push(site);
  }
  imp.budgets.forEach((b, n) => {
    const i = next.budgets.findIndex((x) => x.name.trim().toLowerCase() === b.name.trim().toLowerCase());
    if (i >= 0) next.budgets[i] = { ...b, id: next.budgets[i].id };
    else next.budgets.push({ ...b, id: `phone-${now}-${n}` });
  });
  for (const f of imp.features) {
    const i = next.features.findIndex((x) => x.id === f.id);
    if (i >= 0) next.features[i] = f;
  }
  if (imp.strictness) next.strictness = imp.strictness;
  if (imp.bedtime) next.bedtime = imp.bedtime;
  if (imp.work) next.work = imp.work;
  if (imp.name) next.name = imp.name;
  if (imp.mascot) next.mascot = imp.mascot;
  if (imp.flowers !== null) next.flowers = imp.flowers;
  next.imported = { at: now, appVersion: imp.appVersion };
  return cleanSettings(next);
}
