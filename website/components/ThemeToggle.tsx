"use client";
// Light/dark button. Remembers the choice in this browser only.
import { Icon } from "./Icon";

export function ThemeToggle() {
  function toggle() {
    const root = document.documentElement;
    const set = root.getAttribute("data-theme");
    const dark = set === "dark" || (!set && window.matchMedia("(prefers-color-scheme: dark)").matches);
    const next = dark ? "light" : "dark";
    root.setAttribute("data-theme", next);
    try {
      localStorage.setItem("theme", next);
    } catch {
      // private mode: the choice just isn't remembered
    }
  }
  return (
    <button type="button" className="round-btn theme-btn" onClick={toggle} aria-label="Switch light or dark">
      <span className="sun"><Icon name="sun" /></span>
      <span className="moon"><Icon name="moon" /></span>
    </button>
  );
}
