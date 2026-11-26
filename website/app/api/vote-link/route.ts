// POST /api/vote-link {email}: emails a link to the vote page. The address itself is never saved: only a
// scrambled fingerprint of it, so one person can't vote twice (lib/votes.ts).
// The answer is the same whether or not this person voted before, so nobody can check who did.
import { getDb } from "@/lib/db";
import { IDEA_IDS, MAX_PER_IDEA, VOTE_BUDGET } from "@/lib/ideas";
import { sendVoteLinkEmail } from "@/lib/mail";
import { newToken, newsletterReady, originOf } from "@/lib/newsletter";
import { clientIp, hashIp, turnstileOk, underLimit } from "@/lib/spam";
import { VoteLinkSchema, dropVoteLink, newVoteLink, voterKey } from "@/lib/votes";

const json = (body: object, status = 200) => Response.json(body, { status });

export async function POST(req: Request) {
  const raw = await req.json().catch(() => null);
  const parsed = VoteLinkSchema.safeParse(raw);
  if (!parsed.success) return json({ error: parsed.error.issues[0]?.message ?? "Please check your email." }, 400);
  const { email, website, turnstileToken, idea } = parsed.data;

  if (website) return json({ ok: true }); // a bot filled in the hidden field

  const ip = clientIp(req);
  if (!(await turnstileOk(turnstileToken, ip))) {
    return json({ error: "The spam check didn't pass. Please try again." }, 400);
  }

  const dbPromise = getDb();
  if (!dbPromise || !newsletterReady()) return json({ error: "Voting isn't switched on yet. Please try again later." }, 503);

  try {
    const db = await dbPromise;
    if (!(await underLimit(db, `votelink:${hashIp(ip)}`))) {
      return json({ error: "That's a lot of vote links in a short time. Please try again in an hour." }, 429);
    }
    const token = newToken();
    // Asked again within a few minutes: the first email is still on its way, so don't send another.
    if (!(await newVoteLink(db, voterKey(email), token))) return json({ ok: true });
    try {
      // The vote page starts with one vote on the idea whose button was pressed (if it's a real idea).
      const ideaPart = IDEA_IDS.has(idea) ? `&idea=${idea}` : "";
      await sendVoteLinkEmail(email, `${originOf(req)}/vote?t=${encodeURIComponent(token)}${ideaPart}`, VOTE_BUDGET, MAX_PER_IDEA);
    } catch (err) {
      await dropVoteLink(db, token); // so asking again sends a new email straight away
      throw err;
    }
    return json({ ok: true });
  } catch (err) {
    console.error("vote link failed", err);
    return json({ error: "Something went wrong on our side. Please try again later." }, 500);
  }
}
