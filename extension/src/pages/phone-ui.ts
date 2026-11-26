// The "Import from phone" pieces, shared by settings and setup: pick or drop the file, then show what
// was found before anything changes.

import { ImportError, parsePhoneFile, type PhoneImport } from "../lib/phone.ts";
import { FEATURES } from "../lib/sites.ts";
import { duration, formatTime } from "../lib/time.ts";
import { h, icon } from "./ui.ts";

/** A drop zone with a "Pick the file" button. Calls [onFile] with what was found, or [onError]. */
export function phoneDropZone(onFile: (imp: PhoneImport) => void, onError: (message: string) => void): HTMLElement {
  const input = h("input", { type: "file", accept: ".json,application/json", class: "sr-only", "aria-label": "The ScrollNah file from your phone" });
  const read = async (file: File | undefined) => {
    if (!file) return;
    try {
      onFile(parsePhoneFile(await file.text()));
    } catch (e) {
      onError(e instanceof ImportError ? e.message : "Couldn't read that file.");
    }
    input.value = "";
  };
  input.addEventListener("change", () => void read(input.files?.[0]));
  const zone = h(
    "div",
    { class: "drop" },
    icon("file"),
    h("p", { class: "bold" }, "Drop the file here"),
    h("button", { class: "btn outline small", type: "button", onclick: () => input.click() }, "or pick it"),
    input,
  );
  zone.addEventListener("dragover", (e) => {
    e.preventDefault();
    zone.classList.add("over");
  });
  zone.addEventListener("dragleave", () => zone.classList.remove("over"));
  zone.addEventListener("drop", (e) => {
    e.preventDefault();
    zone.classList.remove("over");
    void read(e.dataTransfer?.files?.[0]);
  });
  return zone;
}

function schedule(s: { enabled: boolean; start: number; end: number } | null): string | null {
  return s?.enabled ? `${formatTime(s.start)} to ${formatTime(s.end)}` : null;
}

/** What was found in the file, in plain words, before importing. */
export function phonePreview(imp: PhoneImport, actions: HTMLElement): HTMLElement {
  const when = imp.exportedAt ? new Date(imp.exportedAt).toLocaleDateString([], { day: "numeric", month: "short" }) : null;
  const items = imp.sites.map(({ site, appNames }) => {
    const weekend = site.weekendMinutes !== null ? ` (${duration(site.weekendMinutes)} on weekends)` : "";
    const visits = site.maxOpens !== null ? `, ${site.maxOpens} visits a day` : "";
    const off = site.enabled ? "" : " (switched off)";
    return h("li", null, icon("check"), h("span", null, h("b", null, appNames.join(" + ")), `: ${duration(site.minutes)}${weekend}${visits}${off} → ${site.id}`));
  });
  for (const name of imp.skipped) items.push(h("li", { class: "skip" }, icon("minus"), `${name}: no website, skipped`));

  const extras: string[] = [];
  if (imp.strictness) extras.push(`Strictness: ${imp.strictness[0].toUpperCase()}${imp.strictness.slice(1)}`);
  const bed = schedule(imp.bedtime);
  if (bed) extras.push(`Bedtime ${bed}`);
  const work = schedule(imp.work);
  if (work) extras.push(`Work hours ${work}`);
  for (const b of imp.budgets) extras.push(`${b.name}: ${duration(b.minutes)} together`);
  for (const f of imp.features) if (f.enabled) extras.push(`${FEATURES.find((x) => x.id === f.id)?.name}: ${duration(f.minutes)} a day`);
  if (imp.mascot) extras.push(`Mascot: ${imp.mascot === "bloomy" ? "Bloomy" : "Blobby"}`);

  return h(
    "div",
    { class: "card found" },
    h("h3", null, imp.sites.length ? `Found ${imp.sites.length} ${imp.sites.length === 1 ? "limit" : "limits"} on your phone` : "No website limits in this file"),
    h("p", { class: "small muted" }, `From ScrollNah${imp.appVersion ? ` ${imp.appVersion}` : ""}${when ? `, saved ${when}` : ""}.`),
    h("ul", null, ...items),
    extras.length ? h("p", { class: "small" }, extras.join(" · ")) : null,
    h("p", { class: "small muted" }, "Sites you only set up on this computer stay as they are. Each device counts its own time."),
    actions,
  );
}
