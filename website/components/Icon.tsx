// Simple 2px stroke icons (like the app's: no emoji), drawn on a 24x24 grid.
const paths: Record<string, React.ReactNode> = {
  smile: (<><circle cx="12" cy="12" r="9" /><path d="M8.5 14c1 1.5 2.2 2.2 3.5 2.2s2.5-.7 3.5-2.2" /><path d="M9 9.5v1M15 9.5v1" /></>),
  pause: (<><circle cx="12" cy="12" r="9" /><path d="M10 9v6M14 9v6" /></>),
  chart: (<><path d="M4 20h16" /><path d="M7 16v-4M12 16V7M17 16v-6" /></>),
  lock: (<><rect x="5" y="11" width="14" height="9" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></>),
  timer: (<><circle cx="12" cy="13" r="8" /><path d="M12 9v4l2.5 2.5M9.5 2.5h5" /></>),
  bell: (<><path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z" /><path d="M10 20.5a2 2 0 0 0 4 0" /></>),
  flower: (<><circle cx="12" cy="8" r="2.2" /><path d="M12 5.8c0-2 1.2-3 2.4-2.6 1.1.4 1.1 2-.2 3M9.8 8c-2 0-3-1.2-2.6-2.4.4-1.1 2-1.1 3 .2M14.2 8c2 0 3 1.2 2.6 2.4-.4 1.1-2 1.1-3-.2M12 10.2c0 2-1.2 3-2.4 2.6" /><path d="M12 10.5V21M12 17c-1.5-2-3.5-2.4-5-2M12 15c1.5-1.6 3.2-2 4.5-1.6" /></>),
  moon: (<path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z" />),
  sun: (<><circle cx="12" cy="12" r="4" /><path d="M12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4" /></>),
  widget: (<><rect x="4" y="4" width="7" height="7" rx="2" /><rect x="13" y="4" width="7" height="7" rx="2" /><rect x="4" y="13" width="7" height="7" rx="2" /><rect x="13" y="13" width="7" height="7" rx="2" /></>),
  film: (<><rect x="4" y="3" width="16" height="18" rx="3" /><path d="M10 9.5v5l4-2.5z" /></>),
  clock: (<><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>),
  shield: (<><path d="M12 3 5 6v5c0 4.5 3 8 7 10 4-2 7-5.5 7-10V6z" /><path d="m9 12 2 2 4-4" /></>),
  users: (<><circle cx="9" cy="9" r="3.2" /><path d="M3.5 19c.6-3 2.8-4.6 5.5-4.6s4.9 1.6 5.5 4.6" /><circle cx="17" cy="8.5" r="2.5" /><path d="M16 13.6c2.4 0 4 1.4 4.5 4" /></>),
  sparkle: (<path d="M12 3c.6 4 2 5.4 6 6-4 .6-5.4 2-6 6-.6-4-2-5.4-6-6 4-.6 5.4-2 6-6zM19 15c.3 1.8.9 2.4 2.5 2.7-1.6.3-2.2.9-2.5 2.6-.3-1.7-.9-2.3-2.5-2.6 1.6-.3 2.2-.9 2.5-2.7z" />),
  hand: (<><path d="M8 11V5.5a1.5 1.5 0 0 1 3 0V11" /><path d="M11 10V4.5a1.5 1.5 0 0 1 3 0V10" /><path d="M14 10V6a1.5 1.5 0 0 1 3 0v7c0 4-2.5 7-6.5 7-3 0-4.5-1.5-6-4.5L3 13.2a1.5 1.5 0 0 1 2.4-1.7L8 14" /></>),
  chat: (<path d="M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7l-4.5 3.5V17H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z" />),
  bulb: (<><path d="M9 18h6M10 21h4" /><path d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" /></>),
  bug: (<><rect x="7" y="8" width="10" height="12" rx="5" /><path d="M12 12v8M9 5.5 10 8M15 5.5 14 8M3 13h4M17 13h4M4 8.5 7 10M20 8.5 17 10M4 18.5 7 17M20 18.5 17 17" /></>),
  coffee: (<><path d="M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z" /><path d="M16 10.5h1.5a2.5 2.5 0 0 1 0 5H15.5" /><path d="M8 3.5c-.8 1 .8 2-.1 3M11.5 3.5c-.8 1 .8 2-.1 3" /></>),
  mail: (<><rect x="3" y="5" width="18" height="14" rx="3" /><path d="m4 7 8 6 8-6" /></>),
  download: (<><path d="M12 4v11M7.5 10.5 12 15l4.5-4.5" /><path d="M5 19.5h14" /></>),
  plus: (<path d="M12 5v14M5 12h14" />),
  minus: (<path d="M5 12h14" />),
  monitor: (<><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></>),
  share: (<><path d="M12 15V4M7.5 8.5 12 4l4.5 4.5" /><path d="M5 13v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5" /></>),
  menu: (<path d="M4 7h16M4 12h16M4 17h16" />),
  arrow: (<path d="M5 12h14M13 6l6 6-6 6" />),
  check: (<path d="m5 12.5 4.5 4.5L19 7.5" />),
  youtube: (<><rect x="2.5" y="5.5" width="19" height="13" rx="4" /><path d="M10 9.2v5.6l4.8-2.8z" /></>),
  instagram: (<><rect x="3.5" y="3.5" width="17" height="17" rx="5" /><circle cx="12" cy="12" r="4" /><path d="M17.2 6.8h.01" /></>),
  github: (<><path d="M9 19c-4 1.3-4-2-5.5-2.5M14.5 21v-3.2c0-1 .1-1.5-.5-2.1 2.6-.3 5.2-1.3 5.2-5.7a4.4 4.4 0 0 0-1.2-3.1 4.1 4.1 0 0 0-.1-3.1s-1-.3-3.2 1.2a11 11 0 0 0-5.8 0C6.7 3.5 5.7 3.8 5.7 3.8a4.1 4.1 0 0 0-.1 3.1 4.4 4.4 0 0 0-1.2 3.1c0 4.4 2.6 5.4 5.2 5.7-.6.6-.6 1.2-.5 2.1V21" /></>),
  linkedin: (<><rect x="3.5" y="3.5" width="17" height="17" rx="4" /><path d="M8 10.5V16M8 7.5v.01M11.5 16v-5.5M11.5 13c0-1.6 1-2.6 2.3-2.6S16 11.2 16 13v3" /></>),
};

export type IconName = keyof typeof paths;

export function Icon({ name, className = "icon", label }: { name: IconName; className?: string; label?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden={label ? undefined : true} role={label ? "img" : undefined} aria-label={label}>
      {paths[name]}
    </svg>
  );
}
