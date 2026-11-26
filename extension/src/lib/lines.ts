// What the mascot says, in its own voice ("I"), same lines as the phone app (ui/mascot/MascotLines.kt).
// Each mood has a few variations; the pick changes once a day, so it feels fresh but not jumpy.

import type { Mood, PauseReason } from "./rules.ts";
import { MASCOT_NAME, type MascotId } from "./settings.ts";
import { duration, durationPair, epochDay, formatTime, ordinal } from "./time.ts";

function forToday<T>(list: T[], salt: number, now = Date.now()): T {
  return list[Math.abs(epochDay(now) * 31 + salt) % list.length];
}

export interface MoodText {
  headline: string;
  line: string;
}

/** The mood card in the popup (the app's Home card). [used]/[limit] = today's minutes, all sites together. */
export function moodCard(mascot: MascotId, mood: Mood, used: number, limit: number, atLimit: string | null): MoodText {
  const bloomy = mascot === "bloomy";
  let headline: string;
  let lines: string[];
  switch (mood) {
    case 1:
      if (used === 0) {
        headline = forToday(["No scrolling yet today!", "Clean slate so far!", "Zero minutes. I'm impressed."], 1);
        lines = bloomy
          ? ["I'm soaking up the sun. Keep it going.", "Fresh air for you, sunshine for me.", "My petals are wide open. Love that."]
          : ["I'm proud of you. Keep it going.", "Go touch some grass. I'll hold the fort.", "Your mouse is on holiday. Love that."];
      } else {
        headline = forToday([`Only ${duration(used)} of scrolling today!`, `Just ${duration(used)} so far. Smooth.`, `${duration(used)}. Barely a dent!`], 3);
        lines = bloomy
          ? ["I'm thriving. Loads of time left today.", "I'm growing tall with you.", "Keep this sunshine coming."]
          : ["I'm proud of you. Loads of time left today.", "I'm vibing with you.", "Keep this energy going."];
      }
      break;
    case 2:
      headline = forToday([`${duration(used)} so far. All good.`, `${duration(used)} used. Nice and steady.`, `${durationPair(used, limit)}. Comfy.`], 5);
      lines = bloomy
        ? ["I'm standing tall. Keep it steady.", "Plenty left. My flowers are open.", "Steady sunshine. I like it."]
        : ["I'm happy with that. Keep it chill.", "Plenty left. I'm smiling.", "Steady wins. I'm here if you need me."];
      break;
    case 3:
      headline = forToday([`${durationPair(used, limit)} used today`, `Careful: ${durationPair(used, limit)} gone`, `${duration(used)} in. I'm watching...`], 7);
      lines = bloomy
        ? ["I'm leaning a little. Maybe save the rest for later?", "Getting close. A glass of water for both of us?", "I'll tell you when it's time."]
        : ["Getting close. I'll tell you when it's time.", "Maybe save the rest for later?", "One more scroll... or a glass of water?"];
      break;
    case 4:
      headline =
        atLimit && used < limit * 0.8
          ? `${atLimit} is at its limit`
          : forToday(
              [`Almost there: ${durationPair(used, limit)}`, `${durationPair(used, limit)}. That's nearly it.`, `Only ${duration(Math.max(0, limit - used))} left today`],
              9,
            );
      lines = bloomy
        ? ["I'm getting thirsty. Maybe stop here?", "My leaves are drooping. Laptop lid down soon?", "A break would water me nicely."]
        : ["I'm sweating a bit. Maybe stop here?", "Nearly at your limit. Tab closed soon?", "I'm getting nervous. Save the last bit?"];
      break;
    default:
      headline = forToday(
        [`${duration(used)} today. That's over the line!`, `Over budget: ${duration(used)} today`, bloomy ? `${duration(used)}. I need some water.` : `${duration(used)}. I need a lie-down.`],
        11,
      );
      lines = bloomy
        ? ["I'm wilting. Screen off for a bit?", "My petals are falling. A break would help.", "Tomorrow's a fresh start. Today, maybe stop here?"]
        : ["I'm melting. Screen off for a bit?", "Close the tab, shoulders down?", "Tomorrow's a fresh start. Today, maybe stop here?"];
  }
  return { headline, line: forToday(lines, mood * 2) };
}

export interface PauseText {
  pill: string;
  title: string;
  subtitle: string;
}

export interface PauseFacts {
  mascot: MascotId;
  site: string; // the site's name, e.g. "YouTube"
  reason: PauseReason;
  locked: boolean; // Beast mode: no "5 more minutes"
  opens: number; // times opened today
  limit?: number; // minutes, opens, or the budget's minutes
  budgetName?: string;
  featureName?: string;
  until?: number; // bedtime / work hours end (minutes after midnight)
}

/** The Pause page's pill, title and the mascot's line (same texts as the app's Pause screen). */
export function pauseText(f: PauseFacts): PauseText {
  const name = f.site;
  const pill =
    f.reason === "bedtime" ? "Bedtime"
    : f.reason === "work" ? "Work hours"
    : f.locked ? "Locked till tomorrow"
    : f.reason === "reels" ? `${f.featureName ?? "Reels"} limit reached`
    : f.opens <= 0 ? "Limit reached"
    : f.opens === 1 ? "1st time today"
    : `${ordinal(f.opens)} time today`;

  const title =
    f.reason === "bedtime" ? `It's bedtime. ${name} can wait.`
    : f.reason === "work" ? `Work time. ${name} can wait.`
    : f.reason === "reels" ? `That's enough ${f.featureName ?? "Reels"} for today.`
    : f.locked ? `That's it for ${name} today.`
    : `Woah. ${name} again?`;

  const locked = forToday(
    [
      `You picked Beast mode, so ${name} is locked until midnight. I believe in you.`,
      `Beast mode says no. ${name} unlocks at midnight. You've got this.`,
      `${name} is done for today. Future you says thanks.`,
    ],
    21,
  );
  let subtitle: string;
  switch (f.reason) {
    case "bedtime":
      subtitle = `I'm sleepy. ${name} is paused until ${formatTime(f.until ?? 0)}.`;
      break;
    case "work":
      subtitle = `I'm keeping ${name} paused until ${formatTime(f.until ?? 0)}. You've got this.`;
      break;
    case "budget":
      subtitle = f.locked
        ? locked
        : `${f.budgetName ?? "Your shared budget"} had ${duration(f.limit ?? 0)} together today, and it's all used. Breathe with me, then choose.`;
      break;
    case "opens":
      subtitle = f.locked
        ? locked
        : `You said ${f.limit} ${f.limit === 1 ? "visit" : "visits"} a day was plenty. Breathe with me, then choose.`;
      break;
    case "reels":
      subtitle = f.locked
        ? `Beast mode says no more ${f.featureName ?? "Reels"} today. The rest of ${name} still works.`
        : `That's your ${duration(f.limit ?? 0)} of ${f.featureName ?? "Reels"}. Breathe with me, then choose.`;
      break;
    default:
      subtitle = f.locked
        ? locked
        : f.limit === undefined
          ? "Take one slow breath with me before you decide."
          : forToday(
              [
                `You've used your ${duration(f.limit)}. Take one slow breath with me before you decide.`,
                `That's your ${duration(f.limit)} done. Breathe with me, then choose.`,
                `Limit reached (${duration(f.limit)}). One breath, then you decide.`,
              ],
              20,
            );
  }
  return { pill, title, subtitle };
}

/** Chill mode's nudge (a notification), signed by the mascot: "Blobby: That's your 30 min of YouTube". */
export function nudgeText(mascot: MascotId, siteName: string, reason: PauseReason, limit: number | undefined, over: number): { title: string; message: string } {
  const who = MASCOT_NAME[mascot];
  const title =
    reason === "opens" ? `${who}: That's a lot of ${siteName} visits`
    : reason === "reels" ? `${who}: That's your Reels & Shorts time`
    : reason === "bedtime" ? `${who}: It's bedtime`
    : reason === "work" ? `${who}: It's work time`
    : over > 0 ? `${who}: ${duration(over)} over on ${siteName}`
    : `${who}: That's your ${duration(limit ?? 0)} of ${siteName}`;
  const message =
    mascot === "bloomy"
      ? over > 0 ? "I'm wilting here. Time for a break?" : "I'm getting thirsty. Take a break?"
      : over > 0 ? "I'm melting here. Time for a break?" : "I'm getting worried. Take a break?";
  return { title, message };
}

/** The "hidden" page: "I hid YouTube Shorts for you." */
export function hiddenText(what: string, siteName: string): { title: string; subtitle: string } {
  return {
    title: `I hid ${what} for you.`,
    subtitle: `You switched ${what} off in ScrollNah. The rest of ${siteName} still works.`,
  };
}

export const PAUSE_REASONS = ["Bored", "Replying to someone", "Pure habit", "Work thing"];
