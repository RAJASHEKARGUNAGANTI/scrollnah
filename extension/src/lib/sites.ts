// Websites ScrollNah knows: their names, all their addresses, and the phone app each one belongs to
// (so limits from the ScrollNah app can be brought over: the app's package name -> the website).
// The app's own list is app/src/main/java/com/unscroll/app/data/Websites.kt.

export interface KnownSite {
  id: string; // the main address, also the site's id in settings
  name: string;
  hosts: string[]; // every address that counts as this site (subdomains count too)
  apps: string[]; // Android package names of the same service
  popular?: boolean; // offered first during setup
}

export const KNOWN_SITES: KnownSite[] = [
  { id: "youtube.com", name: "YouTube", hosts: ["youtube.com", "youtu.be"], apps: ["com.google.android.youtube"], popular: true },
  { id: "instagram.com", name: "Instagram", hosts: ["instagram.com"], apps: ["com.instagram.android", "com.instagram.lite"], popular: true },
  { id: "x.com", name: "X", hosts: ["x.com", "twitter.com"], apps: ["com.twitter.android"], popular: true },
  { id: "reddit.com", name: "Reddit", hosts: ["reddit.com"], apps: ["com.reddit.frontpage"], popular: true },
  { id: "facebook.com", name: "Facebook", hosts: ["facebook.com"], apps: ["com.facebook.katana", "com.facebook.lite"], popular: true },
  { id: "tiktok.com", name: "TikTok", hosts: ["tiktok.com"], apps: ["com.zhiliaoapp.musically", "com.ss.android.ugc.trill"], popular: true },
  { id: "snapchat.com", name: "Snapchat", hosts: ["snapchat.com"], apps: ["com.snapchat.android"] },
  { id: "pinterest.com", name: "Pinterest", hosts: ["pinterest.com"], apps: ["com.pinterest"] },
  { id: "linkedin.com", name: "LinkedIn", hosts: ["linkedin.com"], apps: ["com.linkedin.android"] },
  { id: "threads.com", name: "Threads", hosts: ["threads.com", "threads.net"], apps: ["com.instagram.barcelona"] },
  { id: "bsky.app", name: "Bluesky", hosts: ["bsky.app"], apps: ["xyz.blueskyweb.app"] },
  { id: "twitch.tv", name: "Twitch", hosts: ["twitch.tv"], apps: ["tv.twitch.android.app"] },
  { id: "netflix.com", name: "Netflix", hosts: ["netflix.com"], apps: ["com.netflix.mediaclient"] },
  { id: "primevideo.com", name: "Prime Video", hosts: ["primevideo.com"], apps: ["com.amazon.avod.thirdpartyclient"] },
  { id: "hotstar.com", name: "JioHotstar", hosts: ["hotstar.com", "jiohotstar.com"], apps: ["in.startv.hotstar"] },
  { id: "tumblr.com", name: "Tumblr", hosts: ["tumblr.com"], apps: ["com.tumblr"] },
  { id: "9gag.com", name: "9GAG", hosts: ["9gag.com"], apps: ["com.ninegag.android.app"] },
  { id: "quora.com", name: "Quora", hosts: ["quora.com"], apps: ["com.quora.android"] },
  { id: "discord.com", name: "Discord", hosts: ["discord.com"], apps: ["com.discord"] },
];

/** Parts of a site that are a different thing and never count (people listen to music while they work). */
const NOT_COUNTED = ["music.youtube.com", "studio.youtube.com"];

/** In-site features with their own limits, like the app's Reels & Shorts limits (same ids as the app). */
export interface SiteFeature {
  id: "youtube_shorts" | "instagram_reels";
  name: string;
  site: string;
  /** True if [path] (e.g. "/shorts/abc") is this feature. */
  test(path: string): boolean;
}

export const FEATURES: SiteFeature[] = [
  { id: "youtube_shorts", name: "YouTube Shorts", site: "youtube.com", test: (p) => p === "/shorts" || p.startsWith("/shorts/") },
  {
    id: "instagram_reels",
    name: "Instagram Reels",
    site: "instagram.com",
    test: (p) => p === "/reels" || p.startsWith("/reels/") || p.startsWith("/reel/"),
  },
];

export function knownSite(id: string): KnownSite | undefined {
  return KNOWN_SITES.find((s) => s.id === id);
}

/** The website for an Android app, e.g. "com.google.android.youtube" -> YouTube. */
export function siteForApp(packageName: string): KnownSite | undefined {
  return KNOWN_SITES.find((s) => s.apps.includes(packageName));
}

/** "https://www.youtube.com/watch?v=1" -> "www.youtube.com". Only web pages; anything else is null. */
export function hostOf(url: string | undefined | null): string | null {
  if (!url) return null;
  try {
    const u = new URL(url);
    if (u.protocol !== "https:" && u.protocol !== "http:") return null;
    return u.hostname.toLowerCase().replace(/\.$/, "");
  } catch {
    return null;
  }
}

/** True if [host] is [site] or one of its subdomains ("m.youtube.com" is "youtube.com"). */
export function hostIs(host: string, site: string): boolean {
  return host === site || host.endsWith("." + site);
}

/**
 * Turns what someone typed into a site address: "https://www.YouTube.com/feed" -> "youtube.com".
 * Returns null for something that isn't an address.
 */
export function cleanSiteInput(text: string): string | null {
  let t = text.trim().toLowerCase();
  if (!t || /\s/.test(t)) return null;
  t = t.replace(/^[a-z][a-z0-9+.-]*:\/\//, ""); // https://
  t = t.split(/[/?#]/)[0]; // the path
  t = t.replace(/:\d+$/, "").replace(/\.$/, ""); // a port, a trailing dot
  t = t.replace(/^(www\d?|m|mobile)\./, "");
  if (t.length > 253 || !t.includes(".")) return null;
  const labels = t.split(".");
  if (labels.some((l) => !/^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$/.test(l))) return null;
  if (/^\d+$/.test(labels[labels.length - 1])) return null; // an IP address
  return t;
}

/** A site in the user's list (only the parts matching needs). */
export interface SiteRef {
  id: string;
  hosts: string[];
}

/**
 * Which of the user's sites a page belongs to, or null. The most specific address wins, so
 * "old.reddit.com" can have its own limit next to "reddit.com".
 */
export function siteOfUrl(url: string | undefined | null, sites: SiteRef[]): string | null {
  const host = hostOf(url);
  if (!host || NOT_COUNTED.some((h) => hostIs(host, h))) return null;
  let best: { id: string; len: number } | null = null;
  for (const site of sites) {
    for (const h of site.hosts) {
      if (hostIs(host, h) && (!best || h.length > best.len)) best = { id: site.id, len: h.length };
    }
  }
  return best?.id ?? null;
}

/** The feature (YouTube Shorts, Instagram Reels) a page is, or null. */
export function featureOfUrl(url: string | undefined | null): SiteFeature | null {
  const host = hostOf(url);
  if (!host || !url) return null;
  let path: string;
  try {
    path = new URL(url).pathname.toLowerCase();
  } catch {
    return null;
  }
  return (
    FEATURES.find((f) => {
      const site = knownSite(f.site);
      return !!site && site.hosts.some((h) => hostIs(host, h)) && f.test(path);
    }) ?? null
  );
}

/** Browser match patterns for a site, e.g. "*://youtube.com/*" and "*://*.youtube.com/*". */
export function patternsFor(hosts: string[]): string[] {
  return hosts.flatMap((h) => [`*://${h}/*`, `*://*.${h}/*`]);
}

/** The default name for a site someone added: a known name, or "example.com" -> "Example". */
export function nameForSite(id: string): string {
  const known = knownSite(id);
  if (known) return known.name;
  const main = id.split(".").slice(-2)[0] ?? id;
  return main.charAt(0).toUpperCase() + main.slice(1);
}

/** All addresses for a site id: the known ones, or just the id itself. */
export function hostsForSite(id: string): string[] {
  return knownSite(id)?.hosts ?? [id];
}
