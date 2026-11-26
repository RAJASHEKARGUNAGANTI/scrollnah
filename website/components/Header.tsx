import Link from "next/link";
import { CoffeeLink } from "./CoffeeLink";
import { Icon } from "./Icon";
import { ThemeToggle } from "./ThemeToggle";
import { site } from "@/lib/site";

const links = [
  { href: "/#features", label: "Features" },
  { href: "/#how", label: "How it works" },
  { href: "/#soon", label: "Coming soon" },
  { href: "/feedback", label: "Feedback" },
];

export function Header() {
  return (
    <header className="top">
      <div className="wrap">
        <Link className="brand" href="/" aria-label="ScrollNah home">
          <img src="/img/icon.svg" alt="" width={42} height={42} />
          <span>Scroll<span className="nah">Nah</span></span>
        </Link>
        <nav className="nav" aria-label="Main">
          {links.map((l) => <Link key={l.href} href={l.href}>{l.label}</Link>)}
        </nav>
        <div className="top-actions">
          <CoffeeLink className="round-btn coffee-btn"><Icon name="coffee" /></CoffeeLink>
          <ThemeToggle />
          <a className="btn lime get" href={site.playStore}>Get the app</a>
          {/* Small screens: a simple menu that works without JavaScript */}
          <details className="menu">
            <summary className="round-btn" aria-label="Menu"><Icon name="menu" /></summary>
            <nav className="card" aria-label="Main">
              {links.map((l) => <Link key={l.href} href={l.href}>{l.label}</Link>)}
              <a href={site.playStore}>Get it on Google Play</a>
              <CoffeeLink className="menu-coffee"><Icon name="coffee" /> Buy me a coffee</CoffeeLink>
            </nav>
          </details>
        </div>
      </div>
    </header>
  );
}
