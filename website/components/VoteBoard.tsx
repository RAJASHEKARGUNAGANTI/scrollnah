"use client";
// The vote page's board: a - / + for each idea (0 to 3 votes), "N of 5 votes left" and a Save button.
// Nothing counts until "Save my votes" is pressed.
import { useState } from "react";
import { Icon } from "./Icon";
import { IDEAS, MAX_PER_IDEA, VOTE_BUDGET, type Picks } from "@/lib/ideas";

const total = (p: Picks) => Object.values(p).reduce((a, b) => a + b, 0);
const same = (a: Picks, b: Picks) => IDEAS.every((i) => (a[i.id] || 0) === (b[i.id] || 0));

export function VoteBoard({ token, initial, counts: firstCounts }: { token: string; initial: Picks; counts: Record<string, number> }) {
  const [picks, setPicks] = useState<Picks>(initial);
  const [saved, setSaved] = useState<Picks>(initial); // what the server has
  const [counts, setCounts] = useState(firstCounts);
  const [saving, setSaving] = useState(false);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");

  const left = VOTE_BUDGET - total(picks);
  const changed = !same(picks, saved);

  function change(id: string, by: number) {
    setNote("");
    setPicks((p) => {
      const n = Math.min(MAX_PER_IDEA, Math.max(0, (p[id] || 0) + by));
      const next = { ...p };
      if (n > 0) next[id] = n;
      else delete next[id];
      return next;
    });
  }

  async function save() {
    setSaving(true);
    setError("");
    setNote("");
    try {
      const res = await fetch("/api/votes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, picks }),
      });
      const data = (await res.json().catch(() => ({}))) as { ok?: boolean; error?: string; counts?: Record<string, number> };
      if (res.ok && data.ok) {
        setSaved(picks);
        if (data.counts) setCounts(data.counts);
        setNote(total(picks) > 0 ? "Saved. Thanks for voting!" : "Saved. Your votes are removed.");
      } else {
        setError(data.error || "Couldn't save. Please try again.");
      }
    } catch {
      setError("Couldn't reach the server. Check your internet and try again.");
    }
    setSaving(false);
  }

  return (
    <>
      <div className="grid two">
        {IDEAS.map((idea) => {
          const n = picks[idea.id] || 0;
          const all = counts[idea.id] || 0;
          return (
            <div className={`card soon${n > 0 ? " picked" : ""}`} key={idea.id}>
              <div className={`tile ${idea.bg}`}><Icon name={idea.icon} /></div>
              <div>
                <h3>{idea.title}</h3>
                <p>{idea.text}</p>
                <div className="stepper">
                  <button type="button" onClick={() => change(idea.id, -1)} disabled={n === 0} aria-label={`One vote less for ${idea.title}`}>
                    <Icon name="minus" />
                  </button>
                  <span className="pips" role="img" aria-label={`${n} of ${MAX_PER_IDEA} votes`}>
                    {Array.from({ length: MAX_PER_IDEA }, (_, i) => <span key={i} className={i < n ? "on" : ""} />)}
                  </span>
                  <button type="button" onClick={() => change(idea.id, 1)} disabled={n >= MAX_PER_IDEA || left <= 0} aria-label={`One more vote for ${idea.title}`}>
                    <Icon name="plus" />
                  </button>
                </div>
                <p className="note" style={{ marginTop: 8 }}>{all} {all === 1 ? "vote" : "votes"} from everyone so far</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="card vote-bar">
        <div aria-live="polite">
          <strong>{left < 0 ? `${-left} too many votes` : `${left} of ${VOTE_BUDGET} votes left`}</strong>
          <span className="pips budget" aria-hidden="true">
            {Array.from({ length: VOTE_BUDGET }, (_, i) => <span key={i} className={i < VOTE_BUDGET - left ? "on" : ""} />)}
          </span>
        </div>
        <button className="btn lime" type="button" onClick={save} disabled={saving || !changed || left < 0}>
          {saving ? "Saving..." : "Save my votes"}
        </button>
      </div>
      {note && <p role="status" style={{ marginTop: 14, fontWeight: 700 }}>{note}</p>}
      {error && <p className="error" role="alert" style={{ marginTop: 14 }}>{error}</p>}
    </>
  );
}
