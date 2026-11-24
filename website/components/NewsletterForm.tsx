"use client";
// The newsletter box on the home page. The spam check appears once someone starts typing.
import Link from "next/link";
import { useState } from "react";
import { Turnstile, turnstileEnabled } from "./Turnstile";

export function NewsletterForm() {
  const [email, setEmail] = useState("");
  const [trap, setTrap] = useState(""); // hidden from people; bots fill it in
  const [engaged, setEngaged] = useState(false);
  const [token, setToken] = useState("");
  const [resetKey, setResetKey] = useState(0);
  const [status, setStatus] = useState<"idle" | "sending" | "sent">("idle");
  const [error, setError] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) return setError("Please type your email address.");
    if (turnstileEnabled && !token) return setError("Please tick \"Verify you are human\" first.");
    setStatus("sending");
    try {
      const res = await fetch("/api/subscribe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, website: trap, turnstileToken: token }),
      });
      const data = (await res.json().catch(() => ({}))) as { ok?: boolean; error?: string };
      if (res.ok && data.ok) return setStatus("sent");
      setError(data.error || "Something went wrong. Please try again.");
    } catch {
      setError("Couldn't reach the server. Check your internet and try again.");
    }
    setStatus("idle");
    setToken("");
    setResetKey((k) => k + 1); // a spam-check token works only once
  }

  if (status === "sent") {
    return (
      <div role="status">
        <p style={{ fontWeight: 700, fontSize: 20 }}>Almost there! Check your inbox.</p>
        <p className="note" style={{ marginTop: 6 }}>
          I sent a confirmation email to {email.trim()}. Open it and press &quot;Yes, sign me up&quot;. Not there? Look in Spam or Promotions.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={submit} noValidate style={{ flexDirection: "column", alignItems: "stretch" }}>
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
        <input
          className="input"
          type="email"
          value={email}
          onChange={(e) => { setEmail(e.target.value); setEngaged(true); }}
          onFocus={() => setEngaged(true)}
          placeholder="you@example.com"
          aria-label="Your email"
          autoComplete="email"
          maxLength={200}
          style={{ flex: "1 1 220px" }}
        />
        <button className="btn lime" type="submit" disabled={status === "sending"}>
          {status === "sending" ? "Sending..." : "Sign me up"}
        </button>
      </div>
      <div className="trap" aria-hidden="true">
        <label>Leave this empty <input tabIndex={-1} autoComplete="off" value={trap} onChange={(e) => setTrap(e.target.value)} /></label>
      </div>
      {engaged && <Turnstile onToken={setToken} resetKey={resetKey} />}
      {error && <p className="error" role="alert">{error}</p>}
      <p className="note">
        Only ScrollNah news. No tracking in the emails, and every one has an unsubscribe link. See <Link href="/privacy">privacy</Link>.
      </p>
    </form>
  );
}
