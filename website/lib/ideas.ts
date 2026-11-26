// The ideas people can vote for on the home page ("On the idea list"). Add or remove ideas here;
// the id is what's counted in MongoDB (collection idea_votes), so don't change an id once it has votes.
import type { IconName } from "@/components/Icon";

export type Idea = { id: string; icon: IconName; bg: string; title: string; text: string };

export const IDEAS: Idea[] = [
  { id: "friend-challenges", icon: "users", bg: "bg-orange", title: "Friend challenges", text: "You vs a friend: who scrolls less this week?" },
  { id: "more-mascots", icon: "sparkle", bg: "bg-lilac", title: "More mascots", text: "A third buddy to pick from, with its own moods." },
  { id: "browser-extension", icon: "monitor", bg: "bg-sky", title: "ScrollNah for your browser", text: "Limits and a Pause screen for YouTube, Instagram and more on your computer." },
  { id: "weekly-tips", icon: "chart", bg: "bg-pink", title: "Weekly tips", text: "One short, personal tip each week, from your own numbers." },
];

export const IDEA_IDS = new Set(IDEAS.map((i) => i.id));
