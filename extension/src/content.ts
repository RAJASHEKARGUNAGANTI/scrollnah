// Runs inside the user's limited sites only (registered by background.ts for the sites ScrollNah has
// access to). Two jobs:
//  1. While the page is visible, it tells the background script "still here" every 15 seconds, so time
//     is counted and a limit is caught right on time.
//  2. The "hide" switches: hides YouTube Shorts shelves and the home feed, Instagram's Reels and Explore
//     buttons, and X's "For you" tab.
// It reads nothing from the page except where it is (to know if it's X's home timeline), and stores nothing.
// (A plain script, not a module: content scripts can't import, so it is self-contained.)

import type { Settings } from "./lib/settings.ts";

(() => {
  const w = window as unknown as { __scrollnah?: boolean };
  if (w.__scrollnah) return; // already running in this page
  w.__scrollnah = true;

  const g = globalThis as unknown as { browser?: Ext; chrome?: Ext };
  const api = (g.browser ?? g.chrome) as Ext;

  // ---- 1. "Still here" ----

  let timer = 0;
  let dead = false; // the extension was updated or removed: this copy can't talk to it any more

  function schedule(msLeft?: number) {
    clearTimeout(timer);
    // Every 15 s while you're on the page; when the page is visible but another window has the focus, every minute.
    const base = document.hasFocus() ? 15_000 : 60_000;
    const wait = msLeft && msLeft > 0 ? Math.min(base, msLeft + 300) : base;
    timer = window.setTimeout(beat, wait);
  }

  function beat() {
    clearTimeout(timer);
    if (dead || document.visibilityState !== "visible") return;
    try {
      api.runtime.sendMessage({ type: "beat" }).then(
        (r: { msLeft?: number } | null) => schedule(r?.msLeft),
        () => schedule(),
      );
    } catch {
      dead = true;
    }
  }

  document.addEventListener("visibilitychange", beat);
  window.addEventListener("focus", beat);
  beat();

  // ---- 2. Hiding the endless parts ----

  const CSS: Record<keyof Settings["hide"], string> = {
    youtubeShorts: `
      ytd-rich-shelf-renderer[is-shorts], ytd-reel-shelf-renderer,
      ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts]),
      ytd-rich-section-renderer:has(ytm-shorts-lockup-view-model), ytd-rich-section-renderer:has(ytm-shorts-lockup-view-model-v2),
      grid-shelf-view-model:has(ytm-shorts-lockup-view-model), grid-shelf-view-model:has(ytm-shorts-lockup-view-model-v2),
      ytd-video-renderer:has(a[href^="/shorts/"]), ytd-grid-video-renderer:has(a[href^="/shorts/"]),
      ytd-rich-item-renderer:has(a[href^="/shorts/"]), ytd-compact-video-renderer:has(a[href^="/shorts/"]),
      yt-lockup-view-model:has(a[href^="/shorts/"]),
      ytd-guide-entry-renderer:has(a[title="Shorts"]), ytd-guide-entry-renderer:has(a[href^="/shorts"]),
      ytd-mini-guide-entry-renderer[aria-label="Shorts"], ytd-mini-guide-entry-renderer:has(a[href^="/shorts"]),
      yt-tab-shape[tab-title="Shorts"], yt-chip-cloud-chip-renderer:has([title="Shorts"]),
      ytm-reel-shelf-renderer, ytm-shorts-lockup-view-model, ytm-shorts-lockup-view-model-v2,
      ytm-pivot-bar-item-renderer:has(.pivot-shorts) { display: none !important; }`,
    youtubeHome: `
      ytd-browse[page-subtype="home"] ytd-rich-grid-renderer { display: none !important; }
      ytd-browse[page-subtype="home"] #primary::before {
        content: "ScrollNah hid your home feed. Search for what you came for.";
        display: block; margin: 72px auto; max-width: 420px; text-align: center;
        font: 500 16px/1.5 Roboto, Arial, sans-serif; color: var(--yt-spec-text-secondary, #606060);
      }`,
    instagramReels: `a[href="/reels/"], a[href^="/reels/"] { display: none !important; }`,
    instagramExplore: `a[href="/explore/"], a[href^="/explore/"] { display: none !important; }`,
    xForYou: ``, // done in JavaScript below (X has no stable names for its tabs)
  };

  let hide: Partial<Settings["hide"]> = {};
  let style: HTMLStyleElement | null = null;

  function applyHide() {
    const css = (Object.keys(CSS) as (keyof Settings["hide"])[])
      .filter((k) => hide[k])
      .map((k) => CSS[k])
      .join("\n");
    if (!style) {
      style = document.createElement("style");
      style.id = "scrollnah-hide";
    }
    style.textContent = css;
    if (!style.isConnected) (document.head ?? document.documentElement).appendChild(style);
    if (hide.xForYou) watchX();
  }

  // X: on the home timeline, hide the first tab ("For you") and switch to the second ("Following").
  let xObserver: MutationObserver | null = null;
  let xQueued = false;
  function fixX() {
    xQueued = false;
    if (!hide.xForYou || location.pathname !== "/home") return;
    const tabs = document.querySelectorAll<HTMLElement>('[data-testid="primaryColumn"] [role="tablist"] [role="tab"]');
    if (tabs.length < 2) return;
    const holder = (tabs[0].closest('[role="presentation"]') as HTMLElement | null) ?? tabs[0];
    holder.style.display = "none";
    if (tabs[0].getAttribute("aria-selected") === "true") tabs[1].click();
  }
  function watchX() {
    if (xObserver || !/(^|\.)(x|twitter)\.com$/.test(location.hostname)) return;
    xObserver = new MutationObserver(() => {
      if (!xQueued) {
        xQueued = true;
        requestAnimationFrame(fixX);
      }
    });
    xObserver.observe(document.documentElement, { childList: true, subtree: true });
    fixX();
  }

  api.storage.local.get("settings").then((r) => {
    hide = (r.settings as Settings | undefined)?.hide ?? {};
    applyHide();
  });
  api.storage.onChanged.addListener((changes, area) => {
    if (area === "local" && changes.settings) {
      hide = (changes.settings.newValue as Settings | undefined)?.hide ?? {};
      applyHide();
    }
  });
})();
