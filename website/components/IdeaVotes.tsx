"use client";
// "On the idea list" on the home page: each idea with its votes so far and a Vote button.
// The button opens a popup that emails a vote link; the voting itself happens on /vote (VoteBoard),
// where each person spreads their votes.
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { EmailLinkForm } from "./EmailLinkForm";
import { Icon } from "./Icon";
import { IDEAS, MAX_PER_IDEA, VOTE_BUDGET, type Idea } from "@/lib/ideas";

export function IdeaVotes() {
  const [counts, setCounts] = useState<Record<string, number> | null>(null);
  const [voters, setVoters] = useState(0);
  const [open, setOpen] = useState<Idea | null>(null); // the idea whose popup is open
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    fetch("/api/votes")
      .then((r) => r.json())
      .then((d) => { setCounts(d.counts || {}); setVoters(d.voters || 0); })
      .catch(() => {});
  }, []);

  // The browser's own popup (<dialog>): dims the page, keeps the keyboard inside, closes with Esc.
  // Once it's open, the cursor goes straight into the email box.
  useEffect(() => {
    const d = dialog.current;
    if (!open || !d) return;
    if (!d.open) d.showModal();
    d.querySelector<HTMLInputElement>('input[type="email"]')?.focus();
  }, [open]);

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
                <div className="vote-row">
                  <button type="button" className="vote" onClick={() => setOpen(idea)} aria-label={`Vote for ${idea.title}`}>
                    <Icon name="plus" /> Vote
                  </button>
                  {typeof n === "number" && <span className="vote-total bump" key={n}>{n} {n === 1 ? "vote" : "votes"}</span>}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      {voters > 0 && (
        <p className="note" style={{ marginTop: 14 }}>
          {voters} {voters === 1 ? "person has" : "people have"} voted so far. Everyone gets {VOTE_BUDGET} votes, up to {MAX_PER_IDEA} on one idea.
        </p>
      )}

      <Link className="card soon" href="/feedback?type=feature" style={{ textDecoration: "none", marginTop: 20 }}>
        <div className="tile bg-lime"><Icon name="bulb" /></div>
        <div><h3>Your idea here</h3><p>Tell me what you&apos;d like next. Good ideas end up on this list.</p></div>
      </Link>

      <dialog
        ref={dialog}
        className="card vote-dialog"
        aria-labelledby="vote-dialog-title"
        onClose={() => setOpen(null)}
        // A click on the dimmed area around the popup closes it.
        onClick={(e) => { if (e.target === e.currentTarget) dialog.current?.close(); }}
      >
        {open && (
          <>
            <div className="vote-dialog-head">
              <div className={`tile ${open.bg}`}><Icon name={open.icon} /></div>
              <h3 id="vote-dialog-title">Vote for {open.title}</h3>
              <button type="button" className="round-btn" onClick={() => dialog.current?.close()} aria-label="Close">
                <Icon name="close" />
              </button>
            </div>
            <ol className="vote-steps">
              <li>Type your email. I&apos;ll send you a vote link.</li>
              <li>Open the link. {open.title} already has 1 vote from you.</li>
              <li>You have {VOTE_BUDGET} votes in all, up to {MAX_PER_IDEA} on one idea. Press Save, and that&apos;s it.</li>
            </ol>
            <EmailLinkForm
              key={open.id}
              endpoint="/api/vote-link"
              button="Email me my vote link"
              extra={{ idea: open.id }}
              sentTitle="Check your inbox!"
              sentText={(email) => <>I sent your vote link to {email}. It works for 7 days. Not there? Look in Spam or Promotions.</>}
            >
              <p className="note">
                Your email is only used to send the link; the website doesn&apos;t keep it. See <Link href="/privacy">privacy</Link>.
              </p>
            </EmailLinkForm>
          </>
        )}
      </dialog>
    </>
  );
}
