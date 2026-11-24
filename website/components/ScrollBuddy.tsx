"use client";
// Blobby in the corner. Its mood follows how far down the page you are: this page has a "scroll limit" too.
import { useEffect, useRef, useState } from "react";

const MOODS = [
  { at: 0, img: "blobby_thriving", mood: "thriving", line: "Hi! I'm Blobby. Fun fact: this page has a scroll limit too." },
  { at: 0.3, img: "blobby_happy", mood: "happy", line: "Still fine. A little scrolling is okay." },
  { at: 0.62, img: "blobby_worried", mood: "worried", line: "That's a lot of scrolling. I'm starting to sweat." },
  { at: 0.93, img: "blobby_melting", mood: "melting", line: "You scrolled the whole page. I'm melting. Go do something nice?" },
];

export function ScrollBuddy() {
  const [level, setLevel] = useState(0);
  const [talking, setTalking] = useState(false);
  const quiet = useRef(false); // the visitor closed the bubble
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  function say(ms = 4200) {
    if (quiet.current) return;
    setTalking(true);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setTalking(false), ms);
  }

  useEffect(() => {
    let last = 0;
    function onScroll() {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const p = max > 0 ? window.scrollY / max : 0;
      let i = 0;
      MOODS.forEach((m, k) => { if (p >= m.at) i = k; });
      if (i !== last) {
        last = i;
        setLevel(i);
        if (i > 0) say();
      }
    }
    window.addEventListener("scroll", onScroll, { passive: true });
    const hello = setTimeout(() => say(), 1200);
    return () => {
      window.removeEventListener("scroll", onScroll);
      clearTimeout(hello);
      clearTimeout(timer.current);
    };
  }, []);

  const m = MOODS[level];
  return (
    <div className={`buddy${level === MOODS.length - 1 ? " over" : ""}`}>
      <div className={`say${talking ? " show" : ""}`} role="status" aria-live="polite">
        <b>Blobby · {m.mood}</b>
        <span>{m.line}</span>
      </div>
      <button
        type="button"
        aria-label="Blobby, the ScrollNah mascot. Tap to hear what Blobby thinks."
        onClick={() => {
          if (talking) { quiet.current = true; setTalking(false); }
          else { quiet.current = false; say(6000); }
        }}
      >
        <img src={`/img/${m.img}.png`} alt="" width={72} height={72} />
      </button>
    </div>
  );
}
