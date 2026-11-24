import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Newsletter", robots: { index: false } };

const MESSAGES: Record<string, { img: string; title: string; text: string }> = {
  confirmed: {
    img: "blobby_thriving",
    title: "You're in!",
    text: "Thanks for signing up. The next ScrollNah newsletter will land in your inbox. Every email has an unsubscribe link at the bottom.",
  },
  unsubscribed: {
    img: "blobby_happy",
    title: "You're unsubscribed",
    text: "Your email address is deleted, and no more newsletters will come. Thanks for reading!",
  },
  invalid: {
    img: "blobby_worried",
    title: "That link didn't work",
    text: "It may have expired (sign-up links last 7 days) or been used already. If you're not getting the newsletter, sign up again on the home page.",
  },
  error: {
    img: "blobby_melting",
    title: "Something went wrong",
    text: "That's on our side. Please try the link again in a little while.",
  },
};

export default async function DonePage({ searchParams }: { searchParams: Promise<{ s?: string }> }) {
  const { s } = await searchParams;
  const m = MESSAGES[s ?? ""] ?? MESSAGES.invalid;
  return (
    <div className="wrap lost">
      <img src={`/img/${m.img}.png`} alt="" width={200} height={200} />
      <h1 style={{ fontSize: "clamp(34px, 6vw, 52px)" }}>{m.title}</h1>
      <p className="muted" style={{ maxWidth: 560 }}>{m.text}</p>
      <Link className="btn lime" href="/">Back to home</Link>
    </div>
  );
}
