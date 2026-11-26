// The rules, as plain functions (tested in test/rules.test.ts): today's limit for a site, whether a
// site should be paused right now and why, how long until it would be, and the mascot's mood.
// Same rules as the phone app.

import { MASCOT_NAME, type Budget, type FeatureId, type Settings, type SiteLimit } from "./settings.ts";
import { isWeekend, scheduleActive, scheduleNextStart, nextMidnight } from "./time.ts";

/** One day of counting, saved as "day:2026-11-26". Only numbers: never addresses or page titles. */
export interface DayData {
  ms: Record<string, number>; // site id -> time in front
  featureMs: Record<string, number>; // "youtube_shorts" -> time
  opens: Record<string, number>; // site id -> times opened
  passes: Record<string, number>; // site id -> "5 more minutes" ends at (ms)
  nudged: Record<string, number>; // Chill: minutes used at the last nudge, per limit
  pauses: PauseEntry[]; // what happened on the Pause page
}

export interface PauseEntry {
  at: number;
  site: string;
  why: string | null; // Bored, Replying to someone, Pure habit, Work thing
  choice: "left" | "more";
}

export function emptyDay(): DayData {
  return { ms: {}, featureMs: {}, opens: {}, passes: {}, nudged: {}, pauses: [] };
}

export function cleanDay(raw: unknown): DayData {
  const d = emptyDay();
  if (!raw || typeof raw !== "object") return d;
  const o = raw as Record<string, unknown>;
  const numbers = (v: unknown): Record<string, number> => {
    const out: Record<string, number> = {};
    if (v && typeof v === "object") {
      for (const [k, n] of Object.entries(v)) if (typeof n === "number" && Number.isFinite(n) && n >= 0) out[k] = n;
    }
    return out;
  };
  return {
    ms: numbers(o.ms),
    featureMs: numbers(o.featureMs),
    opens: numbers(o.opens),
    passes: numbers(o.passes),
    nudged: numbers(o.nudged),
    pauses: Array.isArray(o.pauses) ? (o.pauses as PauseEntry[]).slice(-200) : [],
  };
}

/** Whole minutes from milliseconds (like the app: 59 s is still 0 min). */
export function minutes(ms: number | undefined): number {
  return Math.floor((ms ?? 0) / 60_000);
}

/** Today's limit for a site: the weekend one on Saturday and Sunday (if set). */
export function limitOn(site: SiteLimit, at: number): number {
  return site.weekendMinutes !== null && isWeekend(at) ? site.weekendMinutes : site.minutes;
}

export type PauseReason = "limit" | "budget" | "opens" | "reels" | "bedtime" | "work";

export interface Block {
  reason: PauseReason;
  site: string;
  budget?: Budget;
  feature?: FeatureId;
  limit?: number; // minutes (or opens) for the text on the Pause page
}

function budgetUsedMs(day: DayData, b: Budget, settings: Settings): number {
  const enabled = new Set(settings.sites.filter((s) => s.enabled).map((s) => s.id));
  return b.sites.filter((id) => enabled.has(id)).reduce((sum, id) => sum + (day.ms[id] ?? 0), 0);
}

/** True while "5 more minutes" is running for a site. */
export function hasPass(day: DayData, site: string, at: number): boolean {
  return (day.passes[site] ?? 0) > at;
}

/**
 * Why [siteId] should be paused at [at], or null if it's fine. [feature] = the page is YouTube Shorts
 * or Instagram Reels. "5 more minutes" covers everything for its 5 minutes.
 */
export function blockFor(settings: Settings, day: DayData, siteId: string, feature: FeatureId | null, at: number): Block | null {
  const site = settings.sites.find((s) => s.id === siteId);
  if (!site || !site.enabled) return null;
  if (hasPass(day, siteId, at)) return null;

  if (scheduleActive(settings.bedtime, at)) return { reason: "bedtime", site: siteId };
  if (scheduleActive(settings.work, at)) return { reason: "work", site: siteId };

  if (site.maxOpens !== null && (day.opens[siteId] ?? 0) > site.maxOpens) return { reason: "opens", site: siteId, limit: site.maxOpens };

  const limit = limitOn(site, at);
  if (minutes(day.ms[siteId]) >= limit) return { reason: "limit", site: siteId, limit };

  for (const b of settings.budgets) {
    if (b.enabled && b.sites.includes(siteId) && minutes(budgetUsedMs(day, b, settings)) >= b.minutes) {
      return { reason: "budget", site: siteId, budget: b, limit: b.minutes };
    }
  }

  if (feature) {
    const f = settings.features.find((x) => x.id === feature);
    if (f?.enabled && minutes(day.featureMs[feature]) >= f.minutes) return { reason: "reels", site: siteId, feature, limit: f.minutes };
  }
  return null;
}

/**
 * How long (ms) until something changes for [siteId] while it stays in front: a limit is reached,
 * "5 more minutes" ends, bedtime or work hours start, or midnight. Used to check again right on time.
 */
export function msUntilChange(settings: Settings, day: DayData, siteId: string, feature: FeatureId | null, at: number): number {
  const candidates: number[] = [nextMidnight(at) - at];
  const site = settings.sites.find((s) => s.id === siteId);
  if (site?.enabled) {
    const pass = day.passes[siteId] ?? 0;
    if (pass > at) candidates.push(pass - at);
    candidates.push(limitOn(site, at) * 60_000 - (day.ms[siteId] ?? 0));
    for (const b of settings.budgets) {
      if (b.enabled && b.sites.includes(siteId)) candidates.push(b.minutes * 60_000 - budgetUsedMs(day, b, settings));
    }
    if (feature) {
      const f = settings.features.find((x) => x.id === feature);
      if (f?.enabled) candidates.push(f.minutes * 60_000 - (day.featureMs[feature] ?? 0));
    }
    for (const s of [settings.bedtime, settings.work]) {
      const next = scheduleNextStart(s, at);
      if (next !== null) candidates.push(next - at);
    }
  }
  const positive = candidates.filter((c) => c > 0);
  return positive.length ? Math.min(...positive) : 0;
}

// ---- Mood (same rule as the app's MoodLevel.from) ----

export type Mood = 1 | 2 | 3 | 4 | 5; // thriving, happy, okay, worried/thirsty, melting/wilting

export interface MoodInfo {
  level: Mood;
  used: number; // minutes, enabled sites together
  limit: number;
  atLimit: string | null; // a site that reached its own limit (makes the mood at least 4)
}

export function moodFrom(used: number, limit: number, anyAtLimit: boolean): Mood {
  if (limit <= 0) return 1;
  const share = used / limit;
  const level: Mood = share >= 1 ? 5 : share >= 0.8 ? 4 : share >= 0.5 ? 3 : share >= 0.25 ? 2 : 1;
  return anyAtLimit && level < 4 ? 4 : level;
}

export function moodToday(settings: Settings, day: DayData, at: number): MoodInfo {
  let used = 0;
  let limit = 0;
  let atLimit: string | null = null;
  for (const s of settings.sites) {
    if (!s.enabled) continue;
    const u = minutes(day.ms[s.id]);
    const l = limitOn(s, at);
    used += u;
    limit += l;
    if (u >= l && !atLimit) atLimit = s.name;
  }
  return { level: moodFrom(used, limit, atLimit !== null), used, limit, atLimit };
}

const MOOD_NAMES = {
  blobby: ["thriving", "happy", "okay", "worried", "melting"],
  bloomy: ["thriving", "happy", "okay", "thirsty", "wilting"],
};

export function moodName(settings: Pick<Settings, "mascot">, level: Mood): string {
  return MOOD_NAMES[settings.mascot][level - 1];
}

/** The signed mood pill, e.g. "Bloomy · thirsty". */
export function moodPill(settings: Pick<Settings, "mascot">, level: Mood): string {
  return `${MASCOT_NAME[settings.mascot]} · ${moodName(settings, level)}`;
}

/** Minutes left on a site today (negative = over). */
export function minutesLeft(site: SiteLimit, day: DayData, at: number): number {
  return limitOn(site, at) - minutes(day.ms[site.id]);
}
