"use client";
// "How much of your year is scrolling?": minutes a day turned into days a year (like the app's setup step).
import { useState } from "react";

function hm(m: number) {
  const h = Math.floor(m / 60), r = m % 60;
  return [h ? `${h} h` : "", r ? `${r} min` : ""].filter(Boolean).join(" ");
}

export function YearCalculator() {
  const [mins, setMins] = useState(180);
  const days = Math.round((mins * 365) / 60 / 24);
  const movies = Math.round((mins * 365) / 120);
  const face = mins <= 60 ? "blobby_thriving" : mins <= 120 ? "blobby_happy" : mins <= 240 ? "blobby_worried" : "blobby_melting";

  return (
    <div className="card calc">
      <div>
        <h2>How much of your year is scrolling?</h2>
        <label htmlFor="mins">On a normal day, I scroll for <span className="mins">{hm(mins)}</span></label>
        <input id="mins" type="range" min={15} max={480} step={15} value={mins} onChange={(e) => setMins(+e.target.value)} />
        <output htmlFor="mins">About {days} {days === 1 ? "day" : "days"} a year</output>
        <p className="muted">That&apos;s about {movies.toLocaleString("en-US")} movies, every single year. ScrollNah can help you win some of it back.</p>
      </div>
      <img src={`/img/${face}.png`} alt="" width={240} height={240} />
    </div>
  );
}
