"use client";
// "How much of your year is scrolling?": minutes a day turned into days a year (like the app's setup step),
// and a picture of the result to share.
import { useState } from "react";
import { Icon } from "./Icon";

function hm(m: number) {
  const h = Math.floor(m / 60), r = m % 60;
  return [h ? `${h} h` : "", r ? `${r} min` : ""].filter(Boolean).join(" ");
}

export function YearCalculator() {
  const [mins, setMins] = useState(180);
  const [sharing, setSharing] = useState(false);
  const [note, setNote] = useState("");
  const days = Math.round((mins * 365) / 60 / 24);
  const movies = Math.round((mins * 365) / 120);
  const face = mins <= 60 ? "blobby_thriving" : mins <= 120 ? "blobby_happy" : mins <= 240 ? "blobby_worried" : "blobby_melting";

  /** Phones: Android's share menu with the picture. Computers: the picture is downloaded. */
  async function share() {
    setSharing(true);
    setNote("");
    try {
      const res = await fetch(`/api/share-image?mins=${mins}`);
      if (!res.ok) throw new Error("image");
      const blob = await res.blob();
      const file = new File([blob], `scrollnah-${days}-days-a-year.png`, { type: "image/png" });
      const text = `I scroll about ${days} days a year. How about you? https://scrollnah.vercel.app`;
      if (navigator.canShare?.({ files: [file] })) {
        await navigator.share({ files: [file], text });
      } else {
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = file.name;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 2000);
        setNote("Saved the picture to your downloads.");
      }
    } catch (err) {
      // Closing the share menu without sharing isn't an error.
      if (!(err instanceof DOMException && err.name === "AbortError")) setNote("Couldn't make the picture. Please try again.");
    }
    setSharing(false);
  }

  return (
    <div className="card calc">
      <div>
        <h2>How much of your year is scrolling?</h2>
        <label htmlFor="mins">On a normal day, I scroll for <span className="mins">{hm(mins)}</span></label>
        <input id="mins" type="range" min={15} max={480} step={15} value={mins} onChange={(e) => setMins(+e.target.value)} />
        <output htmlFor="mins" key={days}>About {days} {days === 1 ? "day" : "days"} a year</output>
        <p className="muted">That&apos;s about {movies.toLocaleString("en-US")} movies, every single year. ScrollNah can help you win some of it back.</p>
        <div className="share-row">
          <button className="btn" type="button" onClick={share} disabled={sharing}>
            <Icon name="share" /> {sharing ? "Making the picture..." : "Share my number"}
          </button>
          {note && <span className="muted" role="status">{note}</span>}
        </div>
      </div>
      <img key={face} src={`/img/${face}.png`} alt="" width={240} height={240} />
    </div>
  );
}
