// Dates, schedules (bedtime, work hours) and how time is written ("45 min", "1h 18m").
// Everything uses the computer's local time, like the phone app.

/** "2026-11-26" for the local day of [ms]. */
export function dayKey(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** Local midnight at the start of the day after [ms]. */
export function nextMidnight(ms: number): number {
  const d = new Date(ms);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1).getTime();
}

/** Days since 1970 for the local date (changes at local midnight). */
export function epochDay(ms: number): number {
  const d = new Date(ms);
  return Math.floor(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / 86_400_000);
}

export function isWeekend(ms: number): boolean {
  const day = new Date(ms).getDay();
  return day === 0 || day === 6;
}

/** Monday = 1 ... Sunday = 7, like the phone app (java.time.DayOfWeek). */
export function weekday(ms: number): number {
  const day = new Date(ms).getDay();
  return day === 0 ? 7 : day;
}

/** Minutes after local midnight. */
export function minuteOfDay(ms: number): number {
  const d = new Date(ms);
  return d.getHours() * 60 + d.getMinutes();
}

/**
 * Bedtime or work hours: from [start] to [end] (minutes after midnight; may cross midnight).
 * [days] (Monday = 1) only for work hours; bedtime is every day.
 */
export interface Schedule {
  enabled: boolean;
  start: number;
  end: number;
  days?: number[];
}

function onDay(s: Schedule, day: number): boolean {
  return !s.days || s.days.includes(day);
}

/** True if [ms] is inside the schedule. A night shift past midnight belongs to the day it started. */
export function scheduleActive(s: Schedule, ms: number): boolean {
  if (!s.enabled || s.start === s.end) return false;
  const m = minuteOfDay(ms);
  const day = weekday(ms);
  const yesterday = day === 1 ? 7 : day - 1;
  if (s.start < s.end) return onDay(s, day) && m >= s.start && m < s.end;
  return (onDay(s, day) && m >= s.start) || (onDay(s, yesterday) && m < s.end);
}

/** When the schedule window that [ms] is in ends (only meaningful while it's active). */
export function scheduleEndsAt(s: Schedule, ms: number): number {
  const d = new Date(ms);
  const endToday = new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, s.end).getTime();
  return endToday > ms ? endToday : new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1, 0, s.end).getTime();
}

/** The next time after [ms] the schedule starts, within a week, or null. */
export function scheduleNextStart(s: Schedule, ms: number): number | null {
  if (!s.enabled || s.start === s.end) return null;
  const d = new Date(ms);
  for (let i = 0; i <= 7; i++) {
    const at = new Date(d.getFullYear(), d.getMonth(), d.getDate() + i, 0, s.start).getTime();
    if (at > ms && onDay(s, weekday(at))) return at;
  }
  return null;
}

/** 7:00 AM. */
export function formatTime(minutes: number): string {
  const d = new Date(2000, 0, 1, 0, minutes);
  return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

/** "07:00" for <input type="time">, and back. */
export function timeInputValue(minutes: number): string {
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}
export function parseTimeInput(value: string): number | null {
  const m = /^(\d{1,2}):(\d{2})/.exec(value);
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  return h < 24 && min < 60 ? h * 60 + min : null;
}

/** How we show time: under an hour "45 min", otherwise "1h 18m" or "2h" (same as the app). */
export function duration(minutes: number): string {
  return minutes < 60 ? `${minutes} min` : compact(minutes);
}

/** 75 -> "1h 15m", 40 -> "40m", 120 -> "2h". */
export function compact(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}m`;
  return m === 0 ? `${h}h` : `${h}h ${m}m`;
}

/** "3 of 5 min", or "45 min of 1h 30m". */
export function durationPair(used: number, limit: number, separator = " of "): string {
  return used < 60 && limit < 60 ? `${used}${separator}${limit} min` : `${duration(used)}${separator}${duration(limit)}`;
}

/** 1 -> "1st", 2 -> "2nd", 3 -> "3rd", 11 -> "11th". */
export function ordinal(n: number): string {
  const teen = n % 100 >= 11 && n % 100 <= 13;
  const suffix = teen ? "th" : (["th", "st", "nd", "rd"][n % 10] ?? "th");
  return `${n}${suffix}`;
}
