"use client";
// A "Buy me a coffee" link for the header and footer. Not shown on /feedback: the Google Play version of the
// app opens that page, and nothing the Play app opens should show a donation link.
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { site } from "@/lib/site";

type Props = { className?: string; children: ReactNode; /** wrap the link in an <li> (for footer lists) */ li?: boolean };

export function CoffeeLink({ className, children, li }: Props) {
  const path = usePathname();
  if (!site.coffee || path?.startsWith("/feedback")) return null;
  const link = (
    <a className={className} href={site.coffee} title="Buy me a coffee" aria-label="Buy me a coffee">
      {children}
    </a>
  );
  return li ? <li>{link}</li> : link;
}
