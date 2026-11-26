// The Pause page, shown instead of a site that's over its limit (Firm and Beast), during bedtime and
// work hours, and as "I hid this for you" for YouTube Shorts / Instagram Reels when those are hidden.
// Like the app: one slow breath, "Why are you opening it?", then "Nah, I'm good" or 5 more minutes.
// The settings page can open it as a preview (?preview=1): then nothing is saved.

import { ext } from "../lib/ext.ts";
import { hiddenText, PAUSE_REASONS, pauseText } from "../lib/lines.ts";
import { limitOn, minutes, type PauseReason } from "../lib/rules.ts";
import { MASCOT_NAME, type Settings } from "../lib/settings.ts";
import { FEATURES, hostIs, hostOf, knownSite, siteOfUrl } from "../lib/sites.ts";
import { loadDay, loadSettings } from "../lib/store.ts";
import { formatTime, minuteOfDay, scheduleEndsAt } from "../lib/time.ts";
import { $, h, icon, mascotCanvas, toast } from "./ui.ts";

const params = new URLSearchParams(location.search);
const preview = params.get("preview") === "1";
const app = $("#app");

/** Closes this tab; if it's the window's last tab, opens a new tab first so the window stays. */
async function closeTab() {
  const tab = await ext.tabs.getCurrent();
  if (tab?.id === undefined) {
    window.close();
    return;
  }
  const tabs = await ext.tabs.query({ windowId: tab.windowId });
  if (tabs.length <= 1) await ext.tabs.create({ windowId: tab.windowId, active: true });
  await ext.tabs.remove(tab.id);
}

/** A web address on [site] only (never somewhere else), or the site's front page. */
function safeUrlOn(url: string | null, site: string, settings: Settings): string {
  const hosts = settings.sites.find((s) => s.id === site)?.hosts ?? knownSite(site)?.hosts ?? [site];
  const host = hostOf(url);
  if (url && host && hosts.some((h) => hostIs(host, h))) return url;
  return `https://${site}/`;
}

function previewDone() {
  toast("Just a preview. Nothing was saved.");
  setTimeout(() => void closeTab(), 1600);
}

async function show() {
  const settings = await loadSettings();
  const site = params.get("site") ?? "youtube.com";
  const siteName = settings.sites.find((s) => s.id === site)?.name ?? knownSite(site)?.name ?? site;
  const reasonParam = params.get("reason") ?? "limit";

  if (reasonParam === "hidden") return showHidden(settings, site, siteName);

  const reason = (["limit", "budget", "opens", "reels", "bedtime", "work"].includes(reasonParam) ? reasonParam : "limit") as PauseReason;
  const day = await loadDay();
  const siteLimit = settings.sites.find((s) => s.id === site);
  const budget = settings.budgets.find((b) => b.id === params.get("budget"));
  const feature = FEATURES.find((f) => f.id === params.get("feature"));
  const featureLimit = settings.features.find((f) => f.id === feature?.id);
  const schedule = reason === "bedtime" ? settings.bedtime : reason === "work" ? settings.work : null;
  const locked = !preview && settings.strictness === "beast";

  const limit =
    reason === "opens" ? (siteLimit?.maxOpens ?? undefined)
    : reason === "budget" ? budget?.minutes
    : reason === "reels" ? featureLimit?.minutes
    : siteLimit ? limitOn(siteLimit, Date.now())
    : undefined;
  const until = schedule ? minuteOfDay(scheduleEndsAt(schedule, Date.now())) : undefined;
  const text = pauseText({
    mascot: settings.mascot,
    site: siteName,
    reason,
    locked,
    opens: preview ? 3 : (day.opens[site] ?? 0),
    limit: preview ? 30 : limit,
    budgetName: budget?.name,
    featureName: feature?.name,
    until,
  });

  document.title = `${text.title} · ScrollNah`;
  const back = safeUrlOn(params.get("back"), site, settings);
  let why: string | null = null;

  // 3, 2, 1: the buttons wake up after one slow breath.
  const count = h("b", null, "3");
  const label = h("span", null, "breathe in");
  const breath = h("div", { class: "breath go", role: "timer", "aria-live": "polite" }, count, label);
  const more = h("button", { class: "btn outline", type: "button", disabled: true }, reason === "reels" ? `5 more minutes of ${feature?.name ?? "Reels"}` : "Open for 5 minutes");
  let n = 3;
  const tick = setInterval(() => {
    n -= 1;
    if (n > 0) {
      count.textContent = String(n);
      label.textContent = n === 2 ? "hold it" : "breathe out";
    } else {
      clearInterval(tick);
      count.textContent = "1";
      label.textContent = "nice one";
      more.disabled = false;
    }
  }, 1000);

  const log = (choice: "left" | "more") => ext.runtime.sendMessage({ type: "logPause", site, why, choice }).catch(() => {});

  const leave = async () => {
    if (preview) return previewDone();
    await log("left");
    if (reason === "reels") location.href = safeUrlOn(null, site, settings); // the rest of the site still works
    else await closeTab();
  };
  more.addEventListener("click", async () => {
    if (preview) return previewDone();
    const r = await ext.runtime.sendMessage<{ ok: boolean }>({ type: "pass", site }).catch(() => null);
    if (!r?.ok) {
      toast("Couldn't do that right now.");
      return;
    }
    await log("more");
    location.href = back;
  });

  const chips = PAUSE_REASONS.map((r) =>
    h("button", {
      class: "chip",
      type: "button",
      "aria-pressed": "false",
      onclick: (e: Event) => {
        why = why === r ? null : r;
        for (const c of chips) c.setAttribute("aria-pressed", String(c.textContent === why));
        (e.currentTarget as HTMLElement).focus();
      },
    }, r),
  );

  const showWhy = reason !== "bedtime" && reason !== "work" && !locked;
  const leaveLabel = locked ? "Take me home" : reason === "reels" ? `Back to ${siteName}` : "Nah, I'm good";

  app.replaceChildren(
    h(
      "section",
      { class: "side" },
      h("span", { class: "pill" }, preview ? `Preview · ${text.pill}` : text.pill),
      mascotCanvas(200, { mascot: settings.mascot, mood: 4, flowers: settings.flowers, onOrange: true }, "mascot bob"),
      h("span", { class: "pill ink tag" }, MASCOT_NAME[settings.mascot]),
      breath,
    ),
    h(
      "section",
      { class: "main" },
      h("h1", null, text.title),
      h("p", { class: "sub" }, text.subtitle),
      showWhy ? h("div", { class: "why" }, h("h2", null, "Why are you opening it?"), h("div", { class: "chips" }, ...chips)) : null,
      h(
        "div",
        { class: "actions" },
        h("button", { class: "btn ink", type: "button", onclick: leave, autofocus: true }, leaveLabel),
        locked ? null : more,
      ),
      locked
        ? h("p", { class: "lock-note small" }, icon("lock"), until !== undefined ? `Beast mode. ${siteName} opens again at ${formatTime(until)}.` : "Beast mode. It unlocks at midnight.")
        : null,
    ),
  );
  if (reason === "reels" && !locked && siteLimit) {
    // Normal videos on the site still work: say how much site time is left.
    const left = limitOn(siteLimit, Date.now()) - minutes(day.ms[site]);
    if (left > 0) app.querySelector(".sub")?.append(` You still have ${left} min of ${siteName}.`);
  }
}

function showHidden(settings: Settings, site: string, siteName: string) {
  document.body.classList.add("calm");
  const what = params.get("what") ?? "that page";
  const home = safeUrlOn(params.get("home"), site, settings);
  const watchParam = params.get("watch");
  const watch = watchParam && siteOfUrl(watchParam, [{ id: site, hosts: knownSite(site)?.hosts ?? [site] }]) ? watchParam : null;
  const text = hiddenText(what, siteName);
  document.title = `${text.title} · ScrollNah`;
  app.replaceChildren(
    h(
      "section",
      { class: "side" },
      mascotCanvas(200, { mascot: settings.mascot, mood: 2, flowers: settings.flowers }, "mascot bob"),
      h("span", { class: "pill ink tag" }, MASCOT_NAME[settings.mascot]),
    ),
    h(
      "section",
      { class: "main" },
      h("h1", null, text.title),
      h("p", { class: "sub" }, text.subtitle),
      h(
        "div",
        { class: "actions" },
        h("button", { class: "btn", type: "button", onclick: () => (location.href = home), autofocus: true }, `Back to ${siteName}`),
        watch ? h("button", { class: "btn outline", type: "button", onclick: () => (location.href = watch) }, "Watch it as a normal video") : null,
      ),
      h("p", { class: "small muted" }, "Change this in ScrollNah's settings, under \"Hide the endless parts\"."),
    ),
  );
}

void show();
