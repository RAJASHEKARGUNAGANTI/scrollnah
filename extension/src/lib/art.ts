// Blobby and Bloomy, drawn with canvas shapes: a straight port of the phone app's drawing code
// (app/.../ui/mascot/BlobbyArt.kt, BloomyArt.kt, MascotParts.kt), so they look the same everywhere.
// Everything is drawn on a 140x140 grid; [s] scales it to the real size. Works on a page's canvas and
// on an OffscreenCanvas in the background (for the toolbar icon and notification pictures).

import type { MascotId } from "./settings.ts";
import type { Mood } from "./rules.ts";

type Ctx = CanvasRenderingContext2D | OffscreenCanvasRenderingContext2D;
type Pt = [number, number];

export const COLORS = {
  ink: "#16131F",
  lime: "#C6F432",
  orange: "#FF6B35",
  lilac: "#B9A8FF",
  pink: "#FF9EB5",
  white: "#FFFFFF",
  sky: "#7DE2FF",
  paleLeaf: "#E4F7A8",
  palePetal: "#F7D3DC",
  dryPetal: "#D8B4A0",
  brown: "#A7783C",
  soil: "#8A5A2B",
};
const C = COLORS;

/** Small animation values. The defaults are a still pose. */
export interface Pose {
  eyeOpen: number; // 1 = open, 0.1 = mid-blink
  glance: number; // -1..1, eyes look left/right (okay mood)
  sway: number; // -1..1, Bloomy's stems sway
  twinkle: number; // sparkle size
  asleep: boolean;
}
export const STILL: Pose = { eyeOpen: 1, glance: 0, sway: 0, twinkle: 1, asleep: false };

export interface MascotDrawing {
  mascot: MascotId;
  mood: Mood;
  flowers?: number; // Bloomy: 0 = sprout, 1-4 flowers
  budDays?: number; // Bloomy: days toward the next flower (0-6)
  onOrange?: boolean; // on an orange page Blobby turns lime and Bloomy's pot lilac, so they stay visible
  pose?: Partial<Pose>;
}

/** Draws the mascot into a [size] x [size] square at the canvas origin. */
export function drawMascot(ctx: Ctx, size: number, d: MascotDrawing): void {
  const s = size / 140;
  const pose = { ...STILL, ...d.pose };
  ctx.save();
  if (d.mascot === "bloomy") drawBloomy(ctx, s, d.mood, d.flowers ?? 3, !!d.onOrange, pose, d.budDays ?? 0);
  else drawBlobby(ctx, s, d.mood, !!d.onOrange, pose);
  if (pose.asleep) sleepyZs(ctx, s);
  ctx.restore();
}

// ---- Drawing helpers (Compose's drawPath / drawCircle / drawOval / drawLine) ----

function fill(ctx: Ctx, path: Path2D, color: string) {
  ctx.fillStyle = color;
  ctx.fill(path);
}

function stroke(ctx: Ctx, path: Path2D, color: string, width: number, cap: CanvasLineCap = "butt", join: CanvasLineJoin = "miter") {
  ctx.strokeStyle = color;
  ctx.lineWidth = width;
  ctx.lineCap = cap;
  ctx.lineJoin = join;
  ctx.stroke(path);
}

/** Fills a shape and outlines it in ink (the neo-brutalist look). */
function outlined(ctx: Ctx, path: Path2D, color: string, s: number, width = 2.5) {
  fill(ctx, path, color);
  stroke(ctx, path, C.ink, width * s, "butt", "round");
}

function circle(cx: number, cy: number, r: number): Path2D {
  const p = new Path2D();
  p.arc(cx, cy, Math.max(r, 0.01), 0, Math.PI * 2);
  return p;
}

function oval(cx: number, cy: number, rx: number, ry: number): Path2D {
  const p = new Path2D();
  p.ellipse(cx, cy, Math.max(rx, 0.01), Math.max(ry, 0.01), 0, 0, Math.PI * 2);
  return p;
}

function line(ctx: Ctx, a: Pt, b: Pt, color: string, width: number) {
  const p = new Path2D();
  p.moveTo(a[0], a[1]);
  p.lineTo(b[0], b[1]);
  stroke(ctx, p, color, width, "round");
}

// ---- Shared parts: faces, drops, sparkles ----

/** A sweat drop or tear (pointy end up) centred on (cx, cy). */
function drop(ctx: Ctx, cx: number, cy: number, r: number, s: number) {
  const p = new Path2D();
  p.moveTo(cx * s, (cy - 2.2 * r) * s);
  p.quadraticCurveTo((cx + 1.1 * r) * s, (cy - 0.6 * r) * s, (cx + r) * s, cy * s);
  p.bezierCurveTo((cx + r) * s, (cy + 1.35 * r) * s, (cx - r) * s, (cy + 1.35 * r) * s, (cx - r) * s, cy * s);
  p.quadraticCurveTo((cx - 1.1 * r) * s, (cy - 0.6 * r) * s, cx * s, (cy - 2.2 * r) * s);
  p.closePath();
  outlined(ctx, p, C.sky, s, 1.8);
}

/** A four-point sparkle (thriving). */
function sparkle(ctx: Ctx, cx: number, cy: number, r: number, s: number) {
  const p = new Path2D();
  p.moveTo(cx * s, (cy - r) * s);
  p.quadraticCurveTo(cx * s, cy * s, (cx + r) * s, cy * s);
  p.quadraticCurveTo(cx * s, cy * s, cx * s, (cy + r) * s);
  p.quadraticCurveTo(cx * s, cy * s, (cx - r) * s, cy * s);
  p.quadraticCurveTo(cx * s, cy * s, cx * s, (cy - r) * s);
  p.closePath();
  outlined(ctx, p, C.white, s, 1.6);
}

/** Two floating "z"s above a sleeping mascot. */
function sleepyZs(ctx: Ctx, s: number) {
  const zee = (x: number, y: number, size: number) => {
    const p = new Path2D();
    p.moveTo(x * s, y * s);
    p.lineTo((x + size) * s, y * s);
    p.lineTo(x * s, (y + size) * s);
    p.lineTo((x + size) * s, (y + size) * s);
    stroke(ctx, p, C.ink, 3.2 * s, "round", "round");
  };
  zee(104, 22, 13);
  zee(122, 4, 9);
}

/**
 * Eyes, brows, mouth and cheeks for a mood. (cx, cy) is the centre of the face and [k] its size:
 * 1 = Blobby's face, 0.5 = the face on Bloomy's pot.
 */
function face(ctx: Ctx, mood: Mood, cx: number, cy: number, k: number, s: number, pose: Pose) {
  const p = (x: number, y: number): Pt => [(cx + x * k) * s, (cy + y * k) * s];
  const lineWidth = 4 * k * s;
  const strokeLine = (path: Path2D) => stroke(ctx, path, C.ink, lineWidth, "round", "round");
  const curve = (from: Pt, ...quads: [Pt, Pt][]) => {
    const path = new Path2D();
    path.moveTo(from[0], from[1]);
    for (const [c, e] of quads) path.quadraticCurveTo(c[0], c[1], e[0], e[1]);
    return path;
  };
  const eye = (x: number, y: number, w: number, h: number, shine: boolean) => {
    const eh = h * pose.eyeOpen; // blinking squashes the eye
    const [ex, ey] = p(x, y);
    fill(ctx, oval(ex, ey, (w * k * s) / 2, (eh * k * s) / 2), C.ink);
    if (shine && pose.eyeOpen > 0.5) {
      const [sx, sy] = p(x + 2, y - 4);
      fill(ctx, circle(sx, sy, 2.5 * k * s), C.white);
    }
  };
  const cheeks = (y: number, r: number) => {
    for (const x of [-32, 32]) {
      const [px, py] = p(x, y);
      fill(ctx, circle(px, py, r * k * s), C.pink);
    }
  };

  if (pose.asleep) {
    const closedEye = (x: number, y: number, w: number) => strokeLine(curve(p(x - w / 2, y), [p(x, y + w * 0.55), p(x + w / 2, y)]));
    closedEye(-18, -10, 14);
    closedEye(18, -10, 14);
    cheeks(8, 7);
    strokeLine(curve(p(-7, 15), [p(0, 21), p(7, 15)]));
    return;
  }

  switch (mood) {
    case 1: {
      eye(-18, -12, 14, 20, true);
      eye(18, -12, 14, 20, true);
      cheeks(8, 7);
      // Big open grin with a little tongue.
      const grin = curve(p(-17, 4), [p(0, 32), p(17, 4)]);
      grin.closePath();
      fill(ctx, grin, C.ink);
      const [tx, ty] = p(0, 15);
      fill(ctx, oval(tx, ty, 7 * k * s, 3 * k * s), C.pink);
      break;
    }
    case 2:
      eye(-18, -12, 14, 20, true);
      eye(18, -12, 14, 20, true);
      cheeks(8, 7);
      strokeLine(curve(p(-16, 12), [p(0, 30), p(16, 12)]));
      break;
    case 3: {
      const look = pose.glance * 4;
      eye(-18 + look, -10, 13, 15, false);
      eye(18 + look, -10, 13, 15, false);
      cheeks(10, 5.5);
      line(ctx, p(-12, 18), p(12, 18), C.ink, lineWidth);
      break;
    }
    case 4:
      // Raised brows (inner ends up), wobbly mouth.
      line(ctx, p(-27, -27), p(-11, -33), C.ink, 3.5 * k * s);
      line(ctx, p(11, -33), p(27, -27), C.ink, 3.5 * k * s);
      eye(-16, -8, 12, 16, false);
      eye(16, -8, 12, 16, false);
      cheeks(10, 5.5);
      strokeLine(curve(p(-14, 22), [p(-7, 15), p(0, 22)], [p(7, 29), p(14, 22)]));
      break;
    case 5:
      // Sad brows, droopy eyes, frown and a tear.
      line(ctx, p(-27, -24), p(-11, -32), C.ink, 3.5 * k * s);
      line(ctx, p(11, -32), p(27, -24), C.ink, 3.5 * k * s);
      eye(-16, -6, 12, 10, false);
      eye(16, -6, 12, 10, false);
      strokeLine(curve(p(-13, 24), [p(0, 12), p(13, 24)]));
      drop(ctx, cx - 17 * k, cy + 12 * k, Math.max(4 * k, 2.8), s);
      break;
  }
}

// ---- Blobby ----

/** Blobby's round body, stretched to fit the box (l, t) - (r, b). Designed for (12, 14) - (128, 128). */
function blob(l: number, t: number, r: number, b: number, s: number): Path2D {
  const x = (v: number) => (l + ((v - 12) / 116) * (r - l)) * s;
  const y = (v: number) => (t + ((v - 14) / 114) * (b - t)) * s;
  const p = new Path2D();
  p.moveTo(x(70), y(14));
  p.bezierCurveTo(x(106), y(10), x(130), y(36), x(128), y(72));
  p.bezierCurveTo(x(128), y(108), x(102), y(130), x(68), y(128));
  p.bezierCurveTo(x(32), y(128), x(10), y(104), x(12), y(70));
  p.bezierCurveTo(x(12), y(34), x(36), y(16), x(70), y(14));
  p.closePath();
  return p;
}

/** A raised arm: a thick curved stroke with an ink outline, and a round hand. */
function arm(ctx: Ctx, s: number, color: string, fx: number, fy: number, bx: number, by: number, hx: number, hy: number) {
  const p = new Path2D();
  p.moveTo(fx * s, fy * s);
  p.quadraticCurveTo(bx * s, by * s, hx * s, hy * s);
  stroke(ctx, p, C.ink, 13 * s, "round");
  stroke(ctx, p, color, 8 * s, "round");
  const hand = circle(hx * s, hy * s, 7 * s);
  fill(ctx, hand, color);
  stroke(ctx, hand, C.ink, 2.5 * s);
}

function drawBlobby(ctx: Ctx, s: number, mood: Mood, onOrange: boolean, pose: Pose) {
  const color = onOrange ? C.lime : C.orange;
  switch (mood) {
    case 1:
      // Arms up (drawn first so the body covers the shoulders), a smaller body and sparkles.
      arm(ctx, s, color, 34, 80, 16, 64, 15, 34);
      arm(ctx, s, color, 106, 80, 124, 64, 125, 34);
      outlined(ctx, blob(24, 30, 116, 124, s), color, s);
      face(ctx, mood, 70, 79, 0.85, s, pose);
      sparkle(ctx, 38, 13, 6 * pose.twinkle, s);
      sparkle(ctx, 102, 11, 7 * pose.twinkle, s);
      sparkle(ctx, 132, 72, 5 * pose.twinkle, s);
      break;
    case 2:
      outlined(ctx, blob(12, 14, 128, 128, s), color, s);
      face(ctx, mood, 70, 74, 1, s, pose);
      break;
    case 3:
      // A bit flat: lower and wider.
      outlined(ctx, blob(8, 30, 132, 128, s), color, s);
      face(ctx, mood, 70, 80, 1, s, pose);
      break;
    case 4:
      outlined(ctx, blob(12, 14, 128, 128, s), color, s);
      face(ctx, mood, 70, 76, 1, s, pose);
      drop(ctx, 118, 32, 5, s);
      drop(ctx, 129, 52, 3.5, s);
      break;
    case 5: {
      // Melting: a slumped dome that spreads into a puddle.
      const p = new Path2D();
      p.moveTo(6 * s, 126 * s);
      p.bezierCurveTo(6 * s, 114 * s, 18 * s, 112 * s, 28 * s, 110 * s);
      p.bezierCurveTo(22 * s, 82 * s, 40 * s, 54 * s, 70 * s, 54 * s);
      p.bezierCurveTo(100 * s, 54 * s, 118 * s, 82 * s, 112 * s, 110 * s);
      p.bezierCurveTo(122 * s, 112 * s, 134 * s, 114 * s, 134 * s, 126 * s);
      p.bezierCurveTo(134 * s, 134 * s, 6 * s, 134 * s, 6 * s, 126 * s);
      p.closePath();
      outlined(ctx, p, color, s);
      face(ctx, mood, 70, 88, 0.8, s, pose);
      break;
    }
  }
}

// ---- Bloomy ----

interface PlantShape {
  bendOut: number; // how far stems bend away from the middle (degrees)
  lean: number; // all stems tip right
  height: number; // stem length factor
  leaves: [number, number]; // leaf directions (0 = right, -90 = up, 90 = down)
}

const SHAPES: Record<Mood, PlantShape> = {
  1: { bendOut: 0, lean: 0, height: 1.04, leaves: [-145, -35] },
  2: { bendOut: 0, lean: 0, height: 1, leaves: [-150, -30] },
  3: { bendOut: 0, lean: 12, height: 0.98, leaves: [-138, -18] },
  4: { bendOut: 40, lean: 0, height: 0.92, leaves: [175, 5] },
  5: { bendOut: 105, lean: 0, height: 0.8, leaves: [140, 40] },
};

/** Where each stem ends on a healthy plant, for 1-4 flowers. */
const STEM_TIPS: Record<number, Pt[]> = {
  1: [[70, 24]],
  2: [[54, 32], [88, 26]],
  3: [[46, 38], [70, 20], [96, 34]],
  4: [[40, 44], [60, 22], [82, 18], [102, 40]],
};
/** Which stem carries the bud when there are this many stems. */
const BUD_STEM: Record<number, number> = { 1: 0, 2: 1, 3: 1, 4: 2 };

interface Stem {
  base: Pt;
  control: Pt;
  tip: Pt;
}

const rad = (deg: number) => (deg * Math.PI) / 180;

function pointAt(stem: Stem, t: number): Pt {
  const u = 1 - t;
  return [
    stem.base[0] * u * u + stem.control[0] * 2 * u * t + stem.tip[0] * t * t,
    stem.base[1] * u * u + stem.control[1] * 2 * u * t + stem.tip[1] * t * t,
  ];
}

/** Turns a healthy stem (from the soil up to the tip) into the stem for this mood. */
function bend(tipX: number, tipY: number, shape: PlantShape, sway: number, sprout: boolean, grow = 1): Stem {
  const baseX = 70 + (tipX - 70) * 0.15;
  const baseY = 86;
  const dx = tipX - baseX;
  const dy = baseY - tipY;
  const length = Math.hypot(dx, dy) * shape.height * grow;
  const upright = Math.atan2(dx, dy);
  const outward = tipX >= 70 ? 1 : -1;
  const factor = sprout ? 0.45 : Math.abs(tipX - 70) < 15 ? 0.7 : 1;
  const keepUpright = Math.max(1 - shape.bendOut / 150, 0.2);
  const angle = upright * keepUpright + rad(outward * shape.bendOut * factor + shape.lean + sway);
  const controlAngle = upright + rad((shape.lean + sway) * 0.5);
  return {
    base: [baseX, baseY],
    control: [baseX + 0.55 * length * Math.sin(controlAngle), baseY - 0.55 * length * Math.cos(controlAngle)],
    tip: [baseX + length * Math.sin(angle), baseY - length * Math.cos(angle)],
  };
}

/** A pointed leaf growing from [at] in the direction [angle] (degrees). */
function leaf(ctx: Ctx, at: Pt, angle: number, length: number, width: number, color: string, s: number) {
  const dir: Pt = [Math.cos(rad(angle)), Math.sin(rad(angle))];
  const normal: Pt = [-dir[1], dir[0]];
  const mid: Pt = [at[0] + (dir[0] * length) / 2, at[1] + (dir[1] * length) / 2];
  const end: Pt = [at[0] + dir[0] * length, at[1] + dir[1] * length];
  const a: Pt = [mid[0] + normal[0] * width, mid[1] + normal[1] * width];
  const b: Pt = [mid[0] - normal[0] * width, mid[1] - normal[1] * width];
  const p = new Path2D();
  p.moveTo(at[0] * s, at[1] * s);
  p.quadraticCurveTo(a[0] * s, a[1] * s, end[0] * s, end[1] * s);
  p.quadraticCurveTo(b[0] * s, b[1] * s, at[0] * s, at[1] * s);
  p.closePath();
  outlined(ctx, p, color, s, 2);
}

/** A flower: round petals around an orange centre. Sadder moods = fewer, smaller, paler petals. */
function flower(ctx: Ctx, center: Pt, mood: Mood, s: number) {
  const looks: Record<Mood, [number, number, number, string, number]> = {
    1: [6, 6.5, 7.5, C.pink, -90],
    2: [5, 6.5, 7.5, C.pink, -90],
    3: [5, 6, 7, C.pink, -70],
    4: [5, 5.5, 6.5, C.palePetal, 90],
    5: [3, 5, 6, C.dryPetal, 90],
  };
  const [petals, radius, distance, color, start] = looks[mood];
  for (let i = 0; i < petals; i++) {
    const a = rad(start + (i * 360) / petals);
    const c = circle((center[0] + Math.cos(a) * distance) * s, (center[1] + Math.sin(a) * distance) * s, radius * s);
    fill(ctx, c, color);
    stroke(ctx, c, C.ink, 2 * s);
  }
  const middle = circle(center[0] * s, center[1] * s, (mood === 1 ? 5.5 : 5) * s);
  fill(ctx, middle, C.orange);
  stroke(ctx, middle, C.ink, 2 * s);
}

/** A closed flower bud at the end of [stem]; [growth] (0-1) makes it bigger. */
function bud(ctx: Ctx, stem: Stem, growth: number, mood: Mood, plantColor: string, s: number) {
  const tip = stem.tip;
  const near = pointAt(stem, 0.9);
  const along: Pt = [tip[0] - near[0], tip[1] - near[1]];
  const len = Math.max(Math.hypot(along[0], along[1]), 0.01);
  const dir: Pt = [along[0] / len, along[1] / len];
  const angle = (Math.atan2(dir[1], dir[0]) * 180) / Math.PI;
  const size = 4.5 + 4.5 * growth;
  const petal = mood === 4 ? C.palePetal : mood === 5 ? C.dryPetal : C.pink;
  const c = circle((tip[0] + dir[0] * size * 0.9) * s, (tip[1] + dir[1] * size * 0.9) * s, size * s);
  fill(ctx, c, petal);
  stroke(ctx, c, C.ink, 2 * s);
  leaf(ctx, tip, angle - 38, size * 1.7, size * 0.55, plantColor, s);
  leaf(ctx, tip, angle + 38, size * 1.7, size * 0.55, plantColor, s);
}

function fallenPetal(ctx: Ctx, cx: number, cy: number, s: number) {
  const p = oval(cx * s, cy * s, 4.5 * s, 2.5 * s);
  fill(ctx, p, C.dryPetal);
  stroke(ctx, p, C.ink, 1.5 * s);
}

function drawBloomy(ctx: Ctx, s: number, mood: Mood, flowers: number, onOrange: boolean, pose: Pose, budDays: number) {
  const potColor = onOrange ? C.lilac : C.orange;
  const plantColor = mood === 4 ? C.paleLeaf : mood === 5 ? C.brown : C.lime;
  const shape = SHAPES[mood];
  const count = Math.min(4, Math.max(0, Math.round(flowers)));
  const sway = pose.sway * 4;
  const budGrowth = count < 4 ? Math.min(6, Math.max(0, budDays)) / 7 : 0;
  const hasBud = budGrowth > 0;
  const stemCount = hasBud ? count + 1 : count;
  const budIndex = hasBud ? BUD_STEM[stemCount] : -1;

  // 1. Stems (their bottoms are hidden by the soil and pot, drawn later).
  const stems: Stem[] =
    stemCount === 0
      ? [bend(70, 52, shape, sway, true)]
      : STEM_TIPS[stemCount].map(([x, y], i) => bend(x, y, shape, sway, false, i === budIndex ? 0.55 + 0.45 * budGrowth : 1));
  for (const stem of stems) {
    const p = new Path2D();
    p.moveTo(stem.base[0] * s, stem.base[1] * s);
    p.quadraticCurveTo(stem.control[0] * s, stem.control[1] * s, stem.tip[0] * s, stem.tip[1] * s);
    stroke(ctx, p, C.ink, 6.5 * s, "round");
    stroke(ctx, p, plantColor, 3.5 * s, "round");
  }

  // 2. Two leaves: at the top of the sprout, or low on the middle stem.
  const [left, right] = shape.leaves;
  if (stemCount === 0) {
    const tip = stems[0].tip;
    const [l, r] = mood === 5 ? [165, 15] : [left, right]; // a wilting sprout's leaves hang sideways
    leaf(ctx, tip, l + sway, 22, 9, plantColor, s);
    leaf(ctx, tip, r + sway, 22, 9, plantColor, s);
  } else {
    const at = pointAt(stems[Math.floor((stemCount - 1) / 2)], 0.35);
    leaf(ctx, at, left + sway, 18, 7, plantColor, s);
    leaf(ctx, at, right + sway, 18, 7, plantColor, s);
  }

  // 3. Flowers on the stem tips, and the bud on its own stem.
  stems.forEach((stem, i) => {
    if (i === budIndex) bud(ctx, stem, budGrowth, mood, plantColor, s);
    else if (stemCount > 0) flower(ctx, stem.tip, mood, s);
  });

  // 4. Pot: soil, body, rim, then the face on the pot.
  const soil = oval(70 * s, 83 * s, 34 * s, 4.5 * s);
  fill(ctx, soil, C.soil);
  stroke(ctx, soil, C.ink, 2 * s);
  const body = new Path2D();
  body.moveTo(38 * s, 96 * s);
  body.lineTo(102 * s, 96 * s);
  body.lineTo(95 * s, 132 * s);
  body.quadraticCurveTo(94 * s, 136 * s, 90 * s, 136 * s);
  body.lineTo(50 * s, 136 * s);
  body.quadraticCurveTo(46 * s, 136 * s, 45 * s, 132 * s);
  body.closePath();
  outlined(ctx, body, potColor, s);
  const rim = new Path2D();
  rim.roundRect(32 * s, 82 * s, 76 * s, 14 * s, 4 * s);
  outlined(ctx, rim, potColor, s);
  face(ctx, mood, 70, 115, 0.5, s, pose);

  // 5. Extras for the mood.
  if (mood === 1) {
    sparkle(ctx, 18, 24, 6 * pose.twinkle, s);
    sparkle(ctx, 124, 22, 7 * pose.twinkle, s);
    sparkle(ctx, 128, 72, 5 * pose.twinkle, s);
  } else if (mood === 4) {
    drop(ctx, 103, 101, 4, s);
  } else if (mood === 5 && count > 0) {
    fallenPetal(ctx, 17, 133, s);
    fallenPetal(ctx, 113, 135, s);
    fallenPetal(ctx, 126, 130, s);
  }
}

/** The mascot as pixels, for the toolbar icon (background script, OffscreenCanvas). */
export function mascotImageData(size: number, d: MascotDrawing): ImageData {
  const canvas = new OffscreenCanvas(size, size);
  const ctx = canvas.getContext("2d") as OffscreenCanvasRenderingContext2D;
  drawMascot(ctx, size, d);
  return ctx.getImageData(0, 0, size, size);
}

/** The mascot as a PNG data: URL (for notification pictures). */
export async function mascotDataUrl(size: number, d: MascotDrawing): Promise<string> {
  const canvas = new OffscreenCanvas(size, size);
  const ctx = canvas.getContext("2d") as OffscreenCanvasRenderingContext2D;
  drawMascot(ctx, size, d);
  const blob = await canvas.convertToBlob({ type: "image/png" });
  const bytes = new Uint8Array(await blob.arrayBuffer());
  let bin = "";
  for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return "data:image/png;base64," + btoa(bin);
}
