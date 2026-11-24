// GET /api/health: a quick check of the live site. It says which settings are there (yes/no only, never
// the values), whether the database answers, and whether the Turnstile secret is valid. Open it in a browser.
import { getDb } from "@/lib/db";

export const dynamic = "force-dynamic"; // check every time, never a cached answer

const has = (name: string) => Boolean(process.env[name]);

export async function GET() {
  // Database: connect and ping.
  let database = "not set up (MONGODB_URI missing)";
  const dbPromise = getDb();
  if (dbPromise) {
    try {
      const db = await dbPromise;
      await db.command({ ping: 1 });
      database = "ok";
    } catch (err) {
      console.error("health: database", err);
      database = "can't connect (see Vercel Logs)";
    }
  }

  // Turnstile secret: Cloudflare answers "invalid-input-secret" when the secret itself is wrong.
  let turnstileSecret = "not set";
  const secret = process.env.TURNSTILE_SECRET_KEY;
  if (secret) {
    try {
      const res = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", {
        method: "POST",
        body: new URLSearchParams({ secret, response: "health-check" }),
      });
      const data = (await res.json()) as { "error-codes"?: string[] };
      turnstileSecret = data["error-codes"]?.includes("invalid-input-secret") ? "wrong secret" : "ok";
    } catch {
      turnstileSecret = "couldn't check";
    }
  }

  return Response.json(
    {
      build: process.env.VERCEL_GIT_COMMIT_SHA?.slice(0, 7) ?? "local",
      database,
      email: has("GMAIL_USER") && has("GMAIL_APP_PASSWORD") ? "set" : "not set",
      turnstileSiteKey: has("NEXT_PUBLIC_TURNSTILE_SITE_KEY") ? "set" : "not set",
      turnstileSecret,
    },
    { headers: { "Cache-Control": "no-store" } },
  );
}
