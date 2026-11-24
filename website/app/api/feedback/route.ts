// POST /api/feedback: checks the message, stops spam, saves it to MongoDB and emails the developer.
import { getDb } from "@/lib/db";
import { FeedbackSchema } from "@/lib/feedback";
import { notifyNewFeedback } from "@/lib/mail";
import { clientIp, hashIp, turnstileOk, underLimit } from "@/lib/spam";

const json = (body: object, status = 200) => Response.json(body, { status });

export async function POST(req: Request) {
  let raw: unknown;
  try {
    raw = await req.json();
  } catch {
    return json({ error: "That didn't come through. Please try again." }, 400);
  }

  const parsed = FeedbackSchema.safeParse(raw);
  if (!parsed.success) {
    return json({ error: parsed.error.issues[0]?.message ?? "Please check the form." }, 400);
  }
  const f = parsed.data;

  // A bot filled in the hidden field: pretend it worked and save nothing.
  if (f.website) return json({ ok: true });

  const ip = clientIp(req);
  if (!(await turnstileOk(f.turnstileToken, ip))) {
    return json({ error: "The spam check didn't pass. Please try again." }, 400);
  }

  const dbPromise = getDb();
  if (!dbPromise) {
    return json({ error: "The form isn't switched on yet. Please try again later." }, 503);
  }

  try {
    const db = await dbPromise;
    if (!(await underLimit(db, hashIp(ip)))) {
      return json({ error: "That's a lot of messages in a short time. Please try again in an hour." }, 429);
    }
    await db.collection("feedback").insertOne({
      type: f.type,
      message: f.message,
      steps: f.steps,
      email: f.email, // only if they chose to give one, for a reply
      source: f.source,
      appVersion: f.appVersion,
      android: f.android,
      device: f.device,
      status: "new", // change to "seen" or "done" in MongoDB Atlas as you go
      createdAt: new Date(),
    });
  } catch (err) {
    console.error("feedback save failed", err);
    return json({ error: "Something went wrong on our side. Please try again later." }, 500);
  }

  // The message is saved; an email problem shouldn't show the visitor an error.
  try {
    await notifyNewFeedback(f);
  } catch (err) {
    console.error("feedback email failed", err);
  }
  return json({ ok: true });
}
