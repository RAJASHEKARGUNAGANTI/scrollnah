// The parts of the browser extension API that ScrollNah uses, typed by hand so the extension needs no
// npm packages. Chrome calls the API `chrome`, Firefox `browser`; in Manifest V3 both return promises.
// Only used for type-checking (`npm run check`); nothing here ends up in the built extension.

type Listener<T extends (...args: any[]) => any> = {
  addListener(cb: T): void;
  removeListener(cb: T): void;
};

interface ExtTab {
  id?: number;
  windowId: number;
  active: boolean;
  audible?: boolean;
  incognito: boolean;
  /** Only filled in for sites ScrollNah has access to (no "tabs" permission). */
  url?: string;
  pendingUrl?: string;
}

interface ExtWindow {
  id?: number;
  focused: boolean;
  state?: string;
  type?: string;
}

interface ExtAlarm {
  name: string;
  scheduledTime: number;
}

interface ExtStorageArea {
  get(keys?: string | string[] | null): Promise<Record<string, any>>;
  set(items: Record<string, any>): Promise<void>;
  remove(keys: string | string[]): Promise<void>;
  clear(): Promise<void>;
}

interface ExtContentScript {
  id: string;
  matches: string[];
  js?: string[];
  css?: string[];
  runAt?: "document_start" | "document_end" | "document_idle";
  allFrames?: boolean;
  persistAcrossSessions?: boolean;
}

interface ExtMessageSender {
  tab?: ExtTab;
  id?: string;
  url?: string;
}

type ExtImageData = ImageData | Record<string, ImageData>;

interface Ext {
  runtime: {
    id: string;
    getURL(path: string): string;
    getManifest(): { version: string; name: string };
    openOptionsPage(): Promise<void>;
    sendMessage<T = any>(message: any): Promise<T>;
    onMessage: Listener<(message: any, sender: ExtMessageSender, sendResponse: (r?: any) => void) => boolean | void | Promise<any>>;
    onInstalled: Listener<(details: { reason: string; previousVersion?: string }) => void>;
    onStartup: Listener<() => void>;
  };
  storage: {
    local: ExtStorageArea;
    session: ExtStorageArea;
    onChanged: Listener<(changes: Record<string, { oldValue?: any; newValue?: any }>, area: string) => void>;
  };
  tabs: {
    query(info: { active?: boolean; windowId?: number; currentWindow?: boolean; lastFocusedWindow?: boolean }): Promise<ExtTab[]>;
    get(tabId: number): Promise<ExtTab>;
    getCurrent(): Promise<ExtTab | undefined>;
    update(tabId: number, props: { url?: string; active?: boolean }): Promise<ExtTab>;
    create(props: { url?: string; windowId?: number; active?: boolean }): Promise<ExtTab>;
    remove(tabId: number): Promise<void>;
    onActivated: Listener<(info: { tabId: number; windowId: number }) => void>;
    onUpdated: Listener<(tabId: number, change: { url?: string; status?: string; audible?: boolean }, tab: ExtTab) => void>;
    onRemoved: Listener<(tabId: number) => void>;
  };
  windows: {
    WINDOW_ID_NONE: number;
    getLastFocused(info?: { populate?: boolean }): Promise<ExtWindow>;
    onFocusChanged: Listener<(windowId: number) => void>;
  };
  idle: {
    queryState(seconds: number): Promise<"active" | "idle" | "locked">;
    setDetectionInterval(seconds: number): void;
    onStateChanged: Listener<(state: "active" | "idle" | "locked") => void>;
  };
  alarms: {
    create(name: string, info: { when?: number; delayInMinutes?: number; periodInMinutes?: number }): Promise<void> | void;
    clear(name: string): Promise<boolean>;
    get(name: string): Promise<ExtAlarm | undefined>;
    onAlarm: Listener<(alarm: ExtAlarm) => void>;
  };
  action: {
    setIcon(details: { imageData?: ExtImageData; path?: string | Record<string, string>; tabId?: number }): Promise<void>;
    setBadgeText(details: { text: string; tabId?: number }): Promise<void>;
    setBadgeBackgroundColor(details: { color: string; tabId?: number }): Promise<void>;
    setBadgeTextColor?(details: { color: string; tabId?: number }): Promise<void>;
    setTitle(details: { title: string; tabId?: number }): Promise<void>;
  };
  notifications: {
    create(id: string, options: { type: "basic"; iconUrl: string; title: string; message: string; priority?: number }): Promise<string>;
    clear(id: string): Promise<boolean>;
    onClicked: Listener<(id: string) => void>;
  };
  permissions: {
    request(p: { origins?: string[]; permissions?: string[] }): Promise<boolean>;
    contains(p: { origins?: string[]; permissions?: string[] }): Promise<boolean>;
    getAll(): Promise<{ origins?: string[]; permissions?: string[] }>;
    remove(p: { origins?: string[] }): Promise<boolean>;
    onAdded: Listener<(p: { origins?: string[] }) => void>;
    onRemoved: Listener<(p: { origins?: string[] }) => void>;
  };
  scripting: {
    registerContentScripts(scripts: ExtContentScript[]): Promise<void>;
    unregisterContentScripts(filter?: { ids?: string[] }): Promise<void>;
    getRegisteredContentScripts(filter?: { ids?: string[] }): Promise<ExtContentScript[]>;
    executeScript(injection: { target: { tabId: number }; files: string[] }): Promise<unknown[]>;
  };
  extension: {
    isAllowedIncognitoAccess(): Promise<boolean>;
  };
}
