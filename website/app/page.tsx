import Link from "next/link";
import { Icon, type IconName } from "@/components/Icon";
import { NewsletterForm } from "@/components/NewsletterForm";
import { IdeaVotes } from "@/components/IdeaVotes";
import { ScrollBuddy } from "@/components/ScrollBuddy";
import { YearCalculator } from "@/components/YearCalculator";
import { site } from "@/lib/site";

// ---------- Page text (edit freely) ----------
const whys: { icon: IconName; bg: string; title: string; text: string }[] = [
  { icon: "smile", bg: "bg-lime", title: "A buddy, not a nag", text: "Blobby melts if you overdo it. Bloomy grows when you don't. No guilt trips, no lectures." },
  { icon: "pause", bg: "bg-orange", title: "A pause, not a ban", text: "A slow breath before you reopen an app. You still decide, and you pick how strict it is." },
  { icon: "chart", bg: "bg-lilac", title: "See where your time goes", text: "A weekly report of your apps, your days and your busiest hours." },
  { icon: "lock", bg: "bg-sky", title: "Private and tiny", text: "No account, no ads, about 1.5 MB. ScrollNah has no internet access, so nothing leaves your phone." },
];

const features: { icon: IconName; bg: string; title: string; text: string }[] = [
  { icon: "timer", bg: "bg-lime", title: "Daily limits", text: "A budget for any app, from 5 minutes to 3 hours. Different limits for weekends, a limit on how often you open an app, and shared budgets for a group of apps." },
  { icon: "pause", bg: "bg-orange", title: "The Pause screen", text: "Over your limit? Your mascot asks \"Woah. Instagram again?\", breathes with you for 3 seconds, and asks why you're opening it." },
  { icon: "bell", bg: "bg-sky", title: "Break reminders", text: "Long stretch on one app? A nudge after 20 minutes (or your own time), as a notification, a mascot bubble, or both." },
  { icon: "flower", bg: "bg-pink", title: "Grow a garden", text: "Bloomy grows a flower for every week under your limits. A full pot of 4 flowers moves to your garden for good." },
  { icon: "chart", bg: "bg-lilac", title: "Your week, in detail", text: "Where the time went, your \"scroll o'clock\", whether the nudges worked, and week-in-review cards you can share." },
  { icon: "moon", bg: "bg-lime", title: "Focus, bedtime, work hours", text: "Lock your time-sinks for 30 minutes to 2 hours (also from Quick Settings), at night, or during work hours." },
  { icon: "widget", bg: "bg-orange", title: "Home-screen widgets", text: "Your mascot on your home screen, including an animated one that lives right on your wallpaper." },
  { icon: "shield", bg: "bg-sky", title: "Private by design", text: "No account, no ads, no analytics. Backups go to a file you pick, never to a server." },
];

const strictness = [
  { name: "Chill", bg: "bg-sky", text: "A gentle nudge when you hit your limit." },
  { name: "Firm", bg: "bg-lime", text: "The Pause screen: one slow breath, then 5 more minutes if you really need them." },
  { name: "Beast", bg: "bg-orange", text: "Locked until midnight. No extra minutes, no arguing." },
];

const shots = [
  { src: "shot-01-home", label: "Home: your mascot and what's left today" },
  { src: "shot-02-pause", label: "The Pause screen" },
  { src: "shot-03-break-reminder", label: "A break reminder" },
  { src: "shot-04-week", label: "Your week" },
  { src: "shot-05-bloomy", label: "Bloomy, growing" },
  { src: "shot-06-mascot", label: "Pick your buddy" },
  { src: "shot-07-garden", label: "Your garden of full pots" },
  { src: "shot-08-limits", label: "Limits and strictness" },
];

const steps = [
  { title: "Install it", text: "Free on Google Play, or as an APK from GitHub." },
  { title: "Pick your buddy", text: "Blobby the blob or Bloomy the plant." },
  { title: "Allow usage access", text: "So it can count your minutes. Everything stays on your phone." },
  { title: "Set daily limits", text: "For the apps that eat your day." },
  { title: "Choose how strict", text: "Chill, Firm or Beast. Change it any time." },
];

const comingToPlay: { icon: IconName; bg: string; title: string; text: string }[] = [
  { icon: "film", bg: "bg-pink", title: "Reels & Shorts counter", text: "Counts the Reels and Shorts you scroll, with a daily number and a little badge. Optional and off by default." },
  { icon: "hand", bg: "bg-lime", title: "Ask how long", text: "Before an app opens, your mascot asks: 5 or 10 minutes? Then it holds you to it." },
  { icon: "lock", bg: "bg-sky", title: "Parent lock", text: "A PIN, pattern or fingerprint that protects the limits on a child's phone." },
];

const browserFeatures: { icon: IconName; bg: string; title: string; text: string }[] = [
  { icon: "timer", bg: "bg-lime", title: "Daily limits per site", text: "YouTube, Instagram, Reddit or any site you add. A different limit for weekends, and visits a day if you like." },
  { icon: "smile", bg: "bg-orange", title: "Your mascot in the toolbar", text: "Blobby or Bloomy in today's mood, with the minutes left on the site you're on." },
  { icon: "pause", bg: "bg-pink", title: "The Pause page", text: "Chill sends a nudge, Firm pauses with 5 more minutes if you need them, Beast locks the site until midnight." },
  { icon: "film", bg: "bg-sky", title: "Hide the endless parts", text: "Switch off YouTube Shorts and the home feed, Instagram Reels and Explore, or X's \"For you\"." },
  { icon: "share", bg: "bg-lilac", title: "Bring your phone's limits", text: "Copy them from the app in one small file and drop it in. No account, nothing uploaded." },
  { icon: "shield", bg: "bg-lime", title: "Fair and private", text: "Only the tab you're looking at counts. It sees only the sites you add, and everything stays in your browser." },
];

// The extension's stores: a button for each one that's live (links in lib/site.ts), "coming soon" for the rest.
// Brave, Opera and Vivaldi install from the Chrome Web Store.
const browserStores = [
  { name: "Chrome", url: site.chromeStore, also: ["Brave"] },
  { name: "Edge", url: site.edgeStore, also: [] },
  { name: "Firefox", url: site.firefoxStore, also: [] },
];
const comingSoonStores = browserStores.filter((s) => !s.url).flatMap((s) => [s.name, ...s.also]);
function listNames(names: string[]) {
  return names.length < 2 ? names.join("") : `${names.slice(0, -1).join(", ")} and ${names[names.length - 1]}`;
}

const faqs = [
  {
    q: "Is ScrollNah really free?",
    a: ["Yes. No ads, no paid version, no account. If it helps you, you can buy me a coffee, but you never have to."],
  },
  {
    q: "What can ScrollNah see on my phone?",
    a: [
      "Only how many minutes you spend in the apps you choose to limit, through Android's Usage access. It can't read your messages, photos or what's on your screen.",
      "ScrollNah has no internet access at all, so nothing ever leaves your phone.",
    ],
  },
  {
    q: "Google Play or the GitHub version?",
    a: [
      "Both are the same app. New features land in the GitHub version first, then come to Google Play after Google's review.",
      "They can't update each other, so to switch you need to uninstall first. Save a backup in Settings > Data before you do, and restore it afterwards.",
    ],
  },
  {
    q: "How does the Reels & Shorts counter work?",
    a: [
      "It's optional and off by default. Android doesn't tell other apps when you're watching Reels or Shorts, so the counter uses Android's Accessibility service, only to spot those screens and count the videos. It saves only minutes and a number, on your phone.",
      "Some UPI and bank apps refuse to open while any Accessibility service is on. If that happens, switch the counter off.",
    ],
  },
  {
    q: "Will it drain my battery?",
    a: ["No. ScrollNah sleeps while the screen is off and only checks what's on screen while it needs to."],
  },
  {
    q: "Is there an iPhone version?",
    a: ["Not yet. ScrollNah is Android only for now (Android 8 and newer)."],
  },
];

export default function Home() {
  return (
    <>
      {/* ---------- Hero ---------- */}
      <section className="hero">
        <div className="wrap">
          <div>
            <span className="pill">Free · No ads · No account</span>
            <h1 style={{ marginTop: 18 }}>Scroll less, with a <span className="hl">little buddy</span>.</h1>
            <p className="lead">
              ScrollNah is a tiny Android app that helps you cut back on endless scrolling. Set a daily budget for the apps that eat
              your day, and Blobby or Bloomy reacts to every minute.
            </p>
            <div className="ctas">
              <a className="play-badge" href={site.playStore} aria-label="Get it on Google Play">
                <img src="/img/play-badge.png" alt="Get it on Google Play" width={160} height={62} />
              </a>
              <a className="btn" href={site.apk}><Icon name="download" /> Download the APK</a>
              {site.coffee && (
                <a className="coffee-mini" href={site.coffee} aria-label="Buy me a coffee" title="Buy me a coffee">
                  <Icon name="coffee" />
                </a>
              )}
            </div>
            <div className="facts">
              <span className="pill">Android 8 and newer</span>
              <span className="pill">About 1.5 MB</span>
              <span className="pill">Everything stays on your phone</span>
            </div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="phone"><img src="/img/shot-01-home.jpg" alt="" width={540} height={960} /></div>
            <img className="m1" src="/img/blobby_thriving.png" alt="" width={170} height={170} />
            <img className="m2" src="/img/bloomy_thriving4.png" alt="" width={175} height={175} />
          </div>
        </div>
      </section>

      {/* ---------- Why ---------- */}
      <section id="why" className="band">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">Why ScrollNah</span>
            <h2>A friend who notices, not a strict teacher.</h2>
            <p className="muted">Most screen-time apps make you feel bad. ScrollNah keeps it light, and still gets you to put the phone down.</p>
          </div>
          <div className="grid two">
            {whys.map((w) => (
              <div className="card why" key={w.title}>
                <div className={`tile ${w.bg}`}><Icon name={w.icon} /></div>
                <div><h3>{w.title}</h3><p className="muted">{w.text}</p></div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- Features ---------- */}
      <section id="features">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">Features</span>
            <h2>Everything it does</h2>
            <p className="muted">All of it runs on your phone.</p>
          </div>
          <div className="grid four">
            {features.map((f) => (
              <div className="card feature" key={f.title}>
                <div className={`tile ${f.bg}`}><Icon name={f.icon} /></div>
                <h3>{f.title}</h3>
                <p>{f.text}</p>
              </div>
            ))}
          </div>

          <h3 className="group-title" style={{ marginTop: 56 }}>How strict should it be?</h3>
          <div className="grid three">
            {strictness.map((s) => (
              <div className="card strict" key={s.name}>
                <span className={`tag ${s.bg}`}>{s.name}</span>
                <p>{s.text}</p>
              </div>
            ))}
          </div>

          <div className="shots" aria-label="Screenshots">
            {shots.map((s) => (
              <figure className="shot" key={s.src}>
                <img src={`/img/${s.src}.jpg`} alt={`ScrollNah screenshot: ${s.label}`} width={540} height={960} loading="lazy" />
                <figcaption>{s.label}</figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- How to use ---------- */}
      <section id="how" className="band">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">How it works</span>
            <h2>Set up in 2 minutes</h2>
          </div>
          <ol className="steps" style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {steps.map((s, i) => (
              <li className="card step" key={s.title}>
                <div className="num">{i + 1}</div>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
              </li>
            ))}
          </ol>
          <div className="ctas">
            <a className="btn lime" href={site.playStore}>Get it on Google Play</a>
          </div>
        </div>
      </section>

      {/* ---------- Calculator ---------- */}
      <section id="calc">
        <div className="wrap"><YearCalculator /></div>
      </section>

      {/* ---------- Browser extension ---------- */}
      <section id="browser">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">For your computer</span>
            <h2>ScrollNah for your browser</h2>
            <p className="muted">The same limits and the same mascot on your laptop, for Chrome, Edge, Brave, Opera and Firefox.</p>
          </div>
          <div className="grid three">
            {browserFeatures.map((f) => (
              <div className="card soon" key={f.title}>
                <div className={`tile ${f.bg}`}><Icon name={f.icon} /></div>
                <div><h3>{f.title}</h3><p>{f.text}</p></div>
              </div>
            ))}
          </div>
          <div className="ctas">
            {browserStores.filter((s) => s.url).map((s, i) => (
              <a key={s.name} className={`btn${i === 0 ? " lime" : ""}`} href={s.url}>Add to {s.name}</a>
            ))}
            {comingSoonStores.length ? <span className="badge">Coming soon to {listNames(comingSoonStores)}</span> : null}
            <Link href="/privacy#extension" className="muted">How it handles your data</Link>
          </div>
        </div>
      </section>

      {/* ---------- Coming soon ---------- */}
      <section id="soon" className="band">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">Coming soon</span>
            <h2>What&apos;s next</h2>
          </div>
          <h3 className="group-title">Coming to Google Play <span className="badge">Already in the GitHub version</span></h3>
          <div className="grid three">
            {comingToPlay.map((c) => (
              <div className="card soon" key={c.title}>
                <div className={`tile ${c.bg}`}><Icon name={c.icon} /></div>
                <div><h3>{c.title}</h3><p>{c.text}</p></div>
              </div>
            ))}
          </div>
          <h3 className="group-title" id="ideas" style={{ marginTop: 40 }}>On the idea list <span className="badge">Vote for what you want next</span></h3>
          <IdeaVotes />
        </div>
      </section>

      {/* ---------- Feedback ---------- */}
      <section id="feedback">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">Feedback</span>
            <h2>Tell me what you think</h2>
            <p className="muted">ScrollNah is made by one person, and your messages decide what comes next. No account needed.</p>
          </div>
          <div className="grid three">
            <div className="card fb">
              <div className="tile bg-lime"><Icon name="chat" /></div>
              <h3>Share feedback</h3>
              <p>What you like, what annoys you, or just how it&apos;s going.</p>
              <Link className="btn" href="/feedback">Send feedback</Link>
            </div>
            <div className="card fb">
              <div className="tile bg-lilac"><Icon name="bulb" /></div>
              <h3>Request a feature</h3>
              <p>Something you wish ScrollNah did? I read every idea.</p>
              <Link className="btn" href="/feedback?type=feature">Request a feature</Link>
            </div>
            <div className="card fb">
              <div className="tile bg-orange"><Icon name="bug" /></div>
              <h3>Report a bug</h3>
              <p>Something broken? Tell me what happened and on which phone, and I&apos;ll fix it.</p>
              <Link className="btn" href="/feedback?type=bug">Report a bug</Link>
            </div>
          </div>
        </div>
      </section>

      {/* ---------- Newsletter ---------- */}
      <section id="news" style={{ paddingTop: 0 }}>
        <div className="wrap">
          <div className="card panel news">
            <div>
              <h2>Get the news</h2>
              <p className="muted" style={{ marginTop: 10 }}>New versions, new mascots, and what&apos;s coming. About once a month, never spam.</p>
            </div>
            <NewsletterForm />
          </div>
        </div>
      </section>

      {/* ---------- Buy me a coffee ---------- */}
      <section id="support" style={{ paddingTop: 0 }}>
        <div className="wrap">
          <div className="card panel coffee">
            <div className="cup"><Icon name="coffee" /></div>
            <div>
              <h2>Buy me a coffee</h2>
              <p className="muted" style={{ marginTop: 8 }}>
                ScrollNah is free, with no ads and no tracking. If it gave you some time back, a coffee helps me keep building it.
              </p>
            </div>
            {site.coffee ? (
              <a className="btn" href={site.coffee}><Icon name="coffee" /> Buy me a coffee</a>
            ) : (
              <span className="btn" aria-disabled="true">Link coming soon</span>
            )}
          </div>
        </div>
      </section>

      {/* ---------- FAQ ---------- */}
      <section id="faq" className="band">
        <div className="wrap">
          <div className="section-head">
            <span className="eyebrow">Questions</span>
            <h2>Good to know</h2>
          </div>
          <div className="faq">
            {faqs.map((f) => (
              <details className="card" key={f.q}>
                <summary>{f.q}<Icon name="plus" /></summary>
                <div>{f.a.map((p) => <p key={p}>{p}</p>)}</div>
              </details>
            ))}
          </div>
        </div>
      </section>

      <ScrollBuddy />
    </>
  );
}
