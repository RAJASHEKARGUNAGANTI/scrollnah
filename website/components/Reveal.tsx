"use client";
// Sections and cards slide up softly the first time they scroll into view.
// The <head> script in app/layout.tsx turns this on (data-motion on <html>) before the page paints, unless the
// visitor's phone or computer asks for less motion. Without JavaScript nothing is hidden at all.
import { usePathname } from "next/navigation";
import { useEffect } from "react";

/** What slides in. Keep in sync with the "Motion" part of globals.css. */
const REVEAL = ".section-head, .group-title, .grid > *, .steps > *, .shots, .faq > *, .panel, .calc, .lost > *";

declare global {
  interface Window { __reveal?: boolean }
}

export function Reveal() {
  const path = usePathname();

  useEffect(() => {
    window.__reveal = true; // tells the <head> script this ran, so it doesn't switch the motion off again
    if (!document.documentElement.hasAttribute("data-motion")) return;

    // A data attribute, not a class: React rewrites className when a card changes, which would hide it again.
    let waiting = Array.from(document.querySelectorAll<HTMLElement>(REVEAL)).filter((el) => !el.hasAttribute("data-shown"));
    let frame = 0;

    // Shows everything that is in view or already scrolled past (a fast scroll can jump right over a card).
    function check() {
      frame = 0;
      const line = window.innerHeight * 0.92;
      const ready = waiting.filter((el) => el.getBoundingClientRect().top < line); // read everything first...
      waiting = waiting.filter((el) => !ready.includes(el));
      ready.forEach((el, n) => {
        // ...then change. Cards that arrive together get a small delay each, so they come in one after another.
        el.style.setProperty("--i", String(Math.min(n, 6)));
        el.setAttribute("data-shown", "");
      });
      if (waiting.length === 0) stop();
    }
    function onScroll() {
      if (!frame) frame = requestAnimationFrame(check);
    }
    function stop() {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      if (frame) cancelAnimationFrame(frame);
    }

    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    check();
    return stop;
  }, [path]);

  return null;
}
