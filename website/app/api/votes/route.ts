// Idea votes. GET: the count for each idea. POST {id}: adds one vote.
// Votes are anonymous: only a counter goes up. A light limit (scrambled IP, kept 1 hour) stops scripts.
import { getDb } from "@/lib/db";
import { IDEAS, IDEA_IDS } from "@/lib/ideas";
import { clientIp, hashIp, underLimit } from "@/lib/spam";

export const dynamic = "force-dynamic";

const json = (body: object, status = 200) => Response.json(body, { status, headers: { "Cache-Control": "no-store" } });

export async function GET() {
  const dbPromise = getDb();
  if (!dbPromise) return json({ counts: {} });
  try {
    const db = await dbPromise;
    const docs = await db.collection<{ _id: string; count: number }>("idea_votes").find({}).toArray();
    const counts: Record<string, number> = Object.fromEntries(IDEAS.map((i) => [i.id, 0]));
    for (const d of docs) if (IDEA_IDS.has(d._id)) counts[d._id] = d.count;
    return json({ counts });
  } catch (err) {
    console.error("votes read failed", err);
    return json({ counts: {} });
  }
}

export async function POST(req: Request) {
  const body = (await req.json().catch(() => ({}))) as { id?: unknown };
  const id = typeof body.id === "string" ? body.id : "";
  if (!IDEA_IDS.has(id)) return json({ error: "Unknown idea." }, 400);
  const dbPromise = getDb();
  if (!dbPromise) return json({ error: "Voting isn't switched on yet." }, 503);
  try {
    const db = await dbPromise;
    if (!(await underLimit(db, `vote:${hashIp(clientIp(req))}`, 20))) {
      return json({ error: "That's a lot of votes. Try again in an hour." }, 429);
    }
    const doc = await db
      .collection<{ _id: string; count: number }>("idea_votes")
      .findOneAndUpdate({ _id: id }, { $inc: { count: 1 } }, { upsert: true, returnDocument: "after" });
    return json({ id, count: doc?.count ?? 1 });
  } catch (err) {
    console.error("vote failed", err);
    return json({ error: "Something went wrong. Please try again later." }, 500);
  }
}
