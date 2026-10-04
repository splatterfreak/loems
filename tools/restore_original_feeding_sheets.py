#!/usr/bin/env python3
"""Restore the last known-good full-body feeding frames as lossless WebP.

The source PNGs are exported from the commit immediately before the project's
PNG-to-WebP migration.  This intentionally preserves every original head,
face, jaw, body, and food pixel; it does not construct or overlay mouth parts.
"""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = (
    ROOT
    / "art/intermediate/git_original_feeding/app/src/main/res/drawable-nodpi"
)
TARGET = ROOT / "app/src/main/res/drawable-nodpi"

NAMES = (
    "loem_good_ham_sheet",
    "loem_good_melon_sheet",
    "loem_wing_evolution_ham_sheet",
    "loem_wing_evolution_melon_sheet",
    "loem_stormkaiser_ham_sheet",
    "loem_stormkaiser_melon_sheet",
    "loem_stormkaiser_female_ham_sheet",
    "loem_stormkaiser_female_melon_sheet",
)


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = list(rgba.get_flattened_data())
    rgba.putdata([(0, 0, 0, 0) if a == 0 else (r, g, b, a) for r, g, b, a in pixels])
    return rgba


def main() -> None:
    TARGET.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        source_path = SOURCE / f"{name}.png"
        target_path = TARGET / f"{name}.webp"
        source = clear_transparent_rgb(Image.open(source_path))
        source.save(target_path, "WEBP", lossless=True, quality=100, method=6, exact=True)

        decoded = Image.open(target_path).convert("RGBA")
        if decoded.size != source.size or decoded.tobytes() != source.tobytes():
            raise RuntimeError(f"Lossless round-trip mismatch: {target_path}")
        print(f"restored {target_path.relative_to(ROOT)} ({decoded.width}x{decoded.height})")


if __name__ == "__main__":
    main()
