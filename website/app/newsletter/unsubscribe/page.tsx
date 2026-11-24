import type { Metadata } from "next";

export const metadata: Metadata = { title: "Unsubscribe", robots: { index: false } };

// The "Unsubscribe" link at the bottom of every newsletter lands here.
export default async function UnsubscribePage({ searchParams }: { searchParams: Promise<{ id?: string; sig?: string }> }) {
  const { id, sig } = await searchParams;
  return (
    <div className="wrap lost">
      <img src="/img/blobby_worried.png" alt="" width={200} height={200} />
      <h1 style={{ fontSize: "clamp(34px, 6vw, 52px)" }}>Unsubscribe?</h1>
      <p className="muted">You won&apos;t get the ScrollNah newsletter any more, and your email address is deleted straight away.</p>
      {id && sig ? (
        <form method="post" action="/api/newsletter/unsubscribe">
          <input type="hidden" name="id" value={id} />
          <input type="hidden" name="sig" value={sig} />
          <input type="hidden" name="from" value="page" />
          <button className="btn orange" type="submit">Unsubscribe</button>
        </form>
      ) : (
        <p className="error">This link is missing a part. Please open it again from the email.</p>
      )}
    </div>
  );
}
