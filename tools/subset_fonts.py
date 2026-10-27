"""Make small copies of the two app fonts: only the weights the app uses, only Latin letters.

Usage (needs `pip install fonttools`): download the full variable fonts from Google Fonts
(BricolageGrotesque[opsz,wdth,wght].ttf and DMSans[opsz,wght].ttf), rename them to
bricolage_grotesque.ttf and dm_sans.ttf in one folder, then run:
    python tools/subset_fonts.py <that folder> app/src/main/res/font
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

src, out = sys.argv[1], sys.argv[2]

# Latin (English + accented European names), common punctuation, a few symbols.
UNICODES = (
    list(range(0x20, 0x7F)) + list(range(0xA0, 0x180)) +
    [0x131, 0x152, 0x153, 0x2C6, 0x2DA, 0x2DC] +
    list(range(0x2010, 0x2028)) + list(range(0x2030, 0x203B)) +
    [0x20AC, 0x20B9, 0x2122, 0x2212, 0x2215]
)

def make(name, axes, outname, static_name=None):
    font = TTFont(f"{src}/{name}")
    font = instancer.instantiateVariableFont(font, axes)
    opts = subset.Options()
    # Default features only (kerning, ligatures, fractions...); drops unused alternates like old-style numbers.
    opts.name_IDs = ["*"]
    opts.notdef_outline = True
    opts.hinting = False          # Android renders without TrueType hints on high-dpi screens
    opts.drop_tables += ["DSIG"]
    sub = subset.Subsetter(opts)
    sub.populate(unicodes=UNICODES)
    sub.subset(font)
    font.save(f"{out}/{outname}")

# Bricolage: the app only uses ExtraBold (800) at the default width and optical size -> one static font.
make("bricolage_grotesque.ttf", {"wght": 800, "wdth": 100, "opsz": 96}, "bricolage_grotesque.ttf")
# DM Sans: the app uses 400, 500 and 700 -> keep the weight axis but only 400-700; optical size fixed at its default.
make("dm_sans.ttf", {"opsz": 9, "wght": (400, 700)}, "dm_sans.ttf")
