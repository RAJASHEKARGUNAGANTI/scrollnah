"use client";
// The feedback form: one form, three kinds (feedback, feature request, bug report). Sends to /api/feedback.
import Link from "next/link";
import { useState } from "react";
import { Icon, type IconName } from "./Icon";
import { Turnstile, turnstileEnabled } from "./Turnstile";
import type { FeedbackType } from "@/lib/feedback";

type Source = "play" | "github" | "unknown";
type Initial = { type: FeedbackType; source: Source; appVersion: string; android: string; device: string };

const KINDS: { id: FeedbackType; label: string; icon: IconName; ask: string; placeholder: string }[] = [
  { id: "feedback", label: "Feedback", icon: "chat", ask: "What's on your mind?", placeholder: "I like the Pause screen, but..." },
  { id: "feature", label: "Feature request", icon: "bulb", ask: "What would you like ScrollNah to do?", placeholder: "It would be great if ScrollNah could..." },
  { id: "bug", label: "Bug report", icon: "bug", ask: "What went wrong?", placeholder: "When I open Instagram over my limit, the Pause screen..." },
];

export function FeedbackForm({ initial }: { initial: Initial }) {
  const [type, setType] = useState<FeedbackType>(initial.type);
  const [message, setMessage] = useState("");
  const [steps, setSteps] = useState("");
  const [email, setEmail] = useState("");
  const [source, setSource] = useState<Source>(initial.source);
  const [appVersion, setAppVersion] = useState(initial.appVersion);
  const [android, setAndroid] = useState(initial.android);
  const [device, setDevice] = useState(initial.device);
  const [trap, setTrap] = useState(""); // hidden from people; bots fill it in
  const [token, setToken] = useState("");
  const [resetKey, setResetKey] = useState(0);
  const [status, setStatus] = useState<"idle" | "sending" | "sent">("idle");
  const [error, setError] = useState("");

  const kind = KINDS.find((k) => k.id === type)!;
  const fromApp = Boolean(initial.appVersion || initial.android || initial.device);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (message.trim().length < 3) return setError("Please write a few words.");
    if (turnstileEnabled && !token) return setError("Please tick \"Verify you are human\" first, then press Send it.");
    setStatus("sending");
    try {
      const res = await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ type, message, steps: type === "bug" ? steps : "", email, source, appVersion, android, device, website: trap, turnstileToken: token }),
      });
      const data = (await res.json().catch(() => ({}))) as { ok?: boolean; error?: string };
      if (res.ok && data.ok) {
        setStatus("sent");
        return;
      }
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
      <div className="card sent" role="status">
        <img src="/img/blobby_thriving.png" alt="" width={180} height={180} />
        <h2>Got it. Thank you!</h2>
        <p className="muted">I read every message.{email ? ` If I have a question, I'll write to ${email}.` : ""}</p>
        <div className="ctas" style={{ justifyContent: "center", marginTop: 8 }}>
          <button className="btn lime" type="button" onClick={() => { setMessage(""); setSteps(""); setToken(""); setResetKey((k) => k + 1); setStatus("idle"); }}>
            Send another
          </button>
          <Link className="btn" href="/">Back to home</Link>
        </div>
      </div>
    );
  }

  return (
    <form className="card form" onSubmit={submit} noValidate>
      <fieldset className="tabs">
        <legend>What is it?</legend>
        {KINDS.map((k) => (
          <label className="tab" key={k.id}>
            <input type="radio" name="type" value={k.id} checked={type === k.id} onChange={() => setType(k.id)} />
            <span><Icon name={k.icon} /> {k.label}</span>
          </label>
        ))}
      </fieldset>

      <div className="field">
        <label htmlFor="message">{kind.ask}</label>
        <textarea id="message" className="input" value={message} onChange={(e) => setMessage(e.target.value)} placeholder={kind.placeholder} maxLength={5000} required />
      </div>

      {type === "bug" && (
        <div className="field">
          <label htmlFor="steps">Steps to make it happen <span className="hint">(optional)</span></label>
          <textarea id="steps" className="input" style={{ minHeight: 110 }} value={steps} onChange={(e) => setSteps(e.target.value)} placeholder={"1. Open ScrollNah\n2. ..."} maxLength={3000} />
        </div>
      )}

      <details className="more" open={type === "bug" || fromApp}>
        <summary>About your phone <span className="hint">(optional, helps with bugs)</span></summary>
        {fromApp && <p className="note" style={{ marginBottom: 12 }}>Filled in by the app. You can change or remove anything.</p>}
        <div className="field" style={{ marginBottom: 14 }}>
          <span className="label">Where did you get ScrollNah?</span>
          <div className="chips" role="radiogroup" aria-label="Where did you get ScrollNah?">
            {([["play", "Google Play"], ["github", "GitHub"], ["unknown", "Not sure"]] as const).map(([id, label]) => (
              <label className="tab" key={id}>
                <input type="radio" name="source" value={id} checked={source === id} onChange={() => setSource(id)} />
                <span>{label}</span>
              </label>
            ))}
          </div>
        </div>
        <div className="row3">
          <div className="field">
            <label htmlFor="appVersion">ScrollNah version</label>
            <input id="appVersion" className="input" value={appVersion} onChange={(e) => setAppVersion(e.target.value)} placeholder="e.g. 2.3.0" maxLength={40} />
          </div>
          <div className="field">
            <label htmlFor="android">Android version</label>
            <input id="android" className="input" value={android} onChange={(e) => setAndroid(e.target.value)} placeholder="e.g. 15" maxLength={40} />
          </div>
          <div className="field">
            <label htmlFor="device">Phone</label>
            <input id="device" className="input" value={device} onChange={(e) => setDevice(e.target.value)} placeholder="e.g. Redmi Note 13" maxLength={80} />
          </div>
        </div>
      </details>

      <div className="field">
        <label htmlFor="email">Your email <span className="hint">(optional, only if you&apos;d like a reply)</span></label>
        <input id="email" className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" maxLength={200} autoComplete="email" />
      </div>

      <div className="trap" aria-hidden="true">
        <label>Leave this empty <input tabIndex={-1} autoComplete="off" value={trap} onChange={(e) => setTrap(e.target.value)} /></label>
      </div>

      <Turnstile onToken={setToken} resetKey={resetKey} />

      {error && <p className="error" role="alert">{error}</p>}

      <div>
        <button className="btn lime" type="submit" disabled={status === "sending"}>
          {status === "sending" ? "Sending..." : <>Send it <Icon name="arrow" /></>}
        </button>
        <p className="note" style={{ marginTop: 12 }}>
          Saved safely and used only to improve ScrollNah. See <Link href="/privacy">privacy</Link>.
        </p>
      </div>
    </form>
  );
}
