// Idea votes by email. Someone types their email, gets a vote link, and spreads 5 votes over the ideas
// (at most 3 on one idea). The website never keeps the address: only a scrambled fingerprint of it, so the
// same person can't vote twice, plus their votes.
import { createHmac } from "node:crypto";
import type { Db } from "mongodb";
import { z } from "zod";
import { IDEAS, IDEA_IDS, MAX_PER_IDEA, VOTE_BUDGET, type Picks } from "./ideas";
import { hashToken, secret } from "./newsletter";

/** A vote link works this long (a TTL index in lib/db.ts deletes it afterwards). */
export const LINK_DAYS = 7;
/** Don't email another vote link to the same person sooner than this. */
export const RESEND_AFTER_MS = 10 * 60_000;
const DAY_MS = 24 * 60 * 60 * 1000;

type VoteLink = { tokenHash: string; voter: string; createdAt: Date };
type Voter = { _id: string; picks: Picks; createdAt: Date; updatedAt: Date };

export const VoteLinkSchema = z.object({
  email: z.email("That email doesn't look right.").max(200).transform((e) => e.trim()),
  website: z.string().optional().default(""), // hidden trap field, like the other forms
  turnstileToken: z.string().max(4096).optional().default(""),
  idea: z.string().max(60).optional().default(""), // the idea whose Vote button was pressed
});

/**
 * One person, however they write their address: lower case and without "+anything"; for Gmail also
 * without dots (Gmail ignores them), so name+1@gmail.com and n.ame@gmail.com count as name@gmail.com.
 */
export function normalizeEmail(email: string): string {
  const e = email.trim().toLowerCase();
  const at = e.lastIndexOf("@");
  let user = e.slice(0, at).split("+")[0];
  let host = e.slice(at + 1);
  if (host === "googlemail.com") host = "gmail.com";
  if (host === "gmail.com") user = user.replaceAll(".", "");
  return `${user}@${host}`;
}

/**
 * The voter's fingerprint: a keyed hash (HMAC) of the normalized address. It can't be turned back into the
 * address. (If HASH_SALT ever changes, every fingerprint changes too, and people could vote again.)
 */
export function voterKey(email: string): string {
  const s = secret();
  if (!s) throw new Error("HASH_SALT is not set");
  return createHmac("sha256", s).update(`voter:${normalizeEmail(email)}`).digest("hex").slice(0, 32);
}

/** Checks votes sent from the vote page: known ideas, whole numbers 0-3, at most 5 in all. Null if not allowed. */
export function cleanPicks(raw: unknown): Picks | null {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) return null;
  const picks: Picks = {};
  let total = 0;
  for (const [id, n] of Object.entries(raw)) {
    if (!IDEA_IDS.has(id) || !Number.isInteger(n) || n < 0 || n > MAX_PER_IDEA) return null;
    if (n > 0) {
      picks[id] = n;
      total += n;
    }
  }
  return total <= VOTE_BUDGET ? picks : null;
}

/** Saves a new vote link for this voter. Returns false (and saves nothing) when one was sent a few minutes ago. */
export async function newVoteLink(db: Db, voter: string, token: string): Promise<boolean> {
  const links = db.collection<VoteLink>("vote_links");
  const recent = await links.findOne({ voter, createdAt: { $gt: new Date(Date.now() - RESEND_AFTER_MS) } });
  if (recent) return false;
  await links.insertOne({ tokenHash: hashToken(token), voter, createdAt: new Date() });
  return true;
}

/** Forgets a vote link (used when its email couldn't be sent). */
export async function dropVoteLink(db: Db, token: string): Promise<void> {
  await db.collection<VoteLink>("vote_links").deleteOne({ tokenHash: hashToken(token) });
}

/** The voter behind a vote link, or null if the link is unknown or older than 7 days. */
export async function voterForLink(db: Db, token: string): Promise<string | null> {
  if (!token || token.length > 100) return null;
  const link = await db.collection<VoteLink>("vote_links").findOne({ tokenHash: hashToken(token) });
  // MongoDB deletes old links about once a minute, so check the age here too.
  if (!link || Date.now() - link.createdAt.getTime() > LINK_DAYS * DAY_MS) return null;
  return link.voter;
}

/** This voter's current votes (only ideas that are still on the list). */
export async function picksOf(db: Db, voter: string): Promise<Picks> {
  const doc = await db.collection<Voter>("voters").findOne({ _id: voter });
  return Object.fromEntries(Object.entries(doc?.picks ?? {}).filter(([id]) => IDEA_IDS.has(id)));
}

/** Replaces this voter's votes. No votes at all = the entry is deleted. */
export async function savePicks(db: Db, voter: string, picks: Picks): Promise<void> {
  const voters = db.collection<Voter>("voters");
  if (Object.keys(picks).length === 0) {
    await voters.deleteOne({ _id: voter });
    return;
  }
  const now = new Date();
  await voters.updateOne({ _id: voter }, { $set: { picks, updatedAt: now }, $setOnInsert: { createdAt: now } }, { upsert: true });
}

/** Total votes per idea (adding up everyone's votes), and how many people voted. */
export async function voteTotals(db: Db): Promise<{ counts: Record<string, number>; voters: number }> {
  const counts: Record<string, number> = Object.fromEntries(IDEAS.map((i) => [i.id, 0]));
  const voters = db.collection<Voter>("voters");
  const rows = await voters
    .aggregate<{ _id: string; n: number }>([
      { $project: { p: { $objectToArray: "$picks" } } },
      { $unwind: "$p" },
      { $group: { _id: "$p.k", n: { $sum: "$p.v" } } },
    ])
    .toArray();
  for (const r of rows) if (IDEA_IDS.has(r._id)) counts[r._id] = r.n;
  return { counts, voters: await voters.countDocuments() };
}
