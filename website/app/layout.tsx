import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { Reveal } from "@/components/Reveal";
import { siteUrl } from "@/lib/site";
import "./globals.css";

// The app's own fonts, served from this site (no Google Fonts request).
const display = localFont({ src: "./fonts/bricolage_grotesque.ttf", weight: "800", variable: "--font-display", display: "swap" });
const body = localFont({ src: "./fonts/dm_sans.ttf", weight: "400 700", variable: "--font-body", display: "swap" });

const description =
  "ScrollNah is a tiny, free Android app that helps you scroll less. Set a daily budget for the apps that eat your day, and Blobby or Bloomy reacts to every minute. No account, no ads, everything stays on your phone.";

export const metadata: Metadata = {
  metadataBase: siteUrl(),
  title: { default: "ScrollNah: scroll less, with a little buddy", template: "%s · ScrollNah" },
  description,
  openGraph: { title: "ScrollNah: scroll less, with a little buddy", description, images: ["/img/og.png"], type: "website" },
  twitter: { card: "summary_large_image", images: ["/img/og.png"] },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f2eeff" },
    { media: "(prefers-color-scheme: dark)", color: "#16131f" },
  ],
};

// Runs before the page paints, so a saved light/dark choice doesn't flash.
const themeScript = `try{var t=localStorage.getItem("theme");if(t==="light"||t==="dark")document.documentElement.setAttribute("data-theme",t)}catch(e){}`;
// Turns on the scroll-in animations (components/Reveal.tsx) before the page paints, unless the visitor asked
// for less motion. If the page's JavaScript hasn't started after 4 s, it switches them off so nothing stays hidden.
const motionScript = `try{if(!matchMedia("(prefers-reduced-motion: reduce)").matches){var d=document.documentElement;d.setAttribute("data-motion","");setTimeout(function(){if(!window.__reveal)d.removeAttribute("data-motion")},4000)}}catch(e){}`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${display.variable} ${body.variable}`} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript + motionScript }} />
      </head>
      <body>
        <a className="skip" href="#main">Skip to content</a>
        <Header />
        <main id="main">{children}</main>
        <Footer />
        <Reveal />
      </body>
    </html>
  );
}
