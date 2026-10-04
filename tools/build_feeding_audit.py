#!/usr/bin/env python3
"""Build visual QA contacts and synchronized GIFs for every feeding sheet."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CELL = 512
BG = (22, 20, 31, 255)


def sheets(food: str) -> list[Path]:
    pattern = "*_melon_sheet.webp" if food == "melon" else "*_ham_sheet.webp"
    paths = list(DRAWABLE.glob(pattern))
    if food == "ham":
        paths.append(DRAWABLE / "loem_feeding_sheet.webp")
    return sorted(set(paths), key=lambda path: path.name)


def frames(path: Path) -> list[Image.Image]:
    sheet = Image.open(path).convert("RGBA")
    columns = sheet.width // CELL
    rows = sheet.height // CELL
    return [
        sheet.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))
        for row in range(rows)
        for column in range(columns)
    ]


def thumbnail(frame: Image.Image, size: int) -> Image.Image:
    result = Image.new("RGBA", (size, size), BG)
    scaled = frame.copy()
    scaled.thumbnail((size - 8, size - 8), Image.Resampling.LANCZOS)
    result.alpha_composite(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    return result


def build_contact(paths: list[Path], output: Path) -> None:
    thumb = 128
    label = 24
    max_frames = max(len(frames(path)) for path in paths)
    canvas = Image.new("RGBA", (max_frames * thumb, len(paths) * (thumb + label)), BG)
    draw = ImageDraw.Draw(canvas)
    for row, path in enumerate(paths):
        y = row * (thumb + label)
        draw.text((6, y + 4), path.stem, fill=(238, 235, 247, 255))
        for column, frame in enumerate(frames(path)):
            canvas.alpha_composite(thumbnail(frame, thumb), (column * thumb, y + label))
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(output, quality=94)


def build_gif(paths: list[Path], output: Path) -> None:
    tile = 240
    columns = 5
    rows = (len(paths) + columns - 1) // columns
    loaded = [(path, frames(path)) for path in paths]
    animation = []
    for tick in range(12):
        canvas = Image.new("RGBA", (columns * tile, rows * tile), BG)
        draw = ImageDraw.Draw(canvas)
        for index, (path, sequence) in enumerate(loaded):
            frame_index = tick if len(sequence) == 12 else min(tick // 2, len(sequence) - 1)
            x = (index % columns) * tile
            y = (index // columns) * tile
            canvas.alpha_composite(thumbnail(sequence[frame_index], tile - 22), (x + 11, y + 18))
            draw.text((x + 6, y + 3), path.stem, fill=(238, 235, 247, 255))
        animation.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE))
    output.parent.mkdir(parents=True, exist_ok=True)
    animation[0].save(
        output,
        save_all=True,
        append_images=animation[1:],
        duration=190,
        loop=0,
        disposal=2,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    for food in ("ham", "melon"):
        paths = sheets(food)
        build_contact(paths, args.output_dir / f"all_{food}_contact.jpg")
        build_gif(paths, args.output_dir / f"all_{food}_audit.gif")
        print(f"{food}: {len(paths)} sheets")


if __name__ == "__main__":
    main()
