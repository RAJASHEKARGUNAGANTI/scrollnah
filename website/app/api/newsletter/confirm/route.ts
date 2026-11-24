// POST /api/newsletter/confirm: the "Yes, sign me up" button on /newsletter/confirm.
// (A button, not the email link itself: some email apps open links on their own to scan them.)
import { getDb } from "@/lib/db";
import { hashToken } from "@/lib/newsletter";

export async function POST(req: Request) {
  const done = (s: string) => Response.redirect(new URL(`/newsletter/done?s=${s}`, req.url), 303);
  const form = await req.formData().catch(() => null);
  const token = String(form?.get("token") ?? "");
  const dbPromise = getDb();
  if (!token || !dbPromise) return done("invalid");
  try {
    const db = await dbPromise;
    const result = await db.collection("subscribers").updateOne(
      { tokenHash: hashToken(token), status: "pending" },
      { $set: { status: "active", confirmedAt: new Date() }, $unset: { tokenHash: "", sentAt: "" } },
    );
    return done(result.modifiedCount === 1 ? "confirmed" : "invalid");
  } catch (err) {
    console.error("confirm failed", err);
    return done("error");
  }
}
