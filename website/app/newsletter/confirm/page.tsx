import type { Metadata } from "next";

export const metadata: Metadata = { title: "Confirm your sign-up", robots: { index: false } };

// The link in the confirmation email lands here. Confirming needs a tap on the button, so email apps
// that open links by themselves (to scan them) can't sign anyone up.
export default async function ConfirmPage({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token } = await searchParams;
  return (
    <div className="wrap lost">
      <img src="/img/blobby_happy.png" alt="" width={200} height={200} />
      <h1 style={{ fontSize: "clamp(34px, 6vw, 52px)" }}>One more tap</h1>
      <p className="muted">Press the button to get the ScrollNah newsletter: new versions, new mascots and what&apos;s coming, about once a month.</p>
      {token ? (
        <form method="post" action="/api/newsletter/confirm">
          <input type="hidden" name="token" value={token} />
          <button className="btn lime" type="submit">Yes, sign me up</button>
        </form>
      ) : (
        <p className="error">This link is missing a part. Please open it again from the email.</p>
      )}
      <p className="note">Changed your mind? Just close this page. Nothing is sent, and the request is deleted in 7 days.</p>
    </div>
  );
}
