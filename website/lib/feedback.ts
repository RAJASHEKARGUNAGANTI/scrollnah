// What the feedback form may send, checked on the server (never trust the browser).
import { z } from "zod";

const optionalText = (max: number) => z.string().trim().max(max).optional().default("");

export const FeedbackSchema = z.object({
  type: z.enum(["feedback", "feature", "bug"]),
  message: z.string().trim().min(3, "Please write a few words.").max(5000, "That's a bit long (5000 characters max)."),
  steps: optionalText(3000),
  email: z.union([z.literal(""), z.email("That email doesn't look right.").max(200)]).optional().default(""),
  source: z.enum(["play", "github", "unknown"]).optional().default("unknown"),
  appVersion: optionalText(40),
  android: optionalText(40),
  device: optionalText(80),
  website: z.string().optional().default(""), // hidden trap field: people never fill it in, bots do
  turnstileToken: z.string().max(4096).optional().default(""),
});

export type FeedbackInput = z.infer<typeof FeedbackSchema>;
export type FeedbackType = FeedbackInput["type"];
