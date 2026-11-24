// Public links used across the website. Edit them here (they are not secrets).
export const site = {
  name: "ScrollNah",
  playStore: "https://play.google.com/store/apps/details?id=app.scrollnah",
  apk: "https://github.com/RAJASHEKARGUNAGANTI/scrollnah/releases/latest",
  github: "https://github.com/RAJASHEKARGUNAGANTI/scrollnah",
  // The app's full privacy policy (the link Google Play uses).
  appPrivacy: "https://rajashekargunaganti.github.io/scrollnah/privacy-policy",
  youtube: "https://www.youtube.com/@meandmytheta",
  instagram: "https://www.instagram.com/meandmytheta",
  // Fill these in when you have them. Empty = the button shows "coming soon" (coffee) or is hidden (LinkedIn).
  coffee: "https://www.buymeacoffee.com/rajashekargunaganti",
  linkedin: "", // e.g. "https://www.linkedin.com/in/yourname"
};

// The site's own address, for link previews. Vercel fills VERCEL_PROJECT_PRODUCTION_URL in automatically.
export function siteUrl(): URL {
  const host = process.env.VERCEL_PROJECT_PRODUCTION_URL;
  return new URL(host ? `https://${host}` : "http://localhost:3000");
}
