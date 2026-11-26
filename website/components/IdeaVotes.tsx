"use client";
// "On the idea list" with a vote button on each idea. This browser remembers what it voted for
// (only so the button shows "Voted"); the server only keeps a count per idea.
import Link from "next/link";
import { useEffect, useState } from "react";
import { Icon } from "./Icon";
import { IDEAS } from "@/lib/ideas";

const STORE = "scrollnah-votes";

function loadVoted(): string[] {
  try {
    return JSON.parse(localStorage.getItem(STORE) || "[]");
  } catch {
    return [];
  }
}

export function IdeaVotes() {
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [voted, setVoted] = useState<string[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [note, setNote] = useState("");

  useEffect(() => {
    setVoted(loadVoted());
    fetch("/api/votes").then((r) => r.json()).then((d) => setCounts(d.counts || {})).catch(() => {});
  }, []);

  async function vote(id: string) {
    if (voted.includes(id) || busy) return;
    setBusy(id);
    setNote("");
    try {
      const res = await fetch("/api/votes", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ id }) });
      const data = (await res.json().catch(() => ({}))) as { count?: number; error?: string };
      if (!res.ok) {
        setNote(data.error || "Couldn't count that vote. Please try again.");
      } else {
        setCounts((c) => ({ ...c, [id]: data.count ?? (c[id] || 0) + 1 }));
        const next = [...voted, id];
        setVoted(next);
        try { localStorage.setItem(STORE, JSON.stringify(next)); } catch { /* private mode */ }
      }
    } catch {
      setNote("Couldn't reach the server. Please try again.");
    }
    setBusy(null);
  }

  return (
    <>
      <div className="grid two">
        {IDEAS.map((idea) => {
          const done = voted.includes(idea.id);
          const n = counts[idea.id];
          return (
            <div className="card soon" key={idea.id}>
              <div className={`tile ${idea.bg}`}><Icon name={idea.icon} /></div>
              <div style={{ flex: 1 }}>
                <h3>{idea.title}</h3>
                <p>{idea.text}</p>
                <button
                  type="button"
                  className={`vote${done ? " done" : ""}`}
                  onClick={() => vote(idea.id)}
                  disabled={done || busy === idea.id}
                  aria-label={done ? `You voted for ${idea.title}` : `Vote for ${idea.title}`}
                >
                  <Icon name={done ? "check" : "plus"} />
                  {done ? "Voted" : "Want this"}
                  {typeof n === "number" && <span className="count">{n}</span>}
                </button>
              </div>
            </div>
          );
        })}
      </div>
      {note && <p className="error" role="alert" style={{ marginTop: 12 }}>{note}</p>}
      <Link className="card soon" href="/feedback?type=feature" style={{ textDecoration: "none", marginTop: 20 }}>
        <div className="tile bg-lime"><Icon name="bulb" /></div>
        <div><h3>Your idea here</h3><p>Tell me what you&apos;d like next. Good ideas end up on this list.</p></div>
      </Link>
    </>
  );
}
