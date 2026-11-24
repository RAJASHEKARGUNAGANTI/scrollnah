// Spam guards for the public forms: Cloudflare Turnstile + a small per-visitor limit.
import { createHash } from "node:crypto";
import type { Db } from "mongodb";

/** The visitor's IP as Vercel reports it (first address in x-forwarded-for). */
export function clientIp(req: Request): string {
  return (req.headers.get("x-forwarded-for") || "").split(",")[0].trim() || "unknown";
}

/** A scrambled IP, so the real address is never stored. */
export function hashIp(ip: string): string {
  return createHash("sha256").update(`${process.env.HASH_SALT || "scrollnah"}:${ip}`).digest("hex").slice(0, 32);
}

/** True when this visitor sent fewer than `max` messages in the last hour (and records this one). */
export async function underLimit(db: Db, key: string, max = 5): Promise<boolean> {
  const since = new Date(Date.now() - 60 * 60 * 1000);
  const recent = await db.collection("rate_limits").countDocuments({ key, at: { $gt: since } });
  if (recent >= max) return false;
  await db.collection("rate_limits").insertOne({ key, at: new Date() });
  return true;
}

/** Checks the Turnstile token with Cloudflare. Skipped (true) when Turnstile isn't set up yet. */
export async function turnstileOk(token: string, ip: string): Promise<boolean> {
  const secret = process.env.TURNSTILE_SECRET_KEY;
  if (!secret) return true;
  if (!token) return false;
  try {
    const res = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", {
      method: "POST",
      body: new URLSearchParams({ secret, response: token, remoteip: ip }),
    });
    const data = (await res.json()) as { success?: boolean };
    return data.success === true;
  } catch {
    return false;
  }
}
