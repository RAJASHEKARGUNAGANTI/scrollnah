// POST /api/subscribe: newsletter sign-up. Saves the address as "pending" and sends a confirmation email.
// The answer is the same whether or not the address was already signed up, so nobody can check who is.
import { getDb } from "@/lib/db";
import { sendConfirmEmail } from "@/lib/mail";
import { SubscribeSchema, RESEND_AFTER_MS, hashToken, newToken, newsletterReady, originOf } from "@/lib/newsletter";
import { clientIp, hashIp, turnstileOk, underLimit } from "@/lib/spam";

const json = (body: object, status = 200) => Response.json(body, { status });

export async function POST(req: Request) {
  let raw: unknown;
  try {
    raw = await req.json();
  } catch {
    return json({ error: "That didn't come through. Please try again." }, 400);
  }
  const parsed = SubscribeSchema.safeParse(raw);
  if (!parsed.success) return json({ error: parsed.error.issues[0]?.message ?? "Please check your email." }, 400);
  const { email, website, turnstileToken } = parsed.data;

  if (website) return json({ ok: true }); // a bot filled in the hidden field

  const ip = clientIp(req);
  if (!(await turnstileOk(turnstileToken, ip))) {
    return json({ error: "The spam check didn't pass. Please try again." }, 400);
  }

  const dbPromise = getDb();
  if (!dbPromise || !newsletterReady()) {
    return json({ error: "The newsletter isn't switched on yet. Please try again later." }, 503);
  }

  try {
    const db = await dbPromise;
    if (!(await underLimit(db, `subscribe:${hashIp(ip)}`))) {
      return json({ error: "That's a lot of sign-ups in a short time. Please try again in an hour." }, 429);
    }
    const subscribers = db.collection("subscribers");
    const existing = await subscribers.findOne({ email });

    // Already confirmed: nothing to do (and nothing to reveal).
    if (existing?.status === "active") return json({ ok: true });

    // Asked again within a few minutes: don't send a second email yet.
    if (existing?.sentAt && Date.now() - new Date(existing.sentAt).getTime() < RESEND_AFTER_MS) {
      return json({ ok: true });
    }

    const token = newToken();
    const now = new Date();
    await subscribers.updateOne(
      { email },
      {
        $set: { status: "pending", tokenHash: hashToken(token), sentAt: now },
        $setOnInsert: { email, createdAt: now },
      },
      { upsert: true },
    );
    await sendConfirmEmail(email, `${originOf(req)}/newsletter/confirm?token=${encodeURIComponent(token)}`);
    return json({ ok: true });
  } catch (err) {
    console.error("subscribe failed", err);
    return json({ error: "Something went wrong on our side. Please try again later." }, 500);
  }
}
