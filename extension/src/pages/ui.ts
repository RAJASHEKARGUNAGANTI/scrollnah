// Small helpers to build the pages without a framework (keeps the extension tiny).

import { drawMascot, type MascotDrawing } from "../lib/art.ts";

type Child = Node | string | number | null | undefined | false;
type Props = {
  class?: string;
  text?: string;
  style?: string;
  [key: string]: unknown; // attributes ("aria-label", "type"...) and events ("onclick"...)
};

/** h("button", { class: "btn", onclick: go }, "Save") */
export function h<K extends keyof HTMLElementTagNameMap>(tag: K, props?: Props | null, ...children: Child[]): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(props ?? {})) {
    if (value === undefined || value === null || value === false) continue;
    if (key === "class") el.className = String(value);
    else if (key === "text") el.textContent = String(value);
    else if (key.startsWith("on") && typeof value === "function") el.addEventListener(key.slice(2), value as EventListener);
    else if (key === "value" && "value" in el) (el as HTMLInputElement).value = String(value);
    else if (key === "checked" && "checked" in el) (el as HTMLInputElement).checked = !!value;
    else el.setAttribute(key, value === true ? "" : String(value));
  }
  append(el, ...children);
  return el;
}

export function append(el: Element, ...children: Child[]) {
  for (const c of children) {
    if (c === null || c === undefined || c === false) continue;
    el.append(c instanceof Node ? c : String(c));
  }
}

export function $(selector: string): HTMLElement {
  const el = document.querySelector<HTMLElement>(selector);
  if (!el) throw new Error(`Missing ${selector}`);
  return el;
}

/** The mascot on a canvas, sharp on high-resolution screens. */
export function mascotCanvas(size: number, drawing: MascotDrawing, className = "mascot"): HTMLCanvasElement {
  const canvas = h("canvas", { class: className, role: "img", "aria-label": drawing.mascot === "bloomy" ? "Bloomy" : "Blobby" });
  paintMascot(canvas, size, drawing);
  return canvas;
}

export function paintMascot(canvas: HTMLCanvasElement, size: number, drawing: MascotDrawing) {
  const ratio = Math.max(1, Math.min(3, window.devicePixelRatio || 1));
  canvas.width = Math.round(size * ratio);
  canvas.height = Math.round(size * ratio);
  canvas.style.width = `${size}px`;
  canvas.style.height = `${size}px`;
  const ctx = canvas.getContext("2d")!;
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  ctx.clearRect(0, 0, size, size);
  drawMascot(ctx, size, drawing);
}

/** A blink every few seconds (redraws the canvas). Stops with "reduce motion". Returns a stop function. */
export function blink(canvas: HTMLCanvasElement, size: number, drawing: MascotDrawing): () => void {
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) return () => {};
  let timer = 0;
  const loop = () => {
    timer = window.setTimeout(() => {
      paintMascot(canvas, size, { ...drawing, pose: { ...drawing.pose, eyeOpen: 0.1 } });
      window.setTimeout(() => paintMascot(canvas, size, drawing), 140);
      loop();
    }, 2800 + Math.random() * 2600);
  };
  loop();
  return () => clearTimeout(timer);
}

// ---- Stroke icons (2px lines, like the app) ----

const ICONS: Record<string, string> = {
  plus: "M12 5v14M5 12h14",
  minus: "M5 12h14",
  close: "M6 6l12 12M18 6L6 18",
  check: "M5 12.5l4.5 4.5L19 7",
  gear: "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
  phone: "M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zM11 18h2",
  arrow: "M5 12h14M13 6l6 6-6 6",
  back: "M19 12H5M11 18l-6-6 6-6",
  down: "M6 9l6 6 6-6",
  lock: "M7 11V8a5 5 0 0 1 10 0v3M5 11h14v10H5z",
  moon: "M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z",
  eye: "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
  file: "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5",
  trash: "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3",
};

export function icon(name: keyof typeof ICONS | string, label?: string): SVGSVGElement {
  const ns = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(ns, "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("class", "icon");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "2");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  if (label) {
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", label);
  } else svg.setAttribute("aria-hidden", "true");
  const path = document.createElementNS(ns, "path");
  path.setAttribute("d", ICONS[name] ?? "");
  svg.append(path);
  return svg;
}

// ---- Controls ----

/** [-] 30 min [+] */
export function stepper(opts: {
  value: number;
  min: number;
  max: number;
  step: number;
  label: string;
  format: (v: number) => string;
  onChange: (v: number) => void;
}): HTMLElement {
  let value = opts.value;
  const out = h("output", { "aria-live": "polite" }, opts.format(value));
  const minus = h("button", { class: "icon-btn", type: "button", "aria-label": `Less ${opts.label}` }, icon("minus"));
  const plus = h("button", { class: "icon-btn", type: "button", "aria-label": `More ${opts.label}` }, icon("plus"));
  const set = (v: number) => {
    value = Math.min(opts.max, Math.max(opts.min, v));
    out.textContent = opts.format(value);
    minus.disabled = value <= opts.min;
    plus.disabled = value >= opts.max;
  };
  minus.addEventListener("click", () => {
    set(value - opts.step);
    opts.onChange(value);
  });
  plus.addEventListener("click", () => {
    set(value + opts.step);
    opts.onChange(value);
  });
  set(value);
  return h("div", { class: "stepper", role: "group", "aria-label": opts.label }, minus, out, plus);
}

/** An on/off switch with a hidden label for screen readers. */
export function toggle(checked: boolean, label: string, onChange: (on: boolean) => void): HTMLElement {
  const input = h("input", { type: "checkbox", role: "switch", "aria-label": label, checked });
  input.addEventListener("change", () => onChange(input.checked));
  return h("label", { class: "switch" }, input, h("span"));
}

export function tile(name: string): HTMLElement {
  return h("span", { class: "tile", "aria-hidden": "true" }, (name.trim()[0] ?? "?").toUpperCase());
}

export function bar(used: number, limit: number): HTMLElement {
  const share = limit > 0 ? Math.min(1, used / limit) : 0;
  return h("div", { class: `bar${share >= 0.8 ? " warn" : ""}`, role: "progressbar", "aria-valuemin": "0", "aria-valuemax": String(limit), "aria-valuenow": String(Math.min(used, limit)) }, h("span", { style: `width:${Math.round(share * 100)}%` }));
}

let toastTimer = 0;
export function toast(message: string, ms = 2600) {
  document.querySelector(".toast")?.remove();
  const el = h("div", { class: "toast", role: "status" }, message);
  document.body.append(el);
  clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => el.remove(), ms);
}
