// Beast mode can't be loosened in a weak moment (same rule as the app's BeastGuard): giving yourself
// more time needs a 10-minute cool-down first, then there are 15 minutes to make the change.
// Making things stricter never waits. For the first hour after setup ("settling in") anything goes.

import type { Settings, SiteLimit } from "./settings.ts";
import type { Schedule } from "./time.ts";

export const WAIT_MS = 10 * 60_000;
export const WINDOW_MS = 15 * 60_000;
export const SETTLING_MS = 60 * 60_000;

export type GuardState =
  | { kind: "locked" } // no cool-down running: loosening has to wait
  | { kind: "waiting"; msLeft: number } // cool-down running
  | { kind: "open"; msLeft: number } // loosening allowed for msLeft more
  | { kind: "settling"; msLeft: number }; // just after setup: no cool-down needed

/** Where the cool-down is, given when it ends ([unlockAt], 0 = never started). */
export function guardState(unlockAt: number, now: number, setupAt: number): GuardState {
  const settleUntil = setupAt > 0 ? setupAt + SETTLING_MS : 0;
  if (now < settleUntil && settleUntil - now <= SETTLING_MS) return { kind: "settling", msLeft: settleUntil - now };
  if (unlockAt <= 0 || now >= unlockAt + WINDOW_MS) return { kind: "locked" };
  if (now < unlockAt) return { kind: "waiting", msLeft: unlockAt - now };
  return { kind: "open", msLeft: unlockAt + WINDOW_MS - now };
}

export function allowsLoosening(state: GuardState): boolean {
  return state.kind === "open" || state.kind === "settling";
}

const ORDER = { chill: 0, firm: 1, beast: 2 };

function weekendOf(s: SiteLimit): number {
  return s.weekendMinutes ?? s.minutes; // weekends use the normal limit when there's no weekend one
}

/** Every minute a schedule covers (0-1439), so "does the new one cover all of the old one" is simple. */
function minutesCovered(s: Schedule): Set<number> {
  const out = new Set<number>();
  if (!s.enabled || s.start === s.end) return out;
  for (let m = s.start; m !== s.end; m = (m + 1) % 1440) out.add(m);
  return out;
}

function scheduleLoosened(old: Schedule, next: Schedule): boolean {
  if (!old.enabled) return false;
  if (!next.enabled) return true;
  const was = minutesCovered(old);
  const now = minutesCovered(next);
  for (const m of was) if (!now.has(m)) return true;
  if (old.days && next.days) return old.days.some((d) => !next.days!.includes(d));
  return false;
}

/**
 * What would give more time if [next] replaced [old], in plain words (empty = nothing looser).
 * Used for saving settings and for importing from the phone.
 */
export function loosenings(old: Settings, next: Settings): string[] {
  const out: string[] = [];
  if (ORDER[next.strictness] < ORDER[old.strictness]) out.push("a gentler strictness");

  for (const o of old.sites) {
    if (!o.enabled) continue;
    const n = next.sites.find((s) => s.id === o.id);
    if (!n || !n.enabled) {
      out.push(`${o.name} without a limit`);
      continue;
    }
    if (n.minutes > o.minutes || weekendOf(n) > weekendOf(o)) out.push(`more time on ${o.name}`);
    if (o.maxOpens !== null && (n.maxOpens === null || n.maxOpens > o.maxOpens)) out.push(`more visits to ${o.name}`);
  }

  for (const o of old.budgets) {
    if (!o.enabled || o.sites.length === 0) continue;
    const n = next.budgets.find((b) => b.id === o.id);
    if (!n || !n.enabled) out.push(`${o.name} switched off`);
    else if (n.minutes > o.minutes || o.sites.some((id) => !n.sites.includes(id))) out.push(`more time in ${o.name}`);
  }

  for (const o of old.features) {
    if (!o.enabled) continue;
    const n = next.features.find((f) => f.id === o.id);
    if (!n || !n.enabled || n.minutes > o.minutes) out.push("more Reels & Shorts");
  }

  if (scheduleLoosened(old.bedtime, next.bedtime)) out.push("a shorter bedtime");
  if (scheduleLoosened(old.work, next.work)) out.push("shorter work hours");
  for (const k of Object.keys(old.hide) as (keyof Settings["hide"])[]) {
    if (old.hide[k] && !next.hide[k]) {
      out.push("showing hidden feeds again");
      break;
    }
  }
  return [...new Set(out)];
}
