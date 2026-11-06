#!/usr/bin/env python3
"""
Turns the mascot sounds the developer made with AI (design/sounds/*.mp3) into the app's small
sound files (app/src/main/res/raw/mascot_*.ogg). For each sound it:

  1. cuts the silence before and after it (AI tools add up to a second),
  2. removes low rumble and fades both ends, so there's no click,
  3. evens out the volume (the tap a bit softer, the notification a bit louder),
  4. adds the same soft echo to all of them, so they sound like one little creature,
  5. saves a small mono Ogg Vorbis file, plus a WAV in design/sounds/preview/ to listen to.

Needs ffmpeg (with libvorbis). Run from the project folder:

  python3 tools/prepare_sounds.py
"""
import math
import os
import re
import struct
import subprocess

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SOURCE = os.path.join(ROOT, "design", "sounds")
PREVIEW = os.path.join(SOURCE, "preview")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw")

# (file from the AI tool, name in the app, loudness in LUFS: closer to 0 is louder)
SOUNDS = [
    ("tap.mp3", "mascot_tap", -22),  # tapping the mascot: heard often, so softest
    ("happy.mp3", "mascot_happy", -20),  # well done, celebrations
    ("Pause_screen.mp3", "mascot_uhoh", -20),  # the Pause screen
    ("notification.mp3", "mascot_nudge", -18),  # notifications: must be noticed
]

RATE = 48000
QUIET_DB = -50  # anything softer counts as silence
ECHO = "aecho=1.0:0.9:90|190|300:0.25|0.12|0.05"  # three soft repeats, each quieter
TAIL = 0.45  # seconds added at the end for the echo to fade


def ffmpeg(*args, capture=False):
    result = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-y", *args], capture_output=True, check=True)
    return result.stderr.decode() if capture else result


def sound_edges(path):
    """Where the sound starts and ends (seconds), from its loudness every 10 ms."""
    raw = subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-i", path, "-ac", "1", "-ar", str(RATE), "-f", "f32le", "-"],
        capture_output=True, check=True,
    ).stdout
    samples = struct.unpack("<%df" % (len(raw) // 4), raw)
    step = RATE // 100
    levels = []
    for i in range(0, len(samples) - step, step):
        part = samples[i:i + step]
        levels.append(20 * math.log10(math.sqrt(sum(v * v for v in part) / step) + 1e-9))
    loud = [level > QUIET_DB for level in levels]
    # Three loud steps in a row, so a lone click before or after doesn't count.
    first = next(i for i in range(len(loud) - 2) if all(loud[i:i + 3]))
    last = next(i for i in range(len(loud) - 1, 1, -1) if all(loud[i - 2:i + 1]))
    return max(0.0, first / 100 - 0.015), (last + 1) / 100 + 0.03


def loudness(path, filters):
    report = ffmpeg("-i", path, "-af", filters + ",ebur128", "-f", "null", "-", capture=True)
    return float(re.findall(r"I:\s+(-?[\d.]+) LUFS", report)[-1])


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    for source, name, target in SOUNDS:
        path = os.path.join(SOURCE, source)
        start, end = sound_edges(path)
        length = end - start
        fade = min(0.15, length * 0.15)
        dry = (
            f"atrim={start:.3f}:{end:.3f},asetpts=PTS-STARTPTS,highpass=f=120,"
            f"afade=t=in:d=0.008,afade=t=out:st={length - fade:.3f}:d={fade:.3f},"
            # Gently tames the loudest moments, so no part jumps out.
            "acompressor=threshold=-24dB:ratio=2.5:attack=3:release=80"
        )
        gain = target - loudness(path, dry)
        total = length + TAIL
        wet = (
            f"{dry},volume={gain:.1f}dB,apad=pad_dur={TAIL},{ECHO},"
            f"afade=t=out:st={total - 0.12:.3f}:d=0.12,alimiter=limit=0.89:level=false,"
            "aresample=32000"
        )
        ogg = os.path.join(OUT, name + ".ogg")
        ffmpeg("-loglevel", "error", "-i", path, "-af", wet, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", ogg)
        ffmpeg("-loglevel", "error", "-i", path, "-af", wet, "-ac", "1", os.path.join(PREVIEW, name + ".wav"))
        print("%-13s %.2fs of sound (from %.2fs), %+.1f dB, %5d bytes" % (name, length, start, gain, os.path.getsize(ogg)))


if __name__ == "__main__":
    main()
