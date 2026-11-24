// GET /api/health: a quick check of the live site. It says which settings are there (yes/no only, never
// the values), whether the database answers, and whether the Turnstile secret is valid. Open it in a browser.
import { getDb } from "@/lib/db";

export const dynamic = "force-dynamic"; // check every time, never a cached answer

const has = (name: string) => Boolean(process.env[name]);

/** Looks at the shape of MONGODB_URI for the usual copy-paste slips. Never returns any part of it. */
function addressSlip(): string {
  const uri = (process.env.MONGODB_URI || "").trim().replace(/^(["'])(.*)\1$/, "$2");
  if (uri.startsWith("MONGODB_URI=")) return "remove 'MONGODB_URI=' from the start of the value";
  if (!/^mongodb(\+srv)?:\/\//.test(uri)) return "the value must start with mongodb+srv://";
  if (/[<>]/.test(uri)) return "remove the < > around the password";
  // The password sits between "user:" and the last "@" before the host.
  const userInfo = uri.replace(/^mongodb(\+srv)?:\/\//, "").split("/")[0];
  const at = userInfo.lastIndexOf("@");
  const password = at > 0 && userInfo.indexOf(":") > 0 ? userInfo.slice(userInfo.indexOf(":") + 1, at) : "";
  if (/[@:/?#[\]]|%(?![0-9A-Fa-f]{2})/.test(password)) {
    return "the password has special characters that must be encoded (@ = %40, : = %3A, / = %2F, # = %23, ? = %3F, % = %25)";
  }
  if (/\s/.test(uri)) return "there is a space or line break inside the value";
  return "paste it again in Vercel (starts with mongodb+srv://, no quotes, no name, real password)";
}

/** A plain hint for the usual database problems. Never includes the address or password. */
function explainDbError(err: unknown): string {
  const name = err instanceof Error ? err.name : "";
  const message = err instanceof Error ? err.message.toLowerCase() : "";
  // Brackets left around the password still parse, then fail at login, so check them first.
  if (/[<>]/.test(process.env.MONGODB_URI || "")) return "MONGODB_URI: remove the < > around the password";
  if (name === "MongoParseError" || message.includes("invalid scheme") || message.includes("connection string") || name === "TypeError" || name === "URIError") {
    return `MONGODB_URI isn't a valid address: ${addressSlip()}`;
  }
  if (message.includes("auth")) return "wrong database username or password in MONGODB_URI";
  if (message.includes("enotfound") || message.includes("querysrv")) return "can't find the cluster: check the part after @ in MONGODB_URI";
  if (name === "MongoServerSelectionError") return "can't reach the cluster (Atlas > Network Access, or the cluster is paused)";
  return `can't connect (${name || "unknown error"}, see Vercel Logs)`;
}

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
      database = explainDbError(err);
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
      // Which deployment answered (changes on every deploy and redeploy, unlike the commit above).
      deployment: (process.env.VERCEL_DEPLOYMENT_ID || process.env.VERCEL_URL || "local").slice(-10),
      database,
      email: has("GMAIL_USER") && has("GMAIL_APP_PASSWORD") ? "set" : "not set",
      turnstileSiteKey: has("NEXT_PUBLIC_TURNSTILE_SITE_KEY") ? "set" : "not set",
      turnstileSecret,
    },
    { headers: { "Cache-Control": "no-store" } },
  );
}
