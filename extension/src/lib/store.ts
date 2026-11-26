// Reading and writing what's saved in the browser (storage.local). Nothing is ever sent anywhere.
//   "settings"            what the user set up (written by the settings pages)
//   "day:2026-11-26"      one day of counting (written only by the background script)
//   "beastUnlockAt"       Beast mode's cool-down

import { ext } from "./ext.ts";
import { cleanDay, type DayData } from "./rules.ts";
import { cleanSettings, type Settings } from "./settings.ts";
import { dayKey } from "./time.ts";

export const DAYS_KEPT = 60;

export async function loadSettings(): Promise<Settings> {
  const r = await ext.storage.local.get("settings");
  return cleanSettings(r.settings);
}

export async function saveSettings(settings: Settings): Promise<void> {
  await ext.storage.local.set({ settings: cleanSettings(settings) });
}

export async function loadDay(key = dayKey(Date.now())): Promise<DayData> {
  const r = await ext.storage.local.get(`day:${key}`);
  return cleanDay(r[`day:${key}`]);
}

export async function saveDay(key: string, day: DayData): Promise<void> {
  await ext.storage.local.set({ [`day:${key}`]: day });
}

export async function loadBeastUnlockAt(): Promise<number> {
  const r = await ext.storage.local.get("beastUnlockAt");
  return typeof r.beastUnlockAt === "number" ? r.beastUnlockAt : 0;
}

export async function saveBeastUnlockAt(at: number): Promise<void> {
  await ext.storage.local.set({ beastUnlockAt: at });
}

/** Deletes days older than [DAYS_KEPT]. */
export async function pruneDays(now = Date.now()): Promise<void> {
  const all = await ext.storage.local.get(null);
  const oldest = dayKey(now - DAYS_KEPT * 86_400_000);
  const old = Object.keys(all).filter((k) => k.startsWith("day:") && k.slice(4) < oldest);
  if (old.length) await ext.storage.local.remove(old);
}

/** Wipes everything ScrollNah saved in this browser. */
export async function clearEverything(): Promise<void> {
  await ext.storage.local.clear();
  await ext.storage.session.clear().catch(() => {});
}
