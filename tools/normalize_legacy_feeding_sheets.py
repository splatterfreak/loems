#!/usr/bin/env python3
"""Repack legacy feeding art into stable 512 px cells without resizing it."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


FRAME_RECTS: dict[str, tuple[tuple[int, int, int, int], ...]] = {
    "good_melon": (
        (126, 85, 403, 347),
        (560, 86, 371, 346),
        (996, 87, 369, 346),
        (123, 549, 369, 350),
        (559, 549, 367, 350),
        (996, 547, 366, 352),
    ),
    "good_ham": (
        (48, 64, 416, 400),
        (560, 64, 416, 400),
        (1072, 64, 416, 400),
        (48, 576, 416, 400),
        (560, 576, 416, 400),
        (1072, 576, 416, 400),
    ),
    "bad_ham": (
        (83, 165, 405, 236),
        (589, 173, 397, 235),
        (1087, 179, 334, 227),
        (99, 598, 375, 237),
        (596, 596, 359, 237),
        (1084, 612, 331, 221),
    ),
    "bad_melon": (
        (112, 176, 417, 244),
        (590, 177, 379, 243),
        (1042, 178, 384, 242),
        (125, 586, 343, 244),
        (592, 577, 316, 260),
        (1050, 601, 349, 236),
    ),
}


def body_anchor(frame: Image.Image) -> tuple[float, int]:
    points: list[tuple[int, int]] = []
    for y in range(frame.height):
        for x in range(frame.width):
            red, green, blue, alpha = frame.getpixel((x, y))
            if (
                alpha > 32
                and 65 <= max(red, green, blue) <= 245
                and max(red, green, blue) - min(red, green, blue) <= 30
            ):
                points.append((x, y))
    if not points:
        raise ValueError("No neutral-gray body pixels found")
    return sum(x for x, _ in points) / len(points), max(y for _, y in points)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("kind", choices=FRAME_RECTS)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--target-x", type=int, default=244)
    parser.add_argument("--target-ground", type=int, default=408)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    output = Image.new("RGBA", (1536, 1024))
    for index, (left, top, width, height) in enumerate(FRAME_RECTS[args.kind]):
        frame = source.crop((left, top, left + width, top + height))
        center_x, ground = body_anchor(frame)
        cell = Image.new("RGBA", (512, 512))
        cell.alpha_composite(
            frame,
            (round(args.target_x - center_x), args.target_ground - ground),
        )
        output.alpha_composite(cell, ((index % 3) * 512, (index // 3) * 512))

    # WebP may preserve RGB from the legacy PNG beneath fully transparent
    # pixels. Clear it so no stale neighbouring sprite can bleed at runtime.
    pixels = list(output.get_flattened_data())
    output.putdata(
        [(0, 0, 0, 0) if alpha == 0 else (red, green, blue, alpha)
         for red, green, blue, alpha in pixels]
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    output.save(args.output, "WEBP", lossless=True, quality=100, method=6, exact=True)


if __name__ == "__main__":
    main()
