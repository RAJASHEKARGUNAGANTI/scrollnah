# ScrollNah website

The ScrollNah website: what the app is, features, how to use it, what's coming, a feedback form
(feedback, feature requests, bug reports), Buy Me a Coffee and links to the socials.

Built with **Next.js (App Router) + TypeScript**, hosted on **Vercel**. The feedback form saves to
**MongoDB Atlas** and emails you with **Gmail** (Nodemailer). **Cloudflare Turnstile** keeps bots out.
All free to start.

## Where things are

| What | File |
|---|---|
| Links (Play, GitHub, socials, **Buy Me a Coffee**, LinkedIn) | `lib/site.ts` |
| Home page text (features, steps, coming soon, questions) | `app/page.tsx` (lists at the top) |
| Feedback page and form | `app/feedback/page.tsx`, `components/FeedbackForm.tsx` |
| What the form saves (server) | `app/api/feedback/route.ts`, `lib/feedback.ts` |
| Database, email, spam checks | `lib/db.ts`, `lib/mail.ts`, `lib/spam.ts` |
| Privacy page | `app/privacy/page.tsx` |
| Colours, fonts, styles | `app/globals.css` (same colours as the app) |
| Mascot art and screenshots | `public/img/` |

## Run it on your Mac

```bash
cd website
npm install
cp .env.example .env.local   # then fill in the values (see below)
npm run dev                  # open http://localhost:3000
```

Without `.env.local` the site still works; the form just says "isn't switched on yet".
`npm run build` checks everything (TypeScript included) before you deploy.

## Set up the accounts (once)

Secrets go only in `.env.local` (on your Mac) and in Vercel's settings. Never in the code, never on GitHub.

### 1. MongoDB Atlas (the database)
1. Sign up at mongodb.com/atlas and create a **Free** cluster. Pick **AWS, Mumbai (ap-south-1)**.
2. **Database Access** > Add New Database User: a username, "Autogenerate Secure Password" (copy it),
   role "Read and write to any database".
3. **Network Access** > Add IP Address > **Allow access from anywhere** (`0.0.0.0/0`). Vercel's
   addresses change all the time, so this is needed; the strong password protects it.
4. **Connect** > Drivers > copy the connection string, put your password in place of `<db_password>`,
   and paste it into `.env.local` as `MONGODB_URI=...`.

Reading messages: Atlas > **Browse Collections** > `scrollnah` > `feedback`. Change `status` from `new` to
`seen` or `done` as you go. Messages delete themselves after 2 years (as the privacy page says).

### 2. Gmail (an email for every new message)
1. Best: make a separate Gmail just for ScrollNah.
2. Google Account > Security > turn on **2-Step Verification**.
3. Search "App passwords" in your Google Account, create one called "ScrollNah website", copy the 16 letters.
4. In `.env.local`: `GMAIL_USER=` that Gmail, `GMAIL_APP_PASSWORD=` the 16 letters (no spaces),
   and optionally `NOTIFY_EMAIL=` where you want the emails to arrive.

### 3. Cloudflare Turnstile (spam check)
1. Cloudflare dashboard (free account) > **Turnstile** > Add widget.
2. Hostnames: `localhost` and your Vercel address (e.g. `scrollnah.vercel.app`). Mode: Managed.
3. Copy the **Site key** to `NEXT_PUBLIC_TURNSTILE_SITE_KEY` and the **Secret key** to `TURNSTILE_SECRET_KEY`.

Also set `HASH_SALT` to any long random text.

### 4. Vercel (hosting)
The `website/` folder has to be on GitHub first (commit and push it).
1. vercel.com > Add New > Project > import the `scrollnah` repository.
2. **Root Directory: `website`**. Framework: Next.js (found automatically).
3. **Environment Variables**: add every name from `.env.example` with your values.
4. Deploy. Your site gets an address like `scrollnah.vercel.app`; add it to Turnstile's hostnames.
5. Tip: Project > Settings > Functions > region **Mumbai (bom1)**, next to the database.

`vercel.json` skips a new build when a push didn't change anything in `website/`.

Before launch: Vercel's free plan is for non-commercial use, and a Buy Me a Coffee link is a grey area.
Ask Vercel Support in one line whether a donation link is fine on Hobby.

## Privacy rules for this site
- No analytics, no ads, no tracking cookies. Fonts and images are served from this site.
- The form keeps only what people type; the email box is optional. Real IP addresses are never stored
  (a scrambled version is kept for 1 hour for the spam limit).
- `/feedback` has no Buy Me a Coffee link on purpose: the Google Play version of the app links there.
- If you add anything that collects data, update `app/privacy/page.tsx` first.
