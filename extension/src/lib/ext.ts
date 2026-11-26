// The browser's extension API: `browser` in Firefox, `chrome` in Chrome, Edge, Brave and Opera.
// Both return promises in Manifest V3, so the rest of the code just uses `ext`.

const g = globalThis as unknown as { browser?: Ext; chrome?: Ext };

export const ext: Ext = (g.browser ?? g.chrome) as Ext;

/** True in Firefox (its extension pages live at moz-extension://). */
export function isFirefox(): boolean {
  try {
    return ext.runtime.getURL("").startsWith("moz-extension:");
  } catch {
    return false;
  }
}
