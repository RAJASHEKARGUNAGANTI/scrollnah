// Pages that the "hide" switches turn off completely: YouTube Shorts, Instagram's Reels feed and
// Explore. Going to one shows ScrollNah's "I hid this for you" page instead. (Shelves and buttons that
// lead there are hidden inside the page by content.ts.)

import type { Settings } from "./settings.ts";
import { hostIs, hostOf } from "./sites.ts";

export interface HiddenPage {
  what: string; // "YouTube Shorts"
  site: string; // "youtube.com"
  siteName: string;
  home: string; // where "Back to YouTube" goes
  watch?: string; // a Short as a normal video, e.g. a link a friend sent
}

export function hiddenPageFor(url: string | undefined, hide: Settings["hide"]): HiddenPage | null {
  const host = hostOf(url);
  if (!host || !url) return null;
  let u: URL;
  try {
    u = new URL(url);
  } catch {
    return null;
  }
  const path = u.pathname.toLowerCase();
  if (hide.youtubeShorts && hostIs(host, "youtube.com") && (path === "/shorts" || path.startsWith("/shorts/"))) {
    const id = u.pathname.split("/")[2] ?? "";
    return {
      what: "YouTube Shorts",
      site: "youtube.com",
      siteName: "YouTube",
      home: "https://www.youtube.com/",
      watch: /^[A-Za-z0-9_-]{6,20}$/.test(id) ? `https://www.youtube.com/watch?v=${id}` : undefined,
    };
  }
  if (hostIs(host, "instagram.com")) {
    const home = "https://www.instagram.com/";
    if (hide.instagramReels && (path === "/reels" || path.startsWith("/reels/"))) return { what: "Instagram Reels", site: "instagram.com", siteName: "Instagram", home };
    if (hide.instagramExplore && (path === "/explore" || path.startsWith("/explore/"))) return { what: "Instagram Explore", site: "instagram.com", siteName: "Instagram", home };
  }
  return null;
}
