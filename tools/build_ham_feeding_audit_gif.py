#!/usr/bin/env python3
"""Build one animated audit preview for every production feeding sheet of one food."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def sheet_frames(image: Image.Image) -> list[Image.Image]:
    if image.size == (1536, 1024):
        columns, rows = 3, 2
    elif image.size == (2048, 1536):
        columns, rows = 4, 3
    else:
        raise ValueError(f"Unsupported feeding sheet size: {image.size}")
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
    parser.add_argument("--food", choices=("ham", "melon"), default="ham")
    parser.add_argument("--thumb", type=int, default=96)
    parser.add_argument("--duration", type=int, default=220)
    args = parser.parse_args()

    paths = sorted(args.source.glob(f"*{args.food}*sheet.webp"))
    baby = args.source / ("loem_feeding_sheet.webp" if args.food == "ham" else "loem_melon_sheet.webp")
    paths = [path for path in paths if path != baby]
    if baby.exists():
        paths.insert(0, baby)
    label_width = 330
    row_height = args.thumb + 8
    font = ImageFont.load_default(size=14)
    canvases: list[Image.Image] = []

    loaded = [(path, sheet_frames(Image.open(path).convert("RGBA"))) for path in paths]
    for progress in range(6):
        canvas = Image.new("RGBA", (label_width + args.thumb, len(paths) * row_height), (24, 20, 34, 255))
        draw = ImageDraw.Draw(canvas)
        for row, (path, frames) in enumerate(loaded):
            # For 12-frame sheets, retain open/closed pairs in the compact
            # audit instead of sampling only the open frames.
            index = progress if len(frames) == 6 else (0, 1, 4, 5, 8, 11)[progress]
            preview = frames[index].copy()
            preview.thumbnail((args.thumb, args.thumb), Image.Resampling.LANCZOS)
            y = row * row_height
            draw.text((8, y + 38), path.stem, fill=(245, 240, 255, 255), font=font)
            canvas.alpha_composite(preview, (label_width + (args.thumb - preview.width) // 2, y))
            draw.line((0, y + row_height - 1, canvas.width, y + row_height - 1), fill=(64, 52, 82, 255))
        canvases.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    canvases[0].save(
        args.output,
        save_all=True,
        append_images=canvases[1:],
        duration=args.duration,
        loop=0,
        disposal=2,
    )


if __name__ == "__main__":
    main()
