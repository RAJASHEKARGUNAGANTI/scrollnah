"use client";
// Cloudflare Turnstile: a privacy-friendly "are you human?" check. Hidden when no site key is set (while testing).
import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";

type TurnstileApi = {
  render: (el: HTMLElement, opts: Record<string, unknown>) => string;
  reset: (id: string) => void;
  remove: (id: string) => void;
};
declare global {
  interface Window { turnstile?: TurnstileApi }
}

const SITE_KEY = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY || "";
export const turnstileEnabled = SITE_KEY !== "";

/** Shows the check and reports its token. Change `resetKey` to get a fresh check (a token works once). */
export function Turnstile({ onToken, resetKey }: { onToken: (token: string) => void; resetKey: number }) {
  const box = useRef<HTMLDivElement>(null);
  const widget = useRef<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const report = useRef(onToken);
  useEffect(() => { report.current = onToken; }); // call the latest function without redrawing the check

  const draw = useCallback(() => {
    if (!box.current || !window.turnstile) return;
    if (widget.current) window.turnstile.remove(widget.current);
    widget.current = window.turnstile.render(box.current, {
      sitekey: SITE_KEY,
      theme: "auto",
      callback: (t: string) => report.current(t),
      "expired-callback": () => report.current(""),
      "error-callback": () => report.current(""),
    });
  }, []);

  useEffect(() => {
    if (loaded || window.turnstile) draw();
  }, [loaded, resetKey, draw]);

  if (!turnstileEnabled) return null;
  return (
    <>
      <Script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" strategy="afterInteractive" onLoad={() => setLoaded(true)} />
      <div ref={box} />
    </>
  );
}
