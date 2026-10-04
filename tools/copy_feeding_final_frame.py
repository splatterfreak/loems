#!/usr/bin/env python3
"""Copy an approved food-free final frame between matching feeding sheets."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("target", type=Path)
    parser.add_argument("reference", type=Path)
    parser.add_argument("--columns", type=int, default=3)
    parser.add_argument("--rows", type=int, default=2)
    args = parser.parse_args()

    target = Image.open(args.target).convert("RGBA")
    reference = Image.open(args.reference).convert("RGBA")
    if target.size != reference.size:
        raise ValueError("Target and reference sheets must have matching dimensions")
    cell_width = target.width // args.columns
    cell_height = target.height // args.rows
    left = (args.columns - 1) * cell_width
    top = (args.rows - 1) * cell_height
    final_frame = reference.crop((left, top, left + cell_width, top + cell_height))
    target.paste(final_frame, (left, top))
    target.save(args.target, "WEBP", lossless=True, quality=100, method=6)


if __name__ == "__main__":
    main()
