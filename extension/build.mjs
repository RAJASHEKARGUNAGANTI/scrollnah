// Builds ScrollNah for browsers, with no npm packages: Node's own TypeScript stripping turns each .ts
// file into .js (types removed, nothing else changed), then the pages, fonts and icons are copied and a
// manifest is written for each browser.
//
//   node build.mjs           -> dist/chrome, dist/firefox and a .zip of each (for the stores)
//   node build.mjs --test    -> also grants the popular sites up front (for automated tests only)
//   node build.mjs --source  -> also dist/scrollnah-source-<version>.zip (Firefox Add-ons asks for the source code)

import { execFileSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import { stripTypeScriptTypes } from "node:module";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

process.removeAllListeners("warning"); // stripTypeScriptTypes is marked experimental; that's fine here

const root = dirname(fileURLToPath(import.meta.url));
const src = join(root, "src");
const dist = join(root, "dist");
const pkg = JSON.parse(readFileSync(join(root, "package.json"), "utf8"));
const testBuild = process.argv.includes("--test");

/** The Firefox add-on id. Never change it after the first upload. */
const GECKO_ID = "{944b83b9-1b7c-4e38-80a8-0660716da1d1}";

const base = {
  manifest_version: 3,
  name: "ScrollNah: Scroll Less",
  short_name: "ScrollNah",
  version: pkg.version,
  description: "Daily limits for YouTube, Instagram and other sites, with a cute mascot that reacts. Everything stays in your browser.",
  homepage_url: "https://scrollnah.vercel.app",
  icons: { 16: "icons/icon-16.png", 32: "icons/icon-32.png", 48: "icons/icon-48.png", 128: "icons/icon-128.png" },
  action: {
    default_title: "ScrollNah",
    default_popup: "pages/popup.html",
    default_icon: { 16: "icons/icon-16.png", 32: "icons/icon-32.png" },
  },
  options_ui: { page: "pages/options.html", open_in_tab: true },
  // storage: settings and daily minutes. alarms: check again when a limit is reached. idle: don't count
  // time when you're away. notifications: Chill mode's nudge. scripting: the in-page part, only on your sites.
  permissions: ["storage", "alarms", "idle", "notifications", "scripting"],
  // Sites are asked for one by one, only the ones you add (no "read all your data" at install).
  optional_host_permissions: ["*://*/*"],
};

const POPULAR = ["youtube.com", "youtu.be", "instagram.com", "x.com", "twitter.com", "reddit.com", "facebook.com", "tiktok.com"];

function manifest(target) {
  const m = structuredClone(base);
  if (target === "chrome") {
    m.background = { service_worker: "background.js", type: "module" };
    m.minimum_chrome_version = "116";
  } else {
    m.background = { scripts: ["background.js"], type: "module" };
    m.browser_specific_settings = {
      gecko: { id: GECKO_ID, strict_min_version: "140.0", data_collection_permissions: { required: ["none"] } },
    };
  }
  if (testBuild) m.host_permissions = POPULAR.flatMap((h) => [`*://${h}/*`, `*://*.${h}/*`]);
  return m;
}

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name);
    return statSync(p).isDirectory() ? walk(p) : [p];
  });
}

/** One .ts file -> .js: types stripped, and imports of "./x.ts" pointed at "./x.js". */
function compile(file) {
  const js = stripTypeScriptTypes(readFileSync(file, "utf8"), { mode: "strip" });
  return js.replace(/((?:from|import)\s*["'])(\.{1,2}\/[^"']+)\.ts(["'])/g, "$1$2.js$3");
}

function build(target) {
  const out = join(dist, target);
  rmSync(out, { recursive: true, force: true });
  mkdirSync(out, { recursive: true });
  for (const file of walk(src)) {
    const rel = relative(src, file);
    if (rel.startsWith("types")) continue; // type declarations only
    const dest = join(out, rel.replace(/\.ts$/, ".js"));
    mkdirSync(dirname(dest), { recursive: true });
    if (file.endsWith(".ts")) writeFileSync(dest, compile(file));
    else cpSync(file, dest);
  }
  writeFileSync(join(out, "manifest.json"), JSON.stringify(manifest(target), null, 2) + "\n");

  // Safety check: the content script can't use imports (browsers run it as a plain script).
  const content = readFileSync(join(out, "content.js"), "utf8");
  if (/^\s*(import|export)\s/m.test(content)) throw new Error("content.ts must not import or export anything (only `import type`).");

  if (!testBuild) {
    const zip = join(dist, `scrollnah-${target}-${pkg.version}.zip`);
    rmSync(zip, { force: true });
    execFileSync("zip", ["-q", "-r", "-X", zip, "."], { cwd: out });
    console.log(`${target}: ${relative(root, out)}  (${(statSync(zip).size / 1024).toFixed(0)} KB zip)`);
  } else {
    console.log(`${target}: ${relative(root, out)}  (test build: popular sites granted up front)`);
  }
}

if (!existsSync(join(src, "icons", "icon-128.png"))) throw new Error("Missing src/icons (see README: Icons).");
mkdirSync(dist, { recursive: true });
build("chrome");
build("firefox");

if (process.argv.includes("--source")) {
  // Everything needed to rebuild the packages: the extension folder without the builds.
  const zip = join(dist, `scrollnah-source-${pkg.version}.zip`);
  rmSync(zip, { force: true });
  execFileSync("zip", ["-q", "-r", "-X", zip, ".", "-x", "dist/*", "-x", "*.DS_Store", "-x", "node_modules/*"], { cwd: root });
  console.log(`source: ${relative(root, zip)}  (${(statSync(zip).size / 1024).toFixed(0)} KB, for Firefox Add-ons)`);
}
