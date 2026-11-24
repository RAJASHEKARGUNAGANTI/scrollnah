// POST /api/newsletter/unsubscribe: deletes the address straight away.
// Used by the button on /newsletter/unsubscribe, and by email apps' one-click unsubscribe
// (RFC 8058: they POST to the link in the List-Unsubscribe header, with id and sig in the address).
import { ObjectId } from "mongodb";
import { getDb } from "@/lib/db";
import { signatureOk } from "@/lib/newsletter";

export async function POST(req: Request) {
  const url = new URL(req.url);
  const form = await req.formData().catch(() => null);
  const id = String(form?.get("id") ?? url.searchParams.get("id") ?? "");
  const sig = String(form?.get("sig") ?? url.searchParams.get("sig") ?? "");
  const fromPage = form?.get("from") === "page";
  const answer = (s: string, status = 200) =>
    fromPage ? Response.redirect(new URL(`/newsletter/done?s=${s}`, req.url), 303) : new Response(s, { status });

  if (!signatureOk(id, sig)) return answer("invalid", 400);
  const dbPromise = getDb();
  if (!dbPromise) return answer("error", 503);
  try {
    const db = await dbPromise;
    await db.collection("subscribers").deleteOne({ _id: new ObjectId(id) });
    return answer("unsubscribed");
  } catch (err) {
    console.error("unsubscribe failed", err);
    return answer("error", 500);
  }
}
