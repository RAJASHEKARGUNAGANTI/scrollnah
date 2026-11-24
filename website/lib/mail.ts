// Emails the developer when a new message arrives (Gmail + an App password).
import nodemailer from "nodemailer";
import type { FeedbackInput } from "./feedback";

const TYPE_NAMES = { feedback: "Feedback", feature: "Feature request", bug: "Bug report" } as const;

export async function notifyNewFeedback(f: FeedbackInput): Promise<void> {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) return; // email not set up yet: the message is still saved in MongoDB

  const transport = nodemailer.createTransport({ service: "gmail", auth: { user, pass } });
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
