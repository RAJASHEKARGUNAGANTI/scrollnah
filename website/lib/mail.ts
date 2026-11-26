// Emails the developer when a new message arrives (Gmail + an App password).
import nodemailer from "nodemailer";
import type { FeedbackInput } from "./feedback";

const TYPE_NAMES = { feedback: "Feedback", feature: "Feature request", bug: "Bug report" } as const;

/** The Gmail connection, or null when GMAIL_USER / GMAIL_APP_PASSWORD aren't set. */
function gmail() {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) return null;
  return { user, transport: nodemailer.createTransport({ service: "gmail", auth: { user, pass } }) };
}

export async function notifyNewFeedback(f: FeedbackInput): Promise<void> {
  const mail = gmail();
  if (!mail) return; // email not set up yet: the message is still saved in MongoDB
  const { user, transport } = mail;
  const firstLine = f.message.split("\n")[0].slice(0, 60);
  const lines = [
    `${TYPE_NAMES[f.type]} from the ScrollNah website`,
    "",
    f.message,
    f.steps ? `\nSteps:\n${f.steps}` : "",
    "",
    `App version: ${f.appVersion || "-"}   Installed from: ${f.source}`,
    `Phone: ${f.device || "-"}   Android: ${f.android || "-"}`,
    `Reply to: ${f.email || "(no email given)"}`,
  ];
  await transport.sendMail({
    from: `ScrollNah website <${user}>`,
    to: process.env.NOTIFY_EMAIL || user,
    replyTo: f.email || undefined,
    subject: `[ScrollNah] ${TYPE_NAMES[f.type]}: ${firstLine}`.replace(/[\r\n]+/g, " "),
    text: lines.join("\n"),
  });
}

/** The newsletter's "please confirm" email. Plain text: no images, no tracking. */
export async function sendConfirmEmail(to: string, confirmLink: string): Promise<void> {
  const mail = gmail();
  if (!mail) throw new Error("Gmail isn't set up");
  await mail.transport.sendMail({
    from: `ScrollNah <${mail.user}>`,
    to,
    subject: "Confirm your ScrollNah newsletter sign-up",
    text: [
      "Hi!",
      "",
      "Someone (hopefully you) asked for the ScrollNah newsletter at this address: new versions, new",
      "mascots and what's coming, about once a month.",
      "",
      "To confirm, open this link and press \"Yes, sign me up\":",
      confirmLink,
      "",
      "If it wasn't you, just ignore this email. Nothing else will be sent, and the request is deleted in 7 days.",
      "",
      "Raj, who makes ScrollNah",
      "https://scrollnah.vercel.app",
    ].join("\n"),
  });
}

/** The idea-vote link. Plain text, like the newsletter emails. The address isn't saved anywhere on the website. */
export async function sendVoteLinkEmail(to: string, voteLink: string, budget: number, maxPerIdea: number): Promise<void> {
  const mail = gmail();
  if (!mail) throw new Error("Gmail isn't set up");
  await mail.transport.sendMail({
    from: `ScrollNah <${mail.user}>`,
    to,
    subject: "Your ScrollNah vote link",
    text: [
      "Hi!",
      "",
      `Here's your link to vote on what ScrollNah gets next. You have ${budget} votes: put them on the ideas`,
      `you want most, up to ${maxPerIdea} on one idea.`,
      "",
      voteLink,
      "",
      "The link works for 7 days, and you can use it again to change your votes. Need one later? Ask for",
      "a new link on the website.",
      "",
      "If you didn't ask for this, just ignore this email. The website doesn't keep your address.",
      "",
      "Raj, who makes ScrollNah",
      "https://scrollnah.vercel.app",
    ].join("\n"),
  });
}
