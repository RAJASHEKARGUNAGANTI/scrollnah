// Sends a newsletter issue to everyone who confirmed. Run it from the website folder on your Mac:
//
//   node scripts/send-newsletter.mjs newsletter/issues/2026-12-first-issue.md          # dry run: shows what would happen
//   node scripts/send-newsletter.mjs newsletter/issues/2026-12-first-issue.md --test   # sends it only to you (NOTIFY_EMAIL)
//   node scripts/send-newsletter.mjs newsletter/issues/2026-12-first-issue.md --send   # sends it to every confirmed subscriber
//
// The issue file: a first line "Subject: ...", an empty line, then the text. Plain text only (no tracking).
// Each email gets a personal unsubscribe link and the one-click unsubscribe header that Gmail and others use.
// Reads MONGODB_URI, GMAIL_USER, GMAIL_APP_PASSWORD, NOTIFY_EMAIL and HASH_SALT from .env (never printed).
import { createHmac } from "node:crypto";
import { readFileSync } from "node:fs";
import nextEnv from "@next/env";
import { MongoClient } from "mongodb";
import nodemailer from "nodemailer";

nextEnv.loadEnvConfig(process.cwd(), false, { info() {}, error() {} });
const SITE = (process.env.SITE_URL || "https://scrollnah.vercel.app").replace(/\/$/, "");
const GMAIL_DAILY_SAFE = 450; // Gmail allows about 500 emails a day from a normal account

const [file, mode = "--dry"] = process.argv.slice(2);
if (!file || !["--dry", "--test", "--send"].includes(mode)) {
  console.log("Usage: node scripts/send-newsletter.mjs <issue.md> [--test | --send]   (no flag = dry run)");
  process.exit(1);
}

// The issue.
const raw = readFileSync(file, "utf8").replace(/\r\n/g, "\n");
const match = raw.match(/^Subject:\s*(.+)\n\n([\s\S]+)$/);
if (!match) {
  console.log('The file must start with "Subject: ..." then an empty line, then the text.');
  process.exit(1);
}
const subject = match[1].trim();
const body = match[2].trim();

// Must match lib/newsletter.ts (sign and keyFingerprint).
const secret = process.env.HASH_SALT || "";
if (secret.length < 16) {
  console.log("HASH_SALT in .env must be set (16+ characters), the same value as in Vercel.");
  process.exit(1);
}
const sign = (id) => createHmac("sha256", secret).update(`unsubscribe:${id}`).digest("base64url").slice(0, 32);
const fingerprint = createHmac("sha256", secret).update("scrollnah-key-check").digest("hex").slice(0, 8);
const mask = (email) => email.replace(/^(.).*?(@.*)$/, "$1***$2");

function message(to, id) {
  const pageLink = `${SITE}/newsletter/unsubscribe?id=${id}&sig=${sign(id)}`;
  const oneClick = `${SITE}/api/newsletter/unsubscribe?id=${id}&sig=${sign(id)}`;
  return {
    from: `ScrollNah <${process.env.GMAIL_USER}>`,
    to,
    subject,
    text: `${body}\n\n---\nYou're getting this because you signed up at ${SITE}.\nUnsubscribe: ${pageLink}\n`,
    headers: { "List-Unsubscribe": `<${oneClick}>`, "List-Unsubscribe-Post": "List-Unsubscribe=One-Click" },
  };
}

async function main() {
  // 1. The live site must sign unsubscribe links with the same secret, or the links won't work.
  try {
    const health = await (await fetch(`${SITE}/api/health`)).json();
    if (health.newsletterKey !== fingerprint) {
      console.log(`HASH_SALT here doesn't match the live site's (${SITE}). Copy the same value into Vercel (or into .env), redeploy, and try again.`);
      process.exit(1);
    }
  } catch {
    console.log(`Couldn't reach ${SITE}/api/health to check the secret. Check your internet and try again.`);
    process.exit(1);
  }

  const client = await new MongoClient(process.env.MONGODB_URI).connect();
  try {
    const subscribers = await client.db(process.env.MONGODB_DB || "scrollnah").collection("subscribers")
      .find({ status: "active" }, { projection: { email: 1 } }).toArray();

    console.log(`Subject:  ${subject}`);
    console.log(`Preview:  ${body.split("\n").filter((l) => l.trim()).slice(0, 3).join(" / ").slice(0, 120)}...`);
    console.log(`Confirmed subscribers: ${subscribers.length}`);

    if (mode === "--dry") {
      console.log("\nDry run: nothing was sent. Add --test to send it to yourself, or --send to send it to everyone.");
      return;
    }
    const transport = nodemailer.createTransport({ service: "gmail", auth: { user: process.env.GMAIL_USER, pass: process.env.GMAIL_APP_PASSWORD } });

    if (mode === "--test") {
      const me = process.env.NOTIFY_EMAIL || process.env.GMAIL_USER;
      // A made-up subscriber id: the unsubscribe link opens the page but won't match anyone.
      await transport.sendMail(message(me, "000000000000000000000000"));
      console.log(`\nTest sent to ${mask(me)}. Check how it looks, then run again with --send.`);
      return;
    }

    if (subscribers.length > GMAIL_DAILY_SAFE) {
      console.log(`\nThat's more than ${GMAIL_DAILY_SAFE} people, above what Gmail allows in a day. Time to move to an email service (see README).`);
      return;
    }
    let sent = 0, failed = 0;
    for (const s of subscribers) {
      try {
        await transport.sendMail(message(s.email, String(s._id)));
        sent++;
      } catch (err) {
        failed++;
        console.log(`  couldn't send to ${mask(s.email)}: ${err.message}`);
      }
      process.stdout.write(`\r  sent ${sent} of ${subscribers.length}${failed ? `, ${failed} failed` : ""}   `);
      await new Promise((r) => setTimeout(r, 2000)); // gentle pace, so Gmail doesn't flag it
    }
    console.log(`\nDone: sent ${sent}${failed ? `, ${failed} failed` : ""}.`);
  } finally {
    await client.close();
  }
}

main().catch((err) => {
  console.error("Failed:", String(err.message || err).replace(/mongodb(\+srv)?:\/\/[^@]*@/g, "mongodb://***@"));
  process.exit(1);
});
