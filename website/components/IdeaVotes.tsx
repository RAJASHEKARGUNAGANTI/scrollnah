"use client";
// "On the idea list" on the home page: each idea with its votes so far, and a box that emails you a vote link.
// The voting itself happens on /vote (VoteBoard), where each person spreads their votes.
import Link from "next/link";
import { useEffect, useState } from "react";
import { EmailLinkForm } from "./EmailLinkForm";
import { Icon } from "./Icon";
import { IDEAS, MAX_PER_IDEA, VOTE_BUDGET } from "@/lib/ideas";

export function IdeaVotes() {
  const [counts, setCounts] = useState<Record<string, number> | null>(null);
  const [voters, setVoters] = useState(0);

  useEffect(() => {
    fetch("/api/votes")
      .then((r) => r.json())
      .then((d) => { setCounts(d.counts || {}); setVoters(d.voters || 0); })
      .catch(() => {});
  }, []);

  return (
    <>
      <div className="grid two">
        {IDEAS.map((idea) => {
          const n = counts?.[idea.id];
          return (
            <div className="card soon" key={idea.id}>
              <div className={`tile ${idea.bg}`}><Icon name={idea.icon} /></div>
              <div>
                <h3>{idea.title}</h3>
                <p>{idea.text}</p>
                {typeof n === "number" && <span className="badge vote-total">{n} {n === 1 ? "vote" : "votes"}</span>}
              </div>
            </div>
          );
        })}
      </div>

      <div className="card votebox" id="vote">
        <img src="/img/blobby_happy.png" alt="" width={120} height={120} />
        <div>
          <h3>Want a say? Get your vote link</h3>
          <p className="muted">
            You get {VOTE_BUDGET} votes to spread over the ideas, up to {MAX_PER_IDEA} on one. I&apos;ll email you a link to vote.
            {voters > 0 && ` ${voters} ${voters === 1 ? "person has" : "people have"} voted so far.`}
          </p>
          <EmailLinkForm
            endpoint="/api/vote-link"
            button="Email me my vote link"
            sentTitle="Check your inbox!"
            sentText={(email) => <>I sent your vote link to {email}. It works for 7 days. Not there? Look in Spam or Promotions.</>}
          >
            <p className="note">
              Your email is only used to send the link; the website doesn&apos;t keep it. See <Link href="/privacy">privacy</Link>.
            </p>
          </EmailLinkForm>
        </div>
      </div>

      <Link className="card soon" href="/feedback?type=feature" style={{ textDecoration: "none", marginTop: 20 }}>
        <div className="tile bg-lime"><Icon name="bulb" /></div>
        <div><h3>Your idea here</h3><p>Tell me what you&apos;d like next. Good ideas end up on this list.</p></div>
      </Link>
    </>
  );
}
