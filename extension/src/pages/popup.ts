// The panel that opens from the toolbar: the mascot in today's mood (like the app's Home card) and
// today's minutes on each site.

import { ext } from "../lib/ext.ts";
import { moodCard } from "../lib/lines.ts";
import { hasPass, limitOn, minutes, moodPill, moodToday, type DayData } from "../lib/rules.ts";
import type { Settings } from "../lib/settings.ts";
import { FEATURES } from "../lib/sites.ts";
import { loadDay, loadSettings } from "../lib/store.ts";
import { duration, durationPair, formatTime, scheduleActive } from "../lib/time.ts";
import { $, bar, blink, h, icon, mascotCanvas, tile } from "./ui.ts";

const app = $("#app");
let stopBlink = () => {};

function openSettings(hash = "") {
  void ext.tabs.create({ url: ext.runtime.getURL(`pages/options.html${hash}`) });
  window.close();
}

function header(settings: Settings) {
  return h(
    "header",
    null,
    h("h1", null, settings.name ? `Hey ${settings.name}` : "Hey there"),
    h("div", { class: "row" }, h("span", { class: "wordmark", "aria-label": "ScrollNah" }, "scrollnah"), h("button", { class: "icon-btn", type: "button", "aria-label": "Settings", onclick: () => openSettings() }, icon("gear"))),
  );
}

function render(settings: Settings, day: DayData) {
  app.replaceChildren();
  const now = Date.now();

  if (!settings.setupDone) {
    app.append(
      header(settings),
      h(
        "section",
        { class: "card lime mood" },
        h("div", { class: "top" }, mascotCanvas(84, { mascot: settings.mascot, mood: 2, flowers: settings.flowers }, "mascot bob"), h("div", null, h("h2", null, "Let's set you up"), h("p", null, "Pick your sites and limits. It takes a minute."))),
        h("button", { class: "btn ink wide", type: "button", onclick: () => { void ext.tabs.create({ url: ext.runtime.getURL("pages/welcome.html") }); window.close(); } }, "Start setup"),
      ),
    );
    return;
  }

  const mood = moodToday(settings, day, now);
  const over = mood.limit > 0 && mood.used >= mood.limit;
  const text = moodCard(settings.mascot, mood.level, mood.used, mood.limit, mood.atLimit);
  // On the orange card (over the limit) Blobby turns lime and Bloomy's pot lilac, like the app.
  const drawing = { mascot: settings.mascot, mood: mood.level, flowers: settings.flowers, onOrange: over };
  const canvas = mascotCanvas(88, drawing, "mascot bob");
  stopBlink();
  stopBlink = blink(canvas, 88, drawing);
  const left = mood.limit - mood.used;

  const card = h(
    "section",
    { class: `card mood on-bright ${over ? "orange" : "lime"}` },
    h("div", { class: "top" }, canvas, h("div", { class: "grow" }, h("span", { class: "pill" }, moodPill(settings, mood.level)), h("h2", null, text.headline), h("p", null, text.line))),
    mood.limit > 0
      ? h("div", null, bar(mood.used, mood.limit), h("div", { class: "left" }, h("span", null, left >= 0 ? `${duration(left)} left today` : `${duration(-left)} over today`), h("span", null, durationPair(mood.used, mood.limit, " / "))))
      : null,
  );

  const notes: HTMLElement[] = [];
  if (scheduleActive(settings.bedtime, now)) notes.push(h("div", { class: "card note" }, icon("moon"), `Bedtime until ${formatTime(settings.bedtime.end)}. Your sites are paused.`));
  else if (scheduleActive(settings.work, now)) notes.push(h("div", { class: "card note" }, icon("lock"), `Work hours until ${formatTime(settings.work.end)}. Your sites are paused.`));
  for (const s of settings.sites) {
    if (hasPass(day, s.id, now)) {
      const m = Math.max(1, Math.ceil(((day.passes[s.id] ?? now) - now) / 60_000));
      notes.push(h("div", { class: "card note" }, `5 more minutes on ${s.name}: ${m} min left.`));
    }
  }

  const list = h("section", { class: "list", "aria-label": "Today's limits" }, h("h3", null, "Today's limits", h("button", { class: "link-btn small", type: "button", onclick: () => openSettings("#sites") }, "Edit")));
  const enabled = settings.sites.filter((s) => s.enabled);
  if (enabled.length === 0) {
    list.append(h("p", { class: "muted empty" }, "No limits yet."), h("button", { class: "btn wide", type: "button", onclick: () => openSettings("#sites") }, "Add a site"));
  }
  for (const s of enabled) {
    const used = minutes(day.ms[s.id]);
    const limit = limitOn(s, now);
    const visits = day.opens[s.id] ?? 0;
    const visitText = s.maxOpens !== null ? ` · ${visits}/${s.maxOpens} visits` : "";
    list.append(row(s.name, `${durationPair(used, limit, " / ")}${visitText}`, used, limit));
  }
  for (const b of settings.budgets) {
    if (!b.enabled || b.sites.length === 0) continue;
    const used = b.sites.reduce((sum, id) => sum + (settings.sites.find((s) => s.id === id)?.enabled ? minutes(day.ms[id]) : 0), 0);
    list.append(row(b.name, `${durationPair(used, b.minutes, " / ")} together`, used, b.minutes));
  }
  for (const f of settings.features) {
    if (!f.enabled) continue;
    const name = FEATURES.find((x) => x.id === f.id)?.name ?? f.id;
    const used = minutes(day.featureMs[f.id]);
    list.append(row(name, durationPair(used, f.minutes, " / "), used, f.minutes));
  }

  app.append(
    header(settings),
    card,
    ...notes,
    list,
    h("div", { class: "foot" }, h("button", { class: "btn outline", type: "button", onclick: () => openSettings("#sites") }, "Edit limits"), h("button", { class: "btn outline", type: "button", onclick: () => openSettings("#phone") }, icon("phone"), "From phone")),
  );
}

function row(name: string, nums: string, used: number, limit: number) {
  return h("div", { class: "item" }, tile(name), h("div", { class: "grow" }, h("div", { class: "line" }, h("span", { class: "name" }, name), h("span", { class: "nums" }, nums)), bar(used, limit)));
}

async function load() {
  // Count the last few seconds first, so the numbers are up to date.
  await ext.runtime.sendMessage({ type: "flush" }).catch(() => {});
  render(await loadSettings(), await loadDay());
}

void load();
ext.storage.onChanged.addListener((changes, area) => {
  if (area === "local" && Object.keys(changes).some((k) => k === "settings" || k.startsWith("day:"))) {
    void (async () => render(await loadSettings(), await loadDay()))();
  }
});
