// Turning "a site has been in front since [since]" into minutes per day.

import type { FeatureId } from "./settings.ts";
import { dayKey, nextMidnight } from "./time.ts";

/** The current stretch of time: what's in front and since when (site null = nothing to count). */
export interface Live {
  site: string | null;
  feature: FeatureId | null;
  tabId: number | null;
  since: number;
}

/**
 * Checks happen at least every minute while a site is in front (and every 15 s from the page itself).
 * A longer gap means the computer slept or the browser was frozen, so that time isn't counted.
 */
export const MAX_GAP_MS = 150_000;

/** The time to add since [live.since], split at midnight so each day gets its own part. */
export function creditTime(live: Live, now: number): { key: string; ms: number }[] {
  if (!live.site || now <= live.since || now - live.since > MAX_GAP_MS) return [];
  const parts: { key: string; ms: number }[] = [];
  let from = live.since;
  while (from < now) {
    const end = Math.min(now, nextMidnight(from));
    parts.push({ key: dayKey(from), ms: end - from });
    from = end;
  }
  return parts;
}
