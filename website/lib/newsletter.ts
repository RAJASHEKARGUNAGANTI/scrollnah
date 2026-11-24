// Newsletter helpers: checking the email, the confirm token, and signed unsubscribe links.
import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { z } from "zod";

export const SubscribeSchema = z.object({
  email: z.email("That email doesn't look right.").max(200).transform((e) => e.trim().toLowerCase()),
  website: z.string().optional().default(""), // hidden trap field, like the feedback form
  turnstileToken: z.string().max(4096).optional().default(""),
});

/** Unconfirmed sign-ups are deleted after this (a TTL index in lib/db.ts). */
export const PENDING_DAYS = 7;
/** Don't send another confirmation email to the same address sooner than this. */
export const RESEND_AFTER_MS = 10 * 60_000;

/** A fresh random token for the confirmation link. Only its hash is stored. */
export function newToken(): string {
  return randomBytes(24).toString("base64url");
}

export function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

/**
 * The secret that signs unsubscribe links. Must be set (HASH_SALT): with a known secret anyone could
 * make an unsubscribe link for someone else.
 */
function secret(): string | null {
  const s = process.env.HASH_SALT;
  return s && s.length >= 16 ? s : null;
}

export const newsletterReady = () => secret() !== null;

/** Signature for a subscriber's unsubscribe link (stable, so every issue can carry it). */
export function sign(id: string): string {
  const s = secret();
  if (!s) throw new Error("HASH_SALT is not set");
  return createHmac("sha256", s).update(`unsubscribe:${id}`).digest("base64url").slice(0, 32);
}

export function signatureOk(id: string, sig: string): boolean {
  if (!secret() || !/^[a-f0-9]{24}$/.test(id) || typeof sig !== "string") return false;
  const want = Buffer.from(sign(id));
  const got = Buffer.from(sig);
  return want.length === got.length && timingSafeEqual(want, got);
}

/** The address people see in links: the request's own origin (localhost while testing). */
export function originOf(req: Request): string {
  return new URL(req.url).origin;
}

/**
 * A short fingerprint of the signing secret (an HMAC of a fixed text). It can't be turned back into the
 * secret; the send script compares it with the live site's, so both sign unsubscribe links the same way.
 */
export function keyFingerprint(): string | null {
  const s = secret();
  return s ? createHmac("sha256", s).update("scrollnah-key-check").digest("hex").slice(0, 8) : null;
}
