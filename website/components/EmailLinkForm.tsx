"use client";
// An email box that sends you a link (the newsletter sign-up, the idea-vote link).
// The spam check appears once someone starts typing.
import { useState, type ReactNode } from "react";
import { Turnstile, turnstileEnabled } from "./Turnstile";

type Props = {
  /** The API route that gets {email, website, turnstileToken}. */
  endpoint: string;
  button: string;
  /** What shows after sending: a title, and a line that can use the address. */
  sentTitle: string;
  sentText: (email: string) => ReactNode;
  /** Small print under the box. */
  children?: ReactNode;
  /** Extra fields sent along (e.g. which idea the vote link is for). */
  extra?: Record<string, string>;
};

export function EmailLinkForm({ endpoint, button, sentTitle, sentText, children, extra }: Props) {
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
      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...extra, email, website: trap, turnstileToken: token }),
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
        <p style={{ fontWeight: 700, fontSize: 20 }}>{sentTitle}</p>
        <p className="note" style={{ marginTop: 6 }}>{sentText(email.trim())}</p>
      </div>
    );
  }

  return (
    <form onSubmit={submit} noValidate style={{ display: "flex", flexDirection: "column", alignItems: "stretch", gap: 12 }}>
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
          {status === "sending" ? "Sending..." : button}
        </button>
      </div>
      <div className="trap" aria-hidden="true">
        <label>Leave this empty <input tabIndex={-1} autoComplete="off" value={trap} onChange={(e) => setTrap(e.target.value)} /></label>
      </div>
      {engaged && <Turnstile onToken={setToken} resetKey={resetKey} />}
      {error && <p className="error" role="alert">{error}</p>}
      {children}
    </form>
  );
}
