import type { Metadata } from "next";
import Link from "next/link";
import { VoteBoard } from "@/components/VoteBoard";
import { getDb } from "@/lib/db";
import { MAX_PER_IDEA, VOTE_BUDGET, type Picks } from "@/lib/ideas";
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
export default async function VotePage({ searchParams }: { searchParams: Promise<{ t?: string }> }) {
  const { t = "" } = await searchParams;
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
        <Link className="btn lime" href="/#vote">Get a new vote link</Link>
      </div>
    );
  }

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
        <VoteBoard token={t} initial={state.picks} counts={state.counts} />
      </div>
    </section>
  );
}
