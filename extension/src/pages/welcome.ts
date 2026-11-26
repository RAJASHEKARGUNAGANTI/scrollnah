// Setup, opened once after installing: pick your buddy, your sites (or bring them from your phone),
// how strict, access to those sites, private windows, done. Calm, plain words when asking for access.

import { ext, isFirefox } from "../lib/ext.ts";
import { applyImport, type PhoneImport } from "../lib/phone.ts";
import { MASCOT_NAME, MINUTES, newSite, STRICTNESS, type MascotId, type Settings, type SiteLimit } from "../lib/settings.ts";
import { cleanSiteInput, KNOWN_SITES, patternsFor } from "../lib/sites.ts";
import { loadSettings, saveSettings } from "../lib/store.ts";
import { duration } from "../lib/time.ts";
import { phoneDropZone, phonePreview } from "./phone-ui.ts";
import { $, h, icon, mascotCanvas, stepper, tile, toast, toggle } from "./ui.ts";

const app = $("#app");
const STEPS = ["buddy", "sites", "strict", "access", "private", "done"] as const;
type Step = (typeof STEPS)[number];

let s: Settings;
let step: Step = "buddy";
/** Every site shown on the sites step, and which are picked. */
let rows: SiteLimit[] = [];
const picked = new Set<string>();
let pendingImport: PhoneImport | null = null;
let message = "";

const DEFAULTS: Record<string, number> = { "youtube.com": 30 };

function go(next: Step) {
  step = next;
  message = "";
  render();
  window.scrollTo(0, 0);
  app.querySelector<HTMLElement>("h1")?.focus();
}

function chosenSites(): SiteLimit[] {
  return rows.filter((r) => picked.has(r.id));
}

function nav(back: Step | null, next: HTMLElement) {
  return h("div", { class: "nav" }, back ? h("button", { class: "btn outline", type: "button", onclick: () => go(back) }, icon("back"), "Back") : h("span"), next);
}

function stepBuddy() {
  const pick = (id: MascotId) =>
    h(
      "button",
      { class: "choice mascot-choice", type: "button", role: "radio", "aria-checked": String(s.mascot === id), onclick: () => { s.mascot = id; render(); } },
      mascotCanvas(120, { mascot: id, mood: 1, flowers: s.flowers }, "mascot bob"),
      h("b", null, MASCOT_NAME[id]),
      h("span", { class: "small muted" }, id === "blobby" ? "An orange blob. Melts when you overdo it." : "A plant in a pot. Wilts when you overdo it."),
    );
  const name = h("input", { class: "field", type: "text", value: s.name, maxlength: "30", placeholder: "Your name (optional)", "aria-label": "Your name", autocomplete: "given-name" });
  name.addEventListener("input", () => (s.name = name.value.trim()));
  return [
    h("h1", { tabindex: "-1" }, "Hi! Pick your buddy"),
    h("p", { class: "lead muted" }, "Your buddy sits in the toolbar and reacts to how much you scroll today."),
    h("div", { class: "choices", role: "radiogroup", "aria-label": "Mascot" }, pick("blobby"), pick("bloomy")),
    h("label", { class: "stack" }, h("span", { class: "bold" }, `What should ${MASCOT_NAME[s.mascot]} call you?`), name),
    nav(null, h("button", { class: "btn", type: "button", onclick: () => go("sites") }, "Next", icon("arrow"))),
  ];
}

function siteRow(site: SiteLimit) {
  const on = picked.has(site.id);
  return h(
    "div",
    { class: `site${on ? "" : " off"}` },
    h(
      "div",
      { class: "main" },
      h("div", { class: "who" }, tile(site.name), h("div", null, h("div", { class: "name" }, site.name), h("div", { class: "small muted" }, site.id))),
      h(
        "div",
        { class: "controls" },
        on ? stepper({ value: site.minutes, ...MINUTES, label: `minutes a day on ${site.name}`, format: duration, onChange: (v) => (site.minutes = v) }) : null,
        toggle(on, `Limit ${site.name}`, (v) => { if (v) picked.add(site.id); else picked.delete(site.id); render(); }),
      ),
    ),
  );
}

function stepSites() {
  const input = h("input", { class: "field", type: "text", placeholder: "Another site, e.g. twitch.tv", "aria-label": "Website address", autocomplete: "off", spellcheck: "false" });
  const form = h("form", { class: "add" }, input, h("button", { class: "btn outline", type: "submit" }, icon("plus"), "Add"));
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const id = cleanSiteInput(input.value);
    if (!id) {
      message = "That doesn't look like a website address.";
      render();
      return;
    }
    if (!rows.some((r) => r.id === id)) rows.push(newSite(id, 20));
    picked.add(id);
    message = "";
    render();
  });

  const phone = pendingImport
    ? phonePreview(
        pendingImport,
        h(
          "div",
          { class: "row wrap" },
          h("button", {
            class: "btn",
            type: "button",
            onclick: () => {
              const imp = pendingImport!;
              s = applyImport(s, imp, Date.now());
              for (const { site } of imp.sites) {
                const i = rows.findIndex((r) => r.id === site.id);
                if (i >= 0) rows[i] = site;
                else rows.push(site);
                if (site.enabled) picked.add(site.id);
              }
              pendingImport = null;
              toast(`Brought over ${imp.sites.length} ${imp.sites.length === 1 ? "limit" : "limits"} from your phone`);
              render();
            },
          }, "Use these"),
          h("button", { class: "btn outline", type: "button", onclick: () => { pendingImport = null; render(); } }, "Cancel"),
        ),
      )
    : h(
        "details",
        { class: "card" },
        h("summary", { class: "bold" }, "Use ScrollNah on your phone? Bring your limits over"),
        h(
          "div",
          { class: "stack", style: "margin-top:12px" },
          h("p", { class: "small" }, "In the app: Settings > Backup > Copy limits to computer. Send the file to this computer, then drop it here."),
          phoneDropZone((imp) => { pendingImport = imp; render(); }, (msg) => { message = msg; render(); }),
        ),
      );

  const count = picked.size;
  return [
    h("h1", { tabindex: "-1" }, "Which sites eat your time?"),
    h("p", { class: "lead muted" }, "Pick them and give each a daily limit. You can change these any time."),
    phone,
    h("div", { class: "card pick" }, ...rows.map(siteRow)),
    form,
    message ? h("p", { class: "error", role: "alert" }, message) : null,
    nav("buddy", h("button", { class: "btn", type: "button", disabled: count === 0, onclick: () => go("strict") }, count ? `Next (${count})` : "Pick at least one", icon("arrow"))),
  ];
}

function stepStrict() {
  return [
    h("h1", { tabindex: "-1" }, "How strict should I be?"),
    h("p", { class: "lead muted" }, "What happens when a site's time is up."),
    h(
      "div",
      { class: "choices", role: "radiogroup", "aria-label": "Strictness" },
      ...STRICTNESS.map((x) =>
        h("button", { class: "choice", type: "button", role: "radio", "aria-checked": String(s.strictness === x.id), onclick: () => { s.strictness = x.id; render(); } }, h("b", null, x.title), h("span", { class: "muted small" }, x.text)),
      ),
    ),
    nav("sites", h("button", { class: "btn", type: "button", onclick: () => go("access") }, "Next", icon("arrow"))),
  ];
}

function stepAccess() {
  const sites = chosenSites();
  const allow = h("button", {
    class: "btn",
    type: "button",
    onclick: () => {
      // Straight from the click, as browsers require.
      void ext.permissions
        .request({ origins: sites.flatMap((x) => patternsFor(x.hosts)) })
        .catch(() => false)
        .then(async (granted) => {
          if (!granted) {
            message = "No problem. ScrollNah can't count anything without it, so try again when you're ready.";
            render();
            return;
          }
          // Keep sites set up earlier that aren't on this list; replace the ones that are.
          const others = s.sites.filter((x) => !rows.some((r) => r.id === x.id));
          s.sites = [...others, ...sites.map((x) => ({ ...x, enabled: true }))];
          s.setupDone = true;
          s.setupAt = Date.now();
          await saveSettings(s);
          const incognito = await ext.extension.isAllowedIncognitoAccess().catch(() => true);
          go(incognito ? "done" : "private");
        });
    },
  }, "Allow these sites");
  return [
    h("h1", { tabindex: "-1" }, "Let ScrollNah see these sites"),
    h("p", { class: "lead" }, "To count your time and show the Pause page, ScrollNah needs access to these sites only:"),
    h("div", { class: "access-list" }, ...sites.map((x) => h("span", { class: "pill" }, x.hosts.join(", ")))),
    h("p", { class: "muted" }, "It notes which of these sites is open, and for how long. It doesn't read what's on the pages, and nothing leaves this browser."),
    message ? h("p", { class: "error", role: "alert" }, message) : null,
    nav("strict", allow),
  ];
}

function stepPrivate() {
  const firefox = isFirefox();
  return [
    h("h1", { tabindex: "-1" }, firefox ? "Private windows too?" : "Incognito windows too?"),
    h("p", { class: "lead" }, "Right now ScrollNah doesn't count private windows, so they're an easy way around your limits."),
    firefox
      ? h("p", null, "To count them: open the menu > Add-ons and themes > ScrollNah > ", h("b", null, "Run in Private Windows: Allow"), ".")
      : h(
          "div",
          { class: "stack" },
          h("p", null, "To count them: open ScrollNah's details and switch on ", h("b", null, "Allow in Incognito"), "."),
          h("div", null, h("button", { class: "btn outline", type: "button", onclick: () => void ext.tabs.create({ url: `chrome://extensions/?id=${ext.runtime.id}` }).catch(() => toast("Open chrome://extensions and find ScrollNah.")) }, "Open ScrollNah's details")),
        ),
    nav(null, h("button", { class: "btn", type: "button", onclick: () => go("done") }, "Next", icon("arrow"))),
  ];
}

function stepDone() {
  const firefox = isFirefox();
  return [
    h("div", { class: "hero" }, mascotCanvas(160, { mascot: s.mascot, mood: 1, flowers: s.flowers }, "mascot bob")),
    h("h1", { tabindex: "-1" }, s.name ? `You're all set, ${s.name}!` : "You're all set!"),
    h("p", { class: "lead" }, `${MASCOT_NAME[s.mascot]} is watching ${chosenSites().length === 1 ? "your site" : `your ${chosenSites().length} sites`}. I'll tell you when time's up.`),
    h(
      "div",
      { class: "card pin" },
      h("b", null, "Pin me to your toolbar"),
      h("p", { class: "small" }, firefox ? "Click the puzzle piece at the top right, then the gear next to ScrollNah > Pin to Toolbar." : "Click the puzzle piece at the top right, then the pin next to ScrollNah. You'll see my mood and the minutes left."),
    ),
    h(
      "div",
      { class: "nav" },
      h("button", { class: "btn outline", type: "button", onclick: () => (location.href = "options.html") }, "More settings"),
      h("button", { class: "btn", type: "button", onclick: () => void ext.tabs.getCurrent().then((t) => (t?.id !== undefined ? ext.tabs.remove(t.id) : window.close())) }, "Done"),
    ),
  ];
}

function render() {
  const index = STEPS.indexOf(step);
  const body =
    step === "buddy" ? stepBuddy()
    : step === "sites" ? stepSites()
    : step === "strict" ? stepStrict()
    : step === "access" ? stepAccess()
    : step === "private" ? stepPrivate()
    : stepDone();
  app.replaceChildren(
    h("div", { class: "top" }, h("span", { class: "wordmark" }, "scrollnah"), h("div", { class: "dots", "aria-label": `Step ${index + 1} of ${STEPS.length}` }, ...STEPS.map((_, i) => h("span", { class: i <= index ? "on" : "" })))),
    h("section", { class: "step" }, ...body),
  );
}

async function init() {
  s = await loadSettings();
  // The sites step lists the popular sites plus any set up before (running setup again keeps them).
  rows = KNOWN_SITES.filter((k) => k.popular).map((k) => newSite(k.id, DEFAULTS[k.id] ?? 20));
  for (const site of s.sites) {
    const i = rows.findIndex((r) => r.id === site.id);
    if (i >= 0) rows[i] = { ...site };
    else rows.push({ ...site });
    if (site.enabled) picked.add(site.id);
  }
  render();
}

void init();
