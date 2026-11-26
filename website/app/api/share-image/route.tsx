// GET /api/share-image?mins=180 : the year calculator's share picture (1080x1350 PNG), in ScrollNah's style.
// Made with next/og (built into Next.js). Only the minutes are used; nothing about the visitor.
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { ImageResponse } from "next/og";

const INK = "#16131f", LIME = "#c6f432", LILAC = "#f2eeff", MUTED = "#4b4660";

function mascotFor(mins: number) {
  return mins <= 60 ? "blobby_thriving" : mins <= 120 ? "blobby_happy" : mins <= 240 ? "blobby_worried" : "blobby_melting";
}

function hm(m: number) {
  const h = Math.floor(m / 60), r = m % 60;
  return [h ? `${h} h` : "", r ? `${r} min` : ""].filter(Boolean).join(" ");
}

export async function GET(req: Request) {
  // Same steps as the slider: 15 to 480 minutes, in 15s.
  const raw = Number(new URL(req.url).searchParams.get("mins"));
  const mins = Number.isFinite(raw) ? Math.min(480, Math.max(15, Math.round(raw / 15) * 15)) : 180;
  const days = Math.round((mins * 365) / 60 / 24);
  const movies = Math.round((mins * 365) / 120);

  const root = process.cwd();
  // Geist (free font licence, from next/og) for the small line: the picture maker can't read the trimmed DM Sans file.
  const [display, body, mascot] = await Promise.all([
    readFile(join(root, "app/fonts/bricolage_grotesque.ttf")),
    readFile(join(root, "app/fonts/geist_regular.ttf")),
    readFile(join(root, `public/img/${mascotFor(mins)}.png`)),
  ]);
  const mascotSrc = `data:image/png;base64,${mascot.toString("base64")}`;

  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", flexDirection: "column", background: LILAC, padding: 72, fontFamily: "Bricolage", color: INK }}>
        {/* Wordmark */}
        <div style={{ display: "flex", alignItems: "center", fontFamily: "Bricolage", fontSize: 64, letterSpacing: -2 }}>
          <span>Scroll</span>
          <span style={{ background: LIME, border: `5px solid ${INK}`, borderRadius: 20, padding: "0 14px 6px", marginLeft: 6, transform: "rotate(-2deg)" }}>Nah</span>
        </div>

        {/* The card */}
        <div style={{ marginTop: 56, display: "flex", flexDirection: "column", background: "#ffffff", border: `6px solid ${INK}`, borderRadius: 48, boxShadow: `16px 16px 0 ${INK}`, padding: "56px 60px", position: "relative", height: 900 }}>
          <div style={{ fontSize: 50 }}>I scroll about</div>
          <div style={{ display: "flex", marginTop: 12 }}>
            <span style={{ fontFamily: "Bricolage", fontSize: days >= 100 ? 176 : 220, lineHeight: 1, letterSpacing: -8, whiteSpace: "nowrap", background: LIME, padding: "0 24px 18px", borderRadius: 28, border: `6px solid ${INK}` }}>
              {`${days} days`}
            </span>
          </div>
          <div style={{ fontFamily: "Bricolage", fontSize: 96, marginTop: 18, letterSpacing: -3 }}>a year.</div>
          <div style={{ fontFamily: "Geist", fontSize: 36, color: MUTED, marginTop: 26, maxWidth: 430, lineHeight: 1.35 }}>
            {`That's ${hm(mins)} a day, or about ${movies.toLocaleString("en-US")} movies every year.`}
          </div>
          <img src={mascotSrc} width={340} height={340} style={{ position: "absolute", right: 30, bottom: 28 }} />
        </div>

        {/* Footer */}
        <div style={{ marginTop: "auto", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ fontSize: 36 }}>How much of your year is scrolling?</div>
          <div style={{ background: INK, color: LIME, fontSize: 32, borderRadius: 999, padding: "12px 28px 16px" }}>scrollnah.vercel.app</div>
        </div>
      </div>
    ),
    {
      width: 1080,
      height: 1350,
      fonts: [
        { name: "Bricolage", data: display, weight: 800, style: "normal" },
        { name: "Geist", data: body, weight: 400, style: "normal" },
      ],
      // The picture only depends on the minutes, so browsers and Vercel can keep it.
      headers: { "Cache-Control": "public, max-age=86400, immutable" },
    },
  );
}
