#!/usr/bin/env python3
"""Build a labeled contact sheet of candidate mouth-pose source frames."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
OUTPUT = ROOT / "art" / "previews" / "feeding_standard" / "mouth_source_contact.png"
CELL = 512
THUMB = 256


def first_frame(path: Path) -> Image.Image:
    sheet = Image.open(path).convert("RGBA")
    return sheet.crop((0, 0, CELL, CELL)).resize((THUMB, THUMB), Image.Resampling.LANCZOS)


def main() -> None:
    sources = sorted(DRAWABLE.glob("loem*_hungry_sheet.webp"))
    columns = 4
    rows = (len(sources) + columns - 1) // columns
    label_height = 34
    output = Image.new("RGBA", (columns * THUMB, rows * (THUMB + label_height)), "white")
    draw = ImageDraw.Draw(output)
    for index, path in enumerate(sources):
        x = (index % columns) * THUMB
        y = (index // columns) * (THUMB + label_height)
        output.alpha_composite(first_frame(path), (x, y))
        draw.text((x + 4, y + THUMB + 4), path.stem.removesuffix("_hungry_sheet"), fill="black")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    output.convert("RGB").save(OUTPUT, "PNG")


if __name__ == "__main__":
    main()
