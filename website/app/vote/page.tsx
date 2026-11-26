import type { Metadata } from "next";
import Link from "next/link";
import { VoteBoard } from "@/components/VoteBoard";
import { getDb } from "@/lib/db";
import { IDEAS, IDEA_IDS, MAX_PER_IDEA, VOTE_BUDGET, type Picks } from "@/lib/ideas";
import { picksOf, voteTotals, voterForLink } from "@/lib/votes";

export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Vote for what's next", robots: { index: false } };

type Loaded = { kind: "ok"; picks: Picks; counts: Record<string, number> } | { kind: "expired" } | { kind: "down" };

async function load(token: string): Promise<Loaded> {
  const dbPromise = getDb();
  if (!dbPromise) return { kind: "down" };
  try {
    const db = await dbPromise;
    const voter = await voterForLink(db, token);
    if (!voter) return { kind: "expired" };
    const [picks, totals] = await Promise.all([picksOf(db, voter), voteTotals(db)]);
    return { kind: "ok", picks, counts: totals.counts };
  } catch (err) {
    console.error("vote page failed", err);
    return { kind: "down" };
  }
}

// The link in the vote email lands here. Opening it doesn't vote: only "Save my votes" does, so email apps
// that open links by themselves (to scan them) can't vote for anyone.
export default async function VotePage({ searchParams }: { searchParams: Promise<{ t?: string; idea?: string }> }) {
  const { t = "", idea = "" } = await searchParams;
  const state = await load(t);

  if (state.kind !== "ok") {
    return (
      <div className="wrap lost">
        <img src="/img/blobby_worried.png" alt="" width={200} height={200} />
        <h1 style={{ fontSize: "clamp(34px, 6vw, 52px)" }}>{state.kind === "expired" ? "This link has run out" : "Voting is resting"}</h1>
        <p className="muted">
          {state.kind === "expired"
            ? "Vote links work for 7 days. Ask for a new one on the home page; your earlier votes are still there."
            : "Something went wrong on our side. Please try again in a little while."}
        </p>
        <Link className="btn lime" href="/#ideas">Get a new vote link</Link>
      </div>
    );
  }

  // First visit from an idea's Vote button: one vote is already placed on that idea (saved only with
  // "Save my votes"). Someone who voted before sees their saved votes instead.
  const clicked = Object.keys(state.picks).length === 0 && IDEA_IDS.has(idea) ? IDEAS.find((i) => i.id === idea) : undefined;

  return (
    <section style={{ paddingTop: 48 }}>
      <div className="wrap">
        <div className="section-head">
          <span className="eyebrow">Idea votes</span>
          <h1 style={{ fontSize: "clamp(36px, 6vw, 56px)" }}>What should ScrollNah get next?</h1>
          <p className="muted">
            You have {VOTE_BUDGET} votes. Put them on the ideas you want most, up to {MAX_PER_IDEA} on one idea, then press Save.
            You can come back with the same link for 7 days to change them.
          </p>
        </div>
        <VoteBoard
          token={t}
          saved={state.picks}
          start={clicked ? { [clicked.id]: 1 } : state.picks}
          startNote={clicked ? `I put 1 vote on ${clicked.title}. Add more, or press Save my votes.` : ""}
          counts={state.counts}
        />
      </div>
    </section>
  );
}
