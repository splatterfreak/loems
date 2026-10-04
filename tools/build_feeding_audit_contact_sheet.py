#!/usr/bin/env python3
"""Render compact frame strips for reviewing every production feeding sheet."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def grid_for(image: Image.Image) -> tuple[int, int]:
    if image.size == (2048, 1536):
        return 4, 3
    if image.size == (1536, 1024):
        return 3, 2
    raise ValueError(f"Unsupported feeding sheet size: {image.size}")


def frames(image: Image.Image) -> list[Image.Image]:
    columns, rows = grid_for(image)
    width = image.width // columns
    height = image.height // rows
    return [
        image.crop((column * width, row * height, (column + 1) * width, (row + 1) * height))
        for row in range(rows)
        for column in range(columns)
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--food", choices=("melon", "ham"), required=True)
    parser.add_argument("--thumb", type=int, default=128)
    args = parser.parse_args()

    paths = sorted(args.source.glob(f"*{args.food}*sheet.webp"))
    if args.food == "ham":
        baby_ham = args.source / "loem_feeding_sheet.webp"
        if baby_ham.exists():
            paths.insert(0, baby_ham)

    label_width = 350
    row_height = args.thumb + 28
    maximum_frames = max(len(frames(Image.open(path))) for path in paths)
    canvas = Image.new(
        "RGBA",
        (label_width + maximum_frames * args.thumb, len(paths) * row_height),
        (24, 20, 34, 255),
    )
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=16)

    for row, path in enumerate(paths):
        image = Image.open(path).convert("RGBA")
        y = row * row_height
        draw.text((8, y + 8), path.stem, fill=(245, 240, 255, 255), font=font)
        for index, frame in enumerate(frames(image)):
            preview = frame.copy()
            preview.thumbnail((args.thumb, args.thumb), Image.Resampling.LANCZOS)
            x = label_width + index * args.thumb
            canvas.alpha_composite(preview, (x + (args.thumb - preview.width) // 2, y))
            draw.text((x + 4, y + args.thumb + 4), str(index + 1), fill=(190, 175, 220, 255), font=font)
        draw.line((0, y + row_height - 1, canvas.width, y + row_height - 1), fill=(64, 52, 82, 255))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(args.output, quality=92)


if __name__ == "__main__":
    main()
