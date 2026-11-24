import Link from "next/link";

export default function NotFound() {
  return (
    <div className="wrap lost">
      <img src="/img/blobby_melting.png" alt="" width={220} height={220} />
      <h1 style={{ fontSize: "clamp(36px, 6vw, 56px)" }}>Nah, this page isn&apos;t here.</h1>
      <p className="muted">Blobby looked everywhere and melted a little. Let&apos;s go back.</p>
      <Link className="btn lime" href="/">Back to home</Link>
    </div>
  );
}
