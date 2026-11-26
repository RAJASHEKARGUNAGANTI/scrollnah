// ScrollNah's settings page. Changes are a draft until "Lock it in" (like the app's Limits screen).
// In Beast mode, anything that gives more time needs the 10-minute cool-down first (lib/beast.ts).

import { allowsLoosening, guardState, loosenings, WAIT_MS } from "../lib/beast.ts";
import { ext, isFirefox } from "../lib/ext.ts";
import { applyImport, type PhoneImport } from "../lib/phone.ts";
import { moodToday } from "../lib/rules.ts";
import {
  cleanSettings,
  HIDE_OPTIONS,
  MASCOT_NAME,
  MINUTES,
  newSite,
  OPENS,
  STRICTNESS,
  type Budget,
  type MascotId,
  type Settings,
  type SiteLimit,
} from "../lib/settings.ts";
import { cleanSiteInput, FEATURES, KNOWN_SITES, knownSite, patternsFor } from "../lib/sites.ts";
import { clearEverything, DAYS_KEPT, loadBeastUnlockAt, loadDay, loadSettings, saveBeastUnlockAt, saveSettings } from "../lib/store.ts";
import { duration, parseTimeInput, timeInputValue } from "../lib/time.ts";
import { phoneDropZone, phonePreview } from "./phone-ui.ts";
import { $, h, icon, mascotCanvas, stepper, tile, toast, toggle } from "./ui.ts";

const WEBSITE = "https://scrollnah.vercel.app";
const FEEDBACK = "https://github.com/RAJASHEKARGUNAGANTI/scrollnah/discussions";

const app = $("#app");
const savebar = $("#savebar");
const guardDialog = $("#guard") as HTMLDialogElement;

let saved: Settings;
let draft: Settings;
let unlockAt = 0;
let access: Record<string, boolean> = {}; // site id -> ScrollNah may see it
const expanded = new Set<string>();
let pendingImport: PhoneImport | null = null;
let importError = "";
let addError = "";

const fmtMin = (v: number) => duration(v);

function isDirty(): boolean {
  return JSON.stringify(cleanSettings(draft)) !== JSON.stringify(saved);
}

/** After any change to the draft: update the save bar (and redraw when the shape of the page changes). */
function changed(redraw = false) {
  if (redraw) render();
  renderSaveBar();
}

async function checkAccess() {
  const result: Record<string, boolean> = {};
  for (const s of draft.sites) result[s.id] = await ext.permissions.contains({ origins: patternsFor(s.hosts) }).catch(() => false);
  access = result;
}

/** Asks the browser for access to these sites. Must run straight from a click (browsers require that). */
function requestAccess(hosts: string[]): Promise<boolean> {
  return ext.permissions.request({ origins: patternsFor(hosts) }).catch(() => false);
}

// ---- Sections ----

function section(id: string, title: string, intro: string | null, ...children: (Node | null)[]) {
  return h("section", { class: "card", id, "aria-labelledby": `${id}-title` }, h("div", { class: "head" }, h("h2", { id: `${id}-title` }, title), intro ? h("p", { class: "muted" }, intro) : null), ...children);
}

function siteRow(site: SiteLimit): HTMLElement {
  const open = expanded.has(site.id);
  const row = h("div", { class: `site${site.enabled ? "" : " off"}` });
  const moreBtn = h(
    "button",
    { class: "link-btn small", type: "button", "aria-expanded": String(open), onclick: () => { if (open) expanded.delete(site.id); else expanded.add(site.id); render(); } },
    open ? "Less" : "More",
  );
  row.append(
    h(
      "div",
      { class: "main" },
      h("div", { class: "who" }, tile(site.name), h("div", null, h("div", { class: "name" }, site.name), h("div", { class: "small muted" }, site.hosts.join(", "), " · ", moreBtn))),
      h(
        "div",
        { class: "controls" },
        stepper({ value: site.minutes, ...MINUTES, label: `minutes a day on ${site.name}`, format: fmtMin, onChange: (v) => { site.minutes = v; changed(); } }),
        toggle(site.enabled, `Limit ${site.name}`, (on) => { site.enabled = on; changed(true); }),
      ),
    ),
  );
  if (access[site.id] === false) {
    row.append(
      h(
        "div",
        { class: "warn-box" },
        h("span", { class: "grow" }, `ScrollNah can't see ${site.name} yet, so its time isn't counted.`),
        h("button", { class: "btn small", type: "button", onclick: () => void requestAccess(site.hosts).then(async () => { await checkAccess(); render(); }) }, "Allow"),
      ),
    );
  }
  if (open) {
    const weekend = site.weekendMinutes;
    const opens = site.maxOpens;
    row.append(
      h(
        "div",
        { class: "more" },
        h(
          "div",
          { class: "row" },
          h("span", null, "Different limit on weekends"),
          h(
            "div",
            { class: "controls row" },
            weekend !== null ? stepper({ value: weekend, ...MINUTES, label: `minutes on weekends`, format: fmtMin, onChange: (v) => { site.weekendMinutes = v; changed(); } }) : null,
            toggle(weekend !== null, `Weekend limit for ${site.name}`, (on) => { site.weekendMinutes = on ? site.minutes : null; changed(true); }),
          ),
        ),
        h(
          "div",
          { class: "row" },
          h("span", null, "Visits a day"),
          h(
            "div",
            { class: "controls row" },
            opens !== null ? stepper({ value: opens, min: OPENS.min, max: OPENS.max, step: 1, label: "visits a day", format: (v) => `${v} ${v === 1 ? "visit" : "visits"}`, onChange: (v) => { site.maxOpens = v; changed(); } }) : null,
            toggle(opens !== null, `Visit limit for ${site.name}`, (on) => { site.maxOpens = on ? 5 : null; changed(true); }),
          ),
        ),
        h("p", { class: "small muted" }, "A visit is a tab arriving on the site. Switching between tabs doesn't count."),
        h(
          "div",
          null,
          h("button", {
            class: "btn outline small",
            type: "button",
            onclick: () => {
              draft.sites = draft.sites.filter((s) => s.id !== site.id);
              for (const b of draft.budgets) b.sites = b.sites.filter((id) => id !== site.id);
              changed(true);
            },
          }, icon("trash"), `Remove ${site.name}`),
        ),
      ),
    );
  }
  return row;
}

function addSite(id: string) {
  if (draft.sites.some((s) => s.id === id)) {
    toast("That's already on your list.");
    return;
  }
  const site = newSite(id);
  // Ask first (straight from the click), then add.
  void requestAccess(site.hosts).then(async (granted) => {
    if (!granted) {
      addError = `ScrollNah needs access to ${site.name} to count your time there.`;
      render();
      return;
    }
    addError = "";
    draft.sites.push(site);
    await checkAccess();
    changed(true);
  });
}

function sitesSection() {
  const input = h("input", { class: "field", type: "text", placeholder: "Add a site, e.g. twitch.tv", "aria-label": "Website address", autocomplete: "off", spellcheck: "false" });
  const form = h("form", { class: "add" }, input, h("button", { class: "btn", type: "submit" }, icon("plus"), "Add"));
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const id = cleanSiteInput(input.value);
    if (!id) {
      addError = "That doesn't look like a website address.";
      render();
      return;
    }
    addSite(id);
  });
  const missing = KNOWN_SITES.filter((k) => !draft.sites.some((s) => s.id === k.id)).slice(0, 10);
  return section(
    "sites",
    "Your time-sinks",
    "A daily limit for each site. Time counts only while the site is the tab you're looking at.",
    draft.sites.length ? h("div", null, ...draft.sites.map(siteRow)) : h("p", { class: "muted" }, "No sites yet. Add one below."),
    form,
    addError ? h("p", { class: "error", role: "alert" }, addError) : null,
    missing.length ? h("div", { class: "chips", "aria-label": "Quick add" }, ...missing.map((k) => h("button", { class: "chip", type: "button", onclick: () => addSite(k.id) }, `+ ${k.name}`))) : null,
  );
}

function strictSection() {
  const cards = STRICTNESS.map((s) =>
    h(
      "button",
      { class: "choice", type: "button", role: "radio", "aria-checked": String(draft.strictness === s.id), onclick: () => { draft.strictness = s.id; changed(true); } },
      h("b", null, s.title),
      h("span", { class: "muted small" }, s.text),
    ),
  );
  const first = draft.sites[0]?.id ?? "youtube.com";
  return section(
    "strict",
    "How strict should I be?",
    null,
    h("div", { class: "choices", role: "radiogroup", "aria-label": "Strictness" }, ...cards),
    h("div", null, h("button", { class: "link-btn", type: "button", onclick: () => void ext.tabs.create({ url: ext.runtime.getURL(`pages/pause.html?preview=1&site=${encodeURIComponent(first)}&reason=limit`) }) }, "See the Pause page")),
  );
}

function budgetsSection() {
  const list = draft.budgets.map((b) => budgetCard(b));
  const canAdd = draft.sites.length >= 2;
  return section(
    "budgets",
    "Shared budgets",
    "A few sites with one limit together, e.g. Instagram + Reddit, 1 hour. Each site keeps its own limit too.",
    ...list,
    h(
      "div",
      null,
      h("button", {
        class: "btn outline",
        type: "button",
        disabled: !canAdd,
        onclick: () => {
          draft.budgets.push({ id: `b${Date.now()}`, name: "Social", minutes: 60, enabled: true, sites: [] });
          changed(true);
        },
      }, icon("plus"), "Add a shared budget"),
      canAdd ? null : h("p", { class: "small muted", style: "margin-top:10px" }, "Add at least two sites first."),
    ),
  );
}

function budgetCard(b: Budget) {
  const name = h("input", { class: "field", type: "text", value: b.name, maxlength: "30", "aria-label": "Budget name" });
  name.addEventListener("input", () => { b.name = name.value; changed(); });
  return h(
    "div",
    { class: "budget" },
    h(
      "div",
      { class: "row" },
      name,
      h("span", { class: "grow" }),
      stepper({ value: b.minutes, ...MINUTES, label: "minutes together", format: fmtMin, onChange: (v) => { b.minutes = v; changed(); } }),
      toggle(b.enabled, `${b.name} on`, (on) => { b.enabled = on; changed(); }),
    ),
    h(
      "div",
      { class: "chips", "aria-label": "Sites in this budget" },
      ...draft.sites.map((s) =>
        h("button", {
          class: "chip",
          type: "button",
          "aria-pressed": String(b.sites.includes(s.id)),
          onclick: () => {
            b.sites = b.sites.includes(s.id) ? b.sites.filter((x) => x !== s.id) : [...b.sites, s.id];
            changed(true);
          },
        }, s.name),
      ),
    ),
    h("div", null, h("button", { class: "link-btn small", type: "button", onclick: () => { draft.budgets = draft.budgets.filter((x) => x !== b); changed(true); } }, `Remove ${b.name || "this budget"}`)),
  );
}

function reelsSection() {
  return section(
    "reels",
    "Reels & Shorts",
    "Their own daily limit, on top of the site's. Normal videos still play when it's used up.",
    ...draft.features.map((f) => {
      const feature = FEATURES.find((x) => x.id === f.id)!;
      const hasSite = draft.sites.some((s) => s.id === feature.site);
      return h(
        "div",
        { class: `line-item${hasSite ? "" : " disabled"}` },
        h("div", { class: "text" }, h("b", null, feature.name), h("p", { class: "small muted" }, hasSite ? "Minutes a day" : `Add ${knownSite(feature.site)?.name} to your sites first`)),
        hasSite ? h("div", { class: "row" }, stepper({ value: f.minutes, min: 5, max: 180, step: 5, label: `minutes of ${feature.name}`, format: fmtMin, onChange: (v) => { f.minutes = v; changed(); } }), toggle(f.enabled, `${feature.name} limit`, (on) => { f.enabled = on; changed(); })) : null,
      );
    }),
  );
}

function hideSection() {
  return section(
    "hide",
    "Hide the endless parts",
    "Keep the site, lose the feed that never ends.",
    ...HIDE_OPTIONS.map((o) => {
      const hasSite = draft.sites.some((s) => s.id === o.site);
      return h(
        "div",
        { class: `line-item${hasSite ? "" : " disabled"}` },
        h("div", { class: "text" }, h("b", null, o.title), h("p", { class: "small muted" }, hasSite ? o.text : `Add ${knownSite(o.site)?.name} to your sites first`)),
        hasSite ? toggle(draft.hide[o.id], o.title, (on) => { draft.hide[o.id] = on; changed(); }) : null,
      );
    }),
  );
}

function timeInput(value: number, label: string, onChange: (m: number) => void) {
  const input = h("input", { class: "field", type: "time", value: timeInputValue(value), "aria-label": label });
  input.addEventListener("change", () => {
    const m = parseTimeInput(input.value);
    if (m !== null) onChange(m);
  });
  return input;
}

function scheduleSection() {
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const work = draft.work;
  return section(
    "schedule",
    "Bedtime and work hours",
    "Your sites pause during these hours, whatever time is left.",
    h(
      "div",
      { class: "line-item" },
      h("div", { class: "text" }, h("b", null, "Bedtime"), h("div", { class: "times small" }, "From", timeInput(draft.bedtime.start, "Bedtime starts", (m) => { draft.bedtime.start = m; changed(); }), "to", timeInput(draft.bedtime.end, "Bedtime ends", (m) => { draft.bedtime.end = m; changed(); }))),
      toggle(draft.bedtime.enabled, "Bedtime", (on) => { draft.bedtime.enabled = on; changed(); }),
    ),
    h(
      "div",
      { class: "line-item" },
      h(
        "div",
        { class: "text stack" },
        h("b", null, "Work hours"),
        h("div", { class: "times small" }, "From", timeInput(work.start, "Work starts", (m) => { work.start = m; changed(); }), "to", timeInput(work.end, "Work ends", (m) => { work.end = m; changed(); })),
        h(
          "div",
          { class: "chips days", "aria-label": "Work days" },
          ...days.map((d, i) =>
            h("button", {
              class: "chip",
              type: "button",
              "aria-pressed": String(work.days?.includes(i + 1) ?? false),
              onclick: (e: Event) => {
                const day = i + 1;
                const list = work.days ?? [];
                work.days = list.includes(day) ? list.filter((x) => x !== day) : [...list, day].sort();
                (e.currentTarget as HTMLElement).setAttribute("aria-pressed", String(work.days.includes(day)));
                changed();
              },
            }, d),
          ),
        ),
      ),
      toggle(work.enabled, "Work hours", (on) => { work.enabled = on; changed(); }),
    ),
  );
}

function mascotSection() {
  const pick = (id: MascotId) =>
    h(
      "button",
      { class: "choice mascot-choice", type: "button", role: "radio", "aria-checked": String(draft.mascot === id), onclick: () => { draft.mascot = id; changed(true); } },
      mascotCanvas(96, { mascot: id, mood: 2, flowers: draft.flowers }),
      h("b", null, MASCOT_NAME[id]),
      h("span", { class: "small muted" }, id === "blobby" ? "An orange blob" : "A plant in a pot"),
    );
  const name = h("input", { class: "field", type: "text", value: draft.name, maxlength: "30", placeholder: "Your name", "aria-label": "Your name", autocomplete: "given-name" });
  name.addEventListener("input", () => { draft.name = name.value; changed(); });
  return section(
    "mascot",
    "Your mascot",
    "Your buddy sits in the toolbar and reacts to your day.",
    h("div", { class: "choices", role: "radiogroup", "aria-label": "Mascot" }, pick("blobby"), pick("bloomy")),
    h("label", { class: "stack" }, h("span", { class: "bold" }, `What should ${MASCOT_NAME[draft.mascot]} call you?`), name),
  );
}

function phoneSection() {
  const children: (Node | null)[] = [];
  if (saved.imported) {
    const days = Math.floor((Date.now() - saved.imported.at) / 86_400_000);
    children.push(h("p", { class: "pill" }, days <= 0 ? "Imported from your phone today" : `Imported from your phone ${days} ${days === 1 ? "day" : "days"} ago`));
  }
  children.push(
    h(
      "ol",
      { class: "steps" },
      h("li", null, "In the ScrollNah app: ", h("b", null, "Settings > Backup > Copy limits to computer"), "."),
      h("li", null, "Send the file to this computer: email it to yourself, Quick Share, Google Drive or a USB cable."),
      h("li", null, "Drop it here."),
    ),
  );
  if (pendingImport) {
    children.push(
      phonePreview(
        pendingImport,
        h(
          "div",
          { class: "row wrap" },
          h("button", { class: "btn", type: "button", onclick: () => doImport() }, "Import"),
          h("button", { class: "btn outline", type: "button", onclick: () => { pendingImport = null; render(); } }, "Cancel"),
        ),
      ),
    );
  } else {
    children.push(
      phoneDropZone(
        (imp) => { pendingImport = imp; importError = ""; render(); document.getElementById("phone")?.scrollIntoView({ block: "start" }); },
        (msg) => { importError = msg; render(); },
      ),
    );
  }
  if (importError) children.push(h("p", { class: "error", role: "alert" }, importError));
  return section("phone", "Bring your phone's limits", "Set your limits once in the ScrollNah app and copy them here. Nothing is uploaded.", ...children);
}

/** Import: asks for access to the new sites (straight from the click), then saves like "Lock it in". */
function doImport() {
  const imp = pendingImport;
  if (!imp) return;
  const hosts = imp.sites.flatMap((s) => s.site.hosts);
  void (hosts.length ? requestAccess(hosts) : Promise.resolve(true)).then(async () => {
    draft = applyImport(draft, imp, Date.now());
    pendingImport = null;
    await checkAccess();
    const ok = await save();
    if (ok) toast(`Brought over ${imp.sites.length} ${imp.sites.length === 1 ? "limit" : "limits"} from your phone`);
    render();
  });
}

async function privateSection() {
  const allowed = await ext.extension.isAllowedIncognitoAccess().catch(() => false);
  const firefox = isFirefox();
  const how = firefox
    ? h("p", null, "To count them too: open the menu > Add-ons and themes > ScrollNah > ", h("b", null, "Run in Private Windows: Allow"), ".")
    : h(
        "div",
        { class: "stack" },
        h("p", null, "To count them too: open ScrollNah's details and switch on ", h("b", null, "Allow in Incognito"), "."),
        h("div", null, h("button", { class: "btn outline small", type: "button", onclick: () => void ext.tabs.create({ url: `chrome://extensions/?id=${ext.runtime.id}` }).catch(() => toast("Open chrome://extensions and find ScrollNah.")) }, "Open ScrollNah's details")),
      );
  return section(
    "private",
    firefox ? "Private windows" : "Incognito windows",
    null,
    allowed ? h("p", null, h("b", null, "On. "), "ScrollNah counts private windows too.") : h("div", { class: "stack" }, h("p", null, h("b", null, "Off. "), "Time in private windows isn't counted, so they're an easy way around your limits."), how),
  );
}

function dataSection() {
  return section(
    "data",
    "Your data",
    null,
    h("p", null, `Everything stays in this browser: your settings and, for each day, minutes and visits per site (the last ${DAYS_KEPT} days). No account, no server, nothing uploaded. ScrollNah never reads what's on the pages.`),
    h(
      "div",
      { class: "row wrap" },
      h("button", {
        class: "btn outline",
        type: "button",
        onclick: async () => {
          if (saved.strictness === "beast" && !allowsLoosening(guardState(unlockAt, Date.now(), saved.setupAt))) {
            openGuard(["starting over without limits"]);
            return;
          }
          if (!confirm("Reset ScrollNah? This deletes your limits and today's counts in this browser. It can't be undone.")) return;
          await clearEverything();
          location.href = ext.runtime.getURL("pages/welcome.html");
        },
      }, icon("trash"), "Reset ScrollNah"),
    ),
    h(
      "p",
      { class: "small muted" },
      `ScrollNah ${ext.runtime.getManifest().version} · `,
      h("a", { href: WEBSITE, target: "_blank", rel: "noopener" }, "Website"),
      " · ",
      h("a", { href: `${WEBSITE}/privacy#extension`, target: "_blank", rel: "noopener" }, "Privacy"),
      " · ",
      h("a", { href: FEEDBACK, target: "_blank", rel: "noopener" }, "Ideas and feedback"),
    ),
  );
}

// ---- Page ----

async function render() {
  const scroll = window.scrollY;
  const day = await loadDay();
  const mood = moodToday(saved, day, Date.now());
  const top = h(
    "header",
    null,
    mascotCanvas(96, { mascot: draft.mascot, mood: mood.level, flowers: draft.flowers }, "mascot bob"),
    h("div", null, h("h1", null, "ScrollNah settings"), h("p", { class: "muted" }, "Everything stays in this browser.")),
  );
  const jump = h(
    "nav",
    { class: "jump", "aria-label": "Sections" },
    ...[["sites", "Sites"], ["strict", "Strictness"], ["budgets", "Budgets"], ["reels", "Reels & Shorts"], ["hide", "Hide"], ["schedule", "Bedtime"], ["mascot", "Mascot"], ["phone", "From phone"], ["data", "Your data"]].map(([id, label]) =>
      h("a", { class: "chip", href: `#${id}` }, label),
    ),
  );
  const setupNote = saved.setupDone
    ? null
    : h("div", { class: "card lime row wrap" }, h("p", { class: "grow bold" }, "You haven't finished setting up yet."), h("a", { class: "btn ink", href: "welcome.html" }, "Start setup"));
  app.replaceChildren(top, jump, setupNote ?? "", sitesSection(), strictSection(), budgetsSection(), reelsSection(), hideSection(), scheduleSection(), mascotSection(), phoneSection(), await privateSection(), dataSection());
  window.scrollTo(0, scroll);
}

function renderSaveBar() {
  const dirty = isDirty();
  savebar.classList.toggle("hidden", !dirty);
  if (!dirty) return;
  const state = guardState(unlockAt, Date.now(), saved.setupAt);
  const note = saved.strictness === "beast" && state.kind === "open" ? ` You can loosen limits for ${Math.ceil(state.msLeft / 60_000)} more min.` : "";
  savebar.replaceChildren(
    h("span", { class: "grow" }, `You have changes that aren't saved yet.${note}`),
    h("button", { class: "btn outline small", type: "button", onclick: () => { draft = structuredClone(saved); render(); renderSaveBar(); } }, "Undo"),
    h("button", { class: "btn ink", type: "button", onclick: () => void save() }, icon("lock"), "Lock it in"),
  );
}

/** Saves the draft. In Beast mode, loosening waits for the cool-down. Returns true if saved. */
async function save(): Promise<boolean> {
  const next = cleanSettings(draft);
  if (saved.strictness === "beast") {
    const loose = loosenings(saved, next);
    if (loose.length && !allowsLoosening(guardState(unlockAt, Date.now(), saved.setupAt))) {
      openGuard(loose);
      return false;
    }
  }
  // Give back access to sites that were removed (unless another site still uses that address).
  const keep = new Set(next.sites.flatMap((s) => patternsFor(s.hosts)));
  const drop = saved.sites.filter((s) => !next.sites.some((n) => n.id === s.id)).flatMap((s) => patternsFor(s.hosts)).filter((p) => !keep.has(p));
  await saveSettings(next);
  saved = next;
  draft = structuredClone(next);
  if (drop.length) await ext.permissions.remove({ origins: drop }).catch(() => {});
  renderSaveBar();
  toast("Locked in!");
  await render();
  return true;
}

// ---- Beast cool-down dialog ----

let guardTimer = 0;
function openGuard(changes: string[]) {
  const draw = () => {
    const state = guardState(unlockAt, Date.now(), saved.setupAt);
    if (state.kind === "waiting") {
      const left = Math.ceil(state.msLeft / 1000);
      guardDialog.replaceChildren(
        h("h2", null, "Cooling down"),
        h("p", { class: "timer", "aria-live": "polite" }, `${Math.floor(left / 60)}:${String(left % 60).padStart(2, "0")}`),
        h("p", null, "When this reaches zero, you'll have 15 minutes to save looser limits. Still want them then?"),
        h("button", { class: "btn wide", type: "button", onclick: () => guardDialog.close() }, "Close"),
      );
      return;
    }
    if (allowsLoosening(state)) {
      guardDialog.close();
      void save();
      return;
    }
    guardDialog.replaceChildren(
      h("h2", null, "Beast mode is on"),
      h("p", null, "These changes give you more time:"),
      h("ul", null, ...changes.map((c) => h("li", null, c))),
      h("p", null, "Wait 10 minutes first. If you still want them then, you'll have 15 minutes to save."),
      h(
        "div",
        { class: "row wrap" },
        h("button", { class: "btn", type: "button", onclick: async () => { unlockAt = Date.now() + WAIT_MS; await saveBeastUnlockAt(unlockAt); draw(); } }, "Start the 10-minute wait"),
        h("button", { class: "btn outline", type: "button", onclick: () => guardDialog.close() }, "Keep my limits"),
      ),
    );
  };
  draw();
  clearInterval(guardTimer);
  guardTimer = window.setInterval(() => (guardDialog.open ? draw() : clearInterval(guardTimer)), 1000);
  if (!guardDialog.open) guardDialog.showModal();
}

// ---- Start ----

async function init() {
  saved = await loadSettings();
  draft = structuredClone(saved);
  unlockAt = await loadBeastUnlockAt();
  await checkAccess();
  await render();
  renderSaveBar();
  if (location.hash) document.querySelector(location.hash)?.scrollIntoView({ block: "start" });
}

void init();

window.addEventListener("beforeunload", (e) => {
  if (isDirty()) e.preventDefault(); // "Leave site? Changes you made may not be saved."
});

ext.storage.onChanged.addListener((changes, area) => {
  if (area !== "local") return;
  if (changes.settings) {
    void (async () => {
      const wasDirty = isDirty();
      saved = await loadSettings();
      if (!wasDirty) draft = structuredClone(saved);
      await checkAccess();
      await render();
      renderSaveBar();
    })();
  }
  if (changes.beastUnlockAt) unlockAt = (changes.beastUnlockAt.newValue as number) ?? 0;
});
ext.permissions.onAdded.addListener(() => void checkAccess().then(render));
ext.permissions.onRemoved.addListener(() => void checkAccess().then(render));
