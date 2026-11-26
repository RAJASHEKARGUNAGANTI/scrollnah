// Idea votes. GET: the total votes per idea and how many people voted.
// POST {token, picks}: saves someone's votes from the vote page (the token comes from their vote link).
import { getDb } from "@/lib/db";
import { MAX_PER_IDEA, VOTE_BUDGET } from "@/lib/ideas";
import { newsletterReady } from "@/lib/newsletter";
import { clientIp, hashIp, underLimit } from "@/lib/spam";
import { cleanPicks, savePicks, voteTotals, voterForLink } from "@/lib/votes";

export const dynamic = "force-dynamic";

const json = (body: object, status = 200) => Response.json(body, { status, headers: { "Cache-Control": "no-store" } });

export async function GET() {
  const dbPromise = getDb();
  if (!dbPromise) return json({ counts: {}, voters: 0 });
  try {
    return json(await voteTotals(await dbPromise));
  } catch (err) {
    console.error("votes read failed", err);
    return json({ counts: {}, voters: 0 });
  }
}

export async function POST(req: Request) {
  const body = (await req.json().catch(() => ({}))) as { token?: unknown; picks?: unknown };
  const token = typeof body.token === "string" ? body.token : "";
  const picks = cleanPicks(body.picks);
  if (!picks) return json({ error: `You have ${VOTE_BUDGET} votes, with up to ${MAX_PER_IDEA} on one idea.` }, 400);

  const dbPromise = getDb();
  if (!dbPromise || !newsletterReady()) return json({ error: "Voting isn't switched on yet. Please try again later." }, 503);
  try {
    const db = await dbPromise;
    if (!(await underLimit(db, `votesave:${hashIp(clientIp(req))}`, 30))) {
      return json({ error: "That's a lot of saving. Please try again in an hour." }, 429);
    }
    const voter = await voterForLink(db, token);
    if (!voter) return json({ error: "This vote link has expired. Ask for a new one on the home page." }, 403);
    await savePicks(db, voter, picks);
    return json({ ok: true, picks, ...(await voteTotals(db)) });
  } catch (err) {
    console.error("vote save failed", err);
    return json({ error: "Something went wrong. Please try again later." }, 500);
  }
}
