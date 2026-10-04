#!/usr/bin/env python3
"""Remove legacy grid/separator pixels from regular sprite-sheet cell borders."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--columns", type=int, required=True)
    parser.add_argument("--rows", type=int, required=True)
    parser.add_argument("--width", type=int, default=3)
    args = parser.parse_args()

    image = Image.open(args.source).convert("RGBA")
    cell_width = image.width // args.columns
    cell_height = image.height // args.rows
    draw = ImageDraw.Draw(image)
    for column in range(args.columns + 1):
        x = min(image.width - 1, column * cell_width)
        draw.rectangle((max(0, x - args.width), 0, min(image.width - 1, x + args.width), image.height - 1), fill=(0, 0, 0, 0))
    for row in range(args.rows + 1):
        y = min(image.height - 1, row * cell_height)
        draw.rectangle((0, max(0, y - args.width), image.width - 1, min(image.height - 1, y + args.width)), fill=(0, 0, 0, 0))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    # `exact=True` keeps cleared RGB values at zero beneath alpha=0 instead of
    # letting the WebP encoder synthesize invisible color data for compression.
    image.save(args.output, "WEBP", lossless=True, quality=100, method=6, exact=True)


if __name__ == "__main__":
    main()
