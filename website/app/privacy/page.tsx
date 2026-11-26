import type { Metadata } from "next";
import Link from "next/link";
import { site } from "@/lib/site";

export const metadata: Metadata = {
  title: "Privacy",
  description: "What the ScrollNah app, the browser extension and this website keep, and where.",
};

export default function PrivacyPage() {
  return (
    <>
      <section className="page-hero">
        <div className="wrap">
          <span className="eyebrow">Privacy</span>
          <h1 style={{ fontSize: "clamp(38px, 6vw, 60px)" }}>Privacy, in plain words</h1>
          <p className="muted">Last updated: 26 November 2026</p>
        </div>
      </section>
      <div className="wrap"><div className="prose">
        <div className="card callout">
          The app and the browser extension collect nothing: no account, no ads, no analytics, and nothing they know leaves your
          phone or your browser. This website only keeps what you
          type into the feedback form or the newsletter box, and your idea votes (without your email address).
        </div>

        <h2>The ScrollNah app</h2>
        <p>
          Everything the app knows (your limits, minutes, streaks and garden) stays on your phone. It does not send anything to me or to
          anyone else. The full app policy, including the optional Reels &amp; Shorts counter and Parent lock, is here:{" "}
          <a href={site.appPrivacy}>ScrollNah app privacy policy</a>.
        </p>

        <h2 id="extension">The browser extension</h2>
        <p>
          ScrollNah for Chrome, Edge, Brave, Opera and Firefox keeps everything in your own browser (its extension storage) and sends
          nothing to me or to anyone else. It has no account, no server and no analytics. It keeps:
        </p>
        <ul>
          <li>your settings: the sites you limit and their limits, strictness, bedtime and work hours, your mascot and the name you typed</li>
          <li>for each day: minutes and visits per site, minutes on YouTube Shorts and Instagram Reels, and your answers on the Pause page (the last 60 days)</li>
        </ul>
        <p>
          <strong>What it can see.</strong> It asks for access to each site you add, one by one, and to nothing else. On those sites it
          notes which site is open and for how long; it never reads or keeps what&apos;s on the pages, what you type, or the addresses you
          visit. The &ldquo;hide&rdquo; switches only hide parts of the page (like YouTube Shorts) with styles, and on X it checks which
          timeline tab is selected so it can switch to &ldquo;Following&rdquo;.
        </p>
        <p>
          <strong>Import from your phone.</strong> The file you make in the app (Settings &gt; Backup &gt; Copy limits to computer) holds
          only your limits and a few settings. You move it yourself, and the extension reads it on your computer; nothing is uploaded.
        </p>
        <p>
          <strong>Removing it.</strong> Uninstalling the extension deletes everything it saved. You can also reset it any time in its
          settings under &ldquo;Your data&rdquo;.
        </p>

        <h2>This website</h2>
        <p><strong>The feedback form.</strong> When you send a message, the website keeps:</p>
        <ul>
          <li>what you wrote, and whether it&apos;s feedback, a feature request or a bug report</li>
          <li>the phone details you chose to give (ScrollNah version, Android version, phone model, where you got the app)</li>
          <li>your email, only if you typed one, so I can reply</li>
          <li>the date and time</li>
        </ul>
        <p>
          It&apos;s stored in a database hosted by MongoDB Atlas, and I get a copy by email (Gmail) so I don&apos;t miss it. I use it only to
          read, reply and improve ScrollNah. I never sell it or share it, and I don&apos;t add your email to any newsletter. Messages are
          deleted automatically after 2 years at the latest.
        </p>
        <p>
          <strong>The newsletter.</strong> If you sign up, the website keeps your email address and when you signed up and
          confirmed. You get one confirmation email first; nothing else is sent until you confirm, and unconfirmed sign-ups are
          deleted after 7 days. The newsletter comes from ScrollNah&apos;s Gmail, about once a month, with no tracking pixels and
          no tracked links. Every email has an unsubscribe link, and unsubscribing deletes your address straight away. Your
          address is never sold or shared.
        </p>
        <p>
          <strong>Idea votes.</strong> To vote, you type your email address and get a vote link by email. The website doesn&apos;t
          keep your address: it keeps only a scrambled fingerprint of it, which can&apos;t be turned back into the address but
          stops the same address from voting twice, together with how many votes you gave each idea. Vote links stop working
          after 7 days and are then deleted. The link is sent from ScrollNah&apos;s Gmail, so a copy of that email stays in its
          sent mail.
        </p>
        <p>
          <strong>Spam protection.</strong> The forms use Cloudflare Turnstile to check that you&apos;re a person (Cloudflare&apos;s privacy
          policy applies to that check). To stop floods of messages and votes, the website keeps a scrambled version of your IP address for one hour,
          then deletes it. Your real IP address is never stored.
        </p>
        <p>
          <strong>Hosting.</strong> The website runs on Vercel. Like any web host, Vercel may keep short-lived technical logs (such as IP
          address and browser) for security.
        </p>
        <p>
          <strong>No tracking.</strong> No analytics, no ads, no tracking cookies. If you pick light or dark mode, that choice is saved in
          your own browser only.
        </p>

        <h2>Your choices</h2>
        <ul>
          <li>The email box is optional. Leave it empty and your message is anonymous.</li>
          <li>Newsletter: use the unsubscribe link at the bottom of any issue; your address is deleted straight away.</li>
          <li>Idea votes: open a vote link, set every idea to 0 and save. That deletes your entry.</li>
          <li>
            Want your messages deleted? Send a note through the <Link href="/feedback">feedback form</Link> with the email address you
            used, and I&apos;ll delete them within 30 days.
          </li>
        </ul>

        <h2>Children</h2>
        <p>ScrollNah and this website are not aimed at children under 13. Please don&apos;t send personal details if you&apos;re under 13.</p>

        <h2>Changes and contact</h2>
        <p>
          If this page changes, the new version will be posted here with a new date. Questions? Use the{" "}
          <Link href="/feedback">feedback form</Link>.
        </p>
      </div></div>
    </>
  );
}
