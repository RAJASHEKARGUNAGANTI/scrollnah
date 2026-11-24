import Link from "next/link";
import { Icon } from "./Icon";
import { site } from "@/lib/site";

export function Footer() {
  const socials = [
    { href: site.youtube, name: "youtube" as const, label: "ScrollNah on YouTube" },
    { href: site.instagram, name: "instagram" as const, label: "Instagram" },
    { href: site.github, name: "github" as const, label: "Source code on GitHub" },
    { href: site.linkedin, name: "linkedin" as const, label: "LinkedIn" },
  ].filter((s) => s.href);

  return (
    <footer>
      <div className="wrap">
        <div>
          <Link className="brand" href="/">
            <img src="/img/icon.svg" alt="" width={42} height={42} />
            <span>Scroll<span className="nah">Nah</span></span>
          </Link>
          <p className="small">Made by Raj, as idea #1 of ME and MY THETA: ideas from a notes app, turned into real things.</p>
          <div className="socials">
            {socials.map((s) => (
              <a key={s.name} href={s.href} aria-label={s.label} title={s.label}><Icon name={s.name} /></a>
            ))}
          </div>
        </div>
        <div>
          <h3>ScrollNah</h3>
          <ul>
            <li><Link href="/#features">Features</Link></li>
            <li><Link href="/#how">How to use it</Link></li>
            <li><Link href="/#soon">Coming soon</Link></li>
            <li><Link href="/#faq">Questions</Link></li>
          </ul>
        </div>
        <div>
          <h3>Get in touch</h3>
          <ul>
            <li><Link href="/feedback">Send feedback</Link></li>
            <li><Link href="/feedback?type=feature">Request a feature</Link></li>
            <li><Link href="/feedback?type=bug">Report a bug</Link></li>
            <li><Link href="/privacy">Privacy</Link></li>
            <li><a href={site.github}>Source code (MIT)</a></li>
          </ul>
        </div>
      </div>
      <div className="wrap">
        <p className="small">ScrollNah is not affiliated with Instagram, YouTube or any other app it helps you limit.</p>
      </div>
    </footer>
  );
}
