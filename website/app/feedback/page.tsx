import type { Metadata } from "next";
import { FeedbackForm } from "@/components/FeedbackForm";
import type { FeedbackType } from "@/lib/feedback";

export const metadata: Metadata = {
  title: "Feedback",
  description: "Send feedback, request a feature or report a bug in ScrollNah. No account needed.",
};

// This page has no Buy Me a Coffee link on purpose: the Google Play version of the app links here.
type Params = Record<string, string | string[] | undefined>;
const one = (v: string | string[] | undefined, max = 80) => (typeof v === "string" ? v.slice(0, max) : "");

export default async function FeedbackPage({ searchParams }: { searchParams: Promise<Params> }) {
  // The app can open this page with details filled in, e.g. /feedback?type=bug&v=2.5.6&android=15&device=Pixel%208&src=play
  const p = await searchParams;
  const type = (["feedback", "feature", "bug"] as const).find((t) => t === p.type) ?? "feedback";
  const src = p.src === "play" || p.src === "github" ? p.src : "unknown";

  return (
    <>
      <section className="page-hero" style={{ paddingBottom: 28 }}>
        <div className="wrap">
          <span className="eyebrow">Feedback</span>
          <h1 style={{ fontSize: "clamp(38px, 6vw, 60px)" }}>Tell me what you think</h1>
          <p className="muted">Feedback, a feature idea or a bug: it all lands with me, the one person who makes ScrollNah. No account needed.</p>
        </div>
      </section>
      <div className="wrap form-layout">
        <FeedbackForm
          initial={{
            type: type as FeedbackType,
            source: src,
            appVersion: one(p.v, 40),
            android: one(p.android, 40),
            device: one(p.device, 80),
          }}
        />
        <aside className="side">
          <div className="card">
            <h3>For a quick bug fix</h3>
            <ul>
              <li>What you did, and what happened</li>
              <li>What you expected instead</li>
              <li>Your phone and Android version</li>
              <li>Your ScrollNah version: Settings, then About</li>
            </ul>
          </div>
          <div className="card">
            <h3>Want a reply?</h3>
            <p className="muted" style={{ marginTop: 8 }}>Add your email. It&apos;s used only to answer you, never for anything else.</p>
          </div>
        </aside>
      </div>
    </>
  );
}
