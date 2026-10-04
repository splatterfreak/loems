#!/usr/bin/env python3
"""Extract one frame from a regular 512px Loems sprite sheet."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


CELL = 512


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sheet", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--frame", type=int, default=1, help="One-based frame index")
    parser.add_argument("--columns", type=int, default=3)
    args = parser.parse_args()
    image = Image.open(args.sheet).convert("RGBA")
    index = args.frame - 1
    x = (index % args.columns) * CELL
    y = (index // args.columns) * CELL
    args.output.parent.mkdir(parents=True, exist_ok=True)
    image.crop((x, y, x + CELL, y + CELL)).save(args.output, "PNG")


if __name__ == "__main__":
    main()
