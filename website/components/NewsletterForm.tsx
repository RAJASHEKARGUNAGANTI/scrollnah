"use client";
// The newsletter box on the home page.
import Link from "next/link";
import { EmailLinkForm } from "./EmailLinkForm";

export function NewsletterForm() {
  return (
    <EmailLinkForm
      endpoint="/api/subscribe"
      button="Sign me up"
      sentTitle="Almost there! Check your inbox."
      sentText={(email) => (
        <>I sent a confirmation email to {email}. Open it and press &quot;Yes, sign me up&quot;. Not there? Look in Spam or Promotions.</>
      )}
    >
      <p className="note">
        Only ScrollNah news. No tracking in the emails, and every one has an unsubscribe link. See <Link href="/privacy">privacy</Link>.
      </p>
    </EmailLinkForm>
  );
}
